/*
 * Copyright 2015-2019 the original author or authors.
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
package org.lastaflute.web.ruts.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.smartdeploy.ManagedHotdeploy;
import org.lastaflute.di.Disposable;
import org.lastaflute.di.DisposableUtil;
import org.lastaflute.di.core.factory.SingletonLaContainerFactory;

/**
 * @author modified by jflute (originated in Seasar and Struts)
 */
public class ModuleConfig implements Disposable, Serializable {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = 1L;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Map<String, ActionMapping> actionMappingMap = new HashMap<String, ActionMapping>();
    protected final List<ActionMapping> actionMappingList = new ArrayList<ActionMapping>();
    protected volatile boolean initialized;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ModuleConfig() {
        initialize();
    }

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    public void initialize() {
        DisposableUtil.add(this);
        initialized = true;
    }

    // ===================================================================================
    //                                                                        Find Mapping
    //                                                                        ============
    public OptionalThing<ActionMapping> findActionMapping(String actionName) {
        if (!initialized) {
            initialize();
        }
        final ActionMapping mapping = actionMappingMap.get(actionName);
        if (mapping != null) {
            return OptionalThing.of(mapping);
        }
        if (ManagedHotdeploy.isHotdeploy()) {
            prepareActionComponent(actionName); // lazy load, put to the map
        }
        return OptionalThing.ofNullable(actionMappingMap.get(actionName), () -> {
            String msg = "Not found the action mapping for the action key: " + actionName;
            throw new IllegalStateException(msg);
        });
    }

    protected void prepareActionComponent(String actionName) {
        SingletonLaContainerFactory.getContainer().getComponent(actionName); // initialize
    }

    // ===================================================================================
    //                                                                       Configuration
    //                                                                       =============
    public void addActionMapping(ActionMapping mapping) {
        actionMappingMap.put(mapping.getActionName(), mapping);
        actionMappingList.add(mapping);
    }

    // ===================================================================================
    //                                                                           HotDeploy
    //                                                                           =========
    public void dispose() {
        actionMappingMap.clear();
        actionMappingList.clear();
        initialized = false;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final String title = DfTypeUtil.toClassTitle(this);
        final String hash = Integer.toHexString(hashCode());
        return title + ":{mapping=" + actionMappingMap.size() + "}@" + hash;
    }
}
