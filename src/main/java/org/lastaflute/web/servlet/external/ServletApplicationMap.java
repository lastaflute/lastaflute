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

import javax.servlet.ServletContext;

import org.lastaflute.di.core.external.RebuildableExternalContextMap;
import org.lastaflute.di.util.EnumerationIterator;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class ServletApplicationMap extends RebuildableExternalContextMap {

    private final ServletContext context;

    public ServletApplicationMap(ServletContext context) {
        this.context = context;
    }

    protected Object getAttribute(String key) {
        return context.getAttribute(key);
    }

    protected Iterator<String> getAttributeNames() {
        return new EnumerationIterator<String>(context.getAttributeNames());
    }

    protected void removeAttribute(String key) {
        context.removeAttribute(key);
    }

    protected void setAttribute(String key, Object value) {
        context.setAttribute(key, value);
    }
}