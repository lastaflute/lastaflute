/*
 * Copyright 2015-2024 the original author or authors.
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
package org.lastaflute.web.docs;

/**
 * @author jflute
 */
public interface LaActionDocs {

    /**
     * About Action class implementation.
     * <pre>
     * <span style="font-size: 130%; color: #553000">[URL Mapping]</span>
     * <span style="color: #0000C0">ProfilePassword</span>Action#<span style="color: #0000C0">change</span>() =&gt; <span style="color: #994747">/profile/password/change/</span>
     * <span style="color: #0000C0">Product</span>Action#<span style="color: #0000C0">list</span>() =&gt; <span style="color: #994747">/product/list/</span>
     * <span style="color: #0000C0">ProductList</span>Action#index() =&gt; <span style="color: #994747">/product/list/</span>
     * <span style="color: #0000C0">Mypage</span>Action#index() =&gt; <span style="color: #994747">/mypage/</span>
     * <span style="color: #0000C0">Mypage</span>Action#get$index() =&gt; <span style="color: #994747">/mypage/ of HTTP method: GET</span>
     * 
     * <span style="font-size: 130%; color: #553000">[Path Parameter]</span>
     * <span style="color: #70226C">public</span> HtmlResponse index(<span style="color: #994747">Integer pageNumber</span>) { <span style="color: #3F7E5E">// /product/list/3</span>
     * <span style="color: #70226C">public</span> HtmlResponse index(<span style="color: #994747">OptionalThing&lt;Integer&gt;</span> pageNumber) { <span style="color: #3F7E5E">// /product/list/3 or /product/list/</span>
     * 
     * <span style="color: #3F7E5E">// /product/list/mystic/piari/oneman/ (sea=mystic, land=oneman)</span>
     * &#064;Execute(<span style="color: #994747">urlPattern</span> = <span style="color: #2A00FF">"{}/@word/{}"</span>)
     * <span style="color: #70226C">public</span> HtmlResponse piari(String <span style="color: #553000">sea</span>, String <span style="color: #553000">land</span>) {
     * 
     * <span style="font-size: 130%; color: #553000">[Action Form]</span> <span style="color: #3F7E5E">// for POST, GET parameter or JSON body</span>
     * <span style="color: #70226C">public</span> HtmlResponse signin(<span style="color: #994747">SinginForm form</span>) { <span style="color: #3F7E5E">// POST (or also GET)</span>
     * 
     * <span style="color: #3F7E5E">// e.g. /.../list/3?favoriteCode=sea&amp;nextName=land</span>
     * <span style="color: #70226C">public</span> HtmlResponse list(<span style="color: #994747">Integer pageNumber, SinginForm form</span>) { <span style="color: #3F7E5E">// GET</span>
     * 
     * <span style="color: #3F7E5E">// e.g. /.../list/3 (with JSON in request body)</span>
     * <span style="color: #70226C">public</span> JsonResponse list(<span style="color: #994747">Integer pageNumber, SinginBody body</span>) { <span style="color: #3F7E5E">// JSON Body</span>
     * 
     * <span style="font-size: 130%; color: #553000">[Action Response]</span>
     * <span style="color: #70226C">return</span> asHtml(path_MyPage_MyPageHtml); <span style="color: #3F7E5E">// HTML template</span>
     * <span style="color: #70226C">return</span> asJson(bean); <span style="color: #3F7E5E">// JSON e.g. AJAX, API server</span>
     * <span style="color: #70226C">return</span> asStream(fileName).stream(...); <span style="color: #3F7E5E">// Stream e.g. download</span>
     * 
     * <span style="font-size: 130%; color: #553000">[Example Code]</span>
     * &#064;Execute
     * <span style="color: #70226C">public</span> HtmlResponse index() {
     *     ListResultBean&lt;Product&gt; <span style="color: #553000">productList</span> = <span style="color: #0000C0">productBhv</span>.selectList(<span style="color: #553000">cb</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *         <span style="color: #553000">cb</span>.query().addOrderBy_RegularPrice_Desc();
     *         <span style="color: #553000">cb</span>.fetchFirst(<span style="color: #2A00FF">3</span>);
     *     });
     *     ListResultBean&lt;MypageProductBean&gt; <span style="color: #553000">topProducts</span> = <span style="color: #553000">memberList</span>.mappingList(<span style="color: #553000">product</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *         <span style="color: #70226C">return new</span> MypageProductBean(<span style="color: #553000">product</span>);
     *     });
     *     <span style="color: #70226C">return</span> asHtml(<span style="color: #553000">path_Mypage_MypageHtml</span>).renderWith(<span style="color: #553000">data</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *         <span style="color: #553000">data</span>.register(<span style="color: #2A00FF">"topProducts"</span>, <span style="color: #553000">topProducts</span>);
     *     });
     * }
     * </pre>
     */
    default void document0_Action() {
        throw new IllegalStateException("Cannot call it");
    }

    /**
     * You can call the following methods defined at super class in your actions.
     * <pre>
     * <span style="font-size: 130%; color: #553000">[Validation]</span>
     * o validate(form, moreValidationLambda, validationErrorLambda) <span style="color: #3F7E5E">// validation by annotation and program</span>
     * o validateApi(form, moreValidationLambda) <span style="color: #3F7E5E">// for API</span>
     * o throwValidationError(validationMessagesLambda, validationErrorLambda) <span style="color: #3F7E5E">// by original validation</span>
     * o throwValidationErrorApi(validationMessagesLambda) <span style="color: #3F7E5E">// for API</span>
     * 
     * <span style="font-size: 130%; color: #553000">[Response]</span>
     * o asHtml(HTML template) <span style="color: #3F7E5E">// return response as HTML by template e.g. JSP</span>
     * o asJson(JSON bean) <span style="color: #3F7E5E">// return response as JSON from bean</span>
     * o asStream(file name for Stream) <span style="color: #3F7E5E">// return response as stream from input stream</span>
     * o asXml(XML string) <span style="color: #3F7E5E">// return response as stream from input stream</span>
     * 
     * <span style="font-size: 130%; color: #553000">[Routing]</span>
     * o redirect(actionType) <span style="color: #3F7E5E">// redirect to the action, /product/list</span>
     * o redirectById(actionType, ids) <span style="color: #3F7E5E">// by the ID, /product/list/3</span>
     * o redirectByParam(actionType, params) <span style="color: #3F7E5E">// by the GET parameter, /product/list?sea=mystic</span>
     * o redirectWith(actionType, moreUrl_or_params) <span style="color: #3F7E5E">// with various parameters</span>
     * o movedPermanently(response) <span style="color: #3F7E5E">// 301 redirect</span>
     * 
     * o forward(actionType) <span style="color: #3F7E5E">// redirect to the action, /product/list</span>
     * o forwardById(actionType, ids) <span style="color: #3F7E5E">// by the ID, /product/list/3</span>
     * o forwardByParam(actionType, params) <span style="color: #3F7E5E">// by the GET parameter, /product/list?sea=mystic</span>
     * o forwardWith(actionType, moreUrl_or_params) <span style="color: #3F7E5E">// with various parameters</span>
     * 
     * <span style="font-size: 130%; color: #553000">[User Info]</span>
     * o getUserBean() <span style="color: #3F7E5E">// get bean of login user</span>
     * o createMessages() <span style="color: #3F7E5E">// create user messages as type-safe</span>
     * 
     * <span style="font-size: 130%; color: #553000">[Double Submit]</span>
     * o saveToken() <span style="color: #3F7E5E">// save the transaction token to session</span>
     * o verifyToken() <span style="color: #3F7E5E">// verify the request token to prevent double submit</span>
     * o verifyTokenKeep() <span style="color: #3F7E5E">// verify the request token keeping the saved token</span>
     * 
     * <span style="font-size: 130%; color: #553000">[Verify Anything]</span>
     * o verifyOrClientError() <span style="color: #3F7E5E">// verify true or e.g. 400</span>
     * o verifyOrIllegalTransition() <span style="color: #3F7E5E">// verify true or illegal transition exception</span>
     * </pre>
     */
    default void document1_CallableSuperMethod() {
        throw new IllegalStateException("Cannot call it");
    }

    /**
     * You can call the following methods defined at super class in your actions.
     * <pre>
     * <span style="font-size: 130%; color: #553000">[Action Hook]</span>
     * o hookBefore(runtime) <span style="color: #3F7E5E">// hook before action execution</span>
     * o hookFinally(runtime) <span style="color: #3F7E5E">// hook finally of action execution</span>
     *
     * <span style="font-size: 130%; color: #553000">[HTML Response]</span>
     * o setupHtmlData(runtime) <span style="color: #3F7E5E">// set up data that always needs for HTML rendering</span>
     *
     * <span style="font-size: 130%; color: #553000">[Application Exception]</span>
     * o handleApplicationException(runtime, handler) <span style="color: #3F7E5E">// handle as your own rule</span>
     * </pre>
     */
    default void document2_OverridableSuperMethod() {
        throw new IllegalStateException("Cannot call it");
    }

    /**
     * You can inject the following (frequently used) LastaFlute components in your actions.
     * <pre>
     * <span style="font-size: 130%; color: #553000">[Core]</span>
     * o PrimaryCipher primaryCipher <span style="color: #3F7E5E">// e.g. encrypt(), decrypt(), oneway()</span>
     * o TimeManager timeManager <span style="color: #3F7E5E">// e.g. currentDate()</span>
     * o JsonManager jsonManager <span style="color: #3F7E5E">// e.g. fromJson(), toJson()</span>
     * o MessageManager messageManager <span style="color: #3F7E5E">// e.g. getMessage(), findMessage()</span>
     * o TemplateManager templateManager <span style="color: #3F7E5E">// e.g. parse()</span>
     * o AsyncManager asyncManager <span style="color: #3F7E5E">// e.g. async()</span>
     * o Postbox postbox <span style="color: #3F7E5E">// for Mail Postcard of MailFlute</span>
     * 
     * <span style="font-size: 130%; color: #553000">[DB]</span>
     * o TransactionStage transactionStage <span style="color: #3F7E5E">// e.g. required(), requiresNew()</span>
     * 
     * <span style="font-size: 130%; color: #553000">[Web]</span>
     * o RequestManager requestManager <span style="color: #3F7E5E">// e.g. getRequestPath(), findUserBean(), getUserLocale()</span>
     * o ResponseManager responseManager <span style="color: #3F7E5E">// e.g. new400(), new404(), getResponse()</span>
     * o SessionManager sessionManager <span style="color: #3F7E5E">// e.g. getAttribute(), errors()</span>
     * o CookieManager cookieManager <span style="color: #3F7E5E">// e.g. setCookie(), setCookieCiphered(), getCookie()</span>
     * o CsrfManager csrfManager <span style="color: #3F7E5E">// e.g. beginToken(), verifyToken()</span>
     * </pre>
     */
    default void document3_InjectableLastaComponent() {
        throw new IllegalStateException("Cannot call it");
    }

    /**
     * You can inject the following (frequently used) quick components in your actions. <br>
     * These components are registered by suffix naming (e.g. ...Assist, ...Logic).
     * <pre>
     * <span style="font-size: 130%; color: #553000">[Action Assist]</span>
     * The assist of action, that can depend on web and view specification.
     * You can organize action's codes by assists.
     *  app
     *   |-logic
     *   |-web
     *      |-base
     *      |  |-login
     *      |  |  |-<span style="color: #994747">HarborLoginAssist.java</span> <span style="color: #3F7E5E">// common assist in actions</span>
     *      |  |-HarborBaseAction.java
     *      |-sea
     *         |-SeaLandAction.java
     *         |-<span style="color: #994747">SeaLandAssist.java</span> <span style="color: #3F7E5E">// for the actions in the package</span>
     * 
     * <span style="font-size: 130%; color: #553000">[Business Logic]</span>
     * The logic of business rule, that can be recycled by other actions.
     * CANNOT depend on web and view specification. Keep pure logic to be called by everybody.
     *  app
     *   |-logic <span style="color: #3F7E5E">// cannot see web packages</span>
     *   |  |-i18n
     *   |     |-<span style="color: #994747">I18nDateLogic.java</span>
     *   |-web
     * </pre>
     */
    default void document4_InjectableQuickComponent() {
        throw new IllegalStateException("Cannot call it");
    }

    /**
     * <pre>
     * o Body : accepts JSON on the Request body
     * o CDef : is auto-generated ENUM as Classification Definition
     * o Cls : is Classification (CDef)
     * o DBFlute : O/R Mapper
     * o Form : accepts Form parameters
     * o LastaFlute : Typesafe Web Framework of LeAn STArtup
     * </pre>
     */
    default void document8_WordDictionary() {
        throw new IllegalStateException("Cannot call it");
    }
}
