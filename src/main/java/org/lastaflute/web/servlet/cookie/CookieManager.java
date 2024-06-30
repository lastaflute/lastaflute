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

import javax.servlet.http.Cookie;

import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 */
public interface CookieManager {

    // ===================================================================================
    //                                                                          Set Cookie
    //                                                                          ==========
    // -----------------------------------------------------
    //                                                 Basic
    //                                                 -----
    /**
     * Set the cookie by the key and value as the default path and default expire (max age). <br>
     * This method is for simple cookie settings.
     * @param key The key of the cookie. (NotNull)
     * @param value The value of the cookie. (NotNull)
     */
    void setCookie(String key, String value);

    /**
     * Set the cookie by the key and value and expiration (max age) as the path '/'. <br>
     * This method is for simple cookie settings.
     * @param key The key of the cookie. (NotNull)
     * @param value The value of the cookie. (NotNull)
     * @param expire The expire of the cookie in seconds. (NotZero, NotMinus)
     */
    void setCookie(String key, String value, int expire);

    /**
     * Set the cookie by the key and value as the path '/' and default expire (max age). <br>
     * This method is for simple cookie settings with cipher by cookie cipher.
     * @param key The key of the cookie. (NotNull)
     * @param value The value of the cookie, which is ciphered in this method. (NotNull)
     */
    void setCookieCiphered(String key, String value);

    /**
     * Set the cookie by the key and value and expiration (max age) as the path '/'. <br>
     * This method is for simple cookie settings with cipher by cookie cipher. <br>
     * @param key The key of the cookie. (NotNull)
     * @param value The value of the cookie, which is ciphered in this method. (NotNull)
     * @param expire The expire of the cookie in seconds. (NotZero, NotMinus)
     */
    void setCookieCiphered(String key, String value, int expire);

    // -----------------------------------------------------
    //                                                Degage
    //                                                ------
    /**
     * Set the cookie by the key and value and expiration (max age) as the path '/'. <br>
     * This method is for detail cookie settings as plain cookie, <br>
     * with callback of setting up cookie finally. (You can handle cookie object)
     * @param key The key of the cookie. (NotNull)
     * @param value The value of the cookie. (NotNull)
     * @param expire The expire of the cookie in seconds. (NotZero, NotMinus)
     * @param oneArgLambda The callback of setting up cookie finally for various purposes. (NotNull)
     */
    void setCookieDegage(String key, String value, int expire, Consumer<Cookie> oneArgLambda);

    /**
     * Set the cookie by the key and value and expiration (max age) as the path '/'. <br>
     * This method is for detail cookie settings with cipher by cookie cipher, <br>
     * with callback of setting up cookie finally. (You can handle cookie object)
     * @param key The key of the cookie. (NotNull)
     * @param value The value of the cookie, which is ciphered in this method. (NotNull)
     * @param expire The expire of the cookie in seconds. (NotZero, NotMinus)
     * @param oneArgLambda The callback of setting up cookie finally for various purposes. (NotNull)
     */
    void setCookieDegageCiphered(String key, String value, int expire, Consumer<Cookie> oneArgLambda);

    // -----------------------------------------------------
    //                                              Directly
    //                                              --------
    /**
     * Set the cookie directly by your favorite settings using {@link Cookie} of the Servlet. <br>
     * This method is for detail cookie settings. <br>
     * The setting specification is just as the Servlet.
     * @param cookie The cookie object of the Servlet. (NotNull)
     */
    void setCookieDirectly(Cookie cookie);

    /**
     * Set the cookie directly by your favorite settings using {@link Cookie} of the Servlet. <br>
     * This method is for detail cookie settings with cipher by cookie cipher. <br>
     * The setting specification is just as the Servlet.
     * @param cookie The cookie object of the Servlet. (NotNull)
     */
    void setCookieDirectlyCiphered(Cookie cookie);

    // ===================================================================================
    //                                                                          Get Cookie
    //                                                                          ==========
    /**
     * Get the cookie (snapshot) found by the key.
     * @param key The key of the cookie. (NotNull)
     * @return The optional cookie object of the key. (NotNull, EmptyAllowed: when not found)
     */
    OptionalThing<Cookie> getCookie(String key);

    /**
     * Get the ciphered cookie (snapshot) found by the key with automatically-decrypted value.
     * @param key The key of the cookie. (NotNull)
     * @return The optional cookie object of the key. (NotNull, EmptyAllowed: when not found)
     */
    OptionalThing<Cookie> getCookieCiphered(String key);

    // ===================================================================================
    //                                                                       Remove Cookie
    //                                                                       =============
    /**
     * Remove the cookie of the default path by the key.
     * @param key The key of the cookie. (NotNull)
     */
    void removeCookie(String key);

    /**
     * Remove the cookie of the specified path by the key.
     * @param key The key of the cookie. (NotNull)
     * @param path The path of the cookie. (NotNull)
     */
    void removeCookie(String key, String path);

    // ===================================================================================
    //                                                                       Default Value
    //                                                                       =============
    /**
     * Get the default path for the cookie created by this manager.
     * @return The string of path for cookie path. (NotNull: exception if no default)
     */
    String getDefaultPath();

    /**
     * Get the default expire (max age) for the cookie created by this manager.
     * @return The value of expire for cookie max age. (NotNull: exception if no default)
     */
    Integer getDefaultExpire();
}
