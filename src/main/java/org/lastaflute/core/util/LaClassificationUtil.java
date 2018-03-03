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
package org.lastaflute.core.util;

import java.lang.reflect.Method;
import java.util.Optional;

import org.dbflute.jdbc.Classification;
import org.dbflute.jdbc.ClassificationMeta;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfReflectionUtil;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.exception.BeanMethodNotFoundException;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;

/**
 * @author jflute
 */
public class LaClassificationUtil {

    // ===================================================================================
    //                                                                       Determination
    //                                                                       =============
    public static boolean isCls(Class<?> tp) {
        return Classification.class.isAssignableFrom(tp);
    }

    // ===================================================================================
    //                                                                   to Classification
    //                                                                   =================
    /**
     * @param cdefType The classification type of the code. (NotNull)
     * @param code The code to find the classification. (NullAllowed: if null or empty, returns null)
     * @return The classification by the code. (NullAllowed: when the code is null)
     * @throws ClassificationUnknownCodeException When the code is not found in the classification.
     */
    public static Classification toCls(Class<?> cdefType, Object code) throws ClassificationUnknownCodeException {
        assertArgumentNotNull("cdefType", cdefType);
        if (code == null || (code instanceof String && ((String) code).isEmpty())) {
            return null;
        }
        final Object result = nativeCodeOf(cdefType, code); // use native for performance
        if (result == null) { // means not found
            handleClassificationUnknownCode(cdefType, code);
        }
        if (result instanceof OptionalThing<?>) { // handling for future
            @SuppressWarnings("unchecked")
            final OptionalThing<Classification> cast = (OptionalThing<Classification>) result;
            try {
                return cast.get();
            } catch (RuntimeException e) {
                handleClassificationUnknownCode(cdefType, code, e);
                return null; // unreachable
            }
        } else if (result instanceof Optional<?>) {
            @SuppressWarnings("unchecked")
            final Optional<Classification> cast = (Optional<Classification>) result;
            try {
                return cast.get();
            } catch (RuntimeException e) {
                handleClassificationUnknownCode(cdefType, code, e);
                return null; // unreachable
            }
        } else {
            return (Classification) result;
        }
    }

    protected static void handleClassificationUnknownCode(Class<?> cdefType, Object code) throws ClassificationUnknownCodeException {
        handleClassificationUnknownCode(cdefType, code, null);
    }

    protected static void handleClassificationUnknownCode(Class<?> cdefType, Object code, RuntimeException cause)
            throws ClassificationUnknownCodeException {
        final String msg = "Unknown the classification code for " + cdefType.getName() + ": code=" + code;
        if (cause != null) {
            throw new ClassificationUnknownCodeException(msg, cause);
        } else {
            throw new ClassificationUnknownCodeException(msg);
        }
    }

    public static class ClassificationUnknownCodeException extends Exception { // checked

        private static final long serialVersionUID = 1L;

        public ClassificationUnknownCodeException(String msg) {
            super(msg);
        }

        public ClassificationUnknownCodeException(String msg, RuntimeException e) {
            super(msg, e);
        }
    }

    // ===================================================================================
    //                                                                        Find by Code
    //                                                                        ============
    public static OptionalThing<Classification> findByCode(Class<?> cdefType, Object code) {
        assertArgumentNotNull("cdefType", cdefType);
        assertArgumentNotNull("code", code);

        // #hope switch to nativeFindByCode() in future (can be over DBFlute-1.1.1) by jflute
        final Object result = nativeCodeOf(cdefType, code);
        if (result == null) {
            return OptionalThing.empty();
        }
        if (result instanceof OptionalThing<?>) { // handling for future
            @SuppressWarnings("unchecked")
            final OptionalThing<Classification> cast = (OptionalThing<Classification>) result;
            return cast;
        } else if (result instanceof Optional<?>) {
            @SuppressWarnings("unchecked")
            final Optional<Classification> cast = (Optional<Classification>) result;
            return OptionalThing.ofNullable(cast.orElse(null), () -> {
                throw new IllegalStateException("Not found the classification code: " + cdefType.getName() + "@" + code);
            });
        } else {
            return OptionalThing.of((Classification) result);
        }
    }

    // ===================================================================================
    //                                                                           Find Meta
    //                                                                           =========
    public static OptionalThing<ClassificationMeta> findMeta(Class<?> defmetaType, String classificationName) {
        assertArgumentNotNull("defmetaType", defmetaType);
        assertArgumentNotNull("classificationName", classificationName);

        // #hope switch to nativeFindMeta() in future (can be over DBFlute-1.1.1) by jflute
        final Object result;
        try {
            result = nativeMetaOf(defmetaType, classificationName);
        } catch (RuntimeException ignored) { // means not found, not use conrete type because might be changed later 
            return OptionalThing.empty();
        }
        if (result == null) {
            return OptionalThing.empty();
        }
        if (result instanceof OptionalThing<?>) { // handling for future
            @SuppressWarnings("unchecked")
            final OptionalThing<ClassificationMeta> cast = (OptionalThing<ClassificationMeta>) result;
            return cast;
        } else if (result instanceof Optional<?>) {
            @SuppressWarnings("unchecked")
            final Optional<ClassificationMeta> cast = (Optional<ClassificationMeta>) result;
            return OptionalThing.ofNullable(cast.orElse(null), () -> {
                throw new IllegalStateException("Not found the classification code: " + defmetaType.getName() + "@" + classificationName);
            });
        } else {
            return OptionalThing.of((ClassificationMeta) result);
        }
    }

    // ===================================================================================
    //                                                                       Native Method
    //                                                                       =============
    // -----------------------------------------------------
    //                                          cls.of(code)
    //                                          ------------
    protected static OptionalThing<Classification> nativeFindByCode(Class<?> cdefType, Object code) {
        assertArgumentNotNull("cdefType", cdefType);
        assertArgumentNotNull("code", code);
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(cdefType);
        final String methodName = "of";
        final Method method;
        try {
            method = beanDesc.getMethod(methodName, new Class<?>[] { Object.class });
        } catch (BeanMethodNotFoundException e) {
            String msg = "Failed to get the method " + methodName + "() of the classification type: " + cdefType;
            throw new ClassificationFindByCodeMethodNotFoundException(msg, e);
        }
        @SuppressWarnings("unchecked")
        final OptionalThing<Classification> opt =
                (OptionalThing<Classification>) DfReflectionUtil.invokeStatic(method, new Object[] { code });
        return opt;
    }

    public static class ClassificationFindByCodeMethodNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public ClassificationFindByCodeMethodNotFoundException(String msg, BeanMethodNotFoundException e) {
            super(msg, e);
        }
    }

    // -----------------------------------------------------
    //                                       meta.find(name)
    //                                       ---------------
    protected static OptionalThing<ClassificationMeta> nativeFindMeta(Class<?> defmetaType, String classificationName) { // old method
        assertArgumentNotNull("defmetaType", defmetaType);
        assertArgumentNotNull("classificationName", classificationName);
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(defmetaType);
        final String methodName = "find";
        final Method method;
        try {
            method = beanDesc.getMethod(methodName, new Class<?>[] { String.class });
        } catch (BeanMethodNotFoundException e) {
            String msg = "Failed to get the method " + methodName + "() of the def-meta type: " + defmetaType;
            throw new ClassificationMetaFindMethodNotFoundException(msg, e);
        }
        @SuppressWarnings("unchecked")
        OptionalThing<ClassificationMeta> opt =
                (OptionalThing<ClassificationMeta>) DfReflectionUtil.invokeStatic(method, new Object[] { classificationName });
        return opt;
    }

    public static class ClassificationMetaFindMethodNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public ClassificationMetaFindMethodNotFoundException(String msg, BeanMethodNotFoundException e) {
            super(msg, e);
        }
    }

    // -----------------------------------------------------
    //                                               Code-of
    //                                               -------
    public static Object nativeCodeOf(Class<?> cdefType, Object code) { // old method
        assertArgumentNotNull("cdefType", cdefType);
        assertArgumentNotNull("code", code);
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(cdefType);
        final String methodName = "codeOf";
        final Method method;
        try {
            method = beanDesc.getMethod(methodName, new Class<?>[] { Object.class });
        } catch (BeanMethodNotFoundException e) {
            String msg = "Failed to get the method " + methodName + "() of the classification type: " + cdefType;
            throw new ClassificationCodeOfMethodNotFoundException(msg, e);
        }
        return DfReflectionUtil.invokeStatic(method, new Object[] { code });
    }

    public static class ClassificationCodeOfMethodNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public ClassificationCodeOfMethodNotFoundException(String msg, BeanMethodNotFoundException e) {
            super(msg, e);
        }
    }

    // -----------------------------------------------------
    //                                               Meta of
    //                                               -------
    public static Object nativeMetaOf(Class<?> defmetaType, String classificationName) { // old method
        assertArgumentNotNull("defmetaType", defmetaType);
        assertArgumentNotNull("classificationName", classificationName);
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(defmetaType);
        final String methodName = "meta";
        final Method method;
        try {
            method = beanDesc.getMethod(methodName, new Class<?>[] { String.class });
        } catch (BeanMethodNotFoundException e) {
            String msg = "Failed to get the method " + methodName + "() of the def-meta type: " + defmetaType;
            throw new ClassificationMetaOfMethodNotFoundException(msg, e);
        }
        return DfReflectionUtil.invokeStatic(method, new Object[] { classificationName });
    }

    public static class ClassificationMetaOfMethodNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public ClassificationMetaOfMethodNotFoundException(String msg, BeanMethodNotFoundException e) {
            super(msg, e);
        }
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
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
