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
package org.lastaflute.core.message;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.message.exception.MessageKeyNotFoundException;
import org.lastaflute.web.ruts.message.ActionMessages;

/**
 * The manager of message. <br>
 * You can get the message resource as various type. <br>
 * But saving and adding message is provided at RequestManager, SessionManager.
 * @author jflute
 */
public interface MessageManager {

    /**
     * Get the message by the key.
     * @param locale The locale for the message. (NotNull)
     * @param key The key of message. (NotNull)
     * @return The found message, specified locale resolved. (NotNull: if not found, throws exception)
     * @throws MessageKeyNotFoundException When the message is not found.
     */
    String getMessage(Locale locale, String key);

    /**
     * Get the message by the key.
     * @param locale The locale for the message. (NotNull)
     * @param key The key of message. (NotNull)
     * @param values The array of parameters. (NotNull)
     * @return The found message, specified locale resolved. (NotNull: if not found, throws exception)
     * @throws MessageKeyNotFoundException When the message is not found.
     */
    String getMessage(Locale locale, String key, Object[] values);

    /**
     * Find the message by the key.
     * @param locale The locale for the message. (NotNull)
     * @param key The key of message. (NotNull)
     * @return The optional message, specified locale resolved. (NotNull, EmptyAllowed: when not found)
     */
    OptionalThing<String> findMessage(Locale locale, String key);

    /**
     * Find the message by the key.
     * @param locale The locale for the message. (NotNull)
     * @param key The key of message. (NotNull)
     * @param values The array of parameters. (NotNull)
     * @return The optional message, specified locale resolved. (NotNull, EmptyAllowed: when not found)
     */
    OptionalThing<String> findMessage(Locale locale, String key, Object[] values);

    /**
     * Get the list of message text for the errors.
     * @param locale The locale for the message. (NotNull)
     * @param errors The action messages for the errors. (NotNull)
     * @return The list of message, resolved by resource. (NotNull, EmptyAllowed)
     */
    List<String> getMessageList(Locale locale, ActionMessages errors);

    /**
     * Get the map (property : list of message text) of message for the errors.
     * @param locale The locale for the message. (NotNull)
     * @param errors The action messages for the errors. (NotNull)
     * @return The map of message, resolved by resource. (NotNull, EmptyAllowed)
     */
    Map<String, List<String>> getPropertyMessageMap(Locale locale, ActionMessages errors);

    /**
     * Get the gateway for message resources of e.g. Struts.
     * @return The instance of gateway. (NotNull: if not prepared, throws exception)
     */
    MessageResourcesGateway getMessageResourceGateway();
}
