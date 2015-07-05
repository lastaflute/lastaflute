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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.web.aspect.RomanticActionCustomizer;

/**
 * @param <BEAN> The type of JSON bean.
 * @author jflute
 */
public class JsonResponse<BEAN> implements ApiResponse {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final Object DUMMY = new Object();
    protected static final JsonResponse<?> INSTANCE_OF_UNDEFINED = new JsonResponse<Object>(DUMMY).ofUndefined();

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final BEAN jsonBean;
    protected String callback;
    protected Map<String, String[]> headerMap; // lazy loaded (for when no use)
    protected Integer httpStatus;

    protected boolean forcedlyJavaScript;
    protected boolean undefined;
    protected boolean returnAsEmptyBody;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    /**
     * Construct JSON response. <br>
     * This needs {@link RomanticActionCustomizer} in your customizer.dicon.
     * <pre>
     * <span style="color: #3F7E5E">// e.g. normal JSON response</span>
     * return new JsonResponse(bean);
     * 
     * <span style="color: #3F7E5E">// e.g. JSONP response</span>
     * return new JsonResponse(bean).asJsonp("callback");
     * </pre>
     * @param jsonObj The JSON object to send response. (NotNull)
     */
    public JsonResponse(BEAN jsonObj) {
        assertArgumentNotNull("jsonObj", jsonObj);
        this.jsonBean = jsonObj;
    }

    // ===================================================================================
    //                                                                              Header
    //                                                                              ======
    @Override
    public JsonResponse<BEAN> header(String name, String... values) {
        assertArgumentNotNull("name", name);
        assertArgumentNotNull("values", values);
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
    public JsonResponse<BEAN> httpStatus(int httpStatus) {
        assertDefinedState("httpStatus");
        this.httpStatus = httpStatus;
        return this;
    }

    @Override
    public Integer getHttpStatus() {
        return httpStatus;
    }

    // ===================================================================================
    //                                                                              Option
    //                                                                              ======
    // -----------------------------------------------------
    //                                                 JSON
    //                                                ------
    public JsonResponse<BEAN> asJsonp(String callback) {
        assertArgumentNotNull("callback", callback);
        assertDefinedState("asJsonp");
        this.callback = callback;
        return this;
    }

    public JsonResponse<BEAN> forcedlyJavaScript() {
        forcedlyJavaScript = true;
        assertDefinedState("forcedlyJavaScript");
        return this;
    }

    // -----------------------------------------------------
    //                                            Empty Body
    //                                            ----------
    @SuppressWarnings("unchecked")
    public static <OBJ> JsonResponse<OBJ> asEmptyBody() { // user interface
        return (JsonResponse<OBJ>) new JsonResponse<Object>(DUMMY).ofEmptyBody();
    }

    protected JsonResponse<BEAN> ofEmptyBody() { // internal use
        returnAsEmptyBody = true;
        return this;
    }

    // -----------------------------------------------------
    //                                     Undefined Control
    //                                     -----------------
    @SuppressWarnings("unchecked")
    public static <OBJ> JsonResponse<OBJ> undefined() { // user interface
        return (JsonResponse<OBJ>) INSTANCE_OF_UNDEFINED;
    }

    protected JsonResponse<BEAN> ofUndefined() { // internal use
        undefined = true;
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
        final String jsonExp = jsonBean != null ? DfTypeUtil.toClassTitle(jsonBean) : null;
        final String callbackExp = callback != null ? ", callback=" + callback : "";
        final String forcedlyJSExp = forcedlyJavaScript ? ", JavaScript" : "";
        final String emptyExp = returnAsEmptyBody ? ", emptyBody" : "";
        final String undefinedExp = undefined ? ", undefined" : "";
        return classTitle + ":{" + jsonExp + callbackExp + forcedlyJSExp + emptyExp + undefinedExp + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public BEAN getJsonBean() {
        return jsonBean;
    }

    public OptionalThing<String> getCallback() {
        final Class<? extends Object> beanType = jsonBean.getClass();
        return OptionalThing.ofNullable(callback, () -> {
            throw new IllegalStateException("Not found the callback in the JSON response: " + beanType);
        });
    }

    public boolean isForcedlyJavaScript() {
        return forcedlyJavaScript;
    }

    @Override
    public boolean isReturnAsEmptyBody() {
        return returnAsEmptyBody;
    }

    @Override
    public boolean isUndefined() {
        return undefined;
    }
}
