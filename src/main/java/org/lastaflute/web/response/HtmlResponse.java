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

import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.web.response.next.ForwardNext;
import org.lastaflute.web.response.next.RedirectNext;
import org.lastaflute.web.response.next.RedirectNext.RedirectPathStyle;
import org.lastaflute.web.response.next.RoutingNext;
import org.lastaflute.web.response.render.RenderDataRegistration;

/**
 * @author jflute
 */
public class HtmlResponse implements ActionResponse {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final ForwardNext DUMMY = new ForwardNext("dummy");
    protected static final HtmlResponse INSTANCE_OF_EMPTY = new HtmlResponse(DUMMY).asEmptyResponse();

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RoutingNext nextRouting;
    protected Map<String, String> headerMap; // lazy loaded (for when no use)
    protected boolean empty;
    protected boolean skipResponse;
    protected List<RenderDataRegistration> registrationList; // lazy loaded
    protected Class<?> pushedFormType; // optional
    protected boolean errorsToSession;

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

    public static HtmlResponse fromRedirectPath(String redirectPath) {
        return new HtmlResponse(new RedirectNext(redirectPath, RedirectPathStyle.INNER));
    }

    public static HtmlResponse fromRedirectPathAsIs(String redirectPath) {
        return new HtmlResponse(new RedirectNext(redirectPath, RedirectPathStyle.AS_IS));
    }

    // ===================================================================================
    //                                                                              Header
    //                                                                              ======
    // TODO jflute lastaflute: [E] function: Response header multiple value
    @Override
    public HtmlResponse header(String name, String value) {
        assertArgumentNotNull("name", name);
        assertArgumentNotNull("value", value);
        prepareHeaderMap().put(name, value);
        return this;
    }

    @Override
    public Map<String, String> getHeaderMap() {
        return headerMap != null ? Collections.unmodifiableMap(headerMap) : DfCollectionUtil.emptyMap();
    }

    protected Map<String, String> prepareHeaderMap() {
        if (headerMap == null) {
            headerMap = new LinkedHashMap<String, String>(4);
        }
        return headerMap;
    }

    // ===================================================================================
    //                                                                         Render Data
    //                                                                         ===========
    public HtmlResponse renderWith(RenderDataRegistration dataLambda) {
        assertArgumentNotNull("dataLambda", dataLambda);
        if (registrationList == null) {
            registrationList = new ArrayList<RenderDataRegistration>(4);
        }
        registrationList.add(dataLambda);
        return this;
    }

    // ===================================================================================
    //                                                                         Action Form
    //                                                                         ===========
    public HtmlResponse useForm(Class<?> formType) {
        assertArgumentNotNull("formType", formType);
        this.pushedFormType = formType;
        return this;
    }

    // ===================================================================================
    //                                                                              Option
    //                                                                              ======
    public static HtmlResponse empty() { // user interface
        return INSTANCE_OF_EMPTY;
    }

    protected HtmlResponse asEmptyResponse() { // internal use
        empty = true;
        return this;
    }

    /**
     * Save errors in request to session scope. (redirect only use) <br>
     * You can use the errors in next action after redirection.
     */
    protected void saveErrorsToSession() {
        if (!isRedirectTo()) {
            String msg = "Not allowed operation when forward: saveErrorsToSession(): " + toString();
            throw new IllegalStateException(msg);
        }
        errorsToSession = true;
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected void assertArgumentNotNull(String title, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + title + "' should not be null.");
        }
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final String classTitle = DfTypeUtil.toClassTitle(this);
        return classTitle + ":{" + nextRouting + ", empty=" + empty + ", skip=" + skipResponse + "}";
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
    public boolean isEmpty() {
        return empty;
    }

    @Override
    public boolean isSkip() {
        return skipResponse;
    }

    public List<RenderDataRegistration> getRegistrationList() {
        return registrationList != null ? registrationList : Collections.emptyList();
    }

    public Class<?> getPushedFormType() {
        return pushedFormType;
    }

    public boolean isErrorsToSession() {
        return errorsToSession;
    }
}
