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
package org.lastaflute.core.direction;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.dbflute.helper.jprop.ObjectiveProperties;
import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.direction.exception.ConfigPropertyNotFoundException;
import org.lastaflute.di.DisposableUtil;
import org.lastaflute.di.core.LastaDiProperties;
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
    private static final Logger logger = LoggerFactory.getLogger(ObjectiveConfig.class);

    // -----------------------------------------------------
    //                                              Stateful
    //                                              --------
    protected static PropertyFilter bowgunPropertyFilter; // used when initialization
    protected static boolean locked = true;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant director (AD) for framework. (NotNull: after initialization) */
    @Resource
    protected FwAssistantDirector assistantDirector;

    /** The resource path of properties for application. */
    protected String appResource;

    /** The list of resource path for extends-properties. (NotNull, EmptyAllowed) */
    protected final List<String> extendsResourceList = new ArrayList<String>(4);

    /** The objective properties in DBFlute library. (NotNull: after initialization) */
    protected ObjectiveProperties prop;

    /** The filter of configuration value. (NotNull: after initialization) */
    protected PropertyFilter propertyFilter;

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
        doInitialize();
        showBootLogging();
    }

    protected void doInitialize() {
        direct();
        final ObjectiveProperties makingProp = prepareObjectiveProperties();
        makingProp.load();
        prop = makingProp; // prop always be complete object for HotDeploy get() might be called in initialize()
        prepareHotDeploy();
    }

    protected void direct() {
        final FwAssistDirection direction = assistAssistDirection();
        appResource = filterEnvSwitching(direction.assistAppConfig());
        extendsResourceList.clear(); // for reload
        extendsResourceList.addAll(filterEnvSwitchingList(direction.assistExtendsConfigList()));
        final PropertyFilter specified = direction.assistConfigPropertyFilter();
        propertyFilter = specified != null ? specified : createDefaultPropertyFilter();
    }

    protected FwAssistDirection assistAssistDirection() {
        return assistantDirector.assistAssistDirection();
    }

    protected String filterEnvSwitching(String path) {
        return LastaDiProperties.getInstance().resolveLastaEnvPath(path);
    }

    protected List<String> filterEnvSwitchingList(List<String> pathList) {
        return pathList.stream().map(path -> filterEnvSwitching(path)).collect(Collectors.toList());
    }

    protected PropertyFilter createDefaultPropertyFilter() {
        return (propertyKey, propertyValue) -> propertyValue;
    }

    protected ObjectiveProperties prepareObjectiveProperties() {
        final ObjectiveProperties makingProp = newObjectiveProperties(appResource, preparePropertyFilter());
        makingProp.checkImplicitOverride();
        if (!extendsResourceList.isEmpty()) {
            makingProp.extendsProperties(extendsResourceList.toArray(new String[extendsResourceList.size()]));
        }
        return makingProp;
    }

    protected PropertyFilter preparePropertyFilter() {
        if (bowgunPropertyFilter != null) {
            return (key, value) -> bowgunPropertyFilter.filter(key, propertyFilter.filter(key, value));
        } else {
            return propertyFilter;
        }
    }

    protected ObjectiveProperties newObjectiveProperties(String resourcePath, PropertyFilter propertyFilter) {
        return new ObjectiveProperties(resourcePath) {
            @Override
            public String get(String propertyKey) {
                final String propertyValue = super.get(propertyKey);
                verifyPropertyValue(propertyKey, propertyValue);
                return filterPropertyAsDefault(propertyFilter.filter(propertyKey, propertyValue));
            }
        };
    }

    protected void verifyPropertyValue(String propertyKey, final String propertyValue) {
        if (propertyValue == null) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Not found the configuration property by the key.");
            br.addItem("NotFound Property");
            br.addElement(propertyKey);
            br.addItem("Config Display");
            br.addElement(toString());
            final String msg = br.buildExceptionMessage();
            throw new ConfigPropertyNotFoundException(msg);
        }
    }

    protected String filterPropertyAsDefault(String propertyValue) {
        return filterPropertyTrimming(propertyValue);
    }

    protected String filterPropertyTrimming(String propertyValue) {
        return propertyValue != null ? propertyValue.trim() : null; // rear space is unneeded as business
    }

    protected void showBootLogging() {
        if (logger.isInfoEnabled()) {
            logger.info("[Objective Config]");
            logger.info(" " + appResource + " extends " + extendsResourceList);
            final boolean checkImplicitOverride = prop.isCheckImplicitOverride();
            final int count = prop.getJavaPropertiesResult().getPropertyList().size();
            logger.info(" checkImplicitOverride=" + checkImplicitOverride + ", propertyCount=" + count);
            if (bowgunPropertyFilter != null) {
                logger.info(" bowgun=" + bowgunPropertyFilter); // because of important
            }
            // *no logging of all property values because it might contain security info
        }
    }

    // ===================================================================================
    //                                                                        Get Property
    //                                                                        ============
    @Override
    public String get(String propertyKey) {
        reloadIfNeeds();
        return prop.get(propertyKey);
    }

    @Override
    public Integer getAsInteger(String propertyKey) {
        reloadIfNeeds();
        return prop.getAsInteger(propertyKey);
    }

    @Override
    public Long getAsLong(String propertyKey) {
        reloadIfNeeds();
        return prop.getAsLong(propertyKey);
    }

    @Override
    public BigDecimal getAsDecimal(String propertyKey) {
        reloadIfNeeds();
        return prop.getAsDecimal(propertyKey);
    }

    @Override
    public LocalDate getAsDate(String propertyKey) {
        reloadIfNeeds();
        return DfTypeUtil.toLocalDate(prop.getAsDate(propertyKey));
    }

    @Override
    public boolean is(String propertyKey) {
        reloadIfNeeds();
        return prop.is(propertyKey);
    }

    // ===================================================================================
    //                                                                          Hot Deploy
    //                                                                          ==========
    protected void prepareHotDeploy() { // only unused if cool
        DisposableUtil.add(() -> requestHotDeploy());
        hotDeployRequested = false;
    }

    protected void requestHotDeploy() { // called when request ending if HotDeploy
        // no sync to avoid disposable thread locking this (or deadlock) and so no clearing here
        hotDeployRequested = true;
    }

    protected void reloadIfNeeds() {
        if (hotDeployRequested) {
            synchronized (this) {
                // INFO to find mistake that it uses HotDeploy in production
                logger.info("...Reloading objective config by HotDeploy request");
                doInitialize(); // actual dispose, with preparing next HotDeploy, without boot logging
            }
        }
    }

    // ===================================================================================
    //                                                               Bowgun PropertyFilter
    //                                                               =====================
    public static void shootBowgunPropertyFilter(PropertyFilter propertyFilter) {
        assertUnlocked();
        if (logger.isInfoEnabled()) {
            logger.info("...Shooting bowgun property filter: " + propertyFilter);
        }
        bowgunPropertyFilter = propertyFilter;
        lock(); // auto-lock here, because of deep world
    }

    // ===================================================================================
    //                                                                         Config Lock
    //                                                                         ===========
    public static boolean isLocked() {
        return locked;
    }

    public static void lock() {
        if (locked) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("...Locking the objective config!");
        }
        locked = true;
    }

    public static void unlock() {
        if (!locked) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("...Unlocking the objective config!");
        }
        locked = false;
    }

    protected static void assertUnlocked() {
        if (!isLocked()) {
            return;
        }
        throw new IllegalStateException("The objective config is locked.");
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final String title = DfTypeUtil.toClassTitle(this);
        final String hash = Integer.toHexString(hashCode());
        return title + ":{" + appResource + ", " + extendsResourceList + ", " + prop + "}@" + hash;
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
