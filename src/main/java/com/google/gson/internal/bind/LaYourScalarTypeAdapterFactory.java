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
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * @param <SCALAR> The type of scalar for JSON proeprty.
 * @author jflute
 * @since 1.0.2 (2017/10/30 Monday at showbase)
 */
public class LaYourScalarTypeAdapterFactory<SCALAR> implements TypeAdapterFactory {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Class<SCALAR> yourType; // not null
    protected final Function<String, SCALAR> reader; // not null
    protected final Function<SCALAR, String> writer; // not null
    protected final Function<String, String> readingFilter; // null allowed
    protected final boolean emptyToNullReading;
    protected final boolean nullToEmptyWriting;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LaYourScalarTypeAdapterFactory(Class<SCALAR> yourType // scalar type
            , Function<String, SCALAR> reader, Function<SCALAR, String> writer // main function
            , Function<String, String> readingFilter // as basic option
            , boolean emptyToNullReading, boolean nullToEmptyWriting // us too
    ) {
        this.yourType = yourType;
        this.reader = reader;
        this.writer = writer;
        this.readingFilter = readingFilter;
        this.emptyToNullReading = emptyToNullReading;
        this.nullToEmptyWriting = nullToEmptyWriting;
    }

    // ===================================================================================
    //                                                                              Create
    //                                                                              ======
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        final Class<? super T> rawType = type.getRawType();
        if (rawType == null) {
            return null;
        }
        if (yourType.isAssignableFrom(rawType)) {
            @SuppressWarnings("unchecked")
            final TypeAdapter<T> pter = (TypeAdapter<T>) createYourScalarTypeAdapter();
            return pter;
        } else {
            return null;
        }
    }

    protected TypeAdapter<SCALAR> createYourScalarTypeAdapter() {
        return new Adapter<SCALAR>(reader, writer, readingFilter, emptyToNullReading, nullToEmptyWriting);
    }

    // ===================================================================================
    //                                                                        Type Adapter
    //                                                                        ============
    protected static class Adapter<SCALAR> extends TypeAdapter<SCALAR> implements LaJsonFieldingAvailable {

        protected final Function<String, SCALAR> reader; // not null
        protected final Function<SCALAR, String> writer; // not null
        protected final Function<String, String> readingFilter; // null allowed
        protected final boolean emptyToNullReading;
        protected final boolean nullToEmptyWriting;

        public Adapter(Function<String, SCALAR> reader, Function<SCALAR, String> writer, Function<String, String> readingFilter,
                boolean emptyToNullReading, boolean nullToEmptyWriting) {
            this.reader = reader;
            this.writer = writer;
            this.readingFilter = readingFilter; // cache, unwrap for performance
            this.emptyToNullReading = emptyToNullReading;
            this.nullToEmptyWriting = nullToEmptyWriting;
        }

        // -----------------------------------------------------
        //                                                 Read
        //                                                ------
        @Override
        public SCALAR read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            final String exp = filterReading(in.nextString());
            if (isEmptyToNullReading() && "".equals(exp)) { // option
                return null;
            }
            return reader.apply(exp);
        }

        protected String filterReading(String text) {
            if (text == null) {
                return null;
            }
            return readingFilter != null ? readingFilter.apply(text) : text;
        }

        protected boolean isEmptyToNullReading() {
            return emptyToNullReading;
        }

        // -----------------------------------------------------
        //                                                 Write
        //                                                 -----
        @Override
        public void write(JsonWriter out, SCALAR value) throws IOException {
            if (isNullToEmptyWriting() && value == null) { // option
                out.value("");
            } else { // mainly here
                writer.apply(value);
                out.value(value != null ? writer.apply(value) : null);
            }
        }

        protected boolean isNullToEmptyWriting() {
            return nullToEmptyWriting;
        }
    }
}
