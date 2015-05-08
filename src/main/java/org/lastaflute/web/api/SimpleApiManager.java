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
import javax.servlet.http.HttpServletResponse;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.web.callback.ActionRuntimeMeta;
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

    /** The provider of API result. (NotNull: after initialization) */
    protected ApiResultProvider apiResultProvider;

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
        final ApiResultProvider assistedProvider = direction.assistApiResultProvider();
        if (assistedProvider != null) {
            apiResultProvider = assistedProvider;
        } else {
            apiResultProvider = new UnsupportedApiResultProvider();
        }
        showBootLogging();
    }

    protected FwWebDirection assistActionDirection() {
        return assistantDirector.assistWebDirection();
    }

    protected void showBootLogging() {
        if (LOG.isInfoEnabled()) {
            LOG.info("[API Manager]");
            LOG.info(" apiResultProvider: " + DfTypeUtil.toClassTitle(apiResultProvider));
        }
    }

    // ===================================================================================
    //                                                                       Create Result
    //                                                                       =============
    @Override
    public ApiResponse prepareLoginRequiredFailure(ApiResultResource resource, ActionRuntimeMeta meta) {
        return apiResultProvider.prepareLoginRequiredFailure(resource, meta);
    }

    @Override
    public ApiResponse prepareValidationError(ApiResultResource resource, ActionRuntimeMeta meta) {
        return apiResultProvider.prepareValidationError(resource, meta);
    }

    @Override
    public ApiResponse prepareApplicationException(ApiResultResource resource, ActionRuntimeMeta meta, RuntimeException cause) {
        return apiResultProvider.prepareApplicationException(resource, meta, cause);
    }

    @Override
    public OptionalThing<ApiResponse> prepareSystemException(HttpServletResponse response, ActionRuntimeMeta meta, Throwable cause) {
        return apiResultProvider.prepareSystemException(response, meta, cause);
    }
}
