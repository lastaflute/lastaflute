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
package org.lastaflute.core.json.filter;

/**
 * The callback for filtering of simple text parameter. <br>
 * (contains list elements)
 * @author jflute
 * @since 0.7.1 (2015/12/15 Tuesday)
 */
@FunctionalInterface
public interface JsonSimpleTextReadingFilter {

    /**
     * Filter the simple text parameter. (contains list elements) <br>
     * @param text The text as JSON reading. (NotNull: not called if null parameter)
     * @return The filtered parameter or plain parameter. (NullAllowed: then filtered as null value)
     */
    String filter(String text);
}
