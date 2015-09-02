/*
 * Copyright 2014-2015 the original author or authors.
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
package org.lastaflute.core.util;

import java.lang.reflect.Method;

import org.dbflute.jdbc.Classification;
import org.dbflute.util.DfReflectionUtil;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.exception.BeanMethodNotFoundException;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;

/**
 * @author jflute
 */
public class LaDBFluteUtil {

    public static boolean isClassificationType(Class<?> tp) {
        return Classification.class.isAssignableFrom(tp);
    }

    public static Classification toVerifiedClassification(Class<?> cdefType, Object code) throws ClassificationUnknownCodeException {
        if (code == null || (code instanceof String && ((String) code).isEmpty())) {
            return null;
        }
        final Classification cls = invokeClassificationCodeOf(cdefType, code);
        if (cls == null) {
            final String msg = "Unknow the classification code for " + cdefType.getName() + ": code=" + code;
            throw new ClassificationUnknownCodeException(msg);
        }
        return cls;
    }

    public static class ClassificationUnknownCodeException extends Exception { // checked

        private static final long serialVersionUID = 1L;

        public ClassificationUnknownCodeException(String msg) {
            super(msg);
        }
    }

    public static Classification invokeClassificationCodeOf(Class<?> cdefType, Object code) {
        assertArgumentNotNull("cdefType", cdefType);
        assertArgumentNotNull("code", code);
        if (code == null || (code instanceof String && ((String) code).isEmpty())) {
            return null;
        }
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(cdefType);
        final String methodName = "codeOf";
        final Method method;
        try {
            method = beanDesc.getMethod(methodName, new Class<?>[] { Object.class });
        } catch (BeanMethodNotFoundException e) {
            String msg = "Failed to get the method " + methodName + "() of the classification type: " + cdefType;
            throw new ClassificationCodeOfMethodNotFoundException(msg, e);
        }
        return (Classification) DfReflectionUtil.invokeStatic(method, new Object[] { code });
    }

    public static class ClassificationCodeOfMethodNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public ClassificationCodeOfMethodNotFoundException(String msg, BeanMethodNotFoundException e) {
            super(msg, e);
        }
    }

    protected static void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            String msg = "The value should not be null: variableName=null value=" + value;
            throw new IllegalArgumentException(msg);
        }
        if (value == null) {
            String msg = "The value should not be null: variableName=" + variableName;
            throw new IllegalArgumentException(msg);
        }
    }
}
