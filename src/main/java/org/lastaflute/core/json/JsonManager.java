/*
 * Copyright 2015-2018 the original author or authors.
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

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.json.control.JsonControlMeta;
import org.lastaflute.core.json.engine.RealJsonEngine;

/**
 * @author jflute
 */
public interface JsonManager {

    // ===================================================================================
    //                                                                        JSON Frmo/To
    //                                                                        ============
    /**
     * Convert from the JSON string to the specified bean.
     * @param <BEAN> The type of JSON bean.
     * @param json The string of JSON to be parsed. (NotNull, EmptyAllowed: returns new-only)
     * @param beanType The type of bean to convert, should have default constructor. (NotNull)
     * @return The new-created bean that has the JSON values. (NotNull: if empty JSON, new-only)
     */
    <BEAN> BEAN fromJson(String json, Class<BEAN> beanType);

    /**
     * Convert from the JSON string to the parameterized bean with typo.
     * You need to specify parameterized type like this:
     * <pre>
     * List&lt;SeaBean&gt; <span style="color: #553000">seaList</span> = fromJsonParameteried(<span style="color: #553000">json</span>, new ParameterizedRef<span style="color: #994747">&lt;List&lt;SeaBean&gt;&gt;</span>(){
     * }.getType());
     * 
     * MaihamaBean&lt;SeaBean&gt; maihama = fromJsonParameteried(<span style="color: #553000">json</span>, new ParameterizedRef<span style="color: #994747">&lt;MaihamaBean&lt;SeaBean&gt;&gt;</span>() {
     * }.getType());
     * </pre>
     * @param <BEAN> The type of JSON bean as root.
     * @param json The string of JSON to be parsed. (NotNull, EmptyAllowed: returns new-only)
     * @param parameterizedType The parameterized type of bean to convert, should have default constructor. (NotNull)
     * @return The new-created bean that has the JSON values, also List and Map. (NotNull: if empty JSON, new-only)
     */
    <BEAN> BEAN fromJsonParameteried(String json, ParameterizedType parameterizedType);

    /**
     * Convert from the source object to JSON string.
     * @param bean The instance of bean to encode. (NotNull)
     * @return The encoded JSON string. (NotNull)
     */
    String toJson(Object bean);

    // ===================================================================================
    //                                                                        JSON Control
    //                                                                        ============
    /**
     * Create new JSON engine as another rule. (old method, use the overload method) <br>
     * (inheriting basic settings e.g. serializeNulls, prettyPrinting)
     * @param mappingOption The optional option for new another engine. (NotNull, EmptyAllowed: when default options)
     * @return The JSON engine as another rule. (NotNull)
     * @deprecated use newRuledEngine()
     */
    RealJsonEngine newAnotherEngine(OptionalThing<JsonMappingOption> mappingOption);

    /**
     * Create new JSON engine as new rule. <br>
     * You can use your option of JSON mapping for the new engine. <br>
     * If no set here, it uses framework default rule (without assistant director rules). <br>
     * While, inheriting basic settings (e.g. serializeNulls, prettyPrinting) and engine creator.
     * <pre>
     * JsonMappingOption yourOption = new JsonMappingOption();
     * yourOption... // your rule settings
     * 
     * JsonEngineResource resource = new JsonEngineResource();
     * resource.acceptMappingOption(yourOption);
     * ... = newRuledEngine(resource);
     * </pre>
     * @param resource The resource of JSON engine to create instance. (NotNull)
     * @return The JSON engine as another rule. (NotNull)
     */
    RealJsonEngine newRuledEngine(JsonEngineResource resource);

    /**
     * Pull out the meta of JSON control that is embeeded in manager. (e.g. mapping option) 
     * @return The meta of JSON control as read-only. (NotNull)
     */
    JsonControlMeta pulloutControlMeta();
}