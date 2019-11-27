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

import org.lastaflute.core.magic.async.exception.ConcurrentParallelRunnerException;
import org.lastaflute.core.magic.async.future.YourFuture;

/**
 * @author jflute
 */
public interface AsyncManager {

    /**
     * Execute asynchronous process by other thread. <br>
     * <pre>
     * asyncManager.<span style="color: #CC4747">async</span>(() <span style="font-size: 120%">-</span>&gt;</span> {
     *     ... <span style="color: #3F7E5E">// asynchronous process here</span>
     * });
     * 
     * <span style="color: #3F7E5E">// begin asynchronous process after action transaction finished</span>
     * <span style="color: #70226C">return</span> asHtml(...).<span style="color: #994747">afterTxCommit</span>(() <span style="font-size: 120%">-</span>&gt;</span> {
     *     asyncManager.async(() <span style="font-size: 120%">-</span>&gt;</span> {
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
     * Execute parallel process and wait for ending of all threads.
     * <pre>
     * asyncManager.<span style="color: #CC4747">parallel</span>(<span style="color: #553000">runner</span> <span style="font-size: 120%">-</span>&gt;</span> {
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
