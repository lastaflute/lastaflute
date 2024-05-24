/*
 * Copyright 2015-2024 the original author or authors.
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
package org.lastaflute.db.jta;

import java.util.Stack;

/**
 * The context of romantic transaction. <br>
 * This can be nested.
 * @author jflute
 */
public class TransactionRomanticContext {

    /** The thread-local for this. */
    private static final ThreadLocal<Stack<RomanticTransaction>> threadLocal = new ThreadLocal<Stack<RomanticTransaction>>();

    /**
     * Get the value of the romantic transaction.
     * @return The value of the transaction time. (NullAllowed)
     */
    public static RomanticTransaction getRomanticTransaction() {
        final Stack<RomanticTransaction> stack = threadLocal.get();
        return stack != null ? stack.peek() : null;
    }

    /**
     * Set the value of the romantic transaction.
     * @param romanticTransaction The value of the romantic transaction. (NotNull)
     */
    public static void setRomanticTransaction(RomanticTransaction romanticTransaction) {
        if (romanticTransaction == null) {
            String msg = "The argument 'romanticTransaction' should not be null.";
            throw new IllegalArgumentException(msg);
        }
        Stack<RomanticTransaction> stack = threadLocal.get();
        if (stack == null) {
            stack = new Stack<RomanticTransaction>();
            threadLocal.set(stack);
        }
        stack.push(romanticTransaction);
    }

    public static boolean exists() {
        final Stack<RomanticTransaction> stack = threadLocal.get();
        return stack != null ? !stack.isEmpty() : false;
    }

    public static void clear() {
        final Stack<RomanticTransaction> stack = threadLocal.get();
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
