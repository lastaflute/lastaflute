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
package org.lastaflute.core.interceptor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.lastaflute.di.core.aop.frame.MethodInvocation;
import org.lastaflute.di.core.aop.interceptors.AbstractInterceptor;

/**
 * The base interceptor to control detail adjustments.
 * @author jflute
 */
public abstract class ControllableBaseInterceptor extends AbstractInterceptor {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = 1L;

    // ===================================================================================
    //                                                                              Invoke
    //                                                                              ======
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (isNestedInvoke(invocation)) {
            return invocation.proceed();
        }
        if (isOutOfPointcut(invocation)) {
            return invocation.proceed();
        }
        return doAllowedInvoke(invocation);
    }

    protected abstract boolean isNestedInvoke(MethodInvocation invocation);

    protected boolean isOutOfPointcut(MethodInvocation invocation) {
        if (isOutOfPointcutByBaseAnnotationType(invocation)) {
            return true;
        }
        return isOutOfPointcutByHasAnyAnnotationType(invocation);
    }

    protected boolean isOutOfPointcutByBaseAnnotationType(MethodInvocation invocation) {
        final Class<? extends Annotation> annotationType = getBasePointcutAnnotationType();
        if (annotationType == null) {
            return false;
        }
        return !hasAnnotation(invocation, annotationType);
    }

    protected boolean isOutOfPointcutByHasAnyAnnotationType(MethodInvocation invocation) {
        final List<Class<? extends Annotation>> annoTypeList = getHasAnyPointcutAnnotationTypeList();
        if (annoTypeList == null || annoTypeList.isEmpty()) {
            return false; // no point-cut specified
        }
        return !hasAnyAnnotation(invocation, annoTypeList);
    }

    protected boolean hasAnyAnnotation(MethodInvocation invocation, List<Class<? extends Annotation>> annoTypeList) {
        for (Class<? extends Annotation> annoType : annoTypeList) {
            if (hasAnnotation(invocation, annoType)) {
                return true;
            }
        }
        return false;
    }

    protected abstract Class<? extends Annotation> getBasePointcutAnnotationType();

    protected abstract List<Class<? extends Annotation>> getHasAnyPointcutAnnotationTypeList();

    /**
     * for Generic headache.
     * @param annotations The array of annotation. (NotNull, EmptyAllowed)
     * @return The list of annotation type. (NotNull, EmptyAllowed)
     */
    protected List<Class<? extends Annotation>> createAnnotationTypeList(Class<?>... annotations) {
        final List<Class<? extends Annotation>> annotationList = new ArrayList<Class<? extends Annotation>>();
        for (Class<?> annoType : annotations) {
            @SuppressWarnings("unchecked")
            final Class<? extends Annotation> castType = (Class<? extends Annotation>) annoType;
            annotationList.add(castType);
        }
        return annotationList;
    }

    protected abstract Object doAllowedInvoke(MethodInvocation invocation) throws Throwable;

    // ===================================================================================
    //                                                                Target Determination
    //                                                                ====================
    protected boolean hasAnnotation(MethodInvocation invocation, Class<? extends Annotation> annoType) {
        if (hasAnnotationOnClass(invocation, annoType)) {
            return true;
        }
        if (hasAnnotationOnMethod(invocation, annoType)) {
            return true;
        }
        return false;
    }

    protected boolean hasAnnotationOnClass(MethodInvocation invocation, Class<? extends Annotation> annoType) {
        final Class<?> targetClass = getTargetClass(invocation);
        final Annotation classAnno = targetClass.getAnnotation(annoType);
        return classAnno != null;
    }

    protected boolean hasAnnotationOnMethod(MethodInvocation invocation, Class<? extends Annotation> annoType) {
        final Annotation methodAnno = invocation.getMethod().getAnnotation(annoType);
        return methodAnno != null;
    }

    protected boolean isTargetClass(MethodInvocation invocation, Class<?>... targetTypes) {
        final Object targetObj = invocation.getThis();
        for (Class<?> targetType : targetTypes) {
            if (targetType.isAssignableFrom(targetObj.getClass())) {
                return true;
            }
        }
        return false;
    }
}
