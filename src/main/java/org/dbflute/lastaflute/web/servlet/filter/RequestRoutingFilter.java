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
package org.dbflute.lastaflute.web.servlet.filter;

import java.io.IOException;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dbflute.lastaflute.core.direction.FwAssistantDirector;
import org.dbflute.lastaflute.core.util.ContainerUtil;
import org.dbflute.lastaflute.web.direction.OptionalWebDirection;
import org.dbflute.lastaflute.web.path.ActionAdjustmentProvider;
import org.dbflute.lastaflute.web.path.ActionFoundPathHandler;
import org.dbflute.lastaflute.web.path.ActionPathResolver;
import org.dbflute.lastaflute.web.ruts.ActionRequestProcessor;
import org.dbflute.lastaflute.web.ruts.config.ActionExecute;
import org.dbflute.lastaflute.web.ruts.process.ActionRequestResource;
import org.dbflute.lastaflute.web.ruts.process.RequestUrlParamAnalyzer;
import org.dbflute.lastaflute.web.servlet.request.RequestManager;
import org.dbflute.lastaflute.web.util.LaActionExecuteUtil;
import org.dbflute.lastaflute.web.util.LaModuleConfigUtil;
import org.dbflute.optional.OptionalThing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class RequestRoutingFilter implements Filter {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(RequestRoutingFilter.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /**
     * The cache of assistant director, which can be lazy-loaded when you get it.
     * Don't use these variables directly, you should use the getter. (NotNull: after lazy-load)
     */
    protected FwAssistantDirector cachedAssistantDirector;

    /**
     * The cache of request manager, which can be lazy-loaded when you get it.
     * Don't use these variables directly, you should use the getter. (NotNull: after lazy-load)
     */
    protected RequestManager cachedRequestManager;

    /**
     * The cache of URL parameter analyzer, which can be lazy-loaded when you get it.
     * Don't use these variables directly, you should use the getter. (NotNull: after lazy-load)
     */
    protected RequestUrlParamAnalyzer cachedUrlParamAnalyzer;

    /** The processor of action request, lazy loaded so use the getter. (NotNull: after lazy-load) */
    protected ActionRequestProcessor lazyLoadedProcessor; // lazy loaded

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    // ===================================================================================
    //                                                                              Filter
    //                                                                              ======
    @Override
    public void doFilter(ServletRequest servReq, ServletResponse servRes, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpReq = (HttpServletRequest) servReq;
        final HttpServletResponse httpRes = (HttpServletResponse) servRes;
        final String requestPath = extractActionRequestPath(httpReq);
        if (!isRoutingTarget(httpReq, requestPath)) { // e.g. foo.jsp, foo.do, foo.js, foo.css
            chain.doFilter(httpReq, httpRes);
            return;
        }
        // no extension here (may be SAStruts URL)
        final ActionPathResolver resolver = ContainerUtil.getComponent(ActionPathResolver.class);
        try {
            final String contextPath = extractContextPath(httpReq);
            final ActionFoundPathHandler handler = createActionPathHandler(httpReq, httpRes, contextPath); // (#to_action)
            if (resolver.handleActionPath(requestPath, handler)) { // #to_action
                return;
            }
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            } else if (e instanceof ServletException) {
                throw (ServletException) e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else { // no way
                throw new IllegalStateException(e);
            }
        }
        // no routing here
        showExpectedRouting(requestPath, resolver);
        chain.doFilter(servReq, servRes);
    }

    protected String extractActionRequestPath(HttpServletRequest request) {
        // /= = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
        // request specification:
        //   requestURI  : /dockside/member/list/foo%2fbar/
        //   servletPath : /member/list/foo/bar/
        //
        // so uses requestURI but it needs to remove context path
        //  -> /member/list/foo%2fbar/
        // = = = = = = = = = =/
        return getRequestManager().getRequestPath();
    }

    protected String extractContextPath(HttpServletRequest req) {
        final String contextPath = req.getContextPath();
        return contextPath.equals("/") ? "" : contextPath;
    }

    protected boolean isRoutingTarget(HttpServletRequest request, String requestPath) {
        final ActionAdjustmentProvider adjustmentProvider = assistActionAdjustmentProvider();
        if (adjustmentProvider.isForcedRoutingTarget(request, requestPath)) { // you can adjust it
            return true;
        }
        return !isExtensionUrlPossible(request, requestPath); // default determination
    }

    protected ActionAdjustmentProvider assistActionAdjustmentProvider() {
        final OptionalWebDirection direction = getAssistantDirector().assistOptionalWebDirection();
        return direction.assistActionAdjustmentProvider();
    }

    protected boolean isExtensionUrlPossible(HttpServletRequest request, String requestPath) {
        // *added condition 'endsWith()' to allow /member/1.2.3/
        // (you can receive 'urlPattern' that contains dot '.')
        //
        // true  : e.g. foo.jsp, foo.do, foo.js, foo.css, /member/1.2.3
        // false : e.g. /member/list/, /member/list, /member/1.2.3/
        return requestPath.indexOf('.') >= 0 && !requestPath.endsWith("/");
    }

    protected ActionFoundPathHandler createActionPathHandler(HttpServletRequest httpReq, HttpServletResponse httpRes, String contextPath) {
        return (requestPath, actionName, paramPath, execByParam) -> {
            return routingToAction(httpReq, httpRes, contextPath, requestPath, actionName, paramPath, execByParam);
        };
    }

    protected void showExpectedRouting(String requestPath, ActionPathResolver resolver) { // for debug
        if (logger.isDebugEnabled()) {
            if (!requestPath.contains(".")) { // e.g. routing target can be adjusted so may be .jpg
                logger.debug(resolver.prepareExpectedRoutingMessage(requestPath));
            }
        }
    }

    // ===================================================================================
    //                                                                   Routing to Action
    //                                                                   =================
    protected boolean routingToAction(HttpServletRequest request, HttpServletResponse response, String contextPath, String requestPath,
            String actionName, String paramPath, ActionExecute execute) throws IOException, ServletException {
        if (execute == null) {
            if (needsSlashRedirect(request, requestPath, execute)) {
                // TODO jflute lastaflute: [E] thinking: redirectWithSlash() needed? or arrange
                redirectWithSlash(request, response, contextPath, requestPath);
                return true;
            } else {
                final OptionalThing<ActionExecute> foundExecute = LaActionExecuteUtil.findActionExecute(actionName, request);
                if (foundExecute.isPresent()) { // not use lambda because of throws definition
                    processAction(request, response, foundExecute.get(), null); // #to_action
                    return true;
                } else { // e.g. not found index()
                    return false;
                }
            }
        } else {
            processAction(request, response, execute, paramPath); // #to_action
            return true;
        }
    }

    protected boolean needsSlashRedirect(HttpServletRequest request, String requestPath, ActionExecute executeConfig) {
        if (isForcedSuppressRedirectWithSlash(request, requestPath, executeConfig)) {
            return false;
        }
        return "GET".equalsIgnoreCase(request.getMethod()) && !requestPath.endsWith("/"); // default determination
    }

    protected boolean isForcedSuppressRedirectWithSlash(HttpServletRequest request, String requestPath, ActionExecute executeConfig) {
        return assistActionAdjustmentProvider().isForcedSuppressRedirectWithSlash(request, requestPath, executeConfig);
    }

    protected void redirectWithSlash(HttpServletRequest httpReq, HttpServletResponse httpRes, String contextPath, String requestPath)
            throws IOException {
        final String queryString = httpReq.getQueryString();
        final String redirectUrl = contextPath + requestPath + "/" + (queryString != null ? "?" + queryString : "");
        logger.debug("...Redirecting (with slash) to: {}", redirectUrl);
        httpRes.sendRedirect(redirectUrl);
    }

    // ===================================================================================
    //                                                                      Process Action
    //                                                                      ==============
    protected void processAction(HttpServletRequest request, HttpServletResponse response, ActionExecute execute, String paramPath)
            throws IOException, ServletException {
        logger.debug("...Routing to action: name={} params={}", execute.getActionMapping().getActionName(), paramPath);
        LaActionExecuteUtil.setActionExecute(request, execute); // for e.g. tag-library use
        getRequestProcessor().process(execute, prepareActionRequestResource(execute, paramPath)); // #to_action
    }

    // -----------------------------------------------------
    //                                      Request Resource
    //                                      ----------------
    protected ActionRequestResource prepareActionRequestResource(ActionExecute execute, String paramPath) {
        return newActionRequestResource(analyzeUrlParamValue(execute, paramPath));
    }

    protected Map<Integer, Object> analyzeUrlParamValue(ActionExecute execute, String paramPath) {
        return getUrlParamAnalyzer().analyzeUrlParamValue(execute, paramPath);
    }

    protected ActionRequestResource newActionRequestResource(Map<Integer, Object> urlParamValueMap) {
        return new ActionRequestResource(urlParamValueMap);
    }

    // -----------------------------------------------------
    //                                     Request Processor
    //                                     -----------------
    protected ActionRequestProcessor getRequestProcessor() throws ServletException {
        if (lazyLoadedProcessor == null) {
            synchronized (this) {
                prepareRequestProcessorIfNeeds();
            }
        }
        return lazyLoadedProcessor;
    }

    protected void prepareRequestProcessorIfNeeds() throws ServletException {
        if (lazyLoadedProcessor == null) { // re-confirm
            lazyLoadedProcessor = newActionRequestProcessor();
            lazyLoadedProcessor.initialize(LaModuleConfigUtil.getModuleConfig());
        }
    }

    protected ActionRequestProcessor newActionRequestProcessor() {
        return new ActionRequestProcessor();
    }

    // ===================================================================================
    //                                                                             Destroy
    //                                                                             =======
    @Override
    public void destroy() {
    }

    // ===================================================================================
    //                                                                           Component
    //                                                                           =========
    protected FwAssistantDirector getAssistantDirector() {
        if (cachedAssistantDirector != null) {
            return cachedAssistantDirector;
        }
        synchronized (this) {
            if (cachedAssistantDirector != null) {
                return cachedAssistantDirector;
            }
            cachedAssistantDirector = ContainerUtil.getComponent(FwAssistantDirector.class);
        }
        return cachedAssistantDirector;
    }

    protected RequestManager getRequestManager() {
        if (cachedRequestManager != null) {
            return cachedRequestManager;
        }
        synchronized (this) {
            if (cachedRequestManager != null) {
                return cachedRequestManager;
            }
            cachedRequestManager = ContainerUtil.getComponent(RequestManager.class);
        }
        return cachedRequestManager;
    }

    protected RequestUrlParamAnalyzer getUrlParamAnalyzer() {
        if (cachedUrlParamAnalyzer != null) {
            return cachedUrlParamAnalyzer;
        }
        synchronized (this) {
            if (cachedUrlParamAnalyzer != null) {
                return cachedUrlParamAnalyzer;
            }
            cachedUrlParamAnalyzer = new RequestUrlParamAnalyzer(getRequestManager());
        }
        return cachedUrlParamAnalyzer;
    }
}
