/*
 * Copyright 2015-2021 the original author or authors.
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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.web.exception.Forced404NotFoundException;
import org.lastaflute.web.path.ActionAdjustmentProvider;
import org.lastaflute.web.path.ActionFoundPathHandler;
import org.lastaflute.web.path.ActionPathResolver;
import org.lastaflute.web.path.MappingPathResource;
import org.lastaflute.web.path.MappingResolutionResult;
import org.lastaflute.web.path.RoutingParamPath;
import org.lastaflute.web.path.restful.verifier.RestfulMappingVerifier;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.ruts.ActionRequestProcessor;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.process.pathparam.RequestPathParam;
import org.lastaflute.web.ruts.process.pathparam.RequestPathParamAnalyzer;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.util.LaActionExecuteUtil;
import org.lastaflute.web.util.LaModuleConfigUtil;
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
    // -----------------------------------------------------
    //                                                Cached
    //                                                ------
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
     * The cache of action adjustment provider, which can be lazy-loaded when you get it.
     * Don't use these variables directly, you should use the getter. (NotNull: after lazy-load)
     */
    protected ActionAdjustmentProvider cachedActionAdjustmentProvider;

    /**
     * The cache of path parameter analyzer, which can be lazy-loaded when you get it.
     * Don't use these variables directly, you should use the getter. (NotNull: after lazy-load)
     */
    protected RequestPathParamAnalyzer cachedPathParamAnalyzer;

    // -----------------------------------------------------
    //                                             Processor
    //                                             ---------
    /** The processor of action request, lazy loaded so use the getter. (NotNull: after lazy-load) */
    protected ActionRequestProcessor lazyLoadedProcessor; // lazy loaded

    // -----------------------------------------------------
    //                                               Restful
    //                                               -------
    private final RestfulMappingVerifier restfulMappingVerifier = newRestfulMappingVerifier();

    protected RestfulMappingVerifier newRestfulMappingVerifier() {
        return new RestfulMappingVerifier();
    }

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
        handleForced404NotFoundRouting(httpReq, requestPath);
        if (!isRoutingTarget(httpReq, requestPath)) { // e.g. foo.jsp, foo.do, foo.js, foo.css
            chain.doFilter(httpReq, httpRes);
            return;
        }
        // no extension here (may be LastaFlute URL)
        final ActionPathResolver resolver = getRequestManager().getActionPathResolver();
        final MappingPathResource pathResource; // not null, keep for logging
        try {
            final String contextPath = extractContextPath(httpReq);
            final ActionFoundPathHandler handler = createActionFoundPathHandler(httpReq, httpRes, contextPath); // (#to_action)
            final MappingResolutionResult result = resolver.handleActionPath(requestPath, handler); // #to_action
            if (result.isPathHandled()) {
                return;
            }
            pathResource = result.getPathResource();
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            } else if (e instanceof ServletException) {
                throw (ServletException) e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else { // no way, just in case
                throw new IllegalStateException("*No way", e);
            }
        }
        // no routing here
        showNoRouting(pathResource, resolver); // for developer
        handleNoRoutingRequest(httpReq, requestPath); // 404 if it needs
        chain.doFilter(servReq, servRes); // to next filter outside LastaFlute
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

    protected void handleForced404NotFoundRouting(HttpServletRequest request, String requestPath) {
        final ActionAdjustmentProvider adjustmentProvider = getActionAdjustmentProvider();
        if (adjustmentProvider.isForced404NotFoundRouting(request, requestPath)) {
            throw new Forced404NotFoundException("Forcedly 404 not found routing: " + requestPath, UserMessages.empty());
        }
    }

    protected boolean isRoutingTarget(HttpServletRequest request, String requestPath) {
        final ActionAdjustmentProvider adjustmentProvider = getActionAdjustmentProvider();
        if (adjustmentProvider.isForcedRoutingExcept(request, requestPath)) { // you can adjust it
            return false;
        }
        if (adjustmentProvider.isForcedRoutingTarget(request, requestPath)) { // you can adjust it
            return true;
        }
        return !isExtensionUrlPossible(request, requestPath); // default determination
    }

    protected boolean isExtensionUrlPossible(HttpServletRequest request, String requestPath) {
        // *added condition 'endsWith()' to allow /member/1.2.3/
        // (you can receive 'urlPattern' that contains dot '.')
        //
        // true  : e.g. foo.jsp, foo.do, foo.js, foo.css, /member/1.2.3
        // false : e.g. /member/list/, /member/list, /member/1.2.3/
        return requestPath.indexOf('.') >= 0 && !requestPath.endsWith("/");
    }

    protected String extractContextPath(HttpServletRequest req) {
        final String contextPath = req.getContextPath();
        return contextPath.equals("/") ? "" : contextPath;
    }

    protected ActionFoundPathHandler createActionFoundPathHandler(HttpServletRequest request, HttpServletResponse response,
            String contextPath) {
        return (pathResource, actionName, paramPath, execByParam) -> {
            return routingToAction(request, response, contextPath, pathResource, actionName, paramPath, execByParam);
        };
    }

    protected void showNoRouting(MappingPathResource pathResource, ActionPathResolver resolver) { // for debug
        if (logger.isDebugEnabled()) {
            if (!pathResource.getRequestPath().contains(".")) { // e.g. routing target can be adjusted so may be .jpg
                logger.debug(resolver.prepareNoRoutingMessage(pathResource));
            }
        }
    }

    protected void handleNoRoutingRequest(HttpServletRequest request, String requestPath) {
        final ActionAdjustmentProvider adjustmentProvider = getActionAdjustmentProvider();
        if (adjustmentProvider.isNoRoutingRequestAs404NotFound(request, requestPath)) {
            throw new Forced404NotFoundException("No routing request: " + requestPath, UserMessages.empty());
        }
    }

    // ===================================================================================
    //                                                                   Routing to Action
    //                                                                   =================
    protected boolean routingToAction(HttpServletRequest request, HttpServletResponse response, String contextPath,
            MappingPathResource pathResource, String actionName, RoutingParamPath paramPath, ActionExecute execByParam)
            throws IOException, ServletException {
        if (execByParam != null) { // already found
            processAction(pathResource, request, response, execByParam, paramPath); // #to_action
            return true;
        }
        final OptionalThing<ActionExecute> found = LaActionExecuteUtil.findActionExecute(actionName, request);
        if (found.isPresent()) { // not use lambda because of throws definition
            final ActionExecute execute = found.get();
            final String requestPath = pathResource.getRequestPath();
            if (needsTrailingSlashRedirect(request, requestPath, execute)) { // index() or by request parameter
                redirectWithTrailingSlash(request, response, contextPath, requestPath);
            } else {
                processAction(pathResource, request, response, execute, RoutingParamPath.EMPTY); // #to_action
            }
            return true;
        } else { // e.g. not found index()
            return false;
        }
    }

    // -----------------------------------------------------
    //                                        Trailing Slash
    //                                        --------------
    protected boolean needsTrailingSlashRedirect(HttpServletRequest request, String requestPath, ActionExecute execute) {
        if (isOutOfTrailingSlashRedirect(request, requestPath, execute)) {
            return false;
        }
        if (isSuppressTrailingSlashRedirect(request, requestPath, execute)) {
            return false;
        }
        return isNonTrailingSlashRequest(request, requestPath, execute);
    }

    protected boolean isOutOfTrailingSlashRedirect(HttpServletRequest request, String requestPath, ActionExecute execute) {
        return execute.isApiExecute(); // API does not need it (SEO handling)
    }

    protected boolean isSuppressTrailingSlashRedirect(HttpServletRequest request, String requestPath, ActionExecute execute) {
        return getActionAdjustmentProvider().isSuppressTrailingSlashRedirect(request, requestPath, execute);
    }

    protected boolean isNonTrailingSlashRequest(HttpServletRequest request, String requestPath, ActionExecute execute) {
        return "GET".equalsIgnoreCase(request.getMethod()) && !requestPath.endsWith("/"); // default determination
    }

    protected void redirectWithTrailingSlash(HttpServletRequest request, HttpServletResponse response, String contextPath,
            String requestPath) throws IOException {
        final String queryString = request.getQueryString();
        final String redirectUrl = contextPath + requestPath + "/" + (queryString != null ? "?" + queryString : "");
        logger.debug("...Redirecting (with trailing slash) to: {}", redirectUrl);
        getRequestManager().getResponseManager().movedPermanently(HtmlResponse.fromRedirectPathAsIs(redirectUrl));
    }

    // ===================================================================================
    //                                                                      Process Action
    //                                                                      ==============
    protected void processAction(MappingPathResource pathResource, HttpServletRequest request, HttpServletResponse response,
            ActionExecute execute, RoutingParamPath paramPath) throws IOException, ServletException {
        if (logger.isDebugEnabled()) {
            logger.debug("...Routing to action: name={} params={}", execute.getActionMapping().getActionName(), paramPath);
            logger.debug(" by the mapping path: {}", pathResource.getMappingPath());
        }
        verifyRestfulMapping(pathResource, execute, paramPath);
        LaActionExecuteUtil.setActionExecute(execute); // for e.g. tag-library use
        getRequestProcessor().process(execute, analyzePathParam(execute, paramPath)); // #to_action
    }

    protected void verifyRestfulMapping(MappingPathResource pathResource, ActionExecute execute, RoutingParamPath paramPath) {
        restfulMappingVerifier.verifyRestfulMapping(pathResource, execute, paramPath);
    }

    // -----------------------------------------------------
    //                                     Request PathParam
    //                                     -----------------
    protected RequestPathParam analyzePathParam(ActionExecute execute, RoutingParamPath paramPath) {
        return getPathParamAnalyzer().analyzePathParam(execute, paramPath);
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

    protected ActionAdjustmentProvider getActionAdjustmentProvider() {
        if (cachedActionAdjustmentProvider != null) {
            return cachedActionAdjustmentProvider;
        }
        synchronized (this) {
            if (cachedActionAdjustmentProvider != null) {
                return cachedActionAdjustmentProvider;
            }
            cachedActionAdjustmentProvider = getAssistantDirector().assistWebDirection().assistActionAdjustmentProvider();
        }
        return cachedActionAdjustmentProvider;
    }

    protected RequestPathParamAnalyzer getPathParamAnalyzer() {
        if (cachedPathParamAnalyzer != null) {
            return cachedPathParamAnalyzer;
        }
        synchronized (this) {
            if (cachedPathParamAnalyzer != null) {
                return cachedPathParamAnalyzer;
            }
            cachedPathParamAnalyzer = new RequestPathParamAnalyzer(getRequestManager());
        }
        return cachedPathParamAnalyzer;
    }
}
