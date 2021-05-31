/*
 * Copyright 2015-2021 the original author or authors.
 * Copyright 2015-2021 the original author or authors.
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
package org.lastaflute.web.ruts.process.actioncoins;

import javax.servlet.http.HttpServletResponse;

import org.dbflute.hook.AccessContext;
import org.dbflute.hook.CallbackContext;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.magic.TransactionTimeContext;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.db.dbflute.accesscontext.PreparedAccessContext;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.api.ApiFailureResource;
import org.lastaflute.web.api.ApiManager;
import org.lastaflute.web.exception.MessagingClientErrorException;
import org.lastaflute.web.ruts.process.ActionResponseReflector;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.servlet.filter.RequestLoggingFilter;
import org.lastaflute.web.servlet.filter.RequestLoggingFilter.RequestClientErrorException;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.session.SessionManager;
import org.lastaflute.web.util.LaActionRuntimeUtil;

/**
 * @author jflute
 */
public class ActionCoinsHelper { // keep singleton-able to be simple

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final FwAssistantDirector assistantDirector;
    protected final RequestManager requestManager;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionCoinsHelper(FwAssistantDirector assistantDirector, RequestManager requestManager) {
        this.assistantDirector = assistantDirector;
        this.requestManager = requestManager;
    }

    // ===================================================================================
    //                                                                Prepare API Handling
    //                                                                ====================
    // -----------------------------------------------------
    //                                          Client Error
    //                                          ------------
    public void prepareRequestClientErrorHandlingIfApi(ActionRuntime runtime, ActionResponseReflector reflector) {
        if (runtime.isApiExecute()) {
            registerApiClientErrorHandler(runtime, reflector);
        }
    }

    protected void registerApiClientErrorHandler(ActionRuntime runtime, ActionResponseReflector reflector) {
        RequestLoggingFilter.setClientErrorHandlerOnThread((request, response, cause) -> {
            dispatchApiClientErrorException(runtime, reflector, cause);
        }); // cleared at logging filter's finally
    }

    protected void dispatchApiClientErrorException(ActionRuntime runtime, ActionResponseReflector reflector,
            RequestClientErrorException cause) {
        if (isAlreadyCommitted()) { // just in case
            return; // can do nothing
        }
        final OptionalThing<UserMessages> optMessages = findClientErrorMessages(cause);
        final ApiFailureResource resource = createApiFailureResource(runtime, optMessages, cause);
        getApiManager().handleClientException(resource, cause).ifPresent(apiRes -> {
            if (!apiRes.getHttpStatus().isPresent()) { // no specified
                apiRes.httpStatus(cause.getErrorStatus()); // use thrown status
            }
            reflector.reflect(apiRes).getJourneyProvider().bonVoyage(); // always exists if API response
        });
        runtime.clearDisplayData(); // remove (possible) large data just in case
    }

    protected OptionalThing<UserMessages> findClientErrorMessages(RequestClientErrorException cause) {
        final OptionalThing<UserMessages> optMessages;
        if (cause instanceof MessagingClientErrorException) {
            final UserMessages messages = ((MessagingClientErrorException) cause).getMessages();
            optMessages = !messages.isEmpty() ? OptionalThing.of(messages) : OptionalThing.empty();
        } else {
            optMessages = OptionalThing.empty();
        }
        return optMessages;
    }

    // -----------------------------------------------------
    //                                          Server Error
    //                                          ------------
    public void prepareRequestServerErrorHandlingIfApi(ActionRuntime runtime, ActionResponseReflector reflector) {
        if (runtime.isApiExecute()) {
            registerServerErrorHandler(runtime, reflector);
        }
    }

    protected void registerServerErrorHandler(ActionRuntime runtime, ActionResponseReflector reflector) {
        RequestLoggingFilter.setServerErrorHandlerOnThread((request, response, cause) -> {
            dispatchApiServerException(runtime, reflector, cause);
        }); // cleared at logging filter's finally
    }

    protected void dispatchApiServerException(ActionRuntime runtime, ActionResponseReflector reflector, Throwable cause) {
        if (isAlreadyCommitted()) { // just in case
            return; // can do nothing
        }
        final ApiFailureResource resource = createApiFailureResource(runtime, OptionalThing.empty(), cause);
        getApiManager().handleServerException(resource, cause).ifPresent(apiRes -> {
            if (!apiRes.getHttpStatus().isPresent()) { // no specified
                apiRes.httpStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // use fixed status
            }
            reflector.reflect(apiRes).getJourneyProvider().bonVoyage(); // always exists if API response
        });
        runtime.clearDisplayData(); // remove (possible) large data just in case
    }

    // -----------------------------------------------------
    //                                          Assist Logic
    //                                          ------------
    protected boolean isAlreadyCommitted() {
        return requestManager.getResponseManager().isCommitted();
    }

    protected ApiFailureResource createApiFailureResource(ActionRuntime runtime, OptionalThing<UserMessages> messages, Throwable cause) {
        return new ApiFailureResource(runtime, messages, requestManager);
    }

    protected ApiManager getApiManager() {
        return requestManager.getApiManager();
    }

    // ===================================================================================
    //                                                                        Save Runtime
    //                                                                        ============
    public void saveRuntimeToRequest(ActionRuntime runtime) { // to get it from other area
        LaActionRuntimeUtil.setActionRuntime(runtime);
    }

    // ===================================================================================
    //                                                               Remove Cached Message
    //                                                               =====================
    /**
     * Remove the cached messages in session e.g. messages after redirection.
     */
    public void removeCachedMessages() {
        final SessionManager sessionManager = requestManager.getSessionManager();
        removeErrorsUsedMessage(sessionManager);
        removeInformationUsedMessage(sessionManager);
    }

    protected void removeErrorsUsedMessage(SessionManager sessionManager) {
        final String key = LastaWebKey.ACTION_ERRORS_KEY;
        sessionManager.getAttribute(key, UserMessages.class).filter(messages -> messages.isAccessed()).ifPresent(messages -> {
            sessionManager.removeAttribute(key);
        });
    }

    protected void removeInformationUsedMessage(SessionManager sessionManager) {
        final String key = LastaWebKey.ACTION_INFO_KEY;
        sessionManager.getAttribute(key, UserMessages.class).filter(messages -> messages.isAccessed()).ifPresent(messages -> {
            sessionManager.removeAttribute(key);
        });
    }

    // ===================================================================================
    //                                                                      Resolve Locale
    //                                                                      ==============
    public void resolveLocale(ActionRuntime runtime) {
        // you can customize the process e.g. accept cookie locale
        requestManager.resolveUserLocale(runtime);
        requestManager.resolveUserTimeZone(runtime);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    /**
     * Clear various contexts that might be initialized in action just in case
     */
    public void clearContextJustInCase() {
        TransactionTimeContext.clear();
        PreparedAccessContext.clearAccessContextOnThread();
        AccessContext.clearAccessContextOnThread();
        CallbackContext.clearCallbackContextOnThread();
    }
}
