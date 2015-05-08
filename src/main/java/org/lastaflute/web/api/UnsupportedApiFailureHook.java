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
 * @author jflute
 */
public class UnsupportedApiFailureHook implements ApiFailureHook {

    @Override
    public ApiResponse handleLoginRequiredFailure(ApiFailureResource resource, ActionRuntimeMeta meta) {
        throw new UnsupportedOperationException("API not supported yet: " + meta);
    }

    @Override
    public ApiResponse handleValidationError(ApiFailureResource resource, ActionRuntimeMeta meta) {
        throw new UnsupportedOperationException("API not supported yet: " + meta);
    }

    @Override
    public ApiResponse handleApplicationException(ApiFailureResource resource, ActionRuntimeMeta meta, RuntimeException cause) {
        throw new UnsupportedOperationException("API not supported yet: " + meta);
    }

    @Override
    public OptionalThing<ApiResponse> handleSystemException(HttpServletResponse response, ActionRuntimeMeta meta, Throwable cause) {
        throw new UnsupportedOperationException("API not supported yet: " + meta);
    }
}
