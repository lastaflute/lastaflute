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
package org.lastaflute.core.magic;

import java.util.Date;
import java.util.Stack;

/**
 * The context of transaction time. <br>
 * This can be nested.
 * @author jflute
 */
public class TransactionTimeContext {

    /** The thread-local for this. */
    private static final ThreadLocal<Stack<Date>> threadLocal = new ThreadLocal<Stack<Date>>();

    /**
     * Get the value of the transaction time.
     * @return The value of the transaction time. (NullAllowed)
     */
    public static Date getTransactionTime() {
        final Stack<Date> stack = threadLocal.get();
        return stack != null ? stack.peek() : null;
    }

    /**
     * Set the value of the transaction time.
     * @param transactionTime The value of the transaction time. (NotNull)
     */
    public static void setTransactionTime(Date transactionTime) {
        if (transactionTime == null) {
            String msg = "The argument 'transactionTime' should not be null.";
            throw new IllegalArgumentException(msg);
        }
        Stack<Date> stack = threadLocal.get();
        if (stack == null) {
            stack = new Stack<Date>();
            threadLocal.set(stack);
        }
        stack.push(transactionTime);
    }

    public static boolean exists() {
        final Stack<Date> stack = threadLocal.get();
        return stack != null ? !stack.isEmpty() : false;
    }

    public static void clear() {
        final Stack<Date> stack = threadLocal.get();
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
