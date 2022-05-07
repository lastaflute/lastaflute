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
package org.lastaflute.web.validation.theme.typed;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.DfTypeUtil.ParseBooleanException;

/**
 * @author jflute
 * @since 0.6.5 (2015/10/31 Saturday)
 */
public abstract class BooleanTypeValidator implements ConstraintValidator<TypeBoolean, String> {

    @Override
    public void initialize(TypeBoolean constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return determineValid(value);
    }

    protected boolean determineValid(String value) {
        if (value != null && !value.isEmpty()) {
            try {
                DfTypeUtil.toBoolean(value);
                return true;
            } catch (ParseBooleanException ignored) {
                return false;
            }
        } else {
            return true;
        }
    }
}
