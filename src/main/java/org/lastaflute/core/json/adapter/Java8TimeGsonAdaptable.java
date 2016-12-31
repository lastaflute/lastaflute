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
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.core.json.JsonMappingOption;
import org.lastaflute.core.json.annotation.JsonDatePattern;
import org.lastaflute.core.json.exception.JsonPropertyDateTimeParseFailureException;
import org.lastaflute.core.json.filter.JsonSimpleTextReadingFilter;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.LaJsonFieldingAvailable;
import com.google.gson.internal.bind.LaJsonFieldingContext;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * @author jflute
 */
public interface Java8TimeGsonAdaptable {

    // ===================================================================================
    //                                                                        Type Adapter
    //                                                                        ============
    class DateTimeTypeAdapterFactory implements TypeAdapterFactory {

        protected final JsonMappingOption option;

        public DateTimeTypeAdapterFactory(JsonMappingOption option) {
            this.option = option;
        }

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            final Class<? super T> rawType = type.getRawType();
            if (rawType == null) {
                return null;
            }
            if (LocalDate.class.isAssignableFrom(rawType)) {
                @SuppressWarnings("unchecked")
                final TypeAdapter<T> pter = (TypeAdapter<T>) createTypeAdapterLocalDate();
                return pter;
            } else if (LocalDateTime.class.isAssignableFrom(rawType)) {
                @SuppressWarnings("unchecked")
                final TypeAdapter<T> pter = (TypeAdapter<T>) createTypeAdapterLocalDateTime();
                return pter;
            } else if (LocalTime.class.isAssignableFrom(rawType)) {
                @SuppressWarnings("unchecked")
                final TypeAdapter<T> pter = (TypeAdapter<T>) createTypeAdapterLocalTime();
                return pter;
            } else {
                return null;
            }
        }

        protected TypeAdapterLocalDate createTypeAdapterLocalDate() {
            return new TypeAdapterLocalDate(option);
        }

        protected TypeAdapterLocalDateTime createTypeAdapterLocalDateTime() {
            return new TypeAdapterLocalDateTime(option);
        }

        protected TypeAdapterLocalTime createTypeAdapterLocalTime() {
            return new TypeAdapterLocalTime(option);
        }
    }

    abstract class AbstractTypeDateTimeAdapter<DATE extends TemporalAccessor> extends TypeAdapter<DATE> implements LaJsonFieldingAvailable {

        protected final JsonMappingOption option;
        protected final JsonSimpleTextReadingFilter readingFilter; // null allowed

        public AbstractTypeDateTimeAdapter(JsonMappingOption option) {
            this.option = option;
            this.readingFilter = option.getSimpleTextReadingFilter().orElse(null); // cache, unwrap for performance
        }

        @Override
        public DATE read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            final String exp = filterReading(in.nextString());
            if (isEmptyToNullReading() && "".equals(exp)) { // option
                return null;
            }
            final DateTimeFormatter formatter = prepareDateTimeFormatter();
            try {
                return formatter.parse(exp, temporal -> fromTemporal(temporal));
            } catch (DateTimeParseException e) {
                throwJsonPropertyDateTimeParseFailureException(formatter, exp, in, e);
                return null; // unreachable
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

        @Override
        public void write(JsonWriter out, DATE value) throws IOException {
            if (isNullToEmptyWriting() && value == null) { // option
                out.value("");
            } else { // mainly here
                out.value(value != null ? prepareDateTimeFormatter().format(value) : null);
            }
        }

        protected boolean isNullToEmptyWriting() {
            return option.isNullToEmptyWriting();
        }

        private DateTimeFormatter prepareDateTimeFormatter() {
            DateTimeFormatter formatter = null;
            final Field field = LaJsonFieldingContext.getJsonFieldOnThread();
            if (field != null) { // no way but avoid stop
                final JsonDatePattern anno = field.getAnnotation(JsonDatePattern.class);
                if (anno != null) {
                    final String pattern = anno.value();
                    formatter = DateTimeFormatter.ofPattern(pattern); // #hope can be cached? by jflute
                }
            }
            if (formatter == null) {
                formatter = getDateTimeFormatter(); // not null
            }
            return formatter;
        }

        protected abstract DateTimeFormatter getDateTimeFormatter();

        protected abstract DATE fromTemporal(TemporalAccessor temporal);

        protected void throwJsonPropertyDateTimeParseFailureException(DateTimeFormatter formatter, String exp, JsonReader in,
                DateTimeParseException e) {
            final Class<?> dateType = getDateType();
            final String properthPath = in.getPath();
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Failed to parse date-time for the JSON property.");
            br.addItem("Advice");
            br.addElement("Confirm your date-time format for the JSON property.");
            br.addItem("Formatter");
            br.addElement(formatter);
            br.addItem("DateTime Type");
            br.addElement(dateType);
            br.addItem("Specified Expression");
            br.addElement(exp);
            br.addItem("JSON Property");
            br.addElement(properthPath);
            final String msg = br.buildExceptionMessage();
            throw new JsonPropertyDateTimeParseFailureException(msg, dateType, properthPath, e);
        }

        protected abstract Class<?> getDateType();
    }

    class TypeAdapterLocalDate extends AbstractTypeDateTimeAdapter<LocalDate> {

        public static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
        protected final DateTimeFormatter realFormatter;

        public TypeAdapterLocalDate(JsonMappingOption option) {
            super(option);
            realFormatter = option.getLocalDateFormatter().orElse(DEFAULT_FORMATTER);
        }

        @Override
        protected DateTimeFormatter getDateTimeFormatter() {
            return realFormatter;
        }

        @Override
        protected LocalDate fromTemporal(TemporalAccessor temporal) {
            return LocalDate.from(temporal);
        }

        @Override
        protected Class<?> getDateType() {
            return LocalDate.class;
        }
    }

    class TypeAdapterLocalDateTime extends AbstractTypeDateTimeAdapter<LocalDateTime> {

        public static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        protected final DateTimeFormatter realFormatter;

        public TypeAdapterLocalDateTime(JsonMappingOption option) {
            super(option);
            realFormatter = option.getLocalDateTimeFormatter().orElse(DEFAULT_FORMATTER);
        }

        @Override
        protected DateTimeFormatter getDateTimeFormatter() {
            return realFormatter;
        }

        @Override
        protected LocalDateTime fromTemporal(TemporalAccessor temporal) {
            return LocalDateTime.from(temporal);
        }

        @Override
        protected Class<?> getDateType() {
            return LocalDateTime.class;
        }
    }

    class TypeAdapterLocalTime extends AbstractTypeDateTimeAdapter<LocalTime> {

        public static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
        protected final DateTimeFormatter realFormatter;

        public TypeAdapterLocalTime(JsonMappingOption option) {
            super(option);
            realFormatter = option.getLocalTimeFormatter().orElse(DEFAULT_FORMATTER);
        }

        @Override
        protected DateTimeFormatter getDateTimeFormatter() {
            return realFormatter;
        }

        @Override
        protected LocalTime fromTemporal(TemporalAccessor temporal) {
            return LocalTime.from(temporal);
        }

        @Override
        protected Class<?> getDateType() {
            return LocalTime.class;
        }
    }

    // ===================================================================================
    //                                                                             Creator
    //                                                                             =======
    default DateTimeTypeAdapterFactory createDateTimeTypeAdapterFactory() {
        return new DateTimeTypeAdapterFactory(getGsonOption());
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    JsonMappingOption getGsonOption();
}
