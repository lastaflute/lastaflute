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
package org.lastaflute.web;

import java.util.function.Supplier;

import javax.annotation.Resource;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.lastaflute.core.exception.ExceptionTranslator;
import org.lastaflute.core.exception.LaApplicationException;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.db.dbflute.accesscontext.AccessContextArranger;
import org.lastaflute.web.api.ApiManager;
import org.lastaflute.web.callback.ActionHook;
import org.lastaflute.web.callback.ActionRuntime;
import org.lastaflute.web.callback.TypicalEmbeddedKeySupplier;
import org.lastaflute.web.callback.TypicalGodHandActionEpilogue;
import org.lastaflute.web.callback.TypicalGodHandMonologue;
import org.lastaflute.web.callback.TypicalGodHandPrologue;
import org.lastaflute.web.callback.TypicalGodHandResource;
import org.lastaflute.web.callback.TypicalKey.TypicalSimpleEmbeddedKeySupplier;
import org.lastaflute.web.exception.ActionApplicationExceptionHandler;
import org.lastaflute.web.exception.ForcedIllegalTransitionApplicationException;
import org.lastaflute.web.exception.ForcedRequest404NotFoundException;
import org.lastaflute.web.login.LoginManager;
import org.lastaflute.web.login.UserBean;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.request.ResponseManager;
import org.lastaflute.web.servlet.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The typical action for your project. <br>
 * You should extend this class when making your project-base action. <br>
 * And you can add methods for all applications.
 * @author jflute
 */
public abstract class TypicalAction extends LastaAction implements ActionHook {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(TypicalAction.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The manager of time. (NotNull) */
    @Resource
    private TimeManager timeManager;

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
    public ActionResponse godHandPrologue(ActionRuntime runtimeMeta) { // fixed process
        return createTypicalGodHandPrologue().performPrologue(runtimeMeta);
    }

    protected TypicalGodHandPrologue createTypicalGodHandPrologue() {
        final TypicalGodHandResource resource = newTypicalGodHandResource();
        final AccessContextArranger arranger = newAccessContextArranger();
        return newTypicalGodHandPrologue(resource, arranger, () -> getUserBean(), () -> myAppType());
    }

    /**
     * New the arranger of access context.
     * @return The instance of arranger. (NotNull)
     */
    protected abstract AccessContextArranger newAccessContextArranger();

    protected TypicalGodHandPrologue newTypicalGodHandPrologue(TypicalGodHandResource resource, AccessContextArranger arranger,
            Supplier<OptionalThing<? extends UserBean>> userBeanSupplier, Supplier<String> appTypeSupplier) {
        return new TypicalGodHandPrologue(resource, arranger, userBeanSupplier, appTypeSupplier);
    }

    @Override
    public ActionResponse hookBefore(ActionRuntime runtimeMeta) { // application may override
        return ActionResponse.empty();
    }

    // -----------------------------------------------------
    //                                            on Failure
    //                                            ----------
    @Override
    public ActionResponse godHandMonologue(ActionRuntime runtimeMeta) { // fixed process
        return createTypicalGodHandExceptionMonologue().performMonologue(runtimeMeta);
    }

    protected TypicalGodHandMonologue createTypicalGodHandExceptionMonologue() {
        final TypicalGodHandResource resource = newTypicalGodHandResource();
        final TypicalEmbeddedKeySupplier supplier = newTypicalEmbeddedKeySupplier();
        final ActionApplicationExceptionHandler handler = newActionApplicationExceptionHandler();
        return newTypicalGodHandMonologue(resource, supplier, handler);
    }

    protected TypicalEmbeddedKeySupplier newTypicalEmbeddedKeySupplier() {
        return new TypicalSimpleEmbeddedKeySupplier();
    }

    protected ActionApplicationExceptionHandler newActionApplicationExceptionHandler() {
        return new ActionApplicationExceptionHandler() {
            public ActionResponse handle(LaApplicationException appEx) {
                return handleApplicationException(appEx);
            }
        };
    }

    /**
     * Handle the application exception before framework's handling process.
     * @param appEx The thrown application exception. (NotNull)
     * @return The response for the exception. (NullAllowed: if null, to next handling step)
     */
    protected ActionResponse handleApplicationException(LaApplicationException appEx) { // application may override
        return ActionResponse.empty();
    }

    protected TypicalGodHandMonologue newTypicalGodHandMonologue(TypicalGodHandResource resource, TypicalEmbeddedKeySupplier supplier,
            ActionApplicationExceptionHandler handler) {
        return new TypicalGodHandMonologue(resource, supplier, handler);
    }

    // -----------------------------------------------------
    //                                               Finally
    //                                               -------
    @Override
    public void hookFinally(ActionRuntime runtimeMeta) { // application may override
    }

    @Override
    public void godHandEpilogue(ActionRuntime runtimeMeta) { // fixed process
        createTypicalGodHandEpilogue().performEpilogue(runtimeMeta);
    }

    protected TypicalGodHandActionEpilogue createTypicalGodHandEpilogue() {
        return newTypicalGodHandEpilogue(newTypicalGodHandResource());
    }

    protected TypicalGodHandActionEpilogue newTypicalGodHandEpilogue(TypicalGodHandResource resource) {
        return new TypicalGodHandActionEpilogue(resource);
    }

    // -----------------------------------------------------
    //                                      Resource Factory
    //                                      ----------------
    protected TypicalGodHandResource newTypicalGodHandResource() {
        final OptionalThing<LoginManager> loginManager = myLoginManager();
        return new TypicalGodHandResource(requestManager, responseManager, sessionManager, loginManager, apiManager, exceptionTranslator);
    }

    // ===================================================================================
    //                                                                         My Resource
    //                                                                         ===========
    protected abstract String myAppType();

    /**
     * Get the bean of login user on session as interface type. (for application)
     * @return The optional thing of found user bean. (NotNull, EmptyAllowed: when not login)
     */
    protected abstract OptionalThing<? extends UserBean> getUserBean();

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
    //                                                                   Application Check
    //                                                                   =================
    // -----------------------------------------------------
    //                                       Check Parameter
    //                                       ---------------
    protected void verifyParameterExists(Object parameter) { // application may call
        logger.debug("...Verifying the parameter exists: {}", parameter);
        if (parameter == null || (parameter instanceof String && ((String) parameter).isEmpty())) {
            handleParameterFailure();
        }
    }

    protected void verifyParameterTrue(boolean expectedBool) { // application may call
        logger.debug("...Verifying the parameter is true: {}", expectedBool);
        if (!expectedBool) {
            handleParameterFailure();
        }
    }

    protected void handleParameterFailure() {
        lets404(); // no server error because it can occur by user's trick easily e.g. changing GET parameter
    }

    // -----------------------------------------------------
    //                                          Check or ...
    //                                          ------------
    /**
     * Check the condition is true or it throws 404 not found forcedly. <br>
     * You can use this in your action process against invalid URL parameters.
     * @param expectedBool The expected determination for your business, true or false. (false: 404 not found)
     */
    protected void verifyTrueOr404NotFound(boolean expectedBool) { // application may call
        logger.debug("...Verifying the condition is true or 404 not found: {}", expectedBool);
        if (!expectedBool) {
            lets404();
        }
    }

    /**
     * Check the condition is true or it throws illegal transition forcedly. <br>
     * You can use this in your action process against strange request parameters.
     * @param expectedBool The expected determination for your business, true or false. (false: illegal transition)
     */
    protected void verifyTrueOrIllegalTransition(boolean expectedBool) { // application may call
        logger.debug("...Verifying the condition is true or illegal transition: {}", expectedBool);
        if (!expectedBool) {
            letsIllegalTransition();
        }
    }

    protected HtmlResponse lets404() { // e.g. used by error handling of validation for GET parameter
        throw new ForcedRequest404NotFoundException("from lets404()");
    }

    protected void letsIllegalTransition() {
        final String transitionKey = newTypicalEmbeddedKeySupplier().getErrorsAppIllegalTransitionKey();
        throw new ForcedIllegalTransitionApplicationException(transitionKey);
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected boolean isEmpty(String str) {
        return Srl.is_Null_or_Empty(str);
    }

    protected boolean isNotEmpty(String str) {
        return Srl.is_NotNull_and_NotEmpty(str);
    }

    // ===================================================================================
    //                                                                            Document
    //                                                                            ========
    // TODO jflute lastaflute: [C] function: make document()
    /**
     * <pre>
     * [AtMark]Execute
     * public HtmlResponse index() {
     *     ListResultBean&lt;Product&gt; memberList = productBhv.selectList(cb -> {
     *         cb.query().addOrderBy_RegularPrice_Desc();
     *         cb.fetchFirst(3);
     *     });
     *     List&lt;MypageProductBean&gt; beans = memberList.stream().map(member -> {
     *         return new MypageProductBean(member);
     *     }).collect(Collectors.toList());
     *     return asHtml(path_Mypage_MypageJsp).renderWith(data -> {
     *         data.register("beans", beans);
     *     });
     * }
     * </pre>
     */
    protected void documentOfAll() {
    }

    /**
     * <pre>
     * o validate(form, error call): Hibernate Validator's Annotation only
     * o validateMore(form, your validation call, error call): annotation + by-method validation
     * 
     * o asHtml(HTML template): return response as HTML by template e.g. JSP
     * o asJson(JSON bean): return response as JSON from bean
     * o asStream(input stream): return response as stream from input stream
     * </pre>
     */
    protected void documentOfMethods() {
    }
}
