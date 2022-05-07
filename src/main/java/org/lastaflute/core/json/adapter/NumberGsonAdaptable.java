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
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.json.JsonMappingOption;
import org.lastaflute.core.json.exception.JsonPropertyNumberParseFailureException;
import org.lastaflute.core.json.filter.JsonUnifiedTextReadingFilter;

import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * @author jflute
 */
public interface NumberGsonAdaptable { // to show property path in exception message

    // ===================================================================================
    //                                                                        Type Adapter
    //                                                                        ============
    abstract class AbstractTypeAdapterNumber<NUM extends Number> extends TypeAdapter<NUM> {

        protected final JsonMappingOption gsonOption;
        protected final JsonUnifiedTextReadingFilter readingFilter; // null allowed
        protected final Predicate<Class<?>> emptyToNullReadingDeterminer; // null allowed
        protected final Predicate<Class<?>> nullToEmptyWritingDeterminer; // null allowed

        public AbstractTypeAdapterNumber(JsonMappingOption gsonOption) {
            this.gsonOption = gsonOption;
            this.readingFilter = JsonUnifiedTextReadingFilter.unify(gsonOption); // cache as plain for performance
            this.emptyToNullReadingDeterminer = gsonOption.getEmptyToNullReadingDeterminer().orElse(null); // me too
            this.nullToEmptyWritingDeterminer = gsonOption.getNullToEmptyWritingDeterminer().orElse(null); // me too
        }

        @Override
        public NUM read(JsonReader in) throws IOException { // not use real adapter for options
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            final String str = filterReading(in.nextString());
            if (str == null) { // filter makes it null
                return null;
            }
            if ("".equals(str) && isEmptyToNullReading()) {
                return null;
            }
            try {
                if (str != null && str.trim().isEmpty()) { // e.g. "" or " "
                    // toNumber() treats empty as null so throw to keep Gson behavior 
                    throw new NumberFormatException("because of empty string: [" + str + "]");
                }
                @SuppressWarnings("unchecked")
                final NUM num = (NUM) DfTypeUtil.toNumber(str, getNumberType());
                return num;
            } catch (RuntimeException e) {
                throwJsonPropertyNumberParseFailureException(in, e);
                return null; // unreachable
            }
        }

        protected String filterReading(String text) {
            if (text == null) {
                return null;
            }
            return readingFilter != null ? readingFilter.filter(getNumberType(), text) : text;
        }

        protected boolean isEmptyToNullReading() {
            return emptyToNullReadingDeterminer != null && emptyToNullReadingDeterminer.test(getNumberType());
        }

        @Override
        public void write(JsonWriter out, NUM value) throws IOException {
            if (value == null && isNullToEmptyWriting()) { // option
                out.value("");
            } else {
                if (isEverywhereQuoteWriting()) { // option
                    out.value(value.toString()); // quoted
                } else { // mainly here
                    getRealAdapter().write(out, value);
                }
            }
        }

        protected boolean isNullToEmptyWriting() {
            return nullToEmptyWritingDeterminer != null && nullToEmptyWritingDeterminer.test(getNumberType());
        }

        protected boolean isEverywhereQuoteWriting() {
            return gsonOption.isEverywhereQuoteWriting();
        }

        protected abstract TypeAdapter<NUM> getRealAdapter();

        protected void throwJsonPropertyNumberParseFailureException(JsonReader in, RuntimeException e) {
            final Class<?> numberType = getNumberType();
            final String properthPath = in.getPath();
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Failed to parse number for the JSON property.");
            br.addItem("Advice");
            br.addElement("Confirm the next exception message.");
            br.addElement("And make sure your specified value for the JSON property.");
            br.addItem("Number Type");
            br.addElement(numberType.getName());
            br.addItem("JSON Property");
            br.addElement(properthPath);
            final String msg = br.buildExceptionMessage();
            throw new JsonPropertyNumberParseFailureException(msg, numberType, properthPath, e);
        }

        protected abstract Class<?> getNumberType();
    }

    // -----------------------------------------------------
    //                                               Integer
    //                                               -------
    class TypeAdapterInteger extends AbstractTypeAdapterNumber<Number> {

        protected final TypeAdapter<Number> realAdapter = TypeAdapters.INTEGER;

        public TypeAdapterInteger(JsonMappingOption gsonOption) {
            super(gsonOption);
        }

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

        public TypeAdapterLong(JsonMappingOption gsonOption) {
            super(gsonOption);
        }

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

        public TypeAdapterBigDecimal(JsonMappingOption gsonOption) {
            super(gsonOption);
        }

        @Override
        protected TypeAdapter<BigDecimal> getRealAdapter() {
            return realAdapter;
        }

        @Override
        protected Class<?> getNumberType() {
            return BigDecimal.class;
        }
    }

    // ===================================================================================
    //                                                                             Creator
    //                                                                             =======
    default List<TypeAdapterFactory> createNumberFactoryList() {
        return Arrays.asList(createIntegerTypeAdapterFactory() // Integer
                , createLongTypeAdapterFactory() // Long
                , createBigDecimalTypeAdapterFactory() // BigDecimal
        );
    }

    // -----------------------------------------------------
    //                                  Type Adapter Factory
    //                                  --------------------
    default TypeAdapterFactory createIntegerTypeAdapterFactory() {
        return TypeAdapters.newFactory(int.class, Integer.class, createTypeAdapterInteger());
    }

    default TypeAdapterFactory createLongTypeAdapterFactory() {
        return TypeAdapters.newFactory(long.class, Long.class, createTypeAdapterLong());
    }

    default TypeAdapterFactory createBigDecimalTypeAdapterFactory() {
        return TypeAdapters.newFactory(BigDecimal.class, createTypeAdapterBigDecimal());
    }

    // -----------------------------------------------------
    //                                          Type Adapter
    //                                          ------------
    // Integer
    default TypeAdapterInteger createTypeAdapterInteger() {
        return newTypeAdapterInteger(getGsonOption());
    }

    default TypeAdapterInteger newTypeAdapterInteger(JsonMappingOption gsonOption) {
        return new TypeAdapterInteger(gsonOption);
    }

    // Long
    default TypeAdapterLong createTypeAdapterLong() {
        return newTypeAdapterLong(getGsonOption());
    }

    default TypeAdapterLong newTypeAdapterLong(JsonMappingOption gsonOption) {
        return new TypeAdapterLong(gsonOption);
    }

    // BigDecimal
    default TypeAdapterBigDecimal createTypeAdapterBigDecimal() {
        return newTypeAdapterBigDecimal(getGsonOption());
    }

    default TypeAdapterBigDecimal newTypeAdapterBigDecimal(JsonMappingOption gsonOption) {
        return new TypeAdapterBigDecimal(gsonOption);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    JsonMappingOption getGsonOption();
}
