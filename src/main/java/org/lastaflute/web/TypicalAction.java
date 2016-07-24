/*
 * Copyright 2015-2016 the original author or authors.
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
package org.lastaflute.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import javax.annotation.Resource;

import org.dbflute.jdbc.Classification;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.exception.ExceptionTranslator;
import org.lastaflute.core.exception.LaApplicationException;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.core.message.exception.MessageKeyNotFoundException;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.core.util.LaDBFluteUtil;
import org.lastaflute.core.util.LaDBFluteUtil.ClassificationUnknownCodeException;
import org.lastaflute.core.util.LaStringUtil;
import org.lastaflute.db.dbflute.accesscontext.AccessContextArranger;
import org.lastaflute.web.api.ApiManager;
import org.lastaflute.web.docs.LaActionDocs;
import org.lastaflute.web.exception.ActionApplicationExceptionHandler;
import org.lastaflute.web.exception.ForcedIllegalTransitionException;
import org.lastaflute.web.exception.ForcedRequest403ForbiddenException;
import org.lastaflute.web.exception.ForcedRequest404NotFoundException;
import org.lastaflute.web.hook.ActionHook;
import org.lastaflute.web.hook.TooManySqlOption;
import org.lastaflute.web.hook.TypicalEmbeddedKeySupplier;
import org.lastaflute.web.hook.TypicalGodHandEpilogue;
import org.lastaflute.web.hook.TypicalGodHandMonologue;
import org.lastaflute.web.hook.TypicalGodHandPrologue;
import org.lastaflute.web.hook.TypicalGodHandResource;
import org.lastaflute.web.hook.TypicalKey.TypicalSimpleEmbeddedKeySupplier;
import org.lastaflute.web.login.LoginManager;
import org.lastaflute.web.login.UserBean;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.request.ResponseManager;
import org.lastaflute.web.servlet.session.SessionManager;
import org.lastaflute.web.token.DoubleSubmitManager;
import org.lastaflute.web.token.TokenErrorHook;
import org.lastaflute.web.token.exception.DoubleSubmitRequestException;
import org.lastaflute.web.util.LaActionRuntimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The typical action for your project. <br>
 * You should extend this class when making your project-base action. <br>
 * And you can add methods for all applications.
 * @author jflute
 * @author yu1ro (pull request)
 */
public abstract class TypicalAction extends LastaAction implements ActionHook, LaActionDocs {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(TypicalAction.class);
    protected static final String GLOBAL = UserMessages.GLOBAL_PROPERTY_KEY;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant director (AD) for framework. (NotNull) */
    @Resource
    private FwAssistantDirector assistantDirector;

    /** The manager of time. (NotNull) */
    @Resource
    private TimeManager timeManager;

    /** The manager of message. (NotNull) */
    @Resource
    private MessageManager messageManager;

    /** The translator of exception. (NotNull) */
    @Resource
    private ExceptionTranslator exceptionTranslator;

    /** The manager of request. (NotNull) */
    @Resource
    private RequestManager requestManager;

    /** The manager of response. (NotNull) */
    @Resource
    private ResponseManager responseManager;

    /** The manager of session. (NotNull) */
    @Resource
    private SessionManager sessionManager;

    /** The manager of API. (NotNull) */
    @Resource
    private ApiManager apiManager;

    /** The manager of double submit using transaction token. (NotNull) */
    @Resource
    private DoubleSubmitManager doubleSubmitManager;

    // ===================================================================================
    //                                                                               Hook
    //                                                                              ======
    // [typical callback process]
    // read the source code for the details
    // (because of no comment here)
    // -----------------------------------------------------
    //                                                Before
    //                                                ------
    @Override
    public ActionResponse godHandPrologue(ActionRuntime runtime) { // fixed process
        return createTypicalGodHandPrologue(runtime).performPrologue(runtime);
    }

    protected TypicalGodHandPrologue createTypicalGodHandPrologue(ActionRuntime runtime) {
        final TypicalGodHandResource resource = createTypicalGodHandResource(runtime);
        final TypicalEmbeddedKeySupplier supplier = newTypicalEmbeddedKeySupplier();
        final AccessContextArranger arranger = newAccessContextArranger();
        return newTypicalGodHandPrologue(resource, supplier, arranger, () -> getUserBean(), () -> myAppType());
    }

    /**
     * New the arranger of access context.
     * @return The instance of arranger. (NotNull)
     */
    protected abstract AccessContextArranger newAccessContextArranger();

    protected TypicalGodHandPrologue newTypicalGodHandPrologue(TypicalGodHandResource resource, TypicalEmbeddedKeySupplier keySupplier,
            AccessContextArranger arranger, Supplier<OptionalThing<? extends UserBean<?>>> userBeanSupplier,
            Supplier<String> appTypeSupplier) {
        return new TypicalGodHandPrologue(resource, keySupplier, arranger, userBeanSupplier, appTypeSupplier);
    }

    @Override
    public ActionResponse hookBefore(ActionRuntime runtime) { // application may override
        return ActionResponse.undefined();
    }

    // -----------------------------------------------------
    //                                            on Failure
    //                                            ----------
    @Override
    public ActionResponse godHandMonologue(ActionRuntime runtime) { // fixed process
        return createTypicalGodHandMonologue(runtime).performMonologue(runtime);
    }

    protected TypicalGodHandMonologue createTypicalGodHandMonologue(ActionRuntime runtime) {
        final TypicalGodHandResource resource = createTypicalGodHandResource(runtime);
        final TypicalEmbeddedKeySupplier supplier = newTypicalEmbeddedKeySupplier();
        final ActionApplicationExceptionHandler handler = newActionApplicationExceptionHandler();
        return newTypicalGodHandMonologue(resource, supplier, handler);
    }

    protected ActionApplicationExceptionHandler newActionApplicationExceptionHandler() {
        return appEx -> handleApplicationException(appEx);
    }

    /**
     * Handle the application exception before framework's handling process.
     * @param appEx The thrown application exception. (NotNull)
     * @return The response for the exception. (NullAllowed: if null, to next handling step)
     */
    protected ActionResponse handleApplicationException(LaApplicationException appEx) { // application may override
        return ActionResponse.undefined();
    }

    protected TypicalGodHandMonologue newTypicalGodHandMonologue(TypicalGodHandResource resource, TypicalEmbeddedKeySupplier supplier,
            ActionApplicationExceptionHandler handler) {
        return new TypicalGodHandMonologue(resource, supplier, handler);
    }

    // -----------------------------------------------------
    //                                               Finally
    //                                               -------
    @Override
    public void hookFinally(ActionRuntime runtime) { // application may override
    }

    @Override
    public void godHandEpilogue(ActionRuntime runtime) { // fixed process
        if (runtime.isForwardToHtml()) {
            setupHtmlData(runtime);
        }
        createTypicalGodHandEpilogue(runtime).performEpilogue(runtime);
    }

    protected void setupHtmlData(ActionRuntime runtime) { // application may override
    }

    protected TypicalGodHandEpilogue createTypicalGodHandEpilogue(ActionRuntime runtime) {
        return newTypicalGodHandEpilogue(createTypicalGodHandResource(runtime), createTooManySqlOption(runtime));
    }

    protected TypicalGodHandEpilogue newTypicalGodHandEpilogue(TypicalGodHandResource resource, TooManySqlOption tooManySqlOption) {
        return new TypicalGodHandEpilogue(resource, tooManySqlOption);
    }

    protected TooManySqlOption createTooManySqlOption(ActionRuntime runtime) {
        return new TooManySqlOption(calculateSqlExecutionCountLimit(runtime));
    }

    protected int calculateSqlExecutionCountLimit(ActionRuntime runtime) {
        return runtime.getActionExecute().getSqlExecutionCountLimit().orElse(30);
    }

    // -----------------------------------------------------
    //                                      Resource Factory
    //                                      ----------------
    protected TypicalGodHandResource createTypicalGodHandResource(ActionRuntime runtime) {
        return new TypicalGodHandResource(assistantDirector, timeManager, messageManager, exceptionTranslator, requestManager,
                responseManager, sessionManager, myLoginManager(), apiManager);
    }

    protected TypicalEmbeddedKeySupplier newTypicalEmbeddedKeySupplier() {
        return new TypicalSimpleEmbeddedKeySupplier();
    }

    // ===================================================================================
    //                                                                           User Info
    //                                                                           =========
    /**
     * Get the bean of login user on session as interface type. (for application)
     * @return The optional thing of found user bean. (NotNull, EmptyAllowed: when not login)
     */
    protected abstract OptionalThing<? extends UserBean<?>> getUserBean();

    /**
     * Get the application type, e.g. for common column.
     * @return The application type basically fixed string. (NotNull) 
     */
    protected abstract String myAppType();

    /**
     * Get the user type of this applicatoin's login.
     * @return The optional expression of user type. (NotNull, EmptyAllowed: if no login handling) 
     */
    protected abstract OptionalThing<String> myUserType();

    /**
     * Get my (application's) login manager. (for framework)
     * @return The optional instance of login manager. (NotNull, EmptyAllowed: if no login handling)
     */
    protected abstract OptionalThing<LoginManager> myLoginManager();

    // ===================================================================================
    //                                                                       Double Submit
    //                                                                       =============
    /**
     * Save the transaction token to session, using this action as group type.
     */
    protected void saveToken() { // no return because automatically output by template framework
        doubleSubmitManager.saveToken(myTokenGroupType());
    }

    /**
     * Verify the request token (whether the request token is same as saved token) <br>
     * And reset the saved token, it can be used only one-time. <br> 
     * Using this action as group type.
     * @param errorResponseLambda The hook to return action response when token error. (NotNull)
     * @throws DoubleSubmitRequestException When the token is invalid.
     */
    protected void verifyToken(TokenErrorHook errorResponseLambda) {
        doubleSubmitManager.verifyToken(myTokenGroupType(), errorResponseLambda);
    }

    /**
     * Verify the request token (whether the request token is same as saved token) <br>
     * Keep the saved token, so this method is basically for intermediate request. <br>
     * Using this action as group type.
     * @param errorResponseLambda The hook to return action response when token error. (NotNull)
     * @throws DoubleSubmitRequestException When the token is invalid.
     */
    protected void verifyTokenKeep(TokenErrorHook errorResponseLambda) {
        doubleSubmitManager.verifyTokenKeep(myTokenGroupType(), errorResponseLambda);
    }

    protected Class<?> myTokenGroupType() {
        return LaActionRuntimeUtil.getActionRuntime().getActionType();
    }

    // ===================================================================================
    //                                                                     Verify Anything
    //                                                                     ===============
    // -----------------------------------------------------
    //                                      Verify Parameter
    //                                      ----------------
    protected void verifyParameterExists(Object parameter) { // application may call
        if (parameter == null || (parameter instanceof String && ((String) parameter).isEmpty())) {
            handleParameterFailure("Not found the parameter: " + parameter);
        }
    }

    protected void verifyParameterTrue(String msg, boolean expectedBool) { // application may call
        if (!expectedBool) {
            handleParameterFailure(msg);
        }
    }

    protected void handleParameterFailure(String msg) {
        // no server error because it can occur by user's trick easily e.g. changing GET parameter
        throw404(msg);
    }

    // -----------------------------------------------------
    //                                         Verify or ...
    //                                         -------------
    /**
     * Check the condition is true or it throws 404 not found forcedly. <br>
     * You can use this in your action process against invalid URL parameters.
     * @param debugMsg The debug message for developer. (NotNull)
     * @param expectedBool The expected determination for your business, true or false. (false: 404 not found)
     */
    protected void verifyTrueOr404NotFound(String debugMsg, boolean expectedBool) { // application may call
        if (!expectedBool) {
            throw404(debugMsg);
        }
    }

    /**
     * Check the condition is true or it throws illegal transition forcedly. <br>
     * You can use this in your action process against strange request parameters.
     * @param debugMsg The message for exception message. (NotNull)
     * @param expectedBool The expected determination for your business, true or false. (false: illegal transition)
     */
    protected void verifyTrueOrIllegalTransition(String debugMsg, boolean expectedBool) { // application may call
        if (!expectedBool) {
            throwIllegalTransition(debugMsg);
        }
    }

    /**
     * Throw 403 exception, and show 403 error page.
     * <pre>
     * if (...) {
     *     <span style="color: #CC4747">throw403</span>("...");
     * }
     * </pre>
     * @param debugMsg The debug message for developer (not user message). (NotNull)
     */
    protected void throw403(String debugMsg) {
        throw of403(debugMsg);
    }

    /**
     * Throw 404 exception, and show 404 error page.
     * <pre>
     * if (...) {
     *     <span style="color: #CC4747">throw404</span>("...");
     * }
     * </pre>
     * @param debugMsg The debug message for developer (not user message). (NotNull)
     */
    protected void throw404(String debugMsg) { // e.g. used by error handling of validation for GET parameter
        throw of404(debugMsg);
    }

    /**
     * Create exception of 403, for e.g. orElseThrow() of Optional.
     * <pre>
     * }).orElseThrow(() <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     return <span style="color: #CC4747">of403</span>("Not found the product: " + productId);
     * });
     * </pre>
     * @param debugMsg The debug message for developer (not user message). (NotNull)
     * @return The new-created exception of 403. (NotNull)
     */
    protected ForcedRequest403ForbiddenException of403(String debugMsg) {
        assertArgumentNotNull("msg for 403", debugMsg);
        return new ForcedRequest403ForbiddenException(debugMsg);
    }

    /**
     * Create exception of 404, for e.g. orElseThrow() of Optional.
     * <pre>
     * }).orElseThrow(() <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     return <span style="color: #CC4747">of404</span>("Not found the product: " + productId);
     * });
     * </pre>
     * @param debugMsg The debug message for developer (not user message). (NotNull)
     * @return The new-created exception of 404. (NotNull)
     */
    protected ForcedRequest404NotFoundException of404(String debugMsg) {
        assertArgumentNotNull("msg for 404", debugMsg);
        return new ForcedRequest404NotFoundException(debugMsg);
    }

    /**
     * Throw illegal transition exception, as application exception.
     * <pre>
     * if (...) {
     *     <span style="color: #CC4747">throwIllegalTransition</span>("...");
     * }
     * </pre>
     * @param debugMsg The debug message for developer (not user message). (NotNull)
     */
    protected void throwIllegalTransition(String debugMsg) {
        throw ofIllegalTransition(debugMsg);
    }

    /**
     * Create exception of illegal transition as application exception, for e.g. orElseThrow() of Optional.
     * <pre>
     * }).orElseThrow(() <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     return <span style="color: #CC4747">ofIllegalTransition</span>("Not found the product: " + productId);
     * });
     * </pre>
     * @param debugMsg The debug message for developer (not user message). (NotNull)
     * @return The new-created exception of 404. (NotNull)
     */
    protected ForcedIllegalTransitionException ofIllegalTransition(String debugMsg) {
        assertArgumentNotNull("msg for illegal transition", debugMsg);
        final String transitionKey = newTypicalEmbeddedKeySupplier().getErrorsAppIllegalTransitionKey();
        return new ForcedIllegalTransitionException(debugMsg, transitionKey);
    }

    // ===================================================================================
    //                                                                        Small Facade
    //                                                                        ============
    // -----------------------------------------------------
    //                                          Current Date
    //                                          ------------
    /**
     * Get the date that specifies current date for business. <br>
     * The word 'current' means transaction beginning in transaction if default setting of Framework. <br>
     * You can get the same date (but different instances) in the same transaction.
     * @return The local date that has current date. (NotNull)
     */
    protected LocalDate currentDate() {
        return timeManager.currentDate();
    }

    /**
     * Get the date-time that specifies current time for business. <br>
     * The word 'current' means transaction beginning in transaction if default setting of Framework. <br>
     * You can get the same date (but different instances) in the same transaction.
     * @return The local date-time that has current time. (NotNull)
     */
    protected LocalDateTime currentDateTime() {
        return timeManager.currentDateTime();
    }

    /**
     * Get the message for currently-requested user locale from message resources.
     * @param key The key of message managed by message resources. (NotNull)
     * @return The found message, specified locale resolved. (NotNull: if not found, throws exception)
     * @throws MessageKeyNotFoundException When the message is not found.
     */
    protected String getUserMessage(String key) {
        return messageManager.getMessage(requestManager.getUserLocale(), key);
    }

    /**
     * Get the message for currently-requested user locale from message resources.
     * @param key The key of message managed by message resources. (NotNull)
     * @param args The varying arguments for the message. (NotNull, EmptyAllowed)
     * @return The found message, specified locale resolved. (NotNull: if not found, throws exception)
     * @throws MessageKeyNotFoundException When the message is not found.
     */
    protected String getUserMessage(String key, Object... args) {
        return messageManager.getMessage(requestManager.getUserLocale(), key, args);
    }

    // -----------------------------------------------------
    //                                        Empty Handling
    //                                        --------------
    /**
     * @param str might be empty. (NullAllowed: if null, return true)
     * @return true if null or empty, false if blank or has characters.
     */
    protected boolean isEmpty(String str) {
        return LaStringUtil.isEmpty(str);
    }

    /**
     * @param str might not be empty. (NullAllowed: if null, return false)
     * @return true if blank or has characters, false if null or empty.
     */
    protected boolean isNotEmpty(String str) {
        return LaStringUtil.isNotEmpty(str);
    }

    // -----------------------------------------------------
    //                                        Classification
    //                                        --------------
    protected boolean isCls(Class<? extends Classification> cdefType, Object code) {
        assertArgumentNotNull("cdefType", cdefType);
        return LaDBFluteUtil.invokeClassificationCodeOf(cdefType, code) != null;
    }

    protected <CLS extends Classification> OptionalThing<CLS> toCls(Class<CLS> cdefType, Object code) {
        assertArgumentNotNull("cdefType", cdefType);
        if (code == null || (code instanceof String && isEmpty((String) code))) {
            return OptionalThing.ofNullable(null, () -> {
                throw new IllegalStateException("Not found the classification code for " + cdefType.getName() + ": " + code);
            });
        }
        try {
            @SuppressWarnings("unchecked")
            final CLS cls = (CLS) LaDBFluteUtil.toVerifiedClassification(cdefType, code);
            return OptionalThing.of(cls);
        } catch (ClassificationUnknownCodeException e) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Cannot convert the code to the classification:");
            sb.append("\n[Classification Convert Failure]");
            try {
                sb.append("\n").append(LaActionRuntimeUtil.getActionRuntime());
            } catch (RuntimeException continued) { // just in case
                logger.info("Not found the action runtime when toCls() called: " + cdefType.getName() + ", " + code, continued);
            }
            sb.append("\ncode=").append(code);
            //sb.append("\n").append(e.getClass().getName()).append("\n").append(e.getMessage());
            final String msg = sb.toString();
            throw new ForcedRequest404NotFoundException(msg, e);
        }
    }
}
