/*
 * Copyright 2015-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.Srl;
import org.lastaflute.core.exception.LaApplicationException;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.web.api.ApiFailureHook;
import org.lastaflute.web.api.ApiFailureResource;
import org.lastaflute.web.api.theme.FaicliUnifiedFailureResult.FaicliFailureErrorPart;
import org.lastaflute.web.api.theme.FaicliUnifiedFailureResult.FaicliUnifiedFailureType;
import org.lastaflute.web.login.exception.LoginRequiredException;
import org.lastaflute.web.response.ApiResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author jflute
 * @since 1.0.0 (2017/08/10 Thursday)
 */
public abstract class TypicalFaicliApiFailureHook implements ApiFailureHook {

    // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
    // [Reference Site]
    // http://dbflute.seasar.org/ja/lastaflute/howto/impldesign/jsonfaicli.html
    // _/_/_/_/_/_/_/_/_/_/

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final int BUSINESS_FAILURE_STATUS = HttpServletResponse.SC_BAD_REQUEST;
    protected static final String LF = "\n";

    // ===================================================================================
    //                                                                    Business Failure
    //                                                                    ================
    // -----------------------------------------------------
    //                                      Validation Error
    //                                      ----------------
    @Override
    public ApiResponse handleValidationError(ApiFailureResource resource) {
        final FaicliUnifiedFailureType failureType = FaicliUnifiedFailureType.VALIDATION_ERROR;
        final FaicliUnifiedFailureResult result = createFailureResult(failureType, resource, null);
        final int failureStatus = prepareBusinessFailureStatus(result, resource, OptionalThing.empty());
        return asJson(result).httpStatus(failureStatus);
    }

    // -----------------------------------------------------
    //                                 Application Exception
    //                                 ---------------------
    @Override
    public ApiResponse handleApplicationException(ApiFailureResource resource, RuntimeException cause) {
        final FaicliUnifiedFailureType failureType = FaicliUnifiedFailureType.BUSINESS_ERROR;
        final FaicliUnifiedFailureResult result = createFailureResult(failureType, resource, cause);
        final int failureStatus = prepareBusinessFailureStatus(result, resource, OptionalThing.of(cause));
        return asJson(result).httpStatus(failureStatus);
    }

    // -----------------------------------------------------
    //                                        Failure Status
    //                                        --------------
    protected int prepareBusinessFailureStatus(FaicliUnifiedFailureResult result, ApiFailureResource resource,
            OptionalThing<RuntimeException> optCause) {
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
        final FaicliUnifiedFailureType failureType = FaicliUnifiedFailureType.CLIENT_ERROR;
        final FaicliUnifiedFailureResult result = createFailureResult(failureType, resource, cause);
        adjustClientExceptionErrors(resource, cause, result);
        return OptionalThing.of(asJson(result)); // HTTP status will be automatically sent as client error for the cause
    }

    protected void adjustClientExceptionErrors(ApiFailureResource resource, RuntimeException cause, FaicliUnifiedFailureResult result) {
        if (isShowClientExceptionErrors(resource, cause, result)) {
            if (isShowClientExceptionMessage(resource, cause, result)) {
                result.errors.add(prepareClientExceptionThrownPart(resource, cause, result));
            }
        } else { // not show errors, basically here
            result.errors.clear(); // simple implementation for now
        }
    }

    protected boolean isShowClientExceptionErrors(ApiFailureResource resource, RuntimeException cause, FaicliUnifiedFailureResult result) { // for e.g. security
        // basically client error does not need to return user messages
        // and it should be no information for security in production of public server
        // and you can get information of client error by server log (INFO level logging)
        // so framework returns false as default
        return false; // you may set true if development.here for e.g. JSON API client developers
    }

    protected boolean isShowClientExceptionMessage(ApiFailureResource resource, RuntimeException cause, FaicliUnifiedFailureResult result) {
        return false; // should be false if production as public API
    }

    protected FaicliFailureErrorPart prepareClientExceptionThrownPart(ApiFailureResource resource, RuntimeException cause,
            FaicliUnifiedFailureResult result) {
        final Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("type", cause.getClass().getName());
        data.put("message", buildClientExceptionMessage(cause));
        return new FaicliFailureErrorPart(UserMessages.GLOBAL, "THROWN_EXCEPTION", data);
    }

    protected String buildClientExceptionMessage(RuntimeException cause) {
        final StringBuilder sb = new StringBuilder();
        buildSmartStackTrace(sb, cause, 0);
        return sb.toString();
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
    protected FaicliUnifiedFailureResult createFailureResult(FaicliUnifiedFailureType failureType, ApiFailureResource resource,
            RuntimeException cause) {
        final Map<String, List<String>> propertyMessageMap = extractPropertyMessageMap(resource, cause);
        final List<FaicliFailureErrorPart> errors = toErrors(resource, propertyMessageMap);
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
    protected List<FaicliFailureErrorPart> toErrors(ApiFailureResource resource, Map<String, List<String>> propertyMessageMap) {
        final List<FaicliFailureErrorPart> partList = propertyMessageMap.entrySet().stream().flatMap(entry -> {
            return toFailureErrorPart(resource, entry.getKey(), entry.getValue()).stream();
        }).collect(Collectors.toList());
        return new ArrayList<FaicliFailureErrorPart>(partList); // mutable for additional errors
    }

    protected List<FaicliFailureErrorPart> toFailureErrorPart(ApiFailureResource resource, String field, List<String> messageList) {
        final String delimiter = getDataDelimiter();
        return messageList.stream().map(message -> {
            if (message.contains(delimiter)) { // e.g. LENGTH | min:{min}, max:{max}
                return createJsonistaError(resource, field, message, delimiter);
            } else { // e.g. REQUIRED
                return createSimpleError(field, message); // the message can be directly 'code'
            }
        }).collect(Collectors.toList());
    }

    protected String getDataDelimiter() {
        return "|";
    }

    // -----------------------------------------------------
    //                                        Jsonista Error
    //                                        --------------
    protected FaicliFailureErrorPart createJsonistaError(ApiFailureResource resource, String field, String message, String delimiter) {
        final String code = Srl.substringFirstFront(message, delimiter).trim(); // e.g. LENGTH
        final String json = "{" + Srl.substringFirstRear(message, delimiter).trim() + "}"; // e.g. {min:{min}, max:{max}}
        final Map<String, Object> data = parseJsonistaData(resource, field, code, json);
        return newFailureErrorPart(field, code, filterDataParserHeadache(data));
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseJsonistaData(ApiFailureResource resource, String field, String code, String json) {
        try {
            final JsonManager jsonManager = resource.getRequestManager().getJsonManager();
            return jsonManager.fromJson(json, Map.class);
        } catch (RuntimeException e) {
            throwClientManagedMessageBrokenDataException(resource, field, code, json, e);
            return null; // unreachable
        }
    }

    protected void throwClientManagedMessageBrokenDataException(ApiFailureResource resource, String field, String code, String json,
            RuntimeException e) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Failed to parse client-managed message data.");
        br.addItem("Advice");
        br.addElement("Arrange your [app]_message.properties");
        br.addElement("for client-managed message way like this:");
        br.addElement("  constraints.Length.message = LENGTH | min:{min}, max:{max}");
        br.addElement("  constraints.Required.message = REQUIRED");
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
        throw new ClientManagedMessageBrokenDataException(msg, e);
    }

    public static class ClientManagedMessageBrokenDataException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public ClientManagedMessageBrokenDataException(String msg, Throwable cause) {
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

    // -----------------------------------------------------
    //                                          Simple Error
    //                                          ------------
    protected FaicliFailureErrorPart createSimpleError(String field, String code) {
        return newFailureErrorPart(field, code, Collections.emptyMap());
    }

    // -----------------------------------------------------
    //                                         JSON Response
    //                                         -------------
    protected JsonResponse<FaicliUnifiedFailureResult> asJson(FaicliUnifiedFailureResult result) {
        return new JsonResponse<FaicliUnifiedFailureResult>(result);
    }

    // ===================================================================================
    //                                                                         Result Type
    //                                                                         ===========
    protected FaicliUnifiedFailureResult newUnifiedFailureResult(FaicliUnifiedFailureType failureType,
            List<FaicliFailureErrorPart> errors) {
        return new FaicliUnifiedFailureResult(failureType, errors);
    }

    protected FaicliFailureErrorPart newFailureErrorPart(String field, String code, Map<String, Object> data) {
        return new FaicliFailureErrorPart(field, code, data);
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
}
