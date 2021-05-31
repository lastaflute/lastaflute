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
package org.lastaflute.web.token;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 */
public class DoubleSubmitTokenMap implements Serializable { // to avoid map generic headache

    private static final long serialVersionUID = 1L;

    protected final Map<Class<?>, String> tokenMap = new ConcurrentHashMap<>(4);

    public OptionalThing<String> get(Class<?> groupType) {
        return OptionalThing.ofNullable(tokenMap.get(groupType), () -> {
            String msg = "Not found the token by the group type: " + groupType + " tokenMap=" + tokenMap.keySet();
            throw new IllegalStateException(msg);
        });
    }

    public void put(Class<?> groupType, String token) {
        tokenMap.put(groupType, token);
    }

    public String remove(Class<?> groupType) {
        return tokenMap.remove(groupType);
    }

    public boolean isEmpty() {
        return tokenMap.isEmpty();
    }

    @Override
    public String toString() {
        return tokenMap.toString();
    }
}
