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
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.web.direction.FwWebDirection;
import org.lastaflute.web.path.ActionAdjustmentProvider;
import org.lastaflute.web.servlet.filter.accesslog.AccessLogHandler;
import org.lastaflute.web.servlet.filter.accesslog.AccessLogResource;
import org.lastaflute.web.servlet.filter.hook.FilterHook;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * Outside hook and logging.
 * @author jflute
 */
public class LastaShowbaseFilter implements Filter {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected RequestLoggingFilter loggingFilter;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LastaShowbaseFilter() {
    }

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    public void init(FilterConfig filterConfig) throws ServletException {
        initEmbeddedFilter(filterConfig);
        initFilterHook(filterConfig);
    }

    // -----------------------------------------------------
    //                                       Embedded Filter
    //                                       ---------------
    protected void initEmbeddedFilter(FilterConfig filterConfig) throws ServletException {
        loggingFilter = createRequestLoggingFilter();
        loggingFilter.init(filterConfig);
    }

    protected RequestLoggingFilter createRequestLoggingFilter() {
        final FwWebDirection webDirection = getAssistantDirector().assistWebDirection();
        final ActionAdjustmentProvider adjustmentProvider = webDirection.assistActionAdjustmentProvider();
        final RequestManager requestManager = getRequestManager();
        final RequestLoggingFilter filter = newRequestLoggingFilter();
        filter.determineRoutingTarget((request, embeddedDeterminer) -> {
            // forced routings also should be logging control target
            final String requestPath = requestManager.getRequestPath();
            if (adjustmentProvider.isForcedRoutingExcept(request, requestPath)) {
                return false;
            }
            if (adjustmentProvider.isForcedRoutingTarget(request, requestPath)) {
                return true;
            }
            return embeddedDeterminer.determineEmbedded(request);
        });
        filter.suppressServerErrorLogging(cause -> {
            return adjustmentProvider.isSuppressServerErrorLogging(cause);
        });
        return filter;
    }

    protected RequestLoggingFilter newRequestLoggingFilter() {
        return new RequestLoggingFilter();
    }

    // -----------------------------------------------------
    //                                           Filter Hook
    //                                           -----------
    protected void initFilterHook(FilterConfig filterConfig) throws ServletException {
        for (FilterHook hook : assistOutsideHookList()) {
            hook.init(filterConfig);
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
            toNextChain(argreq, argres, chain); // #to_action
        });
    }

    protected void enableAccessLogIfNeeds() { // for e.g. access log
        if (loggingFilter.isAlreadyBegun()) { // means handler to be set here always be cleared later
            final AccessLogHandler handler = assistWebDirection().assistAccessLogHandler();
            if (handler != null) { // specified by application
                RequestLoggingFilter.setAccessLogHandlerOnThread((request, response, cause, before) -> {
                    handler.handle(createAccessLogResource(request, response, cause, before));
                });
            }
        }
    }

    protected AccessLogResource createAccessLogResource(HttpServletRequest request, HttpServletResponse response, Throwable cause,
            long before) {
        return new AccessLogResource(request, response, cause, before);
    }

    protected void toNextChain(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        chain.doFilter(request, response); // #to_action
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
    }

    // -----------------------------------------------------
    //                                           Filter Hook
    //                                           -----------
    protected void destroyFilterHook() {
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

    protected List<FilterHook> assistOutsideHookList() {
        return assistWebDirection().assistOutsideFilterHookList();
    }

    protected RequestManager getRequestManager() {
        return ContainerUtil.getComponent(RequestManager.class);
    }
}
