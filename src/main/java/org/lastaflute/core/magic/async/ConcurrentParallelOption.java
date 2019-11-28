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
package org.lastaflute.core.magic.async;

import java.util.List;

import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 * @since 0.9.6 (2017/05/02 Tuesday)
 */
public class ConcurrentParallelOption {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected List<Object> parameterList; // null allowed (however basically used: mainly parameter-based parallel)
    protected boolean errorHandlingSubsumed; // as completable asynchronous process if true
    protected Long waitingIntervalMillis; // null allowed

    // ===================================================================================
    //                                                                              Facade
    //                                                                              ======
    @SuppressWarnings("unchecked")
    public ConcurrentParallelOption params(List<? extends Object> parameterList) {
        this.parameterList = (List<Object>) parameterList;
        return this;
    }

    public ConcurrentParallelOption subsumeErrorHandling() {
        this.errorHandlingSubsumed = true; // error logging enabled in asynchronous process and no thrown in caller process
        return this;
    }

    public ConcurrentParallelOption waitingIntervalMillis(long waitingIntervalMillis) {
        if (waitingIntervalMillis < 0) {
            throw new IllegalArgumentException("The argument 'waitingIntervalMillis' should not be minus.");
        }
        this.waitingIntervalMillis = waitingIntervalMillis;
        return this;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return "{parameterList=" + parameterList + ", errorLoggingSubsumed=" + errorHandlingSubsumed + ", waitingIntervalMillis="
                + waitingIntervalMillis + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public OptionalThing<List<Object>> getParameterList() {
        return OptionalThing.ofNullable(parameterList, () -> {
            throw new IllegalStateException("Not found the parameter list.");
        });
    }

    public boolean isErrorHandlingSubsumed() {
        return errorHandlingSubsumed;
    }

    public OptionalThing<Long> getWaitingIntervalMillis() {
        return OptionalThing.ofNullable(waitingIntervalMillis, () -> {
            throw new IllegalStateException("Not found the waiting-interval milliseconds.");
        });
    }
}
