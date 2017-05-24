/*
 * Copyright 2015-2017 the original author or authors.
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
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.validation.VaConfigSetupper;

// package is a little strange (path adjustment from the beginning...)
// but no change for compatible
/**
 * @author jflute
 */
public interface ActionAdjustmentProvider {

    /**
     * Provide the limit size of indexed property. <br>
     * You should return effective size against array injection problem.
     * @return The integer for the size. (MinusAllowed: if minus, no guard)
     */
    default int provideIndexedPropertySizeLimit() {
        return 256; // as default
    }

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

    /**
     * Customize the request path for action mapping. <br>
     * This method is called many times so you should care the performance.
     * @param requestPath The path of request to search action, e.g. '/product/list/'. (NotNull)
     * @return The customized request path. (NullAllowed: if null, search by the plain request path)
     */
    default String customizeActionMappingRequestPath(String requestPath) {
        return null;
    }

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

    /**
     * Adjust (defined) action response just before reflecting to response, e.g. header.
     * @param runtime The runtime of action that has current various state. (NotNull)
     * @param response The defined action response. (NotNull)
     */
    default void adjustActionResponseJustBefore(ActionRuntime runtime, ActionResponse response) {
    }

    /**
     * Adjust form mapping from request parameters.
     * @return The option of form mapping. (NullAllowed: if null, no option)
     */
    default FormMappingOption adjustFormMapping() {
        return null;
    }

    /**
     * Adjust action response reflecting.
     * @return The option of action response reflecting. (NullAllowed: if null, no option)
     */
    default ResponseReflectingOption adjustResponseReflecting() {
        return null;
    }

    /**
     * Adjust action validator configuration. (called only once if cached)
     * @return The setupper of hibernate configuration. (NullAllowed: if null, no configuration)
     */
    default VaConfigSetupper adjustValidatorConfig() {
        return null;
    }

    /**
     * Adjust application exception handling.
     * @return The option of API response. (NullAllowed: if null, no option)
     */
    default ApplicationExceptionOption adjustApplicationExceptionHandling() {
        return null;
    }
}
