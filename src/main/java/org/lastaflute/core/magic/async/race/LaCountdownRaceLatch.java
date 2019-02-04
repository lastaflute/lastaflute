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

import java.util.concurrent.CountDownLatch;

import org.dbflute.helper.thread.CountDownRaceLatch;
import org.lastaflute.core.magic.async.race.exception.LaCountdownRaceFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 * @since 0.9.6 (2017/05/02 Tuesday)
 */
public class LaCountdownRaceLatch {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger _log = LoggerFactory.getLogger(CountDownRaceLatch.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final int _runnerCount;
    protected volatile CountDownLatch _ourLatch;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LaCountdownRaceLatch(int runnerCount) {
        _runnerCount = runnerCount;
    }

    // ===================================================================================
    //                                                                     CountDown Latch
    //                                                                     ===============
    public void await() {
        final CountDownLatch latch;
        final boolean last;
        synchronized (this) {
            latch = prepareLatch();
            last = (actuallyGetCount(latch) == 1);
            if (last) {
                if (_log.isDebugEnabled()) {
                    _log.debug("...Restarting count down race");
                }
                clearLatch();
            }
            actuallyCountDown(latch); // ready go if last
        }
        if (!last) {
            if (isWaitingLatch()) {
                if (_log.isDebugEnabled()) {
                    _log.debug("...Awaiting all runners coming here");
                }
                actuallyAwait(latch);
            }
        }
    }

    protected CountDownLatch prepareLatch() {
        if (_ourLatch == null) {
            _ourLatch = new CountDownLatch(_runnerCount);
        }
        return _ourLatch;
    }

    protected void clearLatch() {
        _ourLatch = null;
    }

    protected boolean isWaitingLatch() {
        return _ourLatch != null;
    }

    protected long actuallyGetCount(CountDownLatch latch) {
        return latch.getCount();
    }

    protected void actuallyCountDown(CountDownLatch latch) {
        latch.countDown();
    }

    protected void actuallyAwait(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            String msg = "Failed to await by your latch: latch=" + latch;
            throw new LaCountdownRaceFailureException(msg, e);
        }
    }

    public synchronized void reset() {
        if (_ourLatch == null) {
            return;
        }
        final long count = _ourLatch.getCount();
        if (count > 0) {
            if (_log.isDebugEnabled()) {
                _log.debug("...Resetting your latch: count=" + count);
            }
            for (int i = 0; i < count; i++) {
                _ourLatch.countDown(); // is thread safe and allowed over count down
            }
        }
        _ourLatch = null;
    }
}
