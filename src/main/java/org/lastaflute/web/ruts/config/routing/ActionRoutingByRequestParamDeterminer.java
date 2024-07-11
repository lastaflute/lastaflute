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
package org.lastaflute.web.ruts.config.routing;

import java.util.function.Supplier;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.web.servlet.request.RequestManager;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author jflute
 * @since 1.0.1 (2017/10/16 Monday at miguel's el dorado)
 */
public class ActionRoutingByRequestParamDeterminer {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String mappingMethodName; // not null
    protected final OptionalThing<String> restfulHttpMethod; // not null, empty allowed
    protected final Supplier<RequestManager> requestManagerProvider; // not null

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionRoutingByRequestParamDeterminer(String mappingMethodName, OptionalThing<String> restfulHttpMethod,
            Supplier<RequestManager> requestManagerProvider) {
        this.mappingMethodName = mappingMethodName;
        this.restfulHttpMethod = restfulHttpMethod;
        this.requestManagerProvider = requestManagerProvider;
    }

    // ===================================================================================
    //                                                                           Determine
    //                                                                           =========
    public boolean determine(HttpServletRequest request) {
        // also needs to check HTTP method here by jflute (2021/11/18)
        // (while, conflict trouble is rare case if normal naming)
        if (restfulHttpMethod.filter(httpMethod -> { // copied from by-path-param determiner
            final RequestManager requestManager = requestManagerProvider.get();
            return !matchesWithRequestedHttpMethod(requestManager, httpMethod);
        }).isPresent()) {
            return false;
        }
        // basically for HTML form button request
        // e.g. name="sea" value="mystic" => ...?sea=mystic => mapping to sea()
        final String methodName = mappingMethodName;
        return !isParameterEmpty(request.getParameter(methodName)) // e.g. doUpdate=update
                || !isParameterEmpty(request.getParameter(methodName + ".x")) // e.g. doUpdate.x=update
                || !isParameterEmpty(request.getParameter(methodName + ".y")); // e.g. doUpdate.y=update
    }

    protected boolean matchesWithRequestedHttpMethod(RequestManager requestManager, String httpMethod) {
        return requestManager.isHttpMethod(httpMethod);
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected boolean isParameterEmpty(String str) {
        return str == null || str.isEmpty();
    }
}
