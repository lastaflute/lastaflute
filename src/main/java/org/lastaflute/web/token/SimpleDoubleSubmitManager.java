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
package org.lastaflute.web.token;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import javax.annotation.Resource;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.web.ruts.message.ActionMessages;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.token.exception.DoubleSubmitMessageNotFoundException;
import org.lastaflute.web.token.exception.DoubleSubmitRequestException;
import org.lastaflute.web.util.LaActionRuntimeUtil;

/**
 * @author modified by jflute (originated in Struts)
 */
public class SimpleDoubleSubmitManager implements DoubleSubmitManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final String ERRORS_APP_DOUBLE_SUBMIT_REQUEST = "errors.app.double.submit.request";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    @Resource
    protected MessageManager messageManager;
    @Resource
    protected RequestManager requestManager;

    protected long previous; // keep for unique token

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
            requestManager.getSessionManager().setAttribute(getTokenKey(), firstMap);
            return firstMap;
        });
        final String generated = generateToken(groupType);
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

    // -----------------------------------------------------
    //                                              Generate
    //                                              --------
    @Override
    public synchronized String generateToken(Class<?> groupType) {
        if (groupType == null) {
            throw new IllegalArgumentException("The argument 'groupType' should not be null.");
        }
        final byte[] idBytes = requestManager.getSessionManager().getSessionId().getBytes();
        long current = System.currentTimeMillis();
        if (current == previous) {
            current++;
        }
        previous = current;
        final byte[] now = Long.valueOf(current).toString().getBytes();
        final MessageDigest md = getMessageDigest();
        md.update(idBytes);
        md.update(now);
        md.update(groupType.getName().getBytes());
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

    protected <MESSAGES extends ActionMessages> void doVerifyToken(Class<?> groupType, TokenErrorHook errorHook, boolean keep) {
        final boolean matched;
        if (keep) { // no reset (intermediate request)
            matched = determineToken(groupType);
        } else { // mainly here (finish)
            matched = determineTokenWithReset(groupType);
        }
        if (!matched) {
            throwDoubleSubmitRequestException(groupType, errorHook);
        }
    }

    protected <MESSAGES extends ActionMessages> String throwDoubleSubmitRequestException(Class<?> groupType, TokenErrorHook errorHook) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The request was born from double submit.");
        br.addItem("Advice");
        br.addElement("Double submit by user operation");
        br.addElement("or not saved token but validate it.");
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
        throw new DoubleSubmitRequestException(msg, messageKey).response(() -> errorHook.hook());
    }

    // ===================================================================================
    //                                                                       Token Closing
    //                                                                       =============
    @Override
    public synchronized void resetToken(Class<?> groupType) {
        getSessionTokenMap().ifPresent(tokenMap -> {
            tokenMap.remove(groupType);
            if (tokenMap.isEmpty()) {
                removeTokenFromSession();
            }
        }).orElse(() -> {
            removeTokenFromSession();
        });
    }

    protected void removeTokenFromSession() {
        requestManager.getSessionManager().removeAttribute(getTokenKey());
    }

    // ===================================================================================
    //                                                                        Token Access
    //                                                                        ============
    @Override
    public OptionalThing<String> getRequestedToken() {
        return requestManager.getParameter(getTokenKey());
    }

    @Override
    public OptionalThing<DoubleSubmitTokenMap> getSessionTokenMap() {
        return requestManager.getSessionManager().getAttribute(getTokenKey(), DoubleSubmitTokenMap.class);
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected String getTokenKey() {
        return TOKEN_KEY;
    }
}
