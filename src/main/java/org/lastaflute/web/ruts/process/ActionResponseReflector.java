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
package org.lastaflute.web.ruts.process;

import java.lang.reflect.Parameter;
import java.util.function.Consumer;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;
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
import org.lastaflute.web.ruts.VirtualForm;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.process.ActionRuntime.DisplayDataValidator;
import org.lastaflute.web.servlet.request.RequestManager;
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
    protected final ActionExecute execute;
    protected final ActionRuntime runtime;
    protected final RequestManager requestManager;
    protected final ActionAdjustmentProvider adjustmentProvider;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionResponseReflector(ActionRuntime runtime, RequestManager requestManager, ActionAdjustmentProvider adjustmentProvider) {
        this.execute = runtime.getActionExecute();
        this.runtime = runtime;
        this.requestManager = requestManager;
        this.adjustmentProvider = adjustmentProvider;
    }

    // ===================================================================================
    //                                                                 Reflect to Response
    //                                                                 ===================
    public NextJourney reflect(ActionResponse response) {
        if (response.isUndefined()) {
            return undefinedJourney();
        }
        adjustActionResponseJustBefore(response);
        return doReflect(response); // normally here
    }

    protected void adjustActionResponseJustBefore(ActionResponse response) {
        adjustmentProvider.adjustActionResponseJustBefore(response);
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
        final ResponseManager responseManager = requestManager.getResponseManager();
        setupActionResponseHeader(responseManager, response);
        setupActionResponseHttpStatus(responseManager, response);
        if (response.isReturnAsEmptyBody()) {
            return undefinedJourney();
        }
        if (response.isReturnAsHtmlDirectly()) {
            writeHtmlDirectly(response);
            return undefinedJourney();
        }
        setupForwardRenderData(response);
        setupPushedActionForm(response);
        setupSavingErrorsToSession(response);
        return createActionNext(response);
    }

    protected void writeHtmlDirectly(HtmlResponse response) {
        requestManager.getResponseManager().write(response.getDirectHtml().get(), "text/html");
    }

    protected void setupForwardRenderData(HtmlResponse response) {
        final RenderData data = newRenderData();
        response.getRegistrationList().forEach(reg -> reg.register(data));
        validateHtmlBeanIfNeeds(response); // manage validator
        data.getDataMap().forEach((key, value) -> runtime.registerData(key, value)); // so validated here
    }

    protected RenderData newRenderData() {
        return new RenderData();
    }

    protected void setupPushedActionForm(HtmlResponse response) {
        response.getPushedFormInfo().ifPresent(formInfo -> {
            final String formKey = LastaWebKey.PUSHED_ACTION_FORM_KEY;
            VirtualForm form = createPushedActionForm(formInfo, formKey);
            requestManager.setAttribute(formKey, form);
            runtime.manageActionForm(OptionalThing.of(form)); // to export properties to request attribute
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
        return execute.prepareFormMeta(formType, listFormParameter, formSetupper).get().createActionForm();
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
    //                                             Validator
    //                                             ---------
    protected void validateHtmlBeanIfNeeds(HtmlResponse response) {
        final DisplayDataValidator validator = createDisplayDataValidator(response);
        runtime.getDisplayDataMap().forEach((key, value) -> validator.validate(key, value)); // from e.g. hookBefore()
        runtime.manageDisplayDataValidator(validator); // enable validation when regsitering data
    }

    protected DisplayDataValidator createDisplayDataValidator(HtmlResponse response) {
        return (key, value) -> { // registered data cannot be null
            if (mightBeValidable(value)) {
                validateHtmlBeanIfNeeds(value, response);
            }
        };
    }

    protected boolean mightBeValidable(Object value) { // for performance
        return !(value instanceof String // yes-yes-yes
                || value instanceof Number // e.g. Integer
                || DfTypeUtil.isAnyLocalDate(value) // e.g. LocalDate
                || value instanceof Boolean // of course
                || value.getClass().isPrimitive() // probably no way, just in case
        );
    }

    protected void validateHtmlBeanIfNeeds(Object htmlBean, HtmlResponse response) {
        if (response.isValidatorSuppressed()) { // by individual requirement
            logger.debug("...Suppressing HTML bean validator by response option: {}", response);
            return;
        }
        final ResponseReflectingOption option = adjustResponseReflecting();
        if (option.isHtmlBeanValidatorSuppressed()) { // by project policy
            return;
        }
        doValidateHtmlBean(htmlBean, response, option);
    }

    protected void doValidateHtmlBean(Object htmlBean, HtmlResponse response, ResponseReflectingOption option) {
        createHtmlBeanValidator(response, option).validate(htmlBean);
    }

    protected ResponseHtmlBeanValidator createHtmlBeanValidator(HtmlResponse response, ResponseReflectingOption option) {
        return new ResponseHtmlBeanValidator(requestManager, runtime, option.isHtmlBeanValidationErrorWarned(), response);
    }

    // ===================================================================================
    //                                                                       JSON Response
    //                                                                       =============
    protected NextJourney handleJsonResponse(JsonResponse<?> response) {
        // this needs original action customizer in your customizer.dicon
        final ResponseManager responseManager = requestManager.getResponseManager();
        setupActionResponseHeader(responseManager, response);
        setupActionResponseHttpStatus(responseManager, response);
        if (response.isReturnAsEmptyBody()) {
            return undefinedJourney();
        }
        final String json;
        if (response.isReturnAsJsonDirectly()) {
            json = response.getDirectJson().get();
        } else { // mainly here
            final Object jsonBean = response.getJsonBean();
            validateJsonBeanIfNeeds(jsonBean, response);
            json = requestManager.getJsonManager().toJson(jsonBean);
        }
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
        return undefinedJourney();
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
        final ResponseManager responseManager = requestManager.getResponseManager();
        setupActionResponseHeader(responseManager, response);
        setupActionResponseHttpStatus(responseManager, response);
        if (response.isReturnAsEmptyBody()) {
            return undefinedJourney();
        }
        responseManager.writeAsXml(response.getXmlStr(), response.getEncoding());
        return undefinedJourney();
    }

    // ===================================================================================
    //                                                                     Stream Response
    //                                                                     ===============
    protected NextJourney handleStreamResponse(StreamResponse response) {
        final ResponseManager responseManager = requestManager.getResponseManager();
        // needs to be handled in download()
        //setupActionResponseHeader(responseManager, response);
        setupActionResponseHttpStatus(responseManager, response);
        responseManager.download(response.toDownloadResource());
        return undefinedJourney();
    }

    // ===================================================================================
    //                                                                    Unknown Response
    //                                                                    ================
    protected NextJourney handleUnknownResponse(ActionResponse response) {
        String msg = "Unknown action response type: " + response.getClass() + ", " + response;
        throw new IllegalStateException(msg);
    }

    // ===================================================================================
    //                                                                   Undefined Journey
    //                                                                   =================
    protected NextJourney undefinedJourney() {
        return NextJourney.undefined();
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
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
