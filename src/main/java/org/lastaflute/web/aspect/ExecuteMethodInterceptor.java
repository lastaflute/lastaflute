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
package org.lastaflute.web.aspect;

import java.lang.annotation.Annotation;

import org.lastaflute.core.interceptor.ControllableBaseInterceptor;
import org.lastaflute.web.Execute;

/**
 * The interceptor for execute methods of action to control detail adjustments.
 * @author jflute
 */
public abstract class ExecuteMethodInterceptor extends ControllableBaseInterceptor {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = 1L;

    // ===================================================================================
    //                                                                              Invoke
    //                                                                              ======
    @Override
    protected Class<? extends Annotation> getBasePointcutAnnotationType() {
        return Execute.class;
    }
}
