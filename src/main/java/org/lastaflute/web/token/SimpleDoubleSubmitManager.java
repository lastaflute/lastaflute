/*
 * Copyright 2015-2020 the original author or authors.
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
package org.lastaflute.web.token;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.direction.FwWebDirection;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.token.exception.DoubleSubmitMessageNotFoundException;
import org.lastaflute.web.token.exception.DoubleSubmitVerifyTokenBeforeValidationException;
import org.lastaflute.web.token.exception.DoubleSubmittedRequestException;
import org.lastaflute.web.util.LaActionExecuteUtil;
import org.lastaflute.web.util.LaActionRuntimeUtil;
import org.lastaflute.web.validation.ActionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author modified by jflute (originated in Struts)
 */
public class SimpleDoubleSubmitManager implements DoubleSubmitManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(SimpleDoubleSubmitManager.class);
    protected static final String ERRORS_APP_DOUBLE_SUBMIT_REQUEST = "errors.app.double.submit.request";
    protected static final Object DOUBLE_SUBMITTED_OBJ = new Object();
    protected static final Object SINGLE_SUBMITTED_OBJ = new Object();

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant director (AD) for framework. (NotNull: after initialization) */
    @Resource
    private FwAssistantDirector assistantDirector;

    @Resource
    private MessageManager messageManager;

    @Resource
    private RequestManager requestManager;

    /** Does it allow to call verifyToken() before validate()? */
    protected boolean allowsVerifyTokenBeforeValidation;

    /** The time of previous process to keep for unique token */
    protected long previousTimeMillis;

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
        final DoubleSubmitResourceProvider provider = direction.assistDoubleSubmitResourceProvider();
        allowsVerifyTokenBeforeValidation = provider != null && provider.allowsVerifyTokenBeforeValidation();
        showBootLogging();
    }

    protected FwWebDirection assistWebDirection() {
        return assistantDirector.assistWebDirection();
    }

    protected void showBootLogging() {
        if (logger.isInfoEnabled()) {
            logger.info("[DoubleSubmit Manager]");
            logger.info(" allowsVerifyTokenBeforeValidation: " + allowsVerifyTokenBeforeValidation);
        }
    }

    // #hope can use at JSON API (needs header handling)
    // ===================================================================================
    //                                                                  Token Manipulation
    //                                                                  ==================
    // -----------------------------------------------------
    //                                                 Save
    //                                                ------
    @Override
    public synchronized String saveToken(Class<?> groupType) {
        if (groupType == null) {
            throw new IllegalArgumentException("The argument 'groupType' should not be null.");
        }
        checkDoubleSubmitPreconditionExists(groupType);
        final DoubleSubmitTokenMap tokenMap = getSessionTokenMap().orElseGet(() -> {
            final DoubleSubmitTokenMap firstMap = new DoubleSubmitTokenMap();
            requestManager.getSessionManager().setAttribute(getTransactionTokenKey(), firstMap);
            return firstMap;
        });
        final String generated = generateToken(groupType);
        showSavingToken(groupType, generated);
        tokenMap.put(groupType, generated);
        return generated;
    }

    protected void checkDoubleSubmitPreconditionExists(Class<?> groupType) {
        final Locale userLocale = requestManager.getUserLocale();
        if (!messageManager.findMessage(userLocale, getDoubleSubmitMessageKey()).isPresent()) {
            throwDoubleSubmitMessageNotFoundException(groupType, userLocale);
        }
    }

    protected String throwDoubleSubmitMessageNotFoundException(Class<?> groupType, Locale userLocale) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the double submit message in message resource.");
        br.addItem("Advice");
        br.addElement("The message key should exist in your message resource,");
        br.addElement("when you control double submit by transaction token.");
        br.addElement("For example: (..._message.properties)");
        br.addElement("  " + getDoubleSubmitMessageKey() + " = double submit might be requested");
        br.addItem("Requested Action");
        br.addElement(LaActionRuntimeUtil.hasActionRuntime() ? LaActionRuntimeUtil.getActionRuntime() : null);
        br.addItem("Token Group");
        br.addElement(groupType.getName());
        br.addItem("User Locale");
        br.addElement(userLocale);
        br.addItem("NotFound MessageKey");
        br.addElement(getDoubleSubmitMessageKey());
        final String msg = br.buildExceptionMessage();
        throw new DoubleSubmitMessageNotFoundException(msg);
    }

    protected String getDoubleSubmitMessageKey() {
        return ERRORS_APP_DOUBLE_SUBMIT_REQUEST;
    }

    protected void showSavingToken(Class<?> groupType, String generated) {
        if (logger.isDebugEnabled()) {
            logger.debug("...Saving double-submit token: group={}, token={}", groupType.getSimpleName(), generated);
        }
    }

    // -----------------------------------------------------
    //                                              Generate
    //                                              --------
    @Override
    public synchronized String generateToken(Class<?> groupType) {
        assertArgumentNotNull("groupType", groupType);
        final byte[] sessionIdBytes = prepareSessionIdBytes();
        final byte[] currentBytes = prepareCurrentBytes();
        final byte[] groupTypeBytes = prepareGroupTypeBytes(groupType);
        return buildHex(sessionIdBytes, currentBytes, groupTypeBytes);
    }

    protected String buildHex(byte[] sessionIdBytes, byte[] currentBytes, byte[] groupTypeBytes) {
        final MessageDigest md = getMessageDigest();
        md.update(sessionIdBytes);
        md.update(currentBytes);
        md.update(groupTypeBytes);
        return toHex(md.digest());
    }

    protected MessageDigest getMessageDigest() {
        final String algorithm = "MD5"; // enough
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unknown algorithm: " + algorithm, e);
        }
    }

    protected String toHex(byte[] bt) {
        final StringBuilder sb = new StringBuilder(bt.length * 2);
        for (int i = 0; i < bt.length; i++) {
            sb.append(Character.forDigit((bt[i] & 0xf0) >> 4, 16));
            sb.append(Character.forDigit(bt[i] & 0x0f, 16));
        }
        return sb.toString();
    }

    protected byte[] prepareSessionIdBytes() {
        return requestManager.getSessionManager().getSessionId().getBytes();
    }

    protected byte[] prepareCurrentBytes() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis == previousTimeMillis) {
            currentTimeMillis++;
        }
        previousTimeMillis = currentTimeMillis;
        byte[] currentBytes = Long.valueOf(currentTimeMillis).toString().getBytes();
        return currentBytes;
    }

    protected byte[] prepareGroupTypeBytes(Class<?> groupType) {
        return groupType.getName().getBytes();
    }

    // ===================================================================================
    //                                                                 Token Determination
    //                                                                 ===================
    @Override
    public synchronized boolean determineToken(Class<?> groupType) {
        return doDetermineTokenValid(groupType, false);
    }

    @Override
    public synchronized boolean determineTokenWithReset(Class<?> groupType) {
        return doDetermineTokenValid(groupType, true);
    }

    protected boolean doDetermineTokenValid(Class<?> groupType, boolean reset) {
        return (boolean) getSessionTokenMap().map(tokenMap -> {
            return tokenMap.get(groupType).map(saved -> {
                if (reset) {
                    resetToken(groupType);
                }
                return getRequestedToken().map(token -> token.equals(saved)).orElse(false);
            }).orElse(false);
        }).orElse(false);
    }

    // ===================================================================================
    //                                                                  Token Verification
    //                                                                  ==================
    public void verifyToken(Class<?> groupType, TokenErrorHook errorHook) {
        doVerifyToken(groupType, errorHook, false);
    }

    public void verifyTokenKeep(Class<?> groupType, TokenErrorHook errorHook) {
        doVerifyToken(groupType, errorHook, true);
    }

    protected <MESSAGES extends UserMessages> void doVerifyToken(Class<?> groupType, TokenErrorHook errorHook, boolean keep) {
        assertArgumentNotNull("groupType", groupType);
        assertArgumentNotNull("errorHook", errorHook);
        checkVerifyTokenAfterValidatorCall(); // precisely unneeded if keep, but coherence
        final boolean matched;
        if (keep) { // no reset (intermediate request)
            matched = determineToken(groupType);
        } else { // mainly here (finish)
            matched = determineTokenWithReset(groupType);
        }
        if (!matched) {
            saveDoubleSubmittedMark();
            throwDoubleSubmittedRequestException(groupType, errorHook);
        } else {
            saveSingleSubmittedMark(); // means verified  for e.g. UnitTest
        }
    }

    protected void saveDoubleSubmittedMark() {
        requestManager.setAttribute(getDoubleSubmittedKey(), DOUBLE_SUBMITTED_OBJ);
    }

    protected <MESSAGES extends UserMessages> String throwDoubleSubmittedRequestException(Class<?> groupType, TokenErrorHook errorHook) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The request was born from double submit.");
        br.addItem("Advice");
        br.addElement("Double submit by user operation or no saved token.");
        br.addElement("Default scope of token is action type");
        br.addElement("so SAVE and VERIFY should be in same action.");
        br.addItem("Requested Action");
        br.addElement(LaActionRuntimeUtil.hasActionRuntime() ? LaActionRuntimeUtil.getActionRuntime() : null);
        br.addItem("Token Group");
        br.addElement(groupType.getName());
        br.addItem("Requested Token");
        br.addElement(getRequestedToken());
        br.addItem("Saved Token");
        br.addElement(getSessionTokenMap());
        final String msg = br.buildExceptionMessage();
        final String messageKey = getDoubleSubmitMessageKey();
        throw new DoubleSubmittedRequestException(msg, () -> errorHook.hook(), UserMessages.createAsOneGlobal(messageKey));
    }

    protected void saveSingleSubmittedMark() {
        requestManager.setAttribute(getFirstSubmittedKey(), SINGLE_SUBMITTED_OBJ);
    }

    // -----------------------------------------------------
    //                                       Validation Call
    //                                       ---------------
    protected void checkVerifyTokenAfterValidatorCall() {
        if (allowsVerifyTokenBeforeValidation) {
            return;
        }
        if (LaActionExecuteUtil.hasActionExecute()) { // just in case
            final ActionExecute execute = LaActionExecuteUtil.getActionExecute();
            if (certainlyCanBeValidated(execute) && certainlyValidatorNotCalled()) {
                throwDoubleSubmitVerifyTokenBeforeValidationException(execute);
            }
        }
    }

    protected boolean certainlyCanBeValidated(ActionExecute execute) {
        // if annotations exist, validator is supposed to be called (checked in framework)
        // but if validation without annotation, returns false so not exactly
        return execute.getFormMeta().filter(meta -> meta.isValidatorAnnotated()).isPresent();
    }

    protected boolean certainlyValidatorNotCalled() {
        return ActionValidator.certainlyValidatorNotCalled();
    }

    protected void throwDoubleSubmitVerifyTokenBeforeValidationException(ActionExecute execute) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The verifyToken() was called before validate() in action.");
        br.addItem("Advice");
        br.addElement("The verifyToken() should be after validate().");
        br.addElement("The verifyToken() deletes session token if success,");
        br.addElement("so it may be token-not-found exception if validation error.");
        br.addElement("(validation error's response may need session token)");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse update(Integer memberId) {");
        br.addElement("        verifyToken(...); // *Bad: session token is deleted here");
        br.addElement("        validate(form, messages -> {}, () -> { // may be this exception if validation error");
        br.addElement("            return asHtml(path_...); // the html may need token...");
        br.addElement("        });");
        br.addElement("        ...");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public HtmlResponse update(Integer memberId) {");
        br.addElement("        validate(form, messages -> {}, () -> {");
        br.addElement("            return asHtml(path_...); // session token remains");
        br.addElement("        });");
        br.addElement("        verifyToken(...); // Good");
        br.addElement("        ...");
        br.addElement("    }");
        br.addItem("Execute Method");
        br.addElement(execute.toSimpleMethodExp());
        br.addItem("Requested Token");
        br.addElement(getRequestedToken());
        br.addItem("Saved Token");
        br.addElement(getSessionTokenMap());
        final String msg = br.buildExceptionMessage();
        throw new DoubleSubmitVerifyTokenBeforeValidationException(msg);
    }

    // ===================================================================================
    //                                                                       Token Closing
    //                                                                       =============
    @Override
    public synchronized void resetToken(Class<?> groupType) {
        getSessionTokenMap().ifPresent(tokenMap -> {
            showRemovingToken(groupType, tokenMap);
            tokenMap.remove(groupType);
            if (tokenMap.isEmpty()) {
                removeTokenFromSession();
            }
        }).orElse(() -> {
            removeTokenFromSession();
        });
    }

    protected void showRemovingToken(Class<?> groupType, DoubleSubmitTokenMap tokenMap) {
        if (logger.isDebugEnabled()) {
            final String token = tokenMap.get(groupType).orElse(null); // just in case
            logger.debug("...Removing double-submit token: group={}, token={}", groupType.getSimpleName(), token);
        }
    }

    protected void removeTokenFromSession() {
        requestManager.getSessionManager().removeAttribute(getTransactionTokenKey());
    }

    // ===================================================================================
    //                                                                        Token Access
    //                                                                        ============
    @Override
    public OptionalThing<String> getRequestedToken() {
        return requestManager.getParameter(getTransactionTokenKey());
    }

    @Override
    public OptionalThing<DoubleSubmitTokenMap> getSessionTokenMap() {
        return requestManager.getSessionManager().getAttribute(getTransactionTokenKey(), DoubleSubmitTokenMap.class);
    }

    @Override
    public boolean isDoubleSubmittedRequest() {
        return requestManager.getAttribute(getDoubleSubmittedKey(), Object.class).isPresent();
    }

    @Override
    public boolean isFirstSubmittedRequest() {
        return requestManager.getAttribute(getFirstSubmittedKey(), Object.class).isPresent();
    }

    // ===================================================================================
    //                                                                        Key Provider
    //                                                                        ============
    // but cannot change it because thymeleaf or taglib sees LastaWebKey directly
    protected String getTransactionTokenKey() {
        return LastaWebKey.TRANSACTION_TOKEN_KEY;
    }

    protected String getDoubleSubmittedKey() {
        return LastaWebKey.DOUBLE_SUBMITTED_KEY;
    }

    protected String getFirstSubmittedKey() {
        return LastaWebKey.FIRST_SUBMITTED_KEY;
    }

    // ===================================================================================
    //                                                                       Assert Helper
    //                                                                       =============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            String msg = "The value should not be null: variableName=null value=" + value;
            throw new IllegalArgumentException(msg);
        }
        if (value == null) {
            String msg = "The value should not be null: variableName=" + variableName;
            throw new IllegalArgumentException(msg);
        }
    }
}
