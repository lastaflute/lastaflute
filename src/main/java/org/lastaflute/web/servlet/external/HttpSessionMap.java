/*
 * Copyright 2015-2024 the original author or authors.
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
package org.lastaflute.web.servlet.external;

import java.util.Iterator;

import org.lastaflute.di.core.external.AbstractExternalContextMap;
import org.lastaflute.di.util.EmptyIterator;
import org.lastaflute.di.util.EnumerationIterator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class HttpSessionMap extends AbstractExternalContextMap {

    private static final Iterator<String> EMPTY_ITERATOR = new EmptyIterator<String>();

    private final HttpServletRequest request;

    public HttpSessionMap(HttpServletRequest request) {
        this.request = request;
    }

    protected Object getAttribute(String key) {
        final HttpSession session = getSession();
        return (session != null) ? session.getAttribute(key) : null;
    }

    protected void setAttribute(String key, Object value) {
        request.getSession(true).setAttribute(key, value);
    }

    protected Iterator<String> getAttributeNames() {
        HttpSession session = getSession();
        return (session != null) ? new EnumerationIterator<String>(session.getAttributeNames()) : EMPTY_ITERATOR;
    }

    protected void removeAttribute(String key) {
        HttpSession session = getSession();
        if (session != null) {
            session.removeAttribute(key);
        }
    }

    private HttpSession getSession() {
        return request.getSession(false);
    }
}
