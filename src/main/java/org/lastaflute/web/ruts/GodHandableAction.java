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
package org.lastaflute.web.ruts;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.core.message.UserMessage;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.db.jta.romanticist.SavedTransactionMemories;
import org.lastaflute.db.jta.romanticist.TransactionMemoriesProvider;
import org.lastaflute.db.jta.stage.BegunTx;
import org.lastaflute.db.jta.stage.TransactionGenre;
import org.lastaflute.db.jta.stage.TransactionStage;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.exception.ActionWrappedCheckedException;
import org.lastaflute.web.exception.ExecuteMethodAccessFailureException;
import org.lastaflute.web.exception.ExecuteMethodArgumentMismatchException;
import org.lastaflute.web.hook.ActionHook;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.process.ActionResponseReflector;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.ruts.process.exception.ActionCreateFailureException;
import org.lastaflute.web.servlet.filter.RequestLoggingFilter.WholeShowAttribute;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.util.LaActionExecuteUtil;
import org.lastaflute.web.validation.VaErrorHook;
import org.lastaflute.web.validation.exception.ValidationErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The wrapper of action, which can be God hand. <br>
 * This class is new-created per request.
 * @author jflute
 */
public class GodHandableAction implements VirtualAction {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(GodHandableAction.class);
    protected static final Object[] EMPTY_ARRAY = new Object[0];
    protected static final String LF = "\n";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ActionExecute execute; // fixed info
    protected final ActionRuntime runtime; // has state
    protected final ActionResponseReflector reflector; // has state
    protected final TransactionStage stage; // singleton
    protected final RequestManager requestManager; // singleton
    protected final Object action; // created here
    protected final RedCardableAssist redCardableAssist; // created here

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public GodHandableAction(ActionRuntime runtime, ActionResponseReflector reflector, TransactionStage stage,
            RequestManager requestManager) {
        this.execute = runtime.getActionExecute();
        this.runtime = runtime;
        this.reflector = reflector;
        this.stage = stage;
        this.requestManager = requestManager;
        this.action = createAction();
        this.redCardableAssist = createRedCardableAssist();
    }

    protected Object createAction() {
        try {
            return execute.getActionMapping().createAction();
        } catch (RuntimeException e) {
            throw new ActionCreateFailureException("Failed to create the action: " + execute, e);
        }
    }

    protected RedCardableAssist createRedCardableAssist() {
        return new RedCardableAssist(execute, runtime, requestManager);
    }

    // ===================================================================================
    //                                                                             Execute
    //                                                                             =======
    @Override
    public NextJourney execute(OptionalThing<VirtualForm> form) {
        final ActionHook hook = prepareActionHook();
        try {
            final ActionResponse before = processHookBefore(hook);
            if (before.isDefined()) { // e.g. login required
                return reflect(before);
            } else { // mainly here
                return transactionalExecute(form, hook); // #to_action
            }
        } catch (RuntimeException e) {
            final ActionResponse monologue = tellExceptionMonologue(hook, e);
            return reflect(monologue);
        } catch (Error e) {
            redCardableAssist.translateToHotdeployErrorIfPossible(e);
            throw e;
        } finally {
            processHookFinally(hook);
            prepareTransactionMemoriesIfExists();
        }
    }

    protected ActionHook prepareActionHook() {
        return action instanceof ActionHook ? (ActionHook) action : null;
    }

    protected NextJourney transactionalExecute(OptionalThing<VirtualForm> form, ActionHook hook) {
        final ExecuteTransactionResult result = (ExecuteTransactionResult) stage.selectable(tx -> {
            doExecute(form, hook, tx); // #to_action
        }, getExecuteTransactionGenre()).get(); // because of not null
        if (!result.isRollbackOnly()) {
            hookAfterTxCommitIfExists(result);
        }
        return result.getJourney();
    }

    protected void doExecute(OptionalThing<VirtualForm> form, ActionHook hook, BegunTx<Object> tx) {
        final ActionResponse response = actuallyExecute(form, hook); // #to_action
        redCardableAssist.assertExecuteMethodResponseDefined(response);
        final NextJourney journey = reflect(response);
        final boolean rollbackOnly;
        if (runtime.hasValidationError()) {
            tx.rollbackOnly();
            rollbackOnly = true;
        } else {
            rollbackOnly = false;
        }
        tx.returns(newExecuteTransactionResult(response, journey, rollbackOnly));
    }

    protected ExecuteTransactionResult newExecuteTransactionResult(ActionResponse response, NextJourney journey, boolean rollbackOnly) {
        return new ExecuteTransactionResult(response, journey, rollbackOnly);
    }

    protected static class ExecuteTransactionResult {

        protected final ActionResponse response;
        protected final NextJourney journey;
        protected final boolean rollbackOnly;

        public ExecuteTransactionResult(ActionResponse response, NextJourney journey, boolean rollbackOnly) {
            this.response = response;
            this.journey = journey;
            this.rollbackOnly = rollbackOnly;
        }

        public ActionResponse getResponse() {
            return response;
        }

        public NextJourney getJourney() {
            return journey;
        }

        public boolean isRollbackOnly() {
            return rollbackOnly;
        }
    }

    protected TransactionGenre getExecuteTransactionGenre() {
        return execute.getTransactionGenre();
    }

    protected void hookAfterTxCommitIfExists(ExecuteTransactionResult result) {
        result.getResponse().getAfterTxCommitHook().ifPresent(afterTx -> afterTx.hook());
    }

    // -----------------------------------------------------
    //                                      Reflect Response
    //                                      ----------------
    protected NextJourney reflect(ActionResponse response) {
        return reflector.reflect(response);
    }

    // -----------------------------------------------------
    //                                  Transaction Memories
    //                                  --------------------
    protected void prepareTransactionMemoriesIfExists() {
        final SavedTransactionMemories memories = ThreadCacheContext.findTransactionMemories();
        if (memories != null) {
            final List<TransactionMemoriesProvider> providerList = memories.getOrderedProviderList();
            final StringBuilder sb = new StringBuilder();
            for (TransactionMemoriesProvider provider : providerList) {
                provider.provide().ifPresent(result -> sb.append("\n*").append(result));
            }
            final WholeShowAttribute attribute = new WholeShowAttribute(sb.toString().trim());
            requestManager.setAttribute(LastaWebKey.DBFLUTE_TRANSACTION_MEMORIES_KEY, attribute);
        }
    }

    // ===================================================================================
    //                                                                        Process Hook
    //                                                                        ============
    // -----------------------------------------------------
    //                                                Before
    //                                                ------
    protected ActionResponse processHookBefore(ActionHook hook) {
        if (hook == null) {
            return ActionResponse.undefined();
        }
        showBefore(runtime);
        ActionResponse response = hook.godHandPrologue(runtime);
        if (isUndefined(response)) {
            response = hook.hookBefore(runtime);
        }
        if (isDefined(response)) {
            runtime.manageActionResponse(response);
        }
        redCardableAssist.assertAfterTxCommitHookNotSpecified("before", response);
        return response;
    }

    protected void showBefore(ActionRuntime runtime) {
        if (logger.isDebugEnabled()) {
            logger.debug("#flow ...Calling back #before for {}", buildActionName(runtime));
        }
    }

    // -----------------------------------------------------
    //                                            on Failure
    //                                            ----------
    protected ActionResponse tellExceptionMonologue(ActionHook hook, RuntimeException e) {
        runtime.manageFailureCause(e);
        if (hook == null) {
            throw e;
        }
        final ActionResponse response = hook.godHandMonologue(runtime);
        if (isDefined(response)) {
            runtime.manageActionResponse(response);
            redCardableAssist.assertAfterTxCommitHookNotSpecified("monologue", response);
            return response;
        } else {
            throw e;
        }
    }

    // -----------------------------------------------------
    //                                               Finally
    //                                               -------
    protected void processHookFinally(ActionHook hook) {
        if (hook == null) {
            return;
        }
        showFinally(runtime);
        try {
            hook.hookFinally(runtime);
        } finally {
            hook.godHandEpilogue(runtime);
        }
    }

    protected void showFinally(ActionRuntime runtime) {
        if (logger.isDebugEnabled()) {
            final String failureMark = runtime.hasFailureCause() ? " with failure" : "";
            logger.debug("#flow ...Calling back #finally{} for {}", failureMark, buildActionName(runtime));
        }
    }

    // -----------------------------------------------------
    //                                    Undefined Response
    //                                    ------------------
    protected boolean isUndefined(ActionResponse response) {
        redCardableAssist.assertHookReturnNotNull(response);
        return response.isUndefined();
    }

    protected boolean isDefined(ActionResponse response) {
        redCardableAssist.assertHookReturnNotNull(response);
        return response.isDefined();
    }

    // ===================================================================================
    //                                                                    Actually Execute
    //                                                                    ================
    protected ActionResponse actuallyExecute(OptionalThing<VirtualForm> optForm, ActionHook hook) {
        showAction(runtime);
        final Object[] requestArgs = toRequestArgs(optForm);
        final Object result = invokeExecuteMethod(execute.getExecuteMethod(), requestArgs); // #to_action
        redCardableAssist.assertExecuteReturnNotNull(requestArgs, result);
        redCardableAssist.assertExecuteMethodReturnTypeActionResponse(requestArgs, result);
        final ActionResponse response = (ActionResponse) result;
        runtime.manageActionResponse(response); // always set here because of main
        return response;
    }

    protected Object[] toRequestArgs(OptionalThing<VirtualForm> optForm) {
        final List<Object> paramList = new ArrayList<Object>(4);
        execute.getPathParamArgs().ifPresent(args -> {
            paramList.addAll(runtime.getRequestPathParam().getPathParamValueMap().values());
        });
        optForm.ifPresent(form -> paramList.add(form.getRealForm()));
        return !paramList.isEmpty() ? paramList.toArray(new Object[paramList.size()]) : EMPTY_ARRAY;
    }

    protected void showAction(ActionRuntime runtime) {
        if (logger.isDebugEnabled()) {
            logger.debug("#flow ...Beginning #action {}", buildActionDisp(runtime));
        }
    }

    // -----------------------------------------------------
    //                                         Invoke Action
    //                                         -------------
    protected Object invokeExecuteMethod(Method executeMethod, Object[] requestArgs) {
        Object result = null;
        try {
            result = executeMethod.invoke(action, requestArgs); // #to_action just here
            redCardableAssist.checkValidatorCalled();
        } catch (InvocationTargetException e) { // e.g. exception in the method
            return handleExecuteMethodInvocationTargetException(executeMethod, requestArgs, e);
        } catch (IllegalAccessException e) { // e.g. private invoking
            throwExecuteMethodAccessFailureException(executeMethod, requestArgs, e);
        } catch (IllegalArgumentException e) { // e.g. different argument number
            throwExecuteMethodArgumentMismatchException(executeMethod, requestArgs, e);
        }
        return result;
    }

    protected Object handleExecuteMethodInvocationTargetException(Method executeMethod, Object[] requestArgs, InvocationTargetException e) {
        final Throwable cause = e.getTargetException();
        if (cause instanceof ValidationErrorException) {
            return handleValidationErrorException((ValidationErrorException) cause);
        }
        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }
        if (cause instanceof Error) {
            throw (Error) cause;
        }
        // checked exception e.g. IOException
        final String msg = setupMethodExceptionMessage("Found the exception in the method invoking.", requestArgs);
        throw new ActionWrappedCheckedException(msg, cause);
    }

    protected void throwExecuteMethodAccessFailureException(Method executeMethod, Object[] requestArgs, IllegalAccessException cause) {
        final String msg = setupMethodExceptionMessage("Cannot access the execute method.", requestArgs);
        throw new ExecuteMethodAccessFailureException(msg, cause);
    }

    protected void throwExecuteMethodArgumentMismatchException(Method executeMethod, Object[] requestArgs, IllegalArgumentException cause) {
        final String msg = setupMethodExceptionMessage("Mismatch the argument for the execute method.", requestArgs);
        throw new ExecuteMethodArgumentMismatchException(msg, cause);
    }

    protected String setupMethodExceptionMessage(String notice, Object[] requestArgs) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice(notice);
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(execute.getExecuteMethod()));
        br.addItem("Request Arguments");
        br.addElement(Arrays.asList(requestArgs));
        return br.buildExceptionMessage();
    }

    protected ActionResponse handleValidationErrorException(ValidationErrorException cause) {
        final UserMessages messages = cause.getMessages();
        assertValidationErrorMessagesExists(messages, cause);
        if (logger.isDebugEnabled()) {
            final StringBuilder sb = new StringBuilder();
            sb.append(LF).append("_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/");
            sb.append(LF).append("[Validation Error]: runtimeGroups=");
            sb.append(Stream.of(cause.getRuntimeGroups()).map(tp -> {
                return tp.getSimpleName() + ".class";
            }).collect(Collectors.toList()));
            sb.append(" #").append(Integer.toHexString(cause.hashCode()));
            messages.toPropertySet().forEach(property -> {
                sb.append(LF).append(" ").append(property);
                for (Iterator<UserMessage> ite = messages.silentAccessByIteratorOf(property); ite.hasNext();) {
                    sb.append(LF).append("   ").append(ite.next());
                }
            });
            final Throwable nested = cause.getCause();
            if (nested != null) { // e.g. from remote api
                sb.append(LF).append(" - - - - - - - - - -");
                sb.append(LF).append(" caused by ").append(nested.getClass().getSimpleName()).append(":");
                sb.append(LF).append(nested.getMessage());
            }
            sb.append(LF).append("_/_/_/_/_/_/_/_/_/_/");
            logger.debug(sb.toString());
        }
        requestManager.errors().saveMessages(messages); // also API can use it
        runtime.manageValidationErrors(messages); // reflect to runtime
        runtime.manageFailureCause(cause); // also cause
        final VaErrorHook errorHook = cause.getErrorHook();
        final ActionResponse response = errorHook.hook(); // failure hook here if API
        if (response == null) {
            throw new IllegalStateException("The handler for validation error cannot return null: " + errorHook, cause);
        }
        response.getAfterTxCommitHook().ifPresent(hook -> {
            throw new IllegalStateException("Validation error always rollbacks transaction but tx-commit hook specified:" + hook);
        });
        return response;
    }

    protected void assertValidationErrorMessagesExists(UserMessages errors, ValidationErrorException cause) {
        if (errors.isEmpty()) {
            throw new IllegalStateException("Empty message even if validation error: " + buildActionDisp(runtime), cause);
        }
    }

    // ===================================================================================
    //                                                                         Action Name
    //                                                                         ===========
    protected String buildActionDisp(ActionRuntime runtime) {
        return buildActionName(runtime) + "@" + runtime.getExecuteMethod().getName() + "()";
    }

    protected String buildActionName(ActionRuntime runtime) {
        return runtime.getExecuteMethod().getDeclaringClass().getSimpleName();
    }
}
