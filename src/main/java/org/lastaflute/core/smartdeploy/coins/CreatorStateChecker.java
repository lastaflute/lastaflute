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
package org.lastaflute.core.smartdeploy.coins;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.di.core.ComponentDef;
import org.lastaflute.web.LastaAction;
import org.lastaflute.web.servlet.cookie.CookieManager;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.request.ResponseManager;
import org.lastaflute.web.servlet.session.SessionManager;

/**
 * @author jflute
 * @since 1.1.8 (2020/07/02 Thursday at rjs)
 */
public class CreatorStateChecker {

    // ===================================================================================
    //                                                                      Extends Action
    //                                                                      ==============
    public void checkExtendsAction(ComponentDef componentDef, String title, Function<String, RuntimeException> exceptionProvider) {
        final Class<?> componentType = componentDef.getComponentClass();
        if (LastaAction.class.isAssignableFrom(componentType)) {
            throwNonActionExtendsActionException(componentType, title, exceptionProvider);
        }
    }

    protected void throwNonActionExtendsActionException(Class<?> componentType, String title,
            Function<String, RuntimeException> exceptionProvider) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("No way, the component extends action.");
        br.addItem("Advice");
        br.addElement(title + " is not Action,");
        br.addElement("so the component cannot extend action.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public class Sea" + title + " extends MaihamaBaseAction { // *Bad");
        br.addElement("       ...");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public class Sea" + title + " { // Good");
        br.addElement("       ...");
        br.addElement("    }");
        br.addItem(title);
        br.addElement(componentType);
        br.addItem("Super Class");
        br.addElement(componentType.getSuperclass());
        final String msg = br.buildExceptionMessage();
        throw exceptionProvider.apply(msg);
    }

    // ===================================================================================
    //                                                                       Web Reference
    //                                                                       =============
    public void checkWebReference(ComponentDef componentDef, List<String> webPackagePrefixList, String title,
            Function<String, RuntimeException> exceptionProvider) {
        final Class<?> componentType = componentDef.getComponentClass();
        final List<Field> fieldList = getWholeFieldList(componentType);
        for (Field field : fieldList) {
            final Class<?> fieldType = field.getType();
            if (isWebResource(fieldType, webPackagePrefixList)) {
                throwNonWebComponentWebReferenceException(componentType, field, title, exceptionProvider);
            }
        }
        final List<Method> methodList = getWholeMethodList(componentType);
        for (Method method : methodList) {
            final Class<?>[] parameterTypes = method.getParameterTypes();
            for (Class<?> parameterType : parameterTypes) {
                if (isWebResource(parameterType, webPackagePrefixList)) {
                    throwNonWebComponentWebReferenceException(componentType, method, title, exceptionProvider);
                }
            }
            final Class<?> returnType = method.getReturnType();
            if (isWebResource(returnType, webPackagePrefixList)) {
                throwNonWebComponentWebReferenceException(componentType, method, title, exceptionProvider);
            }
        }
    }

    protected boolean isWebResource(Class<?> tp, List<String> webPackagePrefixList) {
        return isAppWeb(tp, webPackagePrefixList) // e.g. app.web.
                || RequestManager.class.isAssignableFrom(tp) // lastaflute request
                || ResponseManager.class.isAssignableFrom(tp) // lastaflute response
                || SessionManager.class.isAssignableFrom(tp) // lastaflute session
                || CookieManager.class.isAssignableFrom(tp) // lastaflute cookie
                || HttpServletRequest.class.isAssignableFrom(tp) // servlet request
                || HttpServletResponse.class.isAssignableFrom(tp) // servlet response
                || HttpSession.class.isAssignableFrom(tp) // servlet session
        ;
    }

    protected boolean isAppWeb(Class<?> tp, List<String> webPackagePrefixList) {
        for (String webPackage : webPackagePrefixList) {
            if (tp.getName().startsWith(webPackage)) {
                return true;
            }
        }
        return false;
    }

    protected void throwNonWebComponentWebReferenceException(Class<?> componentType, Object target, String title,
            Function<String, RuntimeException> exceptionProvider) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Web reference from the component.");
        br.addItem("Advice");
        br.addElement(title + " should not refer web resources,");
        br.addElement(" e.g. classes under 'app.web' package, RequestManager.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public class Sea" + title + " {");
        br.addElement("        @Resource");
        br.addElement("        private RequestManager requestManager; // *Bad");
        br.addElement("");
        br.addElement("        public void land(SeaForm form) { // *Bad");
        br.addElement("            ...");
        br.addElement("        }");
        br.addElement("    }");
        br.addItem(title);
        br.addElement(componentType);
        br.addItem("Web Reference");
        br.addElement(target);
        final String msg = br.buildExceptionMessage();
        throw exceptionProvider.apply(msg);
    }

    // ===================================================================================
    //                                                                      General Helper
    //                                                                      ==============
    protected List<Field> getWholeFieldList(Class<?> clazz) {
        final List<Field> fieldList = new ArrayList<Field>();
        for (Class<?> target = clazz; target != null && target != Object.class; target = target.getSuperclass()) {
            final Field[] fields = target.getDeclaredFields();
            for (Field method : fields) {
                fieldList.add(method);
            }
        }
        return fieldList;
    }

    protected List<Method> getWholeMethodList(Class<?> clazz) {
        final List<Method> methodList = new ArrayList<Method>();
        for (Class<?> target = clazz; target != null && target != Object.class; target = target.getSuperclass()) {
            final Method[] methods = target.getDeclaredMethods();
            for (Method method : methods) {
                methodList.add(method);
            }
        }
        return methodList;
    }
}
