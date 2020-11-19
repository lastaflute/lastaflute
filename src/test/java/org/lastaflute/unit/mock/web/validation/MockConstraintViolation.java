/*
 * Copyright 2015-2020 the original author or authors.
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
package org.lastaflute.unit.mock.web.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintTarget;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Payload;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.ValidateUnwrappedValue;

import org.dbflute.util.DfCollectionUtil;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.lastaflute.web.validation.ClientError;
import org.lastaflute.web.validation.Required;

/**
 * @author jflute
 */
public class MockConstraintViolation implements ConstraintViolation<Object> {

    protected final String propertyPath;

    public MockConstraintViolation(String propertyPath) {
        this.propertyPath = propertyPath;
    }

    @Override
    public String getMessage() {
        return "message for " + propertyPath;
    }

    @Override
    public String getMessageTemplate() {
        return null;
    }

    @Override
    public Object getRootBean() {
        return null;
    }

    @Override
    public Class<Object> getRootBeanClass() {
        return null;
    }

    @Override
    public Object getLeafBean() {
        return null;
    }

    @Override
    public Object[] getExecutableParameters() {
        return null;
    }

    @Override
    public Object getExecutableReturnValue() {
        return null;
    }

    @Override
    public Path getPropertyPath() {
        return PathImpl.createPathFromString(propertyPath);
    }

    @Override
    public Object getInvalidValue() {
        return null;
    }

    @Required
    @Override
    public ConstraintDescriptor<?> getConstraintDescriptor() {
        String methodName = "getConstraintDescriptor";
        Method method;
        try {
            method = MockConstraintViolation.class.getMethod(methodName, new Class<?>[] {});
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException("Failed to get the method: " + methodName, e);
        }
        Required annotation = method.getAnnotation(Required.class);
        return new ConstraintDescriptor<Annotation>() {

            @Override
            public Annotation getAnnotation() {
                return annotation;
            }

            @Override
            public String getMessageTemplate() {
                return null;
            }

            @Override
            public Set<Class<?>> getGroups() {
                return DfCollectionUtil.newHashSet(ClientError.class);
            }

            @Override
            public Set<Class<? extends Payload>> getPayload() {
                return null;
            }

            @Override
            public ConstraintTarget getValidationAppliesTo() {
                return null;
            }

            @Override
            public List<Class<? extends ConstraintValidator<Annotation, ?>>> getConstraintValidatorClasses() {
                return null;
            }

            @Override
            public Map<String, Object> getAttributes() {
                return null;
            }

            @Override
            public Set<ConstraintDescriptor<?>> getComposingConstraints() {
                return null;
            }

            @Override
            public boolean isReportAsSingleViolation() {
                return false;
            }

            @Override
            public ValidateUnwrappedValue getValueUnwrapping() {
                return null;
            }

            @Override
            public <U> U unwrap(Class<U> type) {
                return null;
            }
        };
    }

    @Override
    public <U> U unwrap(Class<U> type) {
        return null;
    }

    @Override
    public String toString() {
        return "vio:{" + propertyPath + "}";
    }
}
