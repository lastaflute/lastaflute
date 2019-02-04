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
package org.lastaflute.web.ruts.inoutlogging;

import java.util.function.Function;
import java.util.function.Predicate;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.web.ruts.process.ActionRuntime;

/**
 * @author jflute
 * @since 1.0.0 (2017/08/23 Wednesday)
 */
public class InOutLogOption {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected boolean async;
    protected boolean suppressResponseBody; // may be too big
    protected Function<String, String> requestParameterFilter;
    protected Function<String, String> requestBodyFilter;
    protected Function<String, String> responseBodyFilter;
    protected Predicate<ActionRuntime> loggingExceptDeterminer;

    // ===================================================================================
    //                                                                         Easy-to-Use
    //                                                                         ===========
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

    /**
     * @param requestParameterFilter The filter of request parameters, no filter if returns null. (NotNull)
     * @return this. (NotNull)
     */
    public InOutLogOption filterRequestParameter(Function<String, String> requestParameterFilter) {
        if (requestParameterFilter == null) {
            throw new IllegalArgumentException("The argument 'requestParameterFilter' should not be null.");
        }
        this.requestParameterFilter = requestParameterFilter;
        return this;
    }

    /**
     * @param requestBodyFilter The filter of request body, no filter if returns null. (NotNull)
     * @return this. (NotNull)
     */
    public InOutLogOption filterRequestBody(Function<String, String> requestBodyFilter) {
        if (requestBodyFilter == null) {
            throw new IllegalArgumentException("The argument 'requestBodyFilter' should not be null.");
        }
        this.requestBodyFilter = requestBodyFilter;
        return this;
    }

    /**
     * @param responseBodyFilter The filter of response body, no filter if returns null. (NotNull)
     * @return this. (NotNull)
     */
    public InOutLogOption filterResponseBody(Function<String, String> responseBodyFilter) {
        if (responseBodyFilter == null) {
            throw new IllegalArgumentException("The argument 'responseBodyFilter' should not be null.");
        }
        this.responseBodyFilter = responseBodyFilter;
        return this;
    }

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
    //                                                                            Accessor
    //                                                                            ========
    public boolean isAsync() {
        return async;
    }

    public boolean isSuppressResponseBody() {
        return suppressResponseBody;
    }

    public OptionalThing<Function<String, String>> getRequestParameterFilter() {
        return OptionalThing.ofNullable(requestParameterFilter, () -> {
            throw new IllegalStateException("Not found the requestParameterFilter.");
        });
    }

    public OptionalThing<Function<String, String>> getRequestBodyFilter() {
        return OptionalThing.ofNullable(requestBodyFilter, () -> {
            throw new IllegalStateException("Not found the requestBodyFilter.");
        });
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
