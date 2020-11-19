/*
 * Copyright 2015-2020 the original author or authors.
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
package org.lastaflute.core.smartdeploy.exception;

import org.lastaflute.core.exception.LaSystemException;

/**
 * @author jflute
 * @since 0.8.3 (2020/07/01 Wednesday)
 */
public class RepositoryWebReferenceException extends LaSystemException {

    private static final long serialVersionUID = 1L;

    public RepositoryWebReferenceException(String msg) {
        super(msg);
    }

    public RepositoryWebReferenceException(String msg, Throwable e) {
        super(msg, e);
    }
}
