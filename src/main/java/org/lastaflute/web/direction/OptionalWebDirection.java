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

import org.lastaflute.core.direction.exception.FwRequiredAssistNotFoundException;
import org.lastaflute.web.api.ApiResultProvider;
import org.lastaflute.web.path.ActionAdjustmentProvider;
import org.lastaflute.web.servlet.cookie.CookieResourceProvider;
import org.lastaflute.web.servlet.request.ResponseHandlingProvider;
import org.lastaflute.web.servlet.request.UserLocaleProcessProvider;
import org.lastaflute.web.servlet.request.UserTimeZoneProcessProvider;

/**
 * @author jflute
 */
public class OptionalWebDirection {

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
    protected String domainMessageName;
    protected final List<String> extendsMessageNameList = new ArrayList<String>(4);

    // -----------------------------------------------------
    //                                              API Call
    //                                              --------
    protected ApiResultProvider apiResultProvider;

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
    public void directMessage(String domainMessageName, String... extendsMessageNames) {
        this.domainMessageName = domainMessageName;
        if (extendsMessageNames != null && extendsMessageNames.length > 0) {
            this.extendsMessageNameList.addAll(Arrays.asList(extendsMessageNames));
        }
    }

    // -----------------------------------------------------
    //                                              API Call
    //                                              --------
    public void directApiCall(ApiResultProvider apiResultProvider) {
        this.apiResultProvider = apiResultProvider;
    }

    // ===================================================================================
    //                                                                              Assist
    //                                                                              ======
    // -----------------------------------------------------
    //                                               Servlet
    //                                               -------
    public UserLocaleProcessProvider assistUserLocaleProcessProvider() {
        if (userLocaleProcessProvider == null) {
            String msg = "Not found the provider for user locale process in request.";
            throw new FwRequiredAssistNotFoundException(msg);
        }
        return userLocaleProcessProvider;
    }

    public UserTimeZoneProcessProvider assistUserTimeZoneProcessProvider() {
        if (userLocaleProcessProvider == null) {
            String msg = "Not found the provider for user time-zone process in request.";
            throw new FwRequiredAssistNotFoundException(msg);
        }
        return userTimeZoneProcessProvider;
    }

    public CookieResourceProvider assistCookieResourceProvider() {
        if (cookieResourceProvider == null) {
            String msg = "Not found the provider for cookie resource.";
            throw new FwRequiredAssistNotFoundException(msg);
        }
        return cookieResourceProvider;
    }

    public ResponseHandlingProvider assistResponseHandlingProvider() {
        return responseHandlingProvider; // not required for compatibility
    }

    // -----------------------------------------------------
    //                                            Adjustment
    //                                            ----------
    public ActionAdjustmentProvider assistActionAdjustmentProvider() {
        if (actionAdjustmentProvider == null) {
            String msg = "Not found the provider of action adjustment.";
            throw new FwRequiredAssistNotFoundException(msg);
        }
        return actionAdjustmentProvider;
    }

    // -----------------------------------------------------
    //                                               Message
    //                                               -------
    public String assistDomainMessageName() {
        if (domainMessageName == null) {
            String msg = "Not found the (file without extension) name for domain message.";
            throw new FwRequiredAssistNotFoundException(msg);
        }
        return domainMessageName;
    }

    public List<String> assistExtendsMessageNameList() {
        return extendsMessageNameList;
    }

    // -----------------------------------------------------
    //                                              API Call
    //                                              --------
    public ApiResultProvider assistApiResultProvider() {
        return apiResultProvider; // not required for compatibility
    }
}
