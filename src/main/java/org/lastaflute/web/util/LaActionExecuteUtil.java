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
import java.lang.reflect.Modifier;

import javax.servlet.http.HttpServletRequest;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.config.ActionMapping;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class LaActionExecuteUtil {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final String KEY = LastaWebKey.ACTION_EXECUTE_KEY;

    // ===================================================================================
    //                                                             Access to ExecuteConfig
    //                                                             =======================
    public static boolean hasActionExecute() {
        return doGetActionExecute() != null;
    }

    public static ActionExecute getActionExecute() {
        final ActionExecute method = doGetActionExecute();
        if (method == null) {
            String msg = "Not found the execute config for the request: key=" + KEY;
            throw new IllegalStateException(msg);
        }
        return method;
    }

    protected static ActionExecute doGetActionExecute() {
        return (ActionExecute) LaRequestUtil.getRequest().getAttribute(KEY);
    }

    public static void setActionExecute(ActionExecute execute) {
        LaRequestUtil.getRequest().setAttribute(KEY, execute);
    }

    // ===================================================================================
    //                                                                  Find ActionExecute
    //                                                                  ==================
    public static OptionalThing<ActionExecute> findActionExecute(String actionName, String paramPath) {
        return findActionMapping(actionName).map(mapping -> mapping.findActionExecute(paramPath));
    }

    public static OptionalThing<ActionExecute> findActionExecute(String actionName, HttpServletRequest request) {
        return findActionMapping(actionName).map(mapping -> mapping.findActionExecute(request));
    }

    protected static OptionalThing<ActionMapping> findActionMapping(String actionName) {
        return LaModuleConfigUtil.getModuleConfig().findActionMapping(actionName);
    }

    // ===================================================================================
    //                                                                         Debug Parts
    //                                                                         ===========
    public static String buildSimpleMethodExp(Method executeMethod) {
        final StringBuilder sb = new StringBuilder();
        final int modifiers = executeMethod.getModifiers();
        if (Modifier.isPublic(modifiers)) {
            sb.append("public ");
        } else if (Modifier.isProtected(modifiers)) {
            sb.append("protected ");
        } else if (Modifier.isPrivate(modifiers)) {
            sb.append("private ");
        }
        final Class<?> returnType = executeMethod.getReturnType();
        sb.append(returnType.getSimpleName()).append(" ");
        sb.append(executeMethod.getDeclaringClass().getSimpleName());
        sb.append("@").append(executeMethod.getName()).append("(");
        final Class<?>[] parameterTypes = executeMethod.getParameterTypes();
        int index = 0;
        for (Class<?> parameterType : parameterTypes) {
            if (index > 0) {
                sb.append(", ");
            }
            sb.append(parameterType.getSimpleName());
            ++index;
        }
        sb.append(")");
        return sb.toString();
    }

    // ===================================================================================
    //                                                                  Optional Parameter
    //                                                                  ==================
    public static boolean isOptionalParameterType(Class<?> paramType) {
        return OptionalThing.class.isAssignableFrom(paramType); // only optional thing supported for rich error message
    }
}
