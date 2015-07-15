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
package org.lastaflute.web.ruts.config;

/**
 * @author jflute
 */
public class ExecuteOption {

    protected final String specifiedUrlPattern;
    protected final boolean suppressTransaction;
    protected final int sqlExecutionCountLimit;

    /**
     * @param specifiedUrlPattern The URL pattern specified by action. (NullAllowed)
     * @param suppressTransaction Does it suppress transaction for action?
     * @param sqlExecutionCountLimit The integer for limit of SQL execution count in one request. (MinusAllowed: use default limit)
     */
    public ExecuteOption(String specifiedUrlPattern, boolean suppressTransaction, int sqlExecutionCountLimit) {
        this.specifiedUrlPattern = specifiedUrlPattern;
        this.suppressTransaction = suppressTransaction;
        this.sqlExecutionCountLimit = sqlExecutionCountLimit;
    }

    public String getSpecifiedUrlPattern() {
        return specifiedUrlPattern;
    }

    public boolean isSuppressTransaction() {
        return suppressTransaction;
    }

    public int getSqlExecutionCountLimit() {
        return sqlExecutionCountLimit;
    }
}
