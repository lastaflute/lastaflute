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
package org.lastaflute.web.servlet.cookie;

import org.lastaflute.web.servlet.cookie.exception.CookieCipherDecryptFailureException;

/**
 * @author jflute
 */
public interface CookieCipher {

    String encrypt(String plainText);

    /**
     * Decrypt the crypted text. <br>
     * You should handle the failure exception, ignore or catch and throw... <br>
     * (crypted text comes from user world so you needs attention)
     * @param cryptedText The crypted text to be decrypted. (NullAllowed: if null, returns null)
     * @return The decrypted text. (NullAllowed: when the text is null)
     * @throws CookieCipherDecryptFailureException When it fails to decrypt the text. 
     */
    String decrypt(String cryptedText) throws CookieCipherDecryptFailureException;
}
