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
package org.lastaflute.web.ruts.config;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.web.ruts.config.specifed.SpecifiedHttpStatus;
import org.lastaflute.web.ruts.config.specifed.SpecifiedUrlPattern;

/**
 * @author jflute
 */
public class ExecuteOption {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // basically plain value as annotation attributes
    protected final OptionalThing<SpecifiedUrlPattern> specifiedUrlPattern; // empty if e.g. empty string
    protected final boolean suppressTransaction;
    protected final boolean suppressValidatorCallCheck;
    protected final int sqlExecutionCountLimit; // minus allowed, controlled later
    protected final OptionalThing<SpecifiedHttpStatus> successHttpStatus; // empty if e.g. minus

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    /**
     * @param specifiedUrlPattern The optional URL pattern specified by action. (NullAllowed)
     * @param suppressTransaction Does it suppress transaction for action?
     * @param suppressValidatorCallCheck Does it suppress validator call check?
     * @param sqlExecutionCountLimit The integer for limit of SQL execution count in one request. (MinusAllowed: use default limit)
     * @param successHttpStatus The optional information of HTTP status for success story. (NotNull)
     */
    public ExecuteOption(OptionalThing<SpecifiedUrlPattern> specifiedUrlPattern, boolean suppressTransaction,
            boolean suppressValidatorCallCheck, int sqlExecutionCountLimit, OptionalThing<SpecifiedHttpStatus> successHttpStatus) {
        this.specifiedUrlPattern = specifiedUrlPattern;
        this.suppressTransaction = suppressTransaction;
        this.suppressValidatorCallCheck = suppressValidatorCallCheck;
        this.sqlExecutionCountLimit = sqlExecutionCountLimit;
        this.successHttpStatus = successHttpStatus;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public OptionalThing<SpecifiedUrlPattern> getSpecifiedUrlPattern() {
        return specifiedUrlPattern;
    }

    public boolean isSuppressTransaction() {
        return suppressTransaction;
    }

    public boolean isSuppressValidatorCallCheck() {
        return suppressValidatorCallCheck;
    }

    public int getSqlExecutionCountLimit() {
        return sqlExecutionCountLimit;
    }

    public OptionalThing<SpecifiedHttpStatus> getSuccessHttpStatus() {
        return successHttpStatus;
    }
}
