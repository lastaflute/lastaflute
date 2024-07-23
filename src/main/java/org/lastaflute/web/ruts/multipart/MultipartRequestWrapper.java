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
package org.lastaflute.web.ruts.multipart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.Srl;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;

/**
 * @author modified by jflute (originated in Struts)
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class MultipartRequestWrapper implements HttpServletRequest {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final HttpServletRequest wrapped;
    protected final Map<String, String[]> parameters;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public MultipartRequestWrapper(HttpServletRequest wrapped) {
        this.wrapped = wrapped;
        this.parameters = new HashMap<String, String[]>();
    }

    // ===================================================================================
    //                                                                      Implementation
    //                                                                      ==============
    @Override
    public String getParameter(String name) {
        String value = wrapped.getParameter(name);
        if (value == null) {
            final String[] multipleValue = parameters.get(name);
            if ((multipleValue != null) && (multipleValue.length > 0)) {
                value = multipleValue[0];
            }
        }
        return value;
    }

    @Override
    public Enumeration getParameterNames() {
        final Enumeration baseParams = wrapped.getParameterNames();
        final List<Object> list = new ArrayList<Object>();
        while (baseParams.hasMoreElements()) {
            list.add(baseParams.nextElement());
        }
        final Collection<String> multipartParams = parameters.keySet();
        final Iterator<String> iterator = multipartParams.iterator();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return Collections.enumeration(list);
    }

    public String[] getParameterValues(String name) {
        String[] value = wrapped.getParameterValues(name);
        if (value == null) {
            value = (String[]) parameters.get(name);
        }
        return value;
    }

    public Object getAttribute(String name) {
        return wrapped.getAttribute(name);
    }

    public Enumeration getAttributeNames() {
        return wrapped.getAttributeNames();
    }

    public String getCharacterEncoding() {
        return wrapped.getCharacterEncoding();
    }

    public int getContentLength() {
        return wrapped.getContentLength();
    }

    public String getContentType() {
        return wrapped.getContentType();
    }

    public ServletInputStream getInputStream() throws IOException {
        return wrapped.getInputStream();
    }

    public String getProtocol() {
        return wrapped.getProtocol();
    }

    public String getScheme() {
        return wrapped.getScheme();
    }

    public String getServerName() {
        return wrapped.getServerName();
    }

    public int getServerPort() {
        return wrapped.getServerPort();
    }

    public BufferedReader getReader() throws IOException {
        return wrapped.getReader();
    }

    public String getRemoteAddr() {
        return wrapped.getRemoteAddr();
    }

    public String getRemoteHost() {
        return wrapped.getRemoteHost();
    }

    public void setAttribute(String name, Object o) {
        wrapped.setAttribute(name, o);
    }

    public void removeAttribute(String name) {
        wrapped.removeAttribute(name);
    }

    public Locale getLocale() {
        return wrapped.getLocale();
    }

    public Enumeration getLocales() {
        return wrapped.getLocales();
    }

    public boolean isSecure() {
        return wrapped.isSecure();
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        return wrapped.getRequestDispatcher(path);
    }

    //WRAPPER IMPLEMENTATIONS OF HTTPSERVLETREQUEST METHODS
    public String getAuthType() {
        return wrapped.getAuthType();
    }

    public Cookie[] getCookies() {
        return wrapped.getCookies();
    }

    public long getDateHeader(String name) {
        return wrapped.getDateHeader(name);
    }

    public String getHeader(String name) {
        return wrapped.getHeader(name);
    }

    public Enumeration getHeaders(String name) {
        return wrapped.getHeaders(name);
    }

    public Enumeration getHeaderNames() {
        return wrapped.getHeaderNames();
    }

    public int getIntHeader(String name) {
        return wrapped.getIntHeader(name);
    }

    public String getMethod() {
        return wrapped.getMethod();
    }

    public String getPathInfo() {
        return wrapped.getPathInfo();
    }

    public String getPathTranslated() {
        return wrapped.getPathTranslated();
    }

    public String getContextPath() {
        return wrapped.getContextPath();
    }

    public String getQueryString() {
        return wrapped.getQueryString();
    }

    public String getRemoteUser() {
        return wrapped.getRemoteUser();
    }

    public boolean isUserInRole(String user) {
        return wrapped.isUserInRole(user);
    }

    public Principal getUserPrincipal() {
        return wrapped.getUserPrincipal();
    }

    public String getRequestedSessionId() {
        return wrapped.getRequestedSessionId();
    }

    public String getRequestURI() {
        return wrapped.getRequestURI();
    }

    public String getServletPath() {
        return wrapped.getServletPath();
    }

    public HttpSession getSession(boolean create) {
        return wrapped.getSession(create);
    }

    public HttpSession getSession() {
        return wrapped.getSession();
    }

    public boolean isRequestedSessionIdValid() {
        return wrapped.isRequestedSessionIdValid();
    }

    public boolean isRequestedSessionIdFromURL() {
        return wrapped.isRequestedSessionIdFromURL();
    }

    // ===================================================================================
    //                                                                         Servlet-2.3
    //                                                                         ===========
    public Map getParameterMap() {
        return wrapped.getParameterMap();
    }

    public void setCharacterEncoding(String encoding) throws UnsupportedEncodingException {
        wrapped.setCharacterEncoding(encoding);
    }

    public StringBuffer getRequestURL() {
        return wrapped.getRequestURL();
    }

    public boolean isRequestedSessionIdFromCookie() {
        return wrapped.isRequestedSessionIdFromCookie();
    }

    // ===================================================================================
    //                                                                         Servlet-2.4
    //                                                                         ===========
    public String getLocalAddr() {
        return wrapped.getLocalAddr();
    }

    public String getLocalName() {
        return wrapped.getLocalName();
    }

    public int getLocalPort() {
        return wrapped.getLocalPort();
    }

    public int getRemotePort() {
        return wrapped.getRemotePort();
    }

    // ===================================================================================
    //                                                                         Servlet-3.0
    //                                                                         ===========
    @Override
    public ServletContext getServletContext() {
        return wrapped.getServletContext();
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return wrapped.startAsync();
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return wrapped.startAsync(servletRequest, servletResponse);
    }

    @Override
    public boolean isAsyncStarted() {
        return wrapped.isAsyncStarted();
    }

    @Override
    public boolean isAsyncSupported() {
        return wrapped.isAsyncSupported();
    }

    @Override
    public AsyncContext getAsyncContext() {
        return wrapped.getAsyncContext();
    }

    @Override
    public DispatcherType getDispatcherType() {
        return wrapped.getDispatcherType();
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return wrapped.authenticate(response);
    }

    @Override
    public void login(String username, String password) throws ServletException {
        wrapped.login(username, password);
    }

    @Override
    public void logout() throws ServletException {
        wrapped.logout();
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return wrapped.getParts();
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        return wrapped.getPart(name);
    }

    @Override
    public long getContentLengthLong() {
        return wrapped.getContentLengthLong();
    }

    @Override
    public String changeSessionId() {
        return wrapped.changeSessionId();
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        return wrapped.upgrade(handlerClass);
    }

    @Override
    public String toString() {
        final String plain = wrapped.toString();
        final String firstLine = plain.contains("\n") ? Srl.substringFirstFront(plain, "\n") + "..." : plain; // might contain line so...
        return DfTypeUtil.toClassTitle(this) + ":{" + firstLine + "}@" + Integer.toHexString(hashCode());
    }

    @Override
    public String getRequestId() {
        return wrapped.getRequestId();
    }

    @Override
    public String getProtocolRequestId() {
        return wrapped.getProtocolRequestId();
    }

    @Override
    public ServletConnection getServletConnection() {
        return wrapped.getServletConnection();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public HttpServletRequest getWrappedRequest() {
        return wrapped;
    }

    public void setParameter(String name, String value) {
        String[] multipleValue = (String[]) parameters.get(name);
        if (multipleValue == null) {
            multipleValue = new String[0];
        }
        final String[] newValue = new String[multipleValue.length + 1];
        System.arraycopy(multipleValue, 0, newValue, 0, multipleValue.length);
        newValue[multipleValue.length] = value;
        parameters.put(name, newValue);
    }
}
