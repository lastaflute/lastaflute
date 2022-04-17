/*
 * Copyright 2015-2022 the original author or authors.
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

import org.dbflute.utflute.core.PlainTestCase;

/**
 * @author jflute
 */
public class InvertibleCryptographerTest extends PlainTestCase {

    public void test_AES() throws Exception {
        // ## Arrange ##
        String key = "1234567890123456"; // 16 byte
        InvertibleCryptographer cipher = InvertibleCryptographer.createAesCipher(key);

        // ## Act ##
        String encrypted = cipher.encrypt("abc");
        String decrypted = cipher.decrypt(encrypted);

        // ## Assert ##
        log(encrypted, decrypted);
        assertEquals("abc", decrypted);
    }

    public void test_UNSUPPORTED() throws Exception {
        // ## Arrange ##
        InvertibleCryptographer cipher = InvertibleCryptographer.createUnsupportedCipher("sea");

        // ## Act ##
        // ## Assert ##
        assertException(UnsupportedOperationException.class, () -> cipher.encrypt("hangar"));
        assertException(UnsupportedOperationException.class, () -> cipher.decrypt("hangar"));
    }
}
