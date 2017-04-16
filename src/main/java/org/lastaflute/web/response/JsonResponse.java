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
package org.lastaflute.web.response;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dbflute.helper.StringKeyMap;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.web.aspect.RomanticActionCustomizer;

/**
 * @param <RESULT> The type of JSON result.
 * @author jflute
 */
public class JsonResponse<RESULT> implements ApiResponse {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final Object DUMMY = new Object();
    protected static final Class<?>[] EMPTY_TYPES = new Class<?>[0];
    protected static final JsonResponse<?> INSTANCE_OF_UNDEFINED = new JsonResponse<Object>(DUMMY).ofUndefined();

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RESULT jsonResult;
    protected String callback;
    protected Map<String, String[]> headerMap; // lazy loaded (for when no use)
    protected Integer httpStatus;
    protected boolean forcedlyJavaScript;
    protected boolean returnAsEmptyBody;
    protected boolean returnAsJsonDirectly;
    protected String directJson;
    protected boolean undefined;
    protected ResponseHook afterTxCommitHook;
    protected Class<?>[] validatorGroups;
    protected boolean validatorSuppressed;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    /**
     * Construct JSON response. <br>
     * This needs {@link RomanticActionCustomizer} in your customizer.dicon.
     * <pre>
     * <span style="color: #3F7E5E">// e.g. normal JSON response</span>
     * <span style="color: #70226C">return new</span> JsonResponse(result);
     * 
     * <span style="color: #3F7E5E">// e.g. JSONP response</span>
     * <span style="color: #70226C">return new</span> JsonResponse(result).asJsonp("callback");
     * </pre>
     * @param jsonResult The JSON result to send response. (NotNull)
     */
    public JsonResponse(RESULT jsonResult) {
        assertArgumentNotNull("jsonResult", jsonResult);
        this.jsonResult = jsonResult;
    }

    // ===================================================================================
    //                                                                              Header
    //                                                                              ======
    @Override
    public JsonResponse<RESULT> header(String name, String... values) {
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
            headerMap = StringKeyMap.createAsCaseInsensitiveOrdered();
        }
        return headerMap;
    }

    // ===================================================================================
    //                                                                         HTTP Status
    //                                                                         ===========
    @Override
    public JsonResponse<RESULT> httpStatus(int httpStatus) {
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
    public JsonResponse<RESULT> asJsonp(String callback) {
        assertArgumentNotNull("callback", callback);
        assertDefinedState("asJsonp");
        this.callback = callback;
        return this;
    }

    public JsonResponse<RESULT> forcedlyJavaScript() {
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

    protected JsonResponse<RESULT> ofEmptyBody() { // internal use
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

    protected JsonResponse<RESULT> ofJsonDirectly(String json) { // internal use
        assertArgumentNotNull("json", json);
        returnAsJsonDirectly = true; // for quick determination
        directJson = json;
        return this;
    }

    // -----------------------------------------------------
    //                                     Undefined Control
    //                                     -----------------
    /**
     * You cannot use in execute method. Only for action hook. <br>
     * If you return empty body, use asEmptyBody().
     * @return this. (NotNull)
     */
    @SuppressWarnings("unchecked")
    public static <OBJ> JsonResponse<OBJ> undefined() { // user interface
        return (JsonResponse<OBJ>) INSTANCE_OF_UNDEFINED;
    }

    protected JsonResponse<RESULT> ofUndefined() { // internal use
        undefined = true;
        return this;
    }

    // -----------------------------------------------------
    //                                         Response Hook
    //                                         -------------
    public JsonResponse<RESULT> afterTxCommit(ResponseHook noArgLambda) {
        assertArgumentNotNull("noArgLambda", noArgLambda);
        afterTxCommitHook = noArgLambda;
        return this;
    }

    // -----------------------------------------------------
    //                                             Validator
    //                                             ---------
    /**
     * @param groups The array of group types. (NullAllowed, EmptyAllowed: if null or empty, use default groups)
     * @return this. (NotNull)
     */
    public JsonResponse<RESULT> groupValidator(Class<?>... groups) {
        // allow null or empty to flexibly switch by condition
        final Class<?>[] filtered = filterValidatorGroups(groups);
        validatorGroups = filtered.length > 0 ? filtered : null;
        return this;
    }

    protected Class<?>[] filterValidatorGroups(Class<?>[] groups) {
        if (groups == null) { // just in case
            return EMPTY_TYPES;
        }
        // the groups may have null element, if groupValidator(hasSea ? Land.class : null)
        return Stream.of(groups).filter(group -> group != null).collect(Collectors.toList()).toArray(EMPTY_TYPES);
    }

    /**
     * @param suppressed Does it really suppress validator?
     * @return this. (NotNull)
     */
    public JsonResponse<RESULT> suppressValidator(boolean suppressed) { // argument to flexibly switch by condition
        validatorSuppressed = suppressed;
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
        final String jsonExp = jsonResult != null ? DfTypeUtil.toClassTitle(jsonResult) : null;
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
    public RESULT getJsonResult() {
        return jsonResult;
    }

    public RESULT getJsonBean() { // for compatible
        return jsonResult;
    }

    public OptionalThing<String> getCallback() {
        final Class<? extends Object> beanType = jsonResult.getClass();
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

    // -----------------------------------------------------
    //                                             Validator
    //                                             ---------
    public OptionalThing<Class<?>[]> getValidatorGroups() {
        return OptionalThing.ofNullable(validatorGroups, () -> {
            String msg = "Not found the validator groups: " + JsonResponse.this.toString();
            throw new IllegalStateException(msg);
        });
    }

    public boolean isValidatorSuppressed() {
        return validatorSuppressed;
    }
}
