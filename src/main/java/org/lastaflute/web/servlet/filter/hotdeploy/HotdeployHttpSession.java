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
package org.lastaflute.web.servlet.filter.hotdeploy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import org.lastaflute.di.core.smart.hot.HotdeployUtil;
import org.lastaflute.di.exception.SessionObjectNotSerializableRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author modified by jflute (originated in Seasar)
 */
@SuppressWarnings("deprecation")
public class HotdeployHttpSession implements HttpSession {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(HotdeployHttpSession.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final HotdeployHttpServletRequest request;
    protected final HttpSession originalSession;
    protected final Map<String, Object> attributes = new HashMap<String, Object>();
    protected boolean active = true;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public HotdeployHttpSession(final HttpSession originalSession) {
        this(null, originalSession);
    }

    public HotdeployHttpSession(final HotdeployHttpServletRequest request, final HttpSession originalSession) {
        this.request = request;
        this.originalSession = originalSession;
    }

    // ===================================================================================
    //                                                                               Flush
    //                                                                               =====
    public void flush() {
        if (active) {
            for (Iterator<Entry<String, Object>> it = attributes.entrySet().iterator(); it.hasNext();) {
                final Entry<String, Object> entry = (Entry<String, Object>) it.next();
                final String key = (String) entry.getKey();
                try {
                    originalSession.setAttribute(key, new SerializedObjectHolder(key, entry.getValue()));
                } catch (final IllegalStateException e) {
                    return;
                } catch (final Exception e) {
                    logger.info("Failed to set session attribute as HotDeploy: " + key, e);
                }
            }
        }
    }

    // ===================================================================================
    //                                                                  Attribute Handling
    //                                                                  ==================
    public Object getAttribute(final String name) {
        assertActive();
        if (attributes.containsKey(name)) {
            return attributes.get(name);
        }
        Object value = originalSession.getAttribute(name);
        if (value instanceof SerializedObjectHolder) {
            value = ((SerializedObjectHolder) value).getDeserializedObject();
            if (value != null) {
                attributes.put(name, value);
            } else {
                originalSession.removeAttribute(name);
            }
        }
        return value;
    }

    public void setAttribute(final String name, final Object value) {
        assertActive();
        if (value == null) {
            originalSession.setAttribute(name, value);
            return;
        }
        if (!(value instanceof Serializable)) {
            throw new SessionObjectNotSerializableRuntimeException(value.getClass());
        }
        attributes.put(name, value);
        originalSession.setAttribute(name, value);
    }

    public void removeAttribute(final String name) {
        attributes.remove(name);
        originalSession.removeAttribute(name);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Enumeration getAttributeNames() {
        return originalSession.getAttributeNames();
    }

    // ===================================================================================
    //                                                                           Delegator
    //                                                                           =========
    public long getCreationTime() {
        return originalSession.getCreationTime();
    }

    public String getId() {
        return originalSession.getId();
    }

    public long getLastAccessedTime() {
        return originalSession.getLastAccessedTime();
    }

    public int getMaxInactiveInterval() {
        return originalSession.getMaxInactiveInterval();
    }

    public ServletContext getServletContext() {
        return originalSession.getServletContext();
    }

    public HttpSessionContext getSessionContext() {
        return originalSession.getSessionContext();
    }

    public Object getValue(final String name) {
        return getAttribute(name);
    }

    public String[] getValueNames() {
        return originalSession.getValueNames();
    }

    public void invalidate() {
        originalSession.invalidate();
        if (request != null) {
            request.invalidateSession();
        }
        active = false;
    }

    public boolean isNew() {
        return originalSession.isNew();
    }

    public void putValue(final String name, final Object value) {
        setAttribute(name, value);
    }

    public void removeValue(final String name) {
        removeAttribute(name);
    }

    public void setMaxInactiveInterval(final int interval) {
        originalSession.setMaxInactiveInterval(interval);
    }

    // ===================================================================================
    //                                                                   Serialized Object
    //                                                                   =================
    public static class SerializedObjectHolder implements Serializable {

        private static final long serialVersionUID = 1L;

        protected final Object key;
        protected final byte[] bytes;

        public SerializedObjectHolder(Object key, Object sessionObject) {
            this.key = key;
            try {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(sessionObject);
                oos.close();
                bytes = baos.toByteArray();
            } catch (NotSerializableException e) {
                throw new IllegalStateException("Not serializable: " + sessionObject, e);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to serialize: " + sessionObject, e);
            }
        }

        public Object getDeserializedObject() {
            try {
                return HotdeployUtil.deserializeInternal(bytes);
            } catch (Exception e) {
                logger.info("Failed to get deserialized object as HotDeploy: {}" + key, e);
                return null;
            }
        }

        @Override
        public String toString() {
            return "SerializedObjectHolder:{" + key + ", bytes=[" + bytes.length + "]}@" + Integer.toHexString(hashCode());
        }
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected void assertActive() {
        if (!active) {
            throw new IllegalStateException("session invalidated");
        }
    }
}
