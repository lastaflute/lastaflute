/*
 * Copyright 2015-2022 the original author or authors.
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
package org.lastaflute.core.json.adapter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.lastaflute.core.json.JsonMappingOption;

import com.google.gson.Gson;
import com.google.gson.InstanceCreator;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.bind.CollectionTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * @author jflute
 * @since 0.7.3 (2015/12/29 Tuesday)
 */
public interface CollectionGsonAdaptable { // to show property path in exception message

    // ===================================================================================
    //                                                                        Type Adapter
    //                                                                        ============
    class WrappingCollectionTypeAdapterFactory implements TypeAdapterFactory {

        protected final CollectionTypeAdapterFactory embeddedFactory;
        protected final JsonMappingOption gsonOption;

        public WrappingCollectionTypeAdapterFactory(JsonMappingOption gsonOption) {
            this.embeddedFactory = createEmbeddedFactory();
            this.gsonOption = gsonOption;
        }

        protected CollectionTypeAdapterFactory createEmbeddedFactory() {
            final Map<Type, InstanceCreator<?>> instanceCreators = prepareInstanceCreators();
            final ConstructorConstructor constructor = createConstructorConstructor(instanceCreators);
            return newCollectionTypeAdapterFactory(constructor);
        }

        protected Map<Type, InstanceCreator<?>> prepareInstanceCreators() {
            return Collections.emptyMap();
        }

        protected ConstructorConstructor createConstructorConstructor(Map<Type, InstanceCreator<?>> instanceCreators) {
            return new ConstructorConstructor(instanceCreators);
        }

        protected CollectionTypeAdapterFactory newCollectionTypeAdapterFactory(ConstructorConstructor constructor) {
            return new CollectionTypeAdapterFactory(constructor);
        }

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            final TypeAdapter<T> embedded = embeddedFactory.create(gson, typeToken); // null allowed when other types
            return embedded != null ? createWrappingTypeAdapterCollection(embedded) : null;
        }

        @SuppressWarnings("unchecked")
        protected <T> TypeAdapter<T> createWrappingTypeAdapterCollection(TypeAdapter<T> embedded) {
            return (TypeAdapter<T>) newWrappingTypeAdapterCollection(embedded, gsonOption);
        }

        protected <T> WrappingTypeAdapterCollection newWrappingTypeAdapterCollection(TypeAdapter<T> embedded, JsonMappingOption option) {
            return new WrappingTypeAdapterCollection(embedded, option);
        }
    }

    class WrappingTypeAdapterCollection extends TypeAdapter<Collection<?>> {

        protected final TypeAdapter<Collection<?>> embedded;
        protected final JsonMappingOption gsonOption;

        @SuppressWarnings("unchecked")
        public WrappingTypeAdapterCollection(TypeAdapter<?> embedded, JsonMappingOption gsonOption) {
            this.embedded = (TypeAdapter<Collection<?>>) embedded;
            this.gsonOption = gsonOption;
        }

        @Override
        public Collection<?> read(JsonReader in) throws IOException {
            if (gsonOption.isListNullToEmptyReading()) {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return Collections.emptyList();
                }
            }
            return embedded.read(in);
        }

        @Override
        public void write(JsonWriter out, Collection<?> collection) throws IOException {
            if (gsonOption.isListNullToEmptyWriting()) {
                if (collection == null) {
                    out.beginArray();
                    out.endArray();
                    return; // []
                }
            }
            embedded.write(out, collection);
        }
    }

    // ===================================================================================
    //                                                                             Creator
    //                                                                             =======
    default TypeAdapterFactory createCollectionTypeAdapterFactory() {
        return newWrappingCollectionTypeAdapterFactory(getGsonOption());
    }

    default WrappingCollectionTypeAdapterFactory newWrappingCollectionTypeAdapterFactory(JsonMappingOption option) {
        return new WrappingCollectionTypeAdapterFactory(option);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    JsonMappingOption getGsonOption();
}
