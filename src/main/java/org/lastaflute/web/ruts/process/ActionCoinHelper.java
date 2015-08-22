/*
 * Copyright 2014-2015 the original author or authors.
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
package org.lastaflute.web.ruts.process;

import javax.servlet.http.HttpServletResponse;

import org.dbflute.hook.AccessContext;
import org.dbflute.hook.CallbackContext;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.magic.TransactionTimeContext;
import org.lastaflute.db.dbflute.accesscontext.PreparedAccessContext;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.api.ApiFailureResource;
import org.lastaflute.web.api.ApiManager;
import org.lastaflute.web.callback.ActionRuntime;
import org.lastaflute.web.ruts.config.ModuleConfig;
import org.lastaflute.web.ruts.message.ActionMessages;
import org.lastaflute.web.servlet.filter.RequestLoggingFilter;
import org.lastaflute.web.servlet.filter.RequestLoggingFilter.RequestClientErrorException;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.session.SessionManager;
import org.lastaflute.web.util.LaActionRuntimeUtil;

/**
 * @author modified by jflute (originated in Seasar and Struts)
 */
public class ActionCoinHelper {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    /**
     * <p>The request attribute under which the path information is stored for
     * processing during a <code>RequestDispatcher.include</code> call.</p>
     */
    public static final String INCLUDE_PATH_INFO = "javax.servlet.include.path_info";

    /**
     * <p>The request attribute under which the servlet path information is stored
     * for processing during a <code>RequestDispatcher.include</code> call.</p>
     */
    public static final String INCLUDE_SERVLET_PATH = "javax.servlet.include.servlet_path";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ModuleConfig moduleConfig;
    protected final FwAssistantDirector assistantDirector;
    protected final RequestManager requestManager;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionCoinHelper(ModuleConfig moduleConfig, FwAssistantDirector assistantDirector, RequestManager requestManager) {
        this.moduleConfig = moduleConfig;
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
            RequestLoggingFilter.setClientErrorHandlerOnThread((request, response, cause) -> {
                dispatchApiClientException(runtime, reflector, cause);
            }); // cleared at logging filter's finally
        }
    }

    protected void dispatchApiClientException(ActionRuntime runtime, ActionResponseReflector reflector, RequestClientErrorException cause) {
        if (canHandleApiException(runtime)) { // check API action just in case
            getApiManager().handleClientException(createApiFailureResource(runtime), cause).ifPresent(apiRes -> {
                if (apiRes.getHttpStatus() == null) { // no specified
                    apiRes.httpStatus(cause.getErrorStatus()); // use thrown status
                }
                reflector.reflect(apiRes); // empty journey so ignore return
            });
            runtime.clearDisplayData(); // remove (possible) large data just in case
        }
    }

    // -----------------------------------------------------
    //                                          Server Error
    //                                          ------------
    public void prepareRequestServerErrorHandlingIfApi(ActionRuntime runtime, ActionResponseReflector reflector) {
        if (runtime.isApiExecute()) {
            RequestLoggingFilter.setServerErrorHandlerOnThread((request, response, cause) -> {
                dispatchApiServerException(runtime, reflector, cause);
            }); // cleared at logging filter's finally
        }
    }

    protected void dispatchApiServerException(ActionRuntime runtime, ActionResponseReflector reflector, Throwable cause) {
        if (canHandleApiException(runtime)) { // check API action just in case
            getApiManager().handleServerException(createApiFailureResource(runtime), cause).ifPresent(apiRes -> {
                if (apiRes.getHttpStatus() == null) { // no specified
                    apiRes.httpStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // use fixed status
                }
                reflector.reflect(apiRes); // empty journey so ignore return
            });
            runtime.clearDisplayData(); // remove (possible) large data just in case
        }
    }

    // -----------------------------------------------------
    //                                          Assist Logic
    //                                          ------------
    protected boolean canHandleApiException(ActionRuntime runtime) {
        return runtime.isApiExecute() && !requestManager.getResponseManager().isCommitted();
    }

    protected ApiFailureResource createApiFailureResource(ActionRuntime runtime) {
        return new ApiFailureResource(runtime, OptionalThing.empty(), requestManager);
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
        sessionManager.getAttribute(key, ActionMessages.class).filter(messages -> messages.isAccessed()).ifPresent(messages -> {
            sessionManager.removeAttribute(key);
        });
    }

    protected void removeInformationUsedMessage(SessionManager sessionManager) {
        final String key = LastaWebKey.ACTION_INFO_KEY;
        sessionManager.getAttribute(key, ActionMessages.class).filter(messages -> messages.isAccessed()).ifPresent(messages -> {
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
