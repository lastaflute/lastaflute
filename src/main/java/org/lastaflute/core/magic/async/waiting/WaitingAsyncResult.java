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
public class WaitingAsyncResult { // for one asynchronous process

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected WaitingAsyncException waitingAsyncException;

    // #for_now jflute no business result here, almost no wait (2019/11/27)
    // (so async() does not support call-able interface...needed? will think after requested)

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public OptionalThing<WaitingAsyncException> getWaitingAsyncException() { // present when thrown in asynchronous process
        return OptionalThing.ofNullable(waitingAsyncException, () -> {
            throw new IllegalStateException("Not found the thrown cause.");
        });
    }

    public void setWaitingAsyncException(WaitingAsyncException waitingAsyncException) {
        this.waitingAsyncException = waitingAsyncException;
    }
}
