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
package org.lastaflute.web.login.exception;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.exception.LaApplicationException;
import org.lastaflute.web.servlet.request.Redirectable;

/**
 * @author jflute
 */
public abstract class LoginUnauthorizedException extends LaApplicationException {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = 1L;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected Redirectable mappedRedirectable; // set by application exception handler

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LoginUnauthorizedException(String msg) {
        super(msg);
        initializeOption();
    }

    public LoginUnauthorizedException(String msg, Throwable cause) {
        super(msg, cause);
        initializeOption();
    }

    protected void initializeOption() {
        withoutInfo(); // obviously login does not need info logging
    }

    // ===================================================================================
    //                                                                 Mapped Redirectable
    //                                                                 ===================
    public OptionalThing<Redirectable> getMappedRedirectable() { // exists after application exception handling
        final Class<?> exType = getClass();
        return OptionalThing.ofNullable(mappedRedirectable, () -> {
            throw new IllegalStateException("Not found the mapped redirectable in exception: " + exType);
        });
    }

    public void mappingRedirectable(Redirectable redirectable) { // called when application exception handling
        if (redirectable == null) {
            throw new IllegalArgumentException("The argument 'redirectable' should not be null.");
        }
        this.mappedRedirectable = redirectable;
    }
}
