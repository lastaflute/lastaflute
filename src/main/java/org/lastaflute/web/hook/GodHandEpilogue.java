/*
 * Copyright 2015-2018 the original author or authors.
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

import org.dbflute.bhv.proposal.callback.ExecutedSqlCounter;
import org.dbflute.hook.CallbackContext;
import org.dbflute.hook.SqlStringFilter;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.core.mail.PostedMailCounter;
import org.lastaflute.core.mail.RequestedMailCount;
import org.lastaflute.core.remoteapi.CalledRemoteApiCounter;
import org.lastaflute.core.remoteapi.RequestedRemoteApiCount;
import org.lastaflute.db.dbflute.accesscontext.PreparedAccessContext;
import org.lastaflute.db.dbflute.callbackcontext.traceablesql.RequestedSqlCount;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.request.ResponseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class GodHandEpilogue {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(GodHandEpilogue.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RequestManager requestManager;
    protected final ResponseManager responseManager;
    protected final TooManySqlOption tooManySqlOption;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public GodHandEpilogue(GodHandResource resource, TooManySqlOption tooManySqlOption) {
        this.requestManager = resource.getRequestManager();
        this.responseManager = resource.getResponseManager();
        this.tooManySqlOption = tooManySqlOption;
    }

    // ===================================================================================
    //                                                                            Prologue
    //                                                                            ========
    public void performEpilogue(ActionRuntime runtime) { // fixed process
        if (runtime.isForwardToHtml()) {
            arrangeNoCacheResponseWhenJsp(runtime);
        }
        handleSqlCount(runtime);
        handleMailCount(runtime);
        handleRemoteApiCount(runtime);
        clearCallbackContext();
        clearPreparedAccessContext();
    }

    // ===================================================================================
    //                                                                            No Cache
    //                                                                            ========
    protected void arrangeNoCacheResponseWhenJsp(ActionRuntime runtime) {
        responseManager.addNoCache();
    }

    // ===================================================================================
    //                                                                           SQL Count
    //                                                                           =========
    /**
     * Handle count of SQL execution in the request.
     * @param runtime The runtime meta of action execute. (NotNull)
     */
    protected void handleSqlCount(ActionRuntime runtime) {
        final CallbackContext context = CallbackContext.getCallbackContextOnThread();
        if (context == null) {
            return;
        }
        final SqlStringFilter filter = context.getSqlStringFilter();
        if (filter == null || !(filter instanceof ExecutedSqlCounter)) {
            return;
        }
        final ExecutedSqlCounter counter = ((ExecutedSqlCounter) filter);
        final int sqlExecutionCountLimit = getSqlExecutionCountLimit(runtime);
        if (sqlExecutionCountLimit >= 0 && counter.getTotalCountOfSql() > sqlExecutionCountLimit) {
            // minus means no check here, by-annotation cannot specify it, can only as default limit
            // if it needs to specify it by-annotation, enough to set large size
            handleTooManySqlExecution(runtime, counter, sqlExecutionCountLimit);
        }
        saveRequestedSqlCount(counter);
    }

    /**
     * Handle too many SQL executions.
     * @param runtime The runtime meta of action execute. (NotNull)
     * @param sqlCounter The counter object for SQL executions. (NotNull)
     * @param sqlExecutionCountLimit The limit of SQL execution count for the action execute. (NotMinus: already checked here)
     */
    protected void handleTooManySqlExecution(ActionRuntime runtime, ExecutedSqlCounter sqlCounter, int sqlExecutionCountLimit) {
        final int totalCountOfSql = sqlCounter.getTotalCountOfSql();
        final String actionDisp = buildActionDisp(runtime);
        logger.warn("*Too many SQL executions: {}/{} in {}", totalCountOfSql, sqlExecutionCountLimit, actionDisp);
    }

    protected String buildActionDisp(ActionRuntime runtime) {
        return runtime.getActionType().getSimpleName() + "@" + runtime.getExecuteMethod().getName() + "()";
    }

    /**
     * Get the limit of SQL execution. <br>
     * You can override if you need.
     * @param runtime The runtime meta of action execute. (NotNull)
     * @return The limit of SQL execution count. (MinusAllowed: if minus, no check)
     */
    protected int getSqlExecutionCountLimit(ActionRuntime runtime) {
        return tooManySqlOption.getSqlExecutionCountLimit();
    }

    protected void saveRequestedSqlCount(ExecutedSqlCounter counter) {
        requestManager.setAttribute(LastaWebKey.DBFLUTE_SQL_COUNT_KEY, createRequestedSqlCount(counter)); // logged by logging filter
    }

    protected RequestedSqlCount createRequestedSqlCount(ExecutedSqlCounter counter) {
        return new RequestedSqlCount(counter); // as snapshot
    }

    // ===================================================================================
    //                                                                               Mail
    //                                                                              ======
    /**
     * Handle count of mail posting in the request.
     * @param runtime The runtime meta of action execute. (NotNull)
     */
    protected void handleMailCount(ActionRuntime runtime) {
        if (ThreadCacheContext.exists()) {
            final PostedMailCounter counter = ThreadCacheContext.findMailCounter();
            if (counter != null) {
                saveRequestedMailCount(counter);
            }
        }
    }

    protected void saveRequestedMailCount(PostedMailCounter counter) {
        requestManager.setAttribute(LastaWebKey.MAILFLUTE_MAIL_COUNT_KEY, createRequestedMailCount(counter));
    }

    protected RequestedMailCount createRequestedMailCount(PostedMailCounter counter) {
        return new RequestedMailCount(counter); // as snapshot
    }

    // ===================================================================================
    //                                                                           RemoteApi
    //                                                                           =========
    /**
     * Handle count of remoteApi calling in the request.
     * @param runtime The runtime meta of action execute. (NotNull)
     */
    protected void handleRemoteApiCount(ActionRuntime runtime) {
        if (ThreadCacheContext.exists()) {
            final CalledRemoteApiCounter counter = ThreadCacheContext.findRemoteApiCounter();
            if (counter != null) {
                saveRequestedRemoteApiCount(counter);
            }
        }
    }

    protected void saveRequestedRemoteApiCount(CalledRemoteApiCounter counter) {
        requestManager.setAttribute(LastaWebKey.REMOTEAPI_COUNT_KEY, createRequestRemoteApiCount(counter));
    }

    protected RequestedRemoteApiCount createRequestRemoteApiCount(CalledRemoteApiCounter counter) {
        return new RequestedRemoteApiCount(counter); // as snapshot
    }

    // ===================================================================================
    //                                                                    Callback Context
    //                                                                    ================
    /**
     * Clear callback context. <br>
     * This is called by callback process so you should NOT call this directly in your action.
     */
    protected void clearCallbackContext() {
        CallbackContext.clearSqlStringFilterOnThread();
        CallbackContext.clearSqlFireHookOnThread();
    }

    // ===================================================================================
    //                                                                      Access Context
    //                                                                      ==============
    /**
     * Clear prepared access context. <br>
     * This is called by callback process so you should NOT call this directly in your action.
     */
    protected void clearPreparedAccessContext() { // called by callback
        PreparedAccessContext.clearAccessContextOnThread();
    }
}
