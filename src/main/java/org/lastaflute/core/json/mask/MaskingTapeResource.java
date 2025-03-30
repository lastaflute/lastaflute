/*
 * Copyright 2015-2024 the original author or authors.
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
package org.lastaflute.core.json.mask;

import java.util.Set;

/**
 * @author jflute
 * @since 1.2.8 (2025/03/30 Sunday at nakameguro)
 */
public class MaskingTapeResource {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    public static final String DEFAULT_MASKING_STRING = "********"; // same as logging filter

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Set<String> maskParamSet; // not null e.g. password, pwd
    protected String maskingString = DEFAULT_MASKING_STRING; // not null e.g. ********

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public MaskingTapeResource(Set<String> maskParamSet) {
        if (maskParamSet == null) {
            throw new IllegalArgumentException("The argument 'maskParamSet' should not be null.");
        }
        this.maskParamSet = maskParamSet;
    }

    // -----------------------------------------------------
    //                                                Option
    //                                                ------
    public MaskingTapeResource switchMaskingString(String maskingString) {
        if (maskingString == null) {
            throw new IllegalArgumentException("The argument 'maskingString' should not be null.");
        }
        this.maskingString = maskingString;
        return this;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public Set<String> getMaskParamSet() {
        return maskParamSet;
    }

    public String getMaskingString() {
        return maskingString;
    }
}
