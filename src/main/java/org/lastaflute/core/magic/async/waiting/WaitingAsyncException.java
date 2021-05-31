/*
 * Copyright 2015-2021 the original author or authors.
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
package org.lastaflute.core.magic.async.waiting;

import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 * @since 1.1.5 (2019/11/27 Wednesday at sky-high)
 */
public class WaitingAsyncException extends RuntimeException { // for one asynchronous process

    private static final long serialVersionUID = 1L;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // you can keep supplementary values
    protected Integer entryNumber; // null allowed, used by e.g. parallel()
    protected Object parameter; // null allowed, used by e.g. parallel()

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public WaitingAsyncException(String msg, Throwable cause) {
        super(msg, cause);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public OptionalThing<Integer> getEntryNumber() {
        return OptionalThing.ofNullable(entryNumber, () -> {
            throw new IllegalStateException("Not found the entry number.");
        });
    }

    public void setEntryNumber(Integer entryNumber) {
        this.entryNumber = entryNumber;
    }

    public OptionalThing<Object> getParameter() {
        return OptionalThing.ofNullable(parameter, () -> {
            throw new IllegalStateException("Not found the parameter.");
        });
    }

    public void setParameter(Object parameter) {
        this.parameter = parameter;
    }
}
