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
import org.dbflute.util.Srl;

/**
 * @author jflute
 * @since 1.2.1 (2021/05/22 Saturday at roppongi japanese)
 */
public class SpecifiedUrlPattern {

    protected final String urlPattern; // not null, not empty

    // simple factory is enough here
    public static OptionalThing<SpecifiedUrlPattern> create(String plainValue) {
        final boolean validValue = Srl.is_NotNull_and_NotTrimmedEmpty(plainValue);
        final SpecifiedUrlPattern result = validValue ? new SpecifiedUrlPattern(plainValue) : null;
        return OptionalThing.ofNullable(result, () -> {
            throw new IllegalStateException("Not found the specified URL pattern.");
        });
    }

    protected SpecifiedUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    public String getPatternValue() { // not null, not empty
        return urlPattern;
    }
}
