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
import java.util.List;
import java.util.function.Consumer;

import org.lastaflute.core.direction.exception.FwRequiredAssistNotFoundException;
import org.lastaflute.web.api.ApiFailureHook;
import org.lastaflute.web.path.ActionAdjustmentProvider;
import org.lastaflute.web.servlet.cookie.CookieResourceProvider;
import org.lastaflute.web.servlet.request.ResponseHandlingProvider;
import org.lastaflute.web.servlet.request.UserLocaleProcessProvider;
import org.lastaflute.web.servlet.request.UserTimeZoneProcessProvider;

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
    protected final List<String> extendsMessageNameList = new ArrayList<String>(4);

    // -----------------------------------------------------
    //                                              API Call
    //                                              --------
    protected ApiFailureHook apiFailureHook;

    // ===================================================================================
    //                                                                     Direct Property
    //                                                                     ===============
    // -----------------------------------------------------
    //                                               Servlet
    //                                               -------
    public void directRequest(UserLocaleProcessProvider userLocaleProcessProvider, UserTimeZoneProcessProvider userTimeZoneProcessProvider) {
        this.userLocaleProcessProvider = userLocaleProcessProvider;
        this.userTimeZoneProcessProvider = userTimeZoneProcessProvider;
    }

    public void directCookie(CookieResourceProvider cookieResourceProvider) {
        this.cookieResourceProvider = cookieResourceProvider;
    }

    public void directResponse(ResponseHandlingProvider responseHandlingProvider) {
        this.responseHandlingProvider = responseHandlingProvider;
    }

    // -----------------------------------------------------
    //                                            Adjustment
    //                                            ----------
    public void directAdjustment(ActionAdjustmentProvider actionAdjustmentProvider) {
        this.actionAdjustmentProvider = actionAdjustmentProvider;
    }

    // -----------------------------------------------------
    //                                               Message
    //                                               -------
    public void directMessage(Consumer<List<String>> appSetupper, String... commonNames) {
        final List<String> nameList = new ArrayList<String>(4);
        appSetupper.accept(nameList);
        nameList.addAll(Arrays.asList(commonNames));
        appMessageName = nameList.remove(0);
        extendsMessageNameList.addAll(nameList);
    }

    // -----------------------------------------------------
    //                                              API Call
    //                                              --------
    public void directApiCall(ApiFailureHook apiFailureHook) {
        this.apiFailureHook = apiFailureHook;
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

    public List<String> assistExtendsMessageNameList() {
        return extendsMessageNameList; // empty allowed but almost exists 
    }

    // -----------------------------------------------------
    //                                              API Call
    //                                              --------
    public ApiFailureHook assistApiFailureHook() {
        assertAssistObjectNotNull(appMessageName, "Not found the hook for API failure.");
        return apiFailureHook;
    }

    // -----------------------------------------------------
    //                                         Assert Helper
    //                                         -------------
    protected void assertAssistObjectNotNull(Object obj, String msg) {
        if (obj == null) {
            throw new FwRequiredAssistNotFoundException(msg);
        }
    }
}
