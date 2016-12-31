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
package org.lastaflute.core.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author jflute
 */
public abstract class LaApplicationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected List<LaApplicationMessage> messageList; // lazy loaded

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LaApplicationException(String debugMsg) {
        super(debugMsg);
    }

    public LaApplicationException(String debugMsg, Throwable cause) {
        super(debugMsg, cause);
    }

    // ===================================================================================
    //                                                                 Application Message
    //                                                                 ===================
    public List<LaApplicationMessage> getApplicationMessageList() {
        return messageList != null ? Collections.unmodifiableList(messageList) : Collections.emptyList();
    }

    protected void saveApplicationMessage(String property, String messageKey, Object... values) {
        assertArgumentNotNull("messageKey", messageKey);
        assertArgumentNotNull("values", values);
        messageList = new ArrayList<LaApplicationMessage>(1); // overriding
        messageList.add(newApplicationMessage(property, messageKey, values));
    }

    protected LaApplicationMessage newApplicationMessage(String property, String messageKey, Object... values) {
        return new LaApplicationMessage(property, messageKey, values);
    }

    protected void saveApplicationMessages(List<LaApplicationMessage> messageList) {
        assertArgumentNotNull("messageList", messageList);
        this.messageList = messageList; // overriding
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            String msg = "The value should not be null: variableName=null value=" + value;
            throw new IllegalArgumentException(msg);
        }
        if (value == null) {
            String msg = "The value should not be null: variableName=" + variableName;
            throw new IllegalArgumentException(msg);
        }
    }
}
