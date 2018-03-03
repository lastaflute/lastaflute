/*
 * Copyright 2015-2018 the original author or authors.
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

import org.lastaflute.core.security.exception.CipherFailureException;

/**
 * @author jflute
 */
public interface PrimaryCipher {

    /**
     * Encrypt the text as invertible.
     * @param plainText The plain text to be encrypted. (NotNull, EmptyAllowed)
     * @return The encrypted text from the plain text. (NotNull, EmptyAllowed: depends on algorithm)
     * @throws CipherFailureException When the cipher fails.
     */
    String encrypt(String plainText);

    /**
     * Decrypt the encrypted text (back to plain text).
     * @param encryptedText The encrypted text to be decrypted. (NotNull, EmptyAllowed)
     * @return The plain text from the encrypted text. (NotNull, EmptyAllowed: if secret key is empty)
     * @throws CipherFailureException When the cipher fails.
     */
    String decrypt(String encryptedText);

    /**
     * Encrypt the text as one-way code. <br/>
     * If the specified text is null or empty, it returns the text without encrypting.
     * @param plainText The plain text to be encrypted. (NotNull, EmptyAllowed)
     * @return The encrypted text as one-way code. (NotNull, EmptyAllowed: depends on algorithm)
     * @throws CipherFailureException When the cipher fails.
     */
    String oneway(String plainText);
}
