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
package org.lastaflute.web.validation.theme.conversion;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author jflute
 * @since 0.6.5 (2015/11/04 Wednesday)
 */
public class TypeFailureBean {

    protected final Map<String, TypeFailureElement> elementMap = new LinkedHashMap<String, TypeFailureElement>();

    public void register(TypeFailureElement element) {
        if (element == null) {
            throw new IllegalArgumentException("The argument 'element' should not be null.");
        }
        elementMap.put(element.getPropertyPath(), element);
    }

    public boolean hasFailure() {
        return !elementMap.isEmpty();
    }

    @Override
    public String toString() {
        return "typeFailure:{" + elementMap + "}@" + Integer.toHexString(hashCode());
    }

    public Map<String, TypeFailureElement> getElementMap() {
        return Collections.unmodifiableMap(elementMap);
    }
}
