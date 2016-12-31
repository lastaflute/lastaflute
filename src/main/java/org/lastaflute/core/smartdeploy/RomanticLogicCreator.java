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
package org.lastaflute.core.smartdeploy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.core.smartdeploy.exception.LogicExtendsActionException;
import org.lastaflute.core.smartdeploy.exception.LogicWebReferenceException;
import org.lastaflute.di.core.ComponentDef;
import org.lastaflute.di.core.creator.LogicCreator;
import org.lastaflute.di.naming.NamingConvention;
import org.lastaflute.web.LastaAction;
import org.lastaflute.web.servlet.cookie.CookieManager;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.request.ResponseManager;
import org.lastaflute.web.servlet.session.SessionManager;

/**
 * @author jflute
 */
public class RomanticLogicCreator extends LogicCreator {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final List<String> webPackagePrefixList; // not null, for check, e.g. 'org.docksidestage.app.web.'

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public RomanticLogicCreator(NamingConvention namingConvention) {
        super(namingConvention);
        webPackagePrefixList = deriveWebPackageList(namingConvention);
    }

    protected List<String> deriveWebPackageList(NamingConvention namingConvention) {
        final String[] packageNames = namingConvention.getRootPackageNames();
        return Stream.of(packageNames).map(name -> name + ".web.").collect(Collectors.toList());
    }

    // ===================================================================================
    //                                                                       Component Def
    //                                                                       =============
    @Override
    public ComponentDef createComponentDef(Class<?> componentClass) {
        final ComponentDef componentDef = prepareComponentDef(componentClass);
        if (componentDef == null) {
            return null;
        }
        checkExtendsAction(componentDef);
        checkWebReference(componentDef);
        return componentDef;
    }

    protected ComponentDef prepareComponentDef(Class<?> componentClass) {
        final ComponentDef dispatched = dispatchByEnv(componentClass);
        if (dispatched != null) {
            return dispatched;
        }
        return super.createComponentDef(componentClass); // null allowed
    }

    protected ComponentDef dispatchByEnv(Class<?> componentClass) {
        if (!ComponentEnvDispatcher.canDispatch(componentClass)) { // check before for performance
            return null;
        }
        final ComponentEnvDispatcher envDispatcher = createEnvDispatcher();
        return envDispatcher.dispatch(componentClass);
    }

    protected ComponentEnvDispatcher createEnvDispatcher() {
        return new ComponentEnvDispatcher(getNamingConvention(), getInstanceDef(), getAutoBindingDef(), isExternalBinding(),
                getCustomizer());
    }

    // ===================================================================================
    //                                                                      Extends Action
    //                                                                      ==============
    protected void checkExtendsAction(ComponentDef componentDef) {
        final Class<?> componentType = componentDef.getComponentClass();
        if (LastaAction.class.isAssignableFrom(componentType)) {
            throwLogicExtendsActionException(componentType);
        }
    }

    protected void throwLogicExtendsActionException(Class<?> componentType) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("No way, the logic extends action.");
        br.addItem("Advice");
        br.addElement("Logic is not Action,");
        br.addElement("so the logic cannot extend action.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public class SeaLogic extends MaihamaBaseAction { // *Bad");
        br.addElement("       ...");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public class SeaLogic { // Good");
        br.addElement("       ...");
        br.addElement("    }");
        br.addItem("Logic");
        br.addElement(componentType);
        br.addItem("Super Class");
        br.addElement(componentType.getSuperclass());
        final String msg = br.buildExceptionMessage();
        throw new LogicExtendsActionException(msg);
    }

    // ===================================================================================
    //                                                                       Web Reference
    //                                                                       =============
    protected void checkWebReference(ComponentDef componentDef) {
        final Class<?> componentType = componentDef.getComponentClass();
        final List<Field> fieldList = getWholeFieldList(componentType);
        for (Field field : fieldList) {
            final Class<?> fieldType = field.getType();
            if (isWebResource(fieldType)) {
                throwLogicWebReferenceException(componentType, field);
            }
        }
        final List<Method> methodList = getWholeMethodList(componentType);
        for (Method method : methodList) {
            final Class<?>[] parameterTypes = method.getParameterTypes();
            for (Class<?> parameterType : parameterTypes) {
                if (isWebResource(parameterType)) {
                    throwLogicWebReferenceException(componentType, method);
                }
            }
            final Class<?> returnType = method.getReturnType();
            if (isWebResource(returnType)) {
                throwLogicWebReferenceException(componentType, method);
            }
        }
    }

    protected boolean isWebResource(Class<?> tp) {
        return isAppWeb(tp) // e.g. app.web.
                || RequestManager.class.isAssignableFrom(tp) // lastaflute request
                || ResponseManager.class.isAssignableFrom(tp) // lastaflute response
                || SessionManager.class.isAssignableFrom(tp) // lastaflute session
                || CookieManager.class.isAssignableFrom(tp) // lastaflute cookie
                || HttpServletRequest.class.isAssignableFrom(tp) // servlet request
                || HttpServletResponse.class.isAssignableFrom(tp) // servlet response
                || HttpSession.class.isAssignableFrom(tp) // servlet session
        ;
    }

    protected boolean isAppWeb(Class<?> tp) {
        for (String webPackage : webPackagePrefixList) {
            if (tp.getName().startsWith(webPackage)) {
                return true;
            }
        }
        return false;
    }

    protected void throwLogicWebReferenceException(Class<?> componentType, Object target) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Web reference from the logic.");
        br.addItem("Advice");
        br.addElement("Logic should not refer web resources,");
        br.addElement(" e.g. classes under 'app.web' package, RequestManager.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public class SeaLogic {");
        br.addElement("        @Resource");
        br.addElement("        private RequestManager requestManager; // *Bad");
        br.addElement("");
        br.addElement("        public void land(SeaForm form) { // *Bad");
        br.addElement("            ...");
        br.addElement("        }");
        br.addElement("    }");
        br.addItem("Logic");
        br.addElement(componentType);
        br.addItem("Web Reference");
        br.addElement(target);
        final String msg = br.buildExceptionMessage();
        throw new LogicWebReferenceException(msg);
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
