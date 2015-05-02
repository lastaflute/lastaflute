/*
 * Copyright 2014-2015 the original author or authors.
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
package org.lastaflute.web.servlet.request.scoped;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.web.ruts.message.ActionMessage;
import org.lastaflute.web.ruts.message.ActionMessages;

/**
 * @author jflute
 */
public class ScopedMessageHandler {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ScopedAttributeHolder attributeHolder;
    protected final String globalPropertyKey;
    protected final String messagesKey;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ScopedMessageHandler(ScopedAttributeHolder attributeHolder, String globalPropertyKey, String messagesKey) {
        this.attributeHolder = attributeHolder;
        this.globalPropertyKey = globalPropertyKey;
        this.messagesKey = messagesKey;
    }

    // ===================================================================================
    //                                                                               Save
    //                                                                              ======
    /**
     * Save message as (global) action messages. (overriding existing messages) <br>
     * This message will be deleted immediately after display if you use e.g. la:errors.
     * @param messageKey The message key to be saved. (NotNull)
     * @param args The varying array of arguments for the message. (NullAllowed, EmptyAllowed)
     */
    public void save(String messageKey, Object... args) {
        assertObjectNotNull("messageKey", messageKey);
        doSaveInfo(prepareActionMessages(messageKey, args));
    }

    /**
     * Save message as (global) action messages. (overriding existing messages) <br>
     * This message will be deleted immediately after display if you use e.g. la:errors.
     * @param messages The action message for messages. (NotNull, EmptyAllowed)
     */
    public void save(ActionMessages messages) {
        assertObjectNotNull("messages", messages);
        doSaveInfo(messages);
    }

    protected void doSaveInfo(ActionMessages messages) {
        attributeHolder.setAttribute(getMessagesKey(), messages);
    }

    // ===================================================================================
    //                                                                                Add
    //                                                                               =====
    /**
     * Add message as (global) action messages to rear of existing messages. <br>
     * This message will be deleted immediately after display if you use e.g. la:errors.
     * @param messageKey The message key to be added. (NotNull)
     * @param args The varying array of arguments for the message. (NullAllowed, EmptyAllowed)
     */
    public void add(String messageKey, Object... args) {
        assertObjectNotNull("messageKey", messageKey);
        doAddMessages(prepareActionMessages(messageKey, args));
    }

    protected void doAddMessages(ActionMessages messages) {
        if (messages == null) {
            return;
        }
        final ActionMessages existingOrCreated = get().orElseGet(() -> newActionMessages());
        existingOrCreated.add(messages);
        doSaveInfo(existingOrCreated);
    }

    protected ActionMessages newActionMessages() {
        return new ActionMessages();
    }

    // ===================================================================================
    //                                                                  Has, Get and Clear
    //                                                                  ==================
    /**
     * Does it have messages as (global or specified property) action messages at least one?
     * @return The determination, true or false.
     */
    public boolean has() {
        return attributeHolder.getAttribute(getMessagesKey(), ActionMessages.class).filter(messages -> {
            return !messages.isEmpty();
        }).isPresent();
    }

    /**
     * Get action message from (global) action messages.
     * @return The optional action message. (NotNull)
     */
    public OptionalThing<ActionMessages> get() {
        return attributeHolder.getAttribute(getMessagesKey(), ActionMessages.class);
    }

    /**
     * Clear (global) action messages from the scope.
     */
    public void clear() {
        attributeHolder.removeAttribute(getMessagesKey());
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected String getMessagesKey() {
        return messagesKey;
    }

    protected ActionMessages prepareActionMessages(String messageKey, Object[] args) {
        final ActionMessages messages = newActionMessages();
        messages.add(globalPropertyKey, new ActionMessage(messageKey, args));
        return messages;
    }

    protected void assertObjectNotNull(String variableName, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
        }
    }
}
