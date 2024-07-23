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
package org.lastaflute.web.servlet.cookie;

import java.util.function.Consumer;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.direction.exception.FwRequiredAssistNotFoundException;
import org.lastaflute.web.direction.FwWebDirection;
import org.lastaflute.web.exception.CookieNotFoundException;
import org.lastaflute.web.servlet.cookie.exception.CookieCipherDecryptFailureException;
import org.lastaflute.web.util.LaRequestUtil;
import org.lastaflute.web.util.LaResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author jflute
 */
public class SimpleCookieManager implements CookieManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(SimpleCookieManager.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant director (AD) for framework. (NotNull: after initialization) */
    @Resource
    private FwAssistantDirector assistantDirector;

    /** The cipher for cookie's value. (NotNull) */
    @Resource
    private CookieCipher cookieCipher;

    /** The default path when no specified expire. (NotNull: after initialization) */
    protected String defaultPath;

    /** The default expire (max age) when no specified expire. (NotNull: after initialization) */
    protected Integer defaultExpire;

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
        defaultPath = direction.assistCookieResourceProvider().provideDefaultPath();
        if (defaultPath == null) {
            final String msg = "No assist for the default path of cookie.";
            throw new FwRequiredAssistNotFoundException(msg);
        }
        defaultExpire = direction.assistCookieResourceProvider().provideDefaultExpire();
        if (defaultExpire == null) {
            final String msg = "No assist for the default expire of cookie.";
            throw new FwRequiredAssistNotFoundException(msg);
        }
        showBootLogging();
    }

    protected FwWebDirection assistWebDirection() {
        return assistantDirector.assistWebDirection();
    }

    protected void showBootLogging() {
        if (logger.isInfoEnabled()) {
            logger.info("[Cookie Manager]");
            logger.info(" cookieCipher: " + cookieCipher);
            logger.info(" defaultPath: " + defaultPath);
            logger.info(" defaultExpire: " + defaultExpire);
        }
    }

    // ===================================================================================
    //                                                                          Set Cookie
    //                                                                          ==========
    // -----------------------------------------------------
    //                                                 Basic
    //                                                 -----
    @Override
    public void setCookie(String key, String value) {
        assertKeyNotNull(key);
        assertValueNotNull(key, value);
        doSetCookieDegage(key, value, getDefaultExpire(), cookie -> {});
    }

    @Override
    public void setCookie(String key, String value, int expire) {
        assertKeyNotNull(key);
        assertValueNotNull(key, value);
        assertExpirePositive(expire);
        doSetCookieDegage(key, value, expire, cookie -> {});
    }

    @Override
    public void setCookieCiphered(String key, String value) {
        assertKeyNotNull(key);
        assertValueNotNull(key, value);
        doSetCookieDegageCiphered(key, value, getDefaultExpire(), cookie -> {});
    }

    @Override
    public void setCookieCiphered(String key, String value, int expire) {
        assertKeyNotNull(key);
        assertValueNotNull(key, value);
        assertExpirePositive(expire);
        doSetCookieDegageCiphered(key, value, expire, cookie -> {});
    }

    // -----------------------------------------------------
    //                                                Degage
    //                                                ------
    @Override
    public void setCookieDegage(String key, String value, int expire, Consumer<Cookie> oneArgLambda) {
        assertKeyNotNull(key);
        assertValueNotNull(key, value);
        assertExpirePositive(expire);
        assertSetupCallbackNotNull(oneArgLambda);
        doSetCookieDegage(key, value, expire, oneArgLambda);
    }

    protected void doSetCookieDegage(String key, String value, int expire, Consumer<Cookie> cookieSetupper) {
        actuallySetCookie(key, value, getDefaultPath(), expire, cookieSetupper);
    }

    @Override
    public void setCookieDegageCiphered(String key, String value, int expire, Consumer<Cookie> oneArgLambda) {
        assertKeyNotNull(key);
        assertValueNotNull(key, value);
        assertExpirePositive(expire);
        assertSetupCallbackNotNull(oneArgLambda);
        doSetCookieDegageCiphered(key, value, expire, oneArgLambda);
    }

    protected void doSetCookieDegageCiphered(String key, String value, int expire, Consumer<Cookie> cookieSetupper) {
        final String encrypted = cookieCipher.encrypt(value);
        actuallySetCookie(key, encrypted, getDefaultPath(), expire, cookieSetupper);
    }

    protected void actuallySetCookie(String key, String value, String path, int expire, Consumer<Cookie> cookieSetupper) {
        final Cookie cookie = new Cookie(key, value);
        cookie.setPath(path);
        cookie.setMaxAge(expire);
        cookieSetupper.accept(cookie);
        registerCookieToResponse(cookie);
    }

    // -----------------------------------------------------
    //                                              Directly
    //                                              --------
    @Override
    public void setCookieDirectly(Cookie cookie) {
        assertCookieNotNull(cookie);
        registerCookieToResponse(cookie);
    }

    @Override
    public void setCookieDirectlyCiphered(Cookie cookie) {
        assertCookieNotNull(cookie);
        final String value = cookie.getValue();
        if (value != null) {
            cookie.setValue(cookieCipher.encrypt(value));
        }
        registerCookieToResponse(cookie);
    }

    // -----------------------------------------------------
    //                                  Register to Response
    //                                  --------------------
    protected void registerCookieToResponse(Cookie cookie) {
        getResponse().addCookie(cookie);
    }

    // ===================================================================================
    //                                                                          Get Cookie
    //                                                                          ==========
    @Override
    public OptionalThing<Cookie> getCookie(String key) {
        assertKeyNotNull(key);
        return doGetCookie(key);
    }

    protected OptionalThing<Cookie> doGetCookie(String key) {
        final Cookie[] cookies = getRequest().getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (key.equals(cookie.getName())) {
                    return OptionalThing.of(createSnapshotCookie(cookie));
                }
            }
        }
        return OptionalThing.ofNullable(null, () -> {
            throw new CookieNotFoundException("Not found the cookie by the key: " + key);
        });
    }

    protected Cookie createSnapshotCookie(Cookie src) {
        // not use close() to avoid dependency to ServletContainer
        final Cookie snapshot = new Cookie(src.getName(), src.getValue());
        snapshot.setPath(src.getPath());
        snapshot.setMaxAge(src.getMaxAge());
        final String domain = src.getDomain();
        if (domain != null) { // the setter has filter process
            snapshot.setDomain(domain);
        }
        snapshot.setSecure(src.getSecure());
        final String comment = src.getComment();
        if (comment != null) { // just in case
            snapshot.setComment(comment);
        }
        snapshot.setVersion(src.getVersion());
        snapshot.setHttpOnly(src.isHttpOnly());
        return snapshot;
    }

    @Override
    public OptionalThing<Cookie> getCookieCiphered(String key) {
        assertKeyNotNull(key);
        return doGetCookieCiphered(key);
    }

    protected OptionalThing<Cookie> doGetCookieCiphered(String key) {
        return doGetCookie(key).map(cookie -> { // is snapshot
            final String value = cookie.getValue();
            if (value != null) {
                try {
                    cookie.setValue(cookieCipher.decrypt(value));
                } catch (CookieCipherDecryptFailureException e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("...Ignoring decrypt failure to avoid hack cookie: " + value);
                    }
                    return null; /* treated as not found */
                }
            }
            return cookie;
        });
    }

    // ===================================================================================
    //                                                                       Remove Cookie
    //                                                                       =============
    @Override
    public void removeCookie(final String key) {
        assertKeyNotNull(key);
        removeCookie(key, getDefaultPath());
    }

    @Override
    public void removeCookie(final String key, final String path) {
        assertKeyNotNull(key);
        assertPathNotNull(path);
        final Cookie cookie = new Cookie(key, "");
        cookie.setPath(path);
        cookie.setMaxAge(0);
        setCookieDirectly(cookie);
    }

    // ===================================================================================
    //                                                                       Default Value
    //                                                                       =============
    @Override
    public String getDefaultPath() {
        if (defaultPath == null) {
            final String msg = "Not found the default path of cookie.";
            throw new IllegalStateException(msg);
        }
        return defaultPath;
    }

    @Override
    public Integer getDefaultExpire() {
        if (defaultExpire == null) {
            final String msg = "Not found the default expire of cookie.";
            throw new IllegalStateException(msg);
        }
        return defaultExpire;
    }

    // ===================================================================================
    //                                                                       Assert Helper
    //                                                                       =============
    protected void assertValueNotNull(final String key, final String value) {
        if (value == null) {
            final String msg = "The argument 'value' should not be null: key=" + key;
            throw new FwRequiredAssistNotFoundException(msg);
        }
    }

    protected void assertKeyNotNull(final String key) {
        if (key == null) {
            final String msg = "The argument 'key' should not be null.";
            throw new FwRequiredAssistNotFoundException(msg);
        }
    }

    protected void assertExpirePositive(final int expire) {
        if (expire <= 0) {
            final String msg = "The argument 'expire' should not be zero and minus.";
            throw new FwRequiredAssistNotFoundException(msg);
        }
    }

    protected void assertPathNotNull(final String path) {
        if (path == null) {
            final String msg = "The argument 'path' should not be null.";
            throw new FwRequiredAssistNotFoundException(msg);
        }
    }

    protected void assertSetupCallbackNotNull(final Consumer<Cookie> oneArgLambda) {
        if (oneArgLambda == null) {
            final String msg = "The argument 'oneArgLambda(cookieSetupper)' should not be null.";
            throw new FwRequiredAssistNotFoundException(msg);
        }
    }

    protected void assertCookieNotNull(final Cookie cookie) {
        if (cookie == null) {
            final String msg = "The argument 'cookie' should not be null.";
            throw new IllegalArgumentException(msg);
        }
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected HttpServletRequest getRequest() {
        return LaRequestUtil.getRequest();
    }

    protected HttpServletResponse getResponse() {
        return LaResponseUtil.getResponse();
    }
}
