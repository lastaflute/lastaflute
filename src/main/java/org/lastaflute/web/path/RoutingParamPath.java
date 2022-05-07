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

import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 * @since 1.2.2 (2021/08/21 Saturday at roppongi japanese)
 */
public class RoutingParamPath {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    public static final RoutingParamPath EMPTY = new RoutingParamPath("") {
        @Override
        public void acceptMappingParamPath(String mappingParamPath) { // for immutable
            throw new IllegalStateException("empty instance so cannot accept it: " + mappingParamPath);
        }
    };

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String requestParamPath; // not null
    protected String mappingParamPath; // null allowed, be set if needed e.g. RESTish Event

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public RoutingParamPath(String requestParamPath) {
        this.requestParamPath = requestParamPath;
    }

    // ===================================================================================
    //                                                                             Request
    //                                                                             =======
    public boolean isEmpty() {
        return requestParamPath.isEmpty();
    }

    // ===================================================================================
    //                                                                             Mapping
    //                                                                             =======
    public void acceptMappingParamPath(String mappingParamPath) {
        if (mappingParamPath == null) {
            throw new IllegalArgumentException("The argument 'mappingParamPath' should not be null.");
        }
        this.mappingParamPath = mappingParamPath;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (mappingParamPath == null) { // mainly here
            sb.append(requestParamPath);
        } else { // with mapping
            sb.append("request=").append(requestParamPath);
            sb.append(", mapping=").append(mappingParamPath);
        }
        sb.append("}");
        return sb.toString();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getRequestParamPath() {
        return requestParamPath;
    }

    public OptionalThing<String> getMappingParamPath() {
        return OptionalThing.ofNullable(mappingParamPath, () -> {
            throw new IllegalStateException("Not found the mappingParamPath for requestParamPath: " + requestParamPath);
        });
    }
}
