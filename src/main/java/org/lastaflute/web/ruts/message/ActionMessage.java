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
package org.lastaflute.web.ruts.message;

import java.lang.annotation.Annotation;

import org.lastaflute.core.message.UserMessage;

/**
 * @author modified by jflute (originated in Struts)
 */
public class ActionMessage extends UserMessage { // #hope delete after FreeGen adjustment

    private static final long serialVersionUID = 1L;

    public ActionMessage(String key, Object... values) {
        super(key, values);
    }

    protected ActionMessage(String key, Annotation validatorAnnotation, Class<?>[] annotatedGroups) {
        super(key, validatorAnnotation, annotatedGroups);
    }
}
