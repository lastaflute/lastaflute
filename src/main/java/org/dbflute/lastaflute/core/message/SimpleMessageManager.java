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
package org.dbflute.lastaflute.core.message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.lastaflute.core.message.exception.MessageKeyNotFoundException;
import org.dbflute.lastaflute.web.ruts.message.ActionMessage;
import org.dbflute.lastaflute.web.ruts.message.ActionMessages;
import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 */
public class SimpleMessageManager implements MessageManager {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    @Resource
    protected MessageResourcesHolder messageResourcesHolder;

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    /**
     * Initialize this component. <br>
     * This is basically called by DI setting file.
     */
    @PostConstruct
    public synchronized void initialize() {
        // empty for now
    }

    // ===================================================================================
    //                                                                         Get Message
    //                                                                         ===========
    @Override
    public String getMessage(Locale locale, String key) {
        assertArgumentNotNull("locale", locale);
        assertArgumentNotNull("key", key);
        return doFindMessage(locale, key).get();
    }

    protected void throwMessageKeyNotFoundException(Locale locale, String key) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the message by the key.");
        br.addItem("Key");
        br.addElement(key);
        br.addItem("Locale");
        br.addElement(locale);
        br.addItem("MessageResources");
        br.addElement(messageResourcesHolder);
        final String msg = br.buildExceptionMessage();
        throw new MessageKeyNotFoundException(msg);
    }

    @Override
    public String getMessage(Locale locale, String key, Object[] values) {
        assertArgumentNotNull("locale", locale);
        assertArgumentNotNull("key", key);
        assertArgumentNotNull("values", values);
        final MessageResourcesGateway gateway = getMessageResourceGateway();
        final String message = gateway.getMessage(locale, key, values);
        if (message == null) {
            throwMessageKeyNotFoundException(locale, key, values);
        }
        return message;
    }

    protected void throwMessageKeyNotFoundException(Locale locale, String key, Object[] values) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the message by the key.");
        br.addItem("Key");
        br.addElement(key);
        br.addItem("Values");
        br.addElement(values != null ? Arrays.asList(values) : null);
        br.addItem("Locale");
        br.addElement(locale);
        br.addItem("MessageResources");
        br.addElement(messageResourcesHolder);
        final String msg = br.buildExceptionMessage();
        throw new MessageKeyNotFoundException(msg);
    }

    // ===================================================================================
    //                                                                        Find Message
    //                                                                        ============
    @Override
    public OptionalThing<String> findMessage(Locale locale, String key) {
        assertArgumentNotNull("locale", locale);
        assertArgumentNotNull("key", key);
        return doFindMessage(locale, key);
    }

    protected OptionalThing<String> doFindMessage(Locale locale, String key) {
        final MessageResourcesGateway gateway = getMessageResourceGateway();
        final String message = gateway.getMessage(locale, key);
        return OptionalThing.ofNullable(message, () -> {
            throwMessageKeyNotFoundException(locale, key);
        });
    }

    @Override
    public OptionalThing<String> findMessage(Locale locale, String key, Object[] values) {
        assertArgumentNotNull("locale", locale);
        assertArgumentNotNull("key", key);
        assertArgumentNotNull("values", values);
        return doFindMessage(locale, key, values);
    }

    protected OptionalThing<String> doFindMessage(Locale locale, String key, Object[] values) {
        final MessageResourcesGateway gateway = getMessageResourceGateway();
        final String message = gateway.getMessage(locale, key, values);
        return OptionalThing.ofNullable(message, () -> {
            throwMessageKeyNotFoundException(locale, key, values);
        });
    }

    // ===================================================================================
    //                                                                    Resolved Message
    //                                                                    ================
    @Override
    public List<String> getMessageList(Locale locale, ActionMessages errors) {
        assertArgumentNotNull("locale", locale);
        assertArgumentNotNull("errors", errors);
        final List<String> messageList = new ArrayList<String>();
        if (errors.isEmpty()) {
            return messageList;
        }
        final Iterator<ActionMessage> ite = errors.get();
        while (ite.hasNext()) {
            final ActionMessage actionMessage = (ActionMessage) ite.next();
            final String messageText = resolveMessageText(locale, actionMessage);
            messageList.add(messageText);
        }
        return messageList;
    }

    @Override
    public Map<String, List<String>> getPropertyMessageMap(Locale locale, ActionMessages errors) {
        assertArgumentNotNull("locale", locale);
        assertArgumentNotNull("errors", errors);
        final Map<String, List<String>> propertyMessageMap = new LinkedHashMap<String, List<String>>();
        if (errors.isEmpty()) {
            return propertyMessageMap;
        }
        final Iterator<String> properyIte = errors.properties();
        while (properyIte.hasNext()) {
            final String property = properyIte.next();
            List<String> messageList = propertyMessageMap.get(property);
            if (messageList == null) {
                messageList = new ArrayList<String>();
            }
            final Iterator<ActionMessage> actionMessageIte = errors.get(property);
            while (actionMessageIte.hasNext()) {
                final ActionMessage actionMessage = actionMessageIte.next();
                final String messageText = resolveMessageText(locale, actionMessage);
                messageList.add(messageText);
            }
            propertyMessageMap.put(property, messageList);
        }
        return propertyMessageMap;
    }

    protected String resolveMessageText(Locale locale, ActionMessage actionMessage) {
        final String key = actionMessage.getKey();
        final Object[] values = actionMessage.getValues();
        final String messageText;
        if (actionMessage.isResource()) {
            messageText = getMessage(locale, key, values);
        } else {
            messageText = key;
        }
        return messageText;
    }

    // ===================================================================================
    //                                                                   Message Resources
    //                                                                   =================
    @Override
    public MessageResourcesGateway getMessageResourceGateway() {
        final MessageResourcesGateway gateway = messageResourcesHolder.getGateway();
        if (gateway == null) {
            String msg = "Not found the gateway for message resource: holder=" + messageResourcesHolder;
            throw new IllegalStateException(msg);
        }
        return gateway;
    }

    // ===================================================================================
    //                                                                       Assist Helper
    //                                                                       =============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            throw new IllegalArgumentException("The variableName should not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
        }
    }
}
