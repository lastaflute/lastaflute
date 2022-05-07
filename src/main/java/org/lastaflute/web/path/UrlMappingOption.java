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

import java.util.function.Function;

import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 * @since 1.1.0 (2018/09/17 Monday)
 */
public class UrlMappingOption {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected Function<String, String> requestPathFilter; // null allowed
    protected String actionNameSuffix; // null allowed, basically initial upper case, e.g. if 'Sp', search as productListSpAction
    protected boolean restfulMapping; // if customized by RESTful router

    // ===================================================================================
    //                                                                              Facade
    //                                                                              ======
    /**
     * Set filter function to convert requestPath (makingMappingPath) to mappingPath. <br>
     * Returning null in the funtion means no filter. 
     * @param filter the function argument is requestPath (makingMappingPath). (NotNull, CanNullReturn: no filter)
     * @return this. (NotNull)
     */
    public UrlMappingOption filterRequestPath(Function<String, String> filter) {
        if (filter == null) {
            throw new IllegalArgumentException("The argument 'filter' should not be null.");
        }
        requestPathFilter = filter;
        return this;
    }

    public UrlMappingOption useActionNameSuffix(String suffix) {
        if (suffix == null) {
            throw new IllegalArgumentException("The argument 'suffix' should not be null.");
        }
        actionNameSuffix = suffix;
        return this;
    }

    public UrlMappingOption tellRestfulMapping() { // called by e.g. RESTful router
        restfulMapping = true;
        return this;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public OptionalThing<Function<String, String>> getRequestPathFilter() {
        return OptionalThing.ofNullable(requestPathFilter, () -> {
            throw new IllegalStateException("Not found the requestPathFilter.");
        });
    }

    public OptionalThing<String> getActionNameSuffix() {
        return OptionalThing.ofNullable(actionNameSuffix, () -> {
            throw new IllegalStateException("Not found the actionNameSuffix.");
        });
    }

    public boolean isRestfulMapping() {
        return restfulMapping;
    }
}
