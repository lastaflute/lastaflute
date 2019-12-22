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
package org.lastaflute.core.magic.async.bridge;

import java.util.concurrent.Callable;

import org.dbflute.helper.function.IndependentProcessor;
import org.lastaflute.core.magic.async.waiting.WaitingAsyncResult;

/**
 * @author jflute
 * @since 1.1.5 (2019/12/22 Sunday at bay maihama)
 */
public class AsyncStateBridge {

    protected final BridgeCallAdapter callAdapter;
    protected final Callable<WaitingAsyncResult> callableTask;
    protected final AsyncStateBridgeOption option; // for future

    public AsyncStateBridge(BridgeCallAdapter callAdapter, Callable<WaitingAsyncResult> callableTask, AsyncStateBridgeOption option) {
        this.callAdapter = callAdapter;
        this.callableTask = callableTask;
        this.option = option;
    }

    public void cross(IndependentProcessor noArgLambda) {
        callAdapter.adapt(noArgLambda);
        try {
            callableTask.call();
        } catch (Exception e) { // basically no way, may be exception from finally clause
            throw new IllegalStateException("Failed to call the task: " + callableTask, e);
        }
    }
}
