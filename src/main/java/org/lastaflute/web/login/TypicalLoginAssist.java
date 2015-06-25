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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;

import javax.annotation.Resource;

import org.dbflute.Entity;
import org.dbflute.helper.HandyDate;
import org.dbflute.optional.OptionalEntity;
import org.dbflute.optional.OptionalObject;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.lastaflute.core.security.PrimaryCipher;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.db.jta.stage.TransactionStage;
import org.lastaflute.web.api.ApiAction;
import org.lastaflute.web.login.exception.LoginFailureException;
import org.lastaflute.web.login.exception.LoginTimeoutException;
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
 * @param <USER_BEAN> The type of user bean.
 * @param <USER_ENTITY> The type of user entity or model.
 * @author jflute
 */
public abstract class TypicalLoginAssist<USER_BEAN extends UserBean, USER_ENTITY> implements LoginManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(TypicalLoginAssist.class);

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

    @Resource
    private TransactionStage transactionStage;;

    // ===================================================================================
    //                                                                           Find User
    //                                                                           =========
    /**
     * Check the user is login-able. (basically for validation)
     * @param email The email address for the login user. (NotNull)
     * @param password The plain password for the login user, which is encrypted in this method. (NotNull)
     * @return true if the user is login-able.
     */
    public boolean checkUserLoginable(String email, String password) {
        return doCheckUserLoginable(email, encryptPassword(password));
    }

    /**
     * Check the user is login-able. (basically for validation)
     * @param email The email address for the login user. (NotNull)
     * @param cipheredPassword The ciphered password for the login user. (NotNull)
     * @return true if the user is login-able.
     */
    protected abstract boolean doCheckUserLoginable(String email, String cipheredPassword);

    /**
     * Find the login user in the database.
     * @param email The email address for the login user. (NotNull)
     * @param password The plain password for the login user, which is encrypted in this method. (NotNull)
     * @return The optional entity of the found user. (NotNull, EmptyAllowed: when the login user is not found)
     */
    public OptionalEntity<USER_ENTITY> findLoginUser(String email, String password) {
        return doFindLoginUser(email, encryptPassword(password));
    }

    /**
     * Finding the login user in the database.
     * @param email The email address for the login user. (NotNull)
     * @param cipheredPassword The ciphered password for the login user. (NotNull)
     * @return The optional entity of the found user. (NotNull, EmptyAllowed: when the login user is not found)
     */
    protected abstract OptionalEntity<USER_ENTITY> doFindLoginUser(String email, String cipheredPassword);

    /**
     * Encrypt the password of the login user.
     * @param plainPassword The plain password for the login user, which is encrypted in this method. (NotNull)
     * @return The encrypted string of the password. (NotNull)
     */
    protected String encryptPassword(String plainPassword) {
        return primaryCipher.oneway(plainPassword);
    }

    /**
     * Find the login user in the database.
     * @param userId for the login user. (NotNull)
     * @return The optional entity of the found user. (NotNull, EmptyAllowed: when the login user is not found)
     */
    public OptionalEntity<USER_ENTITY> findLoginUser(Long userId) {
        return doFindLoginUser(userId);
    }

    /**
     * Finding the login user in the database.
     * @param userId for the login user. (NotNull)
     * @return The optional entity of the found user. (NotNull, EmptyAllowed: when the login user is not found)
     */
    protected abstract OptionalEntity<USER_ENTITY> doFindLoginUser(Long userId);

    // ===================================================================================
    //                                                                         Login Logic
    //                                                                         ===========
    // -----------------------------------------------------
    //                                       Login Interface
    //                                       ---------------
    @Override
    public void login(String account, String password, LoginOpCall opLambda) throws LoginFailureException {
        final LoginOption option = createLoginOption(opLambda);
        doLogin(account, password, option);
    }

    @Override
    public HtmlResponse loginRedirect(String account, String password, LoginOpCall opLambda, LoginRedirectSuccessCall oneArgLambda)
            throws LoginFailureException {
        final LoginOption option = createLoginOption(opLambda);
        doLogin(account, password, option); // exception if login failure
        return switchToRequestedActionIfExists(oneArgLambda.success()); // so success only here
    }

    @Override
    @SuppressWarnings("unchecked")
    public void givenLogin(Entity givenEntity, LoginOpCall opLambda) throws LoginFailureException {
        final LoginOption option = createLoginOption(opLambda);
        doLoginByGivenEntity((USER_ENTITY) givenEntity, option);
    }

    @Override
    public void identityLogin(Long userId, LoginOpCall opLambda) throws LoginFailureException {
        final LoginOption op = createLoginOption(opLambda);
        doLoginByIdentity(userId, op);
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
     * Do actually login for the user by email and password.
     * @param account The account for the login user. (NotNull)
     * @param password The plain password for the login user, which is encrypted in this method. (NullAllowed: only when given)
     * @param option The option of login specified by caller. (NotNull)
     * @throws LoginFailureException When it fails to do login by the user info.
     */
    protected void doLogin(String account, String password, LoginSpecifiedOption option) throws LoginFailureException {
        assertLoginAccountRequired(account);
        assertLoginPasswordRequired(password);
        handleLoginSuccess(findLoginUser(account, password).orElseThrow(() -> {
            final String msg = "Not found the user by the account and password: " + account + ", " + option;
            final Map<String, String> authMap = DfCollectionUtil.newHashMap("account", account, "password", password);
            return handleLoginFailure(msg, authMap, OptionalThing.of(option));
        }), option);
    }

    /**
     * Do actually login for the user by given entity. (no silent)
     * @param givenEntity The given entity for user. (NotNull)
     * @param option The option of login specified by caller. (NotNull)
     * @throws LoginFailureException When it fails to do login by the user info.
     */
    protected void doLoginByGivenEntity(USER_ENTITY givenEntity, LoginSpecifiedOption option) throws LoginFailureException {
        assertGivenEntityRequired(givenEntity);
        handleLoginSuccess(givenEntity, option);
    }

    /**
     * Do actually login for the user by identity (user ID). (no silent)
     * @param userId for the login user. (NotNull)
     * @param option The option of login specified by caller. (NotNull)
     * @throws LoginFailureException When it fails to do login by the user info.
     */
    protected void doLoginByIdentity(Long userId, LoginSpecifiedOption option) throws LoginFailureException {
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
     * @param silently Is the login executed silently? (no saving history)
     */
    protected void handleLoginSuccess(USER_ENTITY userEntity, LoginSpecifiedOption option) {
        assertUserEntityRequired(userEntity);
        final USER_BEAN userBean = saveLoginInfoToSession(userEntity);
        if (userBean instanceof SyncCheckable) {
            ((SyncCheckable) userBean).setLastestSyncCheckDateTime(timeManager.currentDateTime());
        }
        if (option.isRememberMe()) {
            saveAutoLoginKeyToCookie(userEntity, userBean);
        }
        if (!option.isSilentLogin()) { // mainly here
            transactionCallSaveLoginHistory(userEntity, userBean, option);
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
        sessionManager.setAttribute(userBean);
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

    @Override
    public void reselectSessionUserBeanIfExists() throws LoginFailureException {
        getSessionUserBean().ifPresent(oldBean -> {
            inheritUserBeanAdditionalInfo(oldBean);
            final Long userId = oldBean.getUserId();
            logger.debug("...Re-selecting user bean in session: userId={}", userId);
            sessionManager.setAttribute(createUserBean(findLoginUser(userId).orElseThrow(() -> { /* might be already left */
                logout(); /* to clear old user info in session */
                String msg = "Not found the user by the user ID: " + userId;
                return handleLoginFailure(msg, userId, OptionalThing.ofNullable(null, () -> {
                    throw new IllegalStateException("Not found the login option when reselect: userId=" + userId);
                }));
            })));
        });
    }

    protected void inheritUserBeanAdditionalInfo(USER_BEAN oldBean) {
        // do nothing as default
    }

    // -----------------------------------------------------
    //                                    AutoLogin Handling
    //                                    ------------------
    /**
     * Save remember-me key to cookie.
     * @param userEntity The selected entity of login user. (NotNull)
     * @param userBean The user bean saved in session. (NotNull)
     */
    protected void saveAutoLoginKeyToCookie(USER_ENTITY userEntity, USER_BEAN userBean) {
        final int expireDays = getAutoLoginAccessTokenExpireDays();
        final String cookieKey = getCookieAutoLoginKey();
        doSaveAutoLoginCookie(userEntity, userBean, expireDays, cookieKey);
    }

    /**
     * Get the expire days of both access token and cookie value. <br>
     * You can change it by override.
     * @return The count of expire days. (NotMinus, NotZero)
     */
    protected int getAutoLoginAccessTokenExpireDays() {
        return REMEMBER_ME_ACCESS_TOKEN_DEFAULT_EXPIRE_DAYS; // as default for compatibility
    }

    /**
     * Get the key of auto login saved in cookie.
     * @return The string key for cookie. (NotNull)
     */
    protected abstract String getCookieAutoLoginKey();

    /**
     * Do save remember-me key to cookie.
     * @param userEntity The selected entity of login user. (NotNull)
     * @param userBean The user bean saved in session. (NotNull)
     * @param expireDays The expire days of both access token and cookie value.
     * @param cookieKey The key of the cookie. (NotNull)
     */
    protected void doSaveAutoLoginCookie(USER_ENTITY userEntity, USER_BEAN userBean, int expireDays, String cookieKey) {
        logger.debug("...Saving remember-me key to cookie: key={}", cookieKey);
        final String value = buildAutoLoginCookieValue(userEntity, userBean, expireDays);
        final int expireSeconds = expireDays * 60 * 60 * 24; // cookie's expire, same as access token
        cookieManager.setCookieCiphered(cookieKey, value, expireSeconds);
    }

    /**
     * Build the value for auto login saved in cookie. <br>
     * You can change access token's structure by override. #change_access_token
     * @param userEntity The selected entity of login user. (NotNull)
     * @param userBean The user bean saved in session. (NotNull)
     * @param expireDays The count of expired days from current times. (NotNull)
     * @return The string value for auto login. (NotNull)
     */
    protected String buildAutoLoginCookieValue(USER_ENTITY userEntity, USER_BEAN userBean, int expireDays) {
        final String autoLoginKey = createAutoLoginKey(userEntity, userBean);
        final String delimiter = getAutoLoginDelimiter();
        final HandyDate currentHandyDate = timeManager.currentHandyDate();
        final HandyDate expireDate = currentHandyDate.addDay(expireDays); // access token's expire
        return autoLoginKey + delimiter + formatForAutoLoginExpireDate(expireDate);
    }

    /**
     * Create remember-me key for the user. <br>
     * You can change user key's structure by override. #change_user_key
     * @param userEntity The selected entity of login user. (NotNull)
     * @param userBean The user bean saved in session. (NotNull)
     * @return The string expression for remember-me key. (NotNull)
     */
    protected String createAutoLoginKey(USER_ENTITY userEntity, USER_BEAN userBean) {
        return String.valueOf(userBean.getUserId()); // as default (override if it needs)
    }

    // -----------------------------------------------------
    //                                      History Handling
    //                                      ----------------
    /**
     * Call the process, saving login history, in new transaction for e.g. remember-me in callback. <br>
     * Update statement needs transaction (access-context) so needed. <br>
     * Meanwhile, the transaction inherits already-begun transaction for e.g. normal login process.
     * @param userEntity The entity of the found login user. (NotNull)
     * @param userBean The bean of the user saved in session. (NotNull)
     * @param option The option of login specified by caller. (NotNull)
     */
    protected void transactionCallSaveLoginHistory(USER_ENTITY userEntity, USER_BEAN userBean, LoginSpecifiedOption option) {
        try {
            // inherit when e.g. called by action, begin new when e.g. remember-me
            transactionStage.requiresNew(tx -> {
                saveLoginHistory(userEntity, userBean, option);
            });
        } catch (Throwable e) {
            handleSavingLoginHistoryTransactionFailure(userBean, e);
        }
    }

    /**
     * Handle the exception of transaction failure for saving login history.
     * @param userBean The bean of the user saved in session. (NotNull)
     * @param cause The cause exception of transaction failure. (NotNull)
     */
    protected void handleSavingLoginHistoryTransactionFailure(USER_BEAN userBean, Throwable cause) {
        // continue the request because of history, latter process throws the exception if fatal error
        logger.warn("Failed to save login history: {}", userBean.getUserId(), cause);
    }

    /**
     * Save the history of the success login. (already saved in session at this point) <br>
     * For example, you can save the login user's info to database. <br>
     * This is NOT called when silent login.
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

    // ===================================================================================
    //                                                                    RememberMe Login
    //                                                                    ================
    @Override
    public boolean rememberMe(RememberMeLoginOpCall opLambda) {
        return delegateAutoLogin(createRememberMeLoginOption(opLambda));
    }

    protected RememberMeLoginOption createRememberMeLoginOption(RememberMeLoginOpCall opLambda) {
        final RememberMeLoginOption option = new RememberMeLoginOption();
        opLambda.callback(option);
        return option;
    }

    protected boolean delegateAutoLogin(RememberMeLoginSpecifiedOption option) {
        return cookieManager.getCookieCiphered(getCookieAutoLoginKey()).map(cookie -> {
            final String cookieValue = cookie.getValue();
            if (cookieValue != null && cookieValue.trim().length() > 0) {
                final String[] valueAry = cookieValue.split(getAutoLoginDelimiter());
                final Boolean handled = handleAutoLoginCookie(valueAry, option);
                if (handled != null) {
                    return handled;
                }
                if (handleAutoLoginInvalidCookie(cookieValue, valueAry)) { // you can also retry
                    return true; // success by the handling
                }
            }
            return false;
        }).orElse(false);
    }

    protected String getAutoLoginDelimiter() {
        return REMEMBER_ME_COOKIE_DELIMITER;
    }

    protected boolean handleAutoLoginInvalidCookie(String cookieValue, String[] valueAry) {
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
    protected Boolean handleAutoLoginCookie(String[] valueAry, RememberMeLoginSpecifiedOption option) {
        if (valueAry.length != 2) { // invalid cookie
            return null;
        }
        final String userKey = valueAry[0]; // resolved by identity login
        final String expireDate = valueAry[1]; // AccessToken's expire
        if (isValidAutoLoginCookie(userKey, expireDate)) {
            final Long userId = convertCookieUserKeyToUserId(userKey);
            return doAutoLogin(userId, expireDate, option);
        }
        return null;
    }

    /**
     * Are the user ID and expire date extracted from cookie valid?
     * @param userKey The key of the login user. (NotNull)
     * @param expireDate The string expression for expire date of remember-me access token. (NotNull)
     * @return Is a validation for auto login OK?
     */
    protected boolean isValidAutoLoginCookie(String userKey, String expireDate) {
        final String currentDate = formatForAutoLoginExpireDate(timeManager.currentHandyDate());
        if (currentDate.compareTo(expireDate) < 0) { // String v.s. String
            return true; // valid access token within time limit
        }
        // expired here
        logger.debug("The access token for remember-me expired: userKey={} expireDate={}", userKey, expireDate);
        return false;
    }

    protected String formatForAutoLoginExpireDate(HandyDate expireDate) {
        return expireDate.toDisp(REMEMBER_ME_ACCESS_TOKEN_EXPIRE_DATE_PATTERN);
    }

    /**
     * Convert user key to user ID by your rule. #change_user_key <br>
     * The default rule is simple conversion to number, you can change it by overriding.
     * @param userKey The string key for the login user, same as remember-me key. (NotNull)
     * @return The ID for the login user. (NotNull)
     */
    protected Long convertCookieUserKeyToUserId(String userKey) {
        final Long userId;
        try {
            userId = Long.valueOf(userKey); // as default (override if it needs)
        } catch (NumberFormatException e) {
            throw new LoginFailureException("Invalid user key (not ID): " + userKey, e);
        }
        return userId;
    }

    // -----------------------------------------------------
    //                                    Actually AutoLogin
    //                                    ------------------
    /**
     * Do actually remember-me for the user.
     * @param userId The ID of the login user, used by identity login. (NotNull)
     * @param expireDate The string expression for expire date of remember-me access token. (NotNull)
     * @param option The option of remember-me login specified by caller. (NotNull)
     * @return Is the remember-me success?
     */
    protected boolean doAutoLogin(Long userId, String expireDate, RememberMeLoginSpecifiedOption option) {
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
        sessionManager.removeAttribute(getUserBeanType());
        cookieManager.removeCookie(getCookieAutoLoginKey());
    }

    /**
     * Get the type of user bean basically for session key.
     * @return The type of user bean. (NotNull)
     */
    protected abstract Class<USER_BEAN> getUserBeanType();

    // ===================================================================================
    //                                                                         Login Check
    //                                                                         ===========
    // -----------------------------------------------------
    //                                         LoginRequired
    //                                         -------------
    @Override
    public OptionalThing<String> checkLoginRequired(LoginHandlingResource resource) {
        final OptionalThing<String> redirectTo;
        if (isLoginRequiredAction(resource)) {
            redirectTo = processLoginRequired(resource);
        } else {
            redirectTo = processNotLoginRequired(resource);
        }
        return redirectTo;
    }

    /**
     * Process for the login-required action.
     * @param resource The resource of login handling to determine. (NotNull)
     * @return The optional forward path, basically for login redirect. (NotNull, EmptyAllowed: then login check passed)
     */
    protected OptionalThing<String> processLoginRequired(LoginHandlingResource resource) {
        logger.debug("...Checking login status for login required");
        if (processAlreadyLogin(resource) || processAutoLogin(resource)) {
            return processAuthority(resource);
        }
        saveRequestedLoginRedirectInfo();
        final OptionalThing<String> loginAction = redirectToRequiredCheckedLoginAction();
        loginAction.ifPresent(action -> logger.debug("...Redirecting to login action: {}", action));
        return loginAction;
    }

    /**
     * Redirect to action when required checked (basically login action). <br>
     * You can customize the redirection when not login but login required.
     * @return The optional forward path, basically for login redirect. (NotNull, EmptyAllowed: then login check passed)
     */
    protected OptionalThing<String> redirectToRequiredCheckedLoginAction() {
        return OptionalThing.of(redirectToLoginAction());
    }

    // -----------------------------------------------------
    //                                         Already Login
    //                                         -------------
    protected boolean processAlreadyLogin(LoginHandlingResource resource) {
        return getSessionUserBean().map(userBean -> {
            if (!syncCheckLoginSessionIfNeeds(userBean)) {
                return false;
            }
            clearLoginRedirectBean();
            logger.debug("...Passing login check as already-login");
            return true;
        }).orElse(false);
    }

    // -----------------------------------------------------
    //                                LoginSession SyncCheck
    //                                ----------------------
    protected boolean syncCheckLoginSessionIfNeeds(USER_BEAN userBean) {
        if (!(userBean instanceof SyncCheckable)) {
            return true; // means no check
        }
        final SyncCheckable checkable = (SyncCheckable) userBean;
        final OptionalThing<LocalDateTime> checkDt = checkable.getLastestSyncCheckDateTime(); // might be null
        final LocalDateTime currentDt = timeManager.currentDateTime();
        if (!needsLoginSessionSyncCheck(userBean, checkDt, currentDt)) {
            return true; // means no check
        }
        if (logger.isDebugEnabled()) {
            final Long userId = userBean.getUserId();
            final String checkDisp = checkDt.map(dt -> new HandyDate(dt).toDisp("yyyy/MM/dd HH:mm:ss")).orElse(null);
            logger.debug("...Sync-checking login session: userId={}, checkDate={}", userId, checkDisp);
        }
        checkable.setLastestSyncCheckDateTime(currentDt); // update latest check date
        return findLoginSessionSyncCheckUser(userBean).map(loginUser -> {
            handleLoginSessionSyncCheckSuccess(userBean, loginUser);
            return true;
        }).orElseGet(() -> { /* the user might be assigned here */
            logger.debug("*The user already cannot login: {}", userBean);
            logout(); /* remove user info from session */
            return false; /* means check NG */
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
    //                                            Auto Login
    //                                            ----------
    protected boolean processAutoLogin(LoginHandlingResource resource) {
        final boolean updateToken = isUpdateTokenWhenAutoLogin(resource);
        final boolean silently = isSilentlyWhenAutoLogin(resource);
        final boolean success = rememberMe(op -> op.updateToken(updateToken).silentLogin(silently));
        return success && getSessionUserBean().map(userBean -> {
            clearLoginRedirectBean();
            logger.debug("...Passing login check as remember-me");
            return true;
        }).orElse(false);
    }

    protected boolean isUpdateTokenWhenAutoLogin(LoginHandlingResource resource) {
        return false; // as default
    }

    protected boolean isSilentlyWhenAutoLogin(LoginHandlingResource resource) {
        return false; // as default
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
    public OptionalThing<USER_BEAN> getSessionUserBean() { // quit covariant return type for optional
        final Class<USER_BEAN> beanType = getUserBeanType();
        return OptionalThing.ofNullable(sessionManager.getAttribute(beanType).orElse(null), () -> {
            String msg = "Not found the user in session by the type:" + beanType;
            throw new LoginTimeoutException(msg); /* to login action */
        });
    }

    // -----------------------------------------------------
    //                                             Authority
    //                                             ---------
    /**
     * Process for the authority of the login user. (called in login status)
     * @param resource The resource of login handling to determine. (NotNull)
     * @return The forward path, basically for authority redirect. (NotNull, EmptyAllowed: then authority check passed)
     */
    protected OptionalThing<String> processAuthority(LoginHandlingResource resource) {
        return OptionalObject.empty(); // no check as default, you can override
    }

    // -----------------------------------------------------
    //                                     Not LoginRequired
    //                                     -----------------
    /**
     * Process for the NOT login-required action.
     * @param resource The resource of login handling to determine. (NotNull)
     * @return The forward path, basically for login redirect. (NullAllowed)
     */
    protected OptionalThing<String> processNotLoginRequired(LoginHandlingResource resource) {
        if (isAutoLoginWhenNotLoginRequired(resource)) {
            logger.debug("...Checking login status for not-login required");
            if (processAlreadyLogin(resource) || processAutoLogin(resource)) {
                return processAuthority(resource);
            }
        }
        if (isLoginRedirectBeanKeptAction(resource)) {
            // keep login-redirect path in session
            logger.debug("...Passing login check as login action (or redirect-kept action)");
        } else {
            clearLoginRedirectBean();
            logger.debug("...Passing login check as not required");
        }
        return OptionalObject.empty(); // no redirect
    }

    /**
     * Does the action keep login redirect bean in session?
     * @param resource The resource of login handling to determine. (NotNull)
     * @return The determination, true or false.
     */
    protected boolean isLoginRedirectBeanKeptAction(LoginHandlingResource resource) {
        // normally both are same action, but redirect action might be changed
        return isLoginActionOrRedirectLoginAction(resource);
    }

    /**
     * Does it remember-me when not-login required? <br>
     * If not-login-required action also should accept remember-me, <br>
     * e.g. switch display by login or not, override this and return true.
     * @param resource The resource of login handling to determine. (NotNull)
     * @return The determination, true or false.
     */
    protected boolean isAutoLoginWhenNotLoginRequired(LoginHandlingResource resource) {
        return false; // as default
    }

    // -----------------------------------------------------
    //                                           Action Type
    //                                           -----------
    @Override
    public boolean isLoginAction(LoginHandlingResource resource) {
        final Class<?> actionClass = resource.getActionClass();
        final Class<?> loginActionType = getLoginActionType();
        return loginActionType.isAssignableFrom(actionClass);
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
        final Class<?> actionClass = resource.getActionClass();
        final Class<?> loginActionType = getRedirectLoginActionType();
        return loginActionType.isAssignableFrom(actionClass);
    }

    /**
     * Get the type of login action for login-redirect. <br>
     * It redirects to the action when login required.
     * @return The type of login action for login-redirect. (NotNull)
     */
    protected Class<?> getRedirectLoginActionType() {
        return getLoginActionType(); // same pure login type as default
    }

    @Override
    public boolean isApiAction(LoginHandlingResource resource) {
        final Class<?> actionClass = resource.getActionClass();
        return ApiAction.class.isAssignableFrom(actionClass);
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
    public String redirectToLoginAction() {
        final Class<?> redirectLoginActionType = getRedirectLoginActionType();
        return actionPathResolver.toActionUrl(redirectLoginActionType, true, null);
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
        final OptionalThing<LoginRedirectBean> opt = getLoginRedirectBean();
        if (opt.isPresent() && opt.get().hasRedirectPath()) {
            clearLoginRedirectBean();
            final String redirectPath = opt.get().getRedirectPath();
            logger.debug("...Switching redirection to requested {}", redirectPath);
            return HtmlResponse.fromRedirectPath(redirectPath);
        } else {
            return response;
        }
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

    protected void assertUserIdRequired(Long userId) {
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
