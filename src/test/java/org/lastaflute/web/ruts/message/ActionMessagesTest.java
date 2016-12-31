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
package org.lastaflute.web.ruts.message;

import org.dbflute.utflute.core.PlainTestCase;
import org.lastaflute.core.message.UserMessage;
import org.lastaflute.core.message.UserMessages;

/**
 * @author jflute
 */
public class ActionMessagesTest extends PlainTestCase {

    public void test_hasMessageOf() throws Exception {
        // ## Arrange ##
        UserMessages messages = new UserMessages();
        assertFalse(messages.hasMessageOf("sea"));
        UserMessage message = new UserMessage("dockside", "over");
        assertFalse(messages.isAccessed());
        messages.add("sea", message);

        // ## Act ##
        // ## Assert ##
        assertTrue(messages.hasMessageOf("sea"));
        assertFalse(messages.hasMessageOf("land"));
        assertFalse(messages.isAccessed());
    }

    public void test_hasMessageOf_key() throws Exception {
        // ## Arrange ##
        UserMessages messages = new UserMessages();
        assertFalse(messages.hasMessageOf("sea"));
        UserMessage message = new UserMessage("dockside", "over");
        assertFalse(messages.isAccessed());
        messages.add("sea", message);

        // ## Act ##
        // ## Assert ##
        assertTrue(messages.hasMessageOf("sea", "dockside"));
        assertFalse(messages.hasMessageOf("sea", "hangar"));
        assertFalse(messages.hasMessageOf("land", "dockside"));
        assertFalse(messages.isAccessed());
    }
}
