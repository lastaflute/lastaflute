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

import org.lastaflute.core.direction.exception.FwRequiredAssistNotFoundException;

/**
 * @author jflute
 */
public class OptionalAssistDirection {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected String domainConfigFile;
    protected final List<String> extendsConfigFileList = new ArrayList<String>(4);

    // ===================================================================================
    //                                                                     Direct Property
    //                                                                     ===============
    public void directConfiguration(String domainConfigFile, String... extendsConfigFiles) {
        this.domainConfigFile = domainConfigFile;
        if (extendsConfigFiles != null && extendsConfigFiles.length > 0) {
            this.extendsConfigFileList.addAll(Arrays.asList(extendsConfigFiles));
        }
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String assistDomainConfigFile() {
        if (domainConfigFile == null) {
            String msg = "Not found the file for domain configuration.";
            throw new FwRequiredAssistNotFoundException(msg);
        }
        return domainConfigFile;
    }

    public List<String> assistExtendsConfigFileList() {
        return extendsConfigFileList;
    }
}
