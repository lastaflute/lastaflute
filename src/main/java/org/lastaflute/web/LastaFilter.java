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
package org.lastaflute.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

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

import org.dbflute.bhv.BehaviorSelector;
import org.lastaflute.core.direction.BootListener;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.direction.FwCoreDirection;
import org.lastaflute.core.message.MessageResourcesHolder;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.di.core.ExternalContext;
import org.lastaflute.di.core.LaContainer;
import org.lastaflute.di.core.factory.SingletonLaContainerFactory;
import org.lastaflute.di.core.meta.impl.LaContainerBehavior;
import org.lastaflute.di.core.smart.hot.HotdeployBehavior;
import org.lastaflute.di.core.smart.hot.HotdeployUtil;
import org.lastaflute.web.container.WebLastaContainerDestroyer;
import org.lastaflute.web.container.WebLastaContainerInitializer;
import org.lastaflute.web.direction.FwWebDirection;
import org.lastaflute.web.ruts.config.ModuleConfig;
import org.lastaflute.web.ruts.message.MessageResources;
import org.lastaflute.web.ruts.message.RutsMessageResourceGateway;
import org.lastaflute.web.ruts.message.objective.ObjectiveMessageResources;
import org.lastaflute.web.servlet.filter.FilterListener;
import org.lastaflute.web.servlet.filter.RequestLoggingFilter;
import org.lastaflute.web.servlet.filter.RequestRoutingFilter;
import org.lastaflute.web.servlet.filter.accesslog.AccessLogHandler;
import org.lastaflute.web.servlet.filter.accesslog.AccessLogResource;
import org.lastaflute.web.servlet.filter.hotdeploy.HotdeployHttpServletRequest;
import org.lastaflute.web.servlet.filter.hotdeploy.HotdeployHttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class LastaFilter implements Filter {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(LastaFilter.class);

    public static String ENCODING_KEY = "encoding";
    public static String DEFAULT_ENCODING = "UTF-8";
    public static final String HOTDEPLOY_CLASSLOADER_KEY = "lastaflute.hotdeploy.CLASS_LOADER";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected String encoding;
    protected RequestLoggingFilter loggingFilter;
    protected RequestRoutingFilter routingFilter;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LastaFilter() {
    }

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
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
            String msg = "Failed to initialize Lasta Di.";
            logger.error(msg, e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new IllegalStateException(msg, e);
        }
        try {
            adjustComponent(servletContext);
        } catch (RuntimeException e) {
            logger.error("Failed to adjust components.", e);
            throw e;
        }
        final FwAssistantDirector assistantDirector = getAssistantDirector();
        try {
            callbackProcess(assistantDirector);
        } catch (RuntimeException e) {
            logger.error("Failed to callback process.", e);
            throw e;
        }
        initEmbeddedFilter(filterConfig);
        try {
            showBoot(assistantDirector);
        } catch (RuntimeException e) {
            logger.error("Failed to show boot title.", e);
            throw e;
        }
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
    //                                              LastaDi
    //                                             ---------
    protected void initializeContainer(ServletContext servletContext) {
        final WebLastaContainerInitializer initializer = newWebLastaContainerInitializer();
        initializer.setApplication(servletContext);
        initializer.initialize();
    }

    protected WebLastaContainerInitializer newWebLastaContainerInitializer() {
        return new WebLastaContainerInitializer();
    }

    // -----------------------------------------------------
    //                                    Assistant Director
    //                                    ------------------
    protected FwAssistantDirector getAssistantDirector() {
        return ContainerUtil.getComponent(FwAssistantDirector.class);
    }

    protected FwWebDirection assistWebDirection() {
        return getAssistantDirector().assistWebDirection();
    }

    protected List<FilterListener> assistFilterListenerList() {
        return assistWebDirection().assistFilterListenerList();
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

    protected MessageResourcesHolder getMessageResourceHolder() {
        return ContainerUtil.getComponent(MessageResourcesHolder.class);
    }

    // -----------------------------------------------------
    //                                      Callback Process
    //                                      ----------------
    protected void callbackProcess(FwAssistantDirector assistantDirector) {
        final FwCoreDirection coreDirection = assistantDirector.assistCoreDirection();
        final BootListener callback = coreDirection.assistBootListener();
        if (callback != null) {
            callback.listen(assistantDirector);
        }
    }

    protected BehaviorSelector getBehaviorSelector() {
        return ContainerUtil.getComponent(BehaviorSelector.class);
    }

    // -----------------------------------------------------
    //                                       Embedded Filter
    //                                       ---------------
    protected void initEmbeddedFilter(FilterConfig filterConfig) throws ServletException {
        loggingFilter = newRequestLoggingFilter();
        loggingFilter.init(filterConfig);
        routingFilter = newRequestRoutingFilter();
        routingFilter.init(filterConfig);
    }

    protected RequestLoggingFilter newRequestLoggingFilter() {
        return new RequestLoggingFilter();
    }

    protected RequestRoutingFilter newRequestRoutingFilter() {
        return new RequestRoutingFilter();
    }

    // -----------------------------------------------------
    //                                             Show Boot
    //                                             ---------
    protected void showBoot(FwAssistantDirector assistantDirector) {
        if (logger.isInfoEnabled()) {
            final FwCoreDirection coreDirection = assistantDirector.assistCoreDirection();
            final String domainTitle = coreDirection.assistDomainTitle();
            final String environmentTitle = coreDirection.assistEnvironmentTitle();
            logger.info("_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/");
            logger.info(" the system has been initialized:");
            logger.info("");
            logger.info("  -> " + domainTitle + " (" + environmentTitle + ")");
            logger.info("_/_/_/_/_/_/_/_/_/_/");
        }
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
    protected void viaLastaDiContext(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException,
            ServletException {
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
    protected void viaHotdeploy(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        if (!HotdeployUtil.isHotdeploy()) {
            viaFilterListener(request, response, chain); // #to_action
            return;
        }
        final String loaderKey = HOTDEPLOY_CLASSLOADER_KEY;
        if (request.getAttribute(loaderKey) != null) { // check recursive call
            final ClassLoader loader = (ClassLoader) request.getAttribute(loaderKey);
            Thread.currentThread().setContextClassLoader(loader);
            viaFilterListener(request, response, chain); // #to_action
            return;
        }
        final HotdeployBehavior ondemand = (HotdeployBehavior) LaContainerBehavior.getProvider();
        synchronized (LastaFilter.class) {
            ondemand.start();
            final HotdeployHttpServletRequest hotdeployRequest = newHotdeployHttpServletRequest(request);
            ContainerUtil.overrideExternalRequest(hotdeployRequest); // override formal request
            try {
                request.setAttribute(loaderKey, Thread.currentThread().getContextClassLoader());
                viaFilterListener(hotdeployRequest, response, chain); // #to_action
            } finally {
                final HotdeployHttpSession session = (HotdeployHttpSession) hotdeployRequest.getSession(false);
                if (session != null) {
                    session.flush();
                }
                request.removeAttribute(loaderKey);
                ondemand.stop();
            }
        }
    }

    protected HotdeployHttpServletRequest newHotdeployHttpServletRequest(HttpServletRequest request) {
        return new HotdeployHttpServletRequest(request);
    }

    // -----------------------------------------------------
    //                                   via Filter Listener
    //                                   -------------------
    protected void viaFilterListener(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        viaListenerDeque(request, response, chain, prepareFilterListener()); // #to_action
    }

    protected Deque<FilterListener> prepareFilterListener() { // null allowed (if no listener)
        final List<FilterListener> listenerList = assistFilterListenerList();
        return !listenerList.isEmpty() ? new LinkedList<FilterListener>(listenerList) : null;
    }

    protected void viaListenerDeque(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Deque<FilterListener> deque)
            throws IOException, ServletException {
        final FilterListener next = deque != null ? deque.poll() : null; // null if no listener
        if (next != null) {
            next.listen(() -> viaListenerDeque(request, response, chain, deque)); // e.g. MDC
        } else {
            viaEmbeddedFilter(request, response, chain); // #to_action
        }
    }

    // -----------------------------------------------------
    //                                   via Embedded Filter
    //                                   -------------------
    protected void viaEmbeddedFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        loggingFilter.doFilter(request, response, (req, res) -> {
            enableAccessLogIfNeeds();
            routingFilter.doFilter(req, res, prepareFinalChain(chain)); /* #to_action */
        });
    }

    protected void enableAccessLogIfNeeds() { // for e.g. access log
        if (loggingFilter.isAlreadyBegun()) { // means handler to be set here always be cleared later
            final AccessLogHandler handler = getAssistantDirector().assistWebDirection().assistAccessLogHandler();
            if (handler != null) { // specified by application
                RequestLoggingFilter.setAccessLogHandlerOnThread((request, response, cause, before) -> {
                    handler.handle(new AccessLogResource(request, response, cause, before));
                });
            }
        }
    }

    protected FilterChain prepareFinalChain(FilterChain chain) {
        return chain;
    }

    // ===================================================================================
    //                                                                             Destroy
    //                                                                             =======
    @Override
    public void destroy() {
        WebLastaContainerDestroyer.destroy();
        destroyEmbeddedFilter();
    }

    protected void destroyEmbeddedFilter() {
        if (loggingFilter != null) {
            loggingFilter.destroy();
        }
        if (routingFilter != null) {
            routingFilter.destroy();
        }
    }
}
