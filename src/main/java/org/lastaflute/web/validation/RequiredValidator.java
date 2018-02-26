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
package org.lastaflute.web.validation;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author jflute
 * @since 0.6.0 (2015/08/08 Saturday at American Water Front)
 */
public class RequiredValidator implements ConstraintValidator<Required, Object> {

    @Override
    public void initialize(Required constraintAnnotation) {
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        return determineValid(value);
    }

    protected boolean determineValid(Object value) {
        if (value instanceof String) {
            return ((String) value).trim().length() > 0; // means not blank
        } else if (value instanceof Collection<?>) { // also contains ImmutableList (concrete class)
            return !((Collection<?>) value).isEmpty();
        } else if (value instanceof Map<?, ?>) {
            return !((Map<?, ?>) value).isEmpty();
        } else if (value instanceof Object[]) {
            return ((Object[]) value).length > 0;
        } else if (value != null && value.getClass().isArray()) { // primitive array
            return Array.getLength(value) > 0;
        } else { // e.g. Number, LocalDate
            return value != null;
        }
    }
}
