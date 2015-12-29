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
package org.lastaflute.web.login;

import org.dbflute.Entity;
import org.dbflute.optional.OptionalEntity;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.web.login.exception.LoginFailureException;
import org.lastaflute.web.login.exception.LoginRequiredException;
import org.lastaflute.web.login.option.LoginOpCall;
import org.lastaflute.web.login.option.RememberMeLoginOpCall;
import org.lastaflute.web.login.redirect.LoginRedirectBean;
import org.lastaflute.web.login.redirect.LoginRedirectSuccessCall;
import org.lastaflute.web.response.HtmlResponse;

/**
 * @author jflute
 */
public interface LoginManager {

    // ===================================================================================
    //                                                                           Find User
    //                                                                           =========
    /**
     * Check the user is login-able. (basically for validation)
     * @param email The email address for the login user. (NotNull)
     * @param password The plain password for the login user, which is encrypted in this method. (NotNull)
     * @return true if the user is login-able.
     */
    boolean checkUserLoginable(String email, String password);

    /**
     * Find the login user in the database.
     * @param email The email address for the login user. (NotNull)
     * @param password The plain password for the login user, which is encrypted in this method. (NotNull)
     * @return The optional entity of the found user. (NotNull, EmptyAllowed: when the login user is not found)
     */
    OptionalEntity<? extends Object> findLoginUser(String email, String password);

    /**
     * Find the login user in the database.
     * @param userId for the login user. (NotNull)
     * @return The optional entity of the found user. (NotNull, EmptyAllowed: when the login user is not found)
     */
    OptionalEntity<? extends Object> findLoginUser(Object userId);

    // ===================================================================================
    //                                                                         Basic Login
    //                                                                         ===========
    /**
     * Do login for the user by account and password.
     * @param email The email address for the login user. (NotNull)
     * @param password The plain password for the login user, which is encrypted in this method. (NotNull)
     * @param opLambda The callback for option of login. e.g. useAutoLogin (NotNull)
     * @throws LoginFailureException When it fails to login by the user info.
     */
    void login(String email, String password, LoginOpCall opLambda) throws LoginFailureException;

    /**
     * Do login for the user by account and password. <br>
     * You should specify normal response in the last callback argument. <br>
     * But the response may be switched to response of previously requested action if exists.
     * @param email The email address for the login user. (NotNull)
     * @param password The plain password for the login user, which is encrypted in this method. (NotNull)
     * @param opLambda The callback for option of login. e.g. useAutoLogin (NotNull)
     * @param oneArgLambda The callback for login redirect when success of login, normal response is returned. (NotNull)
     * @return The response of action, controlled as login-redirect, previously requested response or normal response. (NotNull)
     * @throws LoginFailureException When it fails to login by the user info.
     */
    HtmlResponse loginRedirect(String email, String password, LoginOpCall opLambda, LoginRedirectSuccessCall oneArgLambda)
            throws LoginFailureException;

    /**
     * Do login with given user entity, e.g. used for partner authentication. (remember-me or not) <br>
     * No authentication here so the email and password is basically for remember-me key. 
     * @param givenEntity The given entity for user. (NullAllowed: if null, find user by email and password)
     * @param opLambda The callback for option of login. e.g. useAutoLogin (NotNull)
     */
    void givenLogin(Entity givenEntity, LoginOpCall opLambda);

    /**
     * Do login for the user by user ID (means identity login). (for remember-me or partner login)
     * @param userId for the login user. (NotNull)
     * @param opLambda The callback for option of login. e.g. useAutoLogin (NotNull)
     * @throws LoginFailureException When it fails to login by the user info.
     */
    void identityLogin(Object userId, LoginOpCall opLambda) throws LoginFailureException;

    /**
     * Re-select user bean of session if exists. <br>
     * (synchronize user bean with database)
     * @throws LoginFailureException When it fails to find the user.
     */
    void reselectSessionUserBeanIfExists() throws LoginFailureException;

    /**
     * Encrypt the password of the login user.
     * @param plainPassword The plain password for the login user, which is encrypted in this method. (NotNull)
     * @return The encrypted string of the password. (NotNull)
     */
    String encryptPassword(String plainPassword);

    // ===================================================================================
    //                                                                    RememberMe Login
    //                                                                    ================
    /**
     * Remember me for me. (do remember-me login if authentication token exists in cookie)
     * @param opLambda The callback for remember-me login. e.g. update-token (NotNull)
     * @return Is the remember-me success?
     */
    boolean rememberMe(RememberMeLoginOpCall opLambda);

    // ===================================================================================
    //                                                                              Logout
    //                                                                              ======
    /**
     * Logout for the user. (remove session and cookie info)
     */
    void logout();

    // ===================================================================================
    //                                                                         Login Check
    //                                                                         ===========
    /**
     * Check login required for the requested action. (with remember-me, preparing login-redirect)
     * @param resource The resource of login handling to determine required or not. (NotNull)
     * @throws LoginRequiredException When it fails to access the action for non-login.
     */
    void checkLoginRequired(LoginHandlingResource resource) throws LoginRequiredException;

    /**
     * Is the action login-required?
     * @param resource The resource of login handling to determine required or not. (NotNull)
     * @return The determination, true or false.
     */
    boolean isLoginRequiredAction(LoginHandlingResource resource);

    /**
     * Get the user bean in session. (you can determine login or not) <br>
     * Also you can use lookupSessionUserBean() from concrete type instance,
     * which provides user bean as generic type.
     * @return The user bean in session. (NotNull, EmptyAllowed: means not-login)
     */
    OptionalThing<? extends UserBean<?>> getSessionUserBean(); // thanks, feedback of "? extends" way

    /**
     * Get the type of user bean as save key of session.
     * @return The type of user bean. (NotNull)
     */
    Class<?> getSaveKeyUserBeanType();

    /**
     * Is the action for login? (login action or not)
     * @param resource The resource of login handling to determine login action or not. (NotNull)
     * @return The determination, true or false.
     */
    boolean isLoginAction(LoginHandlingResource resource);

    // ===================================================================================
    //                                                                      Login Redirect
    //                                                                      ==============
    /**
     * Save requested info to session for login-redirect.
     */
    void saveRequestedLoginRedirectInfo();

    /**
     * Redirect to login action as login-redirect.
     * @return The response of HTML for login action. (NotNull)
     */
    HtmlResponse redirectToLoginAction();

    /**
     * Get the bean of login redirect saved in session.
     * @return The optional bean of login redirect. (NotNull, EmptyAllowed: when no bean in session)
     */
    OptionalThing<LoginRedirectBean> getLoginRedirectBean();

    /**
     * Clear login redirect bean from session.
     */
    void clearLoginRedirectBean();

    /**
     * Redirect to the requested action before perform-login if it needs (perform-login and redirect info exists).
     * @param response The normal response when no saved requested action. (NotNull)
     * @return The real response for login redirect. (NotNull)
     */
    HtmlResponse switchToRequestedActionIfExists(HtmlResponse response);
}
