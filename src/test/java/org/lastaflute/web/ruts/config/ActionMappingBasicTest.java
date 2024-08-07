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

import java.lang.reflect.Method;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.lastaflute.di.core.meta.impl.ComponentDefImpl;
import org.lastaflute.unit.UnitLastaFluteTestCase;
import org.lastaflute.unit.mock.db.MockOldCDef;
import org.lastaflute.web.Execute;
import org.lastaflute.web.LastaAction;
import org.lastaflute.web.aspect.RomanticActionCustomizer;
import org.lastaflute.web.path.ActionAdjustmentProvider;
import org.lastaflute.web.path.RoutingParamPath;
import org.lastaflute.web.response.HtmlResponse;

/**
 * @author jflute
 * @author pull request #55
 * @since 1.0.1 (2017/10/16 Monday at bay maihama)
 */
public class ActionMappingBasicTest extends UnitLastaFluteTestCase {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final String index = "index";
    private static final String named = "named";
    private static final String nonno = "$notFound";

    // ===================================================================================
    //                                                                               Basic
    //                                                                               =====
    public void test_paramPath_basic01_string_to_number() {
        ActionMapping mapping = prepareMapping(Basic01Str2numAction.class);
        assertExecute(mapping, index, "sea");
        assertExecute(mapping, nonno, "sea/named");
        assertExecute(mapping, nonno, "sea/land");
        assertExecute(mapping, nonno, "sea/land/piari");
        assertExecute(mapping, nonno, "named/sea");
        assertExecute(mapping, nonno, "named/sea/land");
        assertExecute(mapping, named, "named/1");
        assertExecute(mapping, nonno, "named/1/land");
        assertExecute(mapping, index, "1");
        assertExecute(mapping, nonno, "1/named");
        assertExecute(mapping, nonno, "1/land");
        assertExecute(mapping, index, "-1");
        assertExecute(mapping, nonno, "-1/named");
        assertExecute(mapping, nonno, "-1/land");
    }

    private static class Basic01Str2numAction extends LastaAction {

        @Execute
        public HtmlResponse index(String first) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute
        public HtmlResponse named(Integer first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    public void test_paramPath_basic02_number_to_string() {
        ActionMapping mapping = prepareMapping(Basic02Num2strAction.class);
        assertExecute(mapping, nonno, "sea");
        assertExecute(mapping, nonno, "sea/named");
        assertExecute(mapping, nonno, "sea/land");
        assertExecute(mapping, nonno, "sea/land/piari");
        assertExecute(mapping, named, "named/sea");
        assertExecute(mapping, nonno, "named/sea/land");
        assertExecute(mapping, named, "named/1");
        assertExecute(mapping, nonno, "named/1/land");
        assertExecute(mapping, index, "1");
        assertExecute(mapping, nonno, "1/named");
        assertExecute(mapping, nonno, "1/land");
        assertExecute(mapping, index, "-1");
        assertExecute(mapping, nonno, "-1/named");
        assertExecute(mapping, nonno, "-1/land");
    }

    private static class Basic02Num2strAction extends LastaAction {

        @Execute
        public HtmlResponse index(Integer first) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute
        public HtmlResponse named(String first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    public void test_paramPath_basic03_string_to_string() {
        ActionMapping mapping = prepareMapping(Basic03Str2numAction.class);
        assertExecute(mapping, index, "sea");
        assertExecute(mapping, nonno, "sea/named");
        assertExecute(mapping, nonno, "sea/land");
        assertExecute(mapping, nonno, "sea/land/piari");
        assertExecute(mapping, named, "named/sea");
        assertExecute(mapping, nonno, "named/sea/land");
        assertExecute(mapping, named, "named/1");
        assertExecute(mapping, nonno, "named/1/land");
        assertExecute(mapping, index, "1");
        assertExecute(mapping, nonno, "1/named");
        assertExecute(mapping, nonno, "1/land");
        assertExecute(mapping, index, "-1");
        assertExecute(mapping, nonno, "-1/named");
        assertExecute(mapping, nonno, "-1/land");
    }

    private static class Basic03Str2numAction extends LastaAction {

        @Execute
        public HtmlResponse index(String first) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute
        public HtmlResponse named(String first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    public void test_paramPath_basic04_number_to_number() {
        ActionMapping mapping = prepareMapping(Basic04Str2numAction.class);
        assertExecute(mapping, nonno, "sea");
        assertExecute(mapping, nonno, "sea/named");
        assertExecute(mapping, nonno, "sea/land");
        assertExecute(mapping, nonno, "sea/land/piari");
        assertExecute(mapping, nonno, "named/sea");
        assertExecute(mapping, nonno, "named/sea/land");
        assertExecute(mapping, named, "named/1");
        assertExecute(mapping, nonno, "named/1/land");
        assertExecute(mapping, index, "1");
        assertExecute(mapping, nonno, "1/named");
        assertExecute(mapping, nonno, "1/land");
        assertExecute(mapping, index, "-1");
        assertExecute(mapping, nonno, "-1/named");
        assertExecute(mapping, nonno, "-1/land");
    }

    private static class Basic04Str2numAction extends LastaAction {

        @Execute
        public HtmlResponse index(Integer first) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute
        public HtmlResponse named(Integer first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    // ===================================================================================
    //                                                                      Classification
    //                                                                      ==============
    public void test_paramPath_basic10_cls_to_string() {
        ActionMapping mapping = prepareMapping(Basic10Cls2strAction.class);
        assertExecute(mapping, index, "sea"); // #hope nonno
        assertExecute(mapping, nonno, "sea/named");
        assertExecute(mapping, nonno, "sea/land");
        assertExecute(mapping, nonno, "sea/land/piari");
        assertExecute(mapping, index, "FML");
        assertExecute(mapping, nonno, "FML/named");
        assertExecute(mapping, nonno, "FML/land");
        assertExecute(mapping, nonno, "FML/land/piari");
        assertExecute(mapping, named, "named/sea");
        assertExecute(mapping, nonno, "named/sea/land");
        assertExecute(mapping, named, "named/1");
        assertExecute(mapping, nonno, "named/1/land");
        assertExecute(mapping, index, "1"); // #hope nonno
        assertExecute(mapping, nonno, "1/named");
        assertExecute(mapping, nonno, "1/land");
        assertExecute(mapping, index, "-1"); // #hope nonno
        assertExecute(mapping, nonno, "-1/named");
        assertExecute(mapping, nonno, "-1/land");
    }

    private static class Basic10Cls2strAction extends LastaAction {

        @Execute
        public HtmlResponse index(MockOldCDef.MemberStatus first) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute
        public HtmlResponse named(String first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    public void test_paramPath_basic11_cls_to_number() {
        ActionMapping mapping = prepareMapping(Basic11Cls2numAction.class);
        assertExecute(mapping, index, "sea"); // #hope nonno
        assertExecute(mapping, nonno, "sea/named");
        assertExecute(mapping, nonno, "sea/land");
        assertExecute(mapping, nonno, "sea/land/piari");
        assertExecute(mapping, index, "FML");
        assertExecute(mapping, nonno, "FML/named");
        assertExecute(mapping, nonno, "FML/land");
        assertExecute(mapping, nonno, "FML/land/piari");
        assertExecute(mapping, nonno, "named/sea");
        assertExecute(mapping, nonno, "named/sea/land");
        assertExecute(mapping, named, "named/1");
        assertExecute(mapping, nonno, "named/1/land");
        assertExecute(mapping, index, "1"); // #hope nonno
        assertExecute(mapping, nonno, "1/named");
        assertExecute(mapping, nonno, "1/land");
        assertExecute(mapping, index, "-1"); // #hope nonno
        assertExecute(mapping, nonno, "-1/named");
        assertExecute(mapping, nonno, "-1/land");
    }

    private static class Basic11Cls2numAction extends LastaAction {

        @Execute
        public HtmlResponse index(MockOldCDef.MemberStatus first) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute
        public HtmlResponse named(Integer first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    // ===================================================================================
    //                                                                            Optional
    //                                                                            ========
    public void test_paramPath_basic25_optional_cls_to_string() {
        ActionMapping mapping = prepareMapping(Basic25Optcls2strAction.class);
        assertExecute(mapping, nonno, "sea"); // index before
        assertExecute(mapping, nonno, "sea/named");
        assertExecute(mapping, nonno, "sea/land");
        assertExecute(mapping, nonno, "sea/land/piari");
        assertExecute(mapping, index, "FML");
        assertExecute(mapping, nonno, "FML/named");
        assertExecute(mapping, nonno, "FML/land");
        assertExecute(mapping, nonno, "FML/land/piari");
        assertExecute(mapping, named, "named/sea");
        assertExecute(mapping, nonno, "named/sea/land");
        assertExecute(mapping, named, "named/1");
        assertExecute(mapping, nonno, "named/1/land");
        assertExecute(mapping, nonno, "1"); // index before
        assertExecute(mapping, nonno, "1/named");
        assertExecute(mapping, nonno, "1/land");
        assertExecute(mapping, nonno, "-1"); // index before
        assertExecute(mapping, nonno, "-1/named");
        assertExecute(mapping, nonno, "-1/land");
    }

    private static class Basic25Optcls2strAction extends LastaAction {

        @Execute
        public HtmlResponse index(OptionalThing<MockOldCDef.MemberStatus> first) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute
        public HtmlResponse named(String first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    public void test_paramPath_basic35_optional_cls_more_to_string() {
        ActionMapping mapping = prepareMapping(Basic35Optclsmore2strAction.class);
        assertExecute(mapping, nonno, "sea"); // index before
        assertExecute(mapping, nonno, "sea/named"); // index before
        assertExecute(mapping, nonno, "sea/land"); // index before
        assertExecute(mapping, nonno, "sea/land/piari");
        assertExecute(mapping, index, "FML");
        assertExecute(mapping, index, "FML/0");
        assertExecute(mapping, index, "FML/1");
        assertExecute(mapping, nonno, "FML/named"); // index before
        assertExecute(mapping, nonno, "FML/land"); // index before
        assertExecute(mapping, nonno, "FML/land/piari");
        assertExecute(mapping, named, "named/sea"); // index before
        assertExecute(mapping, nonno, "named/sea/land");
        assertExecute(mapping, named, "named/1"); // index before
        assertExecute(mapping, nonno, "named/1/land");
        assertExecute(mapping, nonno, "1"); // index before
        assertExecute(mapping, nonno, "1/named"); // index before
        assertExecute(mapping, nonno, "1/land"); // index before
        assertExecute(mapping, nonno, "-1"); // index before
        assertExecute(mapping, nonno, "-1/named"); // index before
        assertExecute(mapping, nonno, "-1/land"); // index before
    }

    private static class Basic35Optclsmore2strAction extends LastaAction {

        @Execute
        public HtmlResponse index(OptionalThing<MockOldCDef.MemberStatus> first, OptionalThing<MockOldCDef.Flg> second) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute
        public HtmlResponse named(String first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    public void test_paramPath_basic36_int_to_string() {
        ActionMapping mapping = prepareMapping(Basic36Int2strAction.class);
        assertExecute(mapping, nonno, "sea");
        assertExecute(mapping, nonno, "sea/named");
        assertExecute(mapping, nonno, "sea/land");
        assertExecute(mapping, nonno, "sea/land/piari");
        assertExecute(mapping, named, "named/sea");
        assertExecute(mapping, nonno, "named/sea/land");
        assertExecute(mapping, named, "named/1");
        assertExecute(mapping, nonno, "named/1/land");
        assertExecute(mapping, index, "1");
        assertExecute(mapping, nonno, "1/named");
        assertExecute(mapping, nonno, "1/land");
        assertExecute(mapping, index, "-1");
        assertExecute(mapping, nonno, "-1/named");
        assertExecute(mapping, nonno, "-1/land");
    }

    private static class Basic36Int2strAction extends LastaAction {

        @Execute
        public HtmlResponse index(int first) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute
        public HtmlResponse named(String first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    public void test_paramPath_basic37_int_to_form() {
        ActionMapping mapping = prepareMapping(Basic37Int2formAction.class);
        assertExecute(mapping, nonno, "sea");
        assertExecute(mapping, nonno, "sea/named");
        assertExecute(mapping, nonno, "sea/land");
        assertExecute(mapping, nonno, "sea/land/piari");
        assertExecute(mapping, nonno, "named/sea");
        assertExecute(mapping, nonno, "named/sea/land");
        assertExecute(mapping, nonno, "named/1");
        assertExecute(mapping, nonno, "named/1/land");
        assertExecute(mapping, index, "1");
        assertExecute(mapping, nonno, "1/named");
        assertExecute(mapping, nonno, "1/land");
        assertExecute(mapping, index, "-1");
        assertExecute(mapping, nonno, "-1/named");
        assertExecute(mapping, nonno, "-1/land");
    }

    private static class Basic37Int2formForm {
        @SuppressWarnings("unused")
        public String name;
    }

    private static class Basic37Int2formAction extends LastaAction {

        @Execute
        public HtmlResponse index(int first) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute
        public HtmlResponse update(Basic37Int2formForm form) {
            return HtmlResponse.asEmptyBody();
        }
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    public static ActionMapping prepareMapping(Class<?> componentClass) {
        String actionName = Srl.initUncap(componentClass.getSimpleName());
        ComponentDefImpl actionDef = new ComponentDefImpl(componentClass, actionName);
        ModuleConfig moduleConfig = new ModuleConfig();
        RomanticActionCustomizer customizer = new RomanticActionCustomizer() {
            @Override
            protected ModuleConfig getModuleConfig() {
                return moduleConfig;
            }

            @Override
            protected ActionAdjustmentProvider comeOnAdjustmentProvider() {
                return new ActionAdjustmentProvider() {
                };
            }
        };
        customizer.customize(actionDef);
        ActionMapping mapping = moduleConfig.findActionMapping(actionName).get();
        return mapping;
    }

    public static void assertExecute(ActionMapping mapping, String methodName, String paramPath) {
        ActionExecute execute = mapping.findActionExecute(new RoutingParamPath(paramPath));
        if (nonno.equals(methodName)) {
            assertNull("The execute method exists: paramPath=" + paramPath + ", execute=" + execute, execute);
        } else {
            assertNotNull("Not found the action execute: paramPath=" + paramPath, execute);
            Method executeMethod = execute.getExecuteMethod();
            assertNotNull("Not found the execute method: paramPath=" + paramPath, executeMethod);
            String actualName = executeMethod.getName();
            assertEquals(methodName, actualName);
        }
    }
}
