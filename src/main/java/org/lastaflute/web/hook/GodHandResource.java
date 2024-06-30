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
package org.lastaflute.web.hook;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.exception.ExceptionTranslator;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.web.api.ApiManager;
import org.lastaflute.web.login.LoginManager;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.request.ResponseManager;
import org.lastaflute.web.servlet.session.SessionManager;

/**
 * @author jflute
 */
public class GodHandResource {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final FwAssistantDirector assistantDirector;
    protected final TimeManager timeManager;
    protected final MessageManager messageManager;
    protected final ExceptionTranslator exceptionTranslator;
    protected final RequestManager requestManager;
    protected final ResponseManager responseManager;
    protected final SessionManager sessionManager;
    protected final OptionalThing<LoginManager> loginManager;
    protected final ApiManager apiManager;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public GodHandResource(FwAssistantDirector assistantDirector, TimeManager timeManager, MessageManager messageManager,
            ExceptionTranslator exceptionTranslator, RequestManager requestManager, ResponseManager responseManager,
            SessionManager sessionManager, OptionalThing<LoginManager> loginManager, ApiManager apiManager) {
        this.assistantDirector = assistantDirector;
        this.timeManager = timeManager;
        this.messageManager = messageManager;
        this.exceptionTranslator = exceptionTranslator;
        this.requestManager = requestManager;
        this.responseManager = responseManager;
        this.sessionManager = sessionManager;
        this.loginManager = loginManager;
        this.apiManager = apiManager;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public FwAssistantDirector getAssistantDirector() {
        return assistantDirector;
    }

    public TimeManager getTimeManager() {
        return timeManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public ExceptionTranslator getExceptionTranslator() {
        return exceptionTranslator;
    }

    public RequestManager getRequestManager() {
        return requestManager;
    }

    public ResponseManager getResponseManager() {
        return responseManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public OptionalThing<LoginManager> getLoginManager() {
        return loginManager;
    }

    public ApiManager getApiManager() {
        return apiManager;
    }
}
