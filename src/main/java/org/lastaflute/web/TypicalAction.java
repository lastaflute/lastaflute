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

import org.dbflute.jdbc.Classification;
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
import org.lastaflute.web.util.LaActionRuntimeUtil;
import org.lastaflute.web.util.LaDBFluteUtil;
import org.lastaflute.web.util.LaDBFluteUtil.ClassificationConvertFailureException;
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
        final TypicalGodHandResource resource = createTypicalGodHandResource();
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
        return createTypicalGodHandMonologue().performMonologue(runtimeMeta);
    }

    protected TypicalGodHandMonologue createTypicalGodHandMonologue() {
        final TypicalGodHandResource resource = createTypicalGodHandResource();
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
        return newTypicalGodHandEpilogue(createTypicalGodHandResource());
    }

    protected TypicalGodHandActionEpilogue newTypicalGodHandEpilogue(TypicalGodHandResource resource) {
        return new TypicalGodHandActionEpilogue(resource);
    }

    // -----------------------------------------------------
    //                                      Resource Factory
    //                                      ----------------
    protected TypicalGodHandResource createTypicalGodHandResource() {
        final OptionalThing<LoginManager> loginManager = myLoginManager();
        return new TypicalGodHandResource(requestManager, responseManager, sessionManager, loginManager, apiManager, exceptionTranslator);
    }

    // ===================================================================================
    //                                                                           User Info
    //                                                                           =========
    /**
     * Get the bean of login user on session as interface type. (for application)
     * @return The optional thing of found user bean. (NotNull, EmptyAllowed: when not login)
     */
    protected abstract OptionalThing<? extends UserBean> getUserBean();

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
    //                                                                              Verify
    //                                                                              ======
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
        lets404(msg);
    }

    // -----------------------------------------------------
    //                                         Verify or ...
    //                                         -------------
    /**
     * Check the condition is true or it throws 404 not found forcedly. <br>
     * You can use this in your action process against invalid URL parameters.
     * @param msg The message for exception message. (NotNull)
     * @param expectedBool The expected determination for your business, true or false. (false: 404 not found)
     */
    protected void verifyTrueOr404NotFound(String msg, boolean expectedBool) { // application may call
        if (!expectedBool) {
            lets404(msg);
        }
    }

    /**
     * Check the condition is true or it throws illegal transition forcedly. <br>
     * You can use this in your action process against strange request parameters.
     * @param msg The message for exception message. (NotNull)
     * @param expectedBool The expected determination for your business, true or false. (false: illegal transition)
     */
    protected void verifyTrueOrIllegalTransition(String msg, boolean expectedBool) { // application may call
        if (!expectedBool) {
            letsIllegalTransition(msg);
        }
    }

    protected HtmlResponse lets404(String msg) { // e.g. used by error handling of validation for GET parameter
        throw new ForcedRequest404NotFoundException(msg);
    }

    protected void letsIllegalTransition(String msg) {
        final String transitionKey = newTypicalEmbeddedKeySupplier().getErrorsAppIllegalTransitionKey();
        throw new ForcedIllegalTransitionApplicationException(msg, transitionKey);
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
        } catch (ClassificationConvertFailureException e) {
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

    /**
     * <pre>
     * o Cls : is Classification (CDef)
     * o CDef : is auto-generated ENUM as Classification Definition
     * </pre>
     */
    protected void documentOfWordDictionary() {
    }
}
