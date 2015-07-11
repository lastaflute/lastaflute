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
package org.lastaflute.web.ruts;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.db.jta.stage.TransactionGenre;
import org.lastaflute.db.jta.stage.TransactionStage;
import org.lastaflute.web.callback.ActionHook;
import org.lastaflute.web.callback.ActionRuntime;
import org.lastaflute.web.exception.ActionCallbackReturnNullException;
import org.lastaflute.web.exception.ExecuteMethodAccessFailureException;
import org.lastaflute.web.exception.ExecuteMethodArgumentMismatchException;
import org.lastaflute.web.exception.ExecuteMethodReturnNullException;
import org.lastaflute.web.exception.ExecuteMethodReturnTypeNotResponseException;
import org.lastaflute.web.exception.ExecuteMethodReturnUndefinedResponseException;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.message.ActionMessages;
import org.lastaflute.web.ruts.process.ActionResponseReflector;
import org.lastaflute.web.ruts.process.exception.ActionCreateFailureException;
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
    private static final Object[] EMPTY_ARRAY = new Object[0];

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ActionExecute execute; // fixed info
    protected final ActionRuntime runtime; // has state
    protected final ActionResponseReflector reflector; // has state
    protected final TransactionStage stage; // singleton
    protected final RequestManager requestManager; // singleton
    protected final Object action; // created here

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
    }

    protected Object createAction() {
        try {
            return execute.getActionMapping().createAction();
        } catch (RuntimeException e) {
            throw new ActionCreateFailureException("Failed to create the action: " + execute, e);
        }
    }

    // ===================================================================================
    //                                                                             Execute
    //                                                                             =======
    @Override
    public NextJourney execute(OptionalThing<VirtualActionForm> form) {
        final NextJourney journey = godHandyExecute(form);
        setupDisplayData(journey);
        showTransition(journey);
        return journey;
    }

    // -----------------------------------------------------
    //                                             God Handy
    //                                             ---------
    protected NextJourney godHandyExecute(OptionalThing<VirtualActionForm> form) {
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
        } finally {
            processHookFinally(hook);
        }
    }

    protected ActionHook prepareActionHook() {
        return action instanceof ActionHook ? (ActionHook) action : null;
    }

    protected NextJourney transactionalExecute(OptionalThing<VirtualActionForm> form, ActionHook hook) {
        final ExecuteTransactionResult result = (ExecuteTransactionResult) stage.selectable(tx -> {
            final ActionResponse response = actuallyExecute(form, hook); /* #to_action */
            assertExecuteMethodResponseDefined(response);
            final NextJourney journey = reflect(response); /* also response handling in transaction */
            tx.returns(new ExecuteTransactionResult(response, journey));
        } , getExecuteTransactionGenre()).get(); // because of not null
        hookAfterTxCommitIfExists(result);
        return result.getJourney();
    }

    protected static class ExecuteTransactionResult {

        protected final ActionResponse response;
        protected final NextJourney journey;

        public ExecuteTransactionResult(ActionResponse response, NextJourney journey) {
            this.response = response;
            this.journey = journey;
        }

        public ActionResponse getResponse() {
            return response;
        }

        public NextJourney getJourney() {
            return journey;
        }
    }

    protected void assertExecuteMethodResponseDefined(ActionResponse response) {
        if (response.isUndefined()) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Cannot return undefined resopnse from the execute method");
            br.addItem("Advice");
            br.addElement("Not allowed to return undefined() in execute method.");
            br.addElement("If you want to return response as empty body,");
            br.addElement("use asEmptyBody() like this:");
            br.addElement("  @Execute");
            br.addElement("  public HtmlResponse index() {");
            br.addElement("      return HtmlResponse.asEmptyBody();");
            br.addElement("  }");
            br.addItem("Action Execute");
            br.addElement(execute);
            final String msg = br.buildExceptionMessage();
            throw new ExecuteMethodReturnUndefinedResponseException(msg);
        }
    }

    protected TransactionGenre getExecuteTransactionGenre() {
        return execute.getTransactionGenre();
    }

    protected void hookAfterTxCommitIfExists(final ExecuteTransactionResult result) {
        result.getResponse().getAfterTxCommitHook().ifPresent(afterTx -> {
            afterTx.hook();
        });
    }

    // -----------------------------------------------------
    //                                      Reflect Response
    //                                      ----------------
    protected NextJourney reflect(ActionResponse response) {
        return reflector.reflect(response);
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
            runtime.setActionResponse(response);
        }
        assertAfterTxCommitHookNotSpecified("before", response);
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
        runtime.setFailureCause(e);
        if (hook == null) {
            throw e;
        }
        final ActionResponse response = hook.godHandMonologue(runtime);
        if (isDefined(response)) {
            runtime.setActionResponse(response);
            assertAfterTxCommitHookNotSpecified("monologue", response);
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
        assertCallbackReturnNotNull(response);
        return response.isUndefined();
    }

    protected boolean isDefined(ActionResponse response) {
        assertCallbackReturnNotNull(response);
        return response.isDefined();
    }

    // -----------------------------------------------------
    //                                          Assist Logic
    //                                          ------------
    protected void assertAfterTxCommitHookNotSpecified(String actionHookTitle, ActionResponse response) {
        response.getAfterTxCommitHook().ifPresent(hook -> {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("The afterTxCommit() cannot be used in action hook.");
            br.addItem("Advice");
            br.addElement("The method only can be in action execute.");
            br.addElement("Make sure your action hook response.");
            br.addItem("Specified ActionResponse");
            br.addElement(response);
            br.addItem("Specified ResponseHook");
            br.addElement(hook);
            br.addItem("Action Execute");
            br.addElement(execute);
            br.addItem("ActionHook Type");
            br.addElement(actionHookTitle);
            final String msg = br.buildExceptionMessage();
            throw new IllegalStateException(msg);
        });
    }

    // ===================================================================================
    //                                                                    Actually Execute
    //                                                                    ================
    protected ActionResponse actuallyExecute(OptionalThing<VirtualActionForm> optForm, ActionHook hook) {
        showAction(runtime);
        final Object[] requestArgs = toRequestArgs(optForm);
        final Object result = invokeExecuteMethod(execute.getExecuteMethod(), requestArgs); // #to_action
        assertExecuteReturnNotNull(requestArgs, result);
        assertExecuteMethodReturnTypeActionResponse(requestArgs, result);
        final ActionResponse response = (ActionResponse) result;
        runtime.setActionResponse(response); // always set here because of main
        return response;
    }

    protected Object[] toRequestArgs(OptionalThing<VirtualActionForm> optForm) {
        final List<Object> paramList = new ArrayList<Object>(4);
        execute.getUrlParamArgs().ifPresent(args -> {
            paramList.addAll(runtime.getRequestUrlParam().getUrlParamValueMap().values());
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
        } catch (InvocationTargetException e) { // e.g. exception in the method
            return handleExecuteMethodInvocationTargetException(executeMethod, requestArgs, e);
        } catch (IllegalAccessException e) { // e.g. private invoking
            handleExecuteMethodIllegalAccessException(executeMethod, requestArgs, e);
        } catch (IllegalArgumentException e) { // e.g. different argument number
            handleExecuteMethodIllegalArgumentException(executeMethod, requestArgs, e);
        }
        return result;
    }

    protected Object handleExecuteMethodInvocationTargetException(Method executeMethod, Object[] requestArgs, InvocationTargetException e)
            throws Error {
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
        final String msg = setupMethodExceptionMessage("Found the exception in the method invoking.", executeMethod, requestArgs);
        throw new IllegalStateException(msg, cause);
    }

    protected void handleExecuteMethodIllegalAccessException(Method executeMethod, Object[] requestArgs, IllegalAccessException cause) {
        final String msg = setupMethodExceptionMessage("Cannot access the execute method.", executeMethod, requestArgs);
        throw new ExecuteMethodAccessFailureException(msg, cause);
    }

    protected void handleExecuteMethodIllegalArgumentException(Method executeMethod, Object[] requestArgs, IllegalArgumentException cause) {
        throwExecuteMethodArgumentMismatchException(executeMethod, requestArgs, cause);
    }

    protected void throwExecuteMethodArgumentMismatchException(Method executeMethod, Object[] requestArgs, IllegalArgumentException cause) {
        final String msg = setupMethodExceptionMessage("Mismatch the argument for the execute method.", executeMethod, requestArgs);
        throw new ExecuteMethodArgumentMismatchException(msg, cause);
    }

    protected String setupMethodExceptionMessage(String notice, Method executeMethod, Object[] requestArgs) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice(notice);
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        br.addItem("Request Arguments");
        br.addElement(Arrays.asList(requestArgs));
        return br.buildExceptionMessage();
    }

    protected ActionResponse handleValidationErrorException(ValidationErrorException cause) {
        // API dispatch is not here, needs to call it in error handler by developer
        // e.g. validate(form, () -> dispatchApiValidationError())
        final ActionMessages errors = cause.getMessages();
        requestManager.errors().save(errors); // also API can use it
        final VaErrorHook errorHandler = cause.getErrorHandler();
        final ActionResponse response = errorHandler.hook();
        if (response == null) {
            throw new IllegalStateException("The handler for validation error cannot return null: " + errorHandler, cause);
        }
        return response;
    }

    // ===================================================================================
    //                                                                        Display Data
    //                                                                        ============
    protected void setupDisplayData(NextJourney journey) {
        if (runtime.isForwardToHtml() && journey.isDefined()) {
            runtime.getDisplayDataMap().forEach((key, value) -> requestManager.setAttribute(key, value));
        }
    }

    // ===================================================================================
    //                                                                             Logging
    //                                                                             =======
    protected void showTransition(NextJourney journey) {
        if (logger.isDebugEnabled() && journey.isDefined()) {
            final String ing = journey.isRedirectTo() ? "Redirecting" : "Forwarding";
            final String path = journey.getRoutingPath(); // not null
            final String tag = path.endsWith(".html") ? "#html " : (path.endsWith(".jsp") ? "#jsp " : "");
            logger.debug("#flow ...{} to {}{}", ing, tag, path);
        }
    }

    protected String buildActionDisp(ActionRuntime runtime) {
        final Method method = runtime.getExecuteMethod();
        final Class<?> declaringClass = method.getDeclaringClass();
        return declaringClass.getSimpleName() + "@" + method.getName() + "()";
    }

    protected String buildActionName(ActionRuntime runtime) {
        final Method method = runtime.getExecuteMethod();
        final Class<?> declaringClass = method.getDeclaringClass();
        return declaringClass.getSimpleName();
    }

    // ===================================================================================
    //                                                                       Assert Helper
    //                                                                       =============
    protected void assertCallbackReturnNotNull(ActionResponse response) {
        if (response == null) {
            throwActionCallbackReturnNullException();
        }
    }

    protected void throwActionCallbackReturnNullException() {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not allowed to return null from ActionCallback methods.");
        br.addItem("Advice");
        br.addElement("ActionCallback methods should return response instance.");
        br.addElement("For example, if callbackBefore():");
        br.addElement("  (x):");
        br.addElement("    public ActionResponse callbackBefore(...) {");
        br.addElement("        return null; // *NG");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public ActionResponse callbackBefore(...) {");
        br.addElement("        return ActionResponse.empty(); // OK");
        br.addElement("    }");
        br.addElement("    public ActionResponse callbackBefore(...) {");
        br.addElement("        return asHtml(...); // OK");
        br.addElement("    }");
        br.addItem("Action Execute");
        br.addElement(execute);
        br.addItem("Action Object");
        br.addElement(action);
        final String msg = br.buildExceptionMessage();
        throw new ActionCallbackReturnNullException(msg);
    }

    protected void assertExecuteReturnNotNull(Object[] requestArgs, Object result) {
        if (result == null) {
            throwExecuteMethodReturnNullException(requestArgs);
        }
    }

    protected void throwExecuteMethodReturnNullException(Object[] requestArgs) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not allowed to return null from the execute method.");
        br.addItem("Advice");
        br.addElement("Execute method should return response instance.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse index(...) {");
        br.addElement("        return null; // *NG");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public HtmlResponse index(...) {");
        br.addElement("        return asHtml(...); // OK");
        br.addElement("    }");
        br.addItem("Action Execute");
        br.addElement(execute);
        br.addItem("Action Object");
        br.addElement(action);
        br.addItem("Request Arguments");
        br.addElement(Arrays.asList(requestArgs));
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodReturnNullException(msg);
    }

    protected void assertExecuteMethodReturnTypeActionResponse(Object[] requestArgs, Object result) {
        if (!(result instanceof ActionResponse)) {
            throwExecuteMethodReturnTypeNotResponseException(requestArgs, result);
        }
    }

    protected void throwExecuteMethodReturnTypeNotResponseException(Object[] requestArgs, Object result) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not action response type was returned from your action.");
        br.addItem("Action Execute");
        br.addElement(execute);
        br.addItem("Action Object");
        br.addElement(action);
        br.addItem("Request Arguments");
        br.addElement(Arrays.asList(requestArgs));
        br.addItem("Unknoww Return");
        br.addElement(result != null ? result.getClass() : null);
        br.addElement(result);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodReturnTypeNotResponseException(msg);
    }
}
