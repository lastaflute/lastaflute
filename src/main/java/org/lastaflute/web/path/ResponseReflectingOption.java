/*
 * Copyright 2015-2024 the original author or authors.
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
package org.lastaflute.web.path;

import java.util.function.Function;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.json.engine.RealJsonEngine;
import org.lastaflute.web.ruts.process.ActionRuntime;

// package is a little strange (path!? near adjustment provider...)
// but no change for compatible
/**
 * @author jflute
 * @since 0.7.6 (2016/01/04 Monday)
 */
public class ResponseReflectingOption {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                             HTML Bean
    //                                             ---------
    protected boolean htmlBeanValidationErrorWarned;
    protected boolean htmlBeanValidatorSuppressed;

    // -----------------------------------------------------
    //                                             JSON Bean
    //                                             ---------
    protected boolean jsonBeanValidationErrorWarned;
    protected boolean jsonBeanValidatorSuppressed;
    protected boolean jsonEmptyBodyTreatedAsEmptyObject; // for e.g. client fitting

    // -----------------------------------------------------
    //                                           JSON Engine
    //                                           -----------
    protected OptionalThing<Function<ActionRuntime, RealJsonEngine>> responseJsonEngineProvider = OptionalThing.empty();

    // ===================================================================================
    //                                                                              Facade
    //                                                                              ======
    // -----------------------------------------------------
    //                                             HTML Bean
    //                                             ---------
    public ResponseReflectingOption warnHtmlBeanValidationError() {
        htmlBeanValidationErrorWarned = true;
        return this;
    }

    public ResponseReflectingOption suppressHtmlBeanValidator() {
        htmlBeanValidatorSuppressed = true;
        return this;
    }

    // -----------------------------------------------------
    //                                             JSON Bean
    //                                             ---------
    public ResponseReflectingOption warnJsonBeanValidationError() {
        jsonBeanValidationErrorWarned = true;
        return this;
    }

    public ResponseReflectingOption suppressJsonBeanValidator() {
        jsonBeanValidatorSuppressed = true;
        return this;
    }

    public ResponseReflectingOption treatJsonEmptyBodyAsEmptyObject() {
        jsonEmptyBodyTreatedAsEmptyObject = true;
        return this;
    }

    // -----------------------------------------------------
    //                                           JSON Engine
    //                                           -----------
    public ResponseReflectingOption writeJsonBy(Function<ActionRuntime, RealJsonEngine> responseJsonEngineProvider) {
        if (responseJsonEngineProvider == null) {
            throw new IllegalArgumentException("The argument 'responseJsonEngineProvider' should not be null.");
        }
        this.responseJsonEngineProvider = OptionalThing.of(responseJsonEngineProvider);
        return this;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(DfTypeUtil.toClassTitle(this));
        sb.append(":{");
        sb.append("htmlBean:{");
        sb.append(htmlBeanValidationErrorWarned);
        sb.append(", ").append(htmlBeanValidatorSuppressed);
        sb.append("}");
        sb.append(", jsonBean:{");
        sb.append(jsonBeanValidationErrorWarned);
        sb.append(", ").append(jsonBeanValidatorSuppressed);
        sb.append(", ").append(jsonEmptyBodyTreatedAsEmptyObject);
        sb.append("}");
        sb.append("jsonEngine:{");
        sb.append(responseJsonEngineProvider);
        sb.append("}");
        sb.append("}");
        return sb.toString();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    // -----------------------------------------------------
    //                                             HTML Bean
    //                                             ---------
    public boolean isHtmlBeanValidationErrorWarned() {
        return htmlBeanValidationErrorWarned;
    }

    public boolean isHtmlBeanValidatorSuppressed() {
        return htmlBeanValidatorSuppressed;
    }

    // -----------------------------------------------------
    //                                             JSON Bean
    //                                             ---------
    public boolean isJsonBeanValidationErrorWarned() {
        return jsonBeanValidationErrorWarned;
    }

    public boolean isJsonBeanValidatorSuppressed() {
        return jsonBeanValidatorSuppressed;
    }

    public boolean isJsonEmptyBodyTreatedAsEmptyObject() {
        return jsonEmptyBodyTreatedAsEmptyObject;
    }

    // -----------------------------------------------------
    //                                           JSON Engine
    //                                           -----------
    public OptionalThing<Function<ActionRuntime, RealJsonEngine>> getResponseJsonEngineProvider() {
        return responseJsonEngineProvider;
    }
}
