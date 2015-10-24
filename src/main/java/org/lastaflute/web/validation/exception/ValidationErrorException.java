/*
 * Copyright 2014-2015 the original author or authors.
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
package org.lastaflute.web.validation.exception;

import org.lastaflute.web.ruts.message.ActionMessages;
import org.lastaflute.web.validation.VaErrorHook;

/**
 * @author jflute
 */
public class ValidationErrorException extends RuntimeException {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = 1L;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Class<?>[] runtimeGroups; // not null
    protected final ActionMessages messages; // not null
    protected final VaErrorHook errorHandler; // not null

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ValidationErrorException(Class<?>[] runtimeGroups, ActionMessages messages, VaErrorHook errorHandler) {
        if (runtimeGroups == null) {
            throw new IllegalArgumentException("The argument 'runtimeGroups' should not be null.");
        }
        if (messages == null) {
            throw new IllegalArgumentException("The argument 'messages' should not be null.");
        }
        if (errorHandler == null) {
            throw new IllegalArgumentException("The argument 'errorHandler' should not be null.");
        }
        this.runtimeGroups = runtimeGroups;
        this.messages = messages;
        this.errorHandler = errorHandler;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return "validationError:{messages=" + messages + ", handler=" + errorHandler + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public Class<?>[] getRuntimeGroups() {
        return runtimeGroups;
    }

    public ActionMessages getMessages() {
        return messages;
    }

    public VaErrorHook getErrorHandler() {
        return errorHandler;
    }
}