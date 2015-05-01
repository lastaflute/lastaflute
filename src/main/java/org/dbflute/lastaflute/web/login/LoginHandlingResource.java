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
package org.dbflute.lastaflute.web.login;

import java.lang.reflect.Method;

import org.dbflute.lastaflute.web.ruts.message.ActionMessages;

/**
 * @author jflute
 */
public class LoginHandlingResource {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Class<?> actionClass;
    protected final Method actionMethod;
    protected final RuntimeException failureCause;
    protected final ActionMessages validationErrors;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LoginHandlingResource(Class<?> actionClass, Method actionMethod, RuntimeException failureCause,
            ActionMessages validationErrors) {
        this.actionClass = actionClass;
        this.actionMethod = actionMethod;
        this.failureCause = failureCause;
        this.validationErrors = validationErrors;
    }

    // ===================================================================================
    //                                                                      Execute Status
    //                                                                      ==============
    /**
     * Does it have exception as failure cause?
     * @return The determination, true or false.
     */
    public boolean hasFailureCause() {
        return failureCause != null;
    }

    /**
     * Does it have any validation errors?
     * @return The determination, true or false.
     */
    public boolean hasValidationError() {
        return validationErrors != null && !validationErrors.isEmpty();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    /**
     * Get the type of requested action.
     * @return The type object of action. (NotNull)
     */
    public Class<?> getActionClass() {
        return actionClass;
    }

    /**
     * Get the method of requested action.
     * @return The method object of action. (NotNull)
     */
    public Method getActionMethod() {
        return actionMethod;
    }

    /**
     * Get the exception as failure cause thrown by action execute.
     * @return The exception as failure cause. (NullAllowed: when before execute or on success)
     */
    public RuntimeException getFailureCause() {
        return failureCause;
    }

    /**
     * Get the messages as validation error.
     * @return The messages as validation error. (NullAllowed: when no validation error)
     */
    public ActionMessages getValidationErrors() {
        return validationErrors;
    }
}
