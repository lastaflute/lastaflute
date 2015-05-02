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
package org.lastaflute.web.util;

import javax.servlet.http.HttpServletRequest;

import org.lastaflute.core.util.ContainerUtil;

/**
 * @author modified by jflute (originated in Seasar)
 */
public final class LaRequestUtil {

    private LaRequestUtil() {
    }

    /**
     * @return The request of servlet. (NotNull)
     * @throws IllegalStateException When the request is not found.
     */
    public static HttpServletRequest getRequest() {
        final HttpServletRequest request = (HttpServletRequest) ContainerUtil.retrieveExternalContext().getRequest();
        if (request == null) {
            throw new IllegalStateException("Not found the servlet request, not web scope now?");
        }
        return request;
    }
}