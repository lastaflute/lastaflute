/*
 * Copyright 2015-2022 the original author or authors.
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
package org.lastaflute.web.api.theme;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.Srl;
import org.lastaflute.core.exception.LaApplicationException;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.web.api.ApiFailureHook;
import org.lastaflute.web.api.ApiFailureResource;
import org.lastaflute.web.api.BusinessFailureMapping;
import org.lastaflute.web.api.theme.FaihyUnifiedFailureResult.FaihyFailureErrorPart;
import org.lastaflute.web.api.theme.FaihyUnifiedFailureResult.FaihyUnifiedFailureType;
import org.lastaflute.web.login.exception.LoginRequiredException;
import org.lastaflute.web.response.ApiResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.servlet.request.RequestManager;

import jakarta.servlet.http.HttpServletResponse;

/**
 * @author jflute
 * @since 1.0.0 (2017/08/10 Thursday)
 */
public abstract class TypicalFaihyApiFailureHook implements ApiFailureHook {

    // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
    // [Reference Site]
    // http://dbflute.seasar.org/ja/lastaflute/howto/impldesign/jsonfaihy.html
    // _/_/_/_/_/_/_/_/_/_/

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final int BUSINESS_FAILURE_STATUS = HttpServletResponse.SC_BAD_REQUEST;
    protected static final String LF = "\n";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final BusinessFailureMapping<Integer> businessHttpStatusMapping; // for business failure

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public TypicalFaihyApiFailureHook() {
        businessHttpStatusMapping = createBusinessHttpStatusMapping();
    }

    // -----------------------------------------------------
    //                                   Failure HTTP Status
    //                                   -------------------
    protected BusinessFailureMapping<Integer> createBusinessHttpStatusMapping() {
        return new BusinessFailureMapping<Integer>(failureMap -> {
            setupBusinessHttpStatusMap(failureMap);
        });
    }

    protected void setupBusinessHttpStatusMap(Map<Class<?>, Integer> failureMap) { // you can override
        // you can add mapping of failure status with exception here
        //  e.g.
        //   failureMap.put(AccessTokenUnauthorizedException.class, HttpServletResponse.SC_UNAUTHORIZED);
        //   failureMap.put(AccessUnderstoodButRefusedException.class, HttpServletResponse.SC_FORBIDDEN);
    }

    // ===================================================================================
    //                                                                    Business Failure
    //                                                                    ================
    // -----------------------------------------------------
    //                                      Validation Error
    //                                      ----------------
    @Override
    public ApiResponse handleValidationError(ApiFailureResource resource) {
        final FaihyUnifiedFailureType failureType = FaihyUnifiedFailureType.VALIDATION_ERROR;
        final FaihyUnifiedFailureResult result = createFailureResult(failureType, resource, null);
        final int failureStatus = prepareBusinessFailureStatus(result, resource, OptionalThing.empty());
        return asJson(result).httpStatus(failureStatus);
    }

    // -----------------------------------------------------
    //                                 Application Exception
    //                                 ---------------------
    @Override
    public ApiResponse handleApplicationException(ApiFailureResource resource, RuntimeException cause) {
        final FaihyUnifiedFailureType failureType = FaihyUnifiedFailureType.BUSINESS_ERROR;
        final FaihyUnifiedFailureResult result = createFailureResult(failureType, resource, cause);
        final int failureStatus = prepareBusinessFailureStatus(result, resource, OptionalThing.of(cause));
        return asJson(result).httpStatus(failureStatus);
    }

    // -----------------------------------------------------
    //                                        Failure Status
    //                                        --------------
    protected int prepareBusinessFailureStatus(FaihyUnifiedFailureResult result, ApiFailureResource resource,
            OptionalThing<RuntimeException> optCause) {
        return optCause.flatMap(cause -> {
            return businessHttpStatusMapping.findAssignable(cause);
        }).orElseGet(() -> {
            return getDefaultBusinessFailureStatus();
        });
    }

    protected int getDefaultBusinessFailureStatus() {
        return BUSINESS_FAILURE_STATUS; // as default
    }

    // ===================================================================================
    //                                                                      System Failure
    //                                                                      ==============
    // -----------------------------------------------------
    //                                      Client Exception
    //                                      ----------------
    @Override
    public OptionalThing<ApiResponse> handleClientException(ApiFailureResource resource, RuntimeException cause) {
        final FaihyUnifiedFailureType failureType = FaihyUnifiedFailureType.CLIENT_ERROR;
        final FaihyUnifiedFailureResult result = createFailureResult(failureType, resource, cause);
        adjustClientExceptionErrors(resource, cause, result);
        return OptionalThing.of(asJson(result)); // HTTP status will be automatically sent as client error for the cause
    }

    protected void adjustClientExceptionErrors(ApiFailureResource resource, RuntimeException cause, FaihyUnifiedFailureResult result) {
        if (isShowClientExceptionErrors(resource, cause, result)) {
            if (isShowClientExceptionMessage(resource, cause, result)) {
                result.errors.add(prepareClientExceptionThrownPart(resource, cause, result));
            }
        } else { // not show errors, basically here
            result.errors.clear(); // simple implementation for now
        }
    }

    protected boolean isShowClientExceptionErrors(ApiFailureResource resource, RuntimeException cause, FaihyUnifiedFailureResult result) { // for e.g. security
        // basically client error does not need to return user messages
        // and it should be no information for security in production of public server
        // and you can get information of client error by server log (INFO level logging)
        // so framework returns false as default
        return false; // you may set true if development.here for e.g. JSON API client developers
    }

    protected boolean isShowClientExceptionMessage(ApiFailureResource resource, RuntimeException cause, FaihyUnifiedFailureResult result) {
        return false; // should be false if production as public API
    }

    protected FaihyFailureErrorPart prepareClientExceptionThrownPart(ApiFailureResource resource, RuntimeException cause,
            FaihyUnifiedFailureResult result) {
        final Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("type", cause.getClass().getName());
        data.put("message", buildClientExceptionMessage(cause));
        final String serverManaged = buildClientExceptionThrownServerManaged();
        return new FaihyFailureErrorPart(UserMessages.GLOBAL, "THROWN_EXCEPTION", data, serverManaged);
    }

    protected String buildClientExceptionMessage(RuntimeException cause) {
        final StringBuilder sb = new StringBuilder();
        buildSmartStackTrace(sb, cause, 0);
        return sb.toString();
    }

    protected String buildClientExceptionThrownServerManaged() {
        return "Make sure your request."; // used as title
    }

    // -----------------------------------------------------
    //                                      Server Exception
    //                                      ----------------
    @Override
    public OptionalThing<ApiResponse> handleServerException(ApiFailureResource resource, Throwable cause) {
        return OptionalThing.empty(); // means empty body, HTTP status will be automatically sent as server error
    }

    // ===================================================================================
    //                                                                          JSON Logic
    //                                                                          ==========
    // -----------------------------------------------------
    //                                        Failure Result
    //                                        --------------
    protected FaihyUnifiedFailureResult createFailureResult(FaihyUnifiedFailureType failureType, ApiFailureResource resource,
            RuntimeException cause) {
        final Map<String, List<String>> propertyMessageMap = extractPropertyMessageMap(resource, cause);
        final List<FaihyFailureErrorPart> errors = toErrors(resource, propertyMessageMap);
        return newUnifiedFailureResult(failureType, errors);
    }

    protected Map<String, List<String>> extractPropertyMessageMap(ApiFailureResource resource, RuntimeException cause) {
        final Map<String, List<String>> nativeMap = resource.getPropertyMessageMap();
        final Map<String, List<String>> propertyMessageMap;
        if (nativeMap.isEmpty()) {
            if (cause instanceof LoginRequiredException) {
                propertyMessageMap = recoverLoginRequired(resource, cause); // has no embedded message so recovery here
            } else if (cause instanceof LaApplicationException) {
                propertyMessageMap = recoverUnknownApplicationException(resource, cause); // basically should not be here
            } else { // e.g. client exception, server exception
                propertyMessageMap = handleEmptyPropertyMessageMap(resource, cause);
            }
        } else { // has messages
            propertyMessageMap = nativeMap;
        }
        return propertyMessageMap;
    }

    protected Map<String, List<String>> recoverLoginRequired(ApiFailureResource resource, RuntimeException cause) {
        return doRecoverMessages(resource, cause, getErrorsLoginRequired()); // should be defined in [app]_message.properties
    }

    protected abstract String getErrorsLoginRequired();

    protected Map<String, List<String>> recoverUnknownApplicationException(ApiFailureResource resource, RuntimeException cause) {
        return doRecoverMessages(resource, cause, getErrorsUnknownBusinessError()); // should be defined in [app]_message.properties
    }

    protected abstract String getErrorsUnknownBusinessError();

    protected Map<String, List<String>> doRecoverMessages(ApiFailureResource resource, RuntimeException cause, String messageKey) {
        final RequestManager requestManager = resource.getRequestManager();
        final String message = requestManager.getMessageManager().getMessage(requestManager.getUserLocale(), messageKey);
        final Map<String, List<String>> map = DfCollectionUtil.newLinkedHashMap();
        map.put(UserMessages.GLOBAL, DfCollectionUtil.newArrayList(message));
        return Collections.unmodifiableMap(map);
    }

    protected Map<String, List<String>> handleEmptyPropertyMessageMap(ApiFailureResource resource, RuntimeException cause) {
        return Collections.emptyMap(); // no message
    }

    // -----------------------------------------------------
    //                                        Failure Errors
    //                                        --------------
    protected List<FaihyFailureErrorPart> toErrors(ApiFailureResource resource, Map<String, List<String>> propertyMessageMap) {
        return propertyMessageMap.entrySet().stream().flatMap(entry -> {
            return toFailureErrorPart(resource, entry.getKey(), entry.getValue()).stream();
        }).collect(Collectors.toList());
    }

    protected List<FaihyFailureErrorPart> toFailureErrorPart(ApiFailureResource resource, String field, List<String> messageList) {
        final String hybridDelimiter = getHybridDelimiter();
        final String dataDelimiter = getDataDelimiter();
        return messageList.stream().map(message -> {
            assertHybridDelimiterExists(resource, field, hybridDelimiter, message);
            final String clientManaged = Srl.substringLastFront(message, hybridDelimiter).trim();
            final String serverManaged = Srl.substringLastRear(message, hybridDelimiter).trim();
            if (clientManaged.contains(dataDelimiter)) { // e.g. LENGTH | min:{min}, max:{max}
                return createJsonistaError(resource, field, clientManaged, dataDelimiter, serverManaged);
            } else { // e.g. REQUIRED
                return createSimpleError(field, clientManaged, serverManaged); // the clientManaged can be directly 'code'
            }
        }).collect(Collectors.toList());
    }

    protected String getHybridDelimiter() {
        return "::";
    }

    protected String getDataDelimiter() {
        return "|";
    }

    protected void assertHybridDelimiterExists(ApiFailureResource resource, String field, String hybridDelimiter, String message) {
        if (!message.contains(hybridDelimiter)) {
            throwHybridManagedMessageBrokenHybridException(resource, field, hybridDelimiter, message);
        }
    }

    protected void throwHybridManagedMessageBrokenHybridException(ApiFailureResource resource, String field, String hybridDelimiter,
            String message) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the hybrid delimiter in the message.");
        br.addItem("Advice");
        br.addElement("Arrange your [app]_message.properties");
        br.addElement("for hybrid-managed message way like this:");
        br.addElement("  constraints.Length.message = LENGTH | min:{min}, max:{max} :: length must be ...");
        br.addElement("  constraints.Required.message = REQUIRED :: is required");
        br.addElement("  ...");
        br.addItem("Target Field");
        br.addElement(field);
        br.addItem("Message List");
        br.addElement(resource.getMessageList());
        br.addItem("Target Message");
        br.addElement(message);
        br.addItem("Hybrid Delimiter");
        br.addElement(hybridDelimiter);
        final String msg = br.buildExceptionMessage();
        throw new HybridManagedMessageBrokenHybridException(msg);
    }

    public static class HybridManagedMessageBrokenHybridException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public HybridManagedMessageBrokenHybridException(String msg) {
            super(msg);
        }
    }

    // -----------------------------------------------------
    //                                        Jsonista Error
    //                                        --------------
    protected FaihyFailureErrorPart createJsonistaError(ApiFailureResource resource, String field, String clientManaged,
            String dataDelimiter, String serverManaged) {
        final String code = Srl.substringFirstFront(clientManaged, dataDelimiter).trim(); // e.g. LENGTH
        final String json = "{" + Srl.substringFirstRear(clientManaged, dataDelimiter).trim() + "}"; // e.g. {min:{min}, max:{max}}
        final Map<String, Object> jsonistaData = filterDataParserHeadache(parseJsonistaData(resource, field, code, json));
        return newFailureErrorPart(field, code, jsonistaData, filterServerManagedMessage(serverManaged, jsonistaData));
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseJsonistaData(ApiFailureResource resource, String field, String code, String json) {
        try {
            final JsonManager jsonManager = resource.getRequestManager().getJsonManager();
            return jsonManager.fromJson(json, Map.class);
        } catch (RuntimeException e) {
            throwHybridManagedMessageBrokenDataException(resource, field, code, json, e);
            return null; // unreachable
        }
    }

    protected void throwHybridManagedMessageBrokenDataException(ApiFailureResource resource, String field, String code, String json,
            RuntimeException e) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Failed to parse hybrid-managed message data.");
        br.addItem("Advice");
        br.addElement("Arrange your [app]_message.properties");
        br.addElement("for hybrid-managed message way like this:");
        br.addElement("  constraints.Length.message = LENGTH | min:{min}, max:{max} :: length must be ...");
        br.addElement("  constraints.Required.message = REQUIRED :: is required");
        br.addElement("  ...");
        br.addItem("Target Field");
        br.addElement(field);
        br.addItem("Message List");
        br.addElement(resource.getMessageList());
        br.addItem("Error Code");
        br.addElement(code);
        br.addItem("Data as JSON");
        br.addElement(json);
        final String msg = br.buildExceptionMessage();
        throw new HybridManagedMessageBrokenDataException(msg, e);
    }

    public static class HybridManagedMessageBrokenDataException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public HybridManagedMessageBrokenDataException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    protected Map<String, Object> filterDataParserHeadache(Map<String, Object> data) {
        if (data.isEmpty()) {
            return data;
        }
        final Map<String, Object> filteredMap = new LinkedHashMap<String, Object>(data.size());
        data.entrySet().stream().forEach(entry -> {
            Object value = entry.getValue();
            if (value instanceof Double) { // Gson already parses number as double in map
                final Double dble = (Double) value;
                if (Srl.rtrim(dble.toString(), "0").endsWith(".")) { // might be not decimal
                    value = dble.intValue();
                }
            }
            filteredMap.put(entry.getKey(), value);
        });
        return filteredMap;
    }

    protected String filterServerManagedMessage(String serverManaged, Map<String, Object> jsonistaData) {
        final Map<String, String> fromToMap = new HashMap<String, String>();
        jsonistaData.forEach((key, value) -> {
            fromToMap.put(buildServerManagedMessageVariable(key), value.toString()); // e.g. { "$$max$$" = "10" }
        });
        return Srl.replaceBy(serverManaged, fromToMap); // e.g. "less than or equal to $$max$$" => "less than or equal to 10"
    }

    protected String buildServerManagedMessageVariable(String key) {
        return "$$" + key + "$$";
    }

    // -----------------------------------------------------
    //                                          Simple Error
    //                                          ------------
    protected FaihyFailureErrorPart createSimpleError(String field, String code, String serverManaged) {
        return newFailureErrorPart(field, code, Collections.emptyMap(), serverManaged);
    }

    // -----------------------------------------------------
    //                                         JSON Response
    //                                         -------------
    protected JsonResponse<FaihyUnifiedFailureResult> asJson(FaihyUnifiedFailureResult result) {
        return new JsonResponse<FaihyUnifiedFailureResult>(result);
    }

    // ===================================================================================
    //                                                                         Result Type
    //                                                                         ===========
    protected FaihyUnifiedFailureResult newUnifiedFailureResult(FaihyUnifiedFailureType failureType, List<FaihyFailureErrorPart> errors) {
        return new FaihyUnifiedFailureResult(failureType, errors);
    }

    protected FaihyFailureErrorPart newFailureErrorPart(String field, String code, Map<String, Object> data, String serverManaged) {
        return new FaihyFailureErrorPart(field, code, data, serverManaged);
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected void buildSmartStackTrace(StringBuilder sb, Throwable cause, int nestLevel) { // similar to application exception's one
        sb.append(LF).append(nestLevel > 0 ? "Caused by: " : "");
        sb.append(cause.getClass().getName()).append(": ").append(cause.getMessage());
        final StackTraceElement[] stackTrace = cause.getStackTrace();
        if (stackTrace == null) { // just in case
            return;
        }
        final int limit = nestLevel == 0 ? 10 : 3;
        int index = 0;
        for (StackTraceElement element : stackTrace) {
            if (index > limit) { // not all because it's not error
                sb.append(LF).append("  ...");
                break;
            }
            final String className = element.getClassName();
            final String fileName = element.getFileName(); // might be null
            final int lineNumber = element.getLineNumber();
            final String methodName = element.getMethodName();
            sb.append(LF).append("  at ").append(className).append(".").append(methodName);
            sb.append("(").append(fileName);
            if (lineNumber >= 0) {
                sb.append(":").append(lineNumber);
            }
            sb.append(")");
            ++index;
        }
        final Throwable nested = cause.getCause();
        if (nested != null && nested != cause) {
            buildSmartStackTrace(sb, nested, nestLevel + 1);
        }
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public BusinessFailureMapping<Integer> getBusinessHttpStatusMapping() { // for e.g. swagger
        return businessHttpStatusMapping;
    }
}
