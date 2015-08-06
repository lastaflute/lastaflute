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

import org.lastaflute.core.json.GsonOption;

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
        protected final GsonOption option;

        public TypeAdapterString(GsonOption option) {
            this.option = option;
        }

        @Override
        public String read(JsonReader in) throws IOException {
            return realAdapter.read(in);
        }

        @Override
        public void write(JsonWriter out, String value) throws IOException {
            if (value == null && isNullToEmptyWriting()) {
                out.value("");
            } else {
                realAdapter.write(out, value);
            }
        }

        protected boolean isNullToEmptyWriting() {
            return option.isNullToEmptyWriting();
        }
    }

    // ===================================================================================
    //                                                                             Creator
    //                                                                             =======
    default TypeAdapterFactory createStringTypeAdapterFactory() {
        return TypeAdapters.newFactory(String.class, createTypeAdapterString());
    }

    default TypeAdapterString createTypeAdapterString() {
        return new TypeAdapterString(getGsonOption());
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    GsonOption getGsonOption();
}
