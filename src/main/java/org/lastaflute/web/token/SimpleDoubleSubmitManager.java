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
package org.lastaflute.web.token;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Resource;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author modified by jflute (originated in Struts)
 */
public class SimpleDoubleSubmitManager implements DoubleSubmitManager {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    @Resource
    protected RequestManager requestManager;

    protected long previous; // keep for unique token

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
        final String tokenKey = getTokenKey();
        return (boolean) getSessionTokenMap(tokenKey).map(tokenMap -> {
            return tokenMap.get(groupType).map(saved -> {
                if (reset) {
                    resetToken(groupType);
                }
                return getRequestedToken(tokenKey).map(token -> token.equals(saved)).orElse(false);
            }).orElse(false);
        }).orElse(false);
    }

    // ===================================================================================
    //                                                                  Token Manipulation
    //                                                                  ==================
    @Override
    public synchronized String saveToken(Class<?> groupType) {
        final String tokenKey = getTokenKey();
        final DoubleSubmitTokenMap tokenMap = getSessionTokenMap(tokenKey).orElseGet(() -> {
            final DoubleSubmitTokenMap firstMap = new DoubleSubmitTokenMap();
            requestManager.getSessionManager().setAttribute(tokenKey, firstMap);
            return firstMap;
        });
        final String generated = generateToken(groupType);
        tokenMap.put(groupType, generated);
        return generated;
    }

    @Override
    public synchronized String generateToken(Class<?> groupType) {
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

    @Override
    public synchronized void resetToken(Class<?> groupType) {
        final String tokenKey = getTokenKey();
        getSessionTokenMap(tokenKey).ifPresent(tokenMap -> {
            tokenMap.remove(groupType);
            if (tokenMap.isEmpty()) {
                requestManager.getSessionManager().removeAttribute(tokenKey);
            }
        }).orElse(() -> {
            requestManager.getSessionManager().removeAttribute(tokenKey);
        });
    }

    // ===================================================================================
    //                                                                        Token Access
    //                                                                        ============
    protected OptionalThing<DoubleSubmitTokenMap> getSessionTokenMap(String tokenKey) {
        return requestManager.getSessionManager().getAttribute(tokenKey, DoubleSubmitTokenMap.class);
    }

    protected OptionalThing<String> getRequestedToken(String tokenKey) {
        return requestManager.getParameter(tokenKey);
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected String getTokenKey() {
        return TOKEN_KEY;
    }
}
