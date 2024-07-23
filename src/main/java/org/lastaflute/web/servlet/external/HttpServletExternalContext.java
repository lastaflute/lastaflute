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
package org.lastaflute.web.servlet.external;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.lastaflute.di.core.ExternalContext;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * @author modified by jflute (originated in Seasar)
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class HttpServletExternalContext implements ExternalContext {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final Map LAZY_MARK = new HashMap();

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // #hope: resolve generic by jflute
    protected final ThreadLocal requests = new ThreadLocal();
    protected final ThreadLocal responses = new ThreadLocal();
    protected final ThreadLocal requestMaps = new ThreadLocal();
    protected final ThreadLocal requestHeaderMaps = new ThreadLocal();
    protected final ThreadLocal requestHeaderValuesMaps = new ThreadLocal();
    protected final ThreadLocal requestParameterMaps = new ThreadLocal();
    protected final ThreadLocal requestParameterValuesMaps = new ThreadLocal();
    protected final ThreadLocal requestCookieMaps = new ThreadLocal();
    protected final ThreadLocal sessionMaps = new ThreadLocal();

    protected ServletContext application;

    // ===================================================================================
    //                                                                      Implementation
    //                                                                      ==============
    @Override
    public Object getRequest() {
        return getHttpServletRequest();
    }

    protected HttpServletRequest getHttpServletRequest() {
        return (HttpServletRequest) requests.get();
    }

    @Override
    public void setRequest(Object request) {
        requests.set(request);
        if (request == null) {
            requestMaps.set(new HashMap());
            requestHeaderMaps.set(Collections.EMPTY_MAP);
            requestHeaderValuesMaps.set(Collections.EMPTY_MAP);
            requestCookieMaps.set(Collections.EMPTY_MAP);
            requestParameterMaps.set(Collections.EMPTY_MAP);
            requestParameterValuesMaps.set(Collections.EMPTY_MAP);
            sessionMaps.set(new HashMap());
        } else {
            final HttpServletRequest req = (HttpServletRequest) request;
            requestMaps.set(new ServletRequestMap(req));
            requestHeaderMaps.set(new ServletRequestHeaderMap(req));
            requestHeaderValuesMaps.set(new ServletRequestHeaderValuesMap(req));
            requestCookieMaps.set(new CookieMap(req));
            requestParameterMaps.set(LAZY_MARK); // lazy initialize
            requestParameterValuesMaps.set(LAZY_MARK); // lazy initialize
            sessionMaps.set(LAZY_MARK); // lazy initialize
        }
    }

    @Override
    public Object getResponse() {
        return responses.get();
    }

    @Override
    public void setResponse(Object response) {
        responses.set(response);
    }

    @Override
    public Object getSession() {
        return getHttpSession();
    }

    protected HttpSession getHttpSession() {
        HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            return null;
        }
        return request.getSession();
    }

    @Override
    public Object getApplication() {
        return application;
    }

    @Override
    public void setApplication(Object application) {
        if (!(application instanceof ServletContext)) {
            throw new IllegalArgumentException("application:" + application);
        }
        this.application = (ServletContext) application;
    }

    @Override
    public Map getApplicationMap() {
        return new ServletApplicationMap(application);
    }

    @Override
    public Map getInitParameterMap() {
        return new ServletInitParameterMap(application);
    }

    @Override
    public Map getRequestCookieMap() {
        Map requestCookieMap = (Map) requestCookieMaps.get();
        if (requestCookieMap == null) {
            requestCookieMap = Collections.EMPTY_MAP;
            requestCookieMaps.set(requestCookieMap);
        }
        return requestCookieMap;
    }

    @Override
    public Map getRequestHeaderMap() {
        Map requestHeaderMap = (Map) requestHeaderMaps.get();
        if (requestHeaderMap == null) {
            requestHeaderMap = Collections.EMPTY_MAP;
            requestCookieMaps.set(requestHeaderMap);
        }
        return requestHeaderMap;
    }

    @Override
    public Map getRequestHeaderValuesMap() {
        Map requestHeaderMap = (Map) requestHeaderValuesMaps.get();
        if (requestHeaderMap == null) {
            requestHeaderMap = Collections.EMPTY_MAP;
            requestHeaderMaps.set(requestHeaderMap);
        }
        return requestHeaderMap;
    }

    @Override
    public Map getRequestMap() {
        Map requestMap = (Map) requestMaps.get();
        if (requestMap == null) {
            requestMap = new HashMap();
            requestMaps.set(requestMap);
        }
        return requestMap;
    }

    @Override
    public Map getRequestParameterMap() {
        Map requestParameterMap = (Map) requestParameterMaps.get();
        if (requestParameterMap == null) {
            requestParameterMap = Collections.EMPTY_MAP;
            requestParameterMaps.set(requestParameterMap);
        } else if (requestParameterMap == LAZY_MARK) {
            requestParameterMap = new ServletRequestParameterMap(getHttpServletRequest());
            requestParameterMaps.set(requestParameterMap);
        }
        return requestParameterMap;
    }

    @Override
    public Map getRequestParameterValuesMap() {
        Map requestParameterValuesMap = (Map) requestParameterValuesMaps.get();
        if (requestParameterValuesMap == null) {
            requestParameterValuesMap = Collections.EMPTY_MAP;
            requestParameterValuesMaps.set(requestParameterValuesMap);
        } else if (requestParameterValuesMap == LAZY_MARK) {
            requestParameterValuesMap = new ServletRequestParameterValuesMap(getHttpServletRequest());
            requestParameterValuesMaps.set(requestParameterValuesMap);
        }
        return requestParameterValuesMap;
    }

    @Override
    public Map getSessionMap() {
        Map sessionMap = (Map) sessionMaps.get();
        if (sessionMap == null) {
            sessionMap = new HashMap();
            sessionMaps.set(sessionMap);
        } else if (sessionMap == LAZY_MARK) {
            sessionMap = new HttpSessionMap(getHttpServletRequest());
            sessionMaps.set(sessionMap);
        }
        return sessionMap;
    }
}