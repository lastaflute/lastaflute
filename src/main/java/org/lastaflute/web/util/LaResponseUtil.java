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
package org.lastaflute.web.util;

import javax.servlet.http.HttpServletResponse;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.di.core.ExternalContext;

/**
 * @author modified by jflute (originated in Seasar)
 */
public final class LaResponseUtil {

    private LaResponseUtil() {
    }

    /**
     * @return The response of servlet. (NotNull)
     * @throws IllegalStateException When the response (or external context) is not found.
     */
    public static HttpServletResponse getResponse() {
        return getOptionalResponse().get();
    }

    /**
     * @return The optional response of servlet. (NotNull, EmptyAllowed: when out of scope for external context)
     */
    public static OptionalThing<HttpServletResponse> getOptionalResponse() {
        final HttpServletResponse response;
        if (ContainerUtil.hasExternalContext()) {
            final ExternalContext externalContext = ContainerUtil.retrieveExternalContext(); // not null
            response = (HttpServletResponse) externalContext.getResponse(); // null allowed, request not found
        } else {
            response = null; // external context not found
        }
        return toRequestOptional(response);
    }

    private static OptionalThing<HttpServletResponse> toRequestOptional(HttpServletResponse response) {
        return OptionalThing.ofNullable(response, () -> {
            throw new IllegalStateException("Not found the servlet resopnse, not web scope now?");
        });
    }
}