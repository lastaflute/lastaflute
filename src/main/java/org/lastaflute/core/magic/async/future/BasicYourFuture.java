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
package org.lastaflute.core.magic.async.future;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author jflute
 * @since 0.9.6 (2017/04/24 Monday at showbase)
 */
public class BasicYourFuture implements YourFuture {

    protected final Future<?> wrapped; // not null

    public BasicYourFuture(Future<?> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean isDone() {
        return wrapped.isDone();
    }

    @Override
    public void waitForDone() {
        try {
            wrapped.get();
        } catch (InterruptedException e) {
            String msg = "Interrupted the asynchronous process: " + wrapped;
            throw new YourFutureInterruptedException(msg, e);
        } catch (ExecutionException e) { // basically no way, already catched in asyncManager
            String msg = "Failed to wait for the asynchronous process done: " + wrapped;
            throw new YourFutureWaitForDoneFailureException(msg, e.getCause());
        }
    }
}
