/*
 * Copyright 2015-2019 the original author or authors.
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
package org.lastaflute.web.ruts.inoutlogging;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.servlet.filter.RequestLoggingFilter.NonShowAttribute;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author jflute
 * @since 1.0.0 (2017/08/11 Friday)
 */
public class InOutLogKeeper {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final InOutLogOption emptyOption = new InOutLogOption(); // should be immutable but non for now

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected InOutLogOption option; // null allowed
    protected LocalDateTime beginDateTime; // null allowed until beginning
    protected String processHash; // null allowed until beginning
    protected Map<String, Object> requestHeaderMap; // null allowed if request header name is not specified by InOutLogOption
    protected Map<String, Object> requestParameterMap; // null allowed if e.g. no parameter
    protected String requestBodyContent; // null allowed if e.g. no body
    protected String requestBodyType; // body format e.g. json, xml, null allowed if e.g. no body or null body
    protected Map<String, Object> responseHeaderMap; // null allowed if response header name is not specified by InOutLogOption
    protected String responseBodyContent; // null allowed if e.g. no body or null body
    protected String responseBodyType; // body format e.g. json, xml, null allowed until response or if e.g. no body
    protected Throwable frameworkCause; // runtime has only application's one so keep here, null allowed

    // ===================================================================================
    //                                                                  Core Determination
    //                                                                  ==================
    public static boolean isEnabled(RequestManager requestManager) {
        return requestManager.getActionAdjustmentProvider().isUseInOutLogging() && InOutLogger.isLoggerEnabled();
    }

    // ===================================================================================
    //                                                                      Prepare Keeper
    //                                                                      ==============
    public static OptionalThing<InOutLogKeeper> prepare(RequestManager requestManager) {
        return isEnabled(requestManager) ? OptionalThing.of(doPrepare(requestManager)) : OptionalThing.empty();
    }

    protected static InOutLogKeeper doPrepare(RequestManager requestManager) {
        final String key = LastaWebKey.INOUT_LOGGING_KEY;
        final OptionalThing<NonShowAttribute> optAttr = requestManager.getAttribute(key, NonShowAttribute.class);
        if (optAttr.isPresent()) {
            final NonShowAttribute attr = optAttr.get();
            return (InOutLogKeeper) attr.getAttribute();
        } else {
            final InOutLogKeeper keeper = new InOutLogKeeper();
            keeper.acceptOption(requestManager.getActionAdjustmentProvider().adjustInOutLogging()); // null allowed
            requestManager.setAttribute(key, new NonShowAttribute(keeper));
            return keeper;
        }
    }

    // ===================================================================================
    //                                                                       Accept Option
    //                                                                       =============
    public void acceptOption(InOutLogOption option) { // null allowed if no settings
        this.option = option;
    }

    // ===================================================================================
    //                                                                         Keep Facade
    //                                                                         ===========
    public void keepBeginDateTime(LocalDateTime beginDateTime) {
        assertArgumentNotNull("beginDateTime", beginDateTime);
        this.beginDateTime = beginDateTime;
    }

    public void keepProcessHash(String processHash) {
        assertArgumentNotNull("processHash", processHash);
        this.processHash = processHash;
    }

    public void keepRequestHeader(Map<String, Object> requestHeaderMap) {
        assertArgumentNotNull("requestHeaderMap", requestHeaderMap);
        this.requestHeaderMap = requestHeaderMap;
    }

    public void keepRequestParameter(Map<String, Object> parameterMap) {
        assertArgumentNotNull("parameterMap", parameterMap);
        for (Entry<String, Object> entry : parameterMap.entrySet()) {
            addRequestParameter(entry.getKey(), entry.getValue());
        }
    }

    protected void addRequestParameter(String key, Object value) { // value may be null!? accept it just in case
        assertArgumentNotNull("key", key);
        if (requestParameterMap == null) {
            requestParameterMap = new LinkedHashMap<String, Object>();
        }
        requestParameterMap.put(key, value);
    }

    public void keepRequestBody(String requestBodyContent, String requestBodyType) { // accept null just in case
        assertArgumentNotNull("requestBodyType", requestBodyType);
        this.requestBodyContent = requestBodyContent;
        this.requestBodyType = requestBodyType;
    }

    public void keepResponseHeader(Map<String, Object> responseHeaderMap) {
        assertArgumentNotNull("responseHeaderMap", responseHeaderMap);
        this.responseHeaderMap = responseHeaderMap;
    }

    public void keepResponseBody(String responseBodyContent, String responseBodyType) { // accept null just in case
        assertArgumentNotNull("responseBodyType", responseBodyType);
        this.responseBodyContent = responseBodyContent;
        this.responseBodyType = responseBodyType;
    }

    public void keepFrameworkCause(Throwable frameworkCause) {
        assertArgumentNotNull("frameworkCause", frameworkCause);
        this.frameworkCause = frameworkCause;
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            throw new IllegalArgumentException("The variableName should not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
        }
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public InOutLogOption getOption() { // not null, use empty instance because of several times called
        return option != null ? option : emptyOption;
    }

    public OptionalThing<LocalDateTime> getBeginDateTime() {
        return OptionalThing.ofNullable(beginDateTime, () -> {
            throw new IllegalStateException("Not found the begin date-time.");
        });
    }

    public OptionalThing<String> getProcessHash() {
        return OptionalThing.ofNullable(processHash, () -> {
            throw new IllegalStateException("Not found the process hash.");
        });
    }

    public Map<String, Object> getRequestHeaderMap() { // not null
        return requestHeaderMap != null ? Collections.unmodifiableMap(requestHeaderMap) : Collections.emptyMap();
    }

    public Map<String, Object> getRequestParameterMap() { // not null
        return requestParameterMap != null ? Collections.unmodifiableMap(requestParameterMap) : Collections.emptyMap();
    }

    public OptionalThing<String> getRequestBodyContent() {
        return OptionalThing.ofNullable(requestBodyContent, () -> {
            throw new IllegalStateException("Not found the request body content.");
        });
    }

    public OptionalThing<String> getRequestBodyType() {
        return OptionalThing.ofNullable(requestBodyType, () -> {
            throw new IllegalStateException("Not found the request body type.");
        });
    }

    public Map<String, Object> getResponseHeaderMap() { // not null
        return responseHeaderMap != null ? Collections.unmodifiableMap(responseHeaderMap) : Collections.emptyMap();
    }

    public OptionalThing<String> getResponseBodyContent() {
        return OptionalThing.ofNullable(responseBodyContent, () -> {
            throw new IllegalStateException("Not found the response body content.");
        });
    }

    public OptionalThing<String> getResponseBodyType() {
        return OptionalThing.ofNullable(responseBodyType, () -> {
            throw new IllegalStateException("Not found the response body type.");
        });
    }

    public OptionalThing<Throwable> getFrameworkCause() {
        return OptionalThing.ofNullable(frameworkCause, () -> {
            throw new IllegalStateException("Not found the framework cause.");
        });
    }
}
