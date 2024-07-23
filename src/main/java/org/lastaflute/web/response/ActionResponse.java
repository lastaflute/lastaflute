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
package org.lastaflute.web.response;

import java.util.Map;

import org.dbflute.optional.OptionalThing;

/**
 * The response type of action return.
 * @author jflute
 */
public interface ActionResponse {

    // ===================================================================================
    //                                                                              Header
    //                                                                              ======
    /**
     * @param name The name of header. (NotNull)
     * @param values The varying array for value of header. (NotNull)
     * @return this. (NotNull)
     * @throws IllegalStateException When the header already exists.
     */
    ActionResponse header(String name, String... values);

    /**
     * @return The read-only map for headers, map:{header-name = header-value}. (NotNull, NotNullValue)
     */
    Map<String, String[]> getHeaderMap();

    // ===================================================================================
    //                                                                         HTTP Status
    //                                                                         ===========
    /**
     * @param httpStatus The specified HTTP status for the response.
     * @return this. (NotNull)
     */
    ActionResponse httpStatus(int httpStatus);

    /**
     * @return The specified HTTP status. (NotNull, EmptyAllowed: no specified)
     */
    OptionalThing<Integer> getHttpStatus();

    // ===================================================================================
    //                                                                       Determination
    //                                                                       =============
    /**
     * Does it return the response as empty body? <br>
     * e.g. for when response already committed by other ways.
     * @return The determination, true or false.
     */
    boolean isReturnAsEmptyBody();

    /**
     * Is the response defined state? (handled as valid response)
     * @return The determination, true or false.
     */
    default boolean isDefined() {
        return !isUndefined();
    }

    /**
     * Is the response undefined state? (do nothing, to next step)
     * @return The determination, true or false.
     */
    boolean isUndefined();

    // ===================================================================================
    //                                                                       Response Hook
    //                                                                       =============
    /**
     * Hook after action transaction is committed. (not called when roll-back) <br>
     * For when e.g. the callback process depends on current transaction result.
     * <pre>
     * <span style="color: #70226C">return</span> asHtml(<span style="color: #0000C0">path_...</span>).<span style="color: #994747">afterTxCommit</span>(() <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> { <span style="color: #3F7E5E">// in same thread</span>
     *     ... <span style="color: #3F7E5E">// executed after transaction committed</span>
     * });
     * 
     * <span style="color: #70226C">return</span> asHtml(<span style="color: #0000C0">path_...</span>).<span style="color: #994747">afterTxCommit</span>(() <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> { <span style="color: #3F7E5E">// as asynchronous</span>
     *     <span style="color: #994747">async</span>(() <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *         ... <span style="color: #3F7E5E">// executed as asynchronous process after transaction committed</span>
     *     });
     * });
     * </pre>
     * <p>Can use only when action execute method,
     * for example, cannot when hookBefore() of action hook.</p>
     * @param noArgLambda The callback for your process after transaction commit. (NotNull)
     * @return this. (NotNull)
     */
    ActionResponse afterTxCommit(ResponseHook noArgLambda);

    OptionalThing<ResponseHook> getAfterTxCommitHook();

    // ===================================================================================
    //                                                                  Undefined Instance
    //                                                                  ==================
    public static ActionResponse undefined() {
        return UndefinedResponse.instance();
    }
}
