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
package org.lastaflute.web.ruts.multipart.exception;

/**
 * @author jflute
 */
public class MultipartExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected final long actual;
    protected final long permitted;

    public MultipartExceededException(String msg, long actual, long permitted, Exception cause) {
        super(msg, cause);
        this.actual = actual;
        this.permitted = permitted;
    }

    public long getActual() {
        return actual;
    }

    public long getPermitted() {
        return permitted;
    }
}