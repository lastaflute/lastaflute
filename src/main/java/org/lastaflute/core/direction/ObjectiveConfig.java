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
package org.lastaflute.core.direction;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.dbflute.helper.jprop.ObjectiveProperties;
import org.lastaflute.di.Disposable;
import org.lastaflute.di.DisposableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The configuration that can be objective.
 * @author jflute
 */
public class ObjectiveConfig implements AccessibleConfig, Serializable {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(ObjectiveConfig.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant directory (AD) for framework. (NotNull: after initialization) */
    @Resource
    protected FwAssistantDirector assistantDirector;

    /** The resource path of properties for application. */
    protected String appResource;

    /** The list of resource path for extends-properties. (NotNull, EmptyAllowed) */
    protected final List<String> extendsResourceList = new ArrayList<String>(4);

    /** The objective properties in DBFlute library. (NotNull: after initialization) */
    protected ObjectiveProperties prop;

    /** Is hot deploy requested? (true only when local development) */
    protected boolean hotDeployRequested;

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    /**
     * Initialize this component. <br>
     * This is basically called by DI setting file.
     */
    @PostConstruct
    public synchronized void initialize() {
        direct();
        final ObjectiveProperties makingProp = prepareObjectiveProperties();
        makingProp.load();
        prop = makingProp; // prop always be complete object for HotDeploy get() might be called in initialize()
        prepareHotDeploy();
        showBootLogging();
    }

    protected void direct() {
        final FwAssistDirection direction = assistOptionalAssistDirection();
        appResource = filterEnvSwitching(direction.assistAppConfig());
        extendsResourceList.clear(); // for reload
        extendsResourceList.addAll(filterEnvSwitching(direction.assistExtendsConfigList()));
    }

    protected FwAssistDirection assistOptionalAssistDirection() {
        return assistantDirector.assistAssistDirection();
    }

    protected String filterEnvSwitching(String path) {
        // TODO jflute lastaflute: [B] function: java system property switching
        //String systemEnv = System.getProperty("lastaflute.env");
        return path;
    }

    protected List<String> filterEnvSwitching(List<String> pathList) {
        return pathList.stream().map(path -> filterEnvSwitching(path)).collect(Collectors.toList());
    }

    protected ObjectiveProperties prepareObjectiveProperties() {
        final ObjectiveProperties makingProp = newObjectiveProperties(appResource);
        makingProp.checkImplicitOverride();
        if (!extendsResourceList.isEmpty()) {
            makingProp.extendsProperties(extendsResourceList.toArray(new String[extendsResourceList.size()]));
        }
        return makingProp;
    }

    protected ObjectiveProperties newObjectiveProperties(String resourcePath) {
        return new ObjectiveProperties(resourcePath);
    }

    protected void showBootLogging() {
        if (LOG.isInfoEnabled()) {
            LOG.info("[Objective Config]");
            LOG.info(" " + appResource + " extends " + extendsResourceList);
            final boolean checkImplicitOverride = prop.isCheckImplicitOverride();
            final int count = prop.getJavaPropertiesResult().getPropertyList().size();
            LOG.info(" checkImplicitOverride=" + checkImplicitOverride + ", propertyCount=" + count);
            // *no logging of all property values because it might contain security info
        }
    }

    // ===================================================================================
    //                                                                        Get Property
    //                                                                        ============
    /**
     * {@inheritDoc}
     */
    public String get(String propertyKey) {
        reloadIfNeeds();
        return prop.get(propertyKey);
    }

    /**
     * {@inheritDoc}
     */
    public Integer getAsInteger(String propertyKey) {
        reloadIfNeeds();
        return prop.getAsInteger(propertyKey);
    }

    /**
     * {@inheritDoc}
     */
    public Long getAsLong(String propertyKey) {
        reloadIfNeeds();
        return prop.getAsLong(propertyKey);
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal getAsDecimal(String propertyKey) {
        reloadIfNeeds();
        return prop.getAsDecimal(propertyKey);
    }

    /**
     * {@inheritDoc}
     */
    public Date getAsDate(String propertyKey) {
        reloadIfNeeds();
        return prop.getAsDate(propertyKey);
    }

    /**
     * {@inheritDoc}
     */
    public boolean is(String propertyKey) {
        reloadIfNeeds();
        return prop.is(propertyKey);
    }

    // ===================================================================================
    //                                                                          Hot Deploy
    //                                                                          ==========
    protected void prepareHotDeploy() {
        DisposableUtil.add(new Disposable() {
            public void dispose() {
                requestHotDeploy();
            }
        });
        hotDeployRequested = false;
    }

    protected void requestHotDeploy() {
        // no sync to avoid disposable thread locking this (or deadlock)
        // and no clearing here, initialize() clears because of no sync
        hotDeployRequested = true;
    }

    protected void reloadIfNeeds() {
        if (hotDeployRequested) {
            initialize();
        }
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public void setDomainResource(String domainResource) {
        this.appResource = domainResource;
    }

    public void addExtendsResource(String extendsResource) {
        this.extendsResourceList.add(extendsResource);
    }
}
