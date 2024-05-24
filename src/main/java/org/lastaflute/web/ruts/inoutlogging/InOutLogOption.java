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
package org.lastaflute.web.ruts.inoutlogging;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.dbflute.helper.StringSet;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.web.ruts.inoutlogging.InOutLogger.InOutValueEntry;
import org.lastaflute.web.ruts.process.ActionRuntime;

/**
 * @author jflute
 * @author awaawa
 * @since 1.0.0 (2017/08/23 Wednesday)
 */
public class InOutLogOption {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected boolean async;
    protected boolean suppressResponseBody; // may be too big
    protected List<String> requestHeaderNameList; // keeping order
    protected Function<String, String> requestParameterFilter;
    protected Function<InOutValueEntry, Object> requestParameterValueFilter;
    protected Function<String, String> requestBodyFilter;
    protected List<String> responseHeaderNameList; // keeping order
    protected Function<String, String> responseBodyFilter;
    protected Predicate<ActionRuntime> loggingExceptDeterminer;

    // ===================================================================================
    //                                                                         Easy-to-Use
    //                                                                         ===========
    // -----------------------------------------------------
    //                                                 Basic
    //                                                 -----
    /**
     * Enable logging as asynchronous. <br>
     * However basically unneeded because logging is after writing response so no wait. <br>
     * (To be exact, only redirection of HTML response is after logging) <br>
     * And asynchronous thread queue may be over work...worry <br>
     * This is emergency option for unexpected trouble.
     * @return this. (NotNull)
     */
    public InOutLogOption async() {
        async = true;
        return this;
    }

    /**
     * Suppress response body. (remove response body from in-out log)
     * @return this. (NotNull)
     */
    public InOutLogOption suppressResponseBody() {
        suppressResponseBody = true;
        return this;
    }

    // -----------------------------------------------------
    //                                               Request
    //                                               -------
    /**
     * @param requestHeaderNameList The list of target request header names. (NotNull, NotEmpty)
     * @return this. (NotNull)
     */
    public InOutLogOption showRequestHeader(List<String> requestHeaderNameList) {
        if (requestHeaderNameList == null) {
            throw new IllegalArgumentException("The argument 'requestHeaderNameList' should not be null.");
        }
        if (requestHeaderNameList.isEmpty()) {
            throw new IllegalArgumentException("The argument 'requestHeaderNameList' should not be empty.");
        }
        if (hasDuplicateHeader(requestHeaderNameList)) {
            String msg = "The argument 'requestHeaderNameList' should not be duplicate: " + requestHeaderNameList;
            throw new IllegalArgumentException(msg);
        }
        this.requestHeaderNameList = requestHeaderNameList;
        return this;
    }

    /**
     * (Old Style) <br>
     * Filter the request parameter (whole) expression that contains all parameters. <br>
     * The callback argument is e.g. requestParameter:{sea=mystic, land=oneman}. <br>
     * If you need to adjust detailedly, you can use filterRequestParameterMap().
     * @param oneArgLambda The filter of request parameter expression, no filter if returns null. (NotNull)
     * @return this. (NotNull)
     */
    public InOutLogOption filterRequestParameter(Function<String, String> oneArgLambda) {
        if (oneArgLambda == null) {
            throw new IllegalArgumentException("The argument 'oneArgLambda' should not be null.");
        }
        this.requestParameterFilter = oneArgLambda;
        return this;
    }

    /**
     * Filter the request parameter value per key (of request parameter map from Servlet). <br>
     * The callback argument is e.g. sea, land (if the map has sea, land as key).
     * @param oneArgLambda The filter of request parameter value, no filter if returns null. (NotNull)
     * @return this. (NotNull)
     */
    public InOutLogOption filterRequestParameterValue(Function<InOutValueEntry, Object> oneArgLambda) {
        if (oneArgLambda == null) {
            throw new IllegalArgumentException("The argument 'oneArgLambda' should not be null.");
        }
        this.requestParameterValueFilter = oneArgLambda;
        return this;
    }

    /**
     * @param oneArgLambda The filter of request body, no filter if returns null. (NotNull)
     * @return this. (NotNull)
     */
    public InOutLogOption filterRequestBody(Function<String, String> oneArgLambda) {
        if (oneArgLambda == null) {
            throw new IllegalArgumentException("The argument 'requestBodyFilter' should not be null.");
        }
        this.requestBodyFilter = oneArgLambda;
        return this;
    }

    // -----------------------------------------------------
    //                                              Response
    //                                              --------
    /**
     * @param responseHeaderNameList The list of target response header names. (NotNull, NotEmpty)
     * @return this. (NotNull)
     */
    public InOutLogOption showResponseHeader(List<String> responseHeaderNameList) {
        if (responseHeaderNameList == null) {
            throw new IllegalArgumentException("The argument 'responseHeaderNameList' should not be null.");
        }
        if (responseHeaderNameList.isEmpty()) {
            throw new IllegalArgumentException("The argument 'responseHeaderNameList' should not be empty.");
        }
        if (hasDuplicateHeader(responseHeaderNameList)) {
            String msg = "The argument 'responseHeaderNameList' should not be duplicate: " + responseHeaderNameList;
            throw new IllegalArgumentException(msg);
        }
        this.responseHeaderNameList = responseHeaderNameList;
        return this;
    }

    /**
     * @param oneArgLambda The filter of response body, no filter if returns null. (NotNull)
     * @return this. (NotNull)
     */
    public InOutLogOption filterResponseBody(Function<String, String> oneArgLambda) {
        if (oneArgLambda == null) {
            throw new IllegalArgumentException("The argument 'responseBodyFilter' should not be null.");
        }
        this.responseBodyFilter = oneArgLambda;
        return this;
    }

    // -----------------------------------------------------
    //                                            Targetting
    //                                            ----------
    /**
     * @param loggingExceptDeterminer The determiner to except logging. (NotNull)
     * @return this. (NotNull)
     */
    public InOutLogOption exceptLogging(Predicate<ActionRuntime> loggingExceptDeterminer) {
        if (loggingExceptDeterminer == null) {
            throw new IllegalArgumentException("The argument 'loggingExceptDeterminer' should not be null.");
        }
        this.loggingExceptDeterminer = loggingExceptDeterminer;
        return this;
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected boolean hasDuplicateHeader(List<String> requestHeaderNameList) {
        final StringSet caseInsensitiveSet = StringSet.createAsCaseInsensitive();
        caseInsensitiveSet.addAll(requestHeaderNameList);
        return caseInsensitiveSet.size() < requestHeaderNameList.size();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public boolean isAsync() {
        return async;
    }

    public boolean isSuppressResponseBody() {
        return suppressResponseBody;
    }

    public List<String> getRequestHeaderNameList() { // not null
        return requestHeaderNameList != null ? requestHeaderNameList : Collections.emptyList();
    }

    public OptionalThing<Function<String, String>> getRequestParameterFilter() {
        return OptionalThing.ofNullable(requestParameterFilter, () -> {
            throw new IllegalStateException("Not found the requestParameterFilter.");
        });
    }

    public OptionalThing<Function<InOutValueEntry, Object>> getRequestParameterValueFilter() {
        return OptionalThing.ofNullable(requestParameterValueFilter, () -> {
            throw new IllegalStateException("Not found the requestParameterValueFilter.");
        });
    }

    public OptionalThing<Function<String, String>> getRequestBodyFilter() {
        return OptionalThing.ofNullable(requestBodyFilter, () -> {
            throw new IllegalStateException("Not found the requestBodyFilter.");
        });
    }

    public List<String> getResponseHeaderNameList() { // not null
        return responseHeaderNameList != null ? responseHeaderNameList : Collections.emptyList();
    }

    public OptionalThing<Function<String, String>> getResponseBodyFilter() {
        return OptionalThing.ofNullable(responseBodyFilter, () -> {
            throw new IllegalStateException("Not found the responseBodyFilter.");
        });
    }

    public OptionalThing<Predicate<ActionRuntime>> getLoggingExceptDeterminer() {
        return OptionalThing.ofNullable(loggingExceptDeterminer, () -> {
            throw new IllegalStateException("Not found the responseBodyFilter.");
        });
    }
}
