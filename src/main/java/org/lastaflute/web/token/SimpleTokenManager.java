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

import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author modified by jflute (originated in Struts)
 */
public class SimpleTokenManager implements TokenManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final String TOKEN_KEY = LastaWebKey.TRANSACTION_TOKEN_KEY;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    @Resource
    protected RequestManager requestManager;

    protected long previous;

    // ===================================================================================
    //                                                                 Token Determination
    //                                                                 ===================
    @Override
    public synchronized boolean isTokenValid() {
        return isTokenValid(false);
    }

    @Override
    public synchronized boolean isTokenValid(boolean reset) {
        return requestManager.getSessionManager().getAttribute(TOKEN_KEY, String.class).map(saved -> {
            if (reset) {
                resetToken();
            }
            return requestManager.getParameter(TOKEN_KEY).map(token -> saved.equals(token)).orElse(false);
        }).orElse(false);
    }

    // ===================================================================================
    //                                                                  Token Manipulation
    //                                                                  ==================
    @Override
    public synchronized void resetToken() {
        requestManager.getSessionManager().removeAttribute(TOKEN_KEY);
    }

    @Override
    public synchronized void saveToken() {
        requestManager.getSessionManager().setAttribute(TOKEN_KEY, generateToken());
    }

    @Override
    public synchronized String generateToken() {
        try {
            final byte[] idBytes = requestManager.getSessionManager().getSessionId().getBytes();
            long current = System.currentTimeMillis();
            if (current == previous) {
                current++;
            }
            previous = current;
            byte now[] = Long.valueOf(current).toString().getBytes();
            final MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(idBytes);
            md.update(now);
            return toHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            return null;
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
}
