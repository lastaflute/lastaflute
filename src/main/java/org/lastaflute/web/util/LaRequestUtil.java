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
package org.lastaflute.web.util;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.di.core.ExternalContext;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author modified by jflute (originated in Seasar)
 */
public final class LaRequestUtil {

    private LaRequestUtil() {
    }

    /**
     * @return The request of servlet. (NotNull)
     * @throws IllegalStateException When the request (or external context) is not found.
     */
    public static HttpServletRequest getRequest() {
        return getOptionalRequest().get();
    }

    /**
     * @return The optional request of servlet. (NotNull, EmptyAllowed: when out of scope for external context)
     */
    public static OptionalThing<HttpServletRequest> getOptionalRequest() {
        final HttpServletRequest request;
        if (ContainerUtil.hasExternalContext()) {
            final ExternalContext externalContext = ContainerUtil.retrieveExternalContext(); // not null
            request = (HttpServletRequest) externalContext.getRequest(); // null allowed, request not found
        } else {
            request = null; // external context not found
        }
        return toRequestOptional(request);
    }

    private static OptionalThing<HttpServletRequest> toRequestOptional(HttpServletRequest request) {
        return OptionalThing.ofNullable(request, () -> {
            throw new IllegalStateException("Not found the servlet request, not web scope now?");
        });
    }
}