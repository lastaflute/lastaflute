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
package org.lastaflute.web.servlet.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lastaflute.core.direction.CurtainBeforeHook;
import org.lastaflute.core.direction.CurtainFinallyHook;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.direction.FwCoreDirection;
import org.lastaflute.core.message.resources.MessageResourcesHolder;
import org.lastaflute.core.smartdeploy.ManagedHotdeploy;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.di.core.ExternalContext;
import org.lastaflute.di.core.LaContainer;
import org.lastaflute.di.core.factory.SingletonLaContainerFactory;
import org.lastaflute.di.core.smart.hot.HotdeployLock;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.container.WebLastaContainerDestroyer;
import org.lastaflute.web.container.WebLastaContainerInitializer;
import org.lastaflute.web.ruts.config.ModuleConfig;
import org.lastaflute.web.ruts.message.MessageResources;
import org.lastaflute.web.ruts.message.RutsMessageResourceGateway;
import org.lastaflute.web.ruts.message.objective.ObjectiveMessageResources;
import org.lastaflute.web.servlet.filter.bowgun.BowgunCurtainBefore;
import org.lastaflute.web.servlet.filter.hotdeploy.HotdeployHttpServletRequest;
import org.lastaflute.web.servlet.filter.hotdeploy.HotdeployHttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encoding and Di context and hot deploy
 * @author jflute
 */
public class LastaPrepareFilter implements Filter {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(LastaPrepareFilter.class);

    public static String ENCODING_KEY = "encoding";
    public static String DEFAULT_ENCODING = "UTF-8";
    public static final String HOTDEPLOY_CLASSLOADER_KEY = "lastaflute.hotdeploy.CLASS_LOADER";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected String encoding;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LastaPrepareFilter() {
    }

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        encoding = filterConfig.getInitParameter(ENCODING_KEY);
        if (encoding == null) {
            encoding = DEFAULT_ENCODING;
        }
        final ServletContext servletContext = filterConfig.getServletContext();
        initModuleConfig(servletContext); // before container because of used by customizer (when cool)
        try {
            initializeContainer(servletContext);
        } catch (Throwable e) {
            handleErrorCause("Failed to initialize Lasta Di.", e);
        }
        try {
            adjustComponent(servletContext);
        } catch (Throwable e) {
            handleErrorCause("Failed to adjust components.", e);
        }
        final FwAssistantDirector assistantDirector = getAssistantDirector();
        try {
            hookCurtainBefore(assistantDirector);
        } catch (Throwable e) {
            handleErrorCause("Failed to hook process.", e);
        }
    }

    protected void handleErrorCause(String msg, Throwable cause) {
        cause.printStackTrace(); // because it might fail to initialize logback by e.g. 'lasta.env'
        logger.error(msg, cause); // to write to application log
        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }
        throw new IllegalStateException(msg, cause);
    }

    // -----------------------------------------------------
    //                                   Action ModuleConfig
    //                                   -------------------
    protected void initModuleConfig(ServletContext servletContext) throws UnavailableException {
        servletContext.setAttribute(LastaWebKey.MODULE_CONFIG_KEY, newModuleConfig());
    }

    protected ModuleConfig newModuleConfig() {
        return new ModuleConfig();
    }

    // -----------------------------------------------------
    //                                              Lasta Di
    //                                              --------
    protected void initializeContainer(ServletContext servletContext) {
        final WebLastaContainerInitializer initializer = newWebLastaContainerInitializer();
        initializer.setApplication(servletContext);
        initializer.initialize();
    }

    protected WebLastaContainerInitializer newWebLastaContainerInitializer() {
        return new WebLastaContainerInitializer();
    }

    // -----------------------------------------------------
    //                                      Adjust Component
    //                                      ----------------
    protected void adjustComponent(ServletContext context) {
        adjustMessageResources(context);
    }

    protected void adjustMessageResources(ServletContext context) {
        saveMessageResourcesToContext(context);
        saveMessageResourcesToHolder(context);
    }

    // -----------------------------------------------------
    //                           MessageResources to Context
    //                           ---------------------------
    protected void saveMessageResourcesToContext(ServletContext context) {
        context.setAttribute(LastaWebKey.MESSAGE_RESOURCES_KEY, newMessageResources());
    }

    protected MessageResources newMessageResources() {
        return new ObjectiveMessageResources();
    }

    // -----------------------------------------------------
    //                            MessageResources to Holder
    //                            --------------------------
    protected void saveMessageResourcesToHolder(ServletContext context) {
        final MessageResources resources = getMessageResources(context);
        final RutsMessageResourceGateway gateway = newRutsMessageResourceGateway(resources);
        getMessageResourceHolder().acceptGateway(gateway);
    }

    protected MessageResources getMessageResources(ServletContext context) {
        return (MessageResources) context.getAttribute(LastaWebKey.MESSAGE_RESOURCES_KEY);
    }

    protected RutsMessageResourceGateway newRutsMessageResourceGateway(MessageResources messages) {
        return new RutsMessageResourceGateway(messages);
    }

    // -----------------------------------------------------
    //                                        Curtain Before
    //                                        --------------
    protected void hookCurtainBefore(FwAssistantDirector assistantDirector) {
        final FwCoreDirection coreDirection = assistantDirector.assistCoreDirection();
        final CurtainBeforeHook hook = coreDirection.assistCurtainBeforeHook();
        if (hook != null) {
            hook.hook(assistantDirector);
        }
        BowgunCurtainBefore.handleBowgunCurtainBefore(assistantDirector);
    }

    // ===================================================================================
    //                                                                          doFilter()
    //                                                                          ==========
    public void doFilter(ServletRequest servReq, ServletResponse servRes, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) servReq;
        final HttpServletResponse response = (HttpServletResponse) servRes;
        resolveCharacterEncoding(request);
        viaLastaDiContext(request, response, chain); // #to_action
    }

    // -----------------------------------------------------
    //                                        Small Solution
    //                                        --------------
    protected void resolveCharacterEncoding(HttpServletRequest request) throws UnsupportedEncodingException {
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding(encoding);
        }
    }

    // -----------------------------------------------------
    //                                   via LastaDi Context
    //                                   -------------------
    protected void viaLastaDiContext(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        final LaContainer container = SingletonLaContainerFactory.getContainer();
        final ExternalContext externalContext = container.getExternalContext();
        if (externalContext == null) {
            throw new IllegalStateException("The externalContext should not be null from the container: " + container);
        }
        final Object prevoiusRequest = externalContext.getRequest();
        final Object previousResponse = externalContext.getResponse();
        try {
            externalContext.setRequest(request);
            externalContext.setResponse(response);
            viaHotdeploy(request, response, chain); // #to_action
        } finally {
            externalContext.setRequest(prevoiusRequest);
            externalContext.setResponse(previousResponse);
        }
    }

    // -----------------------------------------------------
    //                                via HotDeploy Handling
    //                                ----------------------
    protected void viaHotdeploy(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!ManagedHotdeploy.isHotdeploy()) { // e.g. production, unit-test
            toNextFilter(request, response, chain); // #to_action
            return;
        }
        final String loaderKey = HOTDEPLOY_CLASSLOADER_KEY;
        if (request.getAttribute(loaderKey) != null) { // check recursive call
            final ClassLoader loader = (ClassLoader) request.getAttribute(loaderKey);
            Thread.currentThread().setContextClassLoader(loader);
            toNextFilter(request, response, chain); // #to_action
            return;
        }
        synchronized (HotdeployLock.class) {
            final ClassLoader originalLoader = ManagedHotdeploy.start();
            try {
                final HotdeployHttpServletRequest hotdeployRequest = newHotdeployHttpServletRequest(request);
                ContainerUtil.overrideExternalRequest(hotdeployRequest); // override formal request
                try {
                    request.setAttribute(loaderKey, Thread.currentThread().getContextClassLoader());
                    toNextFilter(hotdeployRequest, response, chain); // #to_action
                } finally {
                    final HotdeployHttpSession session = (HotdeployHttpSession) hotdeployRequest.getSession(false);
                    if (session != null) {
                        session.flush();
                    }
                    request.removeAttribute(loaderKey);
                }
            } finally {
                ManagedHotdeploy.stop(originalLoader);
            }
        }
    }

    protected HotdeployHttpServletRequest newHotdeployHttpServletRequest(HttpServletRequest request) {
        return new HotdeployHttpServletRequest(request);
    }

    protected void toNextFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(request, response); // #to_action
    }

    // ===================================================================================
    //                                                                             Destroy
    //                                                                             =======
    @Override
    public void destroy() {
        hookCurtainFinally(getAssistantDirector());
        destroyContainer();
    }

    // -----------------------------------------------------
    //                                       Curtain Finally
    //                                       ---------------
    protected void hookCurtainFinally(FwAssistantDirector assistantDirector) {
        final FwCoreDirection coreDirection = assistantDirector.assistCoreDirection();
        final CurtainFinallyHook hook = coreDirection.assistCurtainFinallyHook();
        if (hook != null) {
            hook.hook(assistantDirector);
        }
    }

    // -----------------------------------------------------
    //                                     LastaDi Container
    //                                     -----------------
    protected void destroyContainer() {
        WebLastaContainerDestroyer.destroy();
    }

    // ===================================================================================
    //                                                                           Component
    //                                                                           =========
    protected FwAssistantDirector getAssistantDirector() {
        return ContainerUtil.getComponent(FwAssistantDirector.class);
    }

    protected MessageResourcesHolder getMessageResourceHolder() {
        return ContainerUtil.getComponent(MessageResourcesHolder.class);
    }
}
