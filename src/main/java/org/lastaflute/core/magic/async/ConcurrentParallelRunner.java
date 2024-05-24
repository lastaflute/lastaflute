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
package org.lastaflute.core.magic.async;

import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 * @since 0.9.6 (2017/05/02 Tuesday)
 */
public class ConcurrentParallelRunner {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final long threadId;
    protected final int entryNumber;
    protected final Object parameter; // null allowed when no parameter
    protected final Object lockObj; // not null

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ConcurrentParallelRunner(long threadId, int entryNumber, Object parameter, Object lockObj) {
        this.threadId = threadId;
        this.entryNumber = entryNumber;
        this.parameter = parameter;
        this.lockObj = lockObj;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    /**
     * Get the thread ID of the car (current thread).
     * @return The long value of thread ID.
     */
    public long getThreadId() {
        return threadId;
    }

    /**
     * Get the entry number of the runner (current thread).
     * @return The assigned number. e.g. 1, 2, 3... (NotNull)
     */
    public int getEntryNumber() {
        return entryNumber;
    }

    /**
     * Get the parameter for the runner (current thread).
     * @return The optional value as parameter. (NotNull, EmptyAllowed: if no parameters or null element)
     */
    public OptionalThing<Object> getParameter() {
        return OptionalThing.ofNullable(parameter, () -> {
            throw new IllegalStateException("Not found the parameter for the runner: entryNumber=" + entryNumber);
        });
    }

    /**
     * Get the lock object to handle threads as you like it.
     * @return The common instance for all runners. (NotNull)
     */
    public Object getLockObj() {
        return lockObj;
    }
}
