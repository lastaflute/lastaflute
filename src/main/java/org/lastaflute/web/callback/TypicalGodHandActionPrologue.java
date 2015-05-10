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

import java.lang.reflect.Method;
import java.util.function.Supplier;

import org.dbflute.bhv.proposal.callback.ExecutedSqlCounter;
import org.dbflute.bhv.proposal.callback.TraceableSqlAdditionalInfoProvider;
import org.dbflute.hook.AccessContext;
import org.dbflute.hook.CallbackContext;
import org.dbflute.hook.SqlFireHook;
import org.dbflute.hook.SqlStringFilter;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.db.dbflute.accesscontext.AccessContextArranger;
import org.lastaflute.db.dbflute.accesscontext.AccessContextResource;
import org.lastaflute.db.dbflute.accesscontext.PreparedAccessContext;
import org.lastaflute.db.dbflute.callbackcontext.RomanticTraceableSqlFireHook;
import org.lastaflute.db.dbflute.callbackcontext.RomanticTraceableSqlStringFilter;
import org.lastaflute.web.api.ApiFailureResource;
import org.lastaflute.web.api.ApiManager;
import org.lastaflute.web.login.LoginHandlingResource;
import org.lastaflute.web.login.LoginManager;
import org.lastaflute.web.login.UserBean;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class TypicalGodHandActionPrologue {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(TypicalGodHandActionPrologue.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RequestManager requestManager;
    protected final SessionManager sessionManager;
    protected final OptionalThing<LoginManager> loginManager;
    protected final ApiManager apiManager;
    protected final AccessContextArranger accessContextArranger;
    protected final Supplier<OptionalThing<? extends UserBean>> userBeanSupplier;
    protected final Supplier<String> appTypeSupplier;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public TypicalGodHandActionPrologue(TypicalGodHandResource resource, AccessContextArranger accessContextArranger,
            Supplier<OptionalThing<? extends UserBean>> userBeanSupplier, Supplier<String> appTypeSupplier) {
        this.requestManager = resource.getRequestManager();
        this.sessionManager = resource.getSessionManager();
        this.loginManager = resource.getLoginManager();
        this.apiManager = resource.getApiManager();
        this.accessContextArranger = accessContextArranger;
        this.userBeanSupplier = userBeanSupplier;
        this.appTypeSupplier = appTypeSupplier;
    }

    // ===================================================================================
    //                                                                            Prologue
    //                                                                            ========
    public ActionResponse performPrologue(ActionRuntimeMeta executeMeta) { // fixed process
        arrangeThreadCacheContextBasicItem(executeMeta);
        arrangePreparedAccessContext(executeMeta);
        arrangeCallbackContext(executeMeta); // should be after access-context (using access context's info)

        // should be after access-context (may have update)
        final ActionResponse redirectTo = handleLoginRequiredCheck(executeMeta);
        if (redirectTo.isPresent()) {
            return redirectTo;
        }
        arrangeThreadCacheContextLoginItem(executeMeta);
        return ActionResponse.empty();
    }

    protected void arrangeThreadCacheContextBasicItem(ActionRuntimeMeta executeMeta) {
        if (ThreadCacheContext.exists()) { // basically true, just in case
            ThreadCacheContext.registerRequestPath(requestManager.getRequestPathAndQuery());
            ThreadCacheContext.registerEntryMethod(executeMeta.getExecuteMethod());
        }
    }

    protected void arrangeThreadCacheContextLoginItem(ActionRuntimeMeta executeMeta) {
        if (ThreadCacheContext.exists()) { // basically true, just in case
            ThreadCacheContext.registerUserBean(userBeanSupplier.get().orElse(null)); // basically for asynchronous
        }
    }

    // ===================================================================================
    //                                                                      Access Context
    //                                                                      ==============
    /**
     * Arrange prepared access context for DBFlute, which is used for common columns setup. <br>
     * This is called by callback process so you should NOT call this directly in your action.
     * @param executeMeta The meta of action execute. (NotNull)
     */
    protected void arrangePreparedAccessContext(ActionRuntimeMeta executeMeta) { // called by callback
        final AccessContextResource resource = createAccessContextResource(executeMeta);
        final AccessContext accessContext = accessContextArranger.arrangePreparedAccessContext(resource);
        PreparedAccessContext.setAccessContextOnThread(accessContext);
    }

    protected AccessContextResource createAccessContextResource(ActionRuntimeMeta runtimeMeta) {
        final String classTitle = DfTypeUtil.toClassTitle(runtimeMeta.getActionClass());
        return newAccessContextResource(classTitle, runtimeMeta.getExecuteMethod());
    }

    protected AccessContextResource newAccessContextResource(String classTitle, Method method) {
        return new AccessContextResource(classTitle, method);
    }

    /**
     * Clear prepared access context. <br>
     * This is called by callback process so you should NOT call this directly in your action.
     */
    protected void clearPreparedAccessContext() { // called by callback
        PreparedAccessContext.clearAccessContextOnThread();
    }

    // ===================================================================================
    //                                                                    Callback Context
    //                                                                    ================
    /**
     * Arrange callback context for DBFlute, which is used for several purpose. <br>
     * This is called by callback process so you should NOT call this directly in your action.
     * @param executeMeta The meta of action execute. (NotNull)
     */
    protected void arrangeCallbackContext(final ActionRuntimeMeta executeMeta) {
        final SqlFireHook sqlFireHook = createSqlFireHook(executeMeta);
        CallbackContext.setSqlFireHookOnThread(sqlFireHook);
        final SqlStringFilter filter = createSqlStringFilter(executeMeta);
        CallbackContext.setSqlStringFilterOnThread(filter);
    }

    /**
     * Create the filter of SQL string for DBFlute.
     * @param executeMeta The meta of action execute. (NotNull)
     * @return The hook of SQL fire. (NullAllowed: if null, no hook)
     */
    protected SqlFireHook createSqlFireHook(ActionRuntimeMeta executeMeta) {
        return newRomanticTraceableSqlFireHook();
    }

    protected RomanticTraceableSqlFireHook newRomanticTraceableSqlFireHook() {
        return new RomanticTraceableSqlFireHook();
    }

    /**
     * Create the filter of SQL string for DBFlute.
     * @param executeMeta The meta of action execute. (NotNull)
     * @return The filter of SQL string. (NullAllowed: if null, no filter)
     */
    protected SqlStringFilter createSqlStringFilter(final ActionRuntimeMeta executeMeta) {
        final Method actionMethod = executeMeta.getExecuteMethod();
        return newRomanticTraceableSqlStringFilter(actionMethod, new TraceableSqlAdditionalInfoProvider() {
            @Override
            public String provide() { // lazy because it may be auto-login later
                return buildSqlMarkingAdditionalInfo();
            }
        });
    }

    protected RomanticTraceableSqlStringFilter newRomanticTraceableSqlStringFilter(Method actionMethod,
            TraceableSqlAdditionalInfoProvider additionalInfoProvider) {
        return new RomanticTraceableSqlStringFilter(actionMethod, additionalInfoProvider);
    }

    /**
     * Build string for additional info of SQL marking.
     * @return The string expression of additional info. (NullAllowed: if null, no additional info)
     */
    protected String buildSqlMarkingAdditionalInfo() {
        return "{" + appTypeSupplier.get() + "}"; // it doesn't contain user ID for SQL cache in DBMS
    }

    /**
     * Handle count of SQL execution in the request.
     * @param runtimeMeta The runtime meta of action execute. (NotNull)
     */
    protected void handleSqlCount(final ActionRuntimeMeta runtimeMeta) {
        final CallbackContext context = CallbackContext.getCallbackContextOnThread();
        if (context == null) {
            return;
        }
        final SqlStringFilter filter = context.getSqlStringFilter();
        if (filter == null || !(filter instanceof ExecutedSqlCounter)) {
            return;
        }
        final ExecutedSqlCounter counter = ((ExecutedSqlCounter) filter);
        final int limitCountOfSql = getLimitCountOfSql(runtimeMeta);
        if (limitCountOfSql >= 0 && counter.getTotalCountOfSql() > limitCountOfSql) {
            handleTooManySqlExecution(runtimeMeta, counter);
        }
        final String exp = counter.toLineDisp();
        requestManager.setAttribute(RequestManager.DBFLUTE_SQL_COUNT_KEY, exp); // logged by logging filter
    }

    /**
     * Handle too many SQL executions.
     * @param runtimeMeta The runtime meta of action execute. (NotNull)
     * @param sqlCounter The counter object for SQL executions. (NotNull)
     */
    protected void handleTooManySqlExecution(ActionRuntimeMeta runtimeMeta, final ExecutedSqlCounter sqlCounter) {
        final String actionDisp = buildActionDisp(runtimeMeta);
        logger.warn("*Too many SQL executions: " + sqlCounter.getTotalCountOfSql() + " in " + actionDisp);
    }

    protected String buildActionDisp(ActionRuntimeMeta runtimeMeta) {
        return runtimeMeta.getActionClass().getSimpleName() + "." + runtimeMeta.getExecuteMethod().getName() + "()";
    }

    /**
     * Get the limit count of SQL execution. <br>
     * You can override if you need.
     * @param executeMeta The meta of action execute. (NotNull)
     * @return The max count allowed for SQL executions. (MinusAllowed: if minus, no check)
     */
    protected int getLimitCountOfSql(ActionRuntimeMeta executeMeta) {
        return 30; // as default
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
    //                                                                      Login Handling
    //                                                                      ==============
    /**
     * Handle the login required check for the requested action.
     * @param runtimeMeta The runtime meta of action execute to determine required action. (NotNull)
     * @return The forward path, basically for login-redirect. (NullAllowed)
     */
    protected ActionResponse handleLoginRequiredCheck(ActionRuntimeMeta runtimeMeta) {
        return loginManager.map(nager -> {
            final LoginHandlingResource resource = createLogingHandlingResource(runtimeMeta);
            return nager.checkLoginRequired(resource).map(forwardTo -> {
                if (runtimeMeta.isApiAction()) {
                    return dispatchApiLoginRequiredFailure(runtimeMeta);
                } else {
                    return HtmlResponse.fromRedirectPath(forwardTo);
                }
            }).orElse(HtmlResponse.empty());
        }).orElseGet(() -> {
            return ActionResponse.empty();
        });
    }

    protected ActionResponse dispatchApiLoginRequiredFailure(ActionRuntimeMeta runtimeMeta) {
        final ApiFailureResource resource = createApiLoginRequiredFailureResource();
        return apiManager.handleLoginRequiredFailure(resource, runtimeMeta);
    }

    protected ApiFailureResource createApiLoginRequiredFailureResource() {
        return new ApiFailureResource(sessionManager.errors().get(), requestManager);
    }

    protected LoginHandlingResource createLogingHandlingResource(ActionRuntimeMeta runtimeMeta) {
        return new LoginHandlingResource(runtimeMeta);
    }
}
