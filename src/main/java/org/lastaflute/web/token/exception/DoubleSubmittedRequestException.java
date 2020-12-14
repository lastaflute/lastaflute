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
package org.lastaflute.web.token.exception;

import org.lastaflute.core.message.UserMessages;
import org.lastaflute.web.exception.MessageResponseApplicationException;

/**
 * @author jflute
 */
public class DoubleSubmittedRequestException extends MessageResponseApplicationException {

    private static final long serialVersionUID = 1L;

    public DoubleSubmittedRequestException(String msg, MessageResponseHook responseHook, UserMessages messages) {
        super(msg, responseHook, messages);
        initializeOption();
    }

    public DoubleSubmittedRequestException(String msg, MessageResponseHook responseHook, UserMessages messages, Throwable cause) {
        super(msg, responseHook, messages, cause);
        initializeOption();
    }

    protected void initializeOption() {
        // double submit is rare case because of client application's check
        // and sometimes needs to analyze duplicate data so enable info logging here 
        //  by jflute (2017/04/15)
        //withoutInfo(); // obviously double submit does not need info logging
    }
}
