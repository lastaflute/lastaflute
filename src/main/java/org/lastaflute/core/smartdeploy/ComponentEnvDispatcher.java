/*
 * Copyright 2015-2018 the original author or authors.
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

import java.lang.reflect.Modifier;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.core.direction.AccessibleConfig;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.di.core.ComponentDef;
import org.lastaflute.di.core.customizer.ComponentCustomizer;
import org.lastaflute.di.core.factory.SingletonLaContainerFactory;
import org.lastaflute.di.core.factory.annohandler.AnnotationHandler;
import org.lastaflute.di.core.factory.annohandler.AnnotationHandlerFactory;
import org.lastaflute.di.core.meta.AutoBindingDef;
import org.lastaflute.di.core.meta.InstanceDef;
import org.lastaflute.di.naming.NamingConvention;

/**
 * @author jflute
 */
public class ComponentEnvDispatcher {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    public static final String DEVELOPMENT_HERE = "development.here";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final NamingConvention namingConvention;
    protected final InstanceDef instanceDef;
    protected final AutoBindingDef autoBindingDef;
    protected final boolean externalBinding;
    protected final ComponentCustomizer customizer;
    protected final String nameSuffix; // to check duplicate registration

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ComponentEnvDispatcher(NamingConvention namingConvention, InstanceDef instanceDef, AutoBindingDef autoBindingDef,
            boolean externalBinding, ComponentCustomizer customizer, String nameSuffix) {
        this.namingConvention = namingConvention;
        this.instanceDef = instanceDef;
        this.autoBindingDef = autoBindingDef;
        this.externalBinding = externalBinding;
        this.customizer = customizer;
        this.nameSuffix = nameSuffix;
    }

    // ===================================================================================
    //                                                                        Check Before
    //                                                                        ============
    public static boolean canDispatch(Class<?> componentClass) { // check before without instance
        return SingletonLaContainerFactory.hasContainer() && findEnvDispatch(componentClass) != null;
    }

    protected static EnvDispatch findEnvDispatch(Class<?> componentClass) {
        return componentClass.getAnnotation(EnvDispatch.class);
    }

    // ===================================================================================
    //                                                                            Dispatch
    //                                                                            ========
    public ComponentDef dispatch(Class<?> componentClass) {
        if (!canDispatch(componentClass)) { // already checked but just in case
            return null;
        }
        return doDispatch(componentClass, findEnvDispatch(componentClass));
    }

    protected ComponentDef doDispatch(Class<?> componentClass, EnvDispatch dispatch) {
        checkImplementationSuffix(componentClass, dispatch);
        final AccessibleConfig config = ContainerUtil.getComponent(AccessibleConfig.class);
        final String devHereKey = getDevlopmentHereKey(dispatch);
        if (config.get(devHereKey) == null) {
            String msg = "The property is required in the config for environment dispatch: " + devHereKey;
            throw new IllegalStateException(msg);
        }
        final Class<?> implType = config.is(devHereKey) ? dispatch.development() : dispatch.production();
        return actuallyCreateComponentDef(implType);
    }

    protected void checkImplementationSuffix(Class<?> componentClass, EnvDispatch dispatch) {
        doCheckImplementationSuffix(componentClass, dispatch.development());
        doCheckImplementationSuffix(componentClass, dispatch.production());
    }

    protected void doCheckImplementationSuffix(Class<?> componentClass, Class<?> implType) {
        if (implType.getSimpleName().endsWith(nameSuffix)) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("The suffix of env-dispatch implementation class should not be '" + nameSuffix + "'.");
            br.addItem("Advice");
            br.addElement("Duplicate registration (in cool-deploy) if same suffix,");
            br.addElement("so make suer your implementation classes like this:");
            br.addElement("  (x): DevelopmentSeaLogic");
            br.addElement("  (o): SeaLogicDevelopment");
            br.addItem("Interface");
            br.addElement(componentClass.getName());
            br.addItem("Implementation");
            br.addElement(implType.getName());
            final String msg = br.buildExceptionMessage();
            throw new IllegalStateException(msg);
        }
    }

    protected String getDevlopmentHereKey(EnvDispatch dispatch) {
        return dispatch.devProp(); // has default
    }

    // ===================================================================================
    //                                                                     Actually Create
    //                                                                     ===============
    protected ComponentDef actuallyCreateComponentDef(Class<?> implType) {
        final Class<?> targetClass = namingConvention.toCompleteClass(implType);
        checkImplementationCanBeCreated(targetClass);
        final AnnotationHandler handler = AnnotationHandlerFactory.getAnnotationHandler();
        final ComponentDef cd = handler.createComponentDef(targetClass, instanceDef, autoBindingDef, externalBinding);
        if (cd.getComponentName() == null) {
            cd.setComponentName(deriveComponentName(targetClass));
        }
        handler.appendDI(cd);
        customizer.customize(cd);
        handler.appendInitMethod(cd);
        handler.appendDestroyMethod(cd);
        handler.appendAspect(cd);
        handler.appendInterType(cd);
        return cd;
    }

    protected void checkImplementationCanBeCreated(Class<?> targetClass) {
        if (targetClass.isInterface() || Modifier.isAbstract(targetClass.getModifiers())) {
            String msg = "Not implementation class for environment dispatch: " + targetClass;
            throw new IllegalStateException(msg);
        }
    }

    protected String deriveComponentName(Class<?> targetClass) {
        return namingConvention.fromClassNameToComponentName(targetClass.getName());
    }
}
