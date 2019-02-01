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
package org.lastaflute.web.exception;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.core.message.exception.MessagingApplicationException;
import org.lastaflute.web.response.ActionResponse;

/**
 * @author jflute
 */
public class MessageResponseApplicationException extends MessagingApplicationException {

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
    public MessageResponseApplicationException(String msg, MessageResponseHook responseHook, UserMessages messages) {
        super(msg, messages);
        this.responseHook = responseHook;
    }

    public MessageResponseApplicationException(String msg, MessageResponseHook responseHook, UserMessages messages, Throwable cause) {
        super(msg, messages, cause);
        this.responseHook = responseHook;
    }

    @Override
    public MessageResponseApplicationException withoutInfo() {
        return (MessageResponseApplicationException) super.withoutInfo();
    }

    // ===================================================================================
    //                                                                       Response Hook
    //                                                                       =============
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
