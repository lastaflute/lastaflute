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
package org.lastaflute.core.security;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.lastaflute.core.security.exception.CipherFailureException;

/**
 * @author jflute
 */
public class OneWayCryptographer {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    public static final String ALGORITHM_SHA1 = "SHA-1";
    public static final String ENCODING_UTF8 = "UTF-8";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String algorithm;
    protected final String encoding;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public OneWayCryptographer(String algorithm, String encoding) {
        this.algorithm = algorithm;
        this.encoding = encoding;
    }

    public static OneWayCryptographer createSha1Cryptographer() {
        return new OneWayCryptographer(ALGORITHM_SHA1, ENCODING_UTF8);
    }

    // ===================================================================================
    //                                                                             Encrypt
    //                                                                             =======
    /**
     * Encrypt the text as one-way code. <br>
     * If the specified text is null or empty, it returns the text without encrypting.
     * @param plainText The plain text to be encrypted. (NullAllowed: if null, returns null)
     * @return The encrypted text as one-way code. (NullAllowed: when the text is null, EmptyAllowed: if empty specified)
     * @throws CipherFailureException When the cipher fails.
     */
    public String oneway(String plainText) {
        assertArgumentNotNull("plainText", plainText);
        return doOneWay(plainText);
    }

    protected String doOneWay(String plainText) {
        final String encoding = getEncoding();
        final MessageDigest digest = createDigest();
        try {
            digest.update(plainText.getBytes(encoding));
        } catch (UnsupportedEncodingException e) {
            String msg = "Unknown encoding: " + encoding;
            throw new CipherFailureException(msg);
        }
        return convertToCryptoString(digest.digest());
    }

    protected String getEncoding() {
        return encoding;
    }

    protected MessageDigest createDigest() {
        final String algorithm = getAlgorithm();
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            String msg = "Failed to get instance of digest: " + algorithm;
            throw new CipherFailureException(msg, e);
        }
    }

    protected String getAlgorithm() {
        return algorithm;
    }

    protected String convertToCryptoString(byte[] bytes) {
        final StringBuilder sb = new StringBuilder();
        for (byte bt : bytes) {
            sb.append(String.format("%02x", bt));
        }
        return sb.toString();
    }

    // ===================================================================================
    //                                                                       Assist Helper
    //                                                                       =============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            throw new IllegalArgumentException("The variableName should not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
        }
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return "{" + algorithm + ", " + encoding + "}";
    }
}
