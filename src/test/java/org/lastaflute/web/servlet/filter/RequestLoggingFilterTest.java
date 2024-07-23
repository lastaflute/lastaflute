/*
 * Copyright 2015-2024 the original author or authors.
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

    public void test_convertToCutDisp_null() throws Exception {
        assertNull(new RequestLoggingFilter().convertToCutDisp(null));
    }

    public void test_convertToOneLinerDisp_null() throws Exception {
        assertNull(new RequestLoggingFilter().convertToOneLinerDisp(null));
    }
}
