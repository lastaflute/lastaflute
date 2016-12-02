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
