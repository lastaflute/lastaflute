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

import org.dbflute.bhv.proposal.callback.ExecutedSqlCounter;
import org.dbflute.hook.CallbackContext;
import org.dbflute.hook.SqlStringFilter;
import org.lastaflute.db.dbflute.accesscontext.PreparedAccessContext;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.request.ResponseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class TypicalGodHandActionEpilogue {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger LOG = LoggerFactory.getLogger(TypicalGodHandActionEpilogue.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RequestManager requestManager;
    protected final ResponseManager responseManager;
    protected final TooManySqlOption tooManySqlOption;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public TypicalGodHandActionEpilogue(TypicalGodHandResource resource, TooManySqlOption tooManySqlOption) {
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
        clearCallbackContext();
        clearPreparedAccessContext();
    }

    protected void arrangeNoCacheResponseWhenJsp(ActionRuntime runtime) {
        responseManager.addNoCache();
    }

    // ===================================================================================
    //                                                                    Callback Context
    //                                                                    ================
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
        final int limitCountOfSql = getLimitCountOfSql(runtime);
        if (limitCountOfSql >= 0 && counter.getTotalCountOfSql() > limitCountOfSql) {
            // minus means no check here, by-annotation cannot specify it, can only as default limit
            // if it needs to specify it by-annotation, enough to set large size
            handleTooManySqlExecution(runtime, counter, limitCountOfSql);
        }
        final String exp = counter.toLineDisp();
        requestManager.setAttribute(RequestManager.DBFLUTE_SQL_COUNT_KEY, exp); // logged by logging filter
    }

    /**
     * Handle too many SQL executions.
     * @param runtime The runtime meta of action execute. (NotNull)
     * @param sqlCounter The counter object for SQL executions. (NotNull)
     * @param limitCountOfSql The limit of SQL count (executions) for the action execute. (NotMinus: already checked here)
     */
    protected void handleTooManySqlExecution(ActionRuntime runtime, ExecutedSqlCounter sqlCounter, int limitCountOfSql) {
        final int totalCountOfSql = sqlCounter.getTotalCountOfSql();
        final String actionDisp = buildActionDisp(runtime);
        LOG.warn("*Too many SQL executions: {}/{} in {}", totalCountOfSql, limitCountOfSql, actionDisp);
    }

    protected String buildActionDisp(ActionRuntime runtime) {
        return runtime.getActionType().getSimpleName() + "@" + runtime.getExecuteMethod().getName() + "()";
    }

    /**
     * Get the limit of SQL execution. <br>
     * You can override if you need.
     * @param runtime The runtime meta of action execute. (NotNull)
     * @return The max count allowed for SQL executions. (MinusAllowed: if minus, no check)
     */
    protected int getLimitCountOfSql(ActionRuntime runtime) {
        return tooManySqlOption.getLimitCount();
    }

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
