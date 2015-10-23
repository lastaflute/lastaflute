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
package org.lastaflute.web.callback;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.exception.ExceptionTranslator;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.web.api.ApiManager;
import org.lastaflute.web.login.LoginManager;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.request.ResponseManager;
import org.lastaflute.web.servlet.session.SessionManager;
import org.lastaflute.web.token.DoubleSubmitManager;

/**
 * @author jflute
 */
public class TypicalGodHandResource {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final MessageManager messageManager;
    protected final ExceptionTranslator exceptionTranslator;
    protected final RequestManager requestManager;
    protected final ResponseManager responseManager;
    protected final SessionManager sessionManager;
    protected final OptionalThing<LoginManager> loginManager;
    protected final ApiManager apiManager;
    protected final DoubleSubmitManager doubleSubmitManager;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public TypicalGodHandResource(MessageManager messageManager, ExceptionTranslator exceptionTranslator, RequestManager requestManager,
            ResponseManager responseManager, SessionManager sessionManager, OptionalThing<LoginManager> loginManager, ApiManager apiManager,
            DoubleSubmitManager doubleSubmitManager) {
        this.messageManager = messageManager;
        this.exceptionTranslator = exceptionTranslator;
        this.requestManager = requestManager;
        this.responseManager = responseManager;
        this.sessionManager = sessionManager;
        this.loginManager = loginManager;
        this.apiManager = apiManager;
        this.doubleSubmitManager = doubleSubmitManager;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
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

    public DoubleSubmitManager getDoubleSubmitManager() {
        return doubleSubmitManager;
    }
}
