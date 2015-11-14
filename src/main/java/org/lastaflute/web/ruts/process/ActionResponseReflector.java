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
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.path.ActionAdjustmentProvider;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.response.StreamResponse;
import org.lastaflute.web.response.XmlResponse;
import org.lastaflute.web.response.pushed.PushedFormInfo;
import org.lastaflute.web.response.render.RenderData;
import org.lastaflute.web.ruts.NextJourney;
import org.lastaflute.web.ruts.VirtualActionForm;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.request.ResponseManager;

/**
 * @author jflute
 */
public class ActionResponseReflector {

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
    //                                                                   Response Handling
    //                                                                   =================
    // -----------------------------------------------------
    //                                         HTML Response
    //                                         -------------
    protected NextJourney handleHtmlResponse(HtmlResponse response) {
        final ResponseManager responseManager = requestManager.getResponseManager();
        setupActionResponseHeader(responseManager, response);
        setupActionResponseHttpStatus(responseManager, response);
        if (response.isReturnAsEmptyBody()) {
            return undefinedJourney();
        }
        setupForwardRenderData(response);
        setupPushedActionForm(response);
        setupSavingErrorsToSession(response);
        return createActionNext(response);
    }

    protected void setupForwardRenderData(HtmlResponse htmlResponse) {
        final RenderData data = newRenderData();
        htmlResponse.getRegistrationList().forEach(reg -> reg.register(data));
        data.getDataMap().forEach((key, value) -> runtime.registerData(key, value));
    }

    protected RenderData newRenderData() {
        return new RenderData();
    }

    protected void setupPushedActionForm(HtmlResponse response) {
        response.getPushedFormInfo().ifPresent(formInfo -> {
            final String formKey = LastaWebKey.PUSHED_ACTION_FORM_KEY;
            VirtualActionForm form = createPushedActionForm(formInfo, formKey);
            requestManager.setAttribute(formKey, form);
            runtime.setActionForm(OptionalThing.of(form)); // to export properties to request attribute
        });
    }

    protected VirtualActionForm createPushedActionForm(PushedFormInfo formInfo, String formKey) {
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
    //                                         JSON Response
    //                                         -------------
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
            json = requestManager.getJsonManager().toJson(response.getJsonBean());
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
    //                                          XML Response
    //                                          ------------
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

    // -----------------------------------------------------
    //                                       Stream Response
    //                                       ---------------
    protected NextJourney handleStreamResponse(StreamResponse response) {
        final ResponseManager responseManager = requestManager.getResponseManager();
        // needs to be handled in download()
        //setupActionResponseHeader(responseManager, response);
        setupActionResponseHttpStatus(responseManager, response);
        responseManager.download(response.toDownloadResource());
        return undefinedJourney();
    }

    // -----------------------------------------------------
    //                                      Unknown Response
    //                                      ----------------
    protected NextJourney handleUnknownResponse(ActionResponse response) {
        String msg = "Unknown action response type: " + response.getClass() + ", " + response;
        throw new IllegalStateException(msg);
    }

    // -----------------------------------------------------
    //                                       Response Helper
    //                                       ---------------
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

    // ===================================================================================
    //                                                                   Undefined Journey
    //                                                                   =================
    protected NextJourney undefinedJourney() {
        return NextJourney.undefined();
    }
}
