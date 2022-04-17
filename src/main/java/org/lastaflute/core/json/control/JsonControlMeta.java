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
package org.lastaflute.core.json.control;

import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 * @since 1.1.1 (2018/12/18 Tuesday at bay maihama)
 */
public class JsonControlMeta { // read-only

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The control meta of JSON mapping. (NotNull, EmptyAllowed: if empty, use default) */
    protected final OptionalThing<JsonMappingControlMeta> mappingControlMeta;

    /** The print meta of JSON mapping. (NotNull, EmptyAllowed: if empty, use default) */
    protected final OptionalThing<JsonPrintControlMeta> printControlMeta;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public JsonControlMeta(OptionalThing<JsonMappingControlMeta> mappingControlMeta, OptionalThing<JsonPrintControlMeta> printControlMeta) {
        this.mappingControlMeta = mappingControlMeta;
        this.printControlMeta = printControlMeta;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    /**
     * @return The optional control meta of JSON mapping. (NotNull)
     */
    public OptionalThing<JsonMappingControlMeta> getMappingControlMeta() {
        return mappingControlMeta;
    }

    /**
     * @return The optional control meta of JSON print. (NotNull)
     */
    public OptionalThing<JsonPrintControlMeta> getPrintControlMeta() {
        return printControlMeta;
    }
}
