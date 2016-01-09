/*
 * Copyright 2015-2016 the original author or authors.
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

import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.api.ApiManager;
import org.lastaflute.web.login.LoginManager;
import org.lastaflute.web.login.UserBean;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.servlet.cookie.CookieManager;
import org.lastaflute.web.servlet.request.scoped.ScopedAttributeHolder;
import org.lastaflute.web.servlet.request.scoped.ScopedMessageHandler;
import org.lastaflute.web.servlet.session.SessionManager;

/**
 * The manager of request. (request facade)
 * @author jflute
 */
public interface RequestManager extends ScopedAttributeHolder {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    // -----------------------------------------------------
    //                                           User Locale
    //                                           -----------
    /** The key of user locale to cache it in request attribute. */
    String USER_LOCALE_KEY = LastaWebKey.USER_LOCALE_KEY;

    /** The key of user locale to cache it in cookie attribute. */
    String USER_LOCALE_COOKIE = "LALCL";

    // -----------------------------------------------------
    //                                         User TimeZone
    //                                         -------------
    /** The key of user time-zone to cache it in request attribute. */
    String USER_TIMEZONE_KEY = LastaWebKey.USER_TIMEZONE_KEY;

    /** The key of user time-zone to cache it in cookie attribute. */
    String USER_TIMEZONE_COOKIE = "LATZN";

    // -----------------------------------------------------
    //                                     Various Attribute
    //                                     -----------------
    /** The key of SQL count by DBFlute. */
    String DBFLUTE_SQL_COUNT_KEY = "lastaflute.dbflute.SQL_COUNT";

    /** The key of romantic transaction memories by DBFlute. */
    String DBFLUTE_TRANSACTION_MEMORIES_KEY = "lastaflute.dbflute.TRANSACTION_MEMORIES";

    /** The key of mail count by MailFlute. */
    String MAILFLUTE_MAIL_COUNT_KEY = "lastaflute.mailflute.MAIL_COUNT";

    // ===================================================================================
    //                                                                      Basic Handling
    //                                                                      ==============
    /**
     * Get the current request.
     * @return The request object of HTTP Servlet. (NotNull: if not Web application, throws exception)
     * @throws IllegalStateException When the request is not found.
     */
    HttpServletRequest getRequest();

    /**
     * Get the servlet context from the current request.
     * @return The context of servlet. (NotNull)
     * @throws IllegalStateException When the request is not found.
     */
    ServletContext getServletContext();

    /**
     * Get the character encoding of the request.
     * @return The optional character encoding as string. (NullAllowed)
     */
    OptionalThing<String> getCharacterEncoding();

    /**
     * Get the content type of the request.
     * @return The optional content type as string. (NullAllowed: if unknown)
     */
    OptionalThing<String> getContentType();

    /**
     * Is the request POST?
     * @return The determination, true or false.
     */
    boolean isPost();

    // ===================================================================================
    //                                                                  Parameter Handling
    //                                                                  ==================
    /**
     * Get the request parameter by the key. <br>
     * It returns empty optional when not only null but also empty string.
     * @param key The key of the parameter. (NotNull)
     * @return The optional value of the parameter as string. (NotNull, EmptyAllowed: when not found, empty value)
     */
    OptionalThing<String> getParameter(String key);

    /**
     * Get the request body from reader.
     * @return The body of request. (NotNull, EmptyAllowed: when no body)
     */
    String getRequestBody();

    // ===================================================================================
    //                                                                       Path Handling
    //                                                                       =============
    /**
     * Get the context path of this web application. e.g. '/' or '/dockside'
     * @return The path as string. (NotNull, EmptyAllowed: when e.g. root context)
     */
    String getContextPath();

    /**
     * Get the request path (without query) of the current request. e.g. /member/list/ <br>
     * Not contains context path and escaped slash remains.
     * @return The path as string. (NotNull)
     */
    String getRequestPath();

    /**
     * Get the request path and query. e.g. /member/list/?keyword=foo&status=FOO <br>
     * This uses getRequestPath() and HttpServletRequest#getQueryString().
     * @return The path and query as string. (NotNull)
     */
    String getRequestPathAndQuery();

    /**
     * Get the query string of the current request. e.g. keyword=foo&status=FOO <br>
     * This uses {@link HttpServletRequest#getQueryString()}.
     * @return The optional query string. (NotNull, EmptyAllowed: when null query, empty query, so wrapped value cannot be empty)
     */
    OptionalThing<String> getQueryString();

    /**
     * Convert to absolute path from context, e.g. /dockside/product/list/. <br>
     * If the context is empty or '/', it returns the specified path plainly.
     * @param contextRelativePath The relative path from context, e.g. /product/list/ (NotNull)
     * @return The converted path as absolute path. (NotNull)
     */
    String toContextAbsolutePath(String contextRelativePath);

    // ===================================================================================
    //                                                                     Header Handling
    //                                                                     ===============
    /**
     * Get header value. (case insensitive)
     * @param headerKey The key of the header. (NotNull)
     * @return The optional value of specified header as string. (NotNull, EmptyAllowed)
     */
    OptionalThing<String> getHeader(String headerKey);

    /**
     * Get 'Host' from header.
     * @return The optional string for the header 'Host'. (NotNull, EmptyAllowed)
     */
    OptionalThing<String> getHeaderHost();

    /**
     * Get 'Referer' from header.
     * @return The optional string for the header 'Referer'. (NotNull, EmptyAllowed)
     */
    OptionalThing<String> getHeaderReferer();

    /**
     * Get 'User-Agent' from header.
     * @return The optional string for the header 'User-Agent'. (NotNull, EmptyAllowed)
     */
    OptionalThing<String> getHeaderUserAgent();

    /**
     * Get 'X-Forwarded-For' from header.
     * @return The optional string for the header 'X-Forwarded-For'. (NotNull, EmptyAllowed)
     */
    OptionalThing<String> getHeaderXForwardedFor();

    /**
     * Get 'X-SSL' from header.
     * @return The optional string for the header 'X-SSL'. (NotNull, EmptyAllowed)
     */
    OptionalThing<String> getHeaderXSsl();

    // ===================================================================================
    //                                                                     Remote Handling
    //                                                                     ===============
    /**
     * Get the remote address by servlet method plainly called.
     * @return The optional string as remote address. (NotNull, EmptyAllowed)
     */
    OptionalThing<String> getRemoteAddr();

    /**
     * Get the remote host by servlet method plainly called.
     * @return The optional string as remote host. (NotNull, EmptyAllowed)
     */
    OptionalThing<String> getRemoteHost();

    /**
     * Get the remote IP address adjusted like this: <br>
     * At first, find X-Forwarded-For or else getRemoteAddr().
     * @return The optional string as remote IP address. (NotNull, EmptyAllowed)
     */
    OptionalThing<String> getRemoteIp();

    /**
     * Get the remote port by servlet method plainly called.
     * @return The optional string as remote port. (NotNull, EmptyAllowed)
     */
    OptionalThing<Integer> getRemotePort();

    /**
     * Get the remote user by servlet method plainly called.
     * @return The optional string as remote user. (NotNull, EmptyAllowed)
     */
    OptionalThing<String> getRemoteUser();

    // ===================================================================================
    //                                                                      Login Handling
    //                                                                      ==============
    /**
     * Find login manager interface by user bean. <br>
     * Implementations of login managers may be plural (and smart deploy components), <br>
     * so you cannot get it from container by type, you need to find it by this method. <br>
     * And you cannot use same user bean between plural login managers.
     * @param userBeanType The type of user bean to find. (NotNull)
     * @return The optional login manager. (NotNull, EmptyAllowed: if not found, not logined)
     */
    OptionalThing<LoginManager> findLoginManager(Class<?> userBeanType);

    /**
     * Find user bean of current request for the type. (basically from session, but unconcern) <br>
     * Empty optional means not login state, so you can control by optional thing methods. <br>
     * If your application does not use login, always returns empty.
     * <pre>
     * Integer userId = requestManager.findUserBean(SeaUserBean.class).map(userBean -&gt; {
     *     return userBean.getUserId();
     * }).orElse(DEFAULT_USER_ID);
     * </pre>
     * <p>You can get user bean without login manager.
     * Login manager is for login control so many functions are provided.
     * But all cases need only user bean so facade method here.</p>
     * @param <USER_BEAN> The type of user bean.
     * @param <ID> The type of user ID.
     * @param userBeanType The type of user bean to find. (NotNull)
     * @return The optional user bean. (NotNull, EmptyAllowed: if not found, not logined)
     */
    <USER_BEAN extends UserBean<ID>, ID> OptionalThing<USER_BEAN> findUserBean(Class<USER_BEAN> userBeanType);

    // ===================================================================================
    //                                                                     Region Handling
    //                                                                     ===============
    // -----------------------------------------------------
    //                                           User Locale
    //                                           -----------
    /**
     * Get the locale for user of current request. <br>
     * Finding from e.g. cache, session, request.
     * @return The object that specifies user locale. (NotNull)
     */
    Locale getUserLocale();

    /**
     * Resolve the locale for user of current request. <br>
     * Basically this is called before action execution in request processor. <br>
     * So use {@link #getUserLocale()} if you find locale.
     * @param runtime The runtime meta of action execution which you can get the calling method. (NotNull)
     * @return The selected locale for the current request. (NotNull)
     */
    Locale resolveUserLocale(ActionRuntime runtime);

    /**
     * Save the locale for user of current request to cookie. <br>
     * It is precondition that cookie locale can be accepted by option.
     * @param locale The saved locale to cookie. (NullAllowed: if null, remove it from cookie)
     * @throws IllegalStateException When the cookie locale cannot be accepted.
     */
    void saveUserLocaleToCookie(Locale locale);

    /**
     * Save the locale for user of current request to session. <br>
     * The session key is same as Struts so messages process can use it.
     * @param locale The saved locale to session. (NullAllowed: if null, remove it from session)
     */
    void saveUserLocaleToSession(Locale locale);

    // -----------------------------------------------------
    //                                         User TimeZone
    //                                         -------------
    /**
     * Get the time-zone for user of current request. <br>
     * Finding from e.g. cache, session, (assisted default time-zone).
     * @return The object that specifies user time-zone. (NotNull)
     */
    TimeZone getUserTimeZone();

    /**
     * Resolve the time-zone for user of current request. <br>
     * Basically this is called before action execution in request processor. <br>
     * So use {@link #getUserTimeZone()} if you find time-zone.
     * @param runtime The runtime meta of action execution which you can get the calling method. (NotNull)
     * @return The object that specifies request time-zone. (NotNull)
     */
    TimeZone resolveUserTimeZone(ActionRuntime runtime);

    /**
     * Save the time-zone for user of current request to cookie. <br>
     * It is precondition that cookie time-zone can be accepted by option.
     * @param timeZone The saved time-zone to cookie. (NullAllowed: if null, remove it from cookie)
     * @throws IllegalStateException When the cookie time-zone cannot be accepted.
     */
    void saveUserTimeZoneToCookie(TimeZone timeZone);

    /**
     * Save the time-zone for user of current request to session.
     * @param timeZone The saved time-zone to time-zone. (NullAllowed: if null, remove it from session)
     */
    void saveUserTimeZoneToSession(TimeZone timeZone);

    // ===================================================================================
    //                                                                    Message Handling
    //                                                                    ================
    /**
     * @return The handler of action errors on request. (NotNull)
     */
    ScopedMessageHandler errors();

    /**
     * @return The handler of action info on request. (NotNull)
     */
    ScopedMessageHandler info();

    /**
     * Save errors in request to session. <br>
     * No errors, no exception. And overrides existing errors in session.
     */
    void saveErrorsToSession();

    // ===================================================================================
    //                                                                     Friends Gateway
    //                                                                     ===============
    /**
     * Get the manager of response.
     * @return The injected manager of response. (NotNull)
     */
    ResponseManager getResponseManager();

    /**
     * Get the manager of session.
     * @return The injected manager of session. (NotNull)
     */
    SessionManager getSessionManager();

    /**
     * Get the manager of cookie.
     * @return The injected manager of cookie. (NotNull)
     */
    CookieManager getCookieManager();

    /**
     * Get the manager of time.
     * @return The injected manager of time. (NotNull)
     */
    TimeManager getTimeManager();

    /**
     * Get the manager of message.
     * @return The injected manager of message. (NotNull)
     */
    MessageManager getMessageManager();

    /**
     * Get the manager of JSON.
     * @return The injected manager of JSON. (NotNull)
     */
    JsonManager getJsonManager();

    /**
     * Get the manager of API.
     * @return The injected manager of API. (NotNull)
     */
    ApiManager getApiManager();
}
