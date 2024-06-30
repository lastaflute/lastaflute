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
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.web.direction.FwWebDirection;
import org.lastaflute.web.servlet.filter.hook.FilterHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inside hook and routing.
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
        routingFilter = createRequestRoutingFilter();
        routingFilter.init(filterConfig);
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
    }

    // -----------------------------------------------------
    //                                             Show Boot
    //                                             ---------
    protected void showBoot(FwAssistantDirector assistantDirector) {
        if (logger.isInfoEnabled()) {
            final FwCoreDirection coreDirection = assistantDirector.assistCoreDirection();
            final String domainTitle = coreDirection.assistDomainTitle();
            final String environmentTitle = coreDirection.assistEnvironmentTitle();
            final String frameworkDebugExp = coreDirection.isFrameworkDebug() ? " *frameworkDebug" : "";
            logger.info("_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/");
            logger.info(" the system has been initialized:");
            logger.info("");
            logger.info("  -> " + domainTitle + " (" + environmentTitle + ")" + frameworkDebugExp);
            logger.info("_/_/_/_/_/_/_/_/_/_/");
        }
    }

    // ===================================================================================
    //                                                                          doFilter()
    //                                                                          ==========
    public void doFilter(ServletRequest servReq, ServletResponse servRes, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) servReq;
        final HttpServletResponse response = (HttpServletResponse) servRes;
        viaInsideHook(request, response, chain); // #to_action
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
            viaEmbeddedFilter(request, response, chain); // #to_action
        }
    }

    // -----------------------------------------------------
    //                                   via Embedded Filter
    //                                   -------------------
    protected void viaEmbeddedFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        routingFilter.doFilter(request, response, prepareFinalChain(chain)); // #to_action
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
        if (routingFilter != null) { // just in case
            routingFilter.destroy();
        }
    }

    // -----------------------------------------------------
    //                                           Filter Hook
    //                                           -----------
    protected void destroyFilterHook() {
        assistInsideHookList().forEach(hook -> hook.destroy());
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
}
