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
import java.util.function.Consumer;

import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfReflectionUtil;
import org.lastaflute.core.json.adapter.DBFluteGsonAdaptable;
import org.lastaflute.core.json.adapter.Java8TimeGsonAdaptable;
import org.lastaflute.core.json.adapter.NumberGsonAdaptable;
import org.lastaflute.core.json.adapter.StringGsonAdaptable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author jflute
 */
public class GsonJsonParser implements RealJsonParser // adapters here
        , DBFluteGsonAdaptable, StringGsonAdaptable, NumberGsonAdaptable, Java8TimeGsonAdaptable {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final GsonOption option;
    protected final Gson gson;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public GsonJsonParser(Consumer<GsonBuilder> oneArgLambda, Consumer<GsonOption> opLambda) {
        option = createOption(opLambda);
        gson = createGson(oneArgLambda);
    }

    protected GsonOption createOption(Consumer<GsonOption> opLambda) {
        final GsonOption option = new GsonOption();
        opLambda.accept(option);
        return option;
    }

    protected Gson createGson(Consumer<GsonBuilder> settings) {
        final GsonBuilder builder = newGsonBuilder();
        setupDefaultSettings(builder);
        setupYourSettings(builder);
        acceptGsonSettings(settings, builder);
        return builder.create();
    }

    protected GsonBuilder newGsonBuilder() {
        return new GsonBuilder();
    }

    // -----------------------------------------------------
    //                                      Default Settings
    //                                      ----------------
    protected void setupDefaultSettings(GsonBuilder builder) {
        registerDBFluteAdapter(builder);
        registerStringAdapter(builder);
        registerNumberAdapter(builder);
        registerJava8TimeAdapter(builder);
        registerUtilDateFormat(builder);
    }

    protected void registerDBFluteAdapter(GsonBuilder builder) {
        builder.registerTypeAdapterFactory(createClassificationTypeAdapterFactory());
    }

    protected void registerStringAdapter(GsonBuilder builder) {
        builder.registerTypeAdapterFactory(createStringTypeAdapterFactory());
    }

    protected void registerNumberAdapter(GsonBuilder builder) { // to show property path in exception message
        createNumberFactoryList().forEach(factory -> builder.registerTypeAdapterFactory(factory));
    }

    protected void registerJava8TimeAdapter(GsonBuilder builder) { // until supported by Gson
        builder.registerTypeAdapterFactory(createDateTimeTypeAdapterFactory());
    }

    protected void registerUtilDateFormat(GsonBuilder builder) {
        builder.setDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // same as local date-time
    }

    // -----------------------------------------------------
    //                                         Your Settings
    //                                         -------------
    protected void setupYourSettings(GsonBuilder builder) { // you can override
    }

    // -----------------------------------------------------
    //                                         Gson Settings
    //                                         -------------
    protected void acceptGsonSettings(Consumer<GsonBuilder> settings, GsonBuilder builder) {
        settings.accept(builder);
    }

    // ===================================================================================
    //                                                                      JSON Interface
    //                                                                      ==============
    @Override
    public <BEAN> BEAN fromJson(String json, Class<BEAN> beanType) { // are not null, already checked
        final BEAN bean = gson.fromJson(json, beanType); // if empty JSON, new-only instance
        return bean != null ? bean : newEmptyInstance(beanType);
    }

    @SuppressWarnings("unchecked")
    protected <BEAN> BEAN newEmptyInstance(Class<BEAN> beanType) {
        return (BEAN) DfReflectionUtil.newInstance(beanType);
    }

    @Override
    public <BEAN> List<BEAN> fromJsonList(String json, ParameterizedType elementType) { // are not null, already checked
        final List<BEAN> list = gson.fromJson(json, elementType); // if empty JSON, empty list
        return list != null ? Collections.unmodifiableList(list) : DfCollectionUtil.emptyList();
    }

    @Override
    public <BEAN> BEAN fromJsonParameteried(String json, ParameterizedType parameterizedType) {
        final BEAN bean = gson.fromJson(json, parameterizedType); // if empty JSON, new-only instance
        return bean != null ? bean : newEmptyInstance(parameterizedType);
    }

    @SuppressWarnings("unchecked")
    protected <BEAN> BEAN newEmptyInstance(ParameterizedType parameterizedType) {
        final Class<?> rawClass = DfReflectionUtil.getRawClass(parameterizedType);
        if (rawClass == null) {
            throw new IllegalStateException("Cannot get raw type from the parameterized type: " + parameterizedType);
        }
        return (BEAN) newEmptyInstance(rawClass);
    }

    @Override
    public String toJson(Object bean) { // is not null, already checked
        return gson.toJson(bean);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    @Override
    public GsonOption getGsonOption() {
        return option;
    }
}
