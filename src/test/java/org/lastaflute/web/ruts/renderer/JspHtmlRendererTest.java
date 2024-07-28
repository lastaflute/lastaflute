package org.lastaflute.web.ruts.renderer;

import org.dbflute.utflute.core.PlainTestCase;

/**
 * @author jflute
 */
public class JspHtmlRendererTest extends PlainTestCase {

    public void test_determineExportablePropertyTypeName_basic() {
        // ## Arrange ##
        JspHtmlRenderer renderer = new JspHtmlRenderer();

        // ## Act ##
        // ## Assert ##
        assertTrue(renderer.determineExportablePropertyTypeName("java.lang.String"));
        assertTrue(renderer.determineExportablePropertyTypeName("java.lang.Integer"));
        assertTrue(renderer.determineExportablePropertyTypeName("org.lastaflute.Sea"));
        assertFalse(renderer.determineExportablePropertyTypeName("javax.servlet.HttpServletRequest"));
        assertFalse(renderer.determineExportablePropertyTypeName("jakarta.servlet.HttpServletRequest"));
    }
}
