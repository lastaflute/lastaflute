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
package org.lastaflute.web.ruts.config.specifed;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.web.Execute;

/**
 * @author jflute
 * @since 1.2.1 (2021/05/22 Saturday at roppongi japanese)
 */
public class SpecifiedHttpStatus {

    protected final int statusValue; // not minus here
    protected final String description; // empty allowed after all, because of only for document

    // simple factory is enough here
    public static OptionalThing<SpecifiedHttpStatus> create(Execute.HttpStatus annotatedStatus) {
        final int plainValue = annotatedStatus.value();
        final String plainDesc = annotatedStatus.desc();
        final boolean validValue = plainValue >= 0;
        final SpecifiedHttpStatus result = validValue ? new SpecifiedHttpStatus(plainValue, plainDesc) : null;
        return OptionalThing.ofNullable(result, () -> {
            throw new IllegalStateException("Not found the specified HTTP status.");
        });
    }

    protected SpecifiedHttpStatus(int httpStatus, String description) {
        this.statusValue = httpStatus;
        this.description = description;
    }

    public int getStatusValue() {
        return statusValue;
    }

    public String getDescription() {
        return description;
    }
}
