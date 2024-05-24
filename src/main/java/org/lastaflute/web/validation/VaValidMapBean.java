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
package org.lastaflute.web.validation;

import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * @param <KEY> The type of map key.
 * @param <VALUE> The type of map value.
 * @author jflute
 * @since 0.7.6 (2016/01/04 Monday)
 */
public class VaValidMapBean<KEY, VALUE> {

    @NotNull
    @Valid
    protected final Map<KEY, VALUE> map;

    public VaValidMapBean(Map<KEY, VALUE> map) {
        if (map == null) {
            throw new IllegalStateException("The argument 'map' should not be null.");
        }
        this.map = map;
    }

    public Map<KEY, VALUE> getMap() {
        return map;
    }
}