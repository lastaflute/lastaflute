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
package org.lastaflute.web.servlet.filter.mdc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.lastaflute.web.servlet.cookie.CookieManager;

/**
 * @author jflute
 * @since 0.6.0 (2015/06/03 Wednesday)
 */
public class TypicalMDCListenerFactory {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    public static final String LA_REQUEST_PATH = "la.requestPath";
    public static final String LA_USER_AGENT = "la.userAgent";
    public static final String LA_HEADER_HOST = "la.headerHost";
    public static final String LA_X_FORWARDED_FOR = "la.xForwardedFor";
    public static final String LA_REQUEST_ID = "la.requestId";
    public static final String LA_USER_TRACE_ID = "la.userTraceId";
    public static final String DEFAULT_USER_TRACE_COOKIE_KEY = "USTRCID";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final int userTraceExpire; // seconds

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public TypicalMDCListenerFactory(int userTraceExpire) {
        this.userTraceExpire = userTraceExpire;
    }

    // ===================================================================================
    //                                                                              Create
    //                                                                              ======
    public MDCListener create() {
        return new MDCListener(prepareMDCMap());
    }

    protected Map<String, Function<MDCSetupResource, String>> prepareMDCMap() {
        final Map<String, Function<MDCSetupResource, String>> mdcMap = new LinkedHashMap<String, Function<MDCSetupResource, String>>();
        mdcMap.put(LA_REQUEST_PATH, res -> res.getRequestManager().getRequestPath());
        // getting remote host may have heavy performance cost by DNS lookup so not default item
        //mdcMap.put(LA_REMOTE_HOST, res -> res.getRequestManager().getRequest().getRemoteHost());
        mdcMap.put(LA_HEADER_HOST, res -> res.getRequestManager().getHeaderHost().orElse(null));
        mdcMap.put(LA_USER_AGENT, res -> res.getRequestManager().getHeaderUserAgent().orElse(null));
        mdcMap.put(LA_X_FORWARDED_FOR, res -> res.getRequestManager().getHeaderXForwardedFor().orElse(null));
        mdcMap.put(LA_REQUEST_ID, res -> buildRequestId(res));
        mdcMap.put(LA_USER_TRACE_ID, res -> handleUserTrace(res)); // should be after request ID putting
        return mdcMap;
    }

    protected String buildRequestId(MDCSetupResource res) {
        final StringBuilder sb = new StringBuilder();
        sb.append(res.getRequestManager().getTimeManager().currentMillis());
        sb.append(Thread.currentThread().getName());
        sb.append(new Object().hashCode()); // to be (best effort) unique in plural application servers
        return Integer.toHexString(sb.toString().hashCode());
    }

    protected String handleUserTrace(MDCSetupResource res) {
        final String cookieKey = getUserTraceCookieKey();
        final CookieManager cookieManager = res.getRequestManager().getCookieManager();
        return cookieManager.getCookie(cookieKey).map(cookie -> cookie.getValue()).orElseGet(() -> {
            final String requestId = res.getAlreadyRegistered(LA_REQUEST_ID).get(); /* always exists */
            cookieManager.setCookie(cookieKey, requestId, userTraceExpire);
            return requestId;
        });
    }

    protected String getUserTraceCookieKey() {
        return DEFAULT_USER_TRACE_COOKIE_KEY;
    }
}
