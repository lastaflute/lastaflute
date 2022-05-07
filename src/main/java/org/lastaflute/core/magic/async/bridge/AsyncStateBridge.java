/*
 * Copyright 2015-2022 the original author or authors.
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

    protected final BridgeCallAdapter callAdapter; // not null
    protected final Callable<WaitingAsyncResult> callableTask; // not null
    protected final AsyncStateBridgeOption option; // not null, for future

    public AsyncStateBridge(BridgeCallAdapter callAdapter, Callable<WaitingAsyncResult> callableTask, AsyncStateBridgeOption option) {
        this.callAdapter = callAdapter;
        this.callableTask = callableTask;
        this.option = option;
    }

    /**
     * Cross the bridge like this:
     * <pre>
     * AsyncStateBridge <span style="color: #553000">bridge</span> = <span style="color: #0000C0">asyncManager</span>.<span style="color: #994747">bridgeState</span>(<span style="color: #553000">op</span> <span style="font-size: 120%">-</span>&gt;</span> {});
     * yourOtherAsync.something(() <span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #553000">bridge</span>.<span style="color: #CC4747">cross</span>(() <span style="font-size: 120%">-</span>&gt;</span> { <span style="color: #3F7E5E">// inherits caller thread states</span>
     *         ... <span style="color: #3F7E5E">// non-transactional process here</span>
     *         <span style="color: #0000C0">transactionStage</span>.requiresNew(tx <span style="font-size: 120%">-</span>&gt;</span> { <span style="color: #3F7E5E">// should be in cross()</span>
     *             ... <span style="color: #3F7E5E">// asynchronous process here e.g. insert(), update()</span>
     *         });
     *     });
     * );
     * </pre>
     * <p>The rule is same as async(), e.g. ThreadCacheContext, AccessContext, CallbackContext, ExceptionHandling.
     * Also it contains exception handling (error logging) so you does not need to catch it basically.</p>
     * @param noArgLambda The callback for application logic as asynchronous process that has caller thread states. (NotNull)
     */
    public void cross(IndependentProcessor noArgLambda) {
        callAdapter.adapt(noArgLambda);
        try {
            callableTask.call();
        } catch (Exception e) { // basically no way, may be exception from finally clause
            throw new IllegalStateException("Failed to call the task: " + callableTask, e);
        }
    }
}
