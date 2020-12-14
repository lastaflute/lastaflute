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
package org.lastaflute.web.token;

/**
 * @author jflute
 * @since 0.6.5 (2015/10/23 Friday)
 */
public enum TxToken {

    SAVE(true), VALIDATE(true), VALIDATE_KEEP(true), NONE(false);

    private final boolean process;

    private TxToken(boolean process) {
        this.process = process;
    }

    public boolean needsProcess() {
        return process;
    }
}
