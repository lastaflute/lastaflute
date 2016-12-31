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
package org.lastaflute.core.json.adapter;

import java.io.IOException;
import java.util.function.Function;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.json.JsonMappingOption;
import org.lastaflute.core.json.filter.JsonSimpleTextReadingFilter;

import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * @author jflute
 */
public interface BooleanGsonAdaptable { // to show property path in exception message

    // ===================================================================================
    //                                                                        Type Adapter
    //                                                                        ============
    class TypeAdapterBoolean extends TypeAdapter<Boolean> {

        protected final JsonMappingOption option;
        protected final JsonSimpleTextReadingFilter readingFilter; // null allowed

        public TypeAdapterBoolean(JsonMappingOption option) {
            this.option = option;
            this.readingFilter = option.getSimpleTextReadingFilter().orElse(null); // cache, unwrap for performance
        }

        @Override
        public Boolean read(JsonReader in) throws IOException {
            final JsonToken token = in.peek();
            if (token == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            if (token == JsonToken.STRING) {
                final String exp = filterReading(in.nextString());
                if (isEmptyToNullReading() && "".equals(exp)) { // option
                    return null;
                } else {
                    return readAsBoolean(token, exp);
                }
            } else if (token == JsonToken.NUMBER) { // mainly here
                final int exp = in.nextInt();
                return readAsBoolean(token, exp);
            } else { // mainly here
                return in.nextBoolean();
            }
        }

        protected String filterReading(String text) {
            if (text == null) {
                return null;
            }
            return readingFilter != null ? readingFilter.filter(text) : text;
        }

        protected boolean isEmptyToNullReading() {
            return option.isEmptyToNullReading();
        }

        protected Boolean readAsBoolean(JsonToken token, Object exp) throws IOException {
            final OptionalThing<Function<Object, Boolean>> deserializer = option.getBooleanDeserializer();
            if (deserializer.isPresent()) { // cannot use lambda because of IOException
                return deserializer.get().apply(exp);
            } else {
                return Boolean.parseBoolean(exp.toString());
            }
        }

        @Override
        public void write(JsonWriter out, Boolean value) throws IOException {
            if (value == null) {
                if (isNullToEmptyWriting()) { // option
                    out.value("");
                } else {
                    out.nullValue();
                }
                return;
            }
            final OptionalThing<Object> filtered = filterBySerializerIfNeeds(value);
            if (isEverywhereQuoteWriting()) { // option
                out.value(filtered.orElse(value).toString()); // quoted 
            } else {
                if (filtered.isPresent()) { // cannot use lambda because of IOException
                    final Object exp = filtered.get();
                    if (exp instanceof Boolean) {
                        out.value((Boolean) exp);
                    } else if (exp instanceof Number) {
                        out.value((Number) exp);
                    } else {
                        out.value(exp.toString()); // also quoted
                    }
                } else {
                    out.value(value);
                }
            }
        }

        protected OptionalThing<Object> filterBySerializerIfNeeds(Boolean value) {
            return option.getBooleanSerializer().map(serializer -> {
                return serializer.apply(value);
            });
        }

        protected boolean isNullToEmptyWriting() {
            return option.isNullToEmptyWriting();
        }

        protected boolean isEverywhereQuoteWriting() {
            return option.isEverywhereQuoteWriting();
        }
    }

    // ===================================================================================
    //                                                                             Creator
    //                                                                             =======
    // -----------------------------------------------------
    //                                  Type Adapter Factory
    //                                  --------------------
    default TypeAdapterFactory createBooleanTypeAdapterFactory() {
        return TypeAdapters.newFactory(boolean.class, Boolean.class, createTypeAdapterBoolean());
    }

    // -----------------------------------------------------
    //                                          Type Adapter
    //                                          ------------
    default TypeAdapterBoolean createTypeAdapterBoolean() {
        return new TypeAdapterBoolean(getGsonOption());
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    JsonMappingOption getGsonOption();
}
