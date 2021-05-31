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
package org.lastaflute.core.smartdeploy;

import java.util.List;

import org.lastaflute.core.smartdeploy.coins.CreatorPackageProvider;
import org.lastaflute.core.smartdeploy.coins.CreatorStateChecker;
import org.lastaflute.core.smartdeploy.exception.LogicExtendsActionException;
import org.lastaflute.core.smartdeploy.exception.LogicWebReferenceException;
import org.lastaflute.di.core.ComponentDef;
import org.lastaflute.di.core.creator.LogicCreator;
import org.lastaflute.di.naming.NamingConvention;

/**
 * @author jflute
 */
public class RomanticLogicCreator extends LogicCreator {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final List<String> webPackagePrefixList; // not null, for check, e.g. 'org.docksidestage.app.web.'

    protected final CreatorPackageProvider packageProvider = new CreatorPackageProvider();
    protected final CreatorStateChecker stateChecker = createCreatorStateChecker();

    protected CreatorStateChecker createCreatorStateChecker() {
        return new CreatorStateChecker();
    }

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public RomanticLogicCreator(NamingConvention namingConvention) {
        super(namingConvention);
        webPackagePrefixList = deriveWebPackageList(namingConvention);
    }

    protected List<String> deriveWebPackageList(NamingConvention namingConvention) {
        return packageProvider.deriveWebPackageList(namingConvention);
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
                getCustomizer(), getNameSuffix());
    }

    // ===================================================================================
    //                                                                         State Check
    //                                                                         ===========
    protected void checkExtendsAction(ComponentDef componentDef) {
        stateChecker.checkExtendsAction(componentDef, getNameSuffix(), msg -> {
            return new LogicExtendsActionException(msg);
        });
    }

    protected void checkWebReference(ComponentDef componentDef) {
        stateChecker.checkWebReference(componentDef, webPackagePrefixList, getNameSuffix(), msg -> {
            return new LogicWebReferenceException(msg);
        });
    }
}
