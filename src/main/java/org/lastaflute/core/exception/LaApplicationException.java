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
    protected static final Object[] EMPTY_ARGS = new Object[] {};

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected List<LaApplicationMessageItem> itemList; // lazy loaded

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
    //                                                                            Accessor
    //                                                                            ========
    public List<LaApplicationMessageItem> getMessageItemList() {
        return itemList != null ? itemList : Collections.emptyList();
    }

    public void saveMessage(String messageKey, Object... values) {
        if (messageKey == null) {
            throw new IllegalArgumentException("The argument 'messageKey' should not be null.");
        }
        if (itemList == null) {
            itemList = new ArrayList<LaApplicationMessageItem>(1);
        }
        itemList.add(new LaApplicationMessageItem(messageKey, values != null ? values : EMPTY_ARGS));
    }

    public void saveMessages(List<LaApplicationMessageItem> itemList) {
        if (itemList == null) {
            throw new IllegalArgumentException("The argument 'itemList' should not be null.");
        }
        this.itemList = itemList;
    }
}
