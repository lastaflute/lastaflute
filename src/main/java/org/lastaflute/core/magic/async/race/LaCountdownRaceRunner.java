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
package org.lastaflute.core.magic.async.race;

import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 * @since 0.9.6 (2017/05/02 Tuesday)
 */
public class LaCountdownRaceRunner {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final long _threadId;
    protected final LaCountdownRaceLatch _ourLatch;
    protected final int _entryNumber;
    protected final Object _parameter;
    protected final Object _lockObj;
    protected final int _countOfEntry; // to check

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LaCountdownRaceRunner(long threadId, LaCountdownRaceLatch ourLatch, int entryNumber, Object parameter, Object lockObj,
            int countOfEntry) {
        _threadId = threadId;
        _ourLatch = ourLatch;
        _entryNumber = entryNumber;
        _parameter = parameter;
        _lockObj = lockObj;
        _countOfEntry = countOfEntry;
    }

    // ===================================================================================
    //                                                               Basic Thread Handling 
    //                                                               =====================
    public void teaBreak(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            String msg = "Failed to have a tea break but I want to...";
            throw new IllegalStateException(msg, e);
        }
    }

    // ===================================================================================
    //                                                                     CountDown Latch
    //                                                                     ===============
    /**
     * All runners restart from here.
     * <pre>
     * new CountDownRace(3).readyGo(new CountDownRaceExecution() {
     *     public void execute(CountDownRaceRunner runner) {
     *         ...
     *         // all runners stop and wait for other cars coming here
     *         runner.restart();
     *         ...
     *     }
     * });
     * </pre>
     */
    public void restart() {
        _ourLatch.await();
    }

    // ===================================================================================
    //                                                                        Entry Number
    //                                                                        ============
    /**
     * Is this car same as the specified entry number?
     * @param entryNumber The entry number to compare.
     * @return The determination, true or false.
     */
    public boolean isEntryNumber(int entryNumber) {
        checkEntryNumber(entryNumber);
        return _entryNumber == entryNumber;
    }

    protected void checkEntryNumber(int entryNumber) {
        if (entryNumber > _countOfEntry) {
            String msg =
                    "The specified entry number is over count of entries: entryNumber=" + entryNumber + ", countOfEntry=" + _countOfEntry;
            throw new IllegalArgumentException(msg);
        }
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    /**
     * Get the thread ID of the car (current thread).
     * @return The long value of thread ID.
     */
    public long getThreadId() {
        return _threadId;
    }

    /**
     * Get the our latch to handle the threads.
     * @return The instance of our latch for count down race. (NotNull)
     */
    public LaCountdownRaceLatch getOurLatch() {
        return _ourLatch;
    }

    /**
     * Get the entry number of the runner (current thread).
     * @return The assigned number. e.g. 1, 2, 3... (NotNull)
     */
    public int getEntryNumber() {
        return _entryNumber;
    }

    /**
     * Get the parameter for the runner (current thread).
     * @return The optional value as parameter. (NotNull, EmptyAllowed)
     */
    public OptionalThing<Object> getParameter() {
        return OptionalThing.ofNullable(_parameter, () -> {
            throw new IllegalStateException("Not found the parameter for the runner: " + _entryNumber);
        });
    }

    /**
     * Get the lock object to handle threads as you like it.
     * @return The common instance for all runners. (NotNull)
     */
    public Object getLockObj() {
        return _lockObj;
    }
}
