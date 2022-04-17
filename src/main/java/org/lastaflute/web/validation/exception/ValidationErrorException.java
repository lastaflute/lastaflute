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
package org.lastaflute.web.validation.exception;

import org.lastaflute.core.message.UserMessages;
import org.lastaflute.web.validation.VaErrorHook;

/**
 * The exception for validator error. (basically for action) <br>
 * 
 * <p>Application should not throw this because it needs framework process.
 * For example, an errorHook should be related to your ApiFailureHook if API action.</p> 
 * 
 * <p>Instead, you can use this.throwValidationError() (of ActionValidator) in your action.
 * Or use messages.saveSuccessAttribute() in validate() callback in some situations.</p>
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
    protected final UserMessages messages; // not null
    protected final VaErrorHook errorHook; // not null

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ValidationErrorException(Class<?>[] runtimeGroups, UserMessages messages, VaErrorHook errorHook) {
        this(runtimeGroups, messages, errorHook, null);
    }

    public ValidationErrorException(Class<?>[] runtimeGroups, UserMessages messages, VaErrorHook errorHook, Throwable cause) {
        super("Validation Error for user input: " + messages, cause);
        if (runtimeGroups == null) {
            throw new IllegalArgumentException("The argument 'runtimeGroups' should not be null.");
        }
        if (messages == null) {
            throw new IllegalArgumentException("The argument 'messages' should not be null.");
        }
        if (errorHook == null) {
            throw new IllegalArgumentException("The argument 'errorHandler' should not be null.");
        }
        this.runtimeGroups = runtimeGroups;
        this.messages = messages;
        this.errorHook = errorHook;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return "validationError:{messages=" + messages + ", errorHook=" + errorHook + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    /**
     * Get the types as runtime groups. <br>
     * Basically application does not need to use this.
     * @return The array of types as runtime groups. (NotNull)
     */
    public Class<?>[] getRuntimeGroups() {
        return runtimeGroups;
    }

    /**
     * Get the user messages as validation error. <br>
     * You may use this when unit test for validation error.
     * @return The messages object. (NotNull)
     */
    public UserMessages getMessages() {
        return messages;
    }

    /**
     * Get the error hook called by framework. <br>
     * Basically application does not need to use this.
     * @return The callback for error hook. (NotNull)
     */
    public VaErrorHook getErrorHook() {
        return errorHook;
    }
}