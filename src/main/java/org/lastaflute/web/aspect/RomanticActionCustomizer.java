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

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.di.core.ComponentDef;
import org.lastaflute.di.core.customizer.ComponentCustomizer;
import org.lastaflute.web.Execute;
import org.lastaflute.web.aspect.verifier.RomanticStructuredActionVerifier;
import org.lastaflute.web.direction.FwWebDirection;
import org.lastaflute.web.path.ActionAdjustmentProvider;
import org.lastaflute.web.path.restful.verifier.RestfulRomanticVerifier;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.config.ActionMapping;
import org.lastaflute.web.ruts.config.ExecuteOption;
import org.lastaflute.web.ruts.config.ModuleConfig;
import org.lastaflute.web.ruts.config.specifed.SpecifiedHttpStatus;
import org.lastaflute.web.ruts.config.specifed.SpecifiedUrlPattern;
import org.lastaflute.web.util.LaActionExecuteUtil;
import org.lastaflute.web.util.LaModuleConfigUtil;

/**
 * You can get romantic action.
 * @author jflute
 */
public class RomanticActionCustomizer implements ComponentCustomizer {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RomanticStructuredActionVerifier structuredActionVerifier = newRomanticStructuredActionVerifier();

    protected RomanticStructuredActionVerifier newRomanticStructuredActionVerifier() {
        return new RomanticStructuredActionVerifier();
    }

    protected final RestfulRomanticVerifier restfulRomanticVerifier = newRestfulRomanticVerifier();

    protected RestfulRomanticVerifier newRestfulRomanticVerifier() {
        return new RestfulRomanticVerifier();
    }

    // ===================================================================================
    //                                                                           Customize
    //                                                                           =========
    @Override
    public void customize(ComponentDef componentDef) {
        final ActionMapping actionMapping = createActionMapping(componentDef);
        getModuleConfig().addActionMapping(actionMapping);
    }

    // ===================================================================================
    //                                                                      Action Mapping
    //                                                                      ==============
    protected ActionMapping createActionMapping(ComponentDef actionDef) {
        final String actionName = buildActionName(actionDef);
        structuredActionVerifier.verifyPackageConvention(actionDef, actionName);
        final ActionMapping mapping = newActionMapping(actionDef, actionName, comeOnAdjustmentProvider());
        setupMethod(mapping);
        return mapping;
    }

    protected String buildActionName(ComponentDef actionDef) {
        return actionDef.getComponentName();
    }

    protected ActionMapping newActionMapping(ComponentDef actionDef, String actionName, ActionAdjustmentProvider adjustmentProvider) {
        return new ActionMapping(actionDef, actionName, adjustmentProvider);
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
            final ActionExecute execute = createActionExecute(actionMapping, declaredMethod);
            structuredActionVerifier.verifyOverloadExecuteMethod(actionMapping, actionType, execute);
            actionMapping.registerExecute(execute);
        }
        structuredActionVerifier.verifyExecuteMethodSize(actionMapping, actionType);
        structuredActionVerifier.verifyExecuteMethodNotShadowingOthers(actionMapping, actionType);
        structuredActionVerifier.verifyExecuteMethodDefinedInConcreteClassOnly(actionMapping, actionType);
        restfulRomanticVerifier.verifyRestfulIndependent(actionMapping, actionType);
        restfulRomanticVerifier.verifyRestfulHttpMethodAll(actionMapping, actionType);
        restfulRomanticVerifier.verifyRestfulCannotAtWord(actionMapping, actionType);
        restfulRomanticVerifier.verifyRestfulCannotOptional(actionMapping, actionType);
        restfulRomanticVerifier.verifyRestfulCannotEventSuffix(actionMapping, actionType);
        restfulRomanticVerifier.verifyRestfulStructuredMethod(actionMapping, actionType);
    }

    protected boolean isExecuteMethod(Method actionMethod) {
        return LaActionExecuteUtil.isExecuteMethod(actionMethod);
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
        return LaActionExecuteUtil.getExecuteAnnotation(executeMethod);
    }

    protected ExecuteOption createExecuteOption(Execute anno) {
        final OptionalThing<SpecifiedUrlPattern> specifiedUrlPattern = SpecifiedUrlPattern.create(anno.urlPattern());
        final boolean suppressTransaction = anno.suppressTransaction();
        final boolean suppressValidatorCallCheck = anno.suppressValidatorCallCheck();
        final int sqlExecutionCountLimit = anno.sqlExecutionCountLimit();
        final OptionalThing<SpecifiedHttpStatus> successHttpStatus = SpecifiedHttpStatus.create(anno.successHttpStatus());
        return new ExecuteOption(specifiedUrlPattern // basic
                , suppressTransaction, suppressValidatorCallCheck // suppress option
                , sqlExecutionCountLimit // sql option
                , successHttpStatus // HTTP option
        );
    }

    protected ActionExecute newActionExecute(ActionMapping actionMapping, Method executeMethod, ExecuteOption executeOption) {
        return new ActionExecute(actionMapping, executeMethod, executeOption);
    }

    // ===================================================================================
    //                                                                           Component
    //                                                                           =========
    protected ModuleConfig getModuleConfig() {
        return LaModuleConfigUtil.getModuleConfig();
    }

    protected ActionAdjustmentProvider comeOnAdjustmentProvider() {
        final FwAssistantDirector director = ContainerUtil.getComponent(FwAssistantDirector.class);
        final FwWebDirection direction = director.assistWebDirection();
        return direction.assistActionAdjustmentProvider();
    }
}
