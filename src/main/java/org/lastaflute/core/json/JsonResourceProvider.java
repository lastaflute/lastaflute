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
package org.lastaflute.core.json;

import java.util.List;

import org.lastaflute.core.json.bind.JsonYourCollectionResource;
import org.lastaflute.core.json.engine.RealJsonEngine;

/**
 * The provider of JSON resource.
 * @author jflute
 */
public interface JsonResourceProvider {

    // ===================================================================================
    //                                                                       Gson Settings
    //                                                                       =============
    /**
     * Is null property suppressed (not displayed) in output JSON string?
     * @return The determination, true or false.
     */
    default boolean isNullsSuppressed() {
        return false; // display nulls as default
    }

    /**
     * Is pretty print property suppressed (not line separating) in output JSON string?
     * @return The determination, true or false.
     */
    default boolean isPrettyPrintSuppressed() {
        return false; // line separating if development.here as default
    }

    /**
     * Provide the option of JSON mapping, e.g. date format
     * @return The new-created option of JSON mapping. (NullAllowed: if null, use default)
     */
    default JsonMappingOption provideMappingOption() {
        return null;
    }

    /**
     * Provide the option of JSON mapping, e.g. date format
     * @return The new-created option of JSON mapping. (NullAllowed: if null, use default)
     * @deprecated use provideMappingOption()
     */
    default JsonMappingOption provideOption() {
        return null;
    }

    /**
     * Provide the your collections for JSON property. <br>
     * You can use e.g. ImmutableList (Eclipse Collections) as JSON property type.
     * @return The read-only list of your collection resource. (NullAllowed: if null, no your collection)
     * @deprecated use JsonMappingOption's yourCollections()
     */
    default List<JsonYourCollectionResource> provideYourCollections() {
        return null; // do nothing
    }

    // ===================================================================================
    //                                                                         Json Engine
    //                                                                         ===========
    /**
     * Switch to the engine of JSON for your parsing. <br>
     * Dangerous! Embedded Gson settings are disappeared.
     * @return The instance for real engine of JSON. (NullAllowed: if null, use default)
     */
    default RealJsonEngine swtichJsonEngine() {
        return null; // use default
    }
}
