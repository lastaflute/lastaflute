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
package org.lastaflute.web.exception;

import org.lastaflute.core.message.UserMessages;
import org.lastaflute.core.message.exception.MessagingApplicationException;

// unused in LastaFlute now, for application framework and for future (2021/06/05)
/**
 * @author jflute
 * @since 1.2.1 (2021/06/05 Saturday at roppongi japanese)
 */
public class AccessUnderstoodButRefusedException extends MessagingApplicationException {

    private static final long serialVersionUID = 1L;

    public AccessUnderstoodButRefusedException(String debugMsg, UserMessages messages) {
        super(debugMsg, messages);
    }

    public AccessUnderstoodButRefusedException(String debugMsg, UserMessages messages, Throwable cause) {
        super(debugMsg, messages, cause);
    }
}
