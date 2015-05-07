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
import java.util.List;

/**
 * @author jflute
 */
public interface JsonManager {

    /**
     * Convert from the JSON string to the specified bean.
     * @param <BEAN> The type of JSON bean.
     * @param json The string of JSON to be parsed. (NotNull)
     * @param beanType The type of bean to convert, should have default constructor. (NotNull, EmptyAllowed: returns new-only)
     * @return The new-created bean that has the JSON values. (NotNull: if empty JSON, new-only)
     */
    <BEAN> BEAN fromJson(String json, Class<BEAN> beanType);

    /**
     * Convert from the JSON string to the list of specified bean. <br>
     * You need to specify parameterized type for element type.
     * <pre>
     * ... = fromJsonList(json, new ParameterizedRef&lt;List&lt;SeaBean&gt;&gt;(){}.getType());
     * </pre>
     * @param <ELEMENT> The element type of JSON list.
     * @param json The string of JSON to be parsed. (NotNull)
     * @param elementType The type of bean to convert, should have default constructor. (NotNull, EmptyAllowed: returns empty list)
     * @return The read-only list of new-created bean that has the JSON values. (NotNull, EmptyAllowed: if empty JSON)
     */
    <ELEMENT> List<ELEMENT> fromJsonList(String json, ParameterizedType elementType);

    /**
     * Convert from the source object to JSON string.
     * @param bean The instance of bean to encode. (NotNull)
     * @return The encoded JSON string. (NotNull)
     */
    String toJson(Object bean);
}