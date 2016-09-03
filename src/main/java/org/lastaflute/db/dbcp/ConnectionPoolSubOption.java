/*
 * Copyright 2015-2016 the original author or authors.
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
package org.lastaflute.db.dbcp;

/**
 * @author jflute
 * @since 0.8.4 (2016/09/03 Saturday)
 */
public class ConnectionPoolSubOption {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // embedded in Di xml as main option
    //protected Integer maxPoolSize;

    protected Integer minPoolSize;
    protected Long maxWait;
    protected Integer timeout;

    // may be almost unused (want to change name even if supported)
    //protected Boolean allowLocalTx;

    protected Boolean readOnly;

    // may be almost unused (want to use enum even if supported)
    //protected int transactionIsolationLevel;

    protected String validationQuery;
    protected Long validationInterval;

    // ===================================================================================
    //                                                                      Setting Facade
    //                                                                      ==============
    // embedded in Di xml as main option
    ///**
    // * @param maxPoolSize The maximum count of pooled connection, overriding the parameter on properties.
    // * @return this. (NotNull)
    // */
    //public ConnectionPoolOption maxPoolSize(int maxPoolSize) {
    //    this.maxPoolSize = maxPoolSize;
    //    return this;
    //}

    /**
     * @param minPoolSize The minimum count of pooled connection.
     * @return this. (NotNull)
     */
    public ConnectionPoolSubOption minPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
        return this;
    }

    /**
     * @param maxWait The milliseconds of waiting for free connection. (-1: unlimited, 0: no wait)
     * @return this. (NotNull)
     */
    public ConnectionPoolSubOption maxWait(long maxWait) {
        this.maxWait = maxWait;
        return this;
    }

    /**
     * @param timeout The timeout seconds until closing free connection.
     * @return this. (NotNull)
     */
    public ConnectionPoolSubOption timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * For read-only connection.
     * @return this. (NotNull)
     */
    public ConnectionPoolSubOption readOnly() {
        this.readOnly = true;
        return this;
    }

    /**
     * @param validationQuery SQL to check connection life when checking out (e.g. select 1 from dual).
     * @return this. (NotNull)
     */
    public ConnectionPoolSubOption validationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
        return this;
    }

    /**
     * @param validationInterval milliseconds as validation query interval.
     * @return this. (NotNull)
     */
    public ConnectionPoolSubOption validationInterval(long validationInterval) {
        this.validationInterval = validationInterval;
        return this;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    // embedded in Di xml as main option
    //public boolean hasMaxPoolSize() {
    //    return maxPoolSize != null;
    //}
    //
    //public Integer getMaxPoolSize() {
    //    return maxPoolSize;
    //}

    public boolean hasMinPoolSize() {
        return minPoolSize != null;
    }

    public Integer getMinPoolSize() {
        return minPoolSize;
    }

    public boolean hasMaxWait() {
        return maxWait != null;
    }

    public Long getMaxWait() {
        return maxWait;
    }

    public boolean hasTimeout() {
        return timeout != null;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public boolean hasReadOnly() {
        return readOnly != null;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean hasValidationQuery() {
        return validationQuery != null;
    }

    public String getValidationQuery() {
        return validationQuery;
    }

    public boolean hasValidationInterval() {
        return validationInterval != null;
    }

    public Long getValidationInterval() {
        return validationInterval;
    }
}
