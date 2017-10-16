package org.lastaflute.web.ruts.config;

import java.lang.reflect.Method;

import org.dbflute.util.Srl;
import org.lastaflute.di.core.meta.impl.ComponentDefImpl;
import org.lastaflute.unit.UnitLastaFluteTestCase;
import org.lastaflute.unit.mock.db.MockOldCDef;
import org.lastaflute.web.Execute;
import org.lastaflute.web.LastaAction;
import org.lastaflute.web.aspect.RomanticActionCustomizer;
import org.lastaflute.web.path.ActionAdjustmentProvider;
import org.lastaflute.web.response.HtmlResponse;

/**
 * @author jflute
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
        ActionExecute execute = mapping.findActionExecute(paramPath);
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
