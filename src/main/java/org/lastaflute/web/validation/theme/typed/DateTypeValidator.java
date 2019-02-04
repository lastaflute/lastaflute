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
package org.lastaflute.web.validation.theme.typed;

import java.lang.annotation.Annotation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.dbflute.util.DfTypeUtil.ParseDateException;

/**
 * @param <DATE> The type of date annotation.
 * @author jflute
 * @since 0.6.5 (2015/11/05 Thursday)
 */
public abstract class DateTypeValidator<DATE extends Annotation> implements ConstraintValidator<DATE, String> {

    @Override
    public void initialize(DATE constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return determineValid(value);
    }

    protected boolean determineValid(String value) {
        if (value != null && !value.isEmpty()) {
            try {
                dateValueOf(value);
                return true;
            } catch (ParseDateException ignored) {
                return false;
            }
        } else {
            return true;
        }
    }

    protected abstract void dateValueOf(String value) throws ParseDateException;
}
