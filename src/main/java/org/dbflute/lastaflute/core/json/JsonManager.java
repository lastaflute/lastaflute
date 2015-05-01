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
package org.dbflute.lastaflute.core.json;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author jflute
 */
public interface JsonManager {

    /**
     * Encode the source object to JSON string.
     * @param bean The instance of bean to encode. (NotNull)
     * @return The encoded JSON string. (NotNull)
     */
    String encode(Object bean);

    /**
     * Decode the JSON string to the specified bean.
     * @param <BEAN> The type of JSON bean.
     * @param json The string of JSON to be parsed. (NotNull)
     * @param beanType The type of bean to convert. (NotNull)
     * @return The new-created bean that has the JSON values. (NotNull)
     */
    <BEAN> BEAN decode(String json, Class<BEAN> beanType);

    /**
     * Decode the JSON string to the list of specified bean.
     * @param <BEAN> The type of JSON bean.
     * @param json The string of JSON to be parsed. (NotNull)
     * @param beanType The type of bean to convert. (NotNull)
     * @return The list of new-created bean that has the JSON values. (NotNull)
     */
    <BEAN> List<BEAN> decodeList(String json, Class<BEAN> beanType);

    /**
     * Mapping the JSON string to the specified bean instance. (decode)
     * @param <BEAN> The type of JSON bean.
     * @param json The string of JSON to be parsed. (NotNull)
     * @param beanSupplier The supplier of bean instance to be mapped. (NotNull)
     * @return The new-created bean that has the JSON values. (NotNull)
     */
    <BEAN> BEAN mappingJsonTo(String json, Supplier<BEAN> beanSupplier); // basically for framework

    /**
     * Mapping the JSON string to the specified bean instance. (decode)
     * @param <BEAN> The element type of JSON list.
     * @param json The string of JSON to be parsed. (NotNull)
     * @param beanSupplier The supplier of bean instance to be mapped. (NotNull)
     * @return The list of new-created bean that has the JSON values. (NotNull)
     */
    <BEAN> List<BEAN> mappingJsonToList(String json, Supplier<BEAN> beanSupplier); // basically for framework
}