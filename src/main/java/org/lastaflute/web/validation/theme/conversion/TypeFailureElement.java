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
package org.lastaflute.web.validation.theme.conversion;

/**
 * @author jflute
 * @since 0.6.5 (2015/11/04 Wednesday)
 */
public class TypeFailureElement {

    // all not null
    protected final String propertyPath;
    protected final Class<?> propertyType;
    protected final Object failureValue;
    protected final ValidateTypeFailure annotation;
    protected final RuntimeException cause;
    protected final TypeFailureBadRequestThrower badRequestThrower;

    public TypeFailureElement(String propertyPath, Class<?> propertyType, Object failureValue, ValidateTypeFailure annotation,
            RuntimeException cause, TypeFailureBadRequestThrower badRequestThrower) {
        this.propertyPath = propertyPath;
        this.propertyType = propertyType;
        this.failureValue = failureValue;
        this.annotation = annotation;
        this.cause = cause;
        this.badRequestThrower = badRequestThrower;
    }

    @FunctionalInterface
    public static interface TypeFailureBadRequestThrower {
        void throwBadRequest();
    }

    @Override
    public String toString() {
        String causeExp = cause != null ? cause.getClass().getSimpleName() : null; // just in case
        return "failureElement:{" + propertyPath + ", " + failureValue + ", " + annotation + ", " + causeExp + "}";
    }

    public String getPropertyPath() {
        return propertyPath;
    }

    public Class<?> getPropertyType() {
        return propertyType;
    }

    public Object getFailureValue() {
        return failureValue;
    }

    public ValidateTypeFailure getAnnotation() {
        return annotation;
    }

    public RuntimeException getCause() {
        return cause;
    }

    public TypeFailureBadRequestThrower getBadRequestThrower() {
        return badRequestThrower;
    }
}
