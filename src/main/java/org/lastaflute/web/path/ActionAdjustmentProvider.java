/*
 * Copyright 2015-2024 the original author or authors.
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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.config.ActionMapping;
import org.lastaflute.web.ruts.inoutlogging.InOutLogOption;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.validation.VaConfigSetupper;

// package is a little strange (path adjustment from the beginning...)
// but no change for compatible
/**
 * @author jflute
 */
public interface ActionAdjustmentProvider {

    // ===================================================================================
    //                                                                             Routing
    //                                                                             =======
    // -----------------------------------------------------
    //                                 Typical Determination
    //                                 ---------------------
    /**
     * Is the request routing treated as 404 not found forcedly?
     * @param request The request object provided from filter. (NotNull)
     * @param requestPath The path of request to search action (before customization). (NotNull)
     * @return The determination, true or false. If true, returns 404 response immediately.
     */
    default boolean isForced404NotFoundRouting(HttpServletRequest request, String requestPath) {
        return false;
    }

    /**
     * Is the request routing except forcedly?
     * @param request The request object provided from filter. (NotNull)
     * @param requestPath The path of request to search action (before customization). (NotNull)
     * @return The determination, true or false. If false, default determination for routing.
     */
    default boolean isForcedRoutingExcept(HttpServletRequest request, String requestPath) {
        return false;
    }

    /**
     * Is the request routing target forcedly?
     * @param request The request object provided from filter. (NotNull)
     * @param requestPath The path of request to search action (before customization). (NotNull)
     * @return The determination, true or false. If false, default determination for routing.
     */
    default boolean isForcedRoutingTarget(HttpServletRequest request, String requestPath) {
        return false;
    }

    // -----------------------------------------------------
    //                                           URL Mapping
    //                                           -----------
    /**
     * <p>This is an old style method, you can use deeplyCustomizeActionMapping() instead of this.</p>
     * Customize the request path for action mapping. <br>
     * This method is called many times so you should care the performance. <br>
     * And you should also override customizeActionUrlDeriving() for LastaDoc, Swagger as reserve logic. <br>
     * @param requestPath The path of request to search action, e.g. '/product/list/'. (NotNull)
     * @return The customized request path. (NullAllowed: if null, search by the plain request path)
     */
    default String customizeActionMappingRequestPath(String requestPath) { // old style, use customizeActionUrlMapping()
        return null;
    }

    /**
     * Customize the action URL mapping. <br>
     * This method is called many times so you should care the performance. <br>
     * And you should also override customizeActionUrlReverse() for LastaDoc, Swagger as reserve logic.
     * @param resource The resource for customization of URL mapping, the requestPath is e.g. '/product/list/'. (NotNull)
     * @return The option of action URL mapping. (NullAllowed: if null, search by the plain request path)
     */
    default UrlMappingOption customizeActionUrlMapping(UrlMappingResource resource) {
        return null;
    }

    // -----------------------------------------------------
    //                                           URL Reverse
    //                                           -----------
    /**
     * Customize the action URL reverse, e.g. toActionUrl(). <br>
     * This method is called many times so you should care the performance.
     * @param resource The resource of action URL reverse. (NotNull)
     * @return The option of action URL reverse. (NullAllowed: if null, reverse plainly)
     */
    default UrlReverseOption customizeActionUrlReverse(UrlReverseResource resource) {
        return null;
    }

    // -----------------------------------------------------
    //                                        Trailing Slash
    //                                        --------------
    /**
     * Does it suppress 'trailing slash redirect' for SEO?
     * @param request The request object provided from filter. (NotNull)
     * @param requestPath The path of request to search action (after customization). (NotNull)
     * @param execute The action execute of the request. (NotNull)
     * @return The determination, true or false. If false, default determination for routing.
     */
    default boolean isSuppressTrailingSlashRedirect(HttpServletRequest request, String requestPath, ActionExecute execute) {
        return false;
    }

    // -----------------------------------------------------
    //                                         404 Not Found
    //                                         -------------
    /**
     * Is the no-routing request treated as 404 not found?
     * @param request The request object provided from filter. (NotNull)
     * @param requestPath The path of request to search action (before customization). (NotNull)
     * @return The determination, true or false. If true, returns 404 response immediately.
     */
    default boolean isNoRoutingRequestAs404NotFound(HttpServletRequest request, String requestPath) {
        return false;
    }

    // ===================================================================================
    //                                                                         HTML Foward
    //                                                                         ===========
    /**
     * Filter the HTML path.
     * @param path The path for HTML template, e.g. if JSP, it has view-prefix '/WEB-INF/view/'. (NotNull)
     * @param actionMapping The action mapping for current action. (NotNull)
     * @return The filtered path for HTML. (NullAllowed: if null, no filter)
     */
    default String filterHtmlPath(String path, ActionMapping actionMapping) {
        return null;
    }

    /**
     * Prepare the word list for retry calculation of action path for the HTML. <br>
     * You can retry calculation by filtering like this:
     * <pre>
     * e.g. /member/sp_member_list.html, wordList:[sp, member, list]
     *  normally /member/ but /member/list/ by filteredList:[member, list]
     * </pre>
     * @param requestPath The path of requested HTML, that doesn't have view-prefix. (NotNull)
     * @param wordList The list of words in JSP file name, always has two or more elements. (NotNull)
     * @return The filtered list of words for action path calculation. (NullAllowed: if null, no filter)
     */
    default List<String> prepareHtmlRetryWordList(String requestPath, List<String> wordList) {
        return null;
    }

    // ===================================================================================
    //                                                                        Form Mapping
    //                                                                        ============
    /**
     * Provide the limit size of indexed property. <br>
     * You should return effective size against array injection problem.
     * @return The integer for the size. (MinusAllowed: if minus, no guard)
     */
    default int provideIndexedPropertySizeLimit() {
        return 256; // as default
    }

    /**
     * Adjust form mapping from request parameters.
     * @return The option of form mapping. (NullAllowed: if null, no option)
     */
    default FormMappingOption adjustFormMapping() {
        return null;
    }

    // ===================================================================================
    //                                                                          Validation
    //                                                                          ==========
    /**
     * Adjust action validator configuration. (called only once if cached)
     * @return The setupper of hibernate configuration. (NullAllowed: if null, no configuration)
     */
    default VaConfigSetupper adjustValidatorConfig() {
        return null;
    }

    // ===================================================================================
    //                                                                     Action Response
    //                                                                     ===============
    /**
     * Adjust (defined) action response just before reflecting to response, e.g. header.
     * @param runtime The runtime of action that has current various state. (NotNull)
     * @param response The defined action response. (NotNull)
     */
    default void adjustActionResponseJustBefore(ActionRuntime runtime, ActionResponse response) {
    }

    /**
     * Adjust action response reflecting.
     * @return The option of action response reflecting. (NullAllowed: if null, no option)
     */
    default ResponseReflectingOption adjustResponseReflecting() {
        return null;
    }

    // ===================================================================================
    //                                                               Application Exception
    //                                                               =====================
    /**
     * Adjust application exception handling.
     * @return The option of API response. (NullAllowed: if null, no option)
     */
    default ApplicationExceptionOption adjustApplicationExceptionHandling() {
        return null;
    }

    // ===================================================================================
    //                                                                       InOut Logging
    //                                                                       =============
    /**
     * Does it use in-out logging of action? (show e.g. request parameter and response body as INFO) <br>
     * The logger name is "lastaflute.inout".
     * @return The determination, true or false. If false, no logging.
     */
    default boolean isUseInOutLogging() {
        return false;
    }

    /**
     * Adjust in-out logging of action? (e.g. suppress response body) <br>
     * Called per one request so you should cache your option instance. <br>
     * And called only when isUseInOutLogging() returns true.
     * @return The option of in-out logging. (NullAllowed: then no option)
     */
    default InOutLogOption adjustInOutLogging() {
        return null;
    }

    // ===================================================================================
    //                                                                       Error Logging
    //                                                                       =============
    /**
     * Does the exception suppress server error logging?
     * @param cause The exception as server error. (NotNull)
     * @return The determination, true or false. If false, error logging.
     */
    default boolean isSuppressServerErrorLogging(Throwable cause) {
        return false;
    }
}
