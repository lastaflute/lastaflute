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

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.jdbc.Classification;
import org.lastaflute.core.json.exception.JsonPropertyClassificationCodeOfMethodNotFoundException;
import org.lastaflute.core.json.exception.JsonPropertyUnknownClassificationCodeException;
import org.lastaflute.core.util.LaDBFluteUtil;
import org.lastaflute.core.util.LaDBFluteUtil.ClassificationCodeOfMethodNotFoundException;
import org.lastaflute.core.util.LaDBFluteUtil.ClassificationUnknownCodeException;

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
    class TypeAdapterClassifictory implements TypeAdapterFactory {

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            final Class<? super T> rawType = type.getRawType();
            if (rawType != null && LaDBFluteUtil.isClassificationType(rawType)) {
                @SuppressWarnings("unchecked")
                final TypeAdapter<T> pter = (TypeAdapter<T>) new TypeAdapterClassification(rawType);
                return pter;
            } else {
                return null;
            }
        }
    }

    class TypeAdapterClassification extends TypeAdapter<Classification> {

        protected final Class<?> rawType;

        public TypeAdapterClassification(Class<?> rawType) {
            this.rawType = rawType;
        }

        @Override
        public Classification read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            final String code = in.nextString();
            try {
                return LaDBFluteUtil.toVerifiedClassification(rawType, code);
            } catch (ClassificationCodeOfMethodNotFoundException e) {
                throwJsonPropertyClassificationCodeOfMethodNotFoundException(rawType, code, in, e);
                return null; // unreachable
            } catch (ClassificationUnknownCodeException e) {
                throwJsonPropertyUnknownClassificationCodeException(rawType, code, in, e);
                return null; // unreachable
            }
        }

        protected void throwJsonPropertyClassificationCodeOfMethodNotFoundException(Class<?> rawType, String code, JsonReader in,
                ClassificationCodeOfMethodNotFoundException e) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Not found the codeOf() in the classification type for the JSON property.");
            br.addItem("Advice");
            br.addElement("The classification type of JSON property should have the codeOf() method.");
            br.addElement("And you should use CDef type that always has the method.");
            br.addElement("For example:");
            br.addElement("  (x)");
            br.addElement("    public String memberName;");
            br.addElement("    public Classification memberStatus;    // *NG");
            br.addElement("  (o)");
            br.addElement("    public String memberName;");
            br.addElement("    public CDef.MemberStatus memberStatus; // OK");
            br.addItem("Classification");
            br.addElement(rawType);
            br.addItem("Specified Code");
            br.addElement(code);
            br.addItem("JSON Property");
            br.addElement(in.getPath());
            final String msg = br.buildExceptionMessage();
            throw new JsonPropertyClassificationCodeOfMethodNotFoundException(msg, e);
        }

        protected void throwJsonPropertyUnknownClassificationCodeException(Class<?> rawType, String code, JsonReader in,
                ClassificationUnknownCodeException e) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Unknown classification code for the JSON property.");
            br.addItem("Advice");
            br.addElement("Make sure your classification code in requested JSON.");
            br.addElement("And confirm the classification elements.");
            br.addItem("Classification");
            br.addElement(rawType);
            br.addItem("Unknown Code");
            br.addElement(code);
            br.addItem("JSON Property");
            br.addElement(in.getPath());
            final String msg = br.buildExceptionMessage();
            throw new JsonPropertyUnknownClassificationCodeException(msg, e);
        }

        @Override
        public void write(JsonWriter out, Classification value) throws IOException {
            out.value(value != null ? value.code() : null);
        }
    }

    // ===================================================================================
    //                                                                             Creator
    //                                                                             =======
    default TypeAdapterClassifictory createClassifictory() {
        return new TypeAdapterClassifictory();
    }
}
