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
package org.lastaflute.web.path;

import org.dbflute.util.DfTypeUtil;

// package is a little strange (path!? near adjustment provider...)
// but no change for compatible
/**
 * @author jflute
 * @since 0.4.6 (2015/12/14 Monday)
 */
public class ApiResponseOption {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected boolean jsonBeanValidationErrorWarned;
    protected boolean jsonBeanValidatorSuppressed;

    // ===================================================================================
    //                                                                              Facade
    //                                                                              ======
    public ApiResponseOption warnJsonBeanValidationError() {
        jsonBeanValidationErrorWarned = true;
        return this;
    }

    public ApiResponseOption suppressJsonBeanValidator() {
        jsonBeanValidatorSuppressed = true;
        return this;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final String title = DfTypeUtil.toClassTitle(this);
        return title + ":{" + jsonBeanValidationErrorWarned + ", " + jsonBeanValidatorSuppressed + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public boolean isJsonBeanValidationErrorWarned() {
        return jsonBeanValidationErrorWarned;
    }

    public boolean isJsonBeanValidatorSuppressed() {
        return jsonBeanValidatorSuppressed;
    }
}
