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

import java.util.function.Function;

/**
 * @author jflute
 * @since 1.0.2 (2017/10/30 Monday at showbase)
 */
public class JsonYourScalarResource {

    protected final Class<?> yourType;
    protected final Function<String, ?> reader; // not null
    protected final Function<?, String> writer; // not null

    // use generics for user's type-safe
    // e.g.
    //  new JsonYourScalarResource(YearMonth.class, exp -> ..., value -> ...);
    public <SCALAR> JsonYourScalarResource(Class<SCALAR> yourType, Function<String, SCALAR> reader, Function<SCALAR, String> writer) {
        this.yourType = yourType;
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public String toString() {
        return "yourScalar:{" + yourType.getName() + "}";
    }

    public Class<?> getYourType() {
        return yourType;
    }

    public Function<String, ?> getReader() {
        return reader;
    }

    public Function<?, String> getWriter() {
        return writer;
    }
}
