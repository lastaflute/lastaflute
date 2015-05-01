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
package org.dbflute.lastaflute.web.path;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.dbflute.lastaflute.web.ruts.config.ActionExecute;
import org.dbflute.lastaflute.web.ruts.config.ActionMapping;

/**
 * @author jflute
 */
public interface ActionAdjustmentProvider {

    /**
     * Provide the limit size of indexed property. <br>
     * You should return effective size against array injection problem.
     * @return The integer for the size. (MinusAllowed: if minus, no guard)
     */
    int provideIndexedPropertySizeLimit();

    /**
     * Decode the escaped character for property value from URL parameter. <br>
     * The basic characters (e.g. %2d) is already decoded without this method. <br>
     * So this method is basically for your original decoding.
     * @param bean The bean object of the property. (NotNull)
     * @param name The property name for the value. (NotNull)
     * @param value The property value to decode. (NotNull)
     * @return The decoded value for property value. (NullAllowed: if null, no decoded)
     */
    String decodeUrlParameterPropertyValue(Object bean, String name, String value);

    /**
     * Filter the HTML path.
     * @param path The path for JSP, that has view-prefix e.g. '/WEB-INF/view/'. (NotNull)
     * @param actionMapping The action mapping for current action. (NotNull)
     * @return The filtered path for JSP. (NullAllowed: if null, no filter)
     */
    String filterHtmlPath(String path, ActionMapping actionMapping);

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
    List<String> prepareHtmlRetryWordList(String requestPath, List<String> wordList);

    /**
     * Is the request routing target forcedly?
     * @param request The request object provided from filter. (NotNull)
     * @param requestPath The path of request to search action. (NotNull)
     * @return The determination, true or false. If false, default determination for routing.
     */
    boolean isForcedRoutingTarget(HttpServletRequest request, String requestPath);

    /**
     * Does it suppress redirecting with slash forcedly?
     * @param request The request object provided from filter. (NotNull)
     * @param requestPath The path of request to search action. (NotNull)
     * @param executeConfig The configuration for execute-action. (NotNull)
     * @return The determination, true or false. If false, default determination for routing.
     */
    boolean isForcedSuppressRedirectWithSlash(HttpServletRequest request, String requestPath, ActionExecute executeConfig);

    /**
     * Customize the request path for action mapping. <br>
     * This method is called many times so you should care the performance.
     * @param requestPath The path of request to search action. (NotNull)
     * @return The customized request path. (NullAllowed: if null, search by the plain request path)
     */
    String customizeActionMappingRequestPath(String requestPath);
}
