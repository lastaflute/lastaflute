/*
 * Copyright 2015-2017 the original author or authors.
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
package org.lastaflute.core.json.bind;

import java.util.Collection;
import java.util.function.Function;

/**
 * @author jflute
 * @since 0.8.5 (2016/10/23 Sunday) 
 */
public class JsonYourCollectionResource {

    protected final Class<?> yourType;
    protected final Function<Collection<? extends Object>, Iterable<? extends Object>> yourCollectionCreator;

    public JsonYourCollectionResource(Class<?> yourType,
            Function<Collection<? extends Object>, Iterable<? extends Object>> yourCollectionCreator) {
        this.yourType = yourType;
        this.yourCollectionCreator = yourCollectionCreator;
    }

    @Override
    public String toString() {
        return "yourCollection:{" + yourType.getName() + "}";
    }

    public Class<?> getYourType() {
        return yourType;
    }

    public Function<Collection<? extends Object>, Iterable<? extends Object>> getYourCollectionCreator() {
        return yourCollectionCreator;
    }
}
