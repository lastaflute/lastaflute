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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.core.json.exception.JsonPropertyDateTimeParseFailureException;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
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
    class TypeAdapterDateTimctory implements TypeAdapterFactory {

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
            return new TypeAdapterLocalDate();
        }

        protected TypeAdapterLocalDateTime createTypeAdapterLocalDateTime() {
            return new TypeAdapterLocalDateTime();
        }

        protected TypeAdapterLocalTime createTypeAdapterLocalTime() {
            return new TypeAdapterLocalTime();
        }
    }

    abstract class AbstractTypeDateTimeAdapter<DATE extends TemporalAccessor> extends TypeAdapter<DATE> {

        @Override
        public DATE read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            final String code = in.nextString();
            final DateTimeFormatter formatter = getDateTimeFormatter();
            try {
                return formatter.parse(code, temporal -> fromTemporal(temporal));
            } catch (DateTimeParseException e) {
                throwJsonPropertyDateTimeParseFailureException(formatter, getRawType(), code, in, e);
                return null; // unreachable
            }
        }

        @Override
        public void write(JsonWriter out, DATE value) throws IOException {
            out.value(value != null ? getDateTimeFormatter().format(value) : null);
        }

        protected abstract DateTimeFormatter getDateTimeFormatter();

        protected abstract DATE fromTemporal(TemporalAccessor temporal);

        protected abstract Class<?> getRawType();

        protected void throwJsonPropertyDateTimeParseFailureException(DateTimeFormatter formatter, Class<?> rawType, String exp,
                JsonReader in, DateTimeParseException e) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Failed to parse date-time for the JSON property.");
            br.addItem("Advice");
            br.addElement("Confirm your date-time format for the JSON property.");
            br.addItem("Formatter");
            br.addElement(formatter);
            br.addItem("DateTime Type");
            br.addElement(rawType);
            br.addItem("Specified Expression");
            br.addElement(exp);
            br.addItem("JSON Property");
            br.addElement(in.getPath());
            final String msg = br.buildExceptionMessage();
            throw new JsonPropertyDateTimeParseFailureException(msg, e);
        }
    }

    class TypeAdapterLocalDate extends AbstractTypeDateTimeAdapter<LocalDate> {

        public static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        @Override
        protected DateTimeFormatter getDateTimeFormatter() {
            return formatter;
        }

        @Override
        protected LocalDate fromTemporal(TemporalAccessor temporal) {
            return LocalDate.from(temporal);
        }

        @Override
        protected Class<?> getRawType() {
            return LocalDate.class;
        }
    }

    class TypeAdapterLocalDateTime extends AbstractTypeDateTimeAdapter<LocalDateTime> {

        public static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        protected DateTimeFormatter getDateTimeFormatter() {
            return formatter;
        }

        @Override
        protected LocalDateTime fromTemporal(TemporalAccessor temporal) {
            return LocalDateTime.from(temporal);
        }

        @Override
        protected Class<?> getRawType() {
            return LocalDateTime.class;
        }
    }

    class TypeAdapterLocalTime extends AbstractTypeDateTimeAdapter<LocalTime> {

        public static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_TIME;

        @Override
        protected DateTimeFormatter getDateTimeFormatter() {
            return formatter;
        }

        @Override
        protected LocalTime fromTemporal(TemporalAccessor temporal) {
            return LocalTime.from(temporal);
        }

        @Override
        protected Class<?> getRawType() {
            return LocalTime.class;
        }
    }

    // ===================================================================================
    //                                                                             Creator
    //                                                                             =======
    default TypeAdapterDateTimctory createDateTimctory() {
        return new TypeAdapterDateTimctory();
    }
}
