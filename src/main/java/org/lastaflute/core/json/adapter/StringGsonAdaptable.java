/*
 * Copyright 2015-2024 the original author or authors.
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
import java.util.function.Predicate;

import org.lastaflute.core.json.JsonMappingOption;
import org.lastaflute.core.json.filter.JsonUnifiedTextReadingFilter;

import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * @author jflute
 */
public interface StringGsonAdaptable { // to show property path in exception message

    // ===================================================================================
    //                                                                        Type Adapter
    //                                                                        ============
    class TypeAdapterString extends TypeAdapter<String> {

        protected final TypeAdapter<String> realAdapter = TypeAdapters.STRING;
        protected final Class<?> yourType; // not null
        protected final JsonMappingOption gsonOption;
        protected final JsonUnifiedTextReadingFilter readingFilter; // null allowed
        protected final Predicate<Class<?>> emptyToNullReadingDeterminer; // null allowed
        protected final Predicate<Class<?>> nullToEmptyWritingDeterminer; // null allowed

        public TypeAdapterString(JsonMappingOption gsonOption) {
            this.yourType = String.class;
            this.gsonOption = gsonOption;
            this.readingFilter = JsonUnifiedTextReadingFilter.unify(gsonOption); // cache as plain for performance
            this.emptyToNullReadingDeterminer = gsonOption.getEmptyToNullReadingDeterminer().orElse(null); // me too
            this.nullToEmptyWritingDeterminer = gsonOption.getNullToEmptyWritingDeterminer().orElse(null); // me too
        }

        @Override
        public String read(JsonReader in) throws IOException {
            final String read = filterReading(realAdapter.read(in));
            if (read == null) { // filter makes it null
                return null;
            }
            if ("".equals(read) && isEmptyToNullReading()) { // option
                return null;
            } else { // mainly here
                return read;
            }
        }

        protected String filterReading(String text) {
            if (text == null) {
                return null;
            }
            return readingFilter != null ? readingFilter.filter(String.class, text) : text;
        }

        protected boolean isEmptyToNullReading() {
            return emptyToNullReadingDeterminer != null && emptyToNullReadingDeterminer.test(yourType);
        }

        @Override
        public void write(JsonWriter out, String value) throws IOException {
            if (value == null && isNullToEmptyWriting()) { // option
                out.value("");
            } else { // mainly here
                realAdapter.write(out, value);
            }
        }

        protected boolean isNullToEmptyWriting() {
            return nullToEmptyWritingDeterminer != null && nullToEmptyWritingDeterminer.test(yourType);
        }
    }

    // ===================================================================================
    //                                                                             Creator
    //                                                                             =======
    default TypeAdapterFactory createStringTypeAdapterFactory() {
        return TypeAdapters.newFactory(String.class, createTypeAdapterString());
    }

    default TypeAdapterString createTypeAdapterString() {
        return newTypeAdapterString(getGsonOption());
    }

    default TypeAdapterString newTypeAdapterString(JsonMappingOption gsonOption) {
        return new TypeAdapterString(gsonOption);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    JsonMappingOption getGsonOption();
}
