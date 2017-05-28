/*
 * Copyright 2015-2017 the original author or authors.
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
package org.lastaflute.db.jta.stage;

import java.util.Stack;

/**
 * @author jflute
 */
public class BegunTxContext { // similar to DBFlute context implementation

    /** The thread-local for this. */
    private static final ThreadLocal<Stack<BegunTx<?>>> threadLocal = new ThreadLocal<Stack<BegunTx<?>>>();

    /**
     * Get prepared begun-tx on thread.
     * @return The context of DB access. (NullAllowed)
     */
    public static BegunTx<?> getBegunTxOnThread() {
        final Stack<BegunTx<?>> stack = threadLocal.get();
        return stack != null ? stack.peek() : null;
    }

    /**
     * Set prepared begun-tx on thread.
     * @param begunTx The context of DB access. (NotNull)
     */
    public static void setBegunTxOnThread(BegunTx<?> begunTx) {
        if (begunTx == null) {
            String msg = "The argument[begunTx] must not be null.";
            throw new IllegalArgumentException(msg);
        }
        Stack<BegunTx<?>> stack = threadLocal.get();
        if (stack == null) {
            stack = new Stack<BegunTx<?>>();
            threadLocal.set(stack);
        }
        stack.add(begunTx);
    }

    /**
     * Is existing prepared begun-tx on thread?
     * @return The determination, true or false.
     */
    public static boolean isExistBegunTxOnThread() {
        final Stack<BegunTx<?>> stack = threadLocal.get();
        return stack != null ? !stack.isEmpty() : false;
    }

    /**
     * Clear prepared begun-tx on thread.
     */
    public static void clearBegunTxOnThread() {
        final Stack<BegunTx<?>> stack = threadLocal.get();
        if (stack != null) {
            stack.pop(); // remove latest
            if (stack.isEmpty()) {
                perfectlyClear();
            }
        }
    }

    public static void perfectlyClear() {
        threadLocal.set(null);
    }
}
