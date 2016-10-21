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
 * @author jflute
 */
public class LaReflectiveTypeAdapterFactory implements TypeAdapterFactory {

    protected final ConstructorConstructor constructorConstructor;
    protected final FieldNamingStrategy fieldNamingPolicy;
    protected final Excluder excluder;
    protected final JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory;

    public LaReflectiveTypeAdapterFactory(ConstructorConstructor constructorConstructor, FieldNamingStrategy fieldNamingPolicy,
            Excluder excluder, JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory) {
        this.constructorConstructor = constructorConstructor;
        this.fieldNamingPolicy = fieldNamingPolicy;
        this.excluder = excluder;
        this.jsonAdapterFactory = jsonAdapterFactory;
    }

    public boolean excludeField(Field f, boolean serialize) {
        return excludeField(f, serialize, excluder);
    }

    static boolean excludeField(Field f, boolean serialize, Excluder excluder) {
        return !excluder.excludeClass(f.getType(), serialize) && !excluder.excludeField(f, serialize);
    }

    /** first element holds the default name 
     * @param f The field to get name. (NotNull)
     * @return The read-only list of field names. (NotNull) */
    protected List<String> getFieldNames(Field f) {
        SerializedName annotation = f.getAnnotation(SerializedName.class);
        if (annotation == null) {
            String name = fieldNamingPolicy.translateName(f);
            return Collections.singletonList(name);
        }

        String serializedName = annotation.value();
        String[] alternates = annotation.alternate();
        if (alternates.length == 0) {
            return Collections.singletonList(serializedName);
        }

        List<String> fieldNames = new ArrayList<String>(alternates.length + 1);
        fieldNames.add(serializedName);
        for (String alternate : alternates) {
            fieldNames.add(alternate);
        }
        return fieldNames;
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, final TypeToken<T> type) {
        Class<? super T> raw = type.getRawType();

        if (!Object.class.isAssignableFrom(raw)) {
            return null; // it's a primitive!
        }

        ObjectConstructor<T> constructor = constructorConstructor.get(type);
        return new Adapter<T>(constructor, getBoundFields(gson, type, raw));
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
            @SuppressWarnings({ "unchecked", "rawtypes" }) // the type adapter and field type always agree
            @Override
            public void write(JsonWriter writer, Object value) throws IOException, IllegalAccessException {
                final Object fieldValue = field.get(value);
                final TypeAdapter realAdapter = prepareRealAdapter(context, fieldType, jsonAdapterPresent, typeAdapter);
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

            @SuppressWarnings({ "unchecked", "rawtypes" }) // the type adapter and field type always agree
            private TypeAdapter prepareRealAdapter(Gson context, TypeToken<?> fieldType, boolean jsonAdapterPresent,
                    TypeAdapter<?> typeAdapter) {
                return jsonAdapterPresent ? typeAdapter : new TypeAdapterRuntimeTypeWrapper(context, typeAdapter, fieldType.getType());
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
                Object fieldValue = field.get(value);
                return fieldValue != value; // avoid recursion for example for Throwable.cause
            }
        };
    }

    private Map<String, LaBoundField> getBoundFields(Gson context, TypeToken<?> type, Class<?> raw) {
        Map<String, LaBoundField> result = new LinkedHashMap<String, LaBoundField>();
        if (raw.isInterface()) {
            return result;
        }

        Type declaredType = type.getType();
        while (raw != Object.class) {
            Field[] fields = raw.getDeclaredFields();
            for (Field field : fields) {
                boolean serialize = excludeField(field, true);
                boolean deserialize = excludeField(field, false);
                if (!serialize && !deserialize) {
                    continue;
                }
                field.setAccessible(true);
                Type fieldType = $Gson$Types.resolve(type.getType(), raw, field.getGenericType());
                List<String> fieldNames = getFieldNames(field);
                LaBoundField previous = null;
                for (int i = 0; i < fieldNames.size(); ++i) {
                    String name = fieldNames.get(i);
                    if (i != 0)
                        serialize = false; // only serialize the default name
                    LaBoundField boundField = createBoundField(context, field, name, TypeToken.get(fieldType), serialize, deserialize);
                    LaBoundField replaced = result.put(name, boundField);
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

    public static final class Adapter<T> extends TypeAdapter<T> {
        private final ObjectConstructor<T> constructor;
        private final Map<String, LaBoundField> boundFields;

        Adapter(ObjectConstructor<T> constructor, Map<String, LaBoundField> boundFields) {
            this.constructor = constructor;
            this.boundFields = boundFields;
        }

        @Override
        public T read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            T instance = constructor.construct();

            try {
                in.beginObject();
                while (in.hasNext()) {
                    String name = in.nextName();
                    LaBoundField field = boundFields.get(name);
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
        public void write(JsonWriter out, T value) throws IOException {
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
