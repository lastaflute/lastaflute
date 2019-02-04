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
package org.lastaflute.web.ruts.process;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dbflute.Entity;
import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.web.exception.DirectlyEntityDisplayDataNotAllowedException;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.ruts.VirtualForm;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.process.pathparam.RequestPathParam;
import org.lastaflute.web.util.LaParamWrapperUtil;

/**
 * @author jflute
 */
public class ActionRuntime {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                      Request Resource
    //                                      ----------------
    protected final String requestPath; // current request info
    protected final ActionExecute execute; // fixed meta data
    protected final RequestPathParam pathParam; // of current request

    // -----------------------------------------------------
    //                                         Runtime State
    //                                         -------------
    protected OptionalThing<VirtualForm> form;
    protected ActionResponse actionResponse;
    protected RuntimeException failureCause;
    protected UserMessages validationErrors;
    protected Map<String, Object> displayDataMap; // lazy loaded
    protected DisplayDataValidator displayDataValidator; // is set when html responce reflecting

    @FunctionalInterface
    public static interface DisplayDataValidator {

        void validate(String key, Object value);
    }

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionRuntime(String requestPath, ActionExecute execute, RequestPathParam pathParam) {
        this.requestPath = requestPath;
        this.execute = execute;
        this.pathParam = pathParam;
    }

    // ===================================================================================
    //                                                                      Basic Resource
    //                                                                      ==============
    /**
     * Get the type of requested action.
     * @return The type object of action, non enhanced. (NotNull)
     */
    public Class<?> getActionType() {
        return execute.getActionType();
    }

    /**
     * Get the method object of action execute.
     * @return The method object from execute configuration. (NotNull)
     */
    public Method getExecuteMethod() {
        return execute.getExecuteMethod();
    }

    /**
     * Is the action execute for API request? (contains e.g. JSON response return type)
     * @return The determination, true or false.
     */
    public boolean isApiExecute() {
        return execute.isApiExecute();
    }

    // ===================================================================================
    //                                                                       Action Status
    //                                                                       =============
    // -----------------------------------------------------
    //                                              Response
    //                                              --------
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
        return routingPath.endsWith(".html") // e.g. Thymeleaf
                || routingPath.endsWith(".xhtml") // just in case
                || routingPath.endsWith(".jsp") // no comment
        ;
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
     * Is the result of the action execute, JSON?
     * @return The determination, true or false.
     */
    public boolean isReturnJson() {
        return isJsonResponse();
    }

    /**
     * Is the existing response JSON?
     * @return The determination, true or false.
     */
    protected boolean isJsonResponse() {
        return actionResponse != null && actionResponse instanceof JsonResponse;
    }

    // -----------------------------------------------------
    //                                         Failure/Error
    //                                         -------------
    /**
     * Does it have exception as failure cause? (also contains validation error)
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
    /**
     * @param key The key of the data. (NotNull)
     * @param value The value of the data for the key. (NotNull)
     */
    public void registerData(String key, Object value) {
        assertArgumentNotNull("key", key);
        assertArgumentNotNull("value", value);
        if (displayDataMap == null) {
            displayDataMap = new LinkedHashMap<String, Object>(4);
        }
        stopDirectlyEntityDisplayData(key, value);
        if (displayDataValidator != null) { // since reflecting 
            displayDataValidator.validate(key, value);
        }
        displayDataMap.put(key, filterDisplayDataValue(value));
    }

    protected void stopDirectlyEntityDisplayData(String key, Object value) {
        if (value instanceof Entity) {
            throwDirectlyEntityDisplayDataNotAllowedException(key, value);
        } else if (value instanceof Collection<?>) {
            final Collection<?> coll = ((Collection<?>) value);
            if (!coll.isEmpty()) {
                // care performance for List that the most frequent pattern
                final Object first = coll instanceof List<?> ? ((List<?>) coll).get(0) : coll.iterator().next();
                if (first instanceof Entity) {
                    throwDirectlyEntityDisplayDataNotAllowedException(key, value);
                }
            }
        }
        // cannot check perfectly e.g. empty list, map's value, nested property in bean,
        // but only primary patterns are enough, strict check might be provided by UTFlute
    }

    protected void throwDirectlyEntityDisplayDataNotAllowedException(String key, Object value) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Registered the entity directly as display data.");
        br.addItem("Advice");
        br.addElement("Not allowed to register register entity directly as display data.");
        br.addElement("Convert your entity data to web bean for display data.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    Member member = ...");
        br.addElement("    return asHtml(...).renderWith(data -> {");
        br.addElement("        data.register(\"member\", member); // *Bad");
        br.addElement("    });");
        br.addElement("  (o):");
        br.addElement("    Member member = ...");
        br.addElement("    MemberBean bean = mappingToBean(member);");
        br.addElement("    return asHtml(...).renderWith(data -> {");
        br.addElement("        data.register(\"member\", bean); // Good");
        br.addElement("    });");
        br.addItem("Action");
        br.addElement(toString());
        br.addItem("Registered");
        br.addElement("key: " + key);
        if (value instanceof Collection<?>) {
            ((Collection<?>) value).forEach(element -> br.addElement(element));
        } else {
            br.addElement(value);
        }
        final String msg = br.buildExceptionMessage();
        throw new DirectlyEntityDisplayDataNotAllowedException(msg);
    }

    protected Object filterDisplayDataValue(Object value) {
        return LaParamWrapperUtil.convert(value);
    }

    public void clearDisplayData() { // called by system exception dispatch for API, just in case leak
        if (displayDataMap != null) {
            displayDataMap.clear();
            displayDataMap = null;
        }
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("runtime:{");
        sb.append(requestPath);
        sb.append(", ").append(execute.toSimpleMethodExp());
        sb.append(", ").append(pathParam); // e.g. pathParam:{{}}
        if (actionResponse != null) {
            sb.append(", ").append(actionResponse); // e.g. JsonResponse:{...}
        }
        if (failureCause != null) {
            sb.append(", *").append(DfTypeUtil.toClassTitle(failureCause)); // e.g. *SeaException
        }
        if (validationErrors != null) {
            sb.append(", errors=").append(validationErrors.toPropertySet());
        }
        if (displayDataMap != null) {
            sb.append(", display=").append(displayDataMap.keySet());
        }
        sb.append("}");
        return sb.toString();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    // -----------------------------------------------------
    //                                      Request Resource
    //                                      ----------------
    /**
     * Get the request path (without query) of the current request. e.g. /member/list/ <br>
     * Not contains context path and escaped slash remains.
     * @return The path as string. (NotNull)
     */
    public String getRequestPath() {
        return requestPath;
    }

    /**
     * Get the definition of the requested action execute.
     * @return The object that has definition info of action execute. (NotNull)
     */
    public ActionExecute getActionExecute() {
        return execute;
    }

    /**
     * Get the path parameters of the request for the action.
     * @return The object that has e.g. path parameter values. (NotNull)
     */
    public RequestPathParam getRequestPathParam() {
        return pathParam;
    }

    // -----------------------------------------------------
    //                                         Runtime State
    //                                         -------------
    /**
     * Get the action form mapped from request parameter.
     * @return The optional action form. (NotNull, EmptyAllowed: when no form or before form creation)
     */
    public OptionalThing<VirtualForm> getActionForm() {
        return form != null ? form : OptionalThing.empty();
    }

    public void manageActionForm(OptionalThing<VirtualForm> form) {
        assertArgumentNotNull("form", form);
        this.form = form;
    }

    /**
     * Get the action response returned by action execute.
     * @return The action response returned by action execute. (NullAllowed: not null only when success)
     */
    public ActionResponse getActionResponse() {
        return actionResponse;
    }

    public void manageActionResponse(ActionResponse actionResponse) {
        assertArgumentNotNull("actionResponse", actionResponse);
        this.actionResponse = actionResponse;
    }

    /**
     * Get the exception as failure cause thrown by action execute.
     * @return The exception as failure cause. (NullAllowed: when before execute or on success)
     */
    public RuntimeException getFailureCause() {
        return failureCause;
    }

    public void manageFailureCause(RuntimeException failureCause) {
        assertArgumentNotNull("failureCause", failureCause);
        this.failureCause = failureCause;
    }

    /**
     * Get the messages as validation error.
     * @return The messages as validation error. (NullAllowed: when no validation error)
     */
    public UserMessages getValidationErrors() {
        return validationErrors;
    }

    public void manageValidationErrors(UserMessages validationErrors) {
        assertArgumentNotNull("validationErrors", validationErrors);
        this.validationErrors = validationErrors;
    }

    /**
     * Get the map of display data registered by e.g. HtmlResponse, ActionCallback.
     * @return The read-only map of display data. (NotNull)
     */
    public Map<String, Object> getDisplayDataMap() {
        return displayDataMap != null ? Collections.unmodifiableMap(displayDataMap) : Collections.emptyMap();
    }

    public void manageDisplayDataValidator(DisplayDataValidator displayDataValidator) {
        assertArgumentNotNull("displayDataValidator", displayDataValidator);
        this.displayDataValidator = displayDataValidator;
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
}
