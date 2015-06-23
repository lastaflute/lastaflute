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
package org.lastaflute.web.direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.lastaflute.core.direction.exception.FwRequiredAssistNotFoundException;
import org.lastaflute.web.api.ApiFailureHook;
import org.lastaflute.web.path.ActionAdjustmentProvider;
import org.lastaflute.web.servlet.cookie.CookieResourceProvider;
import org.lastaflute.web.servlet.filter.FilterListener;
import org.lastaflute.web.servlet.filter.accesslog.AccessLogHandler;
import org.lastaflute.web.servlet.filter.mdc.MDCListener;
import org.lastaflute.web.servlet.request.ResponseHandlingProvider;
import org.lastaflute.web.servlet.request.UserLocaleProcessProvider;
import org.lastaflute.web.servlet.request.UserTimeZoneProcessProvider;
import org.lastaflute.web.token.CsrfResourceProvider;

/**
 * @author jflute
 */
public class FwWebDirection {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                               Servlet
    //                                               -------
    protected UserLocaleProcessProvider userLocaleProcessProvider;
    protected UserTimeZoneProcessProvider userTimeZoneProcessProvider;
    protected CookieResourceProvider cookieResourceProvider;
    protected ResponseHandlingProvider responseHandlingProvider;

    // -----------------------------------------------------
    //                                            Adjustment
    //                                            ----------
    protected ActionAdjustmentProvider actionAdjustmentProvider;

    // -----------------------------------------------------
    //                                               Message
    //                                               -------
    protected String appMessageName;
    protected List<String> extendsMessageNameList; // lazy loaded

    // -----------------------------------------------------
    //                                              API Call
    //                                              --------
    protected ApiFailureHook apiFailureHook;

    // -----------------------------------------------------
    //                                       Filter Listener
    //                                       ---------------
    protected List<FilterListener> filterListenerList; // lazy loaded

    // -----------------------------------------------------
    //                                            Access Log
    //                                            ----------
    protected AccessLogHandler accessLogHandler;

    // -----------------------------------------------------
    //                                                 Token
    //                                                 -----
    protected CsrfResourceProvider csrfResourceProvider;

    // ===================================================================================
    //                                                                     Direct Property
    //                                                                     ===============
    // -----------------------------------------------------
    //                                               Servlet
    //                                               -------
    public void directRequest(UserLocaleProcessProvider userLocaleProcessProvider, UserTimeZoneProcessProvider userTimeZoneProcessProvider) {
        assertArgumentNotNull("userLocaleProcessProvider", userLocaleProcessProvider);
        assertArgumentNotNull("userTimeZoneProcessProvider", userTimeZoneProcessProvider);
        this.userLocaleProcessProvider = userLocaleProcessProvider;
        this.userTimeZoneProcessProvider = userTimeZoneProcessProvider;
    }

    public void directCookie(CookieResourceProvider cookieResourceProvider) {
        assertArgumentNotNull("cookieResourceProvider", cookieResourceProvider);
        this.cookieResourceProvider = cookieResourceProvider;
    }

    public void directResponse(ResponseHandlingProvider responseHandlingProvider) {
        assertArgumentNotNull("responseHandlingProvider", responseHandlingProvider);
        this.responseHandlingProvider = responseHandlingProvider;
    }

    // -----------------------------------------------------
    //                                            Adjustment
    //                                            ----------
    public void directAdjustment(ActionAdjustmentProvider actionAdjustmentProvider) {
        assertArgumentNotNull("actionAdjustmentProvider", actionAdjustmentProvider);
        this.actionAdjustmentProvider = actionAdjustmentProvider;
    }

    // -----------------------------------------------------
    //                                               Message
    //                                               -------
    public void directMessage(Consumer<List<String>> appSetupper, String... commonNames) {
        assertArgumentNotNull("appSetupper", appSetupper);
        assertArgumentNotNull("commonNames", commonNames);
        final List<String> nameList = new ArrayList<String>(4);
        appSetupper.accept(nameList);
        nameList.addAll(Arrays.asList(commonNames));
        appMessageName = nameList.remove(0);
        getExtendsMessageNameList().addAll(nameList);
    }

    protected List<String> getExtendsMessageNameList() {
        if (extendsMessageNameList == null) {
            extendsMessageNameList = new ArrayList<String>(4);
        }
        return extendsMessageNameList;
    }

    // -----------------------------------------------------
    //                                              API Call
    //                                              --------
    public void directApiCall(ApiFailureHook apiFailureHook) {
        assertArgumentNotNull("apiFailureHook", apiFailureHook);
        this.apiFailureHook = apiFailureHook;
    }

    // -----------------------------------------------------
    //                                       Filter Listener
    //                                       ---------------
    // independent per purpose
    public void directMDC(MDCListener listener) {
        assertArgumentNotNull("listener", listener);
        getFilterListenerList().add(listener);
    }

    protected List<FilterListener> getFilterListenerList() {
        if (filterListenerList == null) {
            filterListenerList = new ArrayList<FilterListener>(4);
        }
        return filterListenerList;
    }

    // -----------------------------------------------------
    //                                            Access Log
    //                                            ----------
    public void directAccessLog(AccessLogHandler accessLogHandler) {
        assertArgumentNotNull("accessLogHandler", accessLogHandler);
        this.accessLogHandler = accessLogHandler;
    }

    // -----------------------------------------------------
    //                                                 Token
    //                                                 -----
    public void directCsrf(CsrfResourceProvider csrfResourceProvider) {
        assertArgumentNotNull("csrfResourceProvider", csrfResourceProvider);
        this.csrfResourceProvider = csrfResourceProvider;
    }

    // ===================================================================================
    //                                                                              Assist
    //                                                                              ======
    // -----------------------------------------------------
    //                                               Servlet
    //                                               -------
    public UserLocaleProcessProvider assistUserLocaleProcessProvider() {
        assertAssistObjectNotNull(userLocaleProcessProvider, "Not found the provider for user locale process in request.");
        return userLocaleProcessProvider;
    }

    public UserTimeZoneProcessProvider assistUserTimeZoneProcessProvider() {
        assertAssistObjectNotNull(userTimeZoneProcessProvider, "Not found the provider for user time-zone process in request.");
        return userTimeZoneProcessProvider;
    }

    public CookieResourceProvider assistCookieResourceProvider() {
        assertAssistObjectNotNull(cookieResourceProvider, "Not found the provider for cookie resource.");
        return cookieResourceProvider;
    }

    public ResponseHandlingProvider assistResponseHandlingProvider() {
        return responseHandlingProvider; // not required, it's optional assist
    }

    // -----------------------------------------------------
    //                                            Adjustment
    //                                            ----------
    public ActionAdjustmentProvider assistActionAdjustmentProvider() {
        assertAssistObjectNotNull(actionAdjustmentProvider, "Not found the provider of action adjustment.");
        return actionAdjustmentProvider;
    }

    // -----------------------------------------------------
    //                                               Message
    //                                               -------
    public String assistAppMessageName() {
        assertAssistObjectNotNull(appMessageName, "Not found the (file without extension) name for application message.");
        return appMessageName;
    }

    public List<String> assistExtendsMessageNameList() { // empty allowed but almost exists
        return extendsMessageNameList != null ? extendsMessageNameList : Collections.emptyList();
    }

    // -----------------------------------------------------
    //                                              API Call
    //                                              --------
    public ApiFailureHook assistApiFailureHook() {
        assertAssistObjectNotNull(appMessageName, "Not found the hook for API failure.");
        return apiFailureHook;
    }

    // -----------------------------------------------------
    //                                       Filter Listener
    //                                       ---------------
    public List<FilterListener> assistFilterListenerList() { // empty allowed and normally empty
        return filterListenerList != null ? filterListenerList : Collections.emptyList();
    }

    // -----------------------------------------------------
    //                                            Access Log
    //                                            ----------
    public AccessLogHandler assistAccessLogHandler() {
        return accessLogHandler; // not required, it's optional assist
    }

    // -----------------------------------------------------
    //                                                 Token
    //                                                 -----
    public CsrfResourceProvider assistCsrfResourceProvider() {
        return csrfResourceProvider; // not required, it's optional assist
    }

    // ===================================================================================
    //                                                                       Assert Helper
    //                                                                       =============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            throw new IllegalArgumentException("The variableName should not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
        }
    }

    protected void assertAssistObjectNotNull(Object obj, String msg) {
        if (obj == null) {
            throw new FwRequiredAssistNotFoundException(msg);
        }
    }
}
