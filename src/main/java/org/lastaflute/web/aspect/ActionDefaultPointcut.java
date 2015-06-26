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
package org.lastaflute.web.aspect;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.lastaflute.core.interceptor.PublicBasisPointcut;
import org.lastaflute.web.callback.ActionHook;

/**
 * @author jflute
 */
public class ActionDefaultPointcut extends PublicBasisPointcut {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Set<Method> callbackMethodSet;

    static {
        final Set<Method> tmpSet = new HashSet<Method>();
        final Method[] methods = ActionHook.class.getMethods();
        for (Method method : methods) {
            tmpSet.add(method);
        }
        callbackMethodSet = Collections.unmodifiableSet(tmpSet);
    }

    // ===================================================================================
    //                                                                             Applied
    //                                                                             =======
    @Override
    public boolean isApplied(Method method) {
        return isPublicAspectablePointcut(method) && !isCallbackMethodImplementation(method);
    }

    // ===================================================================================
    //                                                                     Callback Method
    //                                                                     ===============
    protected boolean isCallbackMethodImplementation(Method method) {
        if (!ActionHook.class.isAssignableFrom(method.getDeclaringClass())) {
            return false; // not required if statement but for performance
        }
        for (Method callbackMethod : callbackMethodSet) {
            if (isImplementsMethod(method, callbackMethod)) {
                return true;
            }
        }
        return false;
    }

    // ===================================================================================
    //                                                                   Reflection Helper
    //                                                                   =================
    protected boolean isImplementsMethod(Method targetMethod, Method interfaceMethod) {
        return isAssignableFromDeclaringClass(targetMethod, interfaceMethod) && equalsIgnoreDeclaringClass(targetMethod, interfaceMethod);
    }

    protected boolean isAssignableFromDeclaringClass(Method targetMethod, Method interfaceMethod) {
        return interfaceMethod.getDeclaringClass().isAssignableFrom(targetMethod.getDeclaringClass());
    }

    protected boolean equalsIgnoreDeclaringClass(Method targetMethod, Method interfaceMethod) {
        if (!targetMethod.getName().equals(interfaceMethod.getName())) {
            return false;
        }
        final Class<?>[] targetParamTypes = targetMethod.getParameterTypes();
        final Class<?>[] interfaceParamTypes = interfaceMethod.getParameterTypes();
        if (targetParamTypes.length != interfaceParamTypes.length) {
            return false;
        }
        for (int i = 0; i < targetParamTypes.length; i++) {
            if (targetParamTypes[i] != interfaceParamTypes[i]) {
                return false;
            }
        }
        return true;
    }
}
