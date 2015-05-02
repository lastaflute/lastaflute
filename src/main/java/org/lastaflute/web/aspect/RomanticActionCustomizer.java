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

import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.di.core.ComponentDef;
import org.lastaflute.di.core.customizer.ComponentCustomizer;
import org.lastaflute.di.util.LdiModifierUtil;
import org.lastaflute.web.Execute;
import org.lastaflute.web.direction.OptionalWebDirection;
import org.lastaflute.web.exception.ExecuteMethodNotFoundRuntimeException;
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
        for (Method actionMethod : actionType.getDeclaredMethods()) {
            if (isTargetMethod(actionMapping, actionType, actionMethod)) {
                actionMapping.addExecute(createActionExecute(actionMapping, actionMethod));
            }
        }
        // TODO jflute lastaflute: [E] check: unneeded old style transaction annotation
        checkExecuteMethodSize(actionMapping, actionType);
    }

    protected boolean isTargetMethod(ActionMapping actionMapping, Class<?> actionClass, Method actionMethod) {
        return isExecuteMethod(actionMethod) && actionMapping.getActionExecute(actionMethod.getName()) == null;
    }

    protected boolean isExecuteMethod(Method actionMethod) {
        return LdiModifierUtil.isPublic(actionMethod) && actionMethod.getAnnotation(Execute.class) != null;
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

    protected void checkExecuteMethodSize(ActionMapping actionMapping, Class<?> actionClass) {
        if (actionMapping.getExecuteSize() == 0) {
            throw new ExecuteMethodNotFoundRuntimeException(actionClass);
        }
    }
}
