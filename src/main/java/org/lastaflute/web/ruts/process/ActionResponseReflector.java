/*
 * Copyright 2015-2018 the original author or authors.
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

import java.lang.reflect.Parameter;
import java.util.function.Consumer;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.json.JsonEngineResource;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.json.JsonMappingOption;
import org.lastaflute.core.json.engine.RealJsonEngine;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.path.ActionAdjustmentProvider;
import org.lastaflute.web.path.ResponseReflectingOption;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.response.StreamResponse;
import org.lastaflute.web.response.XmlResponse;
import org.lastaflute.web.response.pushed.PushedFormInfo;
import org.lastaflute.web.response.render.RenderData;
import org.lastaflute.web.ruts.NextJourney;
import org.lastaflute.web.ruts.NextJourney.PlannedJourneyProvider;
import org.lastaflute.web.ruts.VirtualForm;
import org.lastaflute.web.ruts.inoutlogging.InOutLogKeeper;
import org.lastaflute.web.ruts.process.ActionRuntime.DisplayDataValidator;
import org.lastaflute.web.ruts.process.validatebean.ResponseHtmlBeanValidator;
import org.lastaflute.web.ruts.process.validatebean.ResponseJsonBeanValidator;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.request.ResponseDownloadResource;
import org.lastaflute.web.servlet.request.ResponseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class ActionResponseReflector {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(ActionResponseReflector.class);
    private static final ResponseReflectingOption NULLOBJ_REFLECTING_OPTION = new ResponseReflectingOption(); // simple cache, private to be immutable

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ActionRuntime runtime;
    protected final RequestManager requestManager;
    protected final ActionAdjustmentProvider adjustmentProvider;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionResponseReflector(ActionRuntime runtime, RequestManager requestManager, ActionAdjustmentProvider adjustmentProvider) {
        this.runtime = runtime;
        this.requestManager = requestManager;
        this.adjustmentProvider = adjustmentProvider;
    }

    // ===================================================================================
    //                                                                 Reflect to Response
    //                                                                 ===================
    public NextJourney reflect(ActionResponse response) { // may be in transaction
        if (response.isUndefined()) {
            return undefinedJourney();
        }
        return doReflect(response); // normally here
    }

    protected NextJourney doReflect(ActionResponse response) {
        if (response instanceof HtmlResponse) {
            return handleHtmlResponse((HtmlResponse) response);
        } else if (response instanceof JsonResponse) {
            return handleJsonResponse((JsonResponse<?>) response);
        } else if (response instanceof XmlResponse) {
            return handleXmlResponse((XmlResponse) response);
        } else if (response instanceof StreamResponse) {
            return handleStreamResponse((StreamResponse) response);
        } else {
            return handleUnknownResponse(response);
        }
    }

    // ===================================================================================
    //                                                                       HTML Response
    //                                                                       =============
    protected NextJourney handleHtmlResponse(HtmlResponse response) {
        if (response.isForwardTo()) {
            gatherForwardRenderData(response); // not lazy to be in action transaction
        }
        if (response.isReturnAsEmptyBody()) {
            return createSelfContainedJourney(() -> { // to suppress render handling
                readyHtmlResponseHandling(response);
            });
        } else {
            // lazy to write response after hookFinally()
            // for example, you can add headers in hookFinally() by action response
            // (and response handling should not be in transaction because of unexpected waiting)
            return createHtmlJourney(() -> {
                readyHtmlResponseHandling(response);
                if (response.isReturnAsEmptyBody()) {
                    return;
                }
                if (response.isReturnAsHtmlDirectly()) {
                    writeHtmlDirectly(response);
                    return;
                }
                if (response.isForwardTo()) {
                    setupPushedActionForm(response);
                    setupForwardRenderData();
                }
                if (response.isErrorsToSession()) {
                    saveErrorsToSession(response);
                }
                showHtmlTransition(response);
                keepHtmlBodyForInOutLoggingIfNeeds(response);
            }, response);
        }
    }

    protected void readyHtmlResponseHandling(HtmlResponse response) {
        adjustActionResponseJustBefore(response);
        final ResponseManager responseManager = requestManager.getResponseManager();
        setupActionResponseHeader(responseManager, response);
        setupActionResponseHttpStatus(responseManager, response);
    }

    protected NextJourney createHtmlJourney(PlannedJourneyProvider journeyProvider, HtmlResponse response) {
        return runtime.getActionExecute().getActionMapping().createNextJourney(journeyProvider, response);
    }

    // -----------------------------------------------------
    //                                         HTML Directly
    //                                         -------------
    protected void writeHtmlDirectly(HtmlResponse response) {
        requestManager.getResponseManager().write(response.getDirectHtml().get(), "text/html");
    }

    // -----------------------------------------------------
    //                                     Pushed ActionForm
    //                                     -----------------
    protected void setupPushedActionForm(HtmlResponse response) {
        response.getPushedFormInfo().ifPresent(formInfo -> {
            final String formKey = LastaWebKey.PUSHED_ACTION_FORM_KEY;
            VirtualForm form = createPushedActionForm(formInfo, formKey);
            runtime.manageActionForm(OptionalThing.of(form)); // to export properties to request attribute
            requestManager.setAttribute(formKey, form);
        });
    }

    protected VirtualForm createPushedActionForm(PushedFormInfo formInfo, String formKey) {
        final OptionalThing<Class<?>> formType = OptionalThing.of(formInfo.getFormType());
        final OptionalThing<Parameter> listFormParameter = OptionalThing.empty();
        final OptionalThing<Consumer<Object>> formSetupper = formInfo.getFormOption().map(op -> {
            @SuppressWarnings("unchecked")
            final Consumer<Object> setupper = (Consumer<Object>) op.getFormSetupper();
            return setupper;
        });
        return runtime.getActionExecute().prepareFormMeta(formType, listFormParameter, formSetupper).get().createActionForm();
    }

    // -----------------------------------------------------
    //                                           Render Data
    //                                           -----------
    protected void gatherForwardRenderData(HtmlResponse response) {
        final RenderData data = newRenderData();
        response.getRegistrationList().forEach(reg -> reg.register(data));
        validateHtmlBeanIfNeeds(response); // manage validator
        data.getDataMap().forEach((key, value) -> runtime.registerData(key, value)); // so validated here
    }

    protected RenderData newRenderData() {
        return new RenderData();
    }

    protected void setupForwardRenderData() {
        runtime.getDisplayDataMap().forEach((key, value) -> requestManager.setAttribute(key, value));
    }

    // -----------------------------------------------------
    //                                     Errors to Session
    //                                     -----------------
    protected void saveErrorsToSession(HtmlResponse response) {
        requestManager.saveErrorsToSession();
    }

    // -----------------------------------------------------
    //                                       Bean Validation
    //                                       ---------------
    protected void validateHtmlBeanIfNeeds(HtmlResponse response) {
        final DisplayDataValidator validator = createDisplayDataValidator(response);
        runtime.getDisplayDataMap().forEach((key, value) -> validator.validate(key, value)); // from e.g. hookBefore()
        runtime.manageDisplayDataValidator(validator); // enable validation when regsitering data
    }

    protected DisplayDataValidator createDisplayDataValidator(HtmlResponse response) {
        if (response.isValidatorSuppressed()) { // by individual requirement
            logger.debug("...Suppressing HTML bean validator by response option: {}", response);
            return (key, value) -> {};
        }
        final ResponseReflectingOption option = adjustResponseReflecting();
        if (option.isHtmlBeanValidatorSuppressed()) { // by project policy
            return (key, value) -> {};
        }
        final ResponseHtmlBeanValidator validator = createHtmlBeanValidator(response, option);
        return (key, value) -> { // registered data cannot be null
            validator.validate(key, value); // cannot-be-validatable skip is embedded in the response validator
        };
    }

    protected ResponseHtmlBeanValidator createHtmlBeanValidator(HtmlResponse response, ResponseReflectingOption option) {
        return new ResponseHtmlBeanValidator(requestManager, runtime, option.isHtmlBeanValidationErrorWarned(), response);
    }

    // -----------------------------------------------------
    //                                       HTML Transition
    //                                       ---------------
    protected void showHtmlTransition(HtmlResponse response) {
        if (logger.isDebugEnabled()) {
            final String ing = response.isRedirectTo() ? "Redirecting" : "Forwarding";
            final String path = response.getRoutingPath(); // not null
            final String tag = path.endsWith(".html") ? "#html " : (path.endsWith(".jsp") ? "#jsp " : "");
            logger.debug("#flow ...{} to {}{}", ing, tag, path);
        }
    }

    // ===================================================================================
    //                                                                       JSON Response
    //                                                                       =============
    protected NextJourney handleJsonResponse(JsonResponse<?> response) {
        if (!response.isReturnAsEmptyBody() && !response.isReturnAsJsonDirectly()) {
            validateJsonBeanIfNeeds(response.getJsonResult(), response); // not lazy to be in action transaction
        }
        // lazy because of same reason as HTML response (see the comment)
        return createSelfContainedJourney(() -> {
            adjustActionResponseJustBefore(response);
            final ResponseManager responseManager = requestManager.getResponseManager();
            setupActionResponseHeader(responseManager, response);
            setupActionResponseHttpStatus(responseManager, response);
            final String json;
            if (response.isReturnAsEmptyBody()) { // asEmptyBody()
                if (adjustResponseReflecting().isJsonEmptyBodyTreatedAsEmptyObject()) { // for e.g. client fitting
                    json = "{}"; // is empty object
                } else { // basically here if empty body
                    return; // no write
                }
            } else { // mainly here, normal body
                if (response.isReturnAsJsonDirectly()) { // asJsonDirectly()
                    json = response.getDirectJson().get();
                } else { // mainly here
                    final JsonManager jsonManager = requestManager.getJsonManager();
                    final Object jsonResult = response.getJsonResult();
                    final OptionalThing<Consumer<JsonMappingOption>> switcher = response.getMappingOptionSwitcher();
                    if (switcher.isPresent()) { // switchMappingOption(), e.g. SwaggerAction@json()
                        json = toJsonBySwitchedMappingOption(jsonManager, jsonResult, switcher.get());
                    } else { // mainly here
                        json = jsonManager.toJson(jsonResult);
                    }
                }
            }
            keepOriginalBodyForInOutLoggingIfNeeds(json, "json");
            response.getCallback().ifPresent(callback -> {
                final String script = callback + "(" + json + ")";
                responseManager.writeAsJavaScript(script);
            }).orElse(() -> {
                /* responseManager might have debug logging so no logging here */
                if (response.isForcedlyJavaScript()) {
                    responseManager.writeAsJavaScript(json);
                } else { /* as JSON (default) */
                    responseManager.writeAsJson(json);
                }
            });
        });
    }

    protected String toJsonBySwitchedMappingOption(JsonManager jsonManager, Object jsonResult, Consumer<JsonMappingOption> switcher) {
        final JsonEngineResource resource = new JsonEngineResource();
        final JsonMappingOption option = new JsonMappingOption();
        switcher.accept(option);
        resource.acceptMappingOption(option);
        final RealJsonEngine ruledEngine = jsonManager.newRuledEngine(resource);
        return ruledEngine.toJson(jsonResult);
    }

    // -----------------------------------------------------
    //                                             Validator
    //                                             ---------
    protected void validateJsonBeanIfNeeds(Object jsonBean, JsonResponse<?> response) {
        if (response.isValidatorSuppressed()) { // by individual requirement
            logger.debug("...Suppressing JSON bean validator by response option: {}", response);
            return;
        }
        final ResponseReflectingOption option = adjustResponseReflecting();
        if (option.isJsonBeanValidatorSuppressed()) { // by project policy
            return;
        }
        // cannot-be-validatable skip is embedded in the response validator
        doValidateJsonBean(jsonBean, response, option);
    }

    protected void doValidateJsonBean(Object jsonBean, JsonResponse<?> response, ResponseReflectingOption option) {
        createJsonBeanValidator(response, option).validate(jsonBean);
    }

    protected ResponseJsonBeanValidator createJsonBeanValidator(JsonResponse<?> response, ResponseReflectingOption option) {
        return new ResponseJsonBeanValidator(requestManager, runtime, option.isJsonBeanValidationErrorWarned(), response);
    }

    // ===================================================================================
    //                                                                        XML Response
    //                                                                        ============
    protected NextJourney handleXmlResponse(XmlResponse response) {
        // lazy because of same reason as HTML response (see the comment)
        return createSelfContainedJourney(() -> {
            adjustActionResponseJustBefore(response);
            final ResponseManager responseManager = requestManager.getResponseManager();
            setupActionResponseHeader(responseManager, response);
            setupActionResponseHttpStatus(responseManager, response);
            if (response.isReturnAsEmptyBody()) {
                return;
            }
            final String xmlStr = response.getXmlStr();
            keepOriginalBodyForInOutLoggingIfNeeds(xmlStr, "xml");
            responseManager.writeAsXml(xmlStr, response.getEncoding());
        });
    }

    // ===================================================================================
    //                                                                     Stream Response
    //                                                                     ===============
    protected NextJourney handleStreamResponse(StreamResponse response) {
        // lazy because of same reason as HTML response (see the comment)
        return createSelfContainedJourney(() -> {
            adjustActionResponseJustBefore(response);
            final ResponseManager responseManager = requestManager.getResponseManager();
            // needs to be handled in download()
            //setupActionResponseHeader(responseManager, response);
            setupActionResponseHttpStatus(responseManager, response);
            final ResponseDownloadResource resource = response.toDownloadResource();
            keepStreamBodyForInOutLoggingIfNeeds(resource);
            responseManager.download(resource);
        });
    }

    // ===================================================================================
    //                                                                    Unknown Response
    //                                                                    ================
    protected NextJourney handleUnknownResponse(ActionResponse response) {
        String msg = "Unknown action response type: " + response.getClass() + ", " + response;
        throw new IllegalStateException(msg);
    }

    // ===================================================================================
    //                                                                        Next Journey
    //                                                                        ============
    protected NextJourney undefinedJourney() {
        return NextJourney.undefined();
    }

    protected NextJourney createSelfContainedJourney(PlannedJourneyProvider journeyProcessor) {
        return new NextJourney(journeyProcessor);
    }

    // ===================================================================================
    //                                                                       InOut Logging
    //                                                                       =============
    protected void keepHtmlBodyForInOutLoggingIfNeeds(HtmlResponse response) {
        InOutLogKeeper.prepare(requestManager).ifPresent(keeper -> {
            final String routingPath = response.getRoutingPath();
            final String body = "(" + (response.isRedirectTo() ? "...Redirecting to " : "...Forwarding to ") + routingPath + ")";
            keeper.keepResponseBody(body, "html");
        });
    }

    protected void keepOriginalBodyForInOutLoggingIfNeeds(String bodyContent, String bodyType) {
        InOutLogKeeper.prepare(requestManager).ifPresent(keeper -> keeper.keepResponseBody(bodyContent, bodyType));
    }

    protected void keepStreamBodyForInOutLoggingIfNeeds(ResponseDownloadResource resource) {
        InOutLogKeeper.prepare(requestManager).ifPresent(keeper -> {
            final String fileName = resource.getFileName();
            final String contentType = resource.getContentType();
            final String body = "(...Downloading fileName=" + fileName + ", contentType=" + contentType + ")";
            keeper.keepResponseBody(body, "stream");
        });
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected void adjustActionResponseJustBefore(ActionResponse response) {
        adjustmentProvider.adjustActionResponseJustBefore(runtime, response);
    }

    protected void setupActionResponseHeader(ResponseManager responseManager, ActionResponse response) {
        response.getHeaderMap().forEach((key, values) -> {
            for (String value : values) {
                responseManager.addHeader(key, value); // added as array if already exists
            }
        });
    }

    protected void setupActionResponseHttpStatus(ResponseManager responseManager, ActionResponse response) {
        response.getHttpStatus().ifPresent(status -> responseManager.setResponseStatus(status));
    }

    protected ResponseReflectingOption adjustResponseReflecting() { // not null
        final ResponseReflectingOption option = adjustmentProvider.adjustResponseReflecting();
        return option != null ? option : NULLOBJ_REFLECTING_OPTION;
    }
}
