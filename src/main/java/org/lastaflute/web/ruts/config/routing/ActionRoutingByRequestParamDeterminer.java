/*
 * Copyright 2015-2021 the original author or authors.
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
package org.lastaflute.web.ruts.config.routing;

import javax.servlet.http.HttpServletRequest;

/**
 * @author jflute
 * @since 1.0.1 (2017/10/16 Monday at miguel's el dorado)
 */
public class ActionRoutingByRequestParamDeterminer {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String mappingMethodName; // not null

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionRoutingByRequestParamDeterminer(String mappingMethodName) {
        this.mappingMethodName = mappingMethodName;
    }

    // ===================================================================================
    //                                                                           Determine
    //                                                                           =========
    public boolean determine(HttpServletRequest request) {
        final String methodName = mappingMethodName;
        return !isParameterEmpty(request.getParameter(methodName)) // e.g. doUpdate=update
                || !isParameterEmpty(request.getParameter(methodName + ".x")) // e.g. doUpdate.x=update
                || !isParameterEmpty(request.getParameter(methodName + ".y")); // e.g. doUpdate.y=update
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected boolean isParameterEmpty(String str) {
        return str == null || str.isEmpty();
    }
}
