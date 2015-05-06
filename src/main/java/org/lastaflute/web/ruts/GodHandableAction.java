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
package org.lastaflute.web.ruts;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalObject;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.db.jta.stage.TransactionGenre;
import org.lastaflute.db.jta.stage.TransactionStage;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.api.ApiManager;
import org.lastaflute.web.callback.ActionCallback;
import org.lastaflute.web.callback.ActionRuntimeMeta;
import org.lastaflute.web.exception.ActionCallbackReturnNullException;
import org.lastaflute.web.exception.ExecuteMethodAccessFailureException;
import org.lastaflute.web.exception.ExecuteMethodArgumentMismatchException;
import org.lastaflute.web.exception.ExecuteMethodReturnNullException;
import org.lastaflute.web.exception.ExecuteMethodReturnTypeNotResponseException;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.response.ApiResponse;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.response.StreamResponse;
import org.lastaflute.web.response.XmlResponse;
import org.lastaflute.web.response.render.RenderData;
import org.lastaflute.web.response.render.RenderDataRegistration;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.config.ActionFormMeta;
import org.lastaflute.web.ruts.message.ActionMessages;
import org.lastaflute.web.ruts.process.ActionRequestResource;
import org.lastaflute.web.ruts.process.exception.ActionCreateFailureException;
import org.lastaflute.web.servlet.filter.RequestLoggingFilter;
import org.lastaflute.web.servlet.filter.RequestLoggingFilter.Request500Handler;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.request.ResponseManager;
import org.lastaflute.web.util.LaActionExecuteUtil;
import org.lastaflute.web.validation.ValidationErrorHandler;
import org.lastaflute.web.validation.exception.ValidationErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The wrapper of action, which can be God hand. <br>
 * This class is new-created per request.
 * @author jflute
 */
public class GodHandableAction implements VirtualAction {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger LOG = LoggerFactory.getLogger(GodHandableAction.class);
    private static final Object[] EMPTY_ARRAY = new Object[] {};

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ActionExecute execute;
    protected final ActionRequestResource resource;
    protected final RequestManager requestManager;
    protected final TransactionStage stage;
    protected final Object action;
    protected final ActionRuntimeMeta meta;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public GodHandableAction(ActionExecute execute, ActionRequestResource resource, RequestManager requestManager, TransactionStage stage) {
        this.execute = execute;
        this.resource = resource;
        this.requestManager = requestManager;
        this.stage = stage;
        try {
            this.action = createAction(execute);
        } catch (RuntimeException e) {
            throw new ActionCreateFailureException("Failed to create the action: " + execute, e);
        }
        this.meta = newActionRuntimeMeta(execute);
        saveRuntimeMetaToRequest(requestManager);
    }

    protected Object createAction(ActionExecute execute) {
        return execute.getActionMapping().createAction();
    }

    protected ActionRuntimeMeta newActionRuntimeMeta(ActionExecute execute) {
        return new ActionRuntimeMeta(execute);
    }

    protected void saveRuntimeMetaToRequest(RequestManager requestManager) { // to get it from other area
        requestManager.setAttribute(LastaWebKey.ACTION_RUNTIME_META_KEY, meta);
    }

    // ===================================================================================
    //                                                                    Callback Process
    //                                                                    ================
    @Override
    public NextJourney execute(OptionalThing<VirtualActionForm> form) {
        final ActionCallback callback = prepareActionCallback();
        final NextJourney journey = doExecute(form, callback); // #to_action
        setupDisplayData(journey);
        showTransition(journey);
        return journey;
    }

    protected ActionCallback prepareActionCallback() {
        return action instanceof ActionCallback ? (ActionCallback) action : null;
    }

    // -----------------------------------------------------
    //                                             Main Flow
    //                                             ---------
    protected NextJourney doExecute(OptionalThing<VirtualActionForm> form, ActionCallback callback) {
        prepareRequest500Handling(); // for API
        processLocale();
        try {
            final ActionResponse before = processCallbackBefore(callback);
            if (before.isPresent()) { // e.g. login required
                return handleActionResponse(before);
            }
            return transactionalExecute(form, callback); // #to_action
        } catch (RuntimeException e) {
            final ActionResponse monologue = tellExceptionMonologue(callback, e);
            return handleActionResponse(monologue);
        } finally {
            processCallbackFinally(callback);
        }
    }

    protected NextJourney transactionalExecute(OptionalThing<VirtualActionForm> form, ActionCallback callback) {
        return stage.selectable(() -> {
            final ActionResponse response = actuallyExecute(form, callback); /* #to_action */
            return handleActionResponse(response); /* also response handling in transaction */
        }, getExecuteTransactionGenre()).get(); // because of not null
    }

    protected TransactionGenre getExecuteTransactionGenre() {
        return execute.getTransactionGenre();
    }

    // -----------------------------------------------------
    //                                          500 Handling
    //                                          ------------
    protected void prepareRequest500Handling() {
        RequestLoggingFilter.setRequest500HandlerOnThread(new Request500Handler() {
            public void handle(HttpServletRequest request, HttpServletResponse response, Throwable cause) {
                dispatchApiSystemException(request, response, cause);
            }
        }); // cleared at logging filter's finally
    }

    protected void dispatchApiSystemException(HttpServletRequest request, HttpServletResponse response, Throwable cause) {
        if (meta.isApiAction() && !response.isCommitted()) {
            getApiManager().prepareSystemException(response, meta, cause).ifPresent(apiRes -> {
                handleActionResponse(apiRes);
            });
        }
    }

    // -----------------------------------------------------
    //                                                Locale
    //                                                ------
    protected void processLocale() { // moved from request processor
        // you can customize the process e.g. accept cookie locale
        requestManager.resolveUserLocale(meta);
        requestManager.resolveUserTimeZone(meta);
    }

    // ===================================================================================
    //                                                                    Process Callback
    //                                                                    ================
    // -----------------------------------------------------
    //                                                Before
    //                                                ------
    protected ActionResponse processCallbackBefore(ActionCallback callback) {
        if (callback == null) {
            return ActionResponse.empty();
        }
        showBefore(meta);
        ActionResponse response = callback.godHandActionPrologue(meta);
        if (isEmpty(response)) {
            response = callback.godHandBefore(meta);
            if (isEmpty(response)) {
                response = callback.callbackBefore(meta);
            }
        }
        if (isPresent(response)) {
            meta.setActionResponse(response);
        }
        return response;
    }

    protected void showBefore(ActionRuntimeMeta meta) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("#flow ...Calling back #before for {}", buildActionName(meta));
        }
    }

    // -----------------------------------------------------
    //                                            on Failure
    //                                            ----------
    protected ActionResponse tellExceptionMonologue(ActionCallback callback, RuntimeException e) {
        meta.setFailureCause(e);
        if (callback == null) {
            throw e;
        }
        final ActionResponse response = callback.godHandExceptionMonologue(meta);
        if (isPresent(response)) {
            meta.setActionResponse(response);
            return response;
        } else {
            throw e;
        }
    }

    // -----------------------------------------------------
    //                                               Finally
    //                                               -------
    protected void processCallbackFinally(ActionCallback callback) {
        if (callback == null) {
            return;
        }
        showFinally(meta);
        try {
            callback.callbackFinally(meta);
        } finally {
            try {
                callback.godHandFinally(meta);
            } finally {
                callback.godHandActionEpilogue(meta);
            }
        }
    }

    protected void showFinally(ActionRuntimeMeta meta) {
        if (LOG.isDebugEnabled()) {
            final String failureMark = meta.hasFailureCause() ? " with failure" : "";
            LOG.debug("#flow ...Calling back #finally{} for {}", failureMark, buildActionName(meta));
        }
    }

    // -----------------------------------------------------
    //                                          Small Helper
    //                                          ------------
    protected boolean isEmpty(ActionResponse response) {
        assertCallbackReturnNotNull(response);
        return response.isEmpty();
    }

    protected boolean isPresent(ActionResponse response) {
        assertCallbackReturnNotNull(response);
        return response.isPresent();
    }

    // ===================================================================================
    //                                                                    Actually Execute
    //                                                                    ================
    protected ActionResponse actuallyExecute(OptionalThing<VirtualActionForm> optForm, ActionCallback callback) {
        showAction(meta);
        final Object[] requestArgs = toRequestArgs(optForm);
        final Object result = invokeExecuteMethod(execute.getExecuteMethod(), requestArgs); // #to_action
        assertExecuteReturnNotNull(requestArgs, result);
        assertExecuteMethodReturnTypeActionResponse(requestArgs, result);
        final ActionResponse response = (ActionResponse) result;
        meta.setActionResponse(response); // always set here because of main
        return response;
    }

    protected Object[] toRequestArgs(OptionalThing<VirtualActionForm> optForm) {
        final List<Object> paramList = new ArrayList<Object>(4);
        execute.getUrlParamArgs().ifPresent(args -> paramList.addAll(resource.getUrlParamValueMap().values()));
        optForm.ifPresent(form -> paramList.add(form.getRealForm()));
        return !paramList.isEmpty() ? paramList.toArray(new Object[paramList.size()]) : EMPTY_ARRAY;
    }

    protected void showAction(ActionRuntimeMeta meta) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("#flow ...Beginning #action {}", buildActionDisp(meta));
        }
    }

    // -----------------------------------------------------
    //                                         Invoke Action
    //                                         -------------
    protected Object invokeExecuteMethod(Method executeMethod, Object[] requestArgs) {
        Object result = null;
        try {
            result = executeMethod.invoke(action, requestArgs); // #to_action just here
        } catch (InvocationTargetException e) { // e.g. exception in the method
            return handleExecuteMethodInvocationTargetException(executeMethod, requestArgs, e);
        } catch (IllegalAccessException e) { // e.g. private invoking
            handleExecuteMethodIllegalAccessException(executeMethod, requestArgs, e);
        } catch (IllegalArgumentException e) { // e.g. different argument number
            handleExecuteMethodIllegalArgumentException(executeMethod, requestArgs, e);
        }
        return result;
    }

    protected Object handleExecuteMethodInvocationTargetException(Method executeMethod, Object[] requestArgs, InvocationTargetException e)
            throws Error {
        final Throwable cause = e.getTargetException();
        if (cause instanceof ValidationErrorException) {
            return handleValidationErrorException((ValidationErrorException) cause);
        }
        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }
        if (cause instanceof Error) {
            throw (Error) cause;
        }
        final String msg = setupMethodExceptionMessage("Found the exception in the method invoking.", executeMethod, requestArgs);
        throw new IllegalStateException(msg, cause);
    }

    protected void handleExecuteMethodIllegalAccessException(Method executeMethod, Object[] requestArgs, IllegalAccessException cause) {
        final String msg = setupMethodExceptionMessage("Cannot access the execute method.", executeMethod, requestArgs);
        throw new ExecuteMethodAccessFailureException(msg, cause);
    }

    protected void handleExecuteMethodIllegalArgumentException(Method executeMethod, Object[] requestArgs, IllegalArgumentException cause) {
        throwExecuteMethodArgumentMismatchException(executeMethod, requestArgs, cause);
    }

    protected void throwExecuteMethodArgumentMismatchException(Method executeMethod, Object[] requestArgs, IllegalArgumentException cause) {
        final String msg = setupMethodExceptionMessage("Mismatch the argument for the execute method.", executeMethod, requestArgs);
        throw new ExecuteMethodArgumentMismatchException(msg, cause);
    }

    protected String setupMethodExceptionMessage(String notice, Method executeMethod, Object[] requestArgs) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice(notice);
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        br.addItem("Request Arguments");
        br.addElement(Arrays.asList(requestArgs));
        return br.buildExceptionMessage();
    }

    protected ActionResponse handleValidationErrorException(ValidationErrorException cause) {
        // API dispatch is not here, needs to call it in error handler by developer
        // e.g. validate(form, () -> dispatchApiValidationError())
        final ActionMessages errors = cause.getMessages();
        requestManager.errors().save(errors); // also API can use it
        final ValidationErrorHandler errorHandler = cause.getErrorHandler();
        final ActionResponse response = errorHandler.handle();
        if (response == null) {
            throw new IllegalStateException("The handler for validation error cannot return null: " + errorHandler, cause);
        }
        return response;
    }

    // ===================================================================================
    //                                                                     Handle Response
    //                                                                     ===============
    protected NextJourney handleActionResponse(ActionResponse response) {
        if (response.isEmpty() || response.isSkip()) {
            return emptyJourney();
        }
        return doHandleActionResponse(response);
    }

    protected NextJourney doHandleActionResponse(ActionResponse response) {
        if (response instanceof HtmlResponse) {
            return handleHtmlResponse((HtmlResponse) response);
        } else if (response instanceof JsonResponse) {
            return handleJsonResponse((JsonResponse<?>) response);
        } else if (response instanceof XmlResponse) {
            return handleXmlResponse((XmlResponse) response);
        } else if (response instanceof StreamResponse) {
            return handleStreamResponse((StreamResponse) response);
        } else {
            String msg = "Unknown action response type: " + response.getClass() + ", " + response;
            throw new IllegalStateException(msg);
        }
    }

    // -----------------------------------------------------
    //                                         HTML Response
    //                                         -------------
    protected NextJourney handleHtmlResponse(HtmlResponse response) {
        setupHtmlResponseHeader(response);
        setupForwardRenderData(response);
        setupPushedActionForm(response);
        setupSavingErrorsToSession(response);
        return createActionNext(response);
    }

    protected void setupHtmlResponseHeader(HtmlResponse response) {
        final Map<String, String> headerMap = response.getHeaderMap();
        if (!headerMap.isEmpty()) {
            final ResponseManager responseManager = requestManager.getResponseManager();
            for (Entry<String, String> entry : headerMap.entrySet()) {
                responseManager.addHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    protected void setupForwardRenderData(HtmlResponse htmlResponse) {
        final List<RenderDataRegistration> registrationList = htmlResponse.getRegistrationList();
        final RenderData data = newRenderData();
        for (RenderDataRegistration registration : registrationList) {
            registration.register(data);
        }
        for (Entry<String, Object> entry : data.getDataMap().entrySet()) {
            meta.registerData(entry.getKey(), entry.getValue());
        }
    }

    protected RenderData newRenderData() {
        return new RenderData();
    }

    protected void setupPushedActionForm(HtmlResponse response) {
        final Class<?> formType = response.getPushedFormType();
        if (formType != null) {
            final String formKey = LastaWebKey.PUSHED_ACTION_FORM_KEY;
            final ActionFormMeta formMeta = createPushedActionFormMeta(formType, formKey);
            requestManager.setAttribute(formKey, formMeta.createActionForm());
        }
    }

    protected ActionFormMeta createPushedActionFormMeta(Class<?> formType, String formKey) {
        // TODO jflute lastaflute: [E] fitting: cache of action form meta for pushed and also argument
        return new ActionFormMeta(formKey, formType, OptionalObject.empty());
    }

    protected NextJourney createActionNext(HtmlResponse response) {
        return execute.getActionMapping().createNextJourney(response);
    }

    protected void setupSavingErrorsToSession(HtmlResponse response) {
        if (response.isErrorsToSession()) {
            requestManager.saveErrorsToSession();
        }
    }

    // -----------------------------------------------------
    //                                         JSON Response
    //                                         -------------
    protected NextJourney handleJsonResponse(JsonResponse<?> jsonResponse) {
        // this needs original action customizer in your customizer.dicon
        final JsonManager jsonManager = getJsonManager();
        final String json = jsonManager.toJson(jsonResponse.getJsonObj());
        final ResponseManager responseManager = requestManager.getResponseManager();
        setupApiResponseHeader(responseManager, jsonResponse);
        setupApiResponseHttpStatus(responseManager, jsonResponse);
        final String callback = jsonResponse.getCallback();
        if (callback != null) { // JSONP (needs JavaScript)
            final String script = callback + "(" + json + ")";
            responseManager.writeAsJavaScript(script);
        } else {
            // responseManager might have debug logging so no logging here
            if (jsonResponse.isForcedlyJavaScript()) {
                responseManager.writeAsJavaScript(json);
            } else { // as JSON (default)
                responseManager.writeAsJson(json);
            }
        }
        return emptyJourney();
    }

    protected void setupApiResponseHeader(ResponseManager responseManager, ApiResponse apiResponse) {
        final Map<String, String> headerMap = apiResponse.getHeaderMap();
        if (!headerMap.isEmpty()) {
            final HttpServletResponse response = responseManager.getResponse();
            for (Entry<String, String> entry : headerMap.entrySet()) {
                response.addHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    protected void setupApiResponseHttpStatus(ResponseManager responseManager, ApiResponse apiResponse) {
        final Integer httpStatus = apiResponse.getHttpStatus();
        if (httpStatus != null) {
            responseManager.setResponseStatus(httpStatus);
        }
    }

    // -----------------------------------------------------
    //                                          XML Response
    //                                          ------------
    protected NextJourney handleXmlResponse(XmlResponse xmlResponse) {
        final ResponseManager responseManager = requestManager.getResponseManager();
        setupApiResponseHeader(responseManager, xmlResponse);
        setupApiResponseHttpStatus(responseManager, xmlResponse);
        responseManager.writeAsXml(xmlResponse.getXmlStr(), xmlResponse.getEncoding());
        return emptyJourney();
    }

    // -----------------------------------------------------
    //                                       Stream Response
    //                                       ---------------
    protected NextJourney handleStreamResponse(StreamResponse streamResponse) {
        final ResponseManager responseManager = requestManager.getResponseManager();
        setupStreamResponseHttpStatus(responseManager, streamResponse);
        responseManager.download(streamResponse.toDownloadResource());
        return emptyJourney();
    }

    protected void setupStreamResponseHttpStatus(ResponseManager responseManager, StreamResponse streamResponse) {
        final Integer httpStatus = streamResponse.getHttpStatus();
        if (httpStatus != null) {
            responseManager.setResponseStatus(httpStatus);
        }
    }

    // -----------------------------------------------------
    //                                         Empty Journey
    //                                         -------------
    protected NextJourney emptyJourney() {
        return NextJourney.empty();
    }

    // ===================================================================================
    //                                                                        Display Data
    //                                                                        ============
    protected void setupDisplayData(NextJourney journey) {
        if (meta.isForwardToHtml() && journey.isPresent()) {
            meta.getDisplayDataMap().forEach((key, value) -> requestManager.setAttribute(key, value));
        }
    }

    // ===================================================================================
    //                                                                             Logging
    //                                                                             =======
    protected void showTransition(NextJourney journey) {
        if (LOG.isDebugEnabled()) {
            if (journey.isPresent()) {
                final String ing = journey.isRedirect() ? "Redirecting" : "Forwarding";
                final String path = journey.getRoutingPath(); // basically not null but just in case
                final String tag = path != null && path.endsWith(".jsp") ? "#jsp " : "";
                LOG.debug("#flow ...{} to {}{}", ing, tag, path);
            }
        }
    }

    protected String buildActionDisp(ActionRuntimeMeta meta) {
        final Method method = meta.getExecuteMethod();
        final Class<?> declaringClass = method.getDeclaringClass();
        return declaringClass.getSimpleName() + "." + method.getName() + "()";
    }

    protected String buildActionName(ActionRuntimeMeta meta) {
        final Method method = meta.getExecuteMethod();
        final Class<?> declaringClass = method.getDeclaringClass();
        return declaringClass.getSimpleName();
    }

    // ===================================================================================
    //                                                                           Component
    //                                                                           =========
    protected ApiManager getApiManager() {
        return getComponent(ApiManager.class);
    }

    protected JsonManager getJsonManager() {
        return getComponent(JsonManager.class);
    }

    protected <COMPONENT> COMPONENT getComponent(Class<COMPONENT> type) {
        return ContainerUtil.getComponent(type);
    }

    // ===================================================================================
    //                                                                       Assert Helper
    //                                                                       =============
    protected void assertCallbackReturnNotNull(ActionResponse response) {
        if (response == null) {
            throwActionCallbackReturnNullException();
        }
    }

    protected void throwActionCallbackReturnNullException() {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not allowed to return null from ActionCallback methods.");
        br.addItem("Advice");
        br.addElement("ActionCallback methods should return response instance.");
        br.addElement("For example, if callbackBefore():");
        br.addElement("  (x):");
        br.addElement("    public ActionResponse callbackBefore(...) {");
        br.addElement("        return null; // *NG");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public ActionResponse callbackBefore(...) {");
        br.addElement("        return ActionResponse.empty(); // OK");
        br.addElement("    }");
        br.addElement("    public ActionResponse callbackBefore(...) {");
        br.addElement("        return asHtml(...); // OK");
        br.addElement("    }");
        br.addItem("Action Execute");
        br.addElement(execute);
        br.addItem("Action Object");
        br.addElement(action);
        final String msg = br.buildExceptionMessage();
        throw new ActionCallbackReturnNullException(msg);
    }

    protected void assertExecuteReturnNotNull(Object[] requestArgs, Object result) {
        if (result == null) {
            throwExecuteMethodReturnNullException(requestArgs);
        }
    }

    protected void throwExecuteMethodReturnNullException(Object[] requestArgs) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not allowed to return null from the execute method.");
        br.addItem("Advice");
        br.addElement("Execute method should return response instance.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse index(...) {");
        br.addElement("        return null; // *NG");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public HtmlResponse index(...) {");
        br.addElement("        return asHtml(...); // OK");
        br.addElement("    }");
        br.addItem("Action Execute");
        br.addElement(execute);
        br.addItem("Action Object");
        br.addElement(action);
        br.addItem("Request Arguments");
        br.addElement(Arrays.asList(requestArgs));
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodReturnNullException(msg);
    }

    protected void assertExecuteMethodReturnTypeActionResponse(Object[] requestArgs, Object result) {
        if (!(result instanceof ActionResponse)) {
            throwExecuteMethodReturnTypeNotResponseException(requestArgs, result);
        }
    }

    protected void throwExecuteMethodReturnTypeNotResponseException(Object[] requestArgs, Object result) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not action response type was returned from your action.");
        br.addItem("Action Execute");
        br.addElement(execute);
        br.addItem("Action Object");
        br.addElement(action);
        br.addItem("Request Arguments");
        br.addElement(Arrays.asList(requestArgs));
        br.addItem("Unknoww Return");
        br.addElement(result != null ? result.getClass() : null);
        br.addElement(result);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodReturnTypeNotResponseException(msg);
    }
}
