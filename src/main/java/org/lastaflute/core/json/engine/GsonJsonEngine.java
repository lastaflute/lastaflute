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
package org.lastaflute.core.json.engine;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfReflectionUtil;
import org.lastaflute.core.json.JsonMappingOption;
import org.lastaflute.core.json.JsonMappingOption.JsonFieldNaming;
import org.lastaflute.core.json.adapter.BooleanGsonAdaptable;
import org.lastaflute.core.json.adapter.CollectionGsonAdaptable;
import org.lastaflute.core.json.adapter.DBFluteGsonAdaptable;
import org.lastaflute.core.json.adapter.Java8TimeGsonAdaptable;
import org.lastaflute.core.json.adapter.NumberGsonAdaptable;
import org.lastaflute.core.json.adapter.StringGsonAdaptable;
import org.lastaflute.core.json.bind.JsonYourCollectionResource;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.bind.JsonAdapterAnnotationTypeAdapterFactory;
import com.google.gson.internal.bind.LaReflectiveTypeAdapterFactory;
import com.google.gson.internal.bind.LaYourCollectionTypeAdapterFactory;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;

/**
 * @author jflute
 */
public class GsonJsonEngine implements RealJsonEngine // adapters here
        , StringGsonAdaptable, NumberGsonAdaptable, Java8TimeGsonAdaptable, BooleanGsonAdaptable // basic
        , CollectionGsonAdaptable, DBFluteGsonAdaptable {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final JsonMappingOption option;
    protected final Gson gson;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public GsonJsonEngine(Consumer<GsonBuilder> oneArgLambda, Consumer<JsonMappingOption> opLambda) {
        option = createOption(opLambda); // should be before creating Gson
        gson = createGson(oneArgLambda); // using option variable
    }

    protected JsonMappingOption createOption(Consumer<JsonMappingOption> opLambda) {
        final JsonMappingOption option = new JsonMappingOption();
        opLambda.accept(option);
        return option;
    }

    protected Gson createGson(Consumer<GsonBuilder> settings) {
        final GsonBuilder builder = newGsonBuilder();
        setupDefaultSettings(builder);
        setupYourSettings(builder);
        acceptGsonSettings(settings, builder);
        final Gson newGson = builder.create();
        switchFactories(newGson);
        return newGson;
    }

    protected GsonBuilder newGsonBuilder() {
        return new GsonBuilder();
    }

    // -----------------------------------------------------
    //                                      Default Settings
    //                                      ----------------
    protected void setupDefaultSettings(GsonBuilder builder) {
        registerStringAdapter(builder);
        registerNumberAdapter(builder);
        registerJava8TimeAdapter(builder);
        registerBooleanAdapter(builder);
        registerCollectionAdapter(builder);
        registerDBFluteAdapter(builder);
        registerUtilDateFormat(builder);
        setupFieldPolicy(builder);
        setupYourCollectionSettings(builder);
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

    protected void registerBooleanAdapter(GsonBuilder builder) { // to adjust boolean expression flexibly
        builder.registerTypeAdapterFactory(createBooleanTypeAdapterFactory());
    }

    protected void registerCollectionAdapter(GsonBuilder builder) { // for option of list-null-to-empty
        builder.registerTypeAdapterFactory(createCollectionTypeAdapterFactory());
    }

    protected void registerDBFluteAdapter(GsonBuilder builder) {
        builder.registerTypeAdapterFactory(createClassificationTypeAdapterFactory());
    }

    protected void registerUtilDateFormat(GsonBuilder builder) {
        builder.setDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // same as local date-time
    }

    protected void setupFieldPolicy(GsonBuilder builder) {
        final JsonFieldNaming naming = option.getFieldNaming().orElse(getDefaultFieldNaming());
        builder.setFieldNamingPolicy(deriveFieldNamingPolicy(naming));
    }

    protected JsonFieldNaming getDefaultFieldNaming() {
        return JsonFieldNaming.IDENTITY; // as default
    }

    protected FieldNamingPolicy deriveFieldNamingPolicy(JsonFieldNaming naming) {
        final FieldNamingPolicy policy;
        if (naming == JsonFieldNaming.IDENTITY) {
            policy = FieldNamingPolicy.IDENTITY;
        } else if (naming == JsonFieldNaming.CAMEL_TO_LOWER_SNAKE) {
            policy = FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;
        } else {
            throw new IllegalStateException("Unknown field naming: " + naming);
        }
        return policy;
    }

    protected void setupYourCollectionSettings(GsonBuilder builder) {
        final List<JsonYourCollectionResource> yourCollections = option.getYourCollections();
        for (JsonYourCollectionResource resource : yourCollections) {
            builder.registerTypeAdapterFactory(createYourCollectionTypeAdapterFactory(resource));
        }
    }

    protected LaYourCollectionTypeAdapterFactory createYourCollectionTypeAdapterFactory(JsonYourCollectionResource resource) {
        return new LaYourCollectionTypeAdapterFactory(resource.getYourType(), resource.getYourCollectionCreator());
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

    // -----------------------------------------------------
    //                                      Dangerous Switch
    //                                      ----------------
    protected void switchFactories(Gson newGson) {
        final Field factoriesField = DfReflectionUtil.getWholeField(newGson.getClass(), "factories");
        @SuppressWarnings("unchecked")
        final List<Object> factories = (List<Object>) DfReflectionUtil.getValueForcedly(factoriesField, newGson);
        final List<Object> filtered = new ArrayList<Object>();
        for (Object factory : factories) {
            if (factory instanceof ReflectiveTypeAdapterFactory) { // switched, only one time
                filtered.add(createReflectiveTypeAdapterFactory(newGson, factory));
            } else {
                filtered.add(factory);
            }
        }
        DfReflectionUtil.setValueForcedly(factoriesField, newGson, Collections.unmodifiableList(filtered));
    }

    protected LaReflectiveTypeAdapterFactory createReflectiveTypeAdapterFactory(Gson newGson, Object factory) {
        final ConstructorConstructor constructorConstructor = getConstructorConstructor(factory);
        final JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory = getJsonAdapterFactory(factory);
        final FieldNamingStrategy fieldNamingStrategy = newGson.fieldNamingStrategy();
        final Excluder excluder = newGson.excluder();
        return new LaReflectiveTypeAdapterFactory(constructorConstructor, fieldNamingStrategy, excluder, jsonAdapterFactory);
    }

    protected ConstructorConstructor getConstructorConstructor(Object factory) {
        final Field field = DfReflectionUtil.getWholeField(factory.getClass(), "constructorConstructor");
        return (ConstructorConstructor) DfReflectionUtil.getValueForcedly(field, factory);
    }

    protected JsonAdapterAnnotationTypeAdapterFactory getJsonAdapterFactory(Object factory) {
        final Field field = DfReflectionUtil.getWholeField(factory.getClass(), "jsonAdapterFactory");
        return (JsonAdapterAnnotationTypeAdapterFactory) DfReflectionUtil.getValueForcedly(field, factory);
    }

    // ===================================================================================
    //                                                                      JSON Interface
    //                                                                      ==============
    @Override
    public <BEAN> BEAN fromJson(String json, Class<BEAN> beanType) { // are not null, already checked
        final BEAN bean = gson.fromJson(json, beanType); // if empty JSON, new-only instance
        return bean != null ? bean : newEmptyInstance(beanType);
    }

    @Override
    public <BEAN> BEAN fromJsonParameteried(String json, ParameterizedType parameterizedType) {
        final BEAN bean = gson.fromJson(json, parameterizedType); // if empty JSON, new-only instance
        if (bean != null) {
            return bean;
        } else { // e.g. empty string JSON
            final Class<?> rawClass = DfReflectionUtil.getRawClass(parameterizedType.getRawType()); // null allowed?
            if (List.class.equals(rawClass)) {
                @SuppressWarnings("unchecked")
                final BEAN emptyList = (BEAN) DfCollectionUtil.newArrayListSized(2); // empty but mutable for coherence
                return emptyList;
            } else if (Map.class.equals(rawClass)) {
                @SuppressWarnings("unchecked")
                final BEAN emptyList = (BEAN) DfCollectionUtil.newHashMapSized(2); // empty but mutable for coherence
                return emptyList;
            } else {
                return newEmptyInstance(parameterizedType);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected <BEAN> BEAN newEmptyInstance(ParameterizedType parameterizedType) {
        final Class<?> rawClass = DfReflectionUtil.getRawClass(parameterizedType);
        if (rawClass == null) {
            throw new IllegalStateException("Cannot get raw type from the parameterized type: " + parameterizedType);
        }
        return (BEAN) newEmptyInstance(rawClass);
    }

    @SuppressWarnings("unchecked")
    protected <BEAN> BEAN newEmptyInstance(Class<BEAN> beanType) {
        return (BEAN) DfReflectionUtil.newInstance(beanType);
    }

    @Override
    public String toJson(Object bean) { // is not null, already checked
        return gson.toJson(bean);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    @Override
    public JsonMappingOption getGsonOption() {
        return option;
    }
}
