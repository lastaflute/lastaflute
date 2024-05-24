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
package org.lastaflute.core.json.exception;

import org.lastaflute.core.exception.LaSystemException;

/**
 * @author jflute
 */
public class JsonPropertyClassificationCodeOfMethodNotFoundException extends LaSystemException {

    private static final long serialVersionUID = 1L;

    public JsonPropertyClassificationCodeOfMethodNotFoundException(String msg) {
        super(msg);
    }

    public JsonPropertyClassificationCodeOfMethodNotFoundException(String msg, Throwable e) {
        super(msg, e);
    }
}
