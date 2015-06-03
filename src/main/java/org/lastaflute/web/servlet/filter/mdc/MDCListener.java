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
package org.lastaflute.web.servlet.filter.mdc;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import javax.servlet.ServletException;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.web.servlet.filter.FilterListener;
import org.lastaflute.web.servlet.filter.FilterListenerChain;
import org.lastaflute.web.servlet.request.RequestManager;
import org.slf4j.MDC;

/**
 * @author jflute
 * @since 0.6.0 (2015/05/30 Saturday)
 */
public class MDCListener implements FilterListener {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final ThreadLocal<Map<String, Object>> registeredMapLocal = new ThreadLocal<Map<String, Object>>();
    protected static final ThreadLocal<Object> begunLocal = new ThreadLocal<Object>();

    public static void registerDirectly(String key, String value) {
        if (inTopLevelScope()) {
            String msg = "Not allowed to be called in out of MDC scope: key=" + key + " value=" + value;
            throw new IllegalStateException(msg);
        }
        Map<String, Object> registeredMap = registeredMapLocal.get();
        if (registeredMap == null) {
            registeredMap = new LinkedHashMap<String, Object>();
            registeredMapLocal.set(registeredMap);
        }
        registeredMap.put(key, value);
        MDC.put(key, value);
    }

    protected static boolean inTopLevelScope() {
        return begunLocal.get() == null;
    }

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Map<String, Function<MDCSetupResource, String>> mdcMap; // not null, empty allowed

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public MDCListener(Map<String, Function<MDCSetupResource, String>> mdcMap) {
        if (mdcMap == null) {
            throw new IllegalArgumentException("The argument 'mdcMap' should not be null.");
        }
        this.mdcMap = mdcMap;
    }

    // ===================================================================================
    //                                                                              Listen
    //                                                                              ======
    // TODO lastaflute: [F] test: UnitTest of MDCFilterListener by jflute (2015/05/31)
    @Override
    public void listen(FilterListenerChain chain) throws IOException, ServletException {
        final Map<String, String> originallyMap = prepareOriginallyMap();
        if (originallyMap != null) {
            mdcMap.forEach((key, value) -> {
                final String originallyValue = MDC.get(key);
                if (originallyValue != null) {
                    originallyMap.put(key, originallyValue);
                }
            });
        }
        final boolean topLevel = inTopLevelScope();
        if (topLevel) {
            begunLocal.set(new Object());
        }
        try {
            if (!mdcMap.isEmpty()) {
                final MDCSetupResource resouce = createSetupResource();
                mdcMap.forEach((key, value) -> MDC.put(key, value.apply(resouce)));
            }
            chain.doNext();
        } finally {
            if (originallyMap != null) {
                originallyMap.forEach((key, value) -> MDC.remove(key));
            }
            if (topLevel) {
                final Map<String, Object> registeredMap = registeredMapLocal.get();
                if (registeredMap != null) {
                    registeredMap.forEach((key, value) -> MDC.remove(key));
                    registeredMapLocal.set(null);
                }
                begunLocal.set(null);
            }
        }
    }

    protected Map<String, String> prepareOriginallyMap() {
        return !mdcMap.isEmpty() ? new LinkedHashMap<String, String>() : null;
    }

    protected MDCSetupResource createSetupResource() {
        return new MDCSetupResource(getRequestManager(), key -> {
            return OptionalThing.ofNullable(MDC.get(key), () -> {
                String msg = "Not found the MDC value for the key: " + key;
                throw new IllegalStateException(msg);
            });
        });
    }

    protected RequestManager getRequestManager() {
        return ContainerUtil.getComponent(RequestManager.class);
    }
}
