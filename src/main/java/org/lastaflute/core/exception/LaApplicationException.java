/*
 * Copyright 2015-2016 the original author or authors.
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
    //                                                                             Message
    //                                                                             =======
    public List<LaApplicationMessage> getMessageList() {
        return messageList != null ? Collections.unmodifiableList(messageList) : Collections.emptyList();
    }

    public void saveMessage(String messageKey, Object... values) {
        if (messageKey == null) {
            throw new IllegalArgumentException("The argument 'messageKey' should not be null.");
        }
        if (values == null) {
            throw new IllegalArgumentException("The argument 'values' should not be null.");
        }
        if (messageList == null) {
            messageList = new ArrayList<LaApplicationMessage>(1);
        }
        messageList.add(newApplicationMessage(messageKey, values));
    }

    protected LaApplicationMessage newApplicationMessage(String messageKey, Object... values) {
        return new LaApplicationMessage(messageKey, values);
    }

    public void saveMessages(List<LaApplicationMessage> itemList) {
        if (itemList == null) {
            throw new IllegalArgumentException("The argument 'itemList' should not be null.");
        }
        this.messageList = itemList;
    }
}
