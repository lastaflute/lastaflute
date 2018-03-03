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
package org.lastaflute.db.dbflute.accesscontext;

import java.lang.reflect.Method;

/**
 * @author jflute
 */
public class AccessContextResource {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The name of access module. (NotNull) */
    protected final String moduleName;

    /** The method object of access process. (NullAllowed: means unknown) */
    protected final Method method;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    /**
     * Constructor.
     * @param moduleName The name of access module. (NotNull)
     * @param method The method object of access process. (NullAllowed: means unknown)
     */
    public AccessContextResource(String moduleName, Method method) {
        this.moduleName = moduleName;
        this.method = method;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    /**
     * Get the name of access module.
     * @return The name of access module. (NotNull)
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Get the method object of access process.
     * @return The method object of access process. (NullAllowed: means unknown)
     */
    public Method getMethod() {
        return method;
    }
}
