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
package com.google.gson.internal.bind;

import java.lang.reflect.Field;

/**
 * @author jflute
 * @since 0.8.5 (2016/10/21 Friday at showbase)
 */
public class LaJsonFieldingContext {

    protected static final ThreadLocal<Field> _defaultThreadLocal = new ThreadLocal<Field>();

    public static Field getJsonFieldOnThread() {
        return _defaultThreadLocal.get();
    }

    public static void setJsonFieldOnThread(Field field) {
        if (field == null) {
            String msg = "The argument 'field' should not be null.";
            throw new IllegalArgumentException(msg);
        }
        _defaultThreadLocal.set(field);
    }

    public static void clearAccessContextOnThread() {
        _defaultThreadLocal.set(null);
    }
}
