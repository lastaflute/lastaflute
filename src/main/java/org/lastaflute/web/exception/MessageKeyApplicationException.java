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
package org.lastaflute.web.exception;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.exception.LaApplicationException;
import org.lastaflute.web.response.ActionResponse;

/**
 * @author jflute
 */
public class MessageKeyApplicationException extends LaApplicationException {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = 1L;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected MessageResponseHook responseHook;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public MessageKeyApplicationException(String msg, String messageKey, Object... args) {
        super(msg + ": " + messageKey);
        saveErrors(messageKey, args);
    }

    public MessageKeyApplicationException(String msg, Throwable cause, String messageKey, Object... args) {
        super(msg + ": " + messageKey, cause);
        saveErrors(messageKey, args);
    }

    // ===================================================================================
    //                                                                       Response Hook
    //                                                                       ====== =======
    public MessageKeyApplicationException response(MessageResponseHook responseHook) {
        this.responseHook = responseHook;
        return this;
    }

    public OptionalThing<MessageResponseHook> getResponseHook() {
        return OptionalThing.ofNullable(responseHook, () -> {
            throw new IllegalStateException("Not found the response hook.");
        });
    }

    @FunctionalInterface
    public static interface MessageResponseHook {

        /**
         * @return The action response for application exception. (NotNull)
         */
        ActionResponse hook();
    }
}
