/*
 * Copyright 2015-2017 the original author or authors.
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
import org.lastaflute.web.api.ApiManager;
import org.lastaflute.web.exception.ApplicationExceptionHandler;
import org.lastaflute.web.login.LoginManager;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.session.SessionManager;

/**
 * @author jflute
 */
public class GodHandMonologue {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final HtmlResponse DEFAULT_SHOW_ERRORS_FORWARD = HtmlResponse.fromForwardPath("/error/show_errors.jsp");

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final FwAssistantDirector assistantDirector;
    protected final RequestManager requestManager;
    protected final SessionManager sessionManager;
    protected final OptionalThing<LoginManager> loginManager;
    protected final ApiManager apiManager;
    protected final ExceptionTranslator exceptionTranslator;
    protected final EmbeddedMessageKeySupplier typicalKeySupplier;
    protected final ApplicationExceptionHandler applicationExceptionHandler;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public GodHandMonologue(GodHandResource resource, EmbeddedMessageKeySupplier typicalKeySupplier,
            ApplicationExceptionHandler applicationExceptionHandler) {
        this.assistantDirector = resource.getAssistantDirector();
        this.requestManager = resource.getRequestManager();
        this.sessionManager = resource.getSessionManager();
        this.loginManager = resource.getLoginManager();
        this.apiManager = resource.getApiManager();
        this.exceptionTranslator = resource.getExceptionTranslator();
        this.typicalKeySupplier = typicalKeySupplier;
        this.applicationExceptionHandler = applicationExceptionHandler;
    }

    // ===================================================================================
    //                                                                           Monologue
    //                                                                           =========
    public ActionResponse performMonologue(ActionRuntime runtime) {
        final RuntimeException cause = runtime.getFailureCause();
        RuntimeException translated = null;
        try {
            translateException(cause);
        } catch (RuntimeException e) {
            translated = e;
        }
        final RuntimeException handlingEx = translated != null ? translated : cause;
        final ActionResponse response = handleApplicationException(runtime, handlingEx);
        if (response.isDefined()) {
            return response;
        }
        if (translated != null) {
            throw translated;
        }
        return ActionResponse.undefined();
    }

    protected void translateException(RuntimeException cause) {
        exceptionTranslator.translateException(cause);
    }

    protected ActionResponse handleApplicationException(ActionRuntime runtime, RuntimeException cause) {
        final ApplicationExceptionResolver resolver //
                = new ApplicationExceptionResolver(assistantDirector //
                        , requestManager, sessionManager, loginManager //
                        , apiManager, typicalKeySupplier, applicationExceptionHandler);
        return resolver.resolve(runtime, cause);
    }
}
