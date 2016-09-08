/*
 * Copyright 2015-2016 the original author or authors.
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
package org.lastaflute.core.util;

import java.lang.reflect.Field;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.dbflute.util.DfReflectionUtil;
import org.lastaflute.di.core.ExternalContext;
import org.lastaflute.di.core.SingletonLaContainer;
import org.lastaflute.di.core.exception.ComponentNotFoundException;
import org.lastaflute.di.core.exception.CyclicReferenceComponentException;
import org.lastaflute.di.core.exception.TooManyRegistrationComponentException;
import org.lastaflute.di.core.factory.SingletonLaContainerFactory;

/**
 * @author jflute
 */
public abstract class ContainerUtil {

    // ===================================================================================
    //                                                                           Component
    //                                                                           =========
    /**
     * @param componentType The component type to find. (NotNull)
     * @return The found component. (NotNull)
     * @throws ComponentNotFoundException When the component is not found by the type.
     * @throws TooManyRegistrationComponentException When the component key is related to plural components.
     * @throws CyclicReferenceComponentException When the components refers each other.
     */
    public static <COMPONENT> COMPONENT getComponent(Class<COMPONENT> componentType) { // most frequently used
        return (COMPONENT) SingletonLaContainer.getComponent(componentType);
    }

    /**
     * @param componentName The component name to find. (NotNull)
     * @return The found component. (NotNull)
     * @throws ComponentNotFoundException When the component is not found by the type.
     * @throws TooManyRegistrationComponentException When the component key is related to plural components.
     * @throws CyclicReferenceComponentException When the components refers each other.
     */
    public static <COMPONENT> COMPONENT pickupComponentByName(String componentName) {
        final COMPONENT component = SingletonLaContainer.getComponent(componentName); // variable for generic
        return component;
    }

    /**
     * @param componentType The component type to find. (NotNull)
     * @return The array of found components. (NotNull)
     * @throws CyclicReferenceComponentException When the components refers each other.
     */
    @SuppressWarnings("unchecked")
    public static <COMPONENT> COMPONENT[] searchComponents(Class<COMPONENT> componentType) {
        return (COMPONENT[]) SingletonLaContainerFactory.getContainer().findComponents(componentType);
    }

    /**
     * @param componentType The component type to find. (NotNull)
     * @return The array of found components. (NotNull)
     * @throws CyclicReferenceComponentException When the components refers each other.
     */
    @SuppressWarnings("unchecked")
    public static <COMPONENT> COMPONENT[] searchComponentsAll(Class<COMPONENT> componentType) {
        return (COMPONENT[]) SingletonLaContainerFactory.getContainer().findAllComponents(componentType);
    }

    /**
     * @param componentType The component type to find. (NotNull)
     * @return The determination, true or false.
     */
    public static boolean hasComponent(Class<?> componentType) {
        return SingletonLaContainerFactory.getContainer().hasComponentDef(componentType);
    }

    /**
     * @param componentName The component name to find. (NotNull)
     * @return The determination, true or false.
     */
    public static boolean hasComponent(String componentName) {
        return SingletonLaContainerFactory.getContainer().hasComponentDef(componentName);
    }

    // ===================================================================================
    //                                                                    External Context
    //                                                                    ================
    /**
     * @return The external context of Lasta Di. (NotNull)
     */
    public static ExternalContext retrieveExternalContext() {
        final ExternalContext context = SingletonLaContainerFactory.getExternalContext();
        if (context == null) {
            throw new IllegalStateException("Not found external context in Lasta Di container.");
        }
        return context;
    }

    /**
     * @param request The request for external context of Lasta Di. (NotNull)
     */
    public static void overrideExternalRequest(HttpServletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("The argument 'request' should not be null.");
        }
        final ExternalContext context = retrieveExternalContext();
        final Object existing = context.getRequest();
        if (existing == null) {
            throw new IllegalStateException("Not found external request in Lasta Di container for your overriding by: " + request);
        }
        context.setRequest(request);
    }

    // ===================================================================================
    //                                                                            Injector
    //                                                                            ========
    /**
     * @param target The target instance to which injecting. (NotNull)
     */
    public static void injectSimply(Object target) {
        if (target == null) {
            throw new IllegalArgumentException("The argument 'target' should not be null.");
        }
        // no cache fields #for_now so don't use it at frequently-called object
        for (Class<?> currentType = target.getClass(); !currentType.equals(Object.class); currentType = currentType.getSuperclass()) {
            final Field[] fields = currentType.getDeclaredFields();
            for (Field field : fields) {
                if (field.getAnnotation(Resource.class) != null) { // type only #for_now
                    DfReflectionUtil.setValueForcedly(field, target, getComponent(field.getType()));
                }
            }
        }
    }
}
