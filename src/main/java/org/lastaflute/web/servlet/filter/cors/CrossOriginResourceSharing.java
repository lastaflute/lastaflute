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
package org.lastaflute.web.servlet.filter.cors;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author jflute
 * @since 0.6.0 (2015/07/05 Sunday at maihama)
 */
public class CrossOriginResourceSharing {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String allowOrigin;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public CrossOriginResourceSharing(String allowOrigin) {
        assertArgumentNotNull("allowOrigin", allowOrigin);
        this.allowOrigin = allowOrigin;
    }

    // ===================================================================================
    //                                                                               Share
    //                                                                               =====
    public boolean share(HttpServletRequest request, HttpServletResponse response) {
        setupAllowHeader(response);
        return isOptionsRequest(request);
    }

    protected boolean isOptionsRequest(HttpServletRequest request) {
        return request.getMethod().equals("OPTIONS");
    }

    protected void setupAllowHeader(HttpServletResponse response) {
        prepareAllowHeaderMap().forEach((key, value) -> response.addHeader(key, value));
    }

    // -----------------------------------------------------
    //                                          Allow Header
    //                                          ------------
    protected Map<String, String> prepareAllowHeaderMap() {
        final Map<String, String> headerMap = new HashMap<String, String>(5);
        headerMap.put("Access-Control-Allow-Origin", prepareAllowOrigin());
        headerMap.put("Access-Control-Allow-Methods", prepareAllowMethods());
        headerMap.put("Access-Control-Max-Age", String.valueOf(prepareMaxAge()));
        headerMap.put("Access-Control-Allow-Headers", prepareAllowHeaders());
        headerMap.put("Access-Control-Allow-Credentials", prepareAllowCredentials());
        return headerMap;
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
