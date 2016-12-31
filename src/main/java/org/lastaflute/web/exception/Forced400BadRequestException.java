/*
 * Copyright 2015-2017 the original author or authors.
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

import javax.servlet.http.HttpServletResponse;

import org.lastaflute.core.message.UserMessages;

/**
 * @author jflute
 */
public class Forced400BadRequestException extends MessagingClientErrorException {

    private static final long serialVersionUID = 1L;

    protected static final String TITLE = "400 Bad Request";
    protected static final int STATUS = HttpServletResponse.SC_BAD_REQUEST;

    public Forced400BadRequestException(String debugMsg, UserMessages messages) {
        super(debugMsg, TITLE, STATUS, messages);
    }

    public Forced400BadRequestException(String debugMsg, UserMessages messages, Throwable cause) {
        super(debugMsg, TITLE, STATUS, messages, cause);
    }
}
