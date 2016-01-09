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
package org.lastaflute.web.validation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import org.lastaflute.unit.UnitLastaFluteTestCase;

/**
 * @author jflute
 */
public class RequiredValidatorTest extends UnitLastaFluteTestCase {

    public void test_determineValid_basic() throws Exception {
        // ## Arrange ##
        RequiredValidator validator = new RequiredValidator();

        // ## Act ##
        // ## Assert ##
        assertFalse(validator.determineValid(null));
        assertFalse(validator.determineValid(""));
        assertFalse(validator.determineValid(" "));
        assertTrue(validator.determineValid("sea"));

        assertTrue(validator.determineValid(1));
        assertTrue(validator.determineValid(3L));
        assertTrue(validator.determineValid(new BigDecimal("1.2")));

        assertTrue(validator.determineValid(new Date()));
        assertTrue(validator.determineValid(LocalDate.now()));
        assertTrue(validator.determineValid(LocalDateTime.now()));

        assertTrue(validator.determineValid(true));
        assertTrue(validator.determineValid(false));

        assertFalse(validator.determineValid(newArrayList()));
        assertTrue(validator.determineValid(newArrayList("sea")));

        assertFalse(validator.determineValid(newHashSet()));
        assertTrue(validator.determineValid(newHashSet("sea")));

        assertFalse(validator.determineValid(newHashMap()));
        assertTrue(validator.determineValid(newHashMap("sea", "land")));

        assertFalse(validator.determineValid(new String[] {}));
        assertTrue(validator.determineValid(new String[] { "sea" }));
        assertFalse(validator.determineValid(new int[] {}));
        assertTrue(validator.determineValid(new int[] { 1 }));
    }
}
