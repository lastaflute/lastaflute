package org.lastaflute.web.servlet.filter;

import org.dbflute.utflute.core.PlainTestCase;

/**
 * @author jflute
 */
public class RequestLoggingFilterTest extends PlainTestCase {

    public void test_filterAttributeDisp_cut() throws Exception {
        // ## Arrange ##
        RequestLoggingFilter filter = new RequestLoggingFilter();

        // ## Act ##
        // ## Assert ##
        assertEquals(null, filter.filterAttributeDisp(null));
        assertEquals("sea", filter.filterAttributeDisp("sea"));
        assertEquals("se...", filter.filterAttributeDisp("se\na"));

        StringBuilder overSb = new StringBuilder();
        for (int i = 0; i < 800; i++) {
            overSb.append("a");
        }
        StringBuilder justSb = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            justSb.append("a");
        }
        String justMsg = justSb.toString();
        String overMsg = overSb.toString();
        assertEquals(justMsg + "...", filter.filterAttributeDisp(overMsg));
        assertEquals(justMsg, filter.filterAttributeDisp(justMsg));

        StringBuilder mixSb = new StringBuilder();
        for (int i = 0; i < 600; i++) {
            if (i == 400) {
                mixSb.append("\n");
            }
            mixSb.append("a");
        }
        StringBuilder linedSb = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            linedSb.append("a");
        }
        String mixMsg = mixSb.toString();
        String linedMsg = linedSb.toString();
        assertEquals(linedMsg + "...", filter.filterAttributeDisp(mixMsg));
    }
}
