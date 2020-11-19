/*
 * Copyright 2015-2020 the original author or authors.
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

import java.util.Iterator;

import org.lastaflute.core.message.UserMessage;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.web.servlet.filter.RequestLoggingFilter.RequestClientErrorException;

/**
 * @author jflute
 */
public abstract class MessagingClientErrorException extends RequestClientErrorException {

    private static final long serialVersionUID = 1L;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final UserMessages userMessages; // not null, keep for anything

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public MessagingClientErrorException(String debugMsg, String title, int httpStatus, UserMessages messages) {
        super(debugMsg, title, httpStatus);
        assertMessages(messages);
        userMessages = messages;
    }

    public MessagingClientErrorException(String debugMsg, String title, int httpStatus, UserMessages messages, Throwable cause) {
        super(debugMsg, title, httpStatus, cause);
        assertMessages(messages);
        userMessages = messages;
    }

    // ===================================================================================
    //                                                                       User Messages
    //                                                                       =============
    public UserMessages getMessages() {
        return userMessages;
    }

    protected void assertMessages(UserMessages messages) {
        if (messages == null) {
            throw new IllegalArgumentException("The argument 'messages' should not be null.");
        }
        // client error does not require message
        //if (messages.isEmpty()) {
        //    throw new IllegalArgumentException("The messages should not be empty: " + messages);
        //}
        for (Iterator<UserMessage> ite = messages.silentAccessByFlatIterator(); ite.hasNext();) {
            final UserMessage message = ite.next();
            verifyResourceMessage(messages, message);
        }
    }

    protected void verifyResourceMessage(UserMessages messages, UserMessage message) {
        // client error can be non-resource message by e.g. validator
        //if (!message.isResource()) {
        //    throw new IllegalArgumentException("Not allowed the non-resource message: " + message + " in " + messages);
        //}
    }
}
