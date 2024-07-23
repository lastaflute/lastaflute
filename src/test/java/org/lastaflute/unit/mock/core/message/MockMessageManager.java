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
package org.lastaflute.unit.mock.core.message;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.core.message.resources.MessageResourcesGateway;

/**
 * @author jflute at land
 */
public class MockMessageManager implements MessageManager {

    @Override
    public String getMessage(Locale locale, String key) {
        return null;
    }

    @Override
    public String getMessage(Locale locale, String key, Object... values) {
        return null;
    }

    @Override
    public OptionalThing<String> findMessage(Locale locale, String key) {
        return null;
    }

    @Override
    public OptionalThing<String> findMessage(Locale locale, String key, Object[] values) {
        return null;
    }

    @Override
    public List<String> toMessageList(Locale locale, UserMessages messages) {
        return null;
    }

    @Override
    public Map<String, List<String>> toPropertyMessageMap(Locale locale, UserMessages messages) {
        return null;
    }

    @Override
    public MessageResourcesGateway getMessageResourceGateway() {
        return null;
    }
}
