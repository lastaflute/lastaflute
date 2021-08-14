/*
 * Copyright 2015-2021 the original author or authors.
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

import org.lastaflute.di.core.customizer.PointTxAttributeCustomizer;
import org.lastaflute.web.Execute;

/**
 * Basically unneeded, use transaction stage.
 * @author jflute
 */
public class ActionTxAttributeCustomizer extends PointTxAttributeCustomizer {

    @Override
    protected boolean isImplicitTxSupportedMethod(Method method) {
        return isActionExecute(method);
    }

    protected boolean isActionExecute(final Method method) {
        return method.getAnnotation(Execute.class) != null;
    }
}
