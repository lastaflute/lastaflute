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

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.jdbc.Classification;
import org.lastaflute.core.json.JsonMappingOption;
import org.lastaflute.core.json.exception.JsonPropertyClassificationCodeOfMethodNotFoundException;
import org.lastaflute.core.json.exception.JsonPropertyClassificationCodeUnknownException;
import org.lastaflute.core.json.filter.JsonSimpleTextReadingFilter;
import org.lastaflute.core.util.LaClassificationUtil;
import org.lastaflute.core.util.LaClassificationUtil.ClassificationCodeOfMethodNotFoundException;
import org.lastaflute.core.util.LaClassificationUtil.ClassificationUnknownCodeException;

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
public interface DBFluteGsonAdaptable {

    // ===================================================================================
    //                                                                        Type Adapter
    //                                                                        ============
    class ClassificationTypeAdapterFactory implements TypeAdapterFactory {

        protected final JsonMappingOption option;

        public ClassificationTypeAdapterFactory(JsonMappingOption option) {
            this.option = option;
        }

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            final Class<? super T> rawType = type.getRawType();
            if (rawType != null && LaClassificationUtil.isCls(rawType)) {
                @SuppressWarnings("unchecked")
                final TypeAdapter<T> pter = (TypeAdapter<T>) createTypeAdapterClassification(rawType);
                return pter;
            } else {
                return null;
            }
        }

        protected TypeAdapterClassification createTypeAdapterClassification(Class<?> rawType) {
            return new TypeAdapterClassification(rawType, option);
        }
    }

    class TypeAdapterClassification extends TypeAdapter<Classification> {

        protected final Class<?> clsType;
        protected final JsonMappingOption option;
        protected final JsonSimpleTextReadingFilter readingFilter; // null allowed

        public TypeAdapterClassification(Class<?> clsType, JsonMappingOption option) {
            this.clsType = clsType;
            this.option = option;
            this.readingFilter = option.getSimpleTextReadingFilter().orElse(null); // cache, unwrap for performance
        }

        @Override
        public Classification read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            final String code = filterReading(in.nextString());
            if (isEmptyToNullReading() && "".equals(code)) { // option
                return null;
            }
            try {
                return LaClassificationUtil.toCls(clsType, code);
            } catch (ClassificationCodeOfMethodNotFoundException e) {
                throwJsonPropertyClassificationCodeOfMethodNotFoundException(code, in, e);
                return null; // unreachable
            } catch (ClassificationUnknownCodeException e) {
                throwJsonPropertyUnknownClassificationCodeException(code, in, e);
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
        public void write(JsonWriter out, Classification value) throws IOException {
            if (isNullToEmptyWriting() && value == null) { // option
                out.value("");
            } else { // mainly here
                out.value(value != null ? value.code() : null);
            }
        }

        protected boolean isNullToEmptyWriting() {
            return option.isNullToEmptyWriting();
        }

        protected void throwJsonPropertyClassificationCodeOfMethodNotFoundException(String code, JsonReader in,
                ClassificationCodeOfMethodNotFoundException e) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Not found the codeOf() in the classification type for the JSON property.");
            br.addItem("Advice");
            br.addElement("The classification type of JSON property should have the codeOf() method.");
            br.addElement("And you should use CDef type that always has the method.");
            br.addElement("For example:");
            br.addElement("  (x)");
            br.addElement("    public String memberName;");
            br.addElement("    public Classification memberStatus;    // *Bad");
            br.addElement("  (o)");
            br.addElement("    public String memberName;");
            br.addElement("    public CDef.MemberStatus memberStatus; // Good");
            br.addItem("Classification");
            br.addElement(clsType);
            br.addItem("Specified Code");
            br.addElement(code);
            br.addItem("JSON Property");
            br.addElement(in.getPath());
            final String msg = br.buildExceptionMessage();
            throw new JsonPropertyClassificationCodeOfMethodNotFoundException(msg, e);
        }

        protected void throwJsonPropertyUnknownClassificationCodeException(String code, JsonReader in,
                ClassificationUnknownCodeException e) {
            final String propertyPath = in.getPath();
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Unknown classification code for the JSON property.");
            br.addItem("Advice");
            br.addElement("Make sure your classification code in requested JSON.");
            br.addElement("And confirm the classification elements.");
            br.addItem("Classification");
            br.addElement(clsType);
            br.addItem("Unknown Code");
            br.addElement(code);
            br.addItem("JSON Property");
            br.addElement(propertyPath);
            final String msg = br.buildExceptionMessage();
            throw new JsonPropertyClassificationCodeUnknownException(msg, clsType, propertyPath, e);
        }
    }

    // ===================================================================================
    //                                                                             Creator
    //                                                                             =======
    default ClassificationTypeAdapterFactory createClassificationTypeAdapterFactory() {
        return new ClassificationTypeAdapterFactory(getGsonOption());
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    JsonMappingOption getGsonOption();
}
