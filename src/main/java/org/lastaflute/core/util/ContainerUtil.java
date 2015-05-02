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
package org.lastaflute.core.util;

import javax.servlet.http.HttpServletRequest;

import org.lastaflute.di.core.ExternalContext;
import org.lastaflute.di.core.SingletonLaContainer;
import org.lastaflute.di.core.factory.SingletonLaContainerFactory;

/**
 * @author jflute
 */
public class ContainerUtil {

    public static <COMPONENT> COMPONENT getComponent(Class<COMPONENT> type) {
        return (COMPONENT) SingletonLaContainer.getComponent(type);
    }

    @SuppressWarnings("unchecked")
    public static <COMPONENT> COMPONENT[] findAllComponents(Class<COMPONENT> type) {
        return (COMPONENT[]) SingletonLaContainerFactory.getContainer().findAllComponents(type);
    }

    public static ExternalContext retrieveExternalContext() {
        final ExternalContext context = SingletonLaContainerFactory.getExternalContext();
        if (context == null) {
            throw new IllegalStateException("Not found external context in Lasta Di container.");
        }
        return context;
    }

    public static void overrideExternalRequest(HttpServletRequest request) {
        final ExternalContext context = retrieveExternalContext();
        final Object existing = context.getRequest();
        if (existing == null) {
            throw new IllegalStateException("Not found external request in Lasta Di container for your overriding by: " + request);
        }
        context.setRequest(request);
    }
}
