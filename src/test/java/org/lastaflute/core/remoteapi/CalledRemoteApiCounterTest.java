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
package org.lastaflute.core.remoteapi;

import org.dbflute.utflute.core.PlainTestCase;

/**
 * @author jflute
 */
public class CalledRemoteApiCounterTest extends PlainTestCase {

    public void test_increment_basic() {
        // ## Arrange ##
        CalledRemoteApiCounter counter = new CalledRemoteApiCounter();
        assertEquals("{total=0}", counter.toLineDisp());
        assertNull(counter.facadeCountMap);

        // ## Act ##
        counter.increment("SeaBhv");

        // ## Assert ##
        assertEquals("{total=1, SeaBhv=1}", counter.toLineDisp());
        assertNotNull(counter.facadeCountMap);

        // ## Act ##
        counter.increment("SeaBhv");

        // ## Assert ##
        assertEquals("{total=2, SeaBhv=2}", counter.toLineDisp());

        // ## Act ##
        counter.increment("LandBhv");

        // ## Assert ##
        assertEquals("{total=3, SeaBhv=2, LandBhv=1}", counter.toLineDisp());
    }

    public void test_increment_null() {
        // ## Arrange ##
        CalledRemoteApiCounter counter = new CalledRemoteApiCounter();

        // ## Act ##
        // ## Assert ##
        assertException(IllegalArgumentException.class, () -> counter.increment(null));
    }
}
