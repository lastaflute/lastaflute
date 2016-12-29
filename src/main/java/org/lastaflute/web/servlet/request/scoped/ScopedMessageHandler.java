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
package org.lastaflute.web.servlet.request.scoped;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.message.UserMessage;
import org.lastaflute.core.message.UserMessages;

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
     * Save message as (global) user messages. (overriding existing messages) <br>
     * This message will be deleted immediately after display if you use e.g. la:errors.
     * @param property The property name corresponding to the message. (NotNull)
     * @param messageKey The message key to be saved. (NotNull)
     * @param args The varying array of arguments for the message. (NullAllowed, EmptyAllowed)
     */
    public void save(String property, String messageKey, Object... args) {
        assertObjectNotNull("messageKey", messageKey);
        doSaveInfo(prepareUserMessages(globalPropertyKey, messageKey, args));
    }

    /**
     * Save message as global user messages. (overriding existing messages) <br>
     * This message will be deleted immediately after display if you use e.g. la:errors.
     * @param messageKey The message key to be saved. (NotNull)
     * @param args The varying array of arguments for the message. (NullAllowed, EmptyAllowed)
     */
    public void saveGlobal(String messageKey, Object... args) {
        assertObjectNotNull("messageKey", messageKey);
        doSaveInfo(prepareUserMessages(globalPropertyKey, messageKey, args));
    }

    /**
     * Save message as user messages. (overriding existing messages) <br>
     * This message will be deleted immediately after display if you use e.g. la:errors.
     * @param messages The action message for messages. (NotNull, EmptyAllowed)
     */
    public void saveMessages(UserMessages messages) {
        assertObjectNotNull("messages", messages);
        doSaveInfo(messages);
    }

    protected void doSaveInfo(UserMessages messages) {
        attributeHolder.setAttribute(getMessagesKey(), messages);
    }

    // ===================================================================================
    //                                                                                Add
    //                                                                               =====
    /**
     * Add message as named user messages to rear of existing messages. <br>
     * This message will be deleted immediately after display if you use e.g. la:errors.
     * @param property The property name corresponding to the message. (NotNull)
     * @param messageKey The message key to be added. (NotNull)
     * @param args The varying array of arguments for the message. (NullAllowed, EmptyAllowed)
     */
    public void add(String property, String messageKey, Object... args) {
        assertObjectNotNull("property", property);
        assertObjectNotNull("messageKey", messageKey);
        doAddMessages(prepareUserMessages(property, messageKey, args));
    }

    /**
     * Add message as global user messages to rear of existing messages. <br>
     * This message will be deleted immediately after display if you use e.g. la:errors.
     * @param messageKey The message key to be added. (NotNull)
     * @param args The varying array of arguments for the message. (NullAllowed, EmptyAllowed)
     */
    public void addGlobal(String messageKey, Object... args) {
        assertObjectNotNull("messageKey", messageKey);
        doAddMessages(prepareUserMessages(globalPropertyKey, messageKey, args));
    }

    /**
     * Add user messages to rear of existing messages. <br>
     * This message will be deleted immediately after display if you use e.g. la:errors.
     * @param messages The action message for messages. (NotNull, EmptyAllowed)
     */
    public void addMessages(UserMessages messages) {
        assertObjectNotNull("messages", messages);
        doAddMessages(messages);
    }

    protected void doAddMessages(UserMessages messages) {
        final UserMessages existingOrCreated = get().orElseGet(() -> newUserMessages());
        existingOrCreated.add(messages);
        doSaveInfo(existingOrCreated);
    }

    protected UserMessages newUserMessages() {
        return new UserMessages();
    }

    // ===================================================================================
    //                                                                  Has, Get and Clear
    //                                                                  ==================
    /**
     * Does it have messages as (global or specified property) user messages at least one?
     * @return The determination, true or false.
     */
    public boolean has() {
        return attributeHolder.getAttribute(getMessagesKey(), UserMessages.class).filter(messages -> {
            return !messages.isEmpty();
        }).isPresent();
    }

    /**
     * Get action message from (global) user messages.
     * @return The optional action message. (NotNull)
     */
    public OptionalThing<UserMessages> get() {
        return attributeHolder.getAttribute(getMessagesKey(), UserMessages.class);
    }

    /**
     * Clear (global) user messages from the scope.
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

    protected UserMessages prepareUserMessages(String property, String messageKey, Object[] args) {
        final UserMessages messages = newUserMessages();
        messages.add(property, new UserMessage(messageKey, args));
        return messages;
    }

    protected void assertObjectNotNull(String variableName, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
        }
    }
}
