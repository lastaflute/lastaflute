/*
 * Copyright 2015-2019 the original author or authors.
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

import javax.servlet.Filter;

import org.lastaflute.core.direction.exception.FwRequiredAssistNotFoundException;
import org.lastaflute.web.api.ApiFailureHook;
import org.lastaflute.web.path.ActionAdjustmentProvider;
import org.lastaflute.web.ruts.multipart.MultipartResourceProvider;
import org.lastaflute.web.ruts.renderer.HtmlRenderingProvider;
import org.lastaflute.web.servlet.cookie.CookieResourceProvider;
import org.lastaflute.web.servlet.filter.accesslog.AccessLogHandler;
import org.lastaflute.web.servlet.filter.cors.CorsHook;
import org.lastaflute.web.servlet.filter.hook.FilterHook;
import org.lastaflute.web.servlet.filter.hook.FilterHookServletAdapter;
import org.lastaflute.web.servlet.filter.mdc.MDCHook;
import org.lastaflute.web.servlet.request.ResponseHandlingProvider;
import org.lastaflute.web.servlet.request.UserLocaleProcessProvider;
import org.lastaflute.web.servlet.request.UserTimeZoneProcessProvider;
import org.lastaflute.web.servlet.session.SessionResourceProvider;
import org.lastaflute.web.token.CsrfResourceProvider;
import org.lastaflute.web.token.DoubleSubmitResourceProvider;

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
    protected SessionResourceProvider sessionResourceProvider;
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
    protected List<FilterHook> insideFilterHookList; // lazy loaded
    protected List<FilterHook> outsideFilterHookList; // lazy loaded

    // -----------------------------------------------------
    //                                            Access Log
    //                                            ----------
    protected AccessLogHandler accessLogHandler;

    // -----------------------------------------------------
    //                                                 Token
    //                                                 -----
    protected CsrfResourceProvider csrfResourceProvider;
    protected DoubleSubmitResourceProvider doubleSubmitResourceProvider;

    // -----------------------------------------------------
    //                                             Multipart
    //                                             ---------
    protected MultipartResourceProvider multipartResourceProvider;

    // -----------------------------------------------------
    //                                         Html Renderer
    //                                         -------------
    protected HtmlRenderingProvider htmlRenderingProvider;

    // ===================================================================================
    //                                                                     Direct Property
    //                                                                     ===============
    // -----------------------------------------------------
    //                                               Servlet
    //                                               -------
    public void directRequest(UserLocaleProcessProvider userLocaleProcessProvider,
            UserTimeZoneProcessProvider userTimeZoneProcessProvider) {
        assertArgumentNotNull("userLocaleProcessProvider", userLocaleProcessProvider);
        assertArgumentNotNull("userTimeZoneProcessProvider", userTimeZoneProcessProvider);
        this.userLocaleProcessProvider = userLocaleProcessProvider;
        this.userTimeZoneProcessProvider = userTimeZoneProcessProvider;
    }

    public void directSession(SessionResourceProvider sessionResourceProvider) {
        assertArgumentNotNull("sessionResourceProvider", sessionResourceProvider);
        this.sessionResourceProvider = sessionResourceProvider;
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
    //                                           Filter Hook
    //                                           -----------
    // independent per purpose
    public void directMDC(MDCHook hook) {
        assertArgumentNotNull("hook", hook);
        getOutsideFilterHookList().add(hook); // for logging
    }

    public void directCors(CorsHook hook) {
        assertArgumentNotNull("hook", hook);
        getOutsideFilterHookList().add(hook); // before routing
    }

    public void directServletFilter(Filter servletFilter, boolean inside) { // inside means after logging
        assertArgumentNotNull("servletFilter", servletFilter);
        final List<FilterHook> hookList = inside ? getInsideFilterHookList() : getOutsideFilterHookList();
        hookList.add(newFilterHookServletAdapter(servletFilter));
    }

    protected FilterHookServletAdapter newFilterHookServletAdapter(Filter servletFilter) {
        return new FilterHookServletAdapter(servletFilter);
    }

    protected List<FilterHook> getInsideFilterHookList() {
        if (insideFilterHookList == null) {
            insideFilterHookList = new ArrayList<FilterHook>(4);
        }
        return insideFilterHookList;
    }

    protected List<FilterHook> getOutsideFilterHookList() {
        if (outsideFilterHookList == null) {
            outsideFilterHookList = new ArrayList<FilterHook>(4);
        }
        return outsideFilterHookList;
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

    public void directDoubleSubmit(DoubleSubmitResourceProvider doubleSubmitResourceProvider) {
        assertArgumentNotNull("doubleSubmitResourceProvider", doubleSubmitResourceProvider);
        this.doubleSubmitResourceProvider = doubleSubmitResourceProvider;
    }

    // -----------------------------------------------------
    //                                             Multipart
    //                                             ---------
    public void directMultipart(MultipartResourceProvider multipartResourceProvider) {
        this.multipartResourceProvider = multipartResourceProvider;
    }

    // -----------------------------------------------------
    //                                         Html Renderer
    //                                         -------------
    public void directHtmlRendering(HtmlRenderingProvider htmlRenderingProvider) {
        this.htmlRenderingProvider = htmlRenderingProvider;
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

    public SessionResourceProvider assistSessionResourceProvider() {
        return sessionResourceProvider; // not required, it's optional assist
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
        assertAssistObjectNotNull(apiFailureHook, "Not found the hook for API failure.");
        return apiFailureHook;
    }

    // -----------------------------------------------------
    //                                           Filter Hook
    //                                           -----------
    public List<FilterHook> assistInsideFilterHookList() { // empty allowed and normally empty
        return insideFilterHookList != null ? insideFilterHookList : Collections.emptyList();
    }

    public List<FilterHook> assistOutsideFilterHookList() { // empty allowed and normally empty
        return outsideFilterHookList != null ? outsideFilterHookList : Collections.emptyList();
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

    public DoubleSubmitResourceProvider assistDoubleSubmitResourceProvider() {
        return doubleSubmitResourceProvider; // not required, it's optional assist
    }

    // -----------------------------------------------------
    //                                             Multipart
    //                                             ---------
    public MultipartResourceProvider assistMultipartResourceProvider() {
        return multipartResourceProvider; // not required, it's optional assist
    }

    // -----------------------------------------------------
    //                                         Html Renderer
    //                                         -------------
    public HtmlRenderingProvider assistHtmlRenderingProvider() { // not null, default is prepared
        return htmlRenderingProvider != null ? htmlRenderingProvider : HtmlRenderingProvider.DEFAULT_RENDERING_PROVIDER;
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
