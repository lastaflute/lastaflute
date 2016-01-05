package org.lastaflute.web.ruts.config.analyzer;

import java.lang.reflect.Method;

import org.lastaflute.unit.UnitLastaFluteTestCase;
import org.lastaflute.web.ruts.config.analyzer.UrlPatternAnalyzer.UrlPatternChosenBox;

/**
 * @author jflute
 */
public class UrlPatternAnalyzerTest extends UnitLastaFluteTestCase {

    public void test_adjustUrlPatternMethodPrefix_methodKeyword_twoWord() throws Exception {
        // ## Arrange ##
        UrlPatternAnalyzer analyzer = new UrlPatternAnalyzer();
        Method dummyMethod = getClass().getMethods()[0];

        // ## Act ##
        UrlPatternChosenBox chosenBox = analyzer.adjustUrlPatternMethodPrefix(dummyMethod, "@word/{}/@word", "seaLand");

        // ## Assert ##
        log(chosenBox.getUrlPattern());
        assertEquals("sea/{}/land", chosenBox.getUrlPattern());
        assertFalse(chosenBox.isMethodNamePrefix());
    }
}
