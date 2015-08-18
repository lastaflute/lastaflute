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
     * 
     * <span style="font-size: 130%; color: #553000">[URL Parameter]</span>
     * <span style="color: #70226C">public</span> HtmlResponse index(<span style="color: #994747">int pageNumber</span>) { <span style="color: #3F7E5E">// /product/list/3</span>
     * <span style="color: #70226C">public</span> HtmlResponse index(<span style="color: #994747">OptionalThing&lt;Integer&gt;</span> pageNumber) { <span style="color: #3F7E5E">// /product/list/3 or /product/list/</span>
     * 
     * <span style="color: #3F7E5E">// /product/list/mystic/ikspiary/oneman/ (sea=mystic, land=oneman)</span>
     * &#064;Execute(<span style="color: #994747">urlPattern</span> = <span style="color: #2A00FF">"{}/ikspiary/{}"</span>)
     * <span style="color: #70226C">public</span> HtmlResponse index(String <span style="color: #553000">sea</span>, String <span style="color: #553000">land</span>) {
     * 
     * <span style="font-size: 130%; color: #553000">[Action Form]</span> <span style="color: #3F7E5E">// for POST, GET parameter</span>
     * <span style="color: #70226C">public</span> HtmlResponse doSignin(<span style="color: #994747">SinginForm form</span>) { <span style="color: #3F7E5E">// POST (or also GET)</span>
     * 
     * <span style="color: #3F7E5E">// e.g. /.../list/3?favoriteCode=sea&nextName=land</span>
     * <span style="color: #70226C">public</span> HtmlResponse list(<span style="color: #994747">Integer pageNumber, SinginForm form</span>) { <span style="color: #3F7E5E">// GET</span>
     * 
     * <span style="font-size: 130%; color: #553000">[Action Response]</span>
     * <span style="color: #70226C">return</span> asHtml(path_MyPage_MyPageJsp); <span style="color: #3F7E5E">// HTML template</span>
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
     *     <span style="color: #70226C">return</span> asHtml(<span style="color: #553000">path_Mypage_MypageJsp</span>).renderWith(<span style="color: #553000">data</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *         <span style="color: #553000">data</span>.register(<span style="color: #2A00FF">"topProducts"</span>, <span style="color: #553000">topProducts</span>);
     *     });
     * }
     * </pre>
     */
    default void document0_Action() {
        throw new IllegalStateException("Cannot call it");
    }

    /**
     * You can call the following methods defined at super class in your action class.
     * <pre>
     * <span style="font-size: 130%; color: #553000">[Validation]</span>
     * o validate(form, moreValidationLambda, validationErrorLambda) <span style="color: #3F7E5E">// validation by annotation and program</span>
     * o validateApi(form, moreValidationLambda) <span style="color: #3F7E5E">// for API</span>
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
     * o toActionUrl(actionType [with chain]) <span style="color: #3F7E5E">// build action URL by action type e.g. /product/list</span>
     * 
     * <span style="font-size: 130%; color: #553000">[Advance]</span>
     * o async(noArgLambda) <span style="color: #3F7E5E">// execute asynchronous process</span>
     * o requiresNew(txLambda) <span style="color: #3F7E5E">// execute new transaction process</span>
     * 
     * <span style="font-size: 130%; color: #553000">[User Info]</span>
     * o getUserBean() <span style="color: #3F7E5E">// get bean of login user</span>
     * 
     * <span style="font-size: 130%; color: #553000">[Verify]</span>
     * o verifyParameterExists() <span style="color: #3F7E5E">// verify parameter exists or 404 (as default)</span>
     * o verifyParameterTrue() <span style="color: #3F7E5E">// verify parameter's anything is true or 404 (as default)</span>
     * o verifyTrueOr404NotFound() <span style="color: #3F7E5E">// verify true or 404</span>
     * o verifyTrueOrIllegalTransition() <span style="color: #3F7E5E">// verify true or illegal transition exception</span>
     * 
     * <span style="font-size: 130%; color: #553000">[Small Helper]</span>
     * o currentDate() <span style="color: #3F7E5E">// current date as LocalDate</span>
     * o currentDateTime() <span style="color: #3F7E5E">// current date-time as LocalDateTime</span>
     * o isEmpty() <span style="color: #3F7E5E">// determine empty (or null)</span>
     * o isNotEmpty() <span style="color: #3F7E5E">// determine not empty (and not null)</span>
     * o isCls(cdefType, code) <span style="color: #3F7E5E">// determine the code matches the classification</span>
     * o toCls(cdefType, code) <span style="color: #3F7E5E">// convert the code to the classification</span>
     * </pre>
     */
    default void document1_CallableSuperMethod() {
        throw new IllegalStateException("Cannot call it");
    }

    /**
     * <pre>
     * o Cls : is Classification (CDef)
     * o CDef : is auto-generated ENUM as Classification Definition
     * </pre>
     */
    default void document8_WordDictionary() {
        throw new IllegalStateException("Cannot call it");
    }
}
