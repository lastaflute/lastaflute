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
package org.lastaflute.web.servlet.filter.accesslog;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author jflute
 * @since 0.6.0 (2015/06/03 Wednesday)
 */
public class AccessLogResource {

    protected final HttpServletRequest request;
    protected final HttpServletResponse response;
    protected final Throwable cause; // null allowed
    protected final long before;

    public AccessLogResource(HttpServletRequest request, HttpServletResponse response, Throwable cause, long before) {
        this.request = request;
        this.response = response;
        this.cause = cause;
        this.before = before;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    /**
     * Get the cause exception of the request.
     * @return The thrown exception in the request. (NullAllowed: when no failure)
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Get the time millisecond before the request.
     * @return The value as millisecond.
     */
    public long getBefore() {
        return before;
    }
}
