/*
 * Copyright 2015-2024 the original author or authors.
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

import java.lang.reflect.Method;

import org.lastaflute.di.core.aop.Pointcut;
import org.lastaflute.di.util.LdiMethodUtil;
import org.lastaflute.di.util.LdiModifierUtil;

/**
 * @author jflute
 */
public class PublicBasisPointcut implements Pointcut {

    @Override
    public boolean isApplied(Method method) {
        return isPublicAspectablePointcut(method);
    }

    protected boolean isPublicAspectablePointcut(Method method) {
        if (LdiMethodUtil.isSyntheticMethod(method) || LdiMethodUtil.isBridgeMethod(method)) {
            return false;
        }
        if (LdiModifierUtil.isFinal(method)) {
            return false;
        }
        if (Object.class.equals(method.getDeclaringClass())) {
            return false;
        }
        return LdiModifierUtil.isPublic(method);
    }
}
