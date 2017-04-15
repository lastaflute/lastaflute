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
package org.lastaflute.web;

import java.util.function.Supplier;

import javax.annotation.Resource;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.exception.ExceptionTranslator;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.db.dbflute.accesscontext.AccessContextArranger;
import org.lastaflute.web.api.ApiManager;
import org.lastaflute.web.docs.LaActionDocs;
import org.lastaflute.web.exception.RequestIllegalTransitionException;
import org.lastaflute.web.hook.ActionHook;
import org.lastaflute.web.hook.ApplicationExceptionHandler;
import org.lastaflute.web.hook.ApplicationExceptionResolver.HandledAppExCall;
import org.lastaflute.web.hook.EmbeddedMessageKey.SimpleEmbeddedMessageKeySupplier;
import org.lastaflute.web.hook.EmbeddedMessageKeySupplier;
import org.lastaflute.web.hook.GodHandEpilogue;
import org.lastaflute.web.hook.GodHandMonologue;
import org.lastaflute.web.hook.GodHandPrologue;
import org.lastaflute.web.hook.GodHandResource;
import org.lastaflute.web.hook.TooManySqlOption;
import org.lastaflute.web.login.LoginManager;
import org.lastaflute.web.login.UserBean;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.request.ResponseManager;
import org.lastaflute.web.servlet.session.SessionManager;
import org.lastaflute.web.token.DoubleSubmitManager;
import org.lastaflute.web.token.TokenErrorHook;
import org.lastaflute.web.token.exception.DoubleSubmittedRequestException;

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
    /** The assistant director (AD) for framework. (NotNull: after initialization) */
    @Resource
    private FwAssistantDirector assistantDirector;

    /** The manager of time. (NotNull: after initialization) */
    @Resource
    private TimeManager timeManager;

    /** The manager of message. (NotNull: after initialization) */
    @Resource
    private MessageManager messageManager;

    /** The translator of exception. (NotNull: after initialization) */
    @Resource
    private ExceptionTranslator exceptionTranslator;

    /** The manager of request. (NotNull: after initialization) */
    @Resource
    private RequestManager requestManager;

    /** The manager of response. (NotNull: after initialization) */
    @Resource
    private ResponseManager responseManager;

    /** The manager of session. (NotNull: after initialization) */
    @Resource
    private SessionManager sessionManager;

    /** The manager of API. (NotNull: after initialization) */
    @Resource
    private ApiManager apiManager;

    /** The manager of double submit using transaction token. (NotNull: after initialization) */
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
        return createGodHandPrologue(runtime).performPrologue(runtime);
    }

    protected GodHandPrologue createGodHandPrologue(ActionRuntime runtime) {
        final GodHandResource resource = createGodHandResource(runtime);
        final EmbeddedMessageKeySupplier supplier = newEmbeddedMessageKeySupplier();
        final AccessContextArranger arranger = newAccessContextArranger();
        return newGodHandPrologue(resource, supplier, arranger, () -> getUserBean(), () -> myAppType());
    }

    /**
     * New the arranger of access context.
     * <pre>
     * <span style="color: #77226C">return</span> <span style="color: #553000">resource</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #77226C">return</span> <span style="color: #553000">accessContextLogic</span>.create(<span style="color: #553000">resource</span>, () <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> myUserType(), () <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> getUserBean().map(userBean <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *         <span style="color: #77226C">return</span> <span style="color: #553000">userBean</span>.getUserId(); <span style="color: #3F7E5E">// as user expression</span>
     *     }), () <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> myAppType());
     * };
     * </pre>
     * @return The new-created instance of arranger. (NotNull)
     */
    protected abstract AccessContextArranger newAccessContextArranger();

    protected GodHandPrologue newGodHandPrologue(GodHandResource resource, EmbeddedMessageKeySupplier keySupplier,
            AccessContextArranger arranger, Supplier<OptionalThing<? extends UserBean<?>>> userBeanSupplier,
            Supplier<String> appTypeSupplier) {
        return new GodHandPrologue(resource, keySupplier, arranger, userBeanSupplier, appTypeSupplier);
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
        return createGodHandMonologue(runtime).performMonologue(runtime);
    }

    protected GodHandMonologue createGodHandMonologue(ActionRuntime runtime) {
        return newGodHandMonologue(createGodHandResource(runtime), newEmbeddedMessageKeySupplier(), handler -> {
            handleApplicationException(handler);
            return handler.getResponse();
        });
    }

    /**
     * Handle the application exception before framework's handling process.
     * <pre>
     * {@literal @}Override
     * <span style="color: #77226C">protected</span> void <span style="color: #CC4747">handleApplicationException</span>(ApplicationExceptionHandler <span style="color: #553000">handler</span>) {
     *     <span style="color: #77226C">super</span>.handleApplicationException(<span style="color: #553000">handler</span>);
     *     <span style="color: #553000">handler</span>.<span style="color: #994747">handle</span>(EntityAlreadyDeletedException.<span style="color: #77226C">class</span>, createMessages().addErrors...(<span style="color: #0000C0">GLOBAL</span>), <span style="color: #553000">cause</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *         <span style="color: #77226C">return</span> asHtml(<span style="color: #0000C0">path_...</span>);
     *     });
     * }
     * </pre>
     * @param handler The handler for the thrown application exception. (NotNull)
     */
    protected void handleApplicationException(ApplicationExceptionHandler handler) { // application may override
    }

    protected GodHandMonologue newGodHandMonologue(GodHandResource resource, EmbeddedMessageKeySupplier supplier, HandledAppExCall call) {
        return new GodHandMonologue(resource, supplier, call);
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
        createGodHandEpilogue(runtime).performEpilogue(runtime);
    }

    /**
     * @param runtime The runtime object of action. (NotNull)
     */
    protected void setupHtmlData(ActionRuntime runtime) { // application may override
    }

    protected GodHandEpilogue createGodHandEpilogue(ActionRuntime runtime) {
        return newGodHandEpilogue(createGodHandResource(runtime), createTooManySqlOption(runtime));
    }

    protected GodHandEpilogue newGodHandEpilogue(GodHandResource resource, TooManySqlOption tooManySqlOption) {
        return new GodHandEpilogue(resource, tooManySqlOption);
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
    protected GodHandResource createGodHandResource(ActionRuntime runtime) {
        return new GodHandResource(assistantDirector, timeManager, messageManager, exceptionTranslator, requestManager, responseManager,
                sessionManager, myLoginManager(), apiManager);
    }

    protected EmbeddedMessageKeySupplier newEmbeddedMessageKeySupplier() {
        return new SimpleEmbeddedMessageKeySupplier();
    }

    // ===================================================================================
    //                                                                           User Info
    //                                                                           =========
    // -----------------------------------------------------
    //                                      Application Info
    //                                      ----------------
    /**
     * Get the application type, e.g. for common column.
     * @return The application type basically fixed string. (NotNull) 
     */
    protected abstract String myAppType();

    // -----------------------------------------------------
    //                                            Login Info
    //                                            ----------
    /**
     * Get the bean of login user on session as interface type. (for application)
     * @return The optional thing of found user bean. (NotNull, EmptyAllowed: when not login)
     */
    protected abstract OptionalThing<? extends UserBean<?>> getUserBean();

    /**
     * Get my (application's) login manager. (for framework)
     * @return The optional instance of login manager. (NotNull, EmptyAllowed: if no login handling)
     */
    protected abstract OptionalThing<LoginManager> myLoginManager();

    /**
     * Get the user type of this applicatoin's login.
     * @return The optional expression of user type. (NotNull, EmptyAllowed: if no login handling) 
     */
    protected abstract OptionalThing<String> myUserType();

    // ===================================================================================
    //                                                                       Double Submit
    //                                                                       =============
    /**
     * Save the transaction token to session, using this action as group type.
     * <pre>
     * public HtmlResponse index() {
     *     ...
     *     <span style="color: #CC4747">saveToken()</span>;
     *     return asHtml(path_...);
     * }
     * 
     * e.g. Thymeleaf template
     *  &lt;input type="hidden" <span style="color: #CC4747">la:token="true"</span>/&gt;
     * </pre>
     */
    protected void saveToken() { // no return because automatically output by e.g. template framework
        doubleSubmitManager.saveToken(myTokenGroupType());
    }

    /**
     * Verify the request token (whether the requested token is same as saved token). <br>
     * And reset the saved token, it can be used only one-time. <br> 
     * It uses this action as group type. <br>
     * It should be after validate() or may be token-not-found exception if validation error.
     * <pre>
     * public HtmlResponse index(Integer memberId) {
     *     validate(form, messages <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {}, () <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *         return asHtml(path_...); <span style="color: #3F7E5E">// the html may use token...</span>
     *     });
     *     verifyToken(() <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> { <span style="color: #3F7E5E">// so should be called after validate()</span>
     *         return asHtml(path_...);
     *     });
     *     ...
     * }
     * </pre>
     * @param errorResponseLambda The hook to return action response when token error. (NotNull)
     * @throws DoubleSubmittedRequestException When the token is invalid. That has specified error hook.
     */
    protected void verifyToken(TokenErrorHook errorResponseLambda) {
        doubleSubmitManager.verifyToken(myTokenGroupType(), errorResponseLambda);
    }

    /**
     * Verify the request token (whether the requested token is same as saved token) <br>
     * Keep the saved token, so this method is basically for intermediate request. <br>
     * It uses this action as group type. <br>
     * @param errorResponseLambda The hook to return action response when token error. (NotNull)
     * @throws DoubleSubmittedRequestException When the token is invalid. That has specified error hook.
     */
    protected void verifyTokenKeep(TokenErrorHook errorResponseLambda) {
        doubleSubmitManager.verifyTokenKeep(myTokenGroupType(), errorResponseLambda);
    }

    protected Class<?> myTokenGroupType() {
        return ContainerUtil.toRealClassIfEnhanced(getClass());
    }

    // ===================================================================================
    //                                                                     Verify Anything
    //                                                                     ===============
    /**
     * Check the condition is true or it throws client error (e.g. 400 bad request) forcedly. <br>
     * You can use this in your action process against invalid URL parameters.
     * <pre>
     * verifyOrClientError("The pageNumber should be positive number: " + pageNumber, pageNumber &gt; 0);
     * </pre>
     * @param debugMsg The debug message for developer. (NotNull)
     * @param expectedBool The expected determination for your business, true or false. (false: e.g. 404 not found)
     */
    protected void verifyOrClientError(String debugMsg, boolean expectedBool) { // application may call
        assertArgumentNotNull("debugMsg", debugMsg);
        if (!expectedBool) {
            throwVerifiedClientError(debugMsg);
        }
    }

    protected void throwVerifiedClientError(String debugMsg) {
        throw responseManager.new400(debugMsg);
    }

    /**
     * Check the condition is true or it throws illegal transition forcedly. <br>
     * You can use this in your action process against strange request parameters.
     * <pre>
     * verifyOrIllegalTransition("The pageNumber should be positive number: " + pageNumber, pageNumber &gt; 0);
     * </pre>
     * @param debugMsg The message for exception message. (NotNull)
     * @param expectedBool The expected determination for your business, true or false. (false: illegal transition)
     */
    protected void verifyOrIllegalTransition(String debugMsg, boolean expectedBool) { // application may call
        assertArgumentNotNull("debugMsg", debugMsg);
        if (!expectedBool) {
            throwVerifiedIllegalTransition(debugMsg);
        }
    }

    protected void throwVerifiedIllegalTransition(String debugMsg) {
        throw new RequestIllegalTransitionException(debugMsg, newEmbeddedMessageKeySupplier().getErrorsAppIllegalTransitionKey());
    }
}
