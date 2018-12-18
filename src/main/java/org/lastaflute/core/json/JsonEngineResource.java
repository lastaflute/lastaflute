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

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.json.engine.YourJsonEngineCreator;

/**
 * @author jflute
 */
public class JsonEngineResource {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected JsonMappingOption mappingOption; // null allowed (not required)
    protected YourJsonEngineCreator yourEngineCreator; // null allowed (not required)

    // it does not contain print control for now,
    // you can customize it by ultimate customizer in mapping option

    // ===================================================================================
    //                                                                              Option
    //                                                                              ======
    public JsonEngineResource acceptMappingOption(JsonMappingOption mappingOption) {
        if (mappingOption == null) {
            throw new IllegalArgumentException("The argument 'mappingOption' should not be null.");
        }
        this.mappingOption = mappingOption;
        return this;
    }

    public JsonEngineResource overrideYourEngineCreator(YourJsonEngineCreator yourEngineCreator) {
        if (yourEngineCreator == null) {
            throw new IllegalArgumentException("The argument 'yourEngineCreator' should not be null.");
        }
        this.yourEngineCreator = yourEngineCreator;
        return this;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public OptionalThing<JsonMappingOption> getMappingOption() {
        return OptionalThing.ofNullable(mappingOption, () -> {
            throw new IllegalStateException("Not found the mapping option.");
        });
    }

    public OptionalThing<YourJsonEngineCreator> getYourEngineCreator() {
        return OptionalThing.ofNullable(yourEngineCreator, () -> {
            throw new IllegalStateException("Not found the your engine creator.");
        });
    }
}
