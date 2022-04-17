/*
 * Copyright 2015-2022 the original author or authors.
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
package org.lastaflute.core.message.resources;

import java.util.Locale;

/**
 * @author jflute
 */
public interface MessageResourcesGateway {

    /**
     * Get the message by the key.
     * @param locale The locale for the message. (NotNull)
     * @param key The key of message. (NotNull)
     * @return The found message, user locale resolved. (NotNull: if not found, throws exception)
     */
    String getMessage(Locale locale, String key);

    /**
     * Get the message by the key.
     * @param locale The locale for the message. (NotNull)
     * @param key The key of message. (NotNull)
     * @param values The array of parameters. (NotNull)
     * @return The found message, user locale resolved. (NotNull: if not found, throws exception)
     */
    String getMessage(Locale locale, String key, Object[] values);
}
