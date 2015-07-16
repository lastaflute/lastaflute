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

import org.dbflute.optional.OptionalThing;
import org.lastaflute.web.callback.ActionRuntime;
import org.lastaflute.web.response.ApiResponse;

/**
 * The manager of API call.
 * @author jflute
 */
public interface ApiManager {

    // ===================================================================================
    //                                                                    Business Failure
    //                                                                    ================
    /**
     * Handle API failure when login required failure. <br>
     * The hookFinally() of action hook will be called after this.
     * @param resource The resource of API result for login, contains e.g. error messages if it exists. (NotNull)
     * @param errors The optional action message for errors, but basically no errors. (NullAllowed)
     * @param runtime The runtime meta of action execute for the current request. (NotNull)
     * @param provider The provider of login redirect info, e.g. redirect path /signin/. (NotNull)
     * @return The API response, which is for e.g. JSON or XML. (NotNull)
     */
    ApiResponse handleLoginRequiredFailure(ApiFailureResource resource, ActionRuntime runtime, ApiLoginRedirectProvider provider);

    /**
     * Handle API failure when validation error. <br>
     * The hookFinally() of action hook will be called after this. <br>
     * This method is called in action transaction (always rolled-back).
     * @param resource The resource of API result, contains e.g. error messages if it exists. (NotNull)
     * @param runtime The runtime meta of action execute for the current request. (NotNull)
     * @return The API response, which is for e.g. JSON or XML. (NotNull)
     */
    ApiResponse handleValidationError(ApiFailureResource resource, ActionRuntime runtime);

    /**
     * Handle API failure when application exception. <br>
     * The hookFinally() of action hook will be called after this.
     * @param resource The resource of API result, contains e.g. error messages if it exists. (NotNull)
     * @param runtime The runtime meta of action execute for the current request. (NotNull)
     * @param cause The exception thrown by (basically) action execute, might be translated. (NotNull)
     * @return The API response, which is for e.g. JSON or XML. (NotNull)
     */
    ApiResponse handleApplicationException(ApiFailureResource resource, ActionRuntime runtime, RuntimeException cause);

    // ===================================================================================
    //                                                                      System Failure
    //                                                                      ==============
    /**
     * Handle API failure when client exception, e.g. 404 not found, 400 bad request. (Not Required) <br>
     * The hookFinally() of action hook NOT always be called after this, depends on occurrence place.
     * @param resource The resource of API result, without error messages, you can get request manager from it. (NotNull)
     * @param runtime The runtime meta of action execute for the current request. (NotNull)
     * @param cause The exception thrown by (basically) action execute, might be translated. (NotNull)
     * @return The optional API response, which is for e.g. JSON or XML. (NotNull: if empty, default handling about it)
     */
    OptionalThing<ApiResponse> handleClientException(ApiFailureResource resource, ActionRuntime runtime, RuntimeException cause);

    /**
     * Handle API failure when server exception, e.g. 500 server error. (Not Required) <br>
     * The hookFinally() of action hook NOT always be called after this, depends on occurrence place.
     * @param resource The resource of API result, without error messages, you can get request manager from it. (NotNull)
     * @param runtime The runtime meta of action execute for the current request. (NotNull)
     * @param cause The exception thrown by (basically) action execute, might be translated. (NotNull)
     * @return The optional API response, which is for e.g. JSON or XML. (NotNull: if empty, default handling about it)
     */
    OptionalThing<ApiResponse> handleServerException(ApiFailureResource resource, ActionRuntime runtime, Throwable cause);
}
