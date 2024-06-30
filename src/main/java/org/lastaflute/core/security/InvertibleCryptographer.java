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
package org.lastaflute.core.security;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.core.security.exception.CipherFailureException;

/**
 * @author jflute (using Commons-Codec logic, thanks)
 */
public class InvertibleCryptographer {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    public static final String ALGORITHM_AES = "AES";
    public static final String ALGORITHM_BLOWFISH = "Blowfish";
    public static final String ALGORITHM_DES = "DES";
    public static final String ALGORITHM_RSA = "RSA";
    public static final String ALGORITHM_UNSUPPORTED = "UNSUPPORTED";
    public static final String ENCODING_UTF8 = "UTF-8";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String algorithm;
    protected final SecretKey skey;
    protected final String encoding;
    protected Cipher encryptingCipher;
    protected Cipher decryptingCipher;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public InvertibleCryptographer(String algorithm, SecretKey skey, String charset) {
        this.algorithm = algorithm;
        this.skey = skey;
        this.encoding = charset;
    }

    public InvertibleCryptographer(String algorithm, String skey, String charset) {
        this.algorithm = algorithm;
        this.skey = createSKey(skey);
        this.encoding = charset;
    }

    protected SecretKey createSKey(String skey) {
        return new SecretKeySpec(skey.getBytes(), algorithm);
    }

    public static InvertibleCryptographer createAesCipher(String skey) {
        return new InvertibleCryptographer(ALGORITHM_AES, skey, ENCODING_UTF8);
    }

    public static InvertibleCryptographer createBlowfishCipher(String skey) {
        return new InvertibleCryptographer(ALGORITHM_BLOWFISH, skey, ENCODING_UTF8);
    }

    public static InvertibleCryptographer createDesCipher(String skey) {
        return new InvertibleCryptographer(ALGORITHM_DES, skey, ENCODING_UTF8);
    }

    public static InvertibleCryptographer createRsaCipher(String skey) {
        return new InvertibleCryptographer(ALGORITHM_RSA, skey, ENCODING_UTF8);
    }

    public static InvertibleCryptographer createUnsupportedCipher(String appMessage) {
        return new InvertibleCryptographer(ALGORITHM_UNSUPPORTED, "dummy", ENCODING_UTF8) {
            @Override
            protected synchronized void initialize() {
                final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
                br.addNotice("Unsupported cipher so you cannot call it now.");
                br.addItem("Advice");
                br.addElement("The cipher is not unsupported as your settings.");
                br.addElement("If you need, set up your cipher at FwAssistantDirector.");
                br.addElement("The director is located at...");
                br.addElement(" e.g. [your-service-package].mylasta.direction.[App]FwAssistantirector");
                br.addItem("Application Message");
                br.addElement(appMessage);
                final String msg = br.buildExceptionMessage();
                throw new UnsupportedOperationException(msg);
            }
        };
    }

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    protected synchronized void initialize() {
        if (encryptingCipher != null) {
            return;
        }
        assertInit();
        doInitializeCipher();
    }

    protected void assertInit() {
        if (skey == null) {
            throw new IllegalStateException("Not found himitu kagi.");
        }
        if (encoding == null) {
            throw new IllegalStateException("Not found charset.");
        }
    }

    protected void doInitializeCipher() {
        try {
            encryptingCipher = Cipher.getInstance(algorithm);
            encryptingCipher.init(Cipher.ENCRYPT_MODE, skey);
            decryptingCipher = Cipher.getInstance(algorithm);
            decryptingCipher.init(Cipher.DECRYPT_MODE, skey);
        } catch (NoSuchAlgorithmException e) {
            throw new CipherFailureException("Failed by unknown algorithm: " + algorithm, e);
        } catch (NoSuchPaddingException e) {
            throw new CipherFailureException("Failed by no such padding: " + algorithm, e);
        } catch (InvalidKeyException e) {
            throwCipherFailureInvalidKeyException(e); // frequently ocurred
        }
    }

    protected void throwCipherFailureInvalidKeyException(InvalidKeyException e) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Failed to initialize the cipher by the invalid key.");
        br.addItem("Advice");
        br.addElement("Make sure your key for your algorithm.");
        br.addElement("The algorithms may require own patterns of key.");
        br.addElement("(cannot show the key for security so read your program)");
        br.addItem("Algorithm");
        br.addElement(algorithm);
        final String msg = br.buildExceptionMessage();
        throw new CipherFailureException(msg, e);
    }

    // ===================================================================================
    //                                                                     Encrypt/Decrypt
    //                                                                     ===============
    /**
     * Encrypt the text as invertible.
     * @param plainText The plain text to be encrypted. (NotNull, EmptyAllowed)
     * @return The encrypted text from the plain text. (NotNull, EmptyAllowed: depends on algorithm)
     * @throws CipherFailureException When the cipher fails.
     */
    public synchronized String encrypt(String plainText) {
        assertArgumentNotNull("plainText", plainText);
        if (encryptingCipher == null) {
            initialize();
        }
        return new String(encodeHex(doEncrypt(plainText)));
    }

    protected byte[] doEncrypt(String plainText) {
        try {
            return encryptingCipher.doFinal(plainText.getBytes(encoding));
        } catch (IllegalBlockSizeException e) {
            throw new CipherFailureException("Failed by illegal block size: " + plainText, e);
        } catch (BadPaddingException e) {
            throw new CipherFailureException("Failed by bad padding: " + plainText, e);
        } catch (UnsupportedEncodingException e) {
            throw new CipherFailureException("Failed by unsupported encoding: " + encoding, e);
        }
    }

    /**
     * Decrypt the encrypted text (back to plain text). <br>
     * @param encryptedText The encrypted text to be decrypted. (NotNull, EmptyAllowed)
     * @return The plain text from the encrypted text. (NotNull, EmptyAllowed: if secret key is empty)
     * @throws CipherFailureException When the cipher fails.
     */
    public synchronized String decrypt(String encryptedText) {
        assertArgumentNotNull("encryptedText", encryptedText);
        if (decryptingCipher == null) {
            initialize();
        }
        try {
            return new String(doDecrypt(encryptedText), encoding);
        } catch (UnsupportedEncodingException e) {
            throw new CipherFailureException("Failed by unsupported encoding: " + encoding, e);
        }
    }

    protected byte[] doDecrypt(String cryptedText) {
        try {
            return decryptingCipher.doFinal(decodeHex(cryptedText.toCharArray()));
        } catch (IllegalBlockSizeException e) {
            throw new CipherFailureException("Failed by illegal block size: " + cryptedText, e);
        } catch (BadPaddingException e) {
            throw new CipherFailureException("Failed by bad padding: " + cryptedText, e);
        }
    }

    // ===================================================================================
    //                                                                  from Commons-Codec
    //                                                                  ==================
    protected static final char[] DIGITS_LOWER = { // hexadecimal
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' // number
            , 'a', 'b', 'c', 'd', 'e', 'f' }; // alphabet

    protected char[] encodeHex(byte[] data) {
        final int len = data.length;
        final char[] out = new char[len << 1];
        for (int i = 0, j = 0; i < len; i++) {
            out[j++] = DIGITS_LOWER[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS_LOWER[0x0F & data[i]];
        }
        return out;
    }

    protected byte[] decodeHex(char[] data) {
        final int len = data.length;
        if ((len & 0x01) != 0) {
            throw new CipherFailureException("Odd number of characters."); // not show data for security
        }
        final byte[] out = new byte[len >> 1];
        for (int i = 0, j = 0; j < len; i++) {
            int f = toDigit(data[j], j) << 4;
            j++;
            f = f | toDigit(data[j], j);
            j++;
            out[i] = (byte) (f & 0xFF);
        }
        return out;
    }

    protected int toDigit(char ch, int index) {
        final int digit = Character.digit(ch, 16);
        if (digit == -1) {
            throw new CipherFailureException("Illegal hexadecimal character " + ch + " at index " + index);
        }
        return digit;
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
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
        return "{" + algorithm + ", " + encoding + "}"; // don't show secret key for security
    }
}
