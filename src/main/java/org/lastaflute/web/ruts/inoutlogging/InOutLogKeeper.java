/*
 * Copyright 2015-2017 the original author or authors.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 * @since 1.0.0 (2017/08/11 Friday)
 */
public class InOutLogKeeper {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(InOutLogKeeper.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected LocalDateTime beginDateTime;
    protected Map<String, Object> requestParameterMap;
    protected String requestBody;
    protected String responseBody;

    // ===================================================================================
    //                                                                  Core Determination
    //                                                                  ==================
    public static boolean isUseInOutLogging(RequestManager requestManager) {
        return requestManager.getActionAdjustmentProvider().isUseInOutLogging();
    }

    // ===================================================================================
    //                                                                      Prepare Keeper
    //                                                                      ==============
    public static OptionalThing<InOutLogKeeper> prepare(RequestManager requestManager) {
        return isUseInOutLogging(requestManager) ? OptionalThing.of(doPrepare(requestManager)) : OptionalThing.empty();
    }

    protected static InOutLogKeeper doPrepare(RequestManager requestManager) {
        final String key = LastaWebKey.INOUT_LOGGING_KEY;
        final OptionalThing<NonShowAttribute> optAttr = requestManager.getAttribute(key, NonShowAttribute.class);
        if (optAttr.isPresent()) {
            final NonShowAttribute attr = optAttr.get();
            return (InOutLogKeeper) attr.getAttribute();
        } else {
            final InOutLogKeeper keeper = new InOutLogKeeper();
            requestManager.setAttribute(key, new NonShowAttribute(keeper));
            return keeper;
        }
    }

    // ===================================================================================
    //                                                                         Keep Facade
    //                                                                         ===========
    public void keepBeginDateTime(LocalDateTime beginDateTime) {
        this.beginDateTime = beginDateTime;
    }

    public void keepRequestParameter(Map<String, Object> allParameters) {
        try {
            for (Entry<String, Object> entry : allParameters.entrySet()) {
                addRequestParameter(entry.getKey(), entry.getValue());
            }
        } catch (RuntimeException continued) { // as not main process
            logger.debug("Failed to register request parameters as in-out log: {}", allParameters, continued);
        }
    }

    protected void addRequestParameter(String key, Object value) {
        if (requestParameterMap == null) {
            requestParameterMap = new LinkedHashMap<String, Object>();
        }
        requestParameterMap.put(key, value);
    }

    public void keepRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public void keepResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public OptionalThing<LocalDateTime> getBeginDateTime() {
        return OptionalThing.ofNullable(beginDateTime, () -> {
            throw new IllegalStateException("Not found the begin date-time.");
        });

    }

    public Map<String, Object> getRequestParameterMap() {
        return requestParameterMap != null ? Collections.unmodifiableMap(requestParameterMap) : Collections.emptyMap();
    }

    public OptionalThing<String> getRequestBody() {
        return OptionalThing.ofNullable(requestBody, () -> {
            throw new IllegalStateException("Not found the request body.");
        });
    }

    public OptionalThing<String> getResponseBody() {
        return OptionalThing.ofNullable(responseBody, () -> {
            throw new IllegalStateException("Not found the response body.");
        });
    }
}
