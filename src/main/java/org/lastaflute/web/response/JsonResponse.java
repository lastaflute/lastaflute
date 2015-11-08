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
    protected boolean returnAsEmptyBody;
    protected boolean returnAsJsonDirectly;
    protected String directJson;
    protected boolean undefined;
    protected ResponseHook afterTxCommitHook;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    /**
     * Construct JSON response. <br>
     * This needs {@link RomanticActionCustomizer} in your customizer.dicon.
     * <pre>
     * <span style="color: #3F7E5E">// e.g. normal JSON response</span>
     * <span style="color: #70226C">return new</span> JsonResponse(bean);
     * 
     * <span style="color: #3F7E5E">// e.g. JSONP response</span>
     * <span style="color: #70226C">return new</span> JsonResponse(bean).asJsonp("callback");
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
    public OptionalThing<Integer> getHttpStatus() {
        return OptionalThing.ofNullable(httpStatus, () -> {
            throw new IllegalStateException("Not found the http status in the response: " + JsonResponse.this.toString());
        });
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
    //                                         Json Directly
    //                                         -------------
    @SuppressWarnings("unchecked")
    public static <OBJ> JsonResponse<OBJ> asJsonDirectly(String json) { // user interface
        return (JsonResponse<OBJ>) new JsonResponse<Object>(DUMMY).ofJsonDirectly(json);
    }

    protected JsonResponse<BEAN> ofJsonDirectly(String json) { // internal use
        returnAsJsonDirectly = true; // for quick determination
        directJson = json;
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

    // -----------------------------------------------------
    //                                         Response Hook
    //                                         -------------
    public JsonResponse<BEAN> afterTxCommit(ResponseHook noArgLambda) {
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
        final String jsonExp = jsonBean != null ? DfTypeUtil.toClassTitle(jsonBean) : null;
        final String callbackExp = callback != null ? ", callback=" + callback : "";
        final String forcedlyJSExp = forcedlyJavaScript ? ", JavaScript" : "";
        final String emptyExp = returnAsEmptyBody ? ", emptyBody" : "";
        final String directExp = returnAsJsonDirectly ? ", directly" : "";
        final String undefinedExp = undefined ? ", undefined" : "";
        return classTitle + ":{" + jsonExp + callbackExp + forcedlyJSExp + emptyExp + directExp + undefinedExp + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    // -----------------------------------------------------
    //                                                 Basic
    //                                                 -----
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

    // -----------------------------------------------------
    //                                            Empty Body
    //                                            ----------
    @Override
    public boolean isReturnAsEmptyBody() {
        return returnAsEmptyBody;
    }

    // -----------------------------------------------------
    //                                         Json Directly
    //                                         -------------
    public boolean isReturnAsJsonDirectly() { // quick determination
        return returnAsJsonDirectly;
    }

    public OptionalThing<String> getDirectJson() {
        return OptionalThing.ofNullable(directJson, () -> {
            String msg = "Not found the direct json: " + JsonResponse.this.toString();
            throw new IllegalStateException(msg);
        });
    }

    // -----------------------------------------------------
    //                                     Undefined Control
    //                                     -----------------
    @Override
    public boolean isUndefined() {
        return undefined;
    }

    // -----------------------------------------------------
    //                                         Response Hook
    //                                         -------------
    public OptionalThing<ResponseHook> getAfterTxCommitHook() {
        return OptionalThing.ofNullable(afterTxCommitHook, () -> {
            String msg = "Not found the response hook: " + JsonResponse.this.toString();
            throw new IllegalStateException(msg);
        });
    }
}
