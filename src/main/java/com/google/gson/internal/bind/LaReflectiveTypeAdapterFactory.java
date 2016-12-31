/*
 * Copyright 2015-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.google.gson.internal.bind;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.$Gson$Types;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.ObjectConstructor;
import com.google.gson.internal.Primitives;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

// should be gson package because of package scope references
/**
 * @author modified by jflute (originated in Gson)
 * @since 0.8.5 (2016/10/21 Friday at showbase)
 */
public class LaReflectiveTypeAdapterFactory implements TypeAdapterFactory {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ConstructorConstructor constructorConstructor;
    protected final FieldNamingStrategy fieldNamingPolicy;
    protected final Excluder excluder;
    protected final JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LaReflectiveTypeAdapterFactory(ConstructorConstructor constructorConstructor, FieldNamingStrategy fieldNamingPolicy,
            Excluder excluder, JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory) {
        this.constructorConstructor = constructorConstructor;
        this.fieldNamingPolicy = fieldNamingPolicy;
        this.excluder = excluder;
        this.jsonAdapterFactory = jsonAdapterFactory;
    }

    // ===================================================================================
    //                                                                              Create
    //                                                                              ======
    @Override
    public <T> TypeAdapter<T> create(Gson gson, final TypeToken<T> type) {
        final Class<? super T> raw = type.getRawType();
        if (!Object.class.isAssignableFrom(raw)) {
            return null; // it's a primitive!
        }
        final ObjectConstructor<T> constructor = constructorConstructor.get(type);
        return new ReflextiveAdapter<T>(constructor, getBoundFields(gson, type, raw));
    }

    protected Map<String, LaBoundField> getBoundFields(Gson context, TypeToken<?> type, Class<?> raw) {
        final Map<String, LaBoundField> result = new LinkedHashMap<String, LaBoundField>();
        if (raw.isInterface()) {
            return result;
        }
        final Type declaredType = type.getType();
        while (raw != Object.class) {
            final Field[] fields = raw.getDeclaredFields();
            for (Field field : fields) {
                boolean serialize = excludeField(field, true);
                final boolean deserialize = excludeField(field, false);
                if (!serialize && !deserialize) {
                    continue;
                }
                field.setAccessible(true);
                final Type fieldType = $Gson$Types.resolve(type.getType(), raw, field.getGenericType());
                final List<String> fieldNames = getFieldNames(field);
                LaBoundField previous = null;
                for (int i = 0; i < fieldNames.size(); ++i) {
                    final String name = fieldNames.get(i);
                    if (i != 0)
                        serialize = false; // only serialize the default name
                    final LaBoundField boundField =
                            createBoundField(context, field, name, TypeToken.get(fieldType), serialize, deserialize);
                    final LaBoundField replaced = result.put(name, boundField);
                    if (previous == null)
                        previous = replaced;
                }
                if (previous != null) {
                    throw new IllegalArgumentException(declaredType + " declares multiple JSON fields named " + previous.name);
                }
            }
            type = TypeToken.get($Gson$Types.resolve(type.getType(), raw, raw.getGenericSuperclass()));
            raw = type.getRawType();
        }
        return result;
    }

    protected boolean excludeField(Field f, boolean serialize) {
        return excludeField(f, serialize, excluder);
    }

    protected static boolean excludeField(Field f, boolean serialize, Excluder excluder) {
        return !excluder.excludeClass(f.getType(), serialize) && !excluder.excludeField(f, serialize);
    }

    /** first element holds the default name 
     * @param f The field to get name. (NotNull)
     * @return The read-only list of field names. (NotNull) */
    protected List<String> getFieldNames(Field f) {
        final SerializedName annotation = f.getAnnotation(SerializedName.class);
        if (annotation == null) {
            String name = fieldNamingPolicy.translateName(f);
            return Collections.singletonList(name);
        }
        final String serializedName = annotation.value();
        final String[] alternates = annotation.alternate();
        if (alternates.length == 0) {
            return Collections.singletonList(serializedName);
        }
        final List<String> fieldNames = new ArrayList<String>(alternates.length + 1);
        fieldNames.add(serializedName);
        for (String alternate : alternates) {
            fieldNames.add(alternate);
        }
        return fieldNames;
    }

    protected LaReflectiveTypeAdapterFactory.LaBoundField createBoundField(final Gson context, final Field field, final String name,
            final TypeToken<?> fieldType, boolean serialize, boolean deserialize) {
        final boolean isPrimitive = Primitives.isPrimitive(fieldType.getRawType());
        // special casing primitives here saves ~5% on Android...
        JsonAdapter annotation = field.getAnnotation(JsonAdapter.class);
        TypeAdapter<?> mapped = null;
        if (annotation != null) {
            mapped = jsonAdapterFactory.getTypeAdapter(constructorConstructor, context, fieldType, annotation);
        }
        final boolean jsonAdapterPresent = mapped != null;
        if (mapped == null)
            mapped = context.getAdapter(fieldType);

        final TypeAdapter<?> typeAdapter = mapped;
        final boolean fieldingAvailable = typeAdapter instanceof LaJsonFieldingAvailable; // #for_lastaflute
        return new LaReflectiveTypeAdapterFactory.LaBoundField(name, serialize, deserialize) {
            @Override
            public void write(JsonWriter writer, Object value) throws IOException, IllegalAccessException {
                final Object fieldValue = field.get(value);
                final TypeAdapter<Object> realAdapter = prepareRealAdapter(context, fieldType, jsonAdapterPresent, typeAdapter);
                if (fieldingAvailable) {
                    try {
                        LaJsonFieldingContext.setJsonFieldOnThread(field); // to give the field to writer
                        realAdapter.write(writer, fieldValue);
                    } finally {
                        LaJsonFieldingContext.clearAccessContextOnThread();
                    }
                } else { // avoid try-finally cost
                    realAdapter.write(writer, fieldValue);
                }
            }

            @Override
            public void read(JsonReader reader, Object value) throws IOException, IllegalAccessException {
                final Object fieldValue;
                if (fieldingAvailable) { // #for_lastaflute
                    LaJsonFieldingContext.setJsonFieldOnThread(field); // to give the field to reader
                    try {
                        fieldValue = typeAdapter.read(reader);
                    } finally {
                        LaJsonFieldingContext.clearAccessContextOnThread();
                    }
                } else { // avoid try-finally cost
                    fieldValue = typeAdapter.read(reader);
                }
                if (fieldValue != null || !isPrimitive) {
                    field.set(value, fieldValue);
                }
            }

            @Override
            public boolean writeField(Object value) throws IOException, IllegalAccessException {
                if (!serialized)
                    return false;
                final Object fieldValue = field.get(value);
                return fieldValue != value; // avoid recursion for example for Throwable.cause
            }
        };
    }

    @SuppressWarnings("unchecked") // the type adapter and field type always agree
    protected TypeAdapter<Object> prepareRealAdapter(Gson context, TypeToken<?> fieldType, boolean jsonAdapterPresent,
            TypeAdapter<?> typeAdapter) {
        if (jsonAdapterPresent) {
            return (TypeAdapter<Object>) typeAdapter;
        } else {
            return newTypeAdapterRuntimeTypeWrapper(context, typeAdapter, fieldType.getType());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" }) // the type adapter and field type always agree
    protected TypeAdapterRuntimeTypeWrapper newTypeAdapterRuntimeTypeWrapper(Gson context, TypeAdapter<?> typeAdapter, Type fieldType) {
        return new TypeAdapterRuntimeTypeWrapper(context, typeAdapter, fieldType);
    }

    // ===================================================================================
    //                                                                         Bound Field
    //                                                                         ===========
    public static abstract class LaBoundField {

        protected final String name;
        protected final boolean serialized;
        protected final boolean deserialized;

        protected LaBoundField(String name, boolean serialized, boolean deserialized) {
            this.name = name;
            this.serialized = serialized;
            this.deserialized = deserialized;
        }

        public abstract void write(JsonWriter writer, Object value) throws IOException, IllegalAccessException;

        public abstract void read(JsonReader reader, Object value) throws IOException, IllegalAccessException;

        public abstract boolean writeField(Object value) throws IOException, IllegalAccessException;
    }

    // ===================================================================================
    //                                                                  Reflective Adapter
    //                                                                  ==================
    public static final class ReflextiveAdapter<PROPERTY> extends TypeAdapter<PROPERTY> {

        protected final ObjectConstructor<PROPERTY> constructor;
        protected final Map<String, LaBoundField> boundFields;

        protected ReflextiveAdapter(ObjectConstructor<PROPERTY> constructor, Map<String, LaBoundField> boundFields) {
            this.constructor = constructor;
            this.boundFields = boundFields;
        }

        @Override
        public PROPERTY read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            final PROPERTY instance = constructor.construct();

            try {
                in.beginObject();
                while (in.hasNext()) {
                    final String name = in.nextName();
                    final LaBoundField field = boundFields.get(name);
                    if (field == null || !field.deserialized) {
                        in.skipValue();
                    } else {
                        field.read(in, instance);
                    }
                }
            } catch (IllegalStateException e) {
                throw new JsonSyntaxException(e);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
            in.endObject();
            return instance;
        }

        @Override
        public void write(JsonWriter out, PROPERTY value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.beginObject();
            try {
                for (LaBoundField boundField : boundFields.values()) {
                    if (boundField.writeField(value)) {
                        out.name(boundField.name);
                        boundField.write(out, value);
                    }
                }
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
            out.endObject();
        }
    }
}
