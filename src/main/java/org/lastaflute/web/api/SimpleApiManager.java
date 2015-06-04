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

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.web.callback.ActionRuntime;
import org.lastaflute.web.direction.FwWebDirection;
import org.lastaflute.web.response.ApiResponse;
import org.lastaflute.web.servlet.request.ResponseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class SimpleApiManager implements ApiManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger LOG = LoggerFactory.getLogger(SimpleApiManager.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant directory (AD) for framework. (NotNull: after initialization) */
    @Resource
    protected FwAssistantDirector assistantDirector;

    /** The manager of JSON. (NotNull: after initialization) */
    @Resource
    protected JsonManager jsonManager;

    /** The manager of response. (NotNull: after initialization) */
    @Resource
    protected ResponseManager responseManager;

    /** The hook for API failures. (NotNull: after initialization) */
    protected ApiFailureHook apiFailureHook;

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    /**
     * Initialize this component. <br>
     * This is basically called by DI setting file.
     */
    @PostConstruct
    public synchronized void initialize() {
        final FwWebDirection direction = assistActionDirection();
        apiFailureHook = direction.assistApiFailureHook();
        showBootLogging();
    }

    protected FwWebDirection assistActionDirection() {
        return assistantDirector.assistWebDirection();
    }

    protected void showBootLogging() {
        if (LOG.isInfoEnabled()) {
            LOG.info("[API Manager]");
            LOG.info(" apiFailureHook: " + DfTypeUtil.toClassTitle(apiFailureHook));
        }
    }

    // ===================================================================================
    //                                                                    Business Failure
    //                                                                    ================
    @Override
    public ApiResponse handleLoginRequiredFailure(ApiFailureResource resource, ActionRuntime runtime, ApiLoginRedirectProvider provider) {
        return apiFailureHook.handleLoginRequiredFailure(resource, runtime, provider);
    }

    @Override
    public ApiResponse handleValidationError(ApiFailureResource resource, ActionRuntime runtime) {
        return apiFailureHook.handleValidationError(resource, runtime);
    }

    @Override
    public ApiResponse handleApplicationException(ApiFailureResource resource, ActionRuntime runtime, RuntimeException cause) {
        return apiFailureHook.handleApplicationException(resource, runtime, cause);
    }

    // ===================================================================================
    //                                                                      System Failure
    //                                                                      ==============
    @Override
    public OptionalThing<ApiResponse> handleClientException(ApiFailureResource resource, ActionRuntime runtime, RuntimeException cause) {
        return apiFailureHook.handleClientException(resource, runtime, cause);
    }

    @Override
    public OptionalThing<ApiResponse> handleServerException(ApiFailureResource resource, ActionRuntime runtime, Throwable cause) {
        return apiFailureHook.handleServerException(resource, runtime, cause);
    }
}
