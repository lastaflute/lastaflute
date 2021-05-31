/*
 * Copyright 2015-2021 the original author or authors.
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
import java.util.Set;

import javax.servlet.ServletRequest;

import org.lastaflute.di.core.external.AbstractUnmodifiableExternalContextMap;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class ServletRequestParameterValuesMap extends AbstractUnmodifiableExternalContextMap {

    private final ServletRequest request;
    private final Set<String> parameterNames;

    public ServletRequestParameterValuesMap(ServletRequest request) {
        this.request = request;
        parameterNames = request.getParameterMap().keySet();
    }

    protected Object getAttribute(final String key) {
        if (parameterNames.contains(key)) {
            return request.getParameterValues(key);
        }
        return null;
    }

    protected Iterator<String> getAttributeNames() {
        return parameterNames.iterator();
    }
}
