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
package org.lastaflute.web.servlet.request;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfStringUtil;
import org.dbflute.util.Srl;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.di.core.ComponentDef;
import org.lastaflute.di.core.exception.ComponentNotFoundException;
import org.lastaflute.di.core.exception.TooManyRegistrationComponentException;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.api.ApiManager;
import org.lastaflute.web.direction.FwWebDirection;
import org.lastaflute.web.exception.RequestAttributeCannotCastException;
import org.lastaflute.web.exception.RequestAttributeNotFoundException;
import org.lastaflute.web.exception.RequestInfoNotFoundException;
import org.lastaflute.web.login.LoginManager;
import org.lastaflute.web.login.UserBean;
import org.lastaflute.web.ruts.message.ActionMessage;
import org.lastaflute.web.ruts.message.ActionMessages;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.servlet.cookie.CookieManager;
import org.lastaflute.web.servlet.request.scoped.ScopedMessageHandler;
import org.lastaflute.web.servlet.session.SessionManager;
import org.lastaflute.web.util.LaRequestUtil;
import org.lastaflute.web.util.LaServletContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class SimpleRequestManager implements RequestManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(SimpleRequestManager.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant director (AD) for framework. (NotNull: after initialization) */
    @Resource
    protected FwAssistantDirector assistantDirector;

    /** The manager of response. (NotNull: after initialization) */
    @Resource
    protected ResponseManager responseManager;

    /** The manager of session. (NotNull: after initialization) */
    @Resource
    protected SessionManager sessionManager;

    /** The manager of cookie. (NotNull: after initialization) */
    @Resource
    protected CookieManager cookieManager;

    /** The manager of time. (NotNull: after initialization) */
    @Resource
    protected TimeManager timeManager;

    /** The manager of message. (NotNull: after initialization) */
    @Resource
    protected MessageManager messageManager;

    /** The manager of API. (NotNull: after initialization) */
    @Resource
    protected ApiManager apiManager;

    /** The manager of JSON. (NotNull: after initialization) */
    @Resource
    protected JsonManager jsonManager;

    /** The provider of request user locale. (NotNull: after initialization) */
    protected UserLocaleProcessProvider localeHandler;

    /** The provider of request user time-zone. (NotNull: after initialization) */
    protected UserTimeZoneProcessProvider timeZoneProvider;

    protected ScopedMessageHandler errorsHandler; // lazy loaded
    protected ScopedMessageHandler infoHandler; // lazy loaded

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    /**
     * Initialize this component. <br>
     * This is basically called by DI setting file.
     */
    @PostConstruct
    public synchronized void initialize() {
        final FwWebDirection direction = assistWebDirection();
        localeHandler = direction.assistUserLocaleProcessProvider();
        timeZoneProvider = direction.assistUserTimeZoneProcessProvider();
        showBootLogging();
    }

    protected FwWebDirection assistWebDirection() {
        return assistantDirector.assistWebDirection();
    }

    protected void showBootLogging() {
        if (logger.isInfoEnabled()) {
            logger.info("[Request Manager]");
            logger.info(" localeProvider: " + localeHandler);
            logger.info(" timeZoneProvider: " + timeZoneProvider);
        }
    }

    // ===================================================================================
    //                                                                      Basic Handling
    //                                                                      ==============
    @Override
    public HttpServletRequest getRequest() {
        final HttpServletRequest request = LaRequestUtil.getRequest();
        if (request == null) {
            throw new IllegalStateException("Not found the request, not web environment?");
        }
        return request;
    }

    @Override
    public ServletContext getServletContext() {
        return getRequest().getServletContext();
    }

    @Override
    public OptionalThing<String> getCharacterEncoding() {
        return OptionalThing.ofNullable(getRequest().getCharacterEncoding(), () -> {
            throw new IllegalStateException("Not found the character encoding for the request: " + getRequestPath());
        });
    }

    @Override
    public OptionalThing<String> getContentType() {
        return OptionalThing.ofNullable(getRequest().getContentType(), () -> {
            throw new IllegalStateException("Not found the content type for the request: " + getRequestPath());
        });
    }

    @Override
    public boolean isPost() {
        return "post".equalsIgnoreCase(getRequest().getMethod());
    }

    // ===================================================================================
    //                                                                  Parameter Handling
    //                                                                  ==================
    @Override
    public OptionalThing<String> getParameter(String key) {
        assertArgumentNotNull("key", key);
        final String param = getRequest().getParameter(key);
        return OptionalThing.ofNullable(param != null && !param.isEmpty() ? param : null, () -> {
            throw new IllegalStateException("Not found the request parameter for the key: " + key);
        });
    }

    @Override
    public String getRequestBody() {
        final BufferedReader reader = prepareRequestBodyReader();
        final StringBuilder sb = new StringBuilder();
        try {
            String line;
            while (true) {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read the line from the reader: " + reader, e);
        }
    }

    protected BufferedReader prepareRequestBodyReader() {
        final HttpServletRequest request = getRequest();
        final BufferedReader reader;
        try {
            reader = request.getReader();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to get reader from the request: " + request, e);
        }
        return reader;
    }

    // ===================================================================================
    //                                                                  Attribute Handling
    //                                                                  ==================
    // -----------------------------------------------------
    //                                                 Basic
    //                                                 -----
    @Override
    public <ATTRIBUTE> OptionalThing<ATTRIBUTE> getAttribute(String key, Class<ATTRIBUTE> attributeType) {
        assertArgumentNotNull("key", key);
        final Object original = getRequest().getAttribute(key);
        final ATTRIBUTE attribute;
        if (original != null) {
            try {
                attribute = attributeType.cast(original);
            } catch (ClassCastException e) {
                final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
                br.addNotice("Cannot cast the request attribute");
                br.addItem("Attribute Key");
                br.addElement(key);
                br.addItem("Specified Type");
                br.addElement(attributeType);
                br.addItem("Existing Attribute");
                br.addElement(original.getClass());
                br.addElement(original);
                br.addItem("Attribute List");
                br.addElement(getAttributeNameList());
                final String msg = br.buildExceptionMessage();
                throw new RequestAttributeCannotCastException(msg);
            }
        } else {
            attribute = null;
        }
        return OptionalThing.ofNullable(attribute, () -> {
            final List<String> nameList = getAttributeNameList();
            final String msg = "Not found the request attribute by the string key: " + key + " existing=" + nameList;
            throw new RequestAttributeNotFoundException(msg);
        });
    }

    @Override
    public List<String> getAttributeNameList() {
        final Enumeration<String> attributeNames = getRequest().getAttributeNames();
        final List<String> nameList = new ArrayList<String>();
        while (attributeNames.hasMoreElements()) {
            nameList.add((String) attributeNames.nextElement());
        }
        return Collections.unmodifiableList(nameList);
    }

    @Override
    public void setAttribute(String key, Object value) {
        assertArgumentNotNull("key", key);
        assertArgumentNotNull("value", value);
        getRequest().setAttribute(key, value);
    }

    @Override
    public void removeAttribute(String key) {
        assertArgumentNotNull("key", key);
        getRequest().removeAttribute(key);
    }

    // see interface ScopedAttributeHolder for the detail
    //@Override
    //public <ATTRIBUTE> OptionalThing<ATTRIBUTE> getAttribute(Class<ATTRIBUTE> typeKey) {
    //    assertArgumentNotNull("typeKey", typeKey);
    //    final String key = typeKey.getName();
    //    @SuppressWarnings("unchecked")
    //    final ATTRIBUTE attribute = (ATTRIBUTE) getRequest().getAttribute(key);
    //    return OptionalThing.ofNullable(attribute, () -> {
    //        final List<String> nameList = getAttributeNameList();
    //        final String msg = "Not found the request attribute by the typed key: " + key + " existing=" + nameList;
    //        throw new RequestAttributeNotFoundException(msg);
    //    });
    //}
    //@Override
    //public void setAttribute(Object value) {
    //    assertArgumentNotNull("value", value);
    //    checkTypedAttributeSettingMistake(value);
    //    getRequest().setAttribute(value.getClass().getName(), value);
    //}
    //protected void checkTypedAttributeSettingMistake(Object value) {
    //    if (value instanceof String) {
    //        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
    //        br.addNotice("The value for typed attribute was simple string type.");
    //        br.addItem("Advice");
    //        br.addElement("The value should not be string.");
    //        br.addElement("Do you forget value setting for the string key?");
    //        br.addElement("The typed attribute setting cannot accept string");
    //        br.addElement("to suppress setting mistake like this:");
    //        br.addElement("  (x):");
    //        br.addElement("    requestManager.setAttribute(\"foo.bar\")");
    //        br.addElement("  (o):");
    //        br.addElement("    requestManager.setAttribute(\"foo.bar\", value)");
    //        br.addElement("  (o):");
    //        br.addElement("    requestManager.setAttribute(bean)");
    //        br.addItem("Specified Value");
    //        br.addElement(value != null ? value.getClass().getName() : null);
    //        br.addElement(value);
    //        final String msg = br.buildExceptionMessage();
    //        throw new IllegalArgumentException(msg);
    //    }
    //}
    //@Override
    //public void removeAttribute(Class<?> type) {
    //    assertArgumentNotNull("type", type);
    //    getRequest().removeAttribute(type.getName());
    //}

    // ===================================================================================
    //                                                                       Path Handling
    //                                                                       =============
    @Override
    public String getContextPath() {
        return getRequest().getContextPath();
    }

    @Override
    public String getRequestPath() {
        return extractActionRequestPath(getRequest());
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
        final String requestPath;
        final String requestURI = request.getRequestURI();
        final String contextPath = request.getContextPath();
        if (contextPath != null && contextPath.trim().length() > 0 && !contextPath.equals("/")) { // e.g. /dockside
            // e.g. /dockside/member/list/ to /member/list/
            requestPath = Srl.removePrefix(requestURI, contextPath);
        } else { // no context path
            requestPath = requestURI;
        }
        return filterRequestPath(requestPath); // e.g. /member/list/foo%2fbar/
    }

    protected String filterRequestPath(String path) {
        return removeViewPrefixFromRequestPathIfNeeds(removeJSessionIDFromRequestPathIfNeeds(path));
    }

    protected String removeJSessionIDFromRequestPathIfNeeds(String path) {
        return Srl.substringFirstFrontIgnoreCase(path, ";" + getJSessionIDParameterKey());
    }

    protected String getJSessionIDParameterKey() {
        return "jsessionid";
    }

    protected String removeViewPrefixFromRequestPathIfNeeds(String path) { // from RequestUtil.getPath()
        if (!path.endsWith(".jsp")) {
            return path;
        }
        final String viewPrefix = LaServletContextUtil.getJspViewPrefix();
        return path.startsWith(viewPrefix) ? path.substring(viewPrefix.length()) : path;
    }

    @Override
    public String getRequestPathAndQuery() {
        return getRequestPath() + getQueryString().map(qr -> "?" + qr).orElse("");
    }

    @Override
    public OptionalThing<String> getQueryString() {
        final String query = getRequest().getQueryString();
        return OptionalThing.ofNullable(query != null && !query.isEmpty() ? query : null, () -> {
            throw new RequestInfoNotFoundException("Not found the query string for the request: " + getRequestPath());
        }); // empty check just in case
    }

    @Override
    public String toContextAbsolutePath(String contextRelativePath) {
        final String ctx = getContextPath();
        return (!ctx.isEmpty() && !ctx.equals("/") ? ctx : "") + contextRelativePath;
    }

    // ===================================================================================
    //                                                                     Header Handling
    //                                                                     ===============
    @Override
    public OptionalThing<String> getHeader(String headerKey) {
        return OptionalThing.ofNullable(getRequest().getHeader(headerKey), () -> {
            throw new RequestInfoNotFoundException("Not found the header for the request: key=" + headerKey + " path=" + getRequestPath());
        });
    }

    @Override
    public OptionalThing<String> getHeaderHost() {
        return getHeader("Host");
    }

    @Override
    public OptionalThing<String> getHeaderReferer() {
        return getHeader("Referer");
    }

    @Override
    public OptionalThing<String> getHeaderUserAgent() {
        return getHeader("User-Agent");
    }

    @Override
    public OptionalThing<String> getHeaderXForwardedFor() {
        return getHeader("X-Forwarded-For");
    }

    @Override
    public OptionalThing<String> getHeaderXSsl() {
        return getHeader("X-SSL");
    }

    // ===================================================================================
    //                                                                     Remote Handling
    //                                                                     ===============
    @Override
    public OptionalThing<String> getRemoteAddr() {
        return OptionalThing.ofNullable(getRequest().getRemoteAddr(), () -> {
            throw new RequestInfoNotFoundException("Not found the remote address for the request: path=" + getRequestPath());
        });
    }

    @Override
    public OptionalThing<String> getRemoteHost() {
        return OptionalThing.ofNullable(getRequest().getRemoteHost(), () -> {
            throw new RequestInfoNotFoundException("Not found the remote host for the request: path=" + getRequestPath());
        });
    }

    @Override
    public OptionalThing<String> getRemoteIp() {
        final OptionalThing<String> xfor = getHeaderXForwardedFor();
        return xfor.isPresent() ? xfor : getRemoteAddr();
    }

    @Override
    public OptionalThing<Integer> getRemotePort() {
        return OptionalThing.ofNullable(getRequest().getRemotePort(), () -> {
            throw new RequestInfoNotFoundException("Not found the remote port for the request: path=" + getRequestPath());
        });
    }

    @Override
    public OptionalThing<String> getRemoteUser() {
        return OptionalThing.ofNullable(getRequest().getRemoteUser(), () -> {
            throw new RequestInfoNotFoundException("Not found the remote user for the request: path=" + getRequestPath());
        });
    }

    // ===================================================================================
    //                                                                      Login Handling
    //                                                                      ==============
    @Override
    public <USER_BEAN extends UserBean<ID>, ID> OptionalThing<USER_BEAN> findUserBean(Class<USER_BEAN> userBeanType) {
        @SuppressWarnings("unchecked")
        final OptionalThing<USER_BEAN> userBean = (OptionalThing<USER_BEAN>) findLoginManager(userBeanType).flatMap(manager -> {
            return manager.getSessionUserBean();
        });
        return userBean;
    }

    @Override
    public OptionalThing<LoginManager> findLoginManager(Class<?> userBeanType) {
        try {
            // login manager's implementation is basically smart deploy component
            // the component may not be initialized yet because of HotDeploy
            // so use ContainerUtil here and handle not-found exception
            return OptionalThing.of(ContainerUtil.getComponent(LoginManager.class));
        } catch (ComponentNotFoundException e) {
            return OptionalThing.ofNullable(null, () -> {
                String msg = "Not found the login manager for the bean type: " + userBeanType.getName();
                throw new IllegalStateException(msg);
            });
        } catch (TooManyRegistrationComponentException e) {
            return handleLoginManagerTooMany(userBeanType, e);
        }
    }

    protected OptionalThing<LoginManager> handleLoginManagerTooMany(Class<?> userBeanType, TooManyRegistrationComponentException cause) {
        // not use findAllComponents() because cannot get components by login manager if hot deploy
        // and cannot use same user bean between plural login managers
        final List<ComponentDef> componentDefList = cause.getComponentDefList();
        return OptionalThing.migratedFrom(componentDefList.stream().map(def -> {
            final Object component = def.getComponent();
            if (component == null) {
                String msg = "Not found the login manager, getComponent() returned null:";
                msg = msg + " componentDef=" + def + " userBeanType=" + userBeanType;
                throw new IllegalStateException(msg, cause);
            }
            if (!(component instanceof LoginManager)) {
                String msg = "Cannot cast the component to login manager:";
                msg = msg + " componentType=" + component.getClass() + " component=" + component + " userBeanType=" + userBeanType;
                throw new IllegalStateException(msg, cause);
            }
            return (LoginManager) component;
        }).filter(manager -> {
            return manager.getSessionUserBean().filter(foundBean -> userBeanType.isAssignableFrom(foundBean.getClass())).isPresent();
        }).findFirst(), () -> {
            String msg = "Not found the login manager for the bean type: " + userBeanType.getName();
            throw new IllegalStateException(msg, cause);
        });
    }

    //                                                                     Region Handling
    //                                                                     ===============
    // -----------------------------------------------------
    //                                           User Locale
    //                                           -----------
    @Override
    public Locale getUserLocale() {
        Locale locale = findCachedLocale();
        if (locale != null) {
            // mainly here if you call this in action process
            // because locale process is called before action
            return locale;
        }
        locale = findSessionLocale();
        if (locale != null) {
            return locale;
        }
        return getRequestedLocale();
    }

    @Override
    public Locale resolveUserLocale(ActionRuntime runtime) {
        Locale locale = findCachedLocale();
        if (locale == null) {
            locale = findBusinessLocale(runtime);
        }
        if (locale == null) {
            locale = findCookieLocale(); // before session
        }
        if (locale == null) {
            locale = findSessionLocale(); // after cookie
        }
        if (locale == null) {
            locale = getRequestedLocale(); // not null
        }
        // not cookie here (should be saved in cookie explicitly)
        saveUserLocaleToSession(locale);
        return locale;
    }

    protected Locale findCachedLocale() { // null allowed because of internal handling
        return getAttribute(getReqeustUserLocaleKey(), Locale.class).orElse(null);
    }

    protected Locale findBusinessLocale(ActionRuntime runtime) { // null allowed because of internal handling
        return localeHandler.findBusinessLocale(runtime, this).orElse(null);
    }

    protected Locale findCookieLocale() { // null allowed because of internal handling
        if (!localeHandler.isAcceptCookieLocale()) {
            return null;
        }
        final String cookieLocaleKey = getCookieUserLocaleKey();
        final OptionalThing<Cookie> optCookie = cookieManager.getCookie(cookieLocaleKey);
        return optCookie.map(cookie -> {
            final String localeExp = cookie.getValue();
            if (localeExp == null || localeExp.trim().length() == 0) {
                return null;
            }
            final List<String> splitList = DfStringUtil.splitList(localeExp, "_");
            if (splitList.size() > 3) { /* invalid e.g. foo_bar_qux_corge */
                cookieManager.removeCookie(cookieLocaleKey);
                return null;
            }
            final String language = splitList.get(0); /* always exists */
            final String country = splitList.size() > 1 ? splitList.get(1) : null;
            final String variant = splitList.size() > 2 ? splitList.get(2) : null;
            try {
                return new Locale(language, country, variant);
            } catch (RuntimeException continued) { /* just in case for user-side value */
                logger.debug("*Cannot get locale: exp={} e={}", localeExp, continued.getMessage());
                cookieManager.removeCookie(cookieLocaleKey);
                return null;
            }
        }).orElse(null);
    }

    protected Locale findSessionLocale() { // null allowed because of internal handling
        return sessionManager.getAttribute(getSessionUserLocaleKey(), Locale.class).orElse(null);
    }

    protected Locale getRequestedLocale() {
        return getRequest().getLocale();
    }

    @Override
    public void saveUserLocaleToCookie(Locale locale) {
        if (!localeHandler.isAcceptCookieLocale()) {
            String msg = "Cookie locale is unavailable so nonsense: locale=" + locale;
            throw new IllegalStateException(msg);
        }
        logger.debug("...Saving user locale to cokie: {}", locale);
        cookieManager.setCookie(getCookieUserLocaleKey(), locale.toString());
        setAttribute(getReqeustUserLocaleKey(), locale);
    }

    @Override
    public void saveUserLocaleToSession(Locale locale) {
        sessionManager.setAttribute(getSessionUserLocaleKey(), locale);
        logger.debug("...Saving user locale to session: {}", locale);
        setAttribute(getReqeustUserLocaleKey(), locale);
    }

    protected String getReqeustUserLocaleKey() {
        return USER_LOCALE_KEY;
    }

    protected String getSessionUserLocaleKey() {
        return USER_LOCALE_KEY;
    }

    protected String getCookieUserLocaleKey() {
        return USER_LOCALE_COOKIE;
    }

    // -----------------------------------------------------
    //                                         User TimeZone
    //                                         -------------
    @Override
    public TimeZone getUserTimeZone() {
        TimeZone timeZone = findCachedTimeZone();
        if (timeZone != null) {
            // mainly here if you call this in action process
            // because time-zone process is called before action
            return timeZone;
        }
        timeZone = findSessionTimeZone();
        if (timeZone != null) {
            return timeZone;
        }
        return getRequestedTimeZone();
    }

    @Override
    public TimeZone resolveUserTimeZone(ActionRuntime runtime) {
        if (!timeZoneProvider.isUseTimeZoneHandling()) {
            return null;
        }
        TimeZone timeZone = findCachedTimeZone();
        if (timeZone == null) {
            timeZone = findBusinessTimeZone(runtime);
        }
        if (timeZone == null) {
            timeZone = findCookieTimeZone(); // before session
        }
        if (timeZone == null) {
            timeZone = findSessionTimeZone(); // after cookie
        }
        if (timeZone == null) {
            timeZone = getRequestedTimeZone(); // not null
        }
        // not cookie here (should be saved in cookie explicitly)
        saveUserTimeZoneToSession(timeZone);
        return timeZone;
    }

    protected TimeZone findCachedTimeZone() { // null allowed because of internal handling
        return getAttribute(getReqeustUserTimeZoneKey(), TimeZone.class).orElse(null);
    }

    protected TimeZone findBusinessTimeZone(ActionRuntime runtime) { // null allowed because of internal handling
        return timeZoneProvider.findBusinessTimeZone(runtime, this).orElse(null);
    }

    protected TimeZone findCookieTimeZone() { // null allowed because of internal handling
        if (!timeZoneProvider.isAcceptCookieTimeZone()) {
            return null;
        }
        final String cookieTimeZoneKey = getCookieUserTimeZoneKey();
        return cookieManager.getCookie(cookieTimeZoneKey).map(cookie -> {
            final String timeZoneId = cookie.getValue();
            if (timeZoneId == null || timeZoneId.trim().length() == 0) {
                return null;
            }
            try {
                return TimeZone.getTimeZone(timeZoneId);
            } catch (RuntimeException continued) { /* just in case for user-side value */
                logger.debug("*Cannot get time-zone: id={} e={}", timeZoneId, continued.getMessage());
                cookieManager.removeCookie(cookieTimeZoneKey);
                return null;
            }
        }).orElse(null);
    }

    protected TimeZone findSessionTimeZone() { // null allowed because of internal handling
        return sessionManager.getAttribute(getSessionUserTimeZoneKey(), TimeZone.class).orElse(null);
    }

    protected TimeZone getRequestedTimeZone() {
        // unfortunately we cannot get time-zone from request
        // so it needs to provide the default time-zone
        final TimeZone zone = timeZoneProvider.getRequestedTimeZone(this);
        if (zone == null) {
            throw new IllegalStateException("The getRequestedTimeZone() cannot return null: " + timeZoneProvider);
        }
        return zone;
    }

    @Override
    public void saveUserTimeZoneToCookie(TimeZone timeZone) {
        if (!timeZoneProvider.isAcceptCookieTimeZone()) {
            String msg = "Cookie time-zone is unavailable so nonsense: time-zone=" + timeZone;
            throw new IllegalStateException(msg);
        }
        logger.debug("...Saving user time-zone to cookie: {}", timeZone);
        cookieManager.setCookie(getCookieUserTimeZoneKey(), timeZone.toString());
        setAttribute(getReqeustUserTimeZoneKey(), timeZone);
    }

    @Override
    public void saveUserTimeZoneToSession(TimeZone timeZone) {
        logger.debug("...Saving user time-zone to session: {}", timeZone);
        sessionManager.setAttribute(getSessionUserTimeZoneKey(), timeZone);
        setAttribute(getReqeustUserTimeZoneKey(), timeZone);
    }

    protected String getReqeustUserTimeZoneKey() {
        return USER_TIMEZONE_KEY;
    }

    protected String getSessionUserTimeZoneKey() {
        return USER_TIMEZONE_KEY;
    }

    protected String getCookieUserTimeZoneKey() {
        return USER_TIMEZONE_COOKIE;
    }

    // ===================================================================================
    //                                                                    Message Handling
    //                                                                    ================
    @Override
    public ScopedMessageHandler errors() {
        if (errorsHandler == null) {
            synchronized (this) {
                if (errorsHandler == null) {
                    errorsHandler = createScopedMessageHandler(getErrorMessagesKey());
                }
            }
        }
        return errorsHandler;
    }

    protected String getErrorMessagesKey() {
        return LastaWebKey.ACTION_ERRORS_KEY;
    }

    @Override
    public ScopedMessageHandler info() {
        if (infoHandler == null) {
            synchronized (this) {
                if (infoHandler == null) {
                    infoHandler = createScopedMessageHandler(getInfoMessagesKey());
                }
            }
        }
        return infoHandler;
    }

    protected ScopedMessageHandler createScopedMessageHandler(String messagesKey) {
        return new ScopedMessageHandler(this, ActionMessages.GLOBAL_PROPERTY_KEY, messagesKey);
    }

    protected String getInfoMessagesKey() {
        return LastaWebKey.ACTION_INFO_KEY;
    }

    @Override
    public void saveErrorsToSession() {
        errors().get().ifPresent(messages -> {
            final Iterator<ActionMessage> ite = messages.accessByFlatIterator();
            sessionManager.errors().clear(); /* means overriding */
            while (ite.hasNext()) {
                final ActionMessage message = ite.next();
                sessionManager.errors().add(message.getKey(), message.getValues());
            }
        });
    }

    // ===================================================================================
    //                                                                     Friends Gateway
    //                                                                     ===============
    @Override
    public ResponseManager getResponseManager() {
        return responseManager;
    }

    @Override
    public SessionManager getSessionManager() {
        return sessionManager;
    }

    @Override
    public CookieManager getCookieManager() {
        return cookieManager;
    }

    @Override
    public TimeManager getTimeManager() {
        return timeManager;
    }

    @Override
    public MessageManager getMessageManager() {
        return messageManager;
    }

    @Override
    public JsonManager getJsonManager() {
        return jsonManager;
    }

    @Override
    public ApiManager getApiManager() {
        return apiManager;
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            throw new IllegalArgumentException("The variableName should not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
        }
    }
}
