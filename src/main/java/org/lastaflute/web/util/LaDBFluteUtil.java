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
package org.lastaflute.web.util;

import java.lang.reflect.Method;

import org.dbflute.helper.beans.DfBeanDesc;
import org.dbflute.helper.beans.factory.DfBeanDescFactory;
import org.dbflute.jdbc.Classification;
import org.dbflute.util.DfReflectionUtil;

/**
 * @author jflute
 */
public class LaDBFluteUtil {

    public static boolean isClassificationType(Class<?> tp) {
        return Classification.class.isAssignableFrom(tp);
    }

    public static Classification toVerifiedClassification(Class<?> cdefType, Object code) throws ClassificationConvertFailureException {
        if (code == null || (code instanceof String && ((String) code).isEmpty())) {
            return null;
        }
        final Classification cls = invokeClassificationCodeOf(cdefType, code);
        if (cls == null) {
            final String msg = "Unknow the classification code for " + cdefType.getName() + ": code=" + code;
            throw new ClassificationConvertFailureException(msg);
        }
        return cls;
    }

    public static class ClassificationConvertFailureException extends Exception {

        private static final long serialVersionUID = 1L;

        public ClassificationConvertFailureException(String msg) {
            super(msg);
        }
    }

    public static Classification invokeClassificationCodeOf(Class<?> cdefType, Object code) {
        assertArgumentNotNull("cdefType", cdefType);
        assertArgumentNotNull("code", code);
        if (code == null || (code instanceof String && ((String) code).isEmpty())) {
            return null;
        }
        final DfBeanDesc beanDesc = DfBeanDescFactory.getBeanDesc(cdefType);
        final Method method = beanDesc.getMethod("codeOf", new Class<?>[] { Object.class });
        return (Classification) DfReflectionUtil.invoke(method, null, new Object[] { code });
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
