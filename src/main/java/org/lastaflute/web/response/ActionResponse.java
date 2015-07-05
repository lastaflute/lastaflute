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
package org.lastaflute.web.response;

import java.util.Map;

/**
 * The response type of action return. <br>
 * You can define the type as execute method of action
 * if you set RomanticActionCustomizer in your customizer.dicon.
 * @author jflute
 */
public interface ActionResponse {

    /**
     * @param name The name of header. (NotNull)
     * @param values The varying array for value of header. (NotNull)
     * @return this. (NotNull)
     * @throws IllegalStateException When the header already exists.
     */
    ActionResponse header(String name, String... values);

    /**
     * @return The read-only map for headers, map:{header-name = header-value}. (NotNull, NotNullValue)
     */
    Map<String, String[]> getHeaderMap();

    /**
     * Does it return the response as empty body? <br>
     * e.g. for when response already committed by other ways.
     * @return The determination, true or false.
     */
    boolean isReturnAsEmptyBody();

    /**
     * Is the response defined state? (handled as valid response)
     * @return The determination, true or false.
     */
    default boolean isDefined() {
        return !isUndefined();
    }

    /**
     * Is the response undefined state? (do nothing, to next step)
     * @return The determination, true or false.
     */
    boolean isUndefined();

    // ===================================================================================
    //                                                                  Undefined Instance
    //                                                                  ==================
    public static ActionResponse undefined() {
        return UndefinedResponse.instance();
    }
}
