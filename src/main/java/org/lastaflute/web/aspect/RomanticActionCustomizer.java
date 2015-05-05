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

import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.di.core.ComponentDef;
import org.lastaflute.di.core.customizer.ComponentCustomizer;
import org.lastaflute.di.util.LdiModifierUtil;
import org.lastaflute.web.Execute;
import org.lastaflute.web.direction.OptionalWebDirection;
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
        final OptionalWebDirection direction = director.assistOptionalWebDirection();
        final ActionAdjustmentProvider adjustmentProvider = direction.assistActionAdjustmentProvider();
        return adjustmentProvider;
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
                String msg = "Cannot define overload method of action execute: " + existing;
                throw new IllegalStateException(msg);
            }
            actionMapping.registerExecute(createActionExecute(actionMapping, declaredMethod));
        }
        checkExecuteMethodSize(actionMapping, actionType);
        checkExecuteMethodBothIndexNamedUrlParameter(actionMapping, actionType);
    }

    protected boolean isExecuteMethod(Method actionMethod) {
        return LdiModifierUtil.isPublic(actionMethod) && actionMethod.getAnnotation(Execute.class) != null;
    }

    // -----------------------------------------------------
    //                                      Check Definition
    //                                      ----------------
    // TODO jflute lastaflute: [E] fitting: exception message
    protected void checkExecuteMethodSize(ActionMapping actionMapping, Class<?> actionType) {
        if (actionMapping.getExecuteMap().size() == 0) {
            String msg = "Not found execute method in the action class (needs at least one method): " + actionType;
            throw new IllegalStateException(msg);
        }
    }

    protected void checkExecuteMethodBothIndexNamedUrlParameter(ActionMapping actionMapping, Class<?> actionType) {
        final Map<String, ActionExecute> executeMap = actionMapping.getExecuteMap();
        final ActionExecute index = executeMap.get("index");
        if (index != null && index.getUrlParamArgs().isPresent()) {
            for (Entry<String, ActionExecute> entry : executeMap.entrySet()) {
                final ActionExecute execute = entry.getValue();
                if (!execute.isIndexMethod() && execute.getUrlParamArgs().isPresent()) {
                    String msg = "Cannot use URL parameter both index() and named execute: named=" + execute;
                    throw new IllegalStateException(msg);
                }
            }
        }
    }

    // ===================================================================================
    //                                                                      Action Execute
    //                                                                      ==============
    protected ActionExecute createActionExecute(ActionMapping actionMapping, Method executeMethod) {
        final Execute anno = executeMethod.getAnnotation(Execute.class); // exists, already checked
        final ExecuteOption executeOption = newExecuteOption(anno);
        return newActionExecute(actionMapping, executeMethod, executeOption);
    }

    protected ExecuteOption newExecuteOption(Execute anno) {
        return new ExecuteOption(anno.urlPattern(), anno.suppressTransaction());
    }

    protected ActionExecute newActionExecute(ActionMapping actionMapping, Method executeMethod, ExecuteOption executeOption) {
        return new ActionExecute(actionMapping, executeMethod, executeOption);
    }
}
