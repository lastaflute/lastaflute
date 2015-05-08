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
package org.lastaflute.core.json;

import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.List;

import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfReflectionUtil;

import com.google.gson.Gson;

/**
 * @author jflute
 */
public class GsonJsonParser implements RealJsonParser {

    protected final Gson gson;

    public GsonJsonParser(Gson gson) {
        this.gson = gson;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <BEAN> BEAN fromJson(String json, Class<BEAN> beanType) { // are not null, already checked
        final BEAN bean = gson.fromJson(json, beanType); // if empty JSON, new-only instance
        return bean != null ? bean : (BEAN) DfReflectionUtil.newInstance(beanType);
    }

    @Override
    public <BEAN> List<BEAN> fromJsonList(String json, ParameterizedType elementType) { // are not null, already checked
        final List<BEAN> list = gson.fromJson(json, elementType); // if empty JSON, empty list
        return list != null ? Collections.unmodifiableList(list) : DfCollectionUtil.emptyList();
    }

    @Override
    public String toJson(Object bean) { // is not null, already checked
        return gson.toJson(bean);
    }
}
