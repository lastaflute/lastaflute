/*
 * Copyright 2015-2019 the original author or authors.
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

import org.lastaflute.core.json.engine.YourJsonEngineCreator;

/**
 * The provider of JSON resource.
 * @author jflute
 */
public interface JsonResourceProvider {

    // ===================================================================================
    //                                                                        Json Control
    //                                                                        ============
    // -----------------------------------------------------
    //                                        Mapping Option
    //                                        --------------
    /**
     * Provide the option of JSON mapping, e.g. date format
     * @return The new-created option of JSON mapping. (NullAllowed: if null, use default)
     */
    default JsonMappingOption provideMappingOption() {
        return null;
    }

    // -----------------------------------------------------
    //                                          Print Option
    //                                          ------------
    // not use option object for now because it's made after these methods...
    // (and not frequent options)
    /**
     * Is null property suppressed (not displayed) in output JSON string?
     * @return The determination, true or false.
     * @deprecated should be fixed control? or may aggregate as option object.
     */
    default boolean isNullsSuppressed() {
        return false; // display nulls as default
    }

    /**
     * Is pretty print property suppressed (not line separating) in output JSON string?
     * @return The determination, true or false.
     * @deprecated should be fixed control? or may aggregate as option object.
     */
    default boolean isPrettyPrintSuppressed() {
        return false; // line separating if development.here as default
    }

    // ===================================================================================
    //                                                                         Your Engine
    //                                                                         ===========
    /**
     * Create you original engine instance for Gson's JSON. <br>
     * Dangerous! This method may be changed future. Watch changes of LastaFlute.
     * @return The creator callback for your original engine instance. (NullAllowed: if null, use default)
     */
    default YourJsonEngineCreator prepareYourEngineCreator() {
        return null; // use default
    }
}
