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
package org.lastaflute.web.servlet.cookie.exception;

/**
 * @author jflute
 */
public class CookieCipherDecryptFailureException extends Exception {
    // ciphered string from user might be invalid (hack string)
    // so check exception to determine whether business logic ignores it or not 

    private static final long serialVersionUID = 1L;

    public CookieCipherDecryptFailureException(String msg) {
        super(msg);
    }

    public CookieCipherDecryptFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
