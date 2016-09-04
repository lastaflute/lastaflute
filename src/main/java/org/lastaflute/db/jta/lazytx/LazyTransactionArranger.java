/*
 * Copyright 2015-2016 the original author or authors.
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
package org.lastaflute.db.jta.lazytx;

import java.util.function.Consumer;

import org.lastaflute.db.dbflute.callbackcontext.lazytx.LazyTxBehaviorCommandHook;

/**
 * @author jflute
 * @since 0.8.4 (2016/09/03 Saturday)
 */
public class LazyTransactionArranger {

    /**
     * Ready lazy transaction handling. (not begin transaction yet here) <br>
     * You should call in e.g. ActionHook like this:
     * <pre>
     * public ActionResponse hookBefore(ActionRuntime runtime) {
     *     arranger.<span style="color: #CC4747">readyLazyTransaction</span>(hook <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *         CallbackContext.<span style="color: #994747">setBehaviorCommandHookOnThread</span>(hook);
     *     });
     *     return super.hookBefore(runtime);
     * }
     * 
     * public void hookFinally(ActionRuntime runtime) {
     *     super.hookFinally(runtime);
     *     arranger.<span style="color: #994747">closeLazyTransaction()</span>;
     *     CallbackContext.<span style="color: #994747">clearBehaviorCommandHookOnThread()</span>;
     * }
     * </pre>
     * @param oneArgLambda The consumer of behavior command hook for lazy transaction. (NotNull)
     */
    public void readyLazyTransaction(Consumer<LazyTxBehaviorCommandHook> oneArgLambda) {
        LazyHookedUserTransaction.readyLazyTransaction();
        oneArgLambda.accept(createLazyTxBehaviorCommandHook());
    }

    protected LazyTxBehaviorCommandHook createLazyTxBehaviorCommandHook() { // you can override
        return new LazyTxBehaviorCommandHook();
    }

    /**
     * Close lazy transaction handling. <br>
     * You should call in e.g. ActionHook like this:
     * <pre>
     * public ActionResponse hookBefore(ActionRuntime runtime) {
     *     arranger.<span style="color: #994747">readyLazyTransaction</span>(hook <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *         CallbackContext.<span style="color: #994747">setBehaviorCommandHookOnThread</span>(hook);
     *     });
     *     return super.hookBefore(runtime);
     * }
     * 
     * public void hookFinally(ActionRuntime runtime) {
     *     super.hookFinally(runtime);
     *     arranger.<span style="color: #CC4747">closeLazyTransaction()</span>;
     *     CallbackContext.<span style="color: #994747">clearBehaviorCommandHookOnThread()</span>;
     * }
     * </pre>
     */
    public void closeLazyTransaction() {
        LazyHookedUserTransaction.closeLazyTransaction();
    }
}
