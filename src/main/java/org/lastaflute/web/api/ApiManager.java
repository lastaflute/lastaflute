/*
 * Copyright 2015-2020 the original author or authors.
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
     * Handle API failure when validation error. <br>
     * The hookFinally() of action hook will be called after this. <br>
     * This method is called in action transaction (always rolled-back).
     * @param resource The resource of API result, basically contains validation error messages. (NotNull)
     * @return The API response, which is for e.g. JSON or XML. (NotNull)
     */
    ApiResponse handleValidationError(ApiFailureResource resource);

    /**
     * Handle API failure when application exception. <br>
     * The hookFinally() of action hook will be called after this.
     * @param resource The resource of API result, may contain error messages (e.g. embedded in exception). (NotNull)
     * @param cause The application exception thrown by (basically) action execute, might be translated. (NotNull)
     * @return The API response, which is for e.g. JSON or XML. (NotNull)
     */
    ApiResponse handleApplicationException(ApiFailureResource resource, RuntimeException cause);

    // ===================================================================================
    //                                                                      System Failure
    //                                                                      ==============
    /**
     * Handle API failure when client exception, e.g. 404 not found, 400 bad request. (Not Required) <br>
     * This hook can be called if action found, so also use Servlet container's error-page if it needs. <br>
     * HTTP status will be automatically sent by framework's hook so empty response allowed. <br>
     * The hookFinally() of action hook NOT always be called after this, depends on occurrence place.
     * @param resource The resource of API result, may contain error messages (e.g. embedded in exception). (NotNull)
     * @param cause The client exception thrown by (basically) action execute, might be translated. (NotNull)
     * @return The optional API response, which is for e.g. JSON or XML. (NotNull: if empty, default handling about it)
     */
    OptionalThing<ApiResponse> handleClientException(ApiFailureResource resource, RuntimeException cause);

    /**
     * Handle API failure when server exception, e.g. 500 server error. (Not Required) <br>
     * This hook can be called if action found, so also use Servlet container's error-page if it needs. <br>
     * HTTP status will be automatically sent by framework's hook so empty response allowed. <br>
     * The hookFinally() of action hook NOT always be called after this, depends on occurrence place.
     * @param resource The resource of API result, without error messages, you can get request manager from it. (NotNull)
     * @param cause The system exception thrown by (basically) action execute, might be translated. (NotNull)
     * @return The optional API response, which is for e.g. JSON or XML. (NotNull: if empty, default handling about it)
     */
    OptionalThing<ApiResponse> handleServerException(ApiFailureResource resource, Throwable cause);
}
