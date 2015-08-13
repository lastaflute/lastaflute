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
package org.lastaflute.web.servlet.filter;

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.direction.FwCoreDirection;
import org.lastaflute.core.message.MessageResourcesHolder;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.web.direction.FwWebDirection;
import org.lastaflute.web.path.ActionAdjustmentProvider;
import org.lastaflute.web.servlet.filter.accesslog.AccessLogHandler;
import org.lastaflute.web.servlet.filter.accesslog.AccessLogResource;
import org.lastaflute.web.servlet.filter.listener.FilterHook;
import org.lastaflute.web.servlet.request.RequestManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class LastaToActionFilter implements Filter {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(LastaToActionFilter.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected RequestLoggingFilter loggingFilter;
    protected RequestRoutingFilter routingFilter;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LastaToActionFilter() {
    }

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    public void init(FilterConfig filterConfig) throws ServletException {
        initEmbeddedFilter(filterConfig);
        initFilterHook(filterConfig);
        try {
            showBoot(getAssistantDirector());
        } catch (RuntimeException e) {
            logger.error("Failed to show boot title.", e);
            throw e;
        }
    }

    // -----------------------------------------------------
    //                                       Embedded Filter
    //                                       ---------------
    protected void initEmbeddedFilter(FilterConfig filterConfig) throws ServletException {
        loggingFilter = createRequestLoggingFilter();
        loggingFilter.init(filterConfig);
        routingFilter = createRequestRoutingFilter();
        routingFilter.init(filterConfig);
    }

    protected RequestLoggingFilter createRequestLoggingFilter() {
        final FwWebDirection webDirection = getAssistantDirector().assistWebDirection();
        final ActionAdjustmentProvider adjustmentProvider = webDirection.assistActionAdjustmentProvider();
        final RequestManager requestManager = getRequestManager();
        return new RequestLoggingFilter() {
            @Override
            protected boolean isTargetPath(HttpServletRequest request) {
                // forced routings also should be logging control target
                if (adjustmentProvider.isForcedRoutingTarget(request, requestManager.getRequestPath())) {
                    return true;
                } else {
                    return super.isTargetPath(request);
                }
            }
        };
    }

    protected RequestRoutingFilter createRequestRoutingFilter() {
        return new RequestRoutingFilter();
    }

    // -----------------------------------------------------
    //                                           Filter Hook
    //                                           -----------
    protected void initFilterHook(FilterConfig filterConfig) throws ServletException {
        for (FilterHook hook : assistInsideHookList()) {
            hook.init(filterConfig);
        }
        for (FilterHook hook : assistOutsideHookList()) {
            hook.init(filterConfig);
        }
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
        viaOutsideHook(request, response, chain); // #to_action
    }

    // -----------------------------------------------------
    //                                      via Outside Hook
    //                                      ----------------
    protected void viaOutsideHook(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        viaOutsideHookDeque(request, response, chain, prepareOutsideHook()); // #to_action
    }

    protected Deque<FilterHook> prepareOutsideHook() { // null allowed (if no hook)
        final List<FilterHook> listenerList = assistOutsideHookList();
        return !listenerList.isEmpty() ? new LinkedList<FilterHook>(listenerList) : null;
    }

    protected void viaOutsideHookDeque(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Deque<FilterHook> deque)
            throws IOException, ServletException {
        final FilterHook next = deque != null ? deque.poll() : null; // null if no hook
        if (next != null) {
            next.hook(request, response, (argreq, argres) -> {
                viaOutsideHookDeque(argreq, argres, chain, deque);
            }); // e.g. MDC
        } else {
            viaEmbeddedFilter(request, response, chain); // #to_action
        }
    }

    // -----------------------------------------------------
    //                                   via Embedded Filter
    //                                   -------------------
    protected void viaEmbeddedFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        loggingFilter.doFilter(request, response, (argreq, argres) -> {
            enableAccessLogIfNeeds();
            viaInsideHook((HttpServletRequest) argreq, (HttpServletResponse) argres, chain);
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

    // -----------------------------------------------------
    //                                       via Inside Hook
    //                                       ---------------
    protected void viaInsideHook(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        viaInsideHookDeque(request, response, chain, prepareInsideHookDeque());
    }

    protected Deque<FilterHook> prepareInsideHookDeque() { // null allowed (if no hook)
        final List<FilterHook> listenerList = assistInsideHookList();
        return !listenerList.isEmpty() ? new LinkedList<FilterHook>(listenerList) : null;
    }

    protected void viaInsideHookDeque(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Deque<FilterHook> deque)
            throws IOException, ServletException {
        final FilterHook next = deque != null ? deque.poll() : null; // null if no hook
        if (next != null) {
            next.hook(request, response, (argreq, argres) -> {
                viaInsideHookDeque(argreq, argres, chain, deque);
            }); // e.g. original hook
        } else {
            routingFilter.doFilter(request, response, prepareFinalChain(chain)); // #to_action
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
        destroyEmbeddedFilter();
        destroyFilterHook();
    }

    // -----------------------------------------------------
    //                                       Embedded Filter
    //                                       ---------------
    protected void destroyEmbeddedFilter() {
        if (loggingFilter != null) { // just in case
            loggingFilter.destroy();
        }
        if (routingFilter != null) { // just in case
            routingFilter.destroy();
        }
    }

    // -----------------------------------------------------
    //                                           Filter Hook
    //                                           -----------
    protected void destroyFilterHook() {
        assistInsideHookList().forEach(hook -> hook.destroy());
        assistOutsideHookList().forEach(hook -> hook.destroy());
    }

    // ===================================================================================
    //                                                                           Component
    //                                                                           =========
    protected FwAssistantDirector getAssistantDirector() {
        return ContainerUtil.getComponent(FwAssistantDirector.class);
    }

    protected FwWebDirection assistWebDirection() {
        return getAssistantDirector().assistWebDirection();
    }

    protected List<FilterHook> assistInsideHookList() {
        return assistWebDirection().assistInsideFilterHookList();
    }

    protected List<FilterHook> assistOutsideHookList() {
        return assistWebDirection().assistOutsideFilterHookList();
    }

    protected MessageResourcesHolder getMessageResourceHolder() {
        return ContainerUtil.getComponent(MessageResourcesHolder.class);
    }

    protected RequestManager getRequestManager() {
        return ContainerUtil.getComponent(RequestManager.class);
    }
}
