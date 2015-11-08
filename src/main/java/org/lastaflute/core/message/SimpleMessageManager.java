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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.lastaflute.core.message.exception.MessageKeyNotFoundException;
import org.lastaflute.web.ruts.message.ActionMessage;
import org.lastaflute.web.ruts.message.ActionMessages;

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
        return doGetMessage(locale, key);
    }

    protected String doGetMessage(Locale locale, String key) {
        return doFindMessage(locale, key).get();
    }

    @Override
    public String getMessage(Locale locale, String key, Object... values) {
        return doGetMessage(locale, key, values);
    }

    protected String doGetMessage(Locale locale, String key, Object[] values) {
        return doFindMessage(locale, key, values).get();
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
        final String filtered = filterMessageKey(key);
        final String message = gateway.getMessage(locale, filtered);
        return OptionalThing.ofNullable(message, () -> {
            throwMessageKeyNotFoundException(locale, key, filtered);
        });
    }

    protected void throwMessageKeyNotFoundException(Locale locale, String key, String filtered) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the message by the key.");
        br.addItem("Key");
        br.addElement(key);
        if (!key.equals(filtered)) {
            br.addElement("(filtered: " + filtered + ")");
        }
        br.addItem("Locale");
        br.addElement(locale);
        br.addItem("MessageResources");
        br.addElement(messageResourcesHolder);
        final String msg = br.buildExceptionMessage();
        throw new MessageKeyNotFoundException(msg);
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
        final String filtered = filterMessageKey(key);
        final String message = gateway.getMessage(locale, filtered, values);
        return OptionalThing.ofNullable(message, () -> {
            throwMessageKeyNotFoundException(locale, filtered, values);
        });
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

    // -----------------------------------------------------
    //                                            Key Filter
    //                                            ----------
    protected String filterMessageKey(String key) { // basically for hibernate validator
        return filterEmbeddedDomain(filterBrace(key));
    }

    protected String filterBrace(String key) {
        return Srl.isQuotedAnything(key, "{", "}") ? Srl.unquoteAnything(key, "{", "}") : key;
    }

    protected String filterEmbeddedDomain(String key) {
        final String javaxPackage = "javax.validation.";
        final String hibernatePackage = "org.hibernate.validator.";
        final String lastaflutePackage = "org.lastaflute.validator.";
        final String realKey;
        if (key.startsWith(javaxPackage)) {
            realKey = Srl.substringFirstRear(key, javaxPackage);
        } else if (key.startsWith(hibernatePackage)) {
            realKey = Srl.substringFirstRear(key, hibernatePackage);
        } else if (key.startsWith(lastaflutePackage)) {
            realKey = Srl.substringFirstRear(key, lastaflutePackage);
        } else {
            realKey = key;
        }
        return realKey;
    }

    // ===================================================================================
    //                                                                    Resolved Message
    //                                                                    ================
    @Override
    public List<String> toMessageList(Locale locale, ActionMessages errors) {
        assertArgumentNotNull("locale", locale);
        assertArgumentNotNull("errors", errors);
        if (errors.isEmpty()) {
            return Collections.emptyList();
        }
        final List<String> messageList = new ArrayList<String>();
        final Iterator<ActionMessage> ite = errors.accessByFlatIterator();
        while (ite.hasNext()) {
            messageList.add(resolveMessageText(locale, ite.next()));
        }
        return Collections.unmodifiableList(messageList);
    }

    @Override
    public Map<String, List<String>> toPropertyMessageMap(Locale locale, ActionMessages errors) {
        assertArgumentNotNull("locale", locale);
        assertArgumentNotNull("errors", errors);
        if (errors.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, List<String>> messageMap = new LinkedHashMap<String, List<String>>();
        final Set<String> propertySet = errors.toPropertySet();
        for (String property : propertySet) {
            final List<String> messageList = new ArrayList<String>();
            for (Iterator<ActionMessage> ite = errors.accessByIteratorOf(property); ite.hasNext();) {
                messageList.add(resolveMessageText(locale, ite.next()));
            }
            messageMap.put(property, Collections.unmodifiableList(messageList));
        }
        return Collections.unmodifiableMap(messageMap);
    }

    protected String resolveMessageText(Locale locale, ActionMessage message) {
        final String key = message.getKey();
        return message.isResource() ? doGetMessage(locale, key, message.getValues()) : key;
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
    //                                                                        Small Helper
    //                                                                        ============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            throw new IllegalArgumentException("The variableName should not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
        }
    }
}
