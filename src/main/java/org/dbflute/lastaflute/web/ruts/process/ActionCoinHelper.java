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
package org.dbflute.lastaflute.web.ruts.process;

import org.dbflute.hook.AccessContext;
import org.dbflute.hook.CallbackContext;
import org.dbflute.lastaflute.core.direction.FwAssistantDirector;
import org.dbflute.lastaflute.core.magic.TransactionTimeContext;
import org.dbflute.lastaflute.db.dbflute.accesscontext.PreparedAccessContext;
import org.dbflute.lastaflute.web.LastaWebKey;
import org.dbflute.lastaflute.web.ruts.config.ModuleConfig;
import org.dbflute.lastaflute.web.ruts.message.ActionMessages;
import org.dbflute.lastaflute.web.servlet.request.RequestManager;
import org.dbflute.lastaflute.web.servlet.session.SessionManager;

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
