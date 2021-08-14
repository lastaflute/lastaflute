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

    // ===================================================================================
    //                                                                       Basic Pattern
    //                                                                       =============
    public void test_basicPattern_basic() throws Exception {
        // ## Arrange ##
        UrlPatternAnalyzer analyzer = new UrlPatternAnalyzer();
        Pattern pattern = analyzer.buildRegexpPattern(UrlPatternAnalyzer.ELEMENT_BASIC_PATTERN);

        // ## Act ##
        // ## Assert ##
        assertTrue(pattern.matcher("sea").find());
        assertTrue(pattern.matcher("$sea").find());
        assertFalse(pattern.matcher("/sea").find());
        assertTrue(pattern.matcher("123").find());
        assertFalse(pattern.matcher("/123").find());
        assertTrue(pattern.matcher("1.3").find());
        assertTrue(pattern.matcher("-13").find());
        assertTrue(pattern.matcher("+13").find());
    }

    // ===================================================================================
    //                                                                      Number Pattern
    //                                                                      ==============
    public void test_numberPattern_number() throws Exception {
        // ## Arrange ##
        UrlPatternAnalyzer analyzer = new UrlPatternAnalyzer();
        Pattern pattern = analyzer.buildRegexpPattern(UrlPatternAnalyzer.ELEMENT_NUMBER_PATTERN);

        // ## Act ##
        // ## Assert ##
        assertFalse(pattern.matcher("sea").find());
        assertFalse(pattern.matcher("$sea").find());
        assertFalse(pattern.matcher("/sea").find());
        assertTrue(pattern.matcher("123").find());
        assertFalse(pattern.matcher("/123").find());
        assertTrue(pattern.matcher("1.3").find());
        assertTrue(pattern.matcher("-13").find());
        assertFalse(pattern.matcher("+13").find());
    }

    public void test_numberPattern_keepCompatible() throws Exception {
        // wrote before fixing regular expression, and expect green after fix
        assertFalse(evaluate("sea/1/2"));
        assertFalse(evaluate("sea/1/2/"));
        assertFalse(evaluate("/sea/1/2"));
        assertFalse(evaluate("/sea/1/2/"));
        assertFalse(evaluate("1/2/sea"));
        assertFalse(evaluate("sea/1"));
        assertFalse(evaluate("1/sea"));
        assertFalse(evaluate("sea"));
        assertFalse(evaluate("/sea"));
        assertFalse(evaluate("/sea/"));
        assertFalse(evaluate("sea1"));
        assertFalse(evaluate("1sea"));
        assertFalse(evaluate("1sea1"));
        assertFalse(evaluate("+1"));
        assertFalse(evaluate("+1.1"));

        assertTrue(evaluate("0"));
        assertTrue(evaluate("1"));
        assertTrue(evaluate("11111111111111111111111111"));
        assertTrue(evaluate("-0"));
        assertTrue(evaluate("-1"));
        assertTrue(evaluate("-11111111111111111111111111"));
        assertTrue(evaluate("012"));
        assertTrue(evaluate("123"));
        assertTrue(evaluate("1.23"));
        assertTrue(evaluate("1.23"));
        assertTrue(evaluate("12.3"));
        assertTrue(evaluate("12222222222222222222.333333333333333333333333"));
        assertTrue(evaluate(".123"));
        assertTrue(evaluate("123."));
        assertTrue(evaluate(".123."));
        assertTrue(evaluate("1.1.1")); // ? but compatible
    }

    private boolean evaluate(String regex) {
        UrlPatternAnalyzer analyzer = new UrlPatternAnalyzer();
        Pattern pattern = analyzer.buildRegexpPattern(UrlPatternAnalyzer.ELEMENT_NUMBER_PATTERN);
        return pattern.matcher(regex).find();
    }

    // ===================================================================================
    //                                                                       Method Prefix
    //                                                                       =============
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
