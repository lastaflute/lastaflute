/*
 * Copyright 2014-2015 the original author or authors.
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
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.core.json.exception.JsonPropertyNumberParseFailureException;

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * @author jflute
 */
public interface NumberGsonAdaptable { // to show property path in exception message

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    TypeAdapter<Number> INTEGER = new TypeAdapterInteger();

    TypeAdapterFactory INTEGER_FACTORY = TypeAdapters.newFactory(int.class, Integer.class, INTEGER);

    TypeAdapter<Number> LONG = new TypeAdapterLong();

    TypeAdapterFactory LONG_FACTORY = TypeAdapters.newFactory(long.class, Long.class, LONG);

    TypeAdapter<BigDecimal> BIGDECIMAL = new TypeAdapterBigDecimal();

    TypeAdapterFactory BIGDECIMAL_FACTORY = TypeAdapters.newFactory(BigDecimal.class, BIGDECIMAL);

    List<TypeAdapterFactory> FACTORIES = Arrays.asList(INTEGER_FACTORY, LONG_FACTORY, BIGDECIMAL_FACTORY);

    // ===================================================================================
    //                                                                        Type Adapter
    //                                                                        ============
    abstract class AbstractTypeAdapterNumber<NUM extends Number> extends TypeAdapter<NUM> {

        @Override
        public NUM read(JsonReader in) throws IOException {
            try {
                return getRealAdapter().read(in);
            } catch (NumberFormatException e) {
                throwJsonPropertyNumberParseFailureException(in, e);
                return null; // unreachable
            } catch (JsonSyntaxException e) {
                throwJsonPropertyNumberParseFailureException(in, e);
                return null; // unreachable
            }
        }

        @Override
        public void write(JsonWriter out, NUM value) throws IOException {
            getRealAdapter().write(out, value);
        }

        protected void throwJsonPropertyNumberParseFailureException(JsonReader in, RuntimeException e) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Failed to parse number for the JSON property.");
            br.addItem("Advice");
            br.addElement("Confirm the next exception message.");
            br.addElement("And make sure your specified value for the JSON property.");
            br.addItem("Number Type");
            br.addElement(getNumberType().getName());
            br.addItem("JSON Property");
            br.addElement(in.getPath());
            final String msg = br.buildExceptionMessage();
            throw new JsonPropertyNumberParseFailureException(msg, e);
        }

        protected abstract TypeAdapter<NUM> getRealAdapter();

        protected abstract Class<?> getNumberType();
    }

    // -----------------------------------------------------
    //                                               Integer
    //                                               -------
    class TypeAdapterInteger extends AbstractTypeAdapterNumber<Number> {

        protected final TypeAdapter<Number> realAdapter = TypeAdapters.INTEGER;

        @Override
        protected TypeAdapter<Number> getRealAdapter() {
            return realAdapter;
        }

        @Override
        protected Class<?> getNumberType() {
            return Integer.class;
        }
    }

    // -----------------------------------------------------
    //                                                 Long
    //                                                ------
    class TypeAdapterLong extends AbstractTypeAdapterNumber<Number> {

        protected final TypeAdapter<Number> realAdapter = TypeAdapters.LONG;

        @Override
        protected TypeAdapter<Number> getRealAdapter() {
            return realAdapter;
        }

        @Override
        protected Class<?> getNumberType() {
            return Long.class;
        }
    }

    // -----------------------------------------------------
    //                                            BigDecimal
    //                                            ----------
    class TypeAdapterBigDecimal extends AbstractTypeAdapterNumber<BigDecimal> {

        protected final TypeAdapter<BigDecimal> realAdapter = TypeAdapters.BIG_DECIMAL;

        @Override
        protected TypeAdapter<BigDecimal> getRealAdapter() {
            return realAdapter;
        }

        @Override
        protected Class<?> getNumberType() {
            return BigDecimal.class;
        }
    }
}
