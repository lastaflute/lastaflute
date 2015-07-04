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

import org.lastaflute.core.json.JsonManager;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.callback.ActionRuntime;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.response.ApiResponse;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.response.StreamResponse;
import org.lastaflute.web.response.XmlResponse;
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

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionResponseReflector(ActionRuntime runtime, RequestManager requestManager) {
        this.execute = runtime.getActionExecute();
        this.runtime = runtime;
        this.requestManager = requestManager;
    }

    // ===================================================================================
    //                                                                 Reflect to Response
    //                                                                 ===================
    public NextJourney reflect(ActionResponse response) {
        if (response.isEmpty() || response.isSkip()) {
            return emptyJourney();
        }
        return doReflect(response);
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

    protected NextJourney handleUnknownResponse(ActionResponse response) {
        String msg = "Unknown action response type: " + response.getClass() + ", " + response;
        throw new IllegalStateException(msg);
    }

    // ===================================================================================
    //                                                                       HTML Response
    //                                                                       =============
    protected NextJourney handleHtmlResponse(HtmlResponse response) {
        setupHtmlResponseHeader(response);
        setupForwardRenderData(response);
        setupPushedActionForm(response);
        setupSavingErrorsToSession(response);
        return createActionNext(response);
    }

    protected void setupHtmlResponseHeader(HtmlResponse response) {
        response.getHeaderMap().forEach((key, values) -> {
            for (String value : values) {
                requestManager.getResponseManager().addHeader(key, value);
            }
        });
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
        response.getPushedFormType().ifPresent(formType -> {
            final String formKey = LastaWebKey.PUSHED_ACTION_FORM_KEY;
            final VirtualActionForm form = createPushedActionForm(formType, formKey);
            requestManager.setAttribute(formKey, form);
        });
    }

    protected VirtualActionForm createPushedActionForm(Class<?> formType, String formKey) {
        return execute.prepareFormMeta(formType, null).get().createActionForm();
    }

    protected NextJourney createActionNext(HtmlResponse response) {
        return execute.getActionMapping().createNextJourney(response);
    }

    protected void setupSavingErrorsToSession(HtmlResponse response) {
        if (response.isErrorsToSession()) {
            requestManager.saveErrorsToSession();
        }
    }

    // ===================================================================================
    //                                                                       JSON Response
    //                                                                       =============
    protected NextJourney handleJsonResponse(JsonResponse<?> jsonResponse) {
        // this needs original action customizer in your customizer.dicon
        final JsonManager jsonManager = requestManager.getJsonManager();
        final String json = jsonManager.toJson(jsonResponse.getJsonBean());
        final ResponseManager responseManager = requestManager.getResponseManager();
        setupApiResponseHeader(responseManager, jsonResponse);
        setupApiResponseHttpStatus(responseManager, jsonResponse);
        jsonResponse.getCallback().ifPresent(callback -> {
            final String script = callback + "(" + json + ")";
            responseManager.writeAsJavaScript(script);
        }).orElse(() -> {
            /* responseManager might have debug logging so no logging here */
            if (jsonResponse.isForcedlyJavaScript()) {
                responseManager.writeAsJavaScript(json);
            } else { /* as JSON (default) */
                responseManager.writeAsJson(json);
            }
        });
        return emptyJourney();
    }

    protected void setupApiResponseHeader(ResponseManager responseManager, ApiResponse apiResponse) {
        apiResponse.getHeaderMap().forEach((key, values) -> {
            for (String value : values) {
                responseManager.addHeader(key, value); // added as array if already exists
            }
        });
    }

    protected void setupApiResponseHttpStatus(ResponseManager responseManager, ApiResponse apiResponse) {
        final Integer httpStatus = apiResponse.getHttpStatus();
        if (httpStatus != null) {
            responseManager.setResponseStatus(httpStatus);
        }
    }

    // ===================================================================================
    //                                                                        XML Response
    //                                                                        ============
    protected NextJourney handleXmlResponse(XmlResponse xmlResponse) {
        final ResponseManager responseManager = requestManager.getResponseManager();
        setupApiResponseHeader(responseManager, xmlResponse);
        setupApiResponseHttpStatus(responseManager, xmlResponse);
        responseManager.writeAsXml(xmlResponse.getXmlStr(), xmlResponse.getEncoding());
        return emptyJourney();
    }

    // ===================================================================================
    //                                                                     Stream Response
    //                                                                     ===============
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

    // ===================================================================================
    //                                                                       Empty Journey
    //                                                                       =============
    protected NextJourney emptyJourney() {
        return NextJourney.empty();
    }
}
