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

import java.util.function.Supplier;

import javax.annotation.Resource;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.exception.ExceptionTranslator;
import org.lastaflute.core.exception.LaApplicationException;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.db.dbflute.accesscontext.AccessContextArranger;
import org.lastaflute.web.api.ApiManager;
import org.lastaflute.web.docs.LaActionDocs;
import org.lastaflute.web.exception.ActionApplicationExceptionHandler;
import org.lastaflute.web.exception.Forced404NotFoundException;
import org.lastaflute.web.exception.RequestIllegalTransitionException;
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
    /**
     * Check the condition is true or it throws client error (e.g. 404 not found) forcedly. <br>
     * You can use this in your action process against invalid URL parameters.
     * @param debugMsg The debug message for developer. (NotNull)
     * @param expectedBool The expected determination for your business, true or false. (false: e.g. 404 not found)
     */
    protected void verifyOrClientError(String debugMsg, boolean expectedBool) { // application may call
        assertArgumentNotNull("debugMsg", debugMsg);
        if (!expectedBool) {
            handleVerifiedClientError(debugMsg);
        }
    }

    protected void handleVerifiedClientError(String debugMsg) {
        throw new Forced404NotFoundException(debugMsg, UserMessages.empty());
    }

    /**
     * Check the condition is true or it throws illegal transition forcedly. <br>
     * You can use this in your action process against strange request parameters.
     * @param debugMsg The message for exception message. (NotNull)
     * @param expectedBool The expected determination for your business, true or false. (false: illegal transition)
     */
    protected void verifyOrIllegalTransition(String debugMsg, boolean expectedBool) { // application may call
        assertArgumentNotNull("debugMsg", debugMsg);
        if (!expectedBool) {
            handleVerifiedIllegalTransition(debugMsg);
        }
    }

    protected void handleVerifiedIllegalTransition(String debugMsg) {
        throw new RequestIllegalTransitionException(debugMsg, newTypicalEmbeddedKeySupplier().getErrorsAppIllegalTransitionKey());
    }
}
