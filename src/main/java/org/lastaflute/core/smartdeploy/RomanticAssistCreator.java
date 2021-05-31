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
import org.lastaflute.core.smartdeploy.exception.AssistExtendsActionException;
import org.lastaflute.core.smartdeploy.exception.JobAssistWebReferenceException;
import org.lastaflute.di.core.ComponentDef;
import org.lastaflute.di.core.creator.AssistCreator;
import org.lastaflute.di.naming.NamingConvention;

/**
 * @author jflute
 * @since 0.7.3 (2015/12/27 Sunday)
 */
public class RomanticAssistCreator extends AssistCreator {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final List<String> webPackagePrefixList; // not null, for check, e.g. 'org.docksidestage.app.web.'
    protected final List<String> jobPackagePrefixList; // not null, for check, e.g. 'org.docksidestage.app.job.'

    protected final CreatorPackageProvider packageProvider = new CreatorPackageProvider();
    protected final CreatorStateChecker stateChecker = createCreatorStateChecker();

    protected CreatorStateChecker createCreatorStateChecker() {
        return new CreatorStateChecker();
    }

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public RomanticAssistCreator(NamingConvention namingConvention) {
        super(namingConvention);
        webPackagePrefixList = deriveWebPackageList(namingConvention);
        jobPackagePrefixList = deriveJobPackageList(namingConvention);
    }

    protected List<String> deriveWebPackageList(NamingConvention namingConvention) {
        return packageProvider.deriveWebPackageList(namingConvention);
    }

    protected List<String> deriveJobPackageList(NamingConvention namingConvention) {
        return packageProvider.deriveJobPackageList(namingConvention);
    }

    // ===================================================================================
    //                                                                       Component Def
    //                                                                       =============
    @Override
    public ComponentDef createComponentDef(Class<?> componentClass) {
        final ComponentDef componentDef = super.createComponentDef(componentClass); // null allowed
        if (componentDef == null) {
            return null;
        }
        checkExtendsAction(componentDef);
        if (isAppJob(componentDef.getComponentClass())) { // job assist e.g. app.job.SeaAssist
            checkWebReference(componentDef);
        }
        return componentDef;
    }

    protected boolean isAppJob(Class<?> tp) {
        for (String jobPackage : jobPackagePrefixList) {
            if (tp.getName().startsWith(jobPackage)) {
                return true;
            }
        }
        return false;
    }

    // ===================================================================================
    //                                                                         State Check
    //                                                                         ===========
    protected void checkExtendsAction(ComponentDef componentDef) {
        stateChecker.checkExtendsAction(componentDef, getNameSuffix(), msg -> {
            return new AssistExtendsActionException(msg);
        });
    }

    protected void checkWebReference(ComponentDef componentDef) {
        stateChecker.checkWebReference(componentDef, webPackagePrefixList, getNameSuffix(), msg -> {
            return new JobAssistWebReferenceException(msg);
        });
    }
}
