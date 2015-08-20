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
package org.lastaflute.web.callback;

import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author jflute
 * @since 0.6.0 (2015/07/05 Sunday at maihama)
 */
public class CrossOriginResourceSharing {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RequestManager requestManager;
    protected final String allowOrigin;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public CrossOriginResourceSharing(RequestManager requestManager, String allowOrigin) {
        assertArgumentNotNull("requestManager", requestManager);
        assertArgumentNotNull("allowOrigin", allowOrigin);
        this.requestManager = requestManager;
        this.allowOrigin = allowOrigin;
    }

    // ===================================================================================
    //                                                                               Share
    //                                                                               =====
    public ActionResponse share() {
        return isTargetRequest() ? createAllowResponse() : ActionResponse.undefined();
    }

    protected ActionResponse createAllowResponse() {
        final ActionResponse response = newAllowResponse();
        response.header("Access-Control-Allow-Origin", prepareAllowOrigin());
        response.header("Access-Control-Allow-Methods", prepareAllowMethods());
        response.header("Access-Control-Max-Age", String.valueOf(prepareMaxAge()));
        response.header("Access-Control-Allow-Headers", prepareAllowHeaders());
        response.header("Access-Control-Allow-Credentials", prepareAllowCredentials());
        return response;
    }

    protected ActionResponse newAllowResponse() {
        return JsonResponse.asEmptyBody();
    }

    protected boolean isTargetRequest() {
        return requestManager.getRequest().getMethod().equals("OPTIONS");
    }

    protected String prepareAllowOrigin() {
        return allowOrigin;
    }

    protected String prepareAllowMethods() {
        return "GET, POST, OPTIONS, DELETE, PUT";
    }

    protected int prepareMaxAge() { // seconds
        return 3600; // as default
    }

    protected String prepareAllowHeaders() {
        return "Origin, Content-Type, Accept, Authorization";
    }

    protected String prepareAllowCredentials() {
        return "true";
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            throw new IllegalArgumentException("The variableName should not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
        }
    }
}
