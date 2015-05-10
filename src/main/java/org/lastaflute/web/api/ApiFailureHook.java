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
package org.lastaflute.web.api;

import javax.servlet.http.HttpServletResponse;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.web.callback.ActionRuntimeMeta;
import org.lastaflute.web.response.ApiResponse;

/**
 * The hook for API failures.
 * @author jflute
 */
public interface ApiFailureHook {

    /**
     * Handle API failure when login required failure.
     * @param resource The resource of API result, contains e.g. error messages if it exists. (NotNull)
     * @param meta The runtime meta of action execute for the current request. (NotNull)
     * @return The API response, which is for e.g. JSON or XML. (NotNull)
     */
    ApiResponse handleLoginRequiredFailure(ApiFailureResource resource, ActionRuntimeMeta meta);

    /**
     * Handle API failure when validation error.
     * @param resource The resource of API result, contains e.g. error messages if it exists. (NotNull)
     * @param meta The runtime meta of action execute for the current request. (NotNull)
     * @return The API response, which is for e.g. JSON or XML. (NotNull)
     */
    ApiResponse handleValidationError(ApiFailureResource resource, ActionRuntimeMeta meta);

    /**
     * Handle API failure when application exception.
     * @param resource The resource of API result, contains e.g. error messages if it exists. (NotNull)
     * @param meta The runtime meta of action execute for the current request. (NotNull)
     * @param cause The exception thrown by (basically) action execute, might be translated. (NotNull)
     * @return The API response, which is for e.g. JSON or XML. (NotNull)
     */
    ApiResponse handleApplicationException(ApiFailureResource resource, ActionRuntimeMeta meta, RuntimeException cause);

    /**
     * Handle API failure when system exception. (Not Required)
     * @param response The HTTP response that is not committed yet. (NotNull)
     * @param meta The runtime meta of action execute for the current request. (NotNull)
     * @param cause The exception thrown by (basically) action execute, might be translated. (NotNull)
     * @return The optional API response, which is for e.g. JSON or XML. (NotNull, EmptyAllowed: if empty, default handling about it)
     */
    OptionalThing<ApiResponse> handleSystemException(HttpServletResponse response, ActionRuntimeMeta meta, Throwable cause);
}
