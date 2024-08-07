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
package org.lastaflute.db.dbflute.accesscontext;

import java.util.Stack;

import org.dbflute.hook.AccessContext;

/**
 * @author jflute
 */
public class SuspendedAccessContext {

    /** The thread-local for this. */
    private static final ThreadLocal<Stack<AccessContext>> threadLocal = new ThreadLocal<Stack<AccessContext>>();

    /**
     * Get prepared access-context on thread.
     * @return The context of DB access. (NullAllowed)
     */
    public static AccessContext getAccessContextOnThread() {
        final Stack<AccessContext> stack = threadLocal.get();
        return stack != null ? stack.peek() : null;
    }

    /**
     * Set prepared access-context on thread.
     * @param accessContext The context of DB access. (NotNull)
     */
    public static void setAccessContextOnThread(AccessContext accessContext) {
        if (accessContext == null) {
            String msg = "The argument[accessContext] must not be null.";
            throw new IllegalArgumentException(msg);
        }
        Stack<AccessContext> stack = threadLocal.get();
        if (stack == null) {
            stack = new Stack<AccessContext>();
            threadLocal.set(stack);
        }
        stack.add(accessContext);
    }

    /**
     * Is existing prepared access-context on thread?
     * @return The determination, true or false.
     */
    public static boolean isExistAccessContextOnThread() {
        final Stack<AccessContext> stack = threadLocal.get();
        return stack != null ? !stack.isEmpty() : false;
    }

    /**
     * Clear prepared access-context on thread.
     */
    public static void clearAccessContextOnThread() {
        final Stack<AccessContext> stack = threadLocal.get();
        if (stack != null) {
            stack.pop();
            if (stack.isEmpty()) {
                perfectlyClear();
            }
        }
    }

    public static void perfectlyClear() {
        threadLocal.set(null);
    }
}
