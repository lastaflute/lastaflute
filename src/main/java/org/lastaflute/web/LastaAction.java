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

import javax.annotation.Resource;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.magic.async.AsyncManager;
import org.lastaflute.core.magic.async.ConcurrentAsyncCall;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.db.jta.stage.TransactionShow;
import org.lastaflute.db.jta.stage.TransactionStage;
import org.lastaflute.web.api.ApiManager;
import org.lastaflute.web.path.ActionPathResolver;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.response.StreamResponse;
import org.lastaflute.web.response.XmlResponse;
import org.lastaflute.web.response.next.ForwardNext;
import org.lastaflute.web.ruts.message.ActionMessages;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.request.ResponseManager;
import org.lastaflute.web.servlet.session.SessionManager;
import org.lastaflute.web.validation.ActionValidator;

/**
 * @author jflute
 */
public abstract class LastaAction {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final UrlChain EMPTY_URL_CHAIN = new UrlChain(null);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The manager of time. (NotNull) */
    @Resource
    private TimeManager timeManager;

    /** The manager of JSON. (NotNull) */
    @Resource
    private JsonManager jsonManager;

    /** The manager of message. (NotNull) */
    @Resource
    private MessageManager messageManager;

    /** The manager of asynchronous. (NotNull) */
    @Resource
    private AsyncManager asycnManager;

    /** The stage of transaction. (NotNull) */
    @Resource
    private TransactionStage transactionStage;

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

    /** The resolver of action, e.g. it can convert action type to action path. (NotNull) */
    @Resource
    private ActionPathResolver actionPathResolver;

    // ===================================================================================
    //                                                                          Validation
    //                                                                          ==========
    @SuppressWarnings("unchecked")
    protected <MESSAGES extends ActionMessages> ActionValidator<MESSAGES> createValidator() { // overridden as type-safe
        return new ActionValidator<MESSAGES>(requestManager, () -> (MESSAGES) createMessages(), myValidationGroups());
    }

    protected Class<?>[] myValidationGroups() { // you can override
        return ActionValidator.DEFAULT_GROUPS; // means default group
    }

    /**
     * Create the action messages basically for session errors or messages. (for application)
     * @return The new-created action messages provided from Struts. (NotNull)
     */
    protected ActionMessages createMessages() { // overridden as type-safe
        return new ActionMessages();
    }

    // ===================================================================================
    //                                                                            Response
    //                                                                            ========
    /**
     * Return as HTML response by the template. <br>
     * <pre>
     * &#064;Execute
     * <span style="color: #70226C">public</span> HtmlResponse index() {
     *     ...
     *     <span style="color: #70226C">return</span> <span style="color: #CC4747">asHtml</span>(<span style="color: #0000C0">path_Sea_SeaJsp</span>);
     * }
     * 
     * <span style="color: #3F7E5E">// you can register the data used in HTML template</span> 
     * &#064;Execute
     * <span style="color: #70226C">public</span> HtmlResponse index() {
     *     ...
     *     <span style="color: #70226C">return</span> <span style="color: #CC4747">asHtml</span>(<span style="color: #0000C0">path_Sea_SeaJsp</span>).renderWith(data <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *         data.<span style="color: #994747">register</span>("beans", beans);
     *     });
     * }
     * </pre>
     * @param path_ The path definition of forwarding next resource e.g. HTML template. (NotNull)
     * @return this. (NotNull)
     */
    protected HtmlResponse asHtml(ForwardNext path_) {
        assertArgumentNotNull("path_ (as forwardNext)", path_);
        return newHtmlResponseAsForward(path_);
    }

    protected HtmlResponse newHtmlResponseAsForward(ForwardNext forwardNext) {
        assertArgumentNotNull("forwardNext", forwardNext);
        return new HtmlResponse(forwardNext);
    }

    /**
     * Return response as JSON.
     * <pre>
     * public void index() {
     *     ...
     *     return asJson(bean);
     * }
     * </pre>
     * @param bean The bean object converted to JSON string. (NotNull)
     * @return The new-created bean for JSON response. (NotNull)
     */
    protected <BEAN> JsonResponse<BEAN> asJson(BEAN bean) {
        assertArgumentNotNull("bean", bean);
        return newJsonResponse(bean);
    }

    /**
     * New-create JSON response object.
     * @param bean The bean object converted to JSON string. (NotNull)
     * @return The new-created bean for JSON response. (NotNull)
     */
    protected <BEAN> JsonResponse<BEAN> newJsonResponse(BEAN bean) {
        assertArgumentNotNull("bean", bean);
        return new JsonResponse<BEAN>(bean);
    }

    /**
     * Return response as stream.
     * <pre>
     * e.g. simple (content-type is octet-stream or found by extension mapping)
     *  return asStream("classificationDefinitionMap.dfprop").stream(ins);
     * 
     * e.g. specify content-type
     *  return asStream("jflute.jpg").contentTypeJpeg().stream(ins);
     * </pre>
     * @param fileName The file name as data of the stream. (NotNull)
     * @return The new-created bean for XML response. (NotNull)
     */
    protected StreamResponse asStream(String fileName) {
        assertArgumentNotNull("fileName", fileName);
        return newStreamResponse(fileName);
    }

    /**
     * New-create stream response object.
     * @param fileName The file name as data of the stream. (NotNull)
     * @return The new-created bean for XML response. (NotNull)
     */
    protected StreamResponse newStreamResponse(String fileName) {
        return new StreamResponse(fileName);
    }

    /**
     * Return response as XML.
     * @param xmlStr The string of XML. (NotNull)
     * @return The new-created bean for XML response. (NotNull)
     */
    protected XmlResponse asXml(String xmlStr) {
        assertArgumentNotNull("xmlStr", xmlStr);
        return newXmlResponse(xmlStr);
    }

    /**
     * New-create XML response object.
     * @param xmlStr The string of XML. (NotNull)
     * @return The new-created bean for XML response. (NotNull)
     */
    protected XmlResponse newXmlResponse(String xmlStr) {
        return new XmlResponse(xmlStr);
    }

    // ===================================================================================
    //                                                                             Routing
    //                                                                             =======
    // -----------------------------------------------------
    //                                              Redirect
    //                                              --------
    /**
     * Redirect to the action (index method).
     * <pre>
     * <span style="color: #3F7E5E">// e.g. /member/edit/</span>
     * return redirect(MemberEditAction.class);
     *
     * <span style="color: #3F7E5E">// e.g. /member/</span>
     * return redirect(MemberAction.class);
     * </pre>
     * @param actionType The class type of action that it redirects to. (NotNull)
     * @return The HTML response for redirect. (NotNull)
     */
    protected HtmlResponse redirect(Class<?> actionType) {
        assertArgumentNotNull("actionType", actionType);
        return redirectWith(actionType, EMPTY_URL_CHAIN);
    }

    /**
     * Redirect to the action (index method) by the IDs on URL.
     * <pre>
     * <span style="color: #3F7E5E">// e.g. /member/edit/3/</span>
     * return redirectById(MemberEditAction.class, 3);
     *
     * <span style="color: #3F7E5E">// e.g. /member/edit/3/197/</span>
     * return redirectById(MemberEditAction.class, 3, 197);
     *
     * <span style="color: #3F7E5E">// e.g. /member/3/</span>
     * return redirectById(MemberAction.class, 3);
     * </pre>
     * @param actionType The class type of action that it redirects to. (NotNull)
     * @param ids The varying array for IDs. (NotNull)
     * @return The HTML response for redirect. (NotNull)
     */
    protected HtmlResponse redirectById(Class<?> actionType, Number... ids) {
        assertArgumentNotNull("actionType", actionType);
        assertArgumentNotNull("ids", ids);
        final Object[] objAry = (Object[]) ids; // to suppress warning
        return redirectWith(actionType, moreUrl(objAry));
    }

    /**
     * Redirect to the action (index method) by the parameters on GET.
     * <pre>
     * <span style="color: #3F7E5E">// e.g. /member/edit/?foo=3</span>
     * return redirectByParam(MemberEditAction.class, "foo", 3);
     *
     * <span style="color: #3F7E5E">// e.g. /member/edit/?foo=3&amp;bar=qux</span>
     * return redirectByParam(MemberEditAction.class, "foo", 3, "bar", "qux");
     *
     * <span style="color: #3F7E5E">// e.g. /member/?foo=3</span>
     * return redirectByParam(MemberAction.class, "foo", 3);
     * </pre>
     * @param actionType The class type of action that it redirects to. (NotNull)
     * @param params The varying array for the parameters on GET. (NotNull)
     * @return The HTML response for redirect. (NotNull)
     */
    protected HtmlResponse redirectByParam(Class<?> actionType, Object... params) {
        assertArgumentNotNull("actionType", actionType);
        assertArgumentNotNull("params", params);
        return redirectWith(actionType, params(params));
    }

    /**
     * Redirect to the action with the more URL parts and the the parameters on GET.
     * <pre>
     * <span style="color: #3F7E5E">// e.g. /member/edit/3/ *same as {@link #redirectById()}</span>
     * return redirectWith(MemberEditAction.class, <span style="color: #FD4747">moreUrl</span>(memberId));
     * 
     * <span style="color: #3F7E5E">// e.g. /member/edit/?memberId=3 *same as {@link #redirectByParam()}</span>
     * return redirectWith(MemberEditAction.class, <span style="color: #FD4747">params</span>("memberId", memberId));
     *
     * <span style="color: #3F7E5E">// e.g. /member/edit/3/</span>
     * return redirectWith(MemberAction.class, <span style="color: #FD4747">moreUrl</span>("edit", memberId));
     *
     * <span style="color: #3F7E5E">// e.g. /member/edit/3/#profile</span>
     * return redirectWith(MemberEditAction.class, <span style="color: #FD4747">moreUrl</span>(memberId).<span style="color: #FD4747">hash</span>("profile"));
     *
     * <span style="color: #3F7E5E">// e.g. /member/edit/?memberId=3#profile</span>
     * return redirectWith(MemberEditAction.class, <span style="color: #FD4747">params</span>("memberId", memberId).<span style="color: #FD4747">hash</span>("profile"));
     * </pre>
     * @param actionType The class type of action that it redirects to. (NotNull)
     * @param moreUrl_or_params The chain of URL. (NotNull)
     * @return The HTML response for redirect containing GET parameters. (NotNull)
     */
    protected HtmlResponse redirectWith(Class<?> actionType, UrlChain moreUrl_or_params) {
        assertArgumentNotNull("actionType", actionType);
        assertArgumentNotNull("moreUrl_or_params", moreUrl_or_params);
        return doRedirect(actionType, moreUrl_or_params);
    }

    /**
     * Do redirect the action with the more URL parts and the the parameters on GET. <br>
     * This method is to other redirect methods so normally you don't use directly from your action.
     * @param actionType The class type of action that it redirects to. (NotNull)
     * @param chain The chain of URL to build additional info on URL. (NotNull)
     * @return The HTML response for redirect containing GET parameters. (NotNull)
     */
    protected HtmlResponse doRedirect(Class<?> actionType, UrlChain chain) {
        assertArgumentNotNull("actionType", actionType);
        assertArgumentNotNull("chain", chain);
        return newHtmlResponseAsRediect(actionPathResolver.toActionUrl(actionType, true, chain));
    }

    protected HtmlResponse newHtmlResponseAsRediect(String redirectPath) {
        return HtmlResponse.fromRedirectPath(redirectPath);
    }

    /**
     * Handle the redirect URL to be MOVED_PERMANENTLY (301 redirect). <br>
     * Remove redirect mark and add header elements as 301 and return null.
     * <pre>
     * <span style="color: #3F7E5E">// e.g. /member/edit/</span>
     * return <span style="color: #FD4747">movedPermanently</span>(redirect(MemberEditAction.class));
     * </pre>
     * @param response The redirect URL with redirect mark for SAStruts. (NotNull)
     * @return The returned URL for execute method of SAStruts. (NullAllowed)
     */
    protected HtmlResponse movedPermanently(HtmlResponse response) {
        return responseManager.movedPermanently(response);
    }

    // -----------------------------------------------------
    //                                               Forward
    //                                               -------
    /**
     * Forward to the action (index method).
     * <pre>
     * <span style="color: #3F7E5E">// e.g. /member/edit/</span>
     * return forward(MemberEditAction.class);
     *
     * <span style="color: #3F7E5E">// e.g. /member/</span>
     * return forward(MemberAction.class);
     * </pre>
     * @param actionType The class type of action that it forwards to. (NotNull)
     * @return The HTML response for forward. (NotNull)
     */
    protected HtmlResponse forward(Class<?> actionType) {
        assertArgumentNotNull("actionType", actionType);
        return forwardWith(actionType, EMPTY_URL_CHAIN);
    }

    /**
     * Forward to the action (index method) by the IDs on URL.
     * <pre>
     * <span style="color: #3F7E5E">// e.g. /member/edit/3/</span>
     * return forwardById(MemberEditAction.class, 3);
     *
     * <span style="color: #3F7E5E">// e.g. /member/edit/3/197/</span>
     * return forwardById(MemberEditAction.class, 3, 197);
     *
     * <span style="color: #3F7E5E">// e.g. /member/3/</span>
     * return forwardById(MemberAction.class, 3);
     * </pre>
     * @param actionType The class type of action that it forwards to. (NotNull)
     * @param ids The varying array for IDs. (NotNull)
     * @return The HTML response for forward. (NotNull)
     */
    protected HtmlResponse forwardById(Class<?> actionType, Number... ids) {
        assertArgumentNotNull("actionType", actionType);
        assertArgumentNotNull("ids", ids);
        final Object[] objAry = (Object[]) ids; // to suppress warning
        return forwardWith(actionType, moreUrl(objAry));
    }

    /**
     * Forward to the action (index method) by the parameters on GET.
     * <pre>
     * <span style="color: #3F7E5E">// e.g. /member/edit/?foo=3</span>
     * return forwardByParam(MemberEditAction.class, "foo", 3);
     *
     * <span style="color: #3F7E5E">// e.g. /member/edit/?foo=3&amp;bar=qux</span>
     * return forwardByParam(MemberEditAction.class, "foo", 3, "bar", "qux");
     *
     * <span style="color: #3F7E5E">// e.g. /member/?foo=3</span>
     * return forwardByParam(MemberAction.class, "foo", 3);
     * </pre>
     * @param actionType The class type of action that it forwards to. (NotNull)
     * @param params The varying array for the parameters on GET. (NotNull)
     * @return The HTML response for forward. (NotNull)
     */
    protected HtmlResponse forwardByParam(Class<?> actionType, Object... params) {
        assertArgumentNotNull("actionType", actionType);
        assertArgumentNotNull("params", params);
        return forwardWith(actionType, params(params));
    }

    /**
     * Forward to the action with the more URL parts and the the parameters on GET.
     * <pre>
     * <span style="color: #3F7E5E">// e.g. /member/edit/3/ *same as {@link #forwardById()}</span>
     * return forwardWith(MemberEditAction.class, <span style="color: #FD4747">moreUrl</span>(memberId));
     *
     * <span style="color: #3F7E5E">// e.g. /member/edit/?memberId=3 *same as {@link #forwardByParam()}</span>
     * return forwardWith(MemberEditAction.class, <span style="color: #FD4747">params</span>("memberId", memberId));
     *
     * <span style="color: #3F7E5E">// e.g. /member/edit/3/</span>
     * return forwardWith(MemberAction.class, <span style="color: #FD4747">moreUrl</span>("edit", memberId));
     *
     * <span style="color: #3F7E5E">// e.g. /member/edit/3/#profile</span>
     * return forwardWith(MemberEditAction.class, <span style="color: #FD4747">moreUrl</span>(memberId).<span style="color: #FD4747">hash</span>("profile"));
     *
     * <span style="color: #3F7E5E">// e.g. /member/edit/?memberId=3#profile</span>
     * return forwardWith(MemberEditAction.class, <span style="color: #FD4747">params</span>("memberId", memberId).<span style="color: #FD4747">hash</span>("profile"));
     * </pre>
     * @param actionType The class type of action that it forwards to. (NotNull)
     * @param moreUrl_or_params The chain of URL. (NotNull)
     * @return The HTML response for forward containing GET parameters. (NotNull)
     */
    protected HtmlResponse forwardWith(Class<?> actionType, UrlChain moreUrl_or_params) {
        assertArgumentNotNull("actionType", actionType);
        assertArgumentNotNull("moreUrl_or_params", moreUrl_or_params);
        return doForward(actionType, moreUrl_or_params);
    }

    /**
     * Do forward the action with the more URL parts and the the parameters on GET. <br>
     * This method is to other forward methods so normally you don't use directly from your action.
     * @param actionType The class type of action that it forwards to. (NotNull)
     * @param chain The chain of URL to build additional info on URL. (NotNull)
     * @return The HTML response for forward containing GET parameters. (NotNull)
     */
    protected HtmlResponse doForward(Class<?> actionType, UrlChain chain) {
        assertArgumentNotNull("actionType", actionType);
        assertArgumentNotNull("chain", chain);
        return newHtmlResponseAsForward(actionPathResolver.toActionUrl(actionType, false, chain));
    }

    protected HtmlResponse newHtmlResponseAsForward(String redirectPath) {
        return HtmlResponse.fromForwardPath(redirectPath);
    }

    // -----------------------------------------------------
    //                                          Chain Method
    //                                          ------------
    /**
     * Set up more URL parts as URL chain. <br>
     * The name and specification of this method is synchronized with {@link UrlChain#moreUrl()}.
     * @param urlParts The varying array of URL parts. (NotNull)
     * @return The created instance of URL chain. (NotNull)
     */
    protected UrlChain moreUrl(Object... urlParts) {
        assertArgumentNotNull("urlParts", urlParts);
        return newUrlChain().moreUrl(urlParts);
    }

    /**
     * Set up parameters on GET as URL chain. <br>
     * The name and specification of this method is synchronized with {@link UrlChain#params()}.
     * @param paramsOnGet The varying array of parameters on GET. (NotNull)
     * @return The created instance of URL chain. (NotNull)
     */
    protected UrlChain params(Object... paramsOnGet) {
        assertArgumentNotNull("paramsOnGet", paramsOnGet);
        return newUrlChain().params(paramsOnGet);
    }

    /**
     * Set up hash on URL as URL chain. <br>
     * The name and specification of this method is synchronized with {@link UrlChain#hash()}.
     * @param hashOnUrl The value of hash on URL. (NotNull)
     * @return The created instance of URL chain. (NotNull)
     */
    protected UrlChain hash(Object hashOnUrl) {
        assertArgumentNotNull("hashOnUrl", hashOnUrl);
        return newUrlChain().hash(hashOnUrl);
    }

    protected UrlChain newUrlChain() {
        return new UrlChain(this);
    }

    // ===================================================================================
    //                                                                             Advance
    //                                                                             =======
    /**
     * Execute asynchronous process by other thread. <br>
     * <pre>
     * async(() <span style="font-size: 120%">-</span>&gt;</span> {
     *     ... <span style="color: #3F7E5E">// asynchronous process here</span>
     * });
     * </pre>
     * You can inherit...
     * <pre>
     * o ThreadCacheContext (copied plainly)
     * o AccessContext (copied as fixed value)
     * o CallbackContext (optional)
     * 
     * *attention: possibility of multiply threads access
     * </pre>
     * <p>Also you can change it from caller thread's one by interface default methods.</p>
     * @param noArgLambda The callback for asynchronous process. (NotNull)
     */
    protected void async(ConcurrentAsyncCall noArgLambda) {
        asycnManager.async(noArgLambda);
    }

    /**
     * Perform the show in transaction (always new transaction), roll-backed if exception.
     * <pre>
     * <span style="color: #3F7E5E">// if no return</span> 
     * requiresNew(<span style="color: #553000">tx</span> <span style="font-size: 120%">-</span>&gt;</span> {
     *     update(...); <span style="color: #3F7E5E">// already in transaction</span>
     *     insert(...); <span style="color: #3F7E5E">// also here</span>
     * });
     * 
     * <span style="color: #3F7E5E">// if returns anything to caller</span> 
     * <span style="color: #994747">Member member</span> = (Member)requiresNew(<span style="color: #553000">tx</span> <span style="font-size: 120%">-</span>&gt;</span> {
     *     update(...);
     *     Member member = select(...);
     *     <span style="color: #553000">tx</span>.<span style="color: #CC4747">returns</span>(member); <span style="color: #3F7E5E">// for return</span>
     * }).<span style="color: #994747">get()</span>; <span style="color: #3F7E5E">// optional handling</span>
     * </pre>
     * @param txLambda The callback for your transaction show on the stage. (NotNull)
     * @return The result of the transaction show. (NotNull, EmptyAllowed: when no result)
     */
    protected <RESULT> OptionalThing<RESULT> requiresNew(TransactionShow<RESULT> txLambda) {
        return transactionStage.requiresNew(txLambda);
    }

    // ===================================================================================
    //                                                                       Assert Helper
    //                                                                       =============
    /**
     * Assert that the object is not null.
     * @param variableName The name of assert-target variable. (NotNull)
     * @param value The value of argument. (NotNull)
     * @throws IllegalArgumentException When the value is null.
     */
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            String msg = "The value should not be null: variableName=null value=" + value;
            throw new IllegalArgumentException(msg);
        }
        if (value == null) {
            String msg = "The value should not be null: variableName=" + variableName;
            throw new IllegalArgumentException(msg);
        }
    }
}
