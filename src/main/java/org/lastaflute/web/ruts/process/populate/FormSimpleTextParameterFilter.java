/*
 * Copyright 2015-2019 the original author or authors.
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
package org.lastaflute.web.ruts.process.populate;

/**
 * The callback for filtering of simple text parameter. <br>
 * (contains array but not JSON)
 * @author jflute
 * @since 0.7.0 (2015/12/05 Saturday)
 */
@FunctionalInterface
public interface FormSimpleTextParameterFilter {

    /**
     * Filter the simple text parameter. (contains array but not JSON) <br>
     * @param parameter The parameter as string. (NotNull: not called if null parameter)
     * @param meta The meta data of simple parameter. (NotNull)
     * @return The filtered parameter or plain parameter. (NullAllowed: then filtered as null value)
     */
    String filter(String parameter, FormSimpleTextParameterMeta meta);
}
