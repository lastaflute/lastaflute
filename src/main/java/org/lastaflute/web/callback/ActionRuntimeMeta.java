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
package org.lastaflute.web.callback;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.dbflute.util.DfTypeUtil;
import org.lastaflute.web.api.ApiAction;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.response.ApiResponse;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.message.ActionMessages;
import org.lastaflute.web.util.LaParamWrapperUtil;

/**
 * @author jflute
 */
public class ActionRuntimeMeta {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ActionExecute execute;
    protected ActionResponse actionResponse;
    protected RuntimeException failureCause;
    protected ActionMessages validationErrors;
    protected Map<String, Object> displayDataMap; // lazy loaded

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionRuntimeMeta(ActionExecute execute) {
        this.execute = execute;
    }

    // ===================================================================================
    //                                                                      Basic Resource
    //                                                                      ==============
    /**
     * Get the method object of action execute.
     * @return The method object from execute configuration. (NotNull)
     */
    public Method getExecuteMethod() {
        return execute.getExecuteMethod();
    }

    /**
     * Is the action for API request? (contains e.g. JSON response return type)
     * @return The determination, true or false.
     */
    public boolean isApiAction() {
        final Method actionMethod = getExecuteMethod();
        if (ApiResponse.class.isAssignableFrom(actionMethod.getReturnType())) {
            return true; // if JSON response, this action can be treated as API without the marker interface
        }
        return ApiAction.class.isAssignableFrom(actionMethod.getDeclaringClass());
    }

    // ===================================================================================
    //                                                                      Execute Status
    //                                                                      ==============
    /**
     * Is the result of the action execute, forward to HTML template?
     * @return The determination, true or false.
     */
    public boolean isForwardToHtml() {
        if (!isHtmlResponse()) { // e.g. exception, AJAX
            return false;
        }
        final HtmlResponse htmlResponse = ((HtmlResponse) actionResponse);
        return !htmlResponse.isRedirectTo() && isHtmlTemplateResponse(htmlResponse);
    }

    protected boolean isHtmlTemplateResponse(final HtmlResponse htmlResponse) {
        final String routingPath = htmlResponse.getRoutingPath();
        return routingPath.endsWith(".html") || routingPath.endsWith(".jsp");
    }

    /**
     * Is the result of the action execute, redirect?
     * @return The determination, true or false.
     */
    public boolean isRedirectTo() {
        return isHtmlResponse() && ((HtmlResponse) actionResponse).isRedirectTo();
    }

    /**
     * Is the existing response HTML?
     * @return The determination, true or false.
     */
    protected boolean isHtmlResponse() {
        return actionResponse != null && actionResponse instanceof HtmlResponse;
    }

    /**
     * Does it have exception as failure cause?
     * @return The determination, true or false.
     */
    public boolean hasFailureCause() {
        return failureCause != null;
    }

    /**
     * Does it have any validation errors?
     * @return The determination, true or false.
     */
    public boolean hasValidationError() {
        return validationErrors != null && !validationErrors.isEmpty();
    }

    // ===================================================================================
    //                                                                        Display Data
    //                                                                        ============
    public void registerData(String key, Object value) {
        if (displayDataMap == null) {
            displayDataMap = new LinkedHashMap<String, Object>(4);
        }
        displayDataMap.put(key, filterDisplayDataValue(value));
    }

    protected Object filterDisplayDataValue(Object value) {
        return LaParamWrapperUtil.convert(value);
    }

    public void clearDisplayData() { // called by system exception dispatch for API, just in case leak
        displayDataMap.clear();
        displayDataMap = null;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return "{" + buildToStringContents() + "}";
    }

    protected String buildToStringContents() {
        final Method method = getExecuteMethod();
        final String invoke = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        final String path = actionResponse != null ? actionResponse.toString() : null;
        final String failure = failureCause != null ? DfTypeUtil.toClassTitle(failureCause) : null;
        return invoke + ":" + path + ":" + failure;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    /**
     * Get the action response returned by action execute.
     * @return The action response returned by action execute. (NullAllowed: not null only when success)
     */
    public ActionResponse getActionResponse() {
        return actionResponse;
    }

    public void setActionResponse(ActionResponse actionResponse) {
        this.actionResponse = actionResponse;
    }

    /**
     * Get the exception as failure cause thrown by action execute.
     * @return The exception as failure cause. (NullAllowed: when before execute or on success)
     */
    public RuntimeException getFailureCause() {
        return failureCause;
    }

    public void setFailureCause(RuntimeException failureCause) {
        this.failureCause = failureCause;
    }

    /**
     * Get the messages as validation error.
     * @return The messages as validation error. (NullAllowed: when no validation error)
     */
    public ActionMessages getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(ActionMessages validationErrors) {
        this.validationErrors = validationErrors;
    }

    /**
     * Get the map of display data registered by e.g. HtmlResponse, ActionCallback.
     * @return The read-only map of display data. (NotNull)
     */
    public Map<String, Object> getDisplayDataMap() {
        return displayDataMap != null ? Collections.unmodifiableMap(displayDataMap) : Collections.emptyMap();
    }
}
