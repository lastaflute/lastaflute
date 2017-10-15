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
import org.lastaflute.web.response.HtmlResponse;

/**
 * @author jflute
 * @since 1.0.1 (2017/10/16 Monday at bay maihama)
 */
public class ActionMappingNearpathTest extends UnitLastaFluteTestCase {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final String index = "index";
    private static final String named = "named";
    private static final String nonno = "$notFound";

    // ===================================================================================
    //                                                                               Basic
    //                                                                               =====
    public void test_paramPath_byword01_number_to_string() {
        ActionMapping mapping = prepareMapping(NearpathByword01Str2numAction.class);
        assertExecute(mapping, index, "sea");
        assertExecute(mapping, nonno, "sea/named");
        assertExecute(mapping, nonno, "sea/land");
        assertExecute(mapping, nonno, "sea/land/piari");
        assertExecute(mapping, index, "1");
        assertExecute(mapping, named, "1/named");
        assertExecute(mapping, nonno, "1/land");
        assertExecute(mapping, index, "-1");
        assertExecute(mapping, named, "-1/named");
    }

    private static class NearpathByword01Str2numAction extends LastaAction {

        @Execute
        public HtmlResponse index(String first) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute(urlPattern = "{}/@word")
        public HtmlResponse named(Integer first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    public void test_paramPath_byword02_number_to_string() {
        ActionMapping mapping = prepareMapping(NearpathByword02Num2strAction.class);
        assertExecute(mapping, nonno, "sea");
        assertExecute(mapping, named, "sea/named");
        assertExecute(mapping, nonno, "sea/land");
        assertExecute(mapping, nonno, "sea/land/piari");
        assertExecute(mapping, index, "1");
        assertExecute(mapping, named, "1/named");
        assertExecute(mapping, nonno, "1/land");
        assertExecute(mapping, index, "-1");
        assertExecute(mapping, named, "-1/named");
        assertExecute(mapping, nonno, "-1/land");
    }

    private static class NearpathByword02Num2strAction extends LastaAction {

        @Execute
        public HtmlResponse index(Integer first) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute(urlPattern = "{}/@word")
        public HtmlResponse named(String first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    // ===================================================================================
    //                                                                            Optional
    //                                                                            ========
    public void test_paramPath_byword05_optional_on_string_to_number() {
        ActionMapping mapping = prepareMapping(NearpathByword05Optonstr2numAction.class);
        assertExecute(mapping, index, "sea");
        assertExecute(mapping, index, "sea/named");
        assertExecute(mapping, index, "sea/land");
        assertExecute(mapping, nonno, "sea/land/piari");

        assertExecute(mapping, index, "1");
        assertExecute(mapping, index, "1/named"); // hide
        assertExecute(mapping, index, "1/land");

        assertExecute(mapping, index, "-1");
        assertExecute(mapping, index, "-1/named"); // hide
        assertExecute(mapping, index, "-1/land");
    }

    private static class NearpathByword05Optonstr2numAction extends LastaAction {

        @Execute
        public HtmlResponse index(String first, OptionalThing<String> second) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute(urlPattern = "{}/@word")
        public HtmlResponse named(Integer first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    public void test_paramPath_byword06_optional_on_number_string() {
        ActionMapping mapping = prepareMapping(NearpathByword06Optonnum2strAction.class);
        assertExecute(mapping, nonno, "sea");
        assertExecute(mapping, named, "sea/named"); // by number check
        assertExecute(mapping, nonno, "sea/land");
        assertExecute(mapping, nonno, "sea/land/piari");

        assertExecute(mapping, index, "1");
        assertExecute(mapping, index, "1/named"); // hide
        assertExecute(mapping, nonno, "1/named/piari");
        assertExecute(mapping, index, "1/land");

        assertExecute(mapping, index, "-1");
        assertExecute(mapping, index, "-1/named"); // hide
        assertExecute(mapping, nonno, "-1/named/piari");
        assertExecute(mapping, index, "-1/land");
    }

    private static class NearpathByword06Optonnum2strAction extends LastaAction {

        @Execute
        public HtmlResponse index(Integer first, OptionalThing<String> second) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute(urlPattern = "{}/@word")
        public HtmlResponse named(String first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    public void test_paramPath_byword07_optional_on_cls_to_string() {
        ActionMapping mapping = prepareMapping(NearpathByword07Optoncls2strAction.class);
        assertExecute(mapping, nonno, "sea");
        assertExecute(mapping, named, "sea/named"); // by classification check
        assertExecute(mapping, nonno, "sea/land"); // by classification check
        assertExecute(mapping, nonno, "sea/land/piari");

        assertExecute(mapping, index, "FML");
        assertExecute(mapping, index, "FML/named");
        assertExecute(mapping, index, "FML/land");
        assertExecute(mapping, nonno, "FML/land/piari");

        assertExecute(mapping, nonno, "1");
        assertExecute(mapping, named, "1/named"); // by classification check
        assertExecute(mapping, nonno, "1/named/piari");
        assertExecute(mapping, nonno, "1/land"); // by classification check

        assertExecute(mapping, nonno, "-1");
        assertExecute(mapping, named, "-1/named"); // by classification check
        assertExecute(mapping, nonno, "-1/named/piari");
        assertExecute(mapping, nonno, "-1/land"); // by classification check
    }

    private static class NearpathByword07Optoncls2strAction extends LastaAction {

        @Execute
        public HtmlResponse index(MockOldCDef.MemberStatus first, OptionalThing<String> second) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute(urlPattern = "{}/@word")
        public HtmlResponse named(String first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    public void test_paramPath_byword08_optional_on_cls_to_number() {
        ActionMapping mapping = prepareMapping(NearpathByword08Optoncls2numAction.class);
        assertExecute(mapping, nonno, "sea");
        assertExecute(mapping, nonno, "sea/named"); // by classification check
        assertExecute(mapping, nonno, "sea/land"); // by classification check
        assertExecute(mapping, nonno, "sea/land/piari");

        assertExecute(mapping, index, "FML");
        assertExecute(mapping, index, "FML/named");
        assertExecute(mapping, index, "FML/land");
        assertExecute(mapping, nonno, "FML/land/piari");

        assertExecute(mapping, nonno, "1");
        assertExecute(mapping, named, "1/named"); // by classification check
        assertExecute(mapping, nonno, "1/named/piari");
        assertExecute(mapping, nonno, "1/land");

        assertExecute(mapping, nonno, "-1");
        assertExecute(mapping, named, "-1/named"); // by classification check
        assertExecute(mapping, nonno, "-1/named/piari");
        assertExecute(mapping, nonno, "-1/land");
    }

    private static class NearpathByword08Optoncls2numAction extends LastaAction {

        @Execute
        public HtmlResponse index(MockOldCDef.MemberStatus first, OptionalThing<String> second) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute(urlPattern = "{}/@word")
        public HtmlResponse named(Integer first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    // ===================================================================================
    //                                                                               Theme
    //                                                                               =====
    public void test_paramPath_byword10_named_parade() {
        ActionMapping mapping = prepareMapping(NearpathByword10NamedparadeAction.class);
        assertExecute(mapping, "sea", "1/sea");
        assertExecute(mapping, "sea", "1/sea");
        assertExecute(mapping, "land", "1/land");
        assertExecute(mapping, "piari", "maihama/piari");
        assertExecute(mapping, "bonvo", "maihama/bonvo");
    }

    private static class NearpathByword10NamedparadeAction extends LastaAction {

        @Execute(urlPattern = "{}/@word")
        public HtmlResponse sea(Integer first) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute(urlPattern = "{}/@word")
        public HtmlResponse land(Integer first) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute(urlPattern = "{}/@word")
        public HtmlResponse piari(String first) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute(urlPattern = "{}/@word")
        public HtmlResponse bonvo(String first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    private ActionMapping prepareMapping(Class<?> componentClass) {
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

    private void assertExecute(ActionMapping mapping, String methodName, String paramPath) {
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
