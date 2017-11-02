package org.lastaflute.web.ruts.config;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.unit.UnitLastaFluteTestCase;
import org.lastaflute.unit.mock.db.MockOldCDef;
import org.lastaflute.web.Execute;
import org.lastaflute.web.LastaAction;
import org.lastaflute.web.response.HtmlResponse;

/**
 * @author jflute
 * @since 1.0.1 (2017/10/16 Monday at bay maihama)
 */
public class ActionMappingNearpathBywordTest extends UnitLastaFluteTestCase {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final String index = "index";
    private static final String named = "named";
    private static final String nonno = "$notFound";

    // ===================================================================================
    //                                                                               Basic
    //                                                                               =====
    public void test_paramPath_byword01_string_to_number() {
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
        assertExecute(mapping, nonno, "-1/land");
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
    //                                                                      First Optional
    //                                                                      ==============
    public void test_paramPath_byword05_optional_string_to_number() {
        ActionMapping mapping = prepareMapping(NearpathByword05Optstr2numAction.class);
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

    private static class NearpathByword05Optstr2numAction extends LastaAction {

        @Execute
        public HtmlResponse index(OptionalThing<String> first) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute(urlPattern = "{}/@word")
        public HtmlResponse named(Integer first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    // ===================================================================================
    //                                                                     Second Optional
    //                                                                     ===============
    public void test_paramPath_byword10_optional_string_for_string_to_number() {
        ActionMapping mapping = prepareMapping(NearpathByword10Optstr4str2numAction.class);
        assertExecute(mapping, index, "sea");
        assertExecute(mapping, index, "sea/named");
        assertExecute(mapping, index, "sea/land");
        assertExecute(mapping, nonno, "sea/land/piari");
        assertExecute(mapping, index, "sea/1"); // #hope nonno
        assertExecute(mapping, index, "sea/-1"); // #hope nonno

        assertExecute(mapping, index, "1");
        assertExecute(mapping, index, "1/named"); // hide
        assertExecute(mapping, index, "1/land");

        assertExecute(mapping, index, "-1");
        assertExecute(mapping, index, "-1/named"); // hide
        assertExecute(mapping, index, "-1/land");
    }

    private static class NearpathByword10Optstr4str2numAction extends LastaAction {

        @Execute
        public HtmlResponse index(String first, OptionalThing<String> second) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute(urlPattern = "{}/@word")
        public HtmlResponse named(Integer first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    public void test_paramPath_byword11_optional_string_for_number_to_string() {
        ActionMapping mapping = prepareMapping(NearpathByword11Optstr4num2strAction.class);
        assertExecute(mapping, nonno, "sea"); // index before
        assertExecute(mapping, named, "sea/named"); // index before
        assertExecute(mapping, nonno, "sea/land"); // index before
        assertExecute(mapping, nonno, "sea/land/piari");
        assertExecute(mapping, nonno, "sea/1"); // index before
        assertExecute(mapping, nonno, "sea/-1"); // index before

        assertExecute(mapping, index, "1");
        assertExecute(mapping, index, "1/named"); // hide
        assertExecute(mapping, nonno, "1/named/piari");
        assertExecute(mapping, index, "1/land");

        assertExecute(mapping, index, "-1");
        assertExecute(mapping, index, "-1/named"); // hide
        assertExecute(mapping, nonno, "-1/named/piari");
        assertExecute(mapping, index, "-1/land");
    }

    private static class NearpathByword11Optstr4num2strAction extends LastaAction {

        @Execute
        public HtmlResponse index(Integer first, OptionalThing<String> second) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute(urlPattern = "{}/@word")
        public HtmlResponse named(String first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    public void test_paramPath_byword12_optional_string_on_cls_to_string() {
        ActionMapping mapping = prepareMapping(NearpathByword12Optstr4cls2strAction.class);
        assertExecute(mapping, nonno, "sea"); // index before
        assertExecute(mapping, named, "sea/named"); // index before
        assertExecute(mapping, nonno, "sea/land"); // index before
        assertExecute(mapping, nonno, "sea/land/piari");

        assertExecute(mapping, index, "FML");
        assertExecute(mapping, index, "FML/named");
        assertExecute(mapping, index, "FML/land");
        assertExecute(mapping, nonno, "FML/land/piari");
        assertExecute(mapping, index, "FML/1");
        assertExecute(mapping, index, "FML/-1");

        assertExecute(mapping, nonno, "1"); // index before
        assertExecute(mapping, named, "1/named"); // index before
        assertExecute(mapping, nonno, "1/named/piari");
        assertExecute(mapping, nonno, "1/land"); // index before

        assertExecute(mapping, nonno, "-1"); // index before
        assertExecute(mapping, named, "-1/named"); // index before
        assertExecute(mapping, nonno, "-1/named/piari");
        assertExecute(mapping, nonno, "-1/land"); // index before
    }

    private static class NearpathByword12Optstr4cls2strAction extends LastaAction {

        @Execute
        public HtmlResponse index(MockOldCDef.MemberStatus first, OptionalThing<String> second) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute(urlPattern = "{}/@word")
        public HtmlResponse named(String first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    public void test_paramPath_byword13_optional_string_on_cls_to_number() {
        ActionMapping mapping = prepareMapping(NearpathByword13Optstr4cls2numAction.class);
        assertExecute(mapping, nonno, "sea"); // index before
        assertExecute(mapping, nonno, "sea/named"); // index before
        assertExecute(mapping, nonno, "sea/land"); // index before
        assertExecute(mapping, nonno, "sea/land/piari");

        assertExecute(mapping, index, "FML");
        assertExecute(mapping, index, "FML/named");
        assertExecute(mapping, index, "FML/land");
        assertExecute(mapping, nonno, "FML/land/piari");
        assertExecute(mapping, index, "FML/1");
        assertExecute(mapping, index, "FML/-1");

        assertExecute(mapping, nonno, "1"); // index before
        assertExecute(mapping, named, "1/named"); // index before
        assertExecute(mapping, nonno, "1/named/piari");
        assertExecute(mapping, nonno, "1/land"); // index before

        assertExecute(mapping, nonno, "-1"); // index before
        assertExecute(mapping, named, "-1/named"); // index before
        assertExecute(mapping, nonno, "-1/named/piari");
        assertExecute(mapping, nonno, "-1/land"); // index before
    }

    private static class NearpathByword13Optstr4cls2numAction extends LastaAction {

        @Execute
        public HtmlResponse index(MockOldCDef.MemberStatus first, OptionalThing<String> second) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute(urlPattern = "{}/@word")
        public HtmlResponse named(Integer first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    public void test_paramPath_byword15_optional_number_for_string_to_number() {
        ActionMapping mapping = prepareMapping(NearpathByword15Optnum4str2numAction.class);
        assertExecute(mapping, index, "sea");
        assertExecute(mapping, nonno, "sea/named"); // index before
        assertExecute(mapping, nonno, "sea/land"); // index before
        assertExecute(mapping, nonno, "sea/land/piari");
        assertExecute(mapping, index, "sea/1");
        assertExecute(mapping, index, "sea/-1");

        assertExecute(mapping, index, "1");
        assertExecute(mapping, named, "1/named"); // index before
        assertExecute(mapping, nonno, "1/land"); // index before

        assertExecute(mapping, index, "-1");
        assertExecute(mapping, named, "-1/named"); // index before
        assertExecute(mapping, nonno, "-1/land"); // index before
    }

    private static class NearpathByword15Optnum4str2numAction extends LastaAction {

        @Execute
        public HtmlResponse index(String first, OptionalThing<Integer> second) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute(urlPattern = "{}/@word")
        public HtmlResponse named(Integer first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    public void test_paramPath_byword16_optional_number_for_number_to_string() {
        ActionMapping mapping = prepareMapping(NearpathByword16Optnum4num2strAction.class);
        assertExecute(mapping, nonno, "sea"); // index before
        assertExecute(mapping, named, "sea/named"); // index before
        assertExecute(mapping, nonno, "sea/land"); // index before
        assertExecute(mapping, nonno, "sea/land/piari");
        assertExecute(mapping, nonno, "sea/1"); // index before
        assertExecute(mapping, nonno, "sea/-1"); // index before

        assertExecute(mapping, index, "1");
        assertExecute(mapping, named, "1/named"); // index before
        assertExecute(mapping, nonno, "1/land"); // index before

        assertExecute(mapping, index, "-1");
        assertExecute(mapping, named, "-1/named"); // index before
        assertExecute(mapping, nonno, "-1/land"); // index before
    }

    private static class NearpathByword16Optnum4num2strAction extends LastaAction {

        @Execute
        public HtmlResponse index(Integer first, OptionalThing<Integer> second) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute(urlPattern = "{}/@word")
        public HtmlResponse named(String first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    public void test_paramPath_byword19_optional_string_for_optional_to_string() {
        ActionMapping mapping = prepareMapping(NearpathByword19Optstr4optstr2strAction.class);
        assertExecute(mapping, index, "sea");
        assertExecute(mapping, index, "sea/named"); // hide, should be named? however compatible...
        assertExecute(mapping, index, "sea/land");
        assertExecute(mapping, nonno, "sea/land/piari");
        assertExecute(mapping, index, "sea/1");
        assertExecute(mapping, index, "sea/-1");

        assertExecute(mapping, index, "1");
        assertExecute(mapping, index, "1/named"); // hide, should be named? however compatible...
        assertExecute(mapping, index, "1/land");

        assertExecute(mapping, index, "-1");
        assertExecute(mapping, index, "-1/named"); // hide, should be named? however compatible...
        assertExecute(mapping, index, "-1/land");
    }

    private static class NearpathByword19Optstr4optstr2strAction extends LastaAction {

        @Execute
        public HtmlResponse index(OptionalThing<String> first, OptionalThing<String> second) {
            return HtmlResponse.asEmptyBody();
        }

        @Execute(urlPattern = "{}/@word")
        public HtmlResponse named(String first) {
            return HtmlResponse.asEmptyBody();
        }
    }

    // ===================================================================================
    //                                                                               Theme
    //                                                                               =====
    public void test_paramPath_byword20_named_parade() {
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
        return ActionMappingBasicTest.prepareMapping(componentClass);
    }

    private void assertExecute(ActionMapping mapping, String methodName, String paramPath) {
        ActionMappingBasicTest.assertExecute(mapping, methodName, paramPath);
    }
}
