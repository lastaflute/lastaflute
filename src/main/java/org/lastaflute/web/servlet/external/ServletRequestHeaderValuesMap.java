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
package org.lastaflute.web.servlet.external;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.lastaflute.di.core.external.AbstractUnmodifiableExternalContextMap;
import org.lastaflute.di.util.EnumerationIterator;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class ServletRequestHeaderValuesMap extends AbstractUnmodifiableExternalContextMap {

    protected static final String[] EMPTY_STRING_ARRAY = new String[0];

    protected final HttpServletRequest request;

    public ServletRequestHeaderValuesMap(HttpServletRequest request) {
        this.request = request;
    }

    protected Object getAttribute(String key) {
        return toStringArray(request.getHeaders(key));
    }

    protected Iterator<String> getAttributeNames() {
        return new EnumerationIterator<String>(request.getHeaderNames());
    }

    private String[] toStringArray(final Enumeration<String> em) {
        if (em == null) {
            return EMPTY_STRING_ARRAY;
        }
        final List<String> list = new ArrayList<String>();
        while (em.hasMoreElements()) {
            list.add(em.nextElement());
        }
        return (String[]) list.toArray(new String[list.size()]);
    }
}
