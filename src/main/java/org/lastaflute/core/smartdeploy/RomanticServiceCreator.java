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
package org.lastaflute.core.smartdeploy;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.core.smartdeploy.exception.ServiceExtendsActionException;
import org.lastaflute.di.core.ComponentDef;
import org.lastaflute.di.core.creator.ServiceCreator;
import org.lastaflute.di.naming.NamingConvention;
import org.lastaflute.web.LastaAction;

/**
 * @author jflute
 * @since 0.7.3 (2015/12/27 Sunday)
 */
public class RomanticServiceCreator extends ServiceCreator {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final List<String> webPackagePrefixList; // not null, for check, e.g. 'org.docksidestage.app.web.'

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public RomanticServiceCreator(NamingConvention namingConvention) {
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
        // env dispatch is only for logic (so use logic about environment process)
        final ComponentDef componentDef = super.createComponentDef(componentClass); // null allowed
        if (componentDef == null) {
            return null;
        }
        checkExtendsAction(componentDef);
        // service has delicate role for various people so no check about web reference
        //checkWebReference(componentDef);
        return componentDef;
    }

    // ===================================================================================
    //                                                                      Extends Action
    //                                                                      ==============
    protected void checkExtendsAction(ComponentDef componentDef) {
        final Class<?> componentType = componentDef.getComponentClass();
        if (LastaAction.class.isAssignableFrom(componentType)) {
            throwServiceExtendsActionException(componentType);
        }
    }

    protected void throwServiceExtendsActionException(Class<?> componentType) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("No way, the service extends action.");
        br.addItem("Advice");
        br.addElement("Service is not Action,");
        br.addElement("so the service cannot extend action.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public class SeaService extends MaihamaBaseAction { // *Bad");
        br.addElement("       ...");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public class SeaService { // Good");
        br.addElement("       ...");
        br.addElement("    }");
        br.addItem("Service");
        br.addElement(componentType);
        br.addItem("Super Class");
        br.addElement(componentType.getSuperclass());
        final String msg = br.buildExceptionMessage();
        throw new ServiceExtendsActionException(msg);
    }
}
