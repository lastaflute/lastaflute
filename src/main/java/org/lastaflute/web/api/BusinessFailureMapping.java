/*
 * Copyright 2015-2018 the original author or authors.
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
package org.lastaflute.web.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 * @param <VALUE> The type of value corresponding to failure type.
 * @since 0.7.6 (2016/01/06 Wednesday)
 */
public class BusinessFailureMapping<VALUE> {

    protected final Map<Class<?>, VALUE> failureMap;

    /**
     * @param oneArgLambda The callback to initialize failure map keyed by type. (NotNull)
     */
    public BusinessFailureMapping(Consumer<Map<Class<?>, VALUE>> oneArgLambda) {
        final Map<Class<?>, VALUE> failureMap = new HashMap<Class<?>, VALUE>();
        oneArgLambda.accept(failureMap);
        this.failureMap = Collections.unmodifiableMap(failureMap);
    }

    /**
     * @param cause The cause exception to find value. (NotNull)
     * @return The optional value corresponding to failure type. (NotNull, EmptyAllowed)
     */
    public OptionalThing<VALUE> findAssignable(RuntimeException cause) {
        final Class<?> key = cause.getClass();
        final VALUE found = failureMap.get(key);
        if (found != null) {
            return OptionalThing.of(found);
        } else {
            return OptionalThing.migratedFrom(failureMap.entrySet().stream().filter(entry -> {
                return entry.getKey().isAssignableFrom(key);
            }).map(entry -> entry.getValue()).findFirst(), () -> {
                throw new IllegalStateException("Not found the exception type in the map: " + failureMap.keySet());
            });
        }
    }
}
