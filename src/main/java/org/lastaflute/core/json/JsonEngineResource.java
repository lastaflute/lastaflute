/*
 * Copyright 2015-2022 the original author or authors.
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
 * @since 1.1.1 (2018/12/18 Tuesday at showbase)
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
    /**
     * Accept the option of JSON mapping. <br>
     * If no set here, it uses framework default rule (without assistant director rules).
     * @param mappingOption The option of JSON mapping. (NotNull)
     * @return this. (NotNull)
     */
    public JsonEngineResource acceptMappingOption(JsonMappingOption mappingOption) {
        if (mappingOption == null) {
            throw new IllegalArgumentException("The argument 'mappingOption' should not be null.");
        }
        this.mappingOption = mappingOption;
        return this;
    }

    /**
     * Use the creator of JSON engine instead of the existing creator (e.g. set by assistant). <br>
     * If no set here, it uses the existing creator (if it exists).
     * @param yourEngineCreator The your creator of JSON engine. (NotNull)
     * @return this. (NotNull)
     */
    public JsonEngineResource useYourEngineCreator(YourJsonEngineCreator yourEngineCreator) {
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
