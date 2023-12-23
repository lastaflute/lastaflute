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
package org.lastaflute.web.servlet.external;

import java.util.Iterator;

import org.lastaflute.di.core.external.AbstractUnmodifiableExternalContextMap;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class CookieMap extends AbstractUnmodifiableExternalContextMap {

    private static final Cookie[] EMPTY_COOKIE = new Cookie[0];

    private final HttpServletRequest request;

    public CookieMap(final HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public boolean containsKey(Object key) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie cookie = cookies[i];
                if (cookie != null && cookie.getName().equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            return false;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie cookie = cookies[i];
                Object cookieValue = cookie.getValue();
                if (value.equals(cookieValue)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        Cookie[] cookies = request.getCookies();
        return cookies == null || cookies.length == 0;
    }

    @Override
    public int size() {
        return getCookies().length;
    }

    protected Object getAttribute(String key) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie cookie = cookies[i];
                if (cookie != null && cookie.getName().equals(key)) {
                    return cookie;
                }
            }
        }
        return null;
    }

    protected Iterator<String> getAttributeNames() {
        final Cookie[] cookies = getCookies();
        return new Iterator<String>() {
            private int index = 0;

            public boolean hasNext() {
                return index < cookies.length;
            }

            public String next() {
                return cookies[index++].getName();
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }

        };
    }

    private Cookie[] getCookies() {
        return (request.getCookies() != null) ? request.getCookies() : EMPTY_COOKIE;
    }
}
