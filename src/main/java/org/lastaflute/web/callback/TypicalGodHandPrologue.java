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
import java.util.Locale;
import java.util.function.Supplier;

import org.dbflute.bhv.proposal.callback.ExecutedSqlCounter;
import org.dbflute.bhv.proposal.callback.TraceableSqlAdditionalInfoProvider;
import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.hook.AccessContext;
import org.dbflute.hook.CallbackContext;
import org.dbflute.hook.SqlFireHook;
import org.dbflute.hook.SqlStringFilter;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.db.dbflute.accesscontext.AccessContextArranger;
import org.lastaflute.db.dbflute.accesscontext.AccessContextResource;
import org.lastaflute.db.dbflute.accesscontext.PreparedAccessContext;
import org.lastaflute.db.dbflute.callbackcontext.RomanticTraceableSqlFireHook;
import org.lastaflute.db.dbflute.callbackcontext.RomanticTraceableSqlStringFilter;
import org.lastaflute.web.api.ApiManager;
import org.lastaflute.web.login.LoginHandlingResource;
import org.lastaflute.web.login.LoginManager;
import org.lastaflute.web.login.UserBean;
import org.lastaflute.web.login.exception.LoginRequiredException;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.session.SessionManager;
import org.lastaflute.web.token.DoubleSubmitManager;
import org.lastaflute.web.token.TxToken;
import org.lastaflute.web.token.exception.DoubleSubmitMessageNotFoundException;
import org.lastaflute.web.token.exception.DoubleSubmitRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class TypicalGodHandPrologue {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(TypicalGodHandPrologue.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final MessageManager messageManager;
    protected final RequestManager requestManager;
    protected final SessionManager sessionManager;
    protected final OptionalThing<LoginManager> loginManager;
    protected final ApiManager apiManager;
    protected final DoubleSubmitManager doubleSubmitManager;
    protected final TypicalEmbeddedKeySupplier keySupplier;
    protected final AccessContextArranger accessContextArranger;
    protected final Supplier<OptionalThing<? extends UserBean<?>>> userBeanSupplier;
    protected final Supplier<String> appTypeSupplier;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public TypicalGodHandPrologue(TypicalGodHandResource resource, TypicalEmbeddedKeySupplier keySupplier,
            AccessContextArranger accessContextArranger, Supplier<OptionalThing<? extends UserBean<?>>> userBeanSupplier,
            Supplier<String> appTypeSupplier) {
        this.messageManager = resource.getMessageManager();
        this.requestManager = resource.getRequestManager();
        this.sessionManager = resource.getSessionManager();
        this.loginManager = resource.getLoginManager();
        this.apiManager = resource.getApiManager();
        this.doubleSubmitManager = resource.getDoubleSubmitManager();
        this.keySupplier = keySupplier;
        this.accessContextArranger = accessContextArranger;
        this.userBeanSupplier = userBeanSupplier;
        this.appTypeSupplier = appTypeSupplier;
    }

    // ===================================================================================
    //                                                                            Prologue
    //                                                                            ========
    public ActionResponse performPrologue(ActionRuntime runtime) { // fixed process
        arrangeThreadCacheContextBasicItem(runtime);
        arrangePreparedAccessContext(runtime);
        arrangeCallbackContext(runtime); // should be after access-context (using access context's info)
        checkLoginRequired(runtime); // should be after access-context (may have update)
        arrangeThreadCacheContextLoginItem(runtime);
        handleDoubleSubmit(runtime);
        return ActionResponse.undefined();
    }

    protected void arrangeThreadCacheContextBasicItem(ActionRuntime runtime) {
        if (ThreadCacheContext.exists()) { // basically true, just in case
            ThreadCacheContext.registerRequestPath(requestManager.getRequestPathAndQuery());
            ThreadCacheContext.registerEntryMethod(runtime.getExecuteMethod());
        }
    }

    protected void arrangeThreadCacheContextLoginItem(ActionRuntime runtime) {
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
     * @param runtime The runtime meta of action execute. (NotNull)
     */
    protected void arrangePreparedAccessContext(ActionRuntime runtime) { // called by callback
        final AccessContextResource resource = createAccessContextResource(runtime);
        final AccessContext accessContext = accessContextArranger.arrangePreparedAccessContext(resource);
        PreparedAccessContext.setAccessContextOnThread(accessContext);
    }

    protected AccessContextResource createAccessContextResource(ActionRuntime runtime) {
        final String classTitle = DfTypeUtil.toClassTitle(runtime.getActionType());
        return newAccessContextResource(classTitle, runtime.getExecuteMethod());
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
     * @param runtime The runtime meta of action execute. (NotNull)
     */
    protected void arrangeCallbackContext(final ActionRuntime runtime) {
        final SqlFireHook sqlFireHook = createSqlFireHook(runtime);
        CallbackContext.setSqlFireHookOnThread(sqlFireHook);
        final SqlStringFilter filter = createSqlStringFilter(runtime);
        CallbackContext.setSqlStringFilterOnThread(filter);
    }

    /**
     * Create the filter of SQL string for DBFlute.
     * @param runtime The runtime meta of action execute. (NotNull)
     * @return The hook of SQL fire. (NullAllowed: if null, no hook)
     */
    protected SqlFireHook createSqlFireHook(ActionRuntime runtime) {
        return newRomanticTraceableSqlFireHook();
    }

    protected RomanticTraceableSqlFireHook newRomanticTraceableSqlFireHook() {
        return new RomanticTraceableSqlFireHook();
    }

    /**
     * Create the filter of SQL string for DBFlute.
     * @param runtime The runtime meta of action execute. (NotNull)
     * @return The filter of SQL string. (NullAllowed: if null, no filter)
     */
    protected SqlStringFilter createSqlStringFilter(ActionRuntime runtime) {
        final Method actionMethod = runtime.getExecuteMethod();
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
     * @param runtime The runtime meta of action execute. (NotNull)
     */
    protected void handleSqlCount(final ActionRuntime runtime) {
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
            handleTooManySqlExecution(runtime, counter);
        }
        final String exp = counter.toLineDisp();
        requestManager.setAttribute(RequestManager.DBFLUTE_SQL_COUNT_KEY, exp); // logged by logging filter
    }

    /**
     * Handle too many SQL executions.
     * @param runtime The runtime meta of action execute. (NotNull)
     * @param sqlCounter The counter object for SQL executions. (NotNull)
     */
    protected void handleTooManySqlExecution(ActionRuntime runtime, ExecutedSqlCounter sqlCounter) {
        final String actionDisp = buildActionDisp(runtime);
        logger.warn("*Too many SQL executions: " + sqlCounter.getTotalCountOfSql() + " in " + actionDisp);
    }

    protected String buildActionDisp(ActionRuntime runtime) {
        return runtime.getActionType().getSimpleName() + "." + runtime.getExecuteMethod().getName() + "()";
    }

    /**
     * Get the limit count of SQL execution. <br>
     * You can override if you need.
     * @param runtime The runtime meta of action execute. (NotNull)
     * @return The max count allowed for SQL executions. (MinusAllowed: if minus, no check)
     */
    protected int getLimitCountOfSql(ActionRuntime runtime) {
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
    //                                                                      Login Required
    //                                                                      ==============
    /**
     * Check the login required for the requested action.
     * @param runtime The runtime meta of action execute to determine required action. (NotNull)
     * @throws LoginRequiredException When it fails to access the action for non-login.
     */
    protected void checkLoginRequired(ActionRuntime runtime) throws LoginRequiredException {
        loginManager.ifPresent(nager -> {
            nager.checkLoginRequired(createLogingHandlingResource(runtime));
        });
    }

    protected LoginHandlingResource createLogingHandlingResource(ActionRuntime runtime) {
        return new LoginHandlingResource(runtime);
    }

    // ===================================================================================
    //                                                                       Double Submit
    //                                                                       =============
    protected void handleDoubleSubmit(ActionRuntime runtime) {
        final TxToken txToken = runtime.getActionExecute().getTxToken();
        if (!txToken.needsProcess()) {
            return;
        }
        // default scope is same action for now, will implement flexible scope if it needs
        final Class<?> actionType = runtime.getActionType();
        if (txToken.equals(TxToken.SAVE)) {
            checkDoubleSubmitPreconditionExists(runtime);
            doubleSubmitManager.saveToken(actionType);
        } else if (txToken.equals(TxToken.VALIDATE) || txToken.equals(TxToken.VALIDATE_KEEP)) {
            final boolean matched;
            if (txToken.equals(TxToken.VALIDATE)) {
                matched = doubleSubmitManager.determineTokenWithReset(actionType);
            } else { // and keep (no reset)
                matched = doubleSubmitManager.determineToken(actionType);
            }
            if (!matched) {
                throwDoubleSubmitRequestException(runtime);
            }
        }
    }

    protected void checkDoubleSubmitPreconditionExists(ActionRuntime runtime) {
        final Locale userLocale = requestManager.getUserLocale();
        if (!messageManager.findMessage(userLocale, getDoubleSubmitMessageKey()).isPresent()) {
            throwDoubleSubmitMessageNotFoundException(runtime, userLocale);
        }
    }

    protected String throwDoubleSubmitMessageNotFoundException(ActionRuntime runtime, Locale userLocale) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the double submit message in message resource.");
        br.addItem("Advice");
        br.addElement("The message key should exist in your message resource,");
        br.addElement("when you control double submit by transaction token.");
        br.addElement("For example: (..._message.properties)");
        br.addElement("  " + getDoubleSubmitMessageKey() + " = double submit might be requested");
        br.addItem("Requested Action");
        br.addElement(runtime);
        br.addItem("User Locale");
        br.addElement(userLocale);
        br.addItem("NotFound MessageKey");
        br.addElement(getDoubleSubmitMessageKey());
        final String msg = br.buildExceptionMessage();
        throw new DoubleSubmitMessageNotFoundException(msg);
    }

    protected String throwDoubleSubmitRequestException(ActionRuntime runtime) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The request was born from double submit.");
        br.addItem("Advice");
        br.addElement("Double submit by user operation");
        br.addElement("or not saved token but validate it.");
        br.addElement("Default scope of token is action type");
        br.addElement("so SAVE and VALIDATE should be in same action.");
        br.addItem("Requested Action");
        br.addElement(runtime);
        br.addItem("Requested Token");
        br.addElement(doubleSubmitManager.getRequestedToken());
        br.addItem("Saved Token");
        br.addElement(doubleSubmitManager.getSessionTokenMap());
        br.addItem("Token Group");
        br.addElement(runtime.getActionType().getName());
        final String msg = br.buildExceptionMessage();
        throw new DoubleSubmitRequestException(msg, getDoubleSubmitMessageKey());
    }

    protected String getDoubleSubmitMessageKey() {
        return keySupplier.getErrorsAppDoubleSubmitRequestKey();
    }
}
