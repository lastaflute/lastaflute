/*
 * Copyright 2015-2021 the original author or authors.
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
package org.lastaflute.web.ruts.config.analyzer;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

import org.lastaflute.unit.UnitLastaFluteTestCase;
import org.lastaflute.web.ruts.config.analyzer.UrlPatternAnalyzer.UrlPatternChosenBox;

/**
 * @author jflute
 */
public class UrlPatternAnalyzerTest extends UnitLastaFluteTestCase {

    public void test_pattern_basic() throws Exception {
        // ## Arrange ##
        String pattern = "^" + UrlPatternAnalyzer.ELEMENT_BASIC_PATTERN + "$";

        // ## Act ##
        // ## Assert ##
        assertTrue(Pattern.compile(pattern).matcher("sea").find());
        assertTrue(Pattern.compile(pattern).matcher("$sea").find());
        assertFalse(Pattern.compile(pattern).matcher("/sea").find());
        assertTrue(Pattern.compile(pattern).matcher("123").find());
        assertFalse(Pattern.compile(pattern).matcher("/123").find());
        assertTrue(Pattern.compile(pattern).matcher("1.3").find());
        assertTrue(Pattern.compile(pattern).matcher("-13").find());
        assertTrue(Pattern.compile(pattern).matcher("+13").find());
    }

    public void test_pattern_number() throws Exception {
        // ## Arrange ##
        String pattern = "^" + UrlPatternAnalyzer.ELEMENT_NUMBER_PATTERN + "$";

        // ## Act ##
        // ## Assert ##
        assertFalse(Pattern.compile(pattern).matcher("sea").find());
        assertFalse(Pattern.compile(pattern).matcher("$sea").find());
        assertFalse(Pattern.compile(pattern).matcher("/sea").find());
        assertTrue(Pattern.compile(pattern).matcher("123").find());
        assertFalse(Pattern.compile(pattern).matcher("/123").find());
        assertTrue(Pattern.compile(pattern).matcher("1.3").find());
        assertTrue(Pattern.compile(pattern).matcher("-13").find());
        assertFalse(Pattern.compile(pattern).matcher("+13").find());
    }

    public void test_adjustUrlPatternMethodPrefix_methodKeyword_twoWord() throws Exception {
        // ## Arrange ##
        UrlPatternAnalyzer analyzer = new UrlPatternAnalyzer();
        Method dummyMethod = getClass().getMethods()[0];

        // ## Act ##
        UrlPatternChosenBox chosenBox = analyzer.adjustUrlPatternMethodPrefix(dummyMethod, "@word/{}/@word", "seaLand", true);

        // ## Assert ##
        log(chosenBox.getResolvedUrlPattern());
        assertEquals("sea/{}/land", chosenBox.getResolvedUrlPattern());
        assertFalse(chosenBox.isMethodNamePrefix());
        assertTrue(chosenBox.isSpecified());
    }
}
