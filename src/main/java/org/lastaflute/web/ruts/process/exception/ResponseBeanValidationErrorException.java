/*
 * Copyright 2015-2024 the original author or authors.
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
package org.lastaflute.web.ruts.process.exception;

import org.lastaflute.core.exception.LaSystemException;
import org.lastaflute.core.message.UserMessages;

/**
 * @author jflute
 * @since 0.7.1 (2015/12/14 Monday)
 */
public class ResponseBeanValidationErrorException extends LaSystemException {

    private static final long serialVersionUID = 1L;

    protected final Object bean; // not null
    protected final UserMessages messages; // not null

    public ResponseBeanValidationErrorException(String msg, Object bean, UserMessages messages) {
        super(msg);
        this.bean = bean;
        this.messages = messages;
    }

    public ResponseBeanValidationErrorException(String msg, Throwable cause, Object bean, UserMessages messages) {
        super(msg, cause);
        this.bean = bean;
        this.messages = messages;
    }

    public Object getBean() {
        return bean;
    }

    public UserMessages getMessages() {
        return messages;
    }
}
