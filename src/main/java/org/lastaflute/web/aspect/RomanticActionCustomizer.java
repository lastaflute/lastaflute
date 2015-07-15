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
import java.util.Map;
import java.util.Map.Entry;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.di.core.ComponentDef;
import org.lastaflute.di.core.customizer.ComponentCustomizer;
import org.lastaflute.di.util.LdiModifierUtil;
import org.lastaflute.web.Execute;
import org.lastaflute.web.direction.FwWebDirection;
import org.lastaflute.web.exception.ExecuteMethodIllegalDefinitionException;
import org.lastaflute.web.path.ActionAdjustmentProvider;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.config.ActionMapping;
import org.lastaflute.web.ruts.config.ExecuteOption;
import org.lastaflute.web.util.LaModuleConfigUtil;

/**
 * You can get romantic action.
 * @author jflute
 */
public class RomanticActionCustomizer implements ComponentCustomizer {

    // ===================================================================================
    //                                                                           Customize
    //                                                                           =========
    @Override
    public void customize(ComponentDef componentDef) {
        final ActionMapping actionMapping = createActionMapping(componentDef);
        LaModuleConfigUtil.getModuleConfig().addActionMapping(actionMapping);
    }

    // ===================================================================================
    //                                                                      Action Mapping
    //                                                                      ==============
    protected ActionMapping createActionMapping(ComponentDef actionDef) {
        final String actionName = buildActionName(actionDef);
        final ActionMapping mapping = newActionMapping(actionDef, actionName, comeOnAdjustmentProvider());
        setupMethod(mapping);
        return mapping;
    }

    protected String buildActionName(ComponentDef actionDef) {
        return actionDef.getComponentName();
    }

    protected ActionAdjustmentProvider comeOnAdjustmentProvider() {
        final FwAssistantDirector director = ContainerUtil.getComponent(FwAssistantDirector.class);
        final FwWebDirection direction = director.assistWebDirection();
        return direction.assistActionAdjustmentProvider();
    }

    protected ActionMapping newActionMapping(ComponentDef actionDef, String actionName, ActionAdjustmentProvider adjustmentProvider) {
        return new ActionMapping(actionDef, actionName, adjustmentProvider);
    }

    protected String buildFieldFormKey(ComponentDef actionDef) {
        return buildActionName(actionDef) + "_Form"; // e.g. member_memberListAction_Form
    }

    // ===================================================================================
    //                                                                       Set up Method
    //                                                                       =============
    protected void setupMethod(ActionMapping actionMapping) {
        final Class<?> actionType = actionMapping.getActionDef().getComponentClass();
        for (Method declaredMethod : actionType.getDeclaredMethods()) {
            if (!isExecuteMethod(declaredMethod)) {
                continue;
            }
            final ActionExecute existing = actionMapping.getActionExecute(declaredMethod);
            if (existing != null) {
                throwOverloadMethodCannotDefinedException(actionType);
            }
            actionMapping.registerExecute(createActionExecute(actionMapping, declaredMethod));
        }
        verifyExecuteMethodSize(actionMapping, actionType);
        verifyExecuteMethodEitherIndexAndNamedUsingUrlParameter(actionMapping, actionType);
        verifyExecuteMethodDefinedInConcreteClassOnly(actionMapping, actionType);
    }

    protected void throwOverloadMethodCannotDefinedException(final Class<?> actionType) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Cannot define overload method of action execute.");
        br.addItem("Advice");
        br.addElement("Same-name different-parameter method");
        br.addElement("cannot be defined as execute method.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse index() {");
        br.addElement("    }");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse index(String sea) {");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse index() {");
        br.addElement("    }");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse land(String sea) {");
        br.addElement("    }");
        br.addElement("Action");
        br.addElement(actionType);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    // -----------------------------------------------------
    //                                     Verify Definition
    //                                     -----------------
    protected void verifyExecuteMethodSize(ActionMapping actionMapping, Class<?> actionType) {
        if (actionMapping.getExecuteMap().isEmpty()) {
            throwExecuteMethodNotFoundInActionException(actionType);
        }
    }

    protected void throwExecuteMethodNotFoundInActionException(Class<?> actionType) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found execute method in the action class.");
        br.addItem("Advice");
        br.addElement("Action class needs at least one method.");
        br.addElement("Confirm the @Execute annotation of your execute method.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse index() {");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse index() {");
        br.addElement("    }");
        br.addElement("Action");
        br.addElement(actionType);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    protected void verifyExecuteMethodEitherIndexAndNamedUsingUrlParameter(ActionMapping actionMapping, Class<?> actionType) {
        final Map<String, ActionExecute> executeMap = actionMapping.getExecuteMap();
        final ActionExecute index = executeMap.get("index");
        if (index != null && index.getUrlParamArgs().isPresent()) {
            for (Entry<String, ActionExecute> entry : executeMap.entrySet()) {
                final ActionExecute execute = entry.getValue();
                if (!execute.isIndexMethod() && execute.getUrlParamArgs().isPresent()) {
                    throwUrlParameterCannotUseBothIndexNamedException(index, execute);
                }
            }
        }
    }

    protected void throwUrlParameterCannotUseBothIndexNamedException(final ActionExecute index, final ActionExecute execute) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Cannot use URL parameter both index() and named execute.");
        br.addItem("Advice");
        br.addElement("URL parameter can be used either index() or named execute method.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse index(Integer pageNumber) {");
        br.addElement("    }");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse sea(String land) {");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse index() {");
        br.addElement("    }");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse sea(String land) {");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse index(String land) {");
        br.addElement("    }");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse sea() {");
        br.addElement("    }");
        br.addElement("Index Execute");
        br.addElement(index);
        br.addElement("Named Execute");
        br.addElement(execute);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    protected void verifyExecuteMethodDefinedInConcreteClassOnly(ActionMapping actionMapping, Class<?> actionType) {
        for (Class<?> clazz = actionType.getSuperclass(); !Object.class.equals(clazz); clazz = clazz.getSuperclass()) {
            if (clazz == null) { // just in case
                break;
            }
            for (Method declaredMethod : clazz.getDeclaredMethods()) {
                if (isExecuteMethod(declaredMethod)) {
                    throwExecuteMethodAtSuperClassCannotBeDefinedException(clazz, declaredMethod);
                }
            }
        }
    }

    protected void throwExecuteMethodAtSuperClassCannotBeDefinedException(Class<?> clazz, Method declaredMethod) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Cannot define execute method at super class.");
        br.addItem("Advice");
        br.addElement("Execute method should be defined at concrete class.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public abstract class SeaBaseAction ... {");
        br.addElement("        @Execute");
        br.addElement("        public HtmlResponse index() {");
        br.addElement("        }");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public class SeaAction ... {");
        br.addElement("        @Execute");
        br.addElement("        public HtmlResponse index() {");
        br.addElement("        }");
        br.addElement("    }");
        br.addElement("Super Class");
        br.addElement(clazz);
        br.addElement("Illegal Execute");
        br.addElement(declaredMethod);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    // ===================================================================================
    //                                                                      Action Execute
    //                                                                      ==============
    protected ActionExecute createActionExecute(ActionMapping actionMapping, Method executeMethod) {
        final Execute anno = getExecuteAnnotation(executeMethod); // exists, already checked
        final ExecuteOption executeOption = createExecuteOption(anno);
        return newActionExecute(actionMapping, executeMethod, executeOption);
    }

    protected Execute getExecuteAnnotation(Method executeMethod) {
        return executeMethod.getAnnotation(Execute.class);
    }

    protected ExecuteOption createExecuteOption(Execute anno) {
        return new ExecuteOption(anno.urlPattern(), anno.suppressTransaction(), anno.sqlCountLimit());
    }

    protected ActionExecute newActionExecute(ActionMapping actionMapping, Method executeMethod, ExecuteOption executeOption) {
        return new ActionExecute(actionMapping, executeMethod, executeOption);
    }

    protected boolean isExecuteMethod(Method actionMethod) {
        return LdiModifierUtil.isPublic(actionMethod) && getExecuteAnnotation(actionMethod) != null;
    }
}
