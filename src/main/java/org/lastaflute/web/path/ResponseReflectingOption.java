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
package org.lastaflute.web.path;

import org.dbflute.util.DfTypeUtil;

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
    //                                                 HTML
    //                                                ------
    protected boolean htmlBeanValidationErrorWarned;
    protected boolean htmlBeanValidatorSuppressed;

    // -----------------------------------------------------
    //                                                 JSON
    //                                                ------
    protected boolean jsonBeanValidationErrorWarned;
    protected boolean jsonBeanValidatorSuppressed;
    protected boolean jsonEmptyBodyTreatedAsEmptyObject; // for e.g. client fitting

    // ===================================================================================
    //                                                                              Facade
    //                                                                              ======
    // -----------------------------------------------------
    //                                                 HTML
    //                                                ------
    public ResponseReflectingOption warnHtmlBeanValidationError() {
        htmlBeanValidationErrorWarned = true;
        return this;
    }

    public ResponseReflectingOption suppressHtmlBeanValidator() {
        htmlBeanValidatorSuppressed = true;
        return this;
    }

    // -----------------------------------------------------
    //                                                 JSON
    //                                                ------
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

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(DfTypeUtil.toClassTitle(this));
        sb.append(":{");
        sb.append("html:{");
        sb.append(htmlBeanValidationErrorWarned);
        sb.append(", ").append(htmlBeanValidatorSuppressed);
        sb.append("}");
        sb.append(", json:{");
        sb.append(jsonBeanValidationErrorWarned);
        sb.append(", ").append(jsonBeanValidatorSuppressed);
        sb.append(", ").append(jsonEmptyBodyTreatedAsEmptyObject);
        sb.append("}");
        sb.append("}");
        return sb.toString();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    // -----------------------------------------------------
    //                                                 HTML
    //                                                ------
    public boolean isHtmlBeanValidationErrorWarned() {
        return htmlBeanValidationErrorWarned;
    }

    public boolean isHtmlBeanValidatorSuppressed() {
        return htmlBeanValidatorSuppressed;
    }

    // -----------------------------------------------------
    //                                                 JSON
    //                                                ------
    public boolean isJsonBeanValidationErrorWarned() {
        return jsonBeanValidationErrorWarned;
    }

    public boolean isJsonBeanValidatorSuppressed() {
        return jsonBeanValidatorSuppressed;
    }

    public boolean isJsonEmptyBodyTreatedAsEmptyObject() {
        return jsonEmptyBodyTreatedAsEmptyObject;
    }
}
