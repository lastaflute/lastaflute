/*
 * Copyright 2015-2024 the original author or authors.
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
package org.lastaflute.web.container;

import org.lastaflute.di.core.deployer.ComponentDeployerFactory;
import org.lastaflute.di.core.deployer.ExternalComponentDeployerProvider;
import org.lastaflute.di.core.factory.SingletonLaContainerFactory;
import org.lastaflute.di.util.LdiStringUtil;
import org.lastaflute.web.servlet.external.HttpServletExternalContext;
import org.lastaflute.web.servlet.external.HttpServletExternalContextComponentDefRegister;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class WebLastaContainerInitializer {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    private Object application;
    private String configPath;

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    public void initialize() {
        if (isAlreadyInitialized()) {
            return;
        }
        setupOriginalConfigPath();
        setupExternalContext();
        doInitContainer();
    }

    protected boolean isAlreadyInitialized() {
        return SingletonLaContainerFactory.hasContainer();
    }

    protected void setupOriginalConfigPath() {
        if (!LdiStringUtil.isEmpty(configPath)) {
            SingletonLaContainerFactory.setConfigPath(configPath);
        }
    }

    protected void setupExternalContext() {
        if (ComponentDeployerFactory.getProvider() instanceof ComponentDeployerFactory.DefaultProvider) {
            ComponentDeployerFactory.setProvider(newExternalComponentDeployerProvider());
        }
        final HttpServletExternalContext externalContext = newHttpServletExternalContext();
        externalContext.setApplication(application);
        SingletonLaContainerFactory.setExternalContext(externalContext);
        SingletonLaContainerFactory.setExternalContextComponentDefRegister(newHttpServletExternalContextComponentDefRegister());
    }

    protected ExternalComponentDeployerProvider newExternalComponentDeployerProvider() {
        return new ExternalComponentDeployerProvider();
    }

    protected HttpServletExternalContext newHttpServletExternalContext() {
        return new HttpServletExternalContext();
    }

    protected HttpServletExternalContextComponentDefRegister newHttpServletExternalContextComponentDefRegister() {
        return new HttpServletExternalContextComponentDefRegister();
    }

    protected void doInitContainer() {
        SingletonLaContainerFactory.init();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public void setApplication(Object application) {
        this.application = application;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }
}
