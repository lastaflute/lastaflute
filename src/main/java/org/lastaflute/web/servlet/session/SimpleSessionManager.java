/*
 * Copyright 2015-2018 the original author or authors.
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
package org.lastaflute.web.servlet.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.direction.FwWebDirection;
import org.lastaflute.web.exception.SessionAttributeCannotCastException;
import org.lastaflute.web.exception.SessionAttributeNotFoundException;
import org.lastaflute.web.servlet.filter.hotdeploy.HotdeployHttpSession;
import org.lastaflute.web.servlet.request.scoped.ScopedMessageHandler;
import org.lastaflute.web.util.LaRequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The simple implementation of session manager. <br>
 * This class is basically defined at DI setting file.
 * @author jflute
 */
public class SimpleSessionManager implements SessionManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(SimpleSessionManager.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant director (AD) for framework. (NotNull: after initialization) */
    @Resource
    private FwAssistantDirector assistantDirector;

    /** The shared storage of session for session sharing. (NotNull, EmptyAllowed: if no storage) */
    protected OptionalThing<SessionSharedStorage> sessionSharedStorage = OptionalThing.empty(); // not null

    /** The arranger of HTTP session for session sharing. (NotNull, EmptyAllowed: if no arranger) */
    protected OptionalThing<HttpSessionArranger> httpSessionArranger = OptionalThing.empty(); // not null

    protected ScopedMessageHandler errorsHandler; // lazy loaded
    protected ScopedMessageHandler infoHandler; // lazy loaded

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    /**
     * Initialize this component. <br>
     * This is basically called by DI setting file.
     */
    @PostConstruct
    public synchronized void initialize() {
        final FwWebDirection direction = assistWebDirection();
        final SessionResourceProvider provider = direction.assistSessionResourceProvider();
        sessionSharedStorage = prepareSessionSharedStorage(provider);
        httpSessionArranger = prepareHttpSessionArranger(provider);
        showBootLogging();
    }

    protected FwWebDirection assistWebDirection() {
        return assistantDirector.assistWebDirection();
    }

    protected OptionalThing<SessionSharedStorage> prepareSessionSharedStorage(SessionResourceProvider provider) {
        final SessionSharedStorage specifiedStorage = provider != null ? provider.provideSharedStorage() : null;
        return OptionalThing.ofNullable(specifiedStorage, () -> {
            throw new IllegalStateException("Not found the session shared storage: " + provider);
        });
    }

    protected OptionalThing<HttpSessionArranger> prepareHttpSessionArranger(SessionResourceProvider provider) {
        final HttpSessionArranger specifiedStorage = provider != null ? provider.provideHttpSessionArranger() : null;
        return OptionalThing.ofNullable(specifiedStorage, () -> {
            throw new IllegalStateException("Not found the HTTP session arranger: " + provider);
        });
    }

    protected void showBootLogging() {
        if (logger.isInfoEnabled()) {
            logger.info("[Session Manager]");
            logger.info(" sessionSharedStorage: " + sessionSharedStorage);
            logger.info(" httpSessionArranger: " + httpSessionArranger);
        }
    }

    // ===================================================================================
    //                                                                  Attribute Handling
    //                                                                  ==================
    @Override
    public <ATTRIBUTE> OptionalThing<ATTRIBUTE> getAttribute(String key, Class<ATTRIBUTE> attributeType) {
        assertArgumentNotNull("key", key);
        final OptionalThing<ATTRIBUTE> foundShared = findAttributeInShareStorage(key, attributeType);
        if (foundShared.isPresent()) {
            return foundShared;
        }
        final HttpSession session = getSessionExisting();
        final Object original = session != null ? session.getAttribute(key) : null;
        if (original instanceof HotdeployHttpSession.SerializedObjectHolder) { // e.g. hot to cool in Tomcat
            logger.debug("...Removing relic session of hot deploy: {}", original);
            removeAttribute(key); // treated as no-existing
            return OptionalThing.empty();
        }
        final ATTRIBUTE attribute;
        if (original != null) {
            try {
                attribute = attributeType.cast(original);
            } catch (ClassCastException e) {
                final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
                br.addNotice("Cannot cast the session attribute");
                br.addItem("Attribute Key");
                br.addElement(key);
                br.addItem("Specified Type");
                br.addElement(attributeType + "@" + Integer.toHexString(attributeType.hashCode()));
                br.addElement("loader: " + attributeType.getClassLoader());
                br.addItem("Existing Attribute");
                final Class<? extends Object> originType = original.getClass();
                br.addElement(originType + "@" + Integer.toHexString(originType.hashCode()));
                br.addElement("loader: " + originType.getClassLoader());
                br.addElement("toString(): " + original.toString());
                br.addItem("Attribute List");
                br.addElement(getAttributeNameList());
                final String msg = br.buildExceptionMessage();
                throw new SessionAttributeCannotCastException(msg, e);
            }
            reflectAttributeToSharedStorage(key, attribute);
        } else {
            attribute = null;
        }
        return OptionalThing.ofNullable(attribute, () -> {
            final List<String> nameList = getAttributeNameList();
            final String msg = "Not found the session attribute by the string key: " + key + " existing=" + nameList;
            throw new SessionAttributeNotFoundException(msg);
        });
    }

    protected <ATTRIBUTE> OptionalThing<ATTRIBUTE> findAttributeInShareStorage(String key, Class<ATTRIBUTE> attributeType) {
        final OptionalThing<ATTRIBUTE> found = sessionSharedStorage.flatMap(storage -> storage.getAttribute(key, attributeType));
        if (logger.isDebugEnabled() && found.isPresent()) {
            logger.debug("Found the session attribute in shared storage: {}={}", key, found.get());
        }
        return found;
    }

    protected void reflectAttributeToSharedStorage(String key, Object value) {
        sessionSharedStorage.ifPresent(storage -> {
            logger.debug("...Reflecting the session attribute to shared storage: {}={}", key, value);
            storage.setAttribute(key, value);
        });
    }

    protected List<String> getAttributeNameList() {
        final HttpSession session = getSessionExisting();
        if (session == null) {
            return Collections.emptyList();
        }
        final Enumeration<String> attributeNames = session.getAttributeNames();
        final List<String> nameList = new ArrayList<String>();
        while (attributeNames.hasMoreElements()) {
            nameList.add((String) attributeNames.nextElement());
        }
        return Collections.unmodifiableList(nameList);
    }

    @Override
    public void setAttribute(String key, Object value) {
        assertArgumentNotNull("key", key);
        assertArgumentNotNull("value", value);
        saveAttributeToSharedStorage(key, value);
        if (!isSuppressHttpSession()) {
            getSessionOrCreated().setAttribute(key, value);
        }
    }

    protected void saveAttributeToSharedStorage(String key, Object value) {
        sessionSharedStorage.ifPresent(storage -> {
            logger.debug("...Saving the session attribute to shared storage: {}={}", key, value);
            storage.setAttribute(key, value);
        });
    }

    @Override
    public void removeAttribute(String key) {
        assertArgumentNotNull("key", key);
        removeAttributeFromSharedStorage(key);
        final HttpSession session = getSessionExisting();
        if (session != null) {
            session.removeAttribute(key);
        }
    }

    protected void removeAttributeFromSharedStorage(String key) {
        sessionSharedStorage.ifPresent(storage -> {
            logger.debug("...Removing the session attribute to shared storage: {}", key);
            storage.removeAttribute(key);
        });
    }

    // see interface ScopedAttributeHolder for the detail
    //@Override
    //@SuppressWarnings("unchecked")
    //public <ATTRIBUTE> OptionalThing<ATTRIBUTE> getAttribute(Class<ATTRIBUTE> typeKey) {
    //    assertArgumentNotNull("type", typeKey);
    //    final HttpSession session = getSessionExisting();
    //    final String key = typeKey.getName();
    //    final ATTRIBUTE attribute = session != null ? (ATTRIBUTE) session.getAttribute(key) : null;
    //    return OptionalThing.ofNullable(attribute, () -> {
    //        final List<String> nameList = getAttributeNameList();
    //        final String msg = "Not found the session attribute by the typed key: " + key + " existing=" + nameList;
    //        throw new SessionAttributeNotFoundException(msg);
    //    });
    //}
    //@Override
    //public void setAttribute(Object value) {
    //    assertArgumentNotNull("value", value);
    //    checkTypedAttributeSettingMistake(value);
    //    getSessionOrCreated().setAttribute(value.getClass().getName(), value);
    //}
    //protected void checkTypedAttributeSettingMistake(Object value) {
    //    if (value instanceof String) {
    //        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
    //        br.addNotice("The value for typed attribute was simple string type.");
    //        br.addItem("Advice");
    //        br.addElement("The value should not be string.");
    //        br.addElement("Do you forget value setting for the string key?");
    //        br.addElement("The typed attribute setting cannot accept string");
    //        br.addElement("to suppress setting mistake like this:");
    //        br.addElement("  (x):");
    //        br.addElement("    sessionManager.setAttribute(\"foo.bar\")");
    //        br.addElement("  (o):");
    //        br.addElement("    sessionManager.setAttribute(\"foo.bar\", value)");
    //        br.addElement("  (o):");
    //        br.addElement("    sessionManager.setAttribute(bean)");
    //        br.addItem("Specified Value");
    //        br.addElement(value != null ? value.getClass().getName() : null);
    //        br.addElement(value);
    //        final String msg = br.buildExceptionMessage();
    //        throw new IllegalArgumentException(msg);
    //    }
    //}
    //@Override
    //public void removeAttribute(Class<?> type) {
    //    assertArgumentNotNull("type", type);
    //    final HttpSession session = getSessionExisting();
    //    if (session != null) {
    //        session.removeAttribute(type.getName());
    //    }
    //}

    // ===================================================================================
    //                                                                    Session Handling
    //                                                                    ================
    @Override
    public String getSessionId() {
        return sessionSharedStorage.flatMap(storage -> storage.getSessionId()).orElseGet(() -> {
            if (isSuppressHttpSession()) {
                String msg = "Not found the session ID of shared storage. (required if no http session)";
                throw new IllegalStateException(msg);
            }
            return getSessionOrCreated().getId(); // normally here
        });
    }

    @Override
    public void invalidate() {
        invalidateSharedStorage();
        final HttpSession session = getSessionExisting();
        if (session != null) {
            session.invalidate();
        }
    }

    protected void invalidateSharedStorage() {
        sessionSharedStorage.ifPresent(storage -> storage.invalidate());
    }

    @Override
    public void regenerateSessionId() {
        regenerateSessionIdOfSharedStorage();
        final HttpSession session = getSessionExisting();
        if (session == null) {
            return;
        }
        final Map<String, Object> savedSessionMap = extractHttpSessionMap(session);
        session.invalidate(); // regenerate ID, native only here
        for (Entry<String, Object> entry : savedSessionMap.entrySet()) {
            setAttribute(entry.getKey(), entry.getValue()); // inherit existing attributes
        }
    }

    protected void regenerateSessionIdOfSharedStorage() {
        sessionSharedStorage.ifPresent(storage -> storage.regenerateSessionId());
    }

    protected Map<String, Object> extractHttpSessionMap(HttpSession session) { // native only
        final Enumeration<String> attributeNames = session.getAttributeNames();
        final Map<String, Object> savedSessionMap = new LinkedHashMap<String, Object>();
        while (attributeNames.hasMoreElements()) { // save existing attributes temporarily
            final String key = attributeNames.nextElement();
            getAttribute(key, Object.class).ifPresent(attribute -> {
                savedSessionMap.put(key, attribute);
            }); // almost be present, but rare case handling just in case
        }
        return savedSessionMap;
    }

    // ===================================================================================
    //                                                                    Message Handling
    //                                                                    ================
    @Override
    public ScopedMessageHandler errors() {
        if (errorsHandler == null) {
            synchronized (this) {
                if (errorsHandler == null) {
                    errorsHandler = createScopedMessageHandler(getErrorMessagesKey());
                }
            }
        }
        return errorsHandler;
    }

    protected String getErrorMessagesKey() {
        return LastaWebKey.ACTION_ERRORS_KEY;
    }

    @Override
    public ScopedMessageHandler info() {
        if (infoHandler == null) {
            synchronized (this) {
                if (infoHandler == null) {
                    infoHandler = createScopedMessageHandler(getInfoMessagesKey());
                }
            }
        }
        return infoHandler;
    }

    protected ScopedMessageHandler createScopedMessageHandler(String messagesKey) {
        return new ScopedMessageHandler(this, UserMessages.GLOBAL, messagesKey);
    }

    protected String getInfoMessagesKey() {
        return LastaWebKey.ACTION_INFO_KEY;
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected HttpServletRequest getRequest() { // basically not null (but null allowed when asynchronous process)
        return LaRequestUtil.getRequest();
    }

    protected HttpSession getSessionOrCreated() { // not null
        //if (isSuppressHttpSession()) {
        //    return ...; // #hope use empty session, but caller check for now by jflute
        //}
        return readyHttpSession(getRequest(), true); // #thinking needs to check for asynchronous process? by jflute
    }

    protected HttpSession getSessionExisting() { // null allowed
        if (isSuppressHttpSession()) {
            return null;
        }
        final HttpServletRequest request = getRequest(); // null allowed when e.g. asynchronous process
        return request != null ? readyHttpSession(request, false) : null;
    }

    protected boolean isSuppressHttpSession() {
        return sessionSharedStorage.map(storage -> storage.suppressesHttpSession()).orElse(false);
    }

    protected HttpSession readyHttpSession(HttpServletRequest request, boolean create) {
        if (httpSessionArranger.isPresent()) {
            return httpSessionArranger.get().create(request, create); // null allowed if create is false
        } else {
            return request.getSession(create); // as default
        }
        // it should return null without default process if arranger.create() returns null  
        //return httpSessionArranger.map(ger -> ger.create(request, create)).orElseGet(() -> {
        //    return request.getSession(create); // as default
        //});
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
