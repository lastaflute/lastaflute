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
package org.lastaflute.web.ruts.config;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.unit.UnitLastaFluteTestCase;
import org.lastaflute.web.Execute;
import org.lastaflute.web.LastaAction;
import org.lastaflute.web.exception.ExecuteMethodIllegalDefinitionException;
import org.lastaflute.web.response.HtmlResponse;

/**
 * @author jflute
 * @since 1.0.1 (2017/10/16 Monday at bay maihama)
 */
@SuppressWarnings("unused") // for now, near-path headache
public class ActionMappingNearpathAsdefaultTest extends UnitLastaFluteTestCase {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final String index = "index";
    private static final String named = "named";
    private static final String nonno = "$notFound";

    // ===================================================================================
    //                                                                            Optional
    //                                                                            ========
    public void test_paramPath_asdefault10_optional_string_to_non() {
        assertException(ExecuteMethodIllegalDefinitionException.class, () -> {
            prepareMapping(NearpathByword10Optstr2nonAction.class);
        });
    }

    private static class NearpathByword10Optstr2nonAction extends LastaAction {

        @Execute
        public HtmlResponse index(OptionalThing<String> first) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute
        public HtmlResponse named() {
            return HtmlResponse.asEmptyBody();
        }
    }

    public void test_paramPath_asdefault11_optional_number_to_non() {
        ActionMapping mapping = prepareMapping(NearpathByword11Optnum2nonAction.class);
        assertExecute(mapping, nonno, "sea"); // index before
        assertExecute(mapping, nonno, "sea/named");
        assertExecute(mapping, nonno, "sea/land");
        assertExecute(mapping, named, "named"); // index before
        assertExecute(mapping, nonno, "named/sea");
        assertExecute(mapping, index, "1");
        assertExecute(mapping, nonno, "1/named");
        assertExecute(mapping, nonno, "1/land");
        assertExecute(mapping, index, "-1");
        assertExecute(mapping, nonno, "-1/named");
        assertExecute(mapping, nonno, "-1/land");
    }

    private static class NearpathByword11Optnum2nonAction extends LastaAction {

        @Execute
        public HtmlResponse index(OptionalThing<Integer> first) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute
        public HtmlResponse named() {
            return HtmlResponse.asEmptyBody();
        }
    }

    // #needs_fix jflute pending, after resolving near-path headache (2020/10/06)
    //    public void test_paramPath_asdefault15_optional_string_more_to_string() {
    //        ActionMapping mapping = prepareMapping(NearpathByword15Optstrmore2strAction.class);
    //        assertExecute(mapping, index, "sea");
    //        assertExecute(mapping, index, "sea/named");
    //        assertExecute(mapping, index, "sea/land");
    //        assertExecute(mapping, nonno, "sea/land/piari");
    //        assertExecute(mapping, index, "named");
    //        assertExecute(mapping, index, "named/sea"); // hide
    //        assertExecute(mapping, index, "1");
    //        assertExecute(mapping, index, "1/named");
    //        assertExecute(mapping, index, "1/land");
    //        assertExecute(mapping, index, "-1");
    //        assertExecute(mapping, index, "-1/named");
    //        assertExecute(mapping, index, "-1/land");
    //    }

    private static class NearpathByword15Optstrmore2strAction extends LastaAction {

        @Execute
        public HtmlResponse index(OptionalThing<String> first, OptionalThing<String> second) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute
        public HtmlResponse named(String first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    private ActionMapping prepareMapping(Class<?> componentClass) {
        return ActionMappingBasicTest.prepareMapping(componentClass);
    }

    private void assertExecute(ActionMapping mapping, String methodName, String paramPath) {
        ActionMappingBasicTest.assertExecute(mapping, methodName, paramPath);
    }
}
