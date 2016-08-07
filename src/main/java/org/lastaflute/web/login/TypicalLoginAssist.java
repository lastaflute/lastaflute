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
package org.lastaflute.web.login;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

import javax.annotation.Resource;

import org.dbflute.Entity;
import org.dbflute.helper.HandyDate;
import org.dbflute.optional.OptionalEntity;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.security.PrimaryCipher;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.login.credential.LoginCredential;
import org.lastaflute.web.login.exception.LoginFailureException;
import org.lastaflute.web.login.exception.LoginRequiredException;
import org.lastaflute.web.login.option.LoginOpCall;
import org.lastaflute.web.login.option.LoginOption;
import org.lastaflute.web.login.option.LoginSpecifiedOption;
import org.lastaflute.web.login.option.RememberMeLoginOpCall;
import org.lastaflute.web.login.option.RememberMeLoginOption;
import org.lastaflute.web.login.option.RememberMeLoginSpecifiedOption;
import org.lastaflute.web.login.redirect.LoginRedirectBean;
import org.lastaflute.web.login.redirect.LoginRedirectSuccessCall;
import org.lastaflute.web.path.ActionPathResolver;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.servlet.cookie.CookieManager;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <ID> The type of user ID.
 * @param <USER_BEAN> The type of user bean.
 * @param <USER_ENTITY> The type of user entity or model.
 * @author jflute
 */
public abstract class TypicalLoginAssist<ID, USER_BEAN extends UserBean<ID>, USER_ENTITY> implements LoginManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(TypicalLoginAssist.class);

    /** The session key of user bean. (actually suffix added per assist) */
    private static final String USER_BEAN_KEY = LastaWebKey.USER_BEAN_KEY;

    /** The delimiter of remember-me login value saved in cookie. */
    private static final String REMEMBER_ME_COOKIE_DELIMITER = ":>:<:";

    /** The default expire days for remember-me login access token. */
    private static final int REMEMBER_ME_ACCESS_TOKEN_DEFAULT_EXPIRE_DAYS = 7; // you can change by override

    /** The pattern of expire date for remember-me. */
    private static final String REMEMBER_ME_ACCESS_TOKEN_EXPIRE_DATE_PATTERN = "yyyy/MM/dd HH:mm:ss.SSS";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    @Resource
    private PrimaryCipher primaryCipher;

    @Resource
    private TimeManager timeManager;

    @Resource
    private RequestManager requestManager;

    @Resource
    private SessionManager sessionManager;

    @Resource
    private CookieManager cookieManager;

    @Resource
    private ActionPathResolver actionPathResolver;

    // ===================================================================================
    //                                                                           Find User
    //                                                                           =========
    // -----------------------------------------------------
    //                                            Credential
    //                                            ----------
    @Override
    public boolean checkUserLoginable(LoginCredential credential) {
        final CredentialChecker checker = new CredentialChecker(credential);
        checkCredential(checker);
        if (!checker.isChecked()) {
            handleUnknownLoginCredential(credential);
        }
        return checker.isLoginable();
    }

    /**
     * Resolve the login credential by agent.
     * @param checker The checker of login credential to determine the user existence. (NotNull)
     */
    protected abstract void checkCredential(CredentialChecker checker);

    @Override
    public OptionalEntity<USER_ENTITY> findLoginUser(LoginCredential credential) {
        final CredentialResolver resolver = new CredentialResolver(credential);
        resolveCredential(resolver);
        if (!resolver.isResolved()) {
            handleUnknownLoginCredential(credential);
        }
        return resolver.getResolvedEntity();
    }

    /**
     * Resolve the login credential by agent.
     * @param resolver The resolver of login credential to find entity. (NotNull)
     */
    protected abstract void resolveCredential(CredentialResolver resolver);

    protected void handleUnknownLoginCredential(LoginCredential credential) {
        throw new IllegalStateException("Unknown login credential: " + credential);
    }

    // checker and resolver are non-static inner class to keep generic type
    // and stateful way to avoid generic hell (could not resolve credential generic type...)
    // similar to chain of responsibility?

    protected class CredentialChecker {

        protected final LoginCredential credential;
        protected boolean checked;
        protected boolean loginable;

        public CredentialChecker(LoginCredential credential) {
            this.credential = credential;
        }

        public <CREDENTIAL extends LoginCredential> void check(Class<CREDENTIAL> credentialType,
                CredentialDeterminer<CREDENTIAL, USER_ENTITY> oneArgLambda) {
            if (!checked && credentialType.isAssignableFrom(credential.getClass())) {
                checked = true;
                @SuppressWarnings("unchecked")
                final CREDENTIAL nativeCredential = (CREDENTIAL) credential;
                loginable = oneArgLambda.determine(nativeCredential);
            }
        }

        public boolean isChecked() {
            return checked;
        }

        public boolean isLoginable() {
            return loginable;
        }
    }

    protected interface CredentialDeterminer<CREDENTIAL extends LoginCredential, USER_ENTITY> {

        boolean determine(CREDENTIAL credential);
    }

    protected class CredentialResolver {

        protected final LoginCredential credential;
        protected boolean resolved;
        protected OptionalEntity<USER_ENTITY> resolvedEntity = OptionalEntity.empty();

        public CredentialResolver(LoginCredential credential) {
            this.credential = credential;
        }

        public <CREDENTIAL extends LoginCredential> void resolve(Class<CREDENTIAL> credentialType,
                CredentialEntityFinder<CREDENTIAL, USER_ENTITY> oneArgLambda) {
            if (!resolved && credentialType.isAssignableFrom(credential.getClass())) {
                resolved = true;
                @SuppressWarnings("unchecked")
                final CREDENTIAL nativeCredential = (CREDENTIAL) credential;
                resolvedEntity = oneArgLambda.find(nativeCredential);
            }
        }

        public boolean isResolved() {
            return resolved;
        }

        public OptionalEntity<USER_ENTITY> getResolvedEntity() {
            return resolvedEntity;
        }
    }

    protected interface CredentialEntityFinder<CREDENTIAL extends LoginCredential, USER_ENTITY> {

        OptionalEntity<USER_ENTITY> find(CREDENTIAL credential);
    }

    // -----------------------------------------------------
    //                                              Identity
    //                                              --------
    /**
     * Find the login user in the database.
     * @param userId for the login user. (NotNull)
     * @return The optional entity of the found user. (NotNull, EmptyAllowed: when the login user is not found)
     */
    @Override
    public OptionalEntity<USER_ENTITY> findLoginUser(Object userId) {
        assertUserIdRequired(userId);
        try {
            @SuppressWarnings("unchecked")
            final ID castId = (ID) userId;
            return doFindLoginUser(castId);
        } catch (ClassCastException e) { // also find method, because of generic cast
            throw new IllegalStateException("Cannot cast the user ID: " + userId.getClass() + ", " + userId, e);
        }
    }

    /**
     * Finding the login user in the database.
     * @param userId for the login user. (NotNull)
     * @return The optional entity of the found user. (NotNull, EmptyAllowed: when the login user is not found)
     */
    protected abstract OptionalEntity<USER_ENTITY> doFindLoginUser(ID userId);

    // ===================================================================================
    //                                                                         Login Logic
    //                                                                         ===========
    // -----------------------------------------------------
    //                                       Login Interface
    //                                       ---------------
    @Override
    public void login(LoginCredential credential, LoginOpCall opLambda) throws LoginFailureException {
        doLogin(credential, createLoginOption(opLambda)); // exception if login failure
    }

    @Override
    public HtmlResponse loginRedirect(LoginCredential credential, LoginOpCall opLambda, LoginRedirectSuccessCall oneArgLambda)
            throws LoginFailureException {
        doLogin(credential, createLoginOption(opLambda)); // exception if login failure
        return switchToRequestedActionIfExists(oneArgLambda.success()); // so success only here
    }

    @Override
    @SuppressWarnings("unchecked")
    public void givenLogin(Entity givenEntity, LoginOpCall opLambda) throws LoginFailureException {
        doLoginByGivenEntity((USER_ENTITY) givenEntity, createLoginOption(opLambda));
    }

    @Override
    public void identityLogin(Object userId, LoginOpCall opLambda) throws LoginFailureException {
        doLoginByIdentity(userId, createLoginOption(opLambda));
    }

    protected LoginOption createLoginOption(LoginOpCall opLambda) {
        final LoginOption option = new LoginOption();
        opLambda.callback(option);
        return option;
    }

    // -----------------------------------------------------
    //                                        Actually Login
    //                                        --------------
    /**
     * Do actually login for the user by credential.
     * @param credential The login credential for the login user. (NotNull)
     * @param option The option of login specified by caller. (NotNull)
     * @throws LoginFailureException When it fails to do login by the user info.
     */
    protected void doLogin(LoginCredential credential, LoginSpecifiedOption option) throws LoginFailureException {
        handleLoginSuccess(findLoginUser(credential).orElseThrow(() -> {
            final String msg = "Not found the user by the credential: " + credential + ", " + option;
            return handleLoginFailure(msg, credential, OptionalThing.of(option));
        }), option);
    }

    /**
     * Do actually login for the user by given entity. (no silent)
     * @param givenEntity The given entity for user. (NotNull)
     * @param option The option of login specified by caller. (NotNull)
     */
    protected void doLoginByGivenEntity(USER_ENTITY givenEntity, LoginSpecifiedOption option) {
        assertGivenEntityRequired(givenEntity);
        handleLoginSuccess(givenEntity, option);
    }

    /**
     * Do actually login for the user by identity (user ID). (no silent)
     * @param userId for the login user. (NotNull)
     * @param option The option of login specified by caller. (NotNull)
     * @throws LoginFailureException When it fails to do login by the user info.
     */
    protected void doLoginByIdentity(Object userId, LoginSpecifiedOption option) throws LoginFailureException {
        assertUserIdRequired(userId);
        handleLoginSuccess(findLoginUser(userId).orElseThrow(() -> {
            String msg = "Not found the user by the user ID: " + userId + ", " + option;
            return handleLoginFailure(msg, userId, OptionalThing.of(option));
        }), option);
    }

    /**
     * Handle login success for the found login user.
     * @param userEntity The found entity of the login user. (NotNull)
     * @param option The option of login specified by caller. (NotNull)
     */
    protected void handleLoginSuccess(USER_ENTITY userEntity, LoginSpecifiedOption option) {
        assertUserEntityRequired(userEntity);
        final USER_BEAN userBean = saveLoginInfoToSession(userEntity);
        if (userBean instanceof SyncCheckable) {
            ((SyncCheckable) userBean).manageLastestSyncCheckTime(timeManager.currentDateTime());
        }
        if (option.isRememberMe()) {
            saveRememberMeKeyToCookie(userEntity, userBean);
        }
        if (!option.isSilentLogin()) { // mainly here
            saveLoginHistory(userEntity, userBean, option);
            processOnBrightLogin(userEntity, userBean, option);
        } else {
            processOnSilentLogin(userEntity, userBean, option);
        }
    }

    protected LoginFailureException handleLoginFailure(String msg, Object resource, OptionalThing<LoginSpecifiedOption> option) {
        return newLoginFailureException(msg);
    }

    protected LoginFailureException newLoginFailureException(String msg) {
        return new LoginFailureException(msg);
    }

    // -----------------------------------------------------
    //                                     UserBean Handling
    //                                     -----------------
    /**
     * Save login info as user bean to session.
     * @param userEntity The entity of the found user. (NotNull)
     * @return The user bean saved in session. (NotNull)
     */
    protected USER_BEAN saveLoginInfoToSession(USER_ENTITY userEntity) {
        regenerateSessionId();
        logger.debug("...Saving login info to session");
        final USER_BEAN userBean = createUserBean(userEntity);
        sessionManager.setAttribute(getUserBeanKey(), userBean);
        return userBean;
    }

    /**
     * Regenerate session ID for security. <br>
     * call invalidate() but it inherits existing session attributes.
     */
    protected void regenerateSessionId() {
        logger.debug("...Regenerating session ID for security");
        sessionManager.regenerateSessionId();
    }

    /**
     * Create the user bean for the user.
     * @param userEntity The selected entity of login user. (NotNull)
     * @return The new-created instance of user bean to be saved in session. (NotNull)
     */
    protected abstract USER_BEAN createUserBean(USER_ENTITY userEntity);

    protected String getUserBeanKey() {
        // suffix for multiple login assist (user bean should be different per assist)
        return USER_BEAN_KEY + "." + getUserBeanType().getSimpleName();
    }

    @Override
    public void reselectSessionUserBeanIfExists() throws LoginFailureException {
        getSavedUserBean().ifPresent(oldBean -> {
            inheritUserBeanAdditionalInfo(oldBean);
            final ID userId = oldBean.getUserId();
            logger.debug("...Re-selecting user bean in session: userId={}", userId);
            final USER_ENTITY userEntity = findLoginUser(userId).orElseThrow(() -> { // might be already left
                logout(); // to clear old user info in session
                final OptionalThing<LoginSpecifiedOption> emptyOption = OptionalThing.ofNullable(null, () -> {
                    throw new IllegalStateException("Not found the login option when reselect: userId=" + userId);
                });
                return handleLoginFailure("Not found the user by the user ID: " + userId, userId, emptyOption);
            });
            sessionManager.setAttribute(getUserBeanKey(), createUserBean(userEntity));
        });
    }

    protected void inheritUserBeanAdditionalInfo(USER_BEAN oldBean) {
        // do nothing as default
    }

    // -----------------------------------------------------
    //                                   RememberMe Handling
    //                                   -------------------
    /**
     * Save remember-me key to cookie.
     * @param userEntity The selected entity of login user. (NotNull)
     * @param userBean The user bean saved in session. (NotNull)
     */
    protected void saveRememberMeKeyToCookie(USER_ENTITY userEntity, USER_BEAN userBean) {
        final int expireDays = getRememberMeAccessTokenExpireDays();
        getCookieRememberMeKey().ifPresent(cookieKey -> {
            doSaveRememberMeCookie(userEntity, userBean, expireDays, cookieKey);
        });
    }

    /**
     * Get the expire days of both access token and cookie value. <br>
     * You can change it by override.
     * @return The count of expire days. (NotMinus, NotZero)
     */
    protected int getRememberMeAccessTokenExpireDays() {
        return REMEMBER_ME_ACCESS_TOKEN_DEFAULT_EXPIRE_DAYS; // as default for compatibility
    }

    /**
     * Get the key of remember-me saved in cookie.
     * @return The string key for cookie. (NotNull, EmptyAllowed: when no remember-me)
     */
    protected abstract OptionalThing<String> getCookieRememberMeKey();

    /**
     * Do save remember-me key to cookie.
     * @param userEntity The selected entity of login user. (NotNull)
     * @param userBean The user bean saved in session. (NotNull)
     * @param expireDays The expire days of both access token and cookie value.
     * @param cookieKey The key of the cookie. (NotNull)
     */
    protected void doSaveRememberMeCookie(USER_ENTITY userEntity, USER_BEAN userBean, int expireDays, String cookieKey) {
        logger.debug("...Saving remember-me key to cookie: key={}", cookieKey);
        final String value = buildRememberMeCookieValue(userEntity, userBean, expireDays);
        final int expireSeconds = expireDays * 60 * 60 * 24; // cookie's expire, same as access token
        cookieManager.setCookieCiphered(cookieKey, value, expireSeconds);
    }

    /**
     * Build the value for remember-me saved in cookie. <br>
     * You can change access token's structure by override. #change_access_token
     * @param userEntity The selected entity of login user. (NotNull)
     * @param userBean The user bean saved in session. (NotNull)
     * @param expireDays The count of expired days from current times. (NotNull)
     * @return The string value for remember-me. (NotNull)
     */
    protected String buildRememberMeCookieValue(USER_ENTITY userEntity, USER_BEAN userBean, int expireDays) {
        final String autoLoginKey = createRememberMeKey(userEntity, userBean);
        final String delimiter = getRememberMeDelimiter();
        final HandyDate currentHandyDate = timeManager.currentHandyDate();
        final HandyDate expireDate = currentHandyDate.addDay(expireDays); // access token's expire
        return autoLoginKey + delimiter + formatForRememberMeExpireDate(expireDate);
    }

    /**
     * Create remember-me key for the user. <br>
     * You can change user key's structure by override. #change_user_key
     * @param userEntity The selected entity of login user. (NotNull)
     * @param userBean The user bean saved in session. (NotNull)
     * @return The string expression for remember-me key. (NotNull)
     */
    protected String createRememberMeKey(USER_ENTITY userEntity, USER_BEAN userBean) {
        return String.valueOf(userBean.getUserId()); // as default (override if it needs)
    }

    /**
     * Save the history of the success login. (except silent login) <br>
     * For example, you can save the login user's info to database. <br>
     * You should use other transaction because this process is not related to main business.
     * <pre>
     * <span style="color: #3F7E5E">// use other transaction</span>
     * <span style="color: #0000C0">transactionStage</span>.requiresNew(tx <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     ...
     * });
     * 
     * <span style="color: #3F7E5E">// and also asynchronous (then exception is handled by the other thread)</span>
     * <span style="color: #0000C0">asyncManager</span>.async(() <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #0000C0">transactionStage</span>.requiresNew(tx <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *         ...
     *     });
     * });
     * </pre>
     * @param userEntity The entity of the login user. (NotNull)
     * @param userBean The user bean of the login user, already saved in session. (NotNull)
     * @param option The option of login specified by caller. (NotNull)
     */
    protected abstract void saveLoginHistory(USER_ENTITY userEntity, USER_BEAN userBean, LoginSpecifiedOption option);

    // -----------------------------------------------------
    //                                    Process on Success
    //                                    ------------------
    /**
     * Process your favorite logic on bright login (except silent-login).
     * @param userEntity The entity of the login user. (NotNull)
     * @param userBean The user bean of the login user, already saved in session. (NotNull)
     * @param option The option of login specified by caller. (NotNull)
     */
    protected void processOnBrightLogin(USER_ENTITY userEntity, USER_BEAN userBean, LoginSpecifiedOption option) {
        // do nothing as default
    }

    /**
     * Process your favorite logic on silent login.
     * @param userEntity The entity of the login user. (NotNull)
     * @param userBean The user bean of the login user, already saved in session. (NotNull)
     * @param option The option of login specified by caller. (NotNull)
     */
    protected void processOnSilentLogin(USER_ENTITY userEntity, USER_BEAN userBean, LoginSpecifiedOption option) {
        // do nothing as default
    }

    /** {@inheritDoc} */
    @Override
    public String encryptPassword(String plainPassword) {
        return primaryCipher.oneway(plainPassword);
    }

    // ===================================================================================
    //                                                                    RememberMe Login
    //                                                                    ================
    @Override
    public boolean rememberMe(RememberMeLoginOpCall opLambda) {
        return getCookieRememberMeKey().map(cookieKey -> {
            return delegateRememberMe(cookieKey, opLambda);
        }).orElse(false);
    }

    protected boolean delegateRememberMe(String cookieKey, RememberMeLoginOpCall opLambda) {
        return cookieManager.getCookieCiphered(cookieKey).map(cookie -> {
            final String cookieValue = cookie.getValue();
            if (cookieValue != null && cookieValue.trim().length() > 0) {
                final String[] valueAry = cookieValue.split(getRememberMeDelimiter());
                final RememberMeLoginOption option = createRememberMeLoginOption(opLambda);
                final Boolean handled = handleRememberMeCookie(valueAry, option);
                if (handled != null) {
                    return handled;
                }
                if (handleRememberMeInvalidCookie(cookieValue, valueAry)) { // you can also retry
                    return true; // success by the handling
                }
            }
            return false;
        }).orElse(false);
    }

    protected String getRememberMeDelimiter() {
        return REMEMBER_ME_COOKIE_DELIMITER;
    }

    protected RememberMeLoginOption createRememberMeLoginOption(RememberMeLoginOpCall opLambda) {
        final RememberMeLoginOption option = new RememberMeLoginOption();
        opLambda.callback(option);
        return option;
    }

    protected boolean handleRememberMeInvalidCookie(String cookieValue, String[] valueAry) {
        return false; // if invalid length, it might be hack so do nothing here as default
    }

    // -----------------------------------------------------
    //                                       Cookie Handling
    //                                       ---------------
    /**
     * Handle remember-me cookie (and do remember-me). <br>
     * You can change access token's structure by override. #change_access_token
     * @param valueAry The array of cookie values. (NotNull)
     * @param option The option of remember-me login specified by caller. (NotNull)
     * @return The determination of remember-me, true or false or null. (NullAllowed: means invalid cookie) 
     */
    protected Boolean handleRememberMeCookie(String[] valueAry, RememberMeLoginSpecifiedOption option) {
        if (valueAry.length != 2) { // invalid cookie
            return null;
        }
        final String userKey = valueAry[0]; // resolved by identity login
        final String expireDate = valueAry[1]; // AccessToken's expire
        if (isValidRememberMeCookie(userKey, expireDate)) {
            final ID userId = convertCookieUserKeyToUserId(userKey);
            return doRememberMe(userId, expireDate, option);
        }
        return null;
    }

    /**
     * Are the user ID and expire date extracted from cookie valid?
     * @param userKey The key of the login user. (NotNull)
     * @param expireDate The string expression for expire date of remember-me access token. (NotNull)
     * @return Is a validation for remember-me OK?
     */
    protected boolean isValidRememberMeCookie(String userKey, String expireDate) {
        final String currentDate = formatForRememberMeExpireDate(timeManager.currentHandyDate());
        if (currentDate.compareTo(expireDate) < 0) { // String v.s. String
            return true; // valid access token within time limit
        }
        // expired here
        logger.debug("The access token for remember-me expired: userKey={} expireDate={}", userKey, expireDate);
        return false;
    }

    protected String formatForRememberMeExpireDate(HandyDate expireDate) {
        return expireDate.toDisp(REMEMBER_ME_ACCESS_TOKEN_EXPIRE_DATE_PATTERN);
    }

    /**
     * Convert user key to user ID by your rule. #change_user_key <br>
     * The default rule is simple conversion to number, you can change it by overriding.
     * @param userKey The string key for the login user, same as remember-me key. (NotNull)
     * @return The ID for the login user. (NotNull)
     */
    protected ID convertCookieUserKeyToUserId(String userKey) {
        final ID userId;
        try {
            userId = toTypedUserId(userKey); // as default (override if it needs)
        } catch (NumberFormatException e) {
            throw new LoginFailureException("Invalid user key (not ID): " + userKey, e);
        }
        return userId;
    }

    protected abstract ID toTypedUserId(String userKey);

    // -----------------------------------------------------
    //                                   Actually RememberMe
    //                                   -------------------
    /**
     * Do actually remember-me for the user.
     * @param userId The ID of the login user, used by identity login. (NotNull)
     * @param expireDate The string expression for expire date of remember-me access token. (NotNull)
     * @param option The option of remember-me login specified by caller. (NotNull)
     * @return Is the remember-me success?
     */
    protected boolean doRememberMe(ID userId, String expireDate, RememberMeLoginSpecifiedOption option) {
        final boolean updateToken = option.isUpdateToken();
        final boolean silentLogin = option.isSilentLogin();
        if (logger.isDebugEnabled()) {
            final StringBuilder sb = new StringBuilder();
            sb.append("...Doing remember-me: user=").append(userId);
            sb.append(", expire=").append(expireDate);
            if (updateToken) {
                sb.append(", updateToken");
            }
            if (silentLogin) {
                sb.append(", silently");
            }
            logger.debug(sb.toString());
        }
        try {
            identityLogin(userId, op -> op.rememberMe(updateToken).silentLogin(silentLogin));
            return true;
        } catch (NumberFormatException invalidUserKey) { // just in case
            // to know invalid user key or bug
            logger.debug("*The user key might be invalid: {}, {}", userId, invalidUserKey.getMessage());
            return false;
        } catch (LoginFailureException autoLoginFailed) {
            return false;
        }
    }

    // ===================================================================================
    //                                                                              Logout
    //                                                                              ======
    @Override
    public void logout() {
        sessionManager.removeAttribute(getUserBeanKey());
        getCookieRememberMeKey().ifPresent(cookieKey -> {
            cookieManager.removeCookie(cookieKey);
        });
    }

    // ===================================================================================
    //                                                                         Login Check
    //                                                                         ===========
    // -----------------------------------------------------
    //                                         LoginRequired
    //                                         -------------
    @Override
    public void checkLoginRequired(LoginHandlingResource resource) throws LoginRequiredException {
        if (isLoginRequiredAction(resource)) {
            asLoginRequired(resource);
        } else {
            asNonLoginRequired(resource);
        }
    }

    /**
     * Check as the login-required action.
     * @param resource The resource of login handling to determine. (NotNull)
     * @throws LoginRequiredException When it fails to access the action for non-login.
     */
    protected void asLoginRequired(LoginHandlingResource resource) throws LoginRequiredException {
        logger.debug("...Checking login status for login required");
        if (tryAlreadyLoginOrRememberMe(resource)) {
            checkPermission(resource); // throws if denied
            return; // Good
        }
        if (needsSavingRequestedLoginRedirect(resource)) {
            saveRequestedLoginRedirectInfo();
        }
        throwLoginRequiredException("Cannot access the action: " + resource);
    }

    protected boolean needsSavingRequestedLoginRedirect(LoginHandlingResource resource) {
        return !resource.isApiExecute(); // unneeded when API
    }

    protected void throwLoginRequiredException(String msg) {
        throw new LoginRequiredException(msg);
    }

    // -----------------------------------------------------
    //                          Already Login or Remember Me
    //                          ----------------------------
    protected boolean tryAlreadyLoginOrRememberMe(LoginHandlingResource resource) {
        return doTryAlreadyLogin(resource) || doTryRememberMe(resource);
    }

    protected boolean doTryAlreadyLogin(LoginHandlingResource resource) {
        return getSavedUserBean().map(userBean -> {
            if (!syncCheckLoginSessionIfNeeds(userBean)) {
                return false;
            }
            clearLoginRedirectBean();
            logger.debug("...Passing login check as already-login");
            return true;
        }).orElse(false);
    }

    protected boolean doTryRememberMe(LoginHandlingResource resource) {
        final boolean updateToken = isUpdateTokenWhenRememberMe(resource);
        final boolean silently = isSilentlyWhenRememberMe(resource);
        final boolean success = rememberMe(op -> op.updateToken(updateToken).silentLogin(silently));
        return success && getSavedUserBean().map(userBean -> {
            clearLoginRedirectBean();
            logger.debug("...Passing login check as remember-me");
            return true;
        }).orElse(false);
    }

    protected boolean isUpdateTokenWhenRememberMe(LoginHandlingResource resource) {
        return false; // as default
    }

    protected boolean isSilentlyWhenRememberMe(LoginHandlingResource resource) {
        return false; // as default
    }

    // -----------------------------------------------------
    //                                LoginSession SyncCheck
    //                                ----------------------
    protected boolean syncCheckLoginSessionIfNeeds(USER_BEAN userBean) {
        if (!(userBean instanceof SyncCheckable)) {
            return true; // means no check
        }
        final SyncCheckable checkable = (SyncCheckable) userBean;
        final OptionalThing<LocalDateTime> checkDt = checkable.getLastestSyncCheckTime(); // might be null
        final LocalDateTime currentDt = timeManager.currentDateTime();
        if (!needsLoginSessionSyncCheck(userBean, checkDt, currentDt)) {
            return true; // means no check
        }
        if (logger.isDebugEnabled()) {
            final ID userId = userBean.getUserId();
            final String checkDisp = checkDt.map(dt -> new HandyDate(dt).toDisp("yyyy/MM/dd HH:mm:ss")).orElse(null);
            logger.debug("...Sync-checking login session: userId={}, checkDate={}", userId, checkDisp);
        }
        checkable.manageLastestSyncCheckTime(currentDt); // update latest check date
        return findLoginSessionSyncCheckUser(userBean).map(loginUser -> {
            handleLoginSessionSyncCheckSuccess(userBean, loginUser);
            return true;
        }).orElseGet(() -> { // the user might be assigned here
            logger.debug("*The user already cannot login: {}", userBean);
            logout(); // remove user info from session
            return false; // means check NG
        });
    }

    protected boolean needsLoginSessionSyncCheck(USER_BEAN userBean, OptionalThing<LocalDateTime> checkDt, LocalDateTime currentDt) {
        return checkDt.map(dt -> {
            final int checkInterval = getLoginSessionSyncCheckInterval();
            return new HandyDate(dt).addSecond(checkInterval).isLessEqual(currentDt);
        }).orElse(true); // e.g. first time
    }

    protected int getLoginSessionSyncCheckInterval() {
        return 300; // as default (second)
    }

    protected OptionalEntity<USER_ENTITY> findLoginSessionSyncCheckUser(USER_BEAN userBean) {
        return findLoginUser(userBean.getUserId());
    }

    protected void handleLoginSessionSyncCheckSuccess(USER_BEAN userBean, USER_ENTITY loginUser) {
        // do nothing as default (you can add original process by override)
    }

    // -----------------------------------------------------
    //                                       Required Action
    //                                       ---------------
    @Override
    public boolean isLoginRequiredAction(LoginHandlingResource resource) {
        return !isImplicitEverybodyOpenAction(resource) && !isExplicitAllowAnyoneAccessAction(resource);
    }

    protected boolean isImplicitEverybodyOpenAction(LoginHandlingResource resource) {
        return isLoginActionOrRedirectLoginAction(resource);
    }

    protected boolean isExplicitAllowAnyoneAccessAction(LoginHandlingResource resource) {
        return hasAnnotation(resource.getActionClass(), resource.getExecuteMethod(), getAllowAnyoneAccessAnnotationType());
    }

    protected Class<? extends Annotation> getAllowAnyoneAccessAnnotationType() {
        return AllowAnyoneAccess.class;
    }

    // -----------------------------------------------------
    //                                      Session UserBean
    //                                      ----------------
    @Override
    public OptionalThing<USER_BEAN> getSavedUserBean() { // use covariant generic type
        final String key = getUserBeanKey();
        return OptionalThing.ofNullable(sessionManager.getAttribute(key, getUserBeanType()).orElse(null), () -> {
            throwLoginRequiredException("Not found the user in session by the key:" + key); // to login action
        });
    }

    /**
     * @deprecated use getSavedUserBean()
     */
    @Override
    public OptionalThing<USER_BEAN> getSessionUserBean() { // use covariant generic type
        return getSavedUserBean();
    }

    @Override
    public Class<?> getSaveKeyUserBeanType() {
        return getUserBeanType();
    }

    /**
     * Get the type of user bean basically for session key.
     * @return The type of user bean. (NotNull)
     */
    protected abstract Class<USER_BEAN> getUserBeanType();

    // -----------------------------------------------------
    //                                     Non LoginRequired
    //                                     -----------------
    /**
     * Check as the non login required action.
     * @param resource The resource of login handling to determine. (NotNull)
     * @throws LoginRequiredException When it fails to access the action for login authority.
     */
    protected void asNonLoginRequired(LoginHandlingResource resource) throws LoginRequiredException {
        if (!isSuppressRememberMeOfNonLoginRequired(resource)) { // option just in case
            logger.debug("...Checking login status for non login required");
            if (tryAlreadyLoginOrRememberMe(resource)) {
                checkPermission(resource); // throws if denied
                return; // Good
            }
        }
        if (isLoginRedirectBeanKeptAction(resource)) {
            // keep login-redirect path in session
            logger.debug("...Passing login check as login action (or redirect-kept action)");
        } else {
            clearLoginRedirectBean();
            logger.debug("...Passing login check as non login required");
        }
    }

    /**
     * Does it suppress remember-me when non login required?
     * @param resource The resource of login handling to determine. (NotNull)
     * @return The determination, true or false.
     */
    protected boolean isSuppressRememberMeOfNonLoginRequired(LoginHandlingResource resource) {
        return false; // as default
    }

    /**
     * Does the action keep login redirect bean in session?
     * @param resource The resource of login handling to determine. (NotNull)
     * @return The determination, true or false.
     */
    protected boolean isLoginRedirectBeanKeptAction(LoginHandlingResource resource) {
        return isLoginActionOrRedirectLoginAction(resource); // normally both are same action, you can change it.
    }

    // -----------------------------------------------------
    //                                            Permission
    //                                            ----------
    /**
     * Check the permission of the login user. (called in login status) <br>
     * If the request cannot access the action for permission denied, throw LoginRequiredException.
     * @param resource The resource of login handling to determine. (NotNull)
     * @throws LoginRequiredException When it fails to access the action for permission denied.
     */
    protected void checkPermission(LoginHandlingResource resource) throws LoginRequiredException {
        // no check as default, you can override
    }

    // -----------------------------------------------------
    //                                           Action Type
    //                                           -----------
    @Override
    public boolean isLoginAction(LoginHandlingResource resource) {
        return getLoginActionType().isAssignableFrom(resource.getActionClass());
    }

    /**
     * Get the type of (pure) login action (not related to login-redirect). <br>
     * Action type for login-redirect is defined at getRedirectLoginActionType().
     * @return The type of (pure) login action or dummy type when no login application. (NotNull)
     */
    protected abstract Class<?> getLoginActionType();

    /**
     * Is the action for login-redirect?
     * @param resource The resource of login handling to determine. (NotNull)
     * @return The determination, true or false.
     */
    protected boolean isRedirectLoginAction(LoginHandlingResource resource) {
        return getRedirectLoginActionType().isAssignableFrom(resource.getActionClass());
    }

    /**
     * Get the type of login action for login-redirect. <br>
     * It redirects to the action when login required.
     * @return The type of login action for login-redirect. (NotNull)
     */
    protected Class<?> getRedirectLoginActionType() {
        return getLoginActionType(); // same pure login type as default
    }

    protected boolean isLoginActionOrRedirectLoginAction(LoginHandlingResource resource) {
        return isLoginAction(resource) || isRedirectLoginAction(resource);
    }

    // ===================================================================================
    //                                                                      Login Redirect
    //                                                                      ==============
    @Override
    public void saveRequestedLoginRedirectInfo() {
        final String redirectPath = requestManager.getRequestPathAndQuery();
        final LoginRedirectBean redirectBean = createLoginRedirectBean(redirectPath);
        // not use instance type because it might be extended
        // (basically not use it when the object might be extended)
        sessionManager.setAttribute(generateLoginRedirectBeanKey(), redirectBean);
    }

    protected LoginRedirectBean createLoginRedirectBean(String redirectPath) {
        return new LoginRedirectBean(redirectPath);
    }

    @Override
    public HtmlResponse redirectToLoginAction() {
        final Class<?> redirectLoginActionType = getRedirectLoginActionType();
        final String actionUrl = actionPathResolver.toActionUrl(redirectLoginActionType);
        return HtmlResponse.fromRedirectPath(actionUrl);
    }

    @Override
    public OptionalThing<LoginRedirectBean> getLoginRedirectBean() {
        return sessionManager.getAttribute(generateLoginRedirectBeanKey(), LoginRedirectBean.class);
    }

    @Override
    public void clearLoginRedirectBean() {
        sessionManager.removeAttribute(generateLoginRedirectBeanKey());
    }

    protected String generateLoginRedirectBeanKey() {
        return getLoginRedirectBeanType().getName();
    }

    protected Class<? extends LoginRedirectBean> getLoginRedirectBeanType() {
        return LoginRedirectBean.class;
    }

    @Override
    public HtmlResponse switchToRequestedActionIfExists(HtmlResponse response) {
        return getLoginRedirectBean().filter(bean -> bean.hasRedirectPath()).map(bean -> {
            clearLoginRedirectBean();
            final String redirectPath = bean.getRedirectPath();
            logger.debug("...Switching redirection to requested {}", redirectPath);
            return HtmlResponse.fromRedirectPath(redirectPath);
        }).orElse(response);
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected boolean hasAnnotation(Class<?> targetClass, Method targetMethod, Class<? extends Annotation> annoType) {
        return hasAnnotationOnClass(targetClass, annoType) || hasAnnotationOnMethod(targetMethod, annoType);
    }

    protected boolean hasAnnotationOnClass(Class<?> targetClass, Class<? extends Annotation> annoType) {
        return targetClass.getAnnotation(annoType) != null;
    }

    protected boolean hasAnnotationOnMethod(Method targetMethod, Class<? extends Annotation> annoType) {
        return targetMethod.getAnnotation(annoType) != null;
    }

    // ===================================================================================
    //                                                                       Assert Helper
    //                                                                       =============
    protected void assertLoginAccountRequired(String account) {
        if (account == null || account.length() == 0) {
            String msg = "The argument 'account' should not be null for login.";
            throw new IllegalArgumentException(msg);
        }
    }

    protected void assertLoginPasswordRequired(String password) {
        if (password == null || password.length() == 0) {
            String msg = "The argument 'password' should not be null for login.";
            throw new IllegalArgumentException(msg);
        }
    }

    protected void assertGivenEntityRequired(USER_ENTITY givenEntity) {
        if (givenEntity == null) {
            String msg = "The argument 'givenEntity' should not be null.";
            throw new IllegalArgumentException(msg);
        }
    }

    protected void assertUserIdRequired(Object userId) {
        if (userId == null) {
            String msg = "The argument 'userId' should not be null.";
            throw new IllegalArgumentException(msg);
        }
    }

    protected void assertUserEntityRequired(USER_ENTITY userEntity) {
        if (userEntity == null) {
            String msg = "The argument 'userEntity' should not be null.";
            throw new IllegalArgumentException(msg);
        }
    }
}
