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
package org.lastaflute.web.path;

import org.dbflute.utflute.core.PlainTestCase;

/**
 * @author jflute
 * @since 1.2.6 (2024/05/19 Sunday at ichihara)
 */
public class FormMappingOptionTest extends PlainTestCase {

    // ===================================================================================
    //                                                                 Undefined Parameter
    //                                                                 ===================
    public void test_undefinedParameter_basic() {
        assertFalse(new FormMappingOption().isUndefinedParameterError());
        assertFalse(new FormMappingOption().isUndefinedParameterWarning());

        assertTrue(new FormMappingOption().asUndefinedParameterError().isUndefinedParameterError());
        assertFalse(new FormMappingOption().asUndefinedParameterError().isUndefinedParameterWarning());

        assertFalse(new FormMappingOption().asUndefinedParameterWarning().isUndefinedParameterError());
        assertTrue(new FormMappingOption().asUndefinedParameterWarning().isUndefinedParameterWarning());

        {
            FormMappingOption option = new FormMappingOption().asUndefinedParameterError().asUndefinedParameterWarning();
            assertFalse(option.isUndefinedParameterError());
            assertTrue(option.isUndefinedParameterWarning());
        }
        {
            FormMappingOption option = new FormMappingOption().asUndefinedParameterWarning().asUndefinedParameterError();
            assertTrue(option.isUndefinedParameterError());
            assertFalse(option.isUndefinedParameterWarning());
        }
    }
}
