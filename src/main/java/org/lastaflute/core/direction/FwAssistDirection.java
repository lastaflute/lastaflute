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
package org.lastaflute.core.direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.lastaflute.core.direction.exception.FwRequiredAssistNotFoundException;

/**
 * @author jflute
 */
public class FwAssistDirection {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected String appConfig;
    protected final List<String> extendsConfigList = new ArrayList<String>(4);

    // ===================================================================================
    //                                                                     Direct Property
    //                                                                     ===============
    public void directConfig(Consumer<List<String>> appSetupper, String... commonNames) {
        final List<String> nameList = new ArrayList<String>(4);
        appSetupper.accept(nameList);
        nameList.addAll(Arrays.asList(commonNames));
        appConfig = nameList.remove(0);
        extendsConfigList.addAll(nameList);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String assistAppConfig() {
        assertAssistObjectNotNull(appConfig, "Not found the file for domain configuration.");
        return appConfig;
    }

    public List<String> assistExtendsConfigList() {
        return extendsConfigList; // empty allowed but almost exists
    }

    // -----------------------------------------------------
    //                                         Assert Helper
    //                                         -------------
    protected void assertAssistObjectNotNull(Object obj, String msg) {
        if (obj == null) {
            throw new FwRequiredAssistNotFoundException(msg);
        }
    }
}
