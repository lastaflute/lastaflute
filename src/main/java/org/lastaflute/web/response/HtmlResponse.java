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
package org.lastaflute.web.response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.web.response.next.ForwardNext;
import org.lastaflute.web.response.next.RedirectNext;
import org.lastaflute.web.response.next.RedirectNext.RedirectPathStyle;
import org.lastaflute.web.response.next.RoutingNext;
import org.lastaflute.web.response.pushed.PushedFormInfo;
import org.lastaflute.web.response.pushed.PushedFormOpCall;
import org.lastaflute.web.response.pushed.PushedFormOption;
import org.lastaflute.web.response.render.RenderDataRegistration;
import org.lastaflute.web.servlet.request.Redirectable;

/**
 * @author jflute
 */
public class HtmlResponse implements ActionResponse, Redirectable {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final ForwardNext DUMMY = new ForwardNext("dummy");
    protected static final HtmlResponse INSTANCE_OF_UNDEFINED = new HtmlResponse(DUMMY).ofUndefined();

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RoutingNext nextRouting;
    protected Map<String, String[]> headerMap; // lazy loaded (for when no use)
    protected Integer httpStatus;
    protected boolean undefined;
    protected boolean returnAsEmptyBody;
    protected List<RenderDataRegistration> registrationList; // lazy loaded
    protected PushedFormInfo pushedFormInfo; // null allowed
    protected boolean errorsToSession;
    protected ResponseHook afterTxCommitHook;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public HtmlResponse(ForwardNext forwardNext) {
        assertArgumentNotNull("forwardNext", forwardNext);
        this.nextRouting = forwardNext;
    }

    public HtmlResponse(RedirectNext redirectNext) {
        assertArgumentNotNull("redirectNext", redirectNext);
        this.nextRouting = redirectNext;
    }

    // -----------------------------------------------------
    //                                             from Path
    //                                             ---------
    public static HtmlResponse fromForwardPath(String forwardPath) {
        return new HtmlResponse(new ForwardNext(forwardPath));
    }

    /**
     * Create HTML response by plain text URL. <br>
     * Basically for outer resources. e.g. http://dbflute.org <br>
     * Or inner resources in same context. e.g. /product/list/ (actually to /harbor/product/list/)
     * @param redirectPath The path for redirect. e.g. /product/list/, http://dbflute.org (NotNull)
     * @return The new-created instance of HTML response. (NotNull)
     */
    public static HtmlResponse fromRedirectPath(String redirectPath) {
        return new HtmlResponse(new RedirectNext(redirectPath, RedirectPathStyle.INNER));
    }

    /**
     * Create HTML response by plain text URL as is. <br>
     * Basically for outer resources. e.g. http://dbflute.org <br>
     * Or resources in same domain without context path adjustment. e.g. /harbor/product/list/
     * @param redirectPath The path for redirect. e.g. /harbor/product/list/, http://dbflute.org (NotNull)
     * @return The new-created instance of HTML response. (NotNull)
     */
    public static HtmlResponse fromRedirectPathAsIs(String redirectPath) {
        return new HtmlResponse(new RedirectNext(redirectPath, RedirectPathStyle.AS_IS));
    }

    // ===================================================================================
    //                                                                              Header
    //                                                                              ======
    @Override
    public HtmlResponse header(String name, String... values) {
        assertArgumentNotNull("name", name);
        assertArgumentNotNull("value", values);
        assertDefinedState("header");
        final Map<String, String[]> headerMap = prepareHeaderMap();
        if (headerMap.containsKey(name)) {
            throw new IllegalStateException("Already exists the header: name=" + name + " existing=" + headerMap);
        }
        headerMap.put(name, values);
        return this;
    }

    @Override
    public Map<String, String[]> getHeaderMap() {
        return headerMap != null ? Collections.unmodifiableMap(headerMap) : DfCollectionUtil.emptyMap();
    }

    protected Map<String, String[]> prepareHeaderMap() {
        if (headerMap == null) {
            headerMap = new LinkedHashMap<String, String[]>(4);
        }
        return headerMap;
    }

    // ===================================================================================
    //                                                                         HTTP Status
    //                                                                         ===========
    @Override
    public HtmlResponse httpStatus(int httpStatus) {
        assertDefinedState("httpStatus");
        this.httpStatus = httpStatus;
        return this;
    }

    @Override
    public Integer getHttpStatus() {
        return httpStatus;
    }

    // ===================================================================================
    //                                                                         Render Data
    //                                                                         ===========
    /**
     * Set up the HTML response rendering the HTML with registered data. <br>
     * And you can use the registered data (variable) in your HTML template.
     * <pre>
     * &#064;Execute
     * <span style="color: #70226C">public</span> HtmlResponse index() {
     *     ...
     *     <span style="color: #70226C">return</span> asHtml(<span style="color: #0000C0">path_Sea_SeaJsp</span>).<span style="color: #CC4747">renderWith</span>(<span style="color: #553000">data</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *         <span style="color: #553000">data</span>.<span style="color: #994747">register</span>(<span style="color: #2A00FF">"beans"</span>, <span style="color: #553000">beans</span>);
     *     });
     * }
     * </pre>
     * @param dataLambda The callback for data registration to HTML template. (NotNull)
     * @return this. (NotNull)
     */
    public HtmlResponse renderWith(RenderDataRegistration dataLambda) {
        assertArgumentNotNull("dataLambda", dataLambda);
        assertDefinedState("renderWith");
        if (registrationList == null) {
            registrationList = new ArrayList<RenderDataRegistration>(4);
        }
        registrationList.add(dataLambda);
        return this;
    }

    // ===================================================================================
    //                                                                         Action Form
    //                                                                         ===========
    /**
     * Set up the HTML response as using action form without requested initial value. <br>
     * And you can use the action form in your HTML template.
     * <pre>
     * <span style="color: #3F7E5E">// case of no requested initial value, empty display, but needs form</span>
     * &#064;Execute
     * <span style="color: #70226C">public</span> HtmlResponse index() {
     *     <span style="color: #70226C">return</span> asHtml(<span style="color: #0000C0">path_Sea_SeaJsp</span>).<span style="color: #CC4747">useForm</span>(SeaForm.<span style="color: #70226C">class</span>);
     * }
     * </pre>
     * @param formType The type of action form. (NotNull)
     * @return this. (NotNull)
     */
    public HtmlResponse useForm(Class<?> formType) {
        assertArgumentNotNull("formType", formType);
        this.pushedFormInfo = createPushedFormInfo(formType);
        return this;
    }

    protected PushedFormInfo createPushedFormInfo(Class<?> formType) {
        return new PushedFormInfo(formType);
    }

    /**
     * Set up the HTML response as using action form with internal initial value. <br>
     * And you can use the action form in your HTML template.
     * <pre>
     * <span style="color: #3F7E5E">// case of internal initial value</span>
     * &#064;Execute
     * <span style="color: #70226C">public</span> HtmlResponse index() {
     *     <span style="color: #70226C">return</span> asHtml(<span style="color: #0000C0">path_Sea_SeaJsp</span>).<span style="color: #CC4747">useForm</span>(SeaForm.<span style="color: #70226C">class</span>, op <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> op.<span style="color: #994747">setup</span>(form <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> ...));
     * }
     * </pre>
     * @param formType The type of action form. (NotNull)
     * @param opLambda The callback for option of the form, provides new-created form instance, so you can setup it. (NotNull)
     * @return this. (NotNull)
     */
    public <FORM> HtmlResponse useForm(Class<FORM> formType, PushedFormOpCall<FORM> opLambda) {
        assertArgumentNotNull("formType", formType);
        assertArgumentNotNull("opLambda", opLambda);
        this.pushedFormInfo = createPushedFormInfo(formType, createPushedFormOption(opLambda));
        return this;
    }

    protected <FORM> PushedFormOption<FORM> createPushedFormOption(PushedFormOpCall<FORM> opLambda) {
        final PushedFormOption<FORM> formOption = new PushedFormOption<FORM>();
        opLambda.callback(formOption);
        return formOption;
    }

    protected <FORM> PushedFormInfo createPushedFormInfo(Class<FORM> formType, PushedFormOption<FORM> formOption) {
        return new PushedFormInfo(formType, formOption);
    }

    // ===================================================================================
    //                                                                              Option
    //                                                                              ======
    // -----------------------------------------------------
    //                                            Empty Body
    //                                            ----------
    public static HtmlResponse asEmptyBody() { // user interface
        return new HtmlResponse(DUMMY).ofEmptyBody();
    }

    protected HtmlResponse ofEmptyBody() { // internal use
        returnAsEmptyBody = true;
        return this;
    }

    // -----------------------------------------------------
    //                                     Undefined Control
    //                                     -----------------
    public static HtmlResponse undefined() { // user interface
        return INSTANCE_OF_UNDEFINED;
    }

    protected HtmlResponse ofUndefined() { // internal use
        undefined = true;
        return this;
    }

    // -----------------------------------------------------
    //                                     Errors to Session
    //                                     -----------------
    /**
     * Save errors in request to session scope. (redirect only use) <br>
     * You can use the errors in next action after redirection.
     */
    public void saveErrorsToSession() {
        if (!isRedirectTo()) {
            String msg = "Not allowed operation when forward: saveErrorsToSession(): " + toString();
            throw new IllegalStateException(msg);
        }
        errorsToSession = true;
    }

    // -----------------------------------------------------
    //                                         Response Hook
    //                                         -------------
    @Override
    public HtmlResponse afterTxCommit(ResponseHook noArgLambda) {
        assertArgumentNotNull("noArgLambda", noArgLambda);
        afterTxCommitHook = noArgLambda;
        return this;
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected void assertArgumentNotNull(String title, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + title + "' should not be null.");
        }
    }

    protected void assertDefinedState(String methodName) {
        if (undefined) {
            throw new IllegalStateException("undefined response: method=" + methodName + "() this=" + toString());
        }
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final String classTitle = DfTypeUtil.toClassTitle(this);
        final String emptyExp = returnAsEmptyBody ? ", emptyBody" : "";
        final String undefinedExp = undefined ? ", undefined" : "";
        return classTitle + ":{" + nextRouting + emptyExp + undefinedExp + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public RoutingNext getNextRouting() {
        return nextRouting;
    }

    public String getRoutingPath() {
        return getNextRouting().getRoutingPath();
    }

    public boolean isRedirectTo() {
        return nextRouting instanceof RedirectNext;
    }

    public boolean isAsIs() {
        return getNextRouting().isAsIs();
    }

    @Override
    public boolean isReturnAsEmptyBody() {
        return returnAsEmptyBody;
    }

    @Override
    public boolean isUndefined() {
        return undefined;
    }

    public List<RenderDataRegistration> getRegistrationList() {
        return registrationList != null ? registrationList : Collections.emptyList();
    }

    public OptionalThing<PushedFormInfo> getPushedFormInfo() {
        return OptionalThing.ofNullable(pushedFormInfo, () -> {
            throw new IllegalStateException("Not found the pushed form info in the HTML response: " + nextRouting);
        });
    }

    public boolean isErrorsToSession() {
        return errorsToSession;
    }

    // -----------------------------------------------------
    //                                         Response Hook
    //                                         -------------
    public OptionalThing<ResponseHook> getAfterTxCommitHook() {
        return OptionalThing.ofNullable(afterTxCommitHook, () -> {
            String msg = "Not found the response hook: " + HtmlResponse.this.toString();
            throw new IllegalStateException(msg);
        });
    }
}
