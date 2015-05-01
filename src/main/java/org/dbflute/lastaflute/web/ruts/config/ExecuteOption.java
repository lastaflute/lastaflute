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
package org.dbflute.lastaflute.web.ruts.config;

/**
 * @author jflute
 */
public class ExecuteOption {

    protected final String specifiedUrlPattern;
    protected final boolean suppressTransaction;

    /**
     * @param specifiedUrlPattern The URL pattern specified by action. (NullAllowed)
     * @param suppressTransaction Does it suppress transaction for action?
     */
    public ExecuteOption(String specifiedUrlPattern, boolean suppressTransaction) {
        this.specifiedUrlPattern = specifiedUrlPattern;
        this.suppressTransaction = suppressTransaction;
    }

    public String getSpecifiedUrlPattern() {
        return specifiedUrlPattern;
    }

    public boolean isSuppressTransaction() {
        return suppressTransaction;
    }
}
