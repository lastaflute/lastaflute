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
package org.lastaflute.core.magic.async;

import org.lastaflute.core.magic.async.bridge.AsyncStateBridge;
import org.lastaflute.core.magic.async.bridge.AsyncStateBridgeOpCall;
import org.lastaflute.core.magic.async.exception.ConcurrentParallelRunnerException;
import org.lastaflute.core.magic.async.future.YourFuture;

/**
 * @author jflute
 */
public interface AsyncManager {

    /**
     * Execute asynchronous process by other thread. <br>
     * <pre>
     * <span style="color: #0000C0">asyncManager</span>.<span style="color: #CC4747">async</span>(() <span style="font-size: 120%">-</span>&gt;</span> {
     *     ... <span style="color: #3F7E5E">// asynchronous process here</span>
     * });
     * 
     * <span style="color: #3F7E5E">// begin asynchronous process after action transaction finished</span>
     * <span style="color: #70226C">return</span> asHtml(...).<span style="color: #994747">afterTxCommit</span>(() <span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #0000C0">asyncManager</span>.async(() <span style="font-size: 120%">-</span>&gt;</span> {
     *         ...
     *     });
     * });
     * </pre>
     * You can inherit...
     * <pre>
     * o ThreadCacheContext (copied plainly)
     * o AccessContext (copied as fixed value)
     * o CallbackContext (optional)
     * 
     * *attention: possibility of multiply threads access
     * </pre>
     * <p>Also you can change it from caller thread's one by interface default methods.</p>
     * @param noArgLambda The callback for asynchronous process. (NotNull)
     * @return The your future to handle the asynchronous process. (NotNull)
     */
    YourFuture async(ConcurrentAsyncCall noArgLambda);

    /**
     * Bridge current thread states (managed by LastaFlute) to other-managed asynchronous processes. <br>
     * The rule is same as async(), e.g. ThreadCacheContext, AccessContext, CallbackContext, ExceptionHandling. <br>
     * Also it contains exception handling (error logging) so you does not need to catch it basically.
     * <pre>
     * AsyncStateBridge <span style="color: #553000">bridge</span> = <span style="color: #0000C0">asyncManager</span>.<span style="color: #CC4747">bridgeState</span>(<span style="color: #553000">op</span> <span style="font-size: 120%">-</span>&gt;</span> {});
     * yourOtherAsync.something(() <span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #553000">bridge</span>.<span style="color: #994747">cross</span>(() <span style="font-size: 120%">-</span>&gt;</span> { <span style="color: #3F7E5E">// inherits caller thread states</span>
     *         ... <span style="color: #3F7E5E">// non-transactional process here</span>
     *         <span style="color: #0000C0">transactionStage</span>.requiresNew(tx <span style="font-size: 120%">-</span>&gt;</span> { <span style="color: #3F7E5E">// should be in cross()</span>
     *             ... <span style="color: #3F7E5E">// asynchronous process here e.g. insert(), update()</span>
     *         });
     *     });
     * );
     * </pre>
     * @param opLambda The callback for option of bridge. (NotNull)
     * @return The bridge that can migrate asynchronous state to other asynchronous processes. (NotNull)
     */
    AsyncStateBridge bridgeState(AsyncStateBridgeOpCall opLambda);

    /**
     * Execute parallel process and wait for ending of all threads.
     * <pre>
     * <span style="color: #0000C0">asyncManager</span>.<span style="color: #CC4747">parallel</span>(<span style="color: #553000">runner</span> <span style="font-size: 120%">-</span>&gt;</span> {
     *     OptionalThing&lt;Object&gt; <span style="color: #553000">parameter</span> = <span style="color: #553000">runner</span>.getParameter();
     *     ... <span style="color: #3F7E5E">// asynchronous process here</span>
     * }, <span style="color: #553000">op</span> <span style="font-size: 120%">-</span>&gt;</span> <span style="color: #553000">op</span>.params(Arrays.asList("sea", "land", "piari")));
     * </pre>
     * You can inherit...
     * <pre>
     * o ThreadCacheContext (copied plainly)
     * o AccessContext (copied as fixed value)
     * o CallbackContext (optional)
     * 
     * *attention: possibility of multiply threads access
     * </pre>
     * @param runnerLambda The callback for runner process executed as parallel. (NotNull)
     * @param opLambda The callback for option. (NotNull)
     * @throws ConcurrentParallelRunnerException When any runner does not reach goal.
     */
    void parallel(ConcurrentParallelCall runnerLambda, ConcurrentParallelOpCall opLambda);
}
