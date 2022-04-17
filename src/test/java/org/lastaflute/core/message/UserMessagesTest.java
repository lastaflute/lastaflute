/*
 * Copyright 2015-2022 the original author or authors.
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
package org.lastaflute.core.message;

import java.util.Iterator;

import org.lastaflute.unit.UnitLastaFluteTestCase;

/**
 * @author jflute
 */
public class UserMessagesTest extends UnitLastaFluteTestCase {

    public void test_basic() {
        UserMessages messages = new UserMessages();
        messages.add("sea", new UserMessage("mystic", "hangar"));
        messages.add("{labels.sea}", new UserMessage("bigband", "theatre"));
        assertTrue(messages.hasMessageOf("sea"));
        assertTrue(messages.hasMessageOf("labels.sea"));
        assertTrue(messages.hasMessageOf("{labels.sea}"));
        assertTrue(messages.hasMessageOf("sea", "bigband"));
        assertTrue(messages.hasMessageOf("sea", "{bigband}"));
        Iterator<UserMessage> iterator = messages.accessByIteratorOf("sea");
        assertTrue(iterator.hasNext());
        UserMessage mystic = (UserMessage) iterator.next();
        assertEquals("mystic", mystic.getMessageKey());
        assertTrue(iterator.hasNext());
        UserMessage bigband = (UserMessage) iterator.next();
        assertEquals("bigband", bigband.getMessageKey());
        assertFalse(iterator.hasNext());
    }
}
