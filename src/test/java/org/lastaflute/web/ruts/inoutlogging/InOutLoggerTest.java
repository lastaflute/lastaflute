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
package org.lastaflute.web.ruts.inoutlogging;

import java.util.LinkedHashMap;
import java.util.Map;

import org.lastaflute.unit.UnitLastaFluteTestCase;

/**
 * @author jflute
 * @since 1.1.9 (2020/11/19 Thursday at RJS)
 */
public class InOutLoggerTest extends UnitLastaFluteTestCase {

    public void test_doBuildRequestParameterExp_basic() {
        // ## Arrange ##
        InOutLogger logger = new InOutLogger();
        InOutLogOption option = new InOutLogOption();
        option.filterRequestParameterValue(entry -> {
            if (entry.getKey().equals("sea")) {
                return entry.getValue().toString().replace("a", "o");
            }
            return null;
        });
        Map<String, Object> requestParameterMap = new LinkedHashMap<>();
        requestParameterMap.put("sea", "hangar");
        requestParameterMap.put("land", "showbase");

        // ## Act ##
        String exp = logger.doBuildRequestParameterExp(requestParameterMap, option);

        // ## Assert ##
        log(exp);
        assertContains(exp, "hongor");
        assertNotContains(exp, "hangar");
        assertContainsAll(exp, "sea", "land", "showbase");
    }

    public void test_doBuildRequestParameterExp_noParameter() {
        // ## Arrange ##
        InOutLogger logger = new InOutLogger();
        InOutLogOption option = new InOutLogOption();
        option.filterRequestParameterValue(entry -> {
            fail(); // no here
            return null;
        });
        Map<String, Object> requestParameterMap = new LinkedHashMap<>();

        // ## Act ##
        String exp = logger.doBuildRequestParameterExp(requestParameterMap, option);

        // ## Assert ##
        log(exp);
        assertNull(exp);
    }
}
