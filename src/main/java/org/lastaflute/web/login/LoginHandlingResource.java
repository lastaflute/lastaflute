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
package org.lastaflute.web.login;

import java.lang.reflect.Method;

import org.lastaflute.web.ruts.message.ActionMessages;
import org.lastaflute.web.ruts.process.ActionRuntime;

/**
 * @author jflute
 */
public class LoginHandlingResource {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ActionRuntime runtime;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LoginHandlingResource(ActionRuntime runtime) {
        this.runtime = runtime;
    }

    // ===================================================================================
    //                                                                      Execute Status
    //                                                                      ==============
    /**
     * Does it have exception as failure cause?
     * @return The determination, true or false.
     */
    public boolean hasFailureCause() {
        return runtime.hasFailureCause();
    }

    /**
     * Does it have any validation errors?
     * @return The determination, true or false.
     */
    public boolean hasValidationError() {
        return runtime.hasValidationError();
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return "resource:{" + runtime + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    /**
     * Get the runtime meta of action execute.
     * @return The instance of runtime meta. (NotNull)
     */
    public ActionRuntime getActionRuntime() {
        return runtime;
    }

    /**
     * Get the type of requested action.
     * @return The type object of action, not enhanced. (NotNull)
     */
    public Class<?> getActionClass() {
        return runtime.getExecuteMethod().getDeclaringClass();
    }

    /**
     * Get the method of action execute.
     * @return The method object of action. (NotNull)
     */
    public Method getExecuteMethod() {
        return runtime.getExecuteMethod();
    }

    /**
     * Is the action execute for API request? (contains e.g. JSON response return type)
     * @return The determination, true or false.
     */
    public boolean isApiExecute() {
        return runtime.isApiExecute();
    }

    /**
     * Get the exception as failure cause thrown by action execute.
     * @return The exception as failure cause. (NullAllowed: when before execute or on success)
     */
    public RuntimeException getFailureCause() {
        return runtime.getFailureCause();
    }

    /**
     * Get the messages as validation error.
     * @return The messages as validation error. (NullAllowed: when no validation error)
     */
    public ActionMessages getValidationErrors() {
        return runtime.getValidationErrors();
    }
}
