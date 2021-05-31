/*
 * Copyright 2015-2021 the original author or authors.
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
import java.util.function.Predicate;

import org.lastaflute.core.json.filter.JsonUnifiedTextReadingFilter;

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
    protected final JsonUnifiedTextReadingFilter readingFilter; // null allowed
    protected final Predicate<Class<?>> emptyToNullReadingDeterminer; // null allowed
    protected final Predicate<Class<?>> nullToEmptyWritingDeterminer; // null allowed

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LaYourScalarTypeAdapterFactory(Class<SCALAR> yourType // scalar type
            , Function<String, SCALAR> reader, Function<SCALAR, String> writer // main function
            , JsonUnifiedTextReadingFilter readingFilter // as basic option
            , Predicate<Class<?>> emptyToNullReadingDeterminer // me too
            , Predicate<Class<?>> nullToEmptyWritingDeterminer // me too
    ) {
        this.yourType = yourType;
        this.reader = reader;
        this.writer = writer;
        this.readingFilter = readingFilter;
        this.emptyToNullReadingDeterminer = emptyToNullReadingDeterminer;
        this.nullToEmptyWritingDeterminer = nullToEmptyWritingDeterminer;
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
        return new Adapter<SCALAR>(yourType, reader, writer, readingFilter, emptyToNullReadingDeterminer, nullToEmptyWritingDeterminer);
    }

    // ===================================================================================
    //                                                                        Type Adapter
    //                                                                        ============
    protected static class Adapter<SCALAR> extends TypeAdapter<SCALAR> implements LaJsonFieldingAvailable {

        protected final Class<SCALAR> yourType; // not null
        protected final Function<String, SCALAR> reader; // not null
        protected final Function<SCALAR, String> writer; // not null
        protected final JsonUnifiedTextReadingFilter readingFilter; // null allowed
        protected final Predicate<Class<?>> emptyToNullReadingDeterminer; // null allowed
        protected final Predicate<Class<?>> nullToEmptyWritingDeterminer; // null allowed

        public Adapter(Class<SCALAR> yourType, Function<String, SCALAR> reader, Function<SCALAR, String> writer,
                JsonUnifiedTextReadingFilter readingFilter, Predicate<Class<?>> emptyToNullReadingDeterminer,
                Predicate<Class<?>> nullToEmptyWritingDeterminer) {
            this.yourType = yourType;
            this.reader = reader;
            this.writer = writer;
            this.readingFilter = readingFilter; // cache, unwrap for performance
            this.emptyToNullReadingDeterminer = emptyToNullReadingDeterminer; // me too
            this.nullToEmptyWritingDeterminer = nullToEmptyWritingDeterminer; // me too
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
            if ("".equals(exp) && isEmptyToNullReading()) { // option
                return null;
            }
            return reader.apply(exp);
        }

        protected String filterReading(String text) {
            if (text == null) {
                return null;
            }
            return readingFilter != null ? readingFilter.filter(yourType, text) : text;
        }

        protected boolean isEmptyToNullReading() {
            return emptyToNullReadingDeterminer != null && emptyToNullReadingDeterminer.test(yourType);
        }

        // -----------------------------------------------------
        //                                                 Write
        //                                                 -----
        @Override
        public void write(JsonWriter out, SCALAR value) throws IOException {
            if (value == null && isNullToEmptyWriting()) { // option
                out.value("");
            } else { // mainly here
                writer.apply(value);
                out.value(value != null ? writer.apply(value) : null);
            }
        }

        protected boolean isNullToEmptyWriting() {
            return nullToEmptyWritingDeterminer != null && nullToEmptyWritingDeterminer.test(yourType);
        }
    }
}
