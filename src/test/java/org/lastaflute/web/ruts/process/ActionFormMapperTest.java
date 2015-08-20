package org.lastaflute.web.ruts.process;

import org.lastaflute.unit.UnitLastaFluteTestCase;

/**
 * @author jflute
 */
public class ActionFormMapperTest extends UnitLastaFluteTestCase {

    // TODO jflute xxxxxx (2015/08/20)
    //    // ===================================================================================
    //    //                                                                       setProperty()
    //    //                                                                       =============
    //    public void test_setProperty_map_genericArray_array() throws Exception {
    //        // ## Arrange ##
    //        ActionFormMapper mapper = createMapper();
    //        SeaForm seaForm = new SeaForm();
    //        String[] value = new String[] { "a", "b" };
    //
    //        // ## Act ##
    //        mapper.setProperty(seaForm, "landMap.oneman", value, null, null);
    //
    //        // ## Assert ##
    //        Object actual = seaForm.landMap.get("oneman");
    //        assertSame(value, actual);
    //    }
    //
    //    // ===================================================================================
    //    //                                                                    setMapProperty()
    //    //                                                                    ================
    //    public void test_setMapProperty_genericArray_array() throws Exception {
    //        // ## Arrange ##
    //        ActionFormMapper mapper = createMapper();
    //        Map<String, Object> map = newLinkedHashMap();
    //        SeaForm seaForm = new SeaForm();
    //        String[] value = new String[] { "a", "b" };
    //
    //        // ## Act ##
    //        mapper.setMapProperty(map, "oneman", value, seaForm, "landMap");
    //
    //        // ## Assert ##
    //        Object actual = map.get("oneman");
    //        assertEquals(value, actual);
    //    }
    //
    //    public void test_setMapProperty_genericArray_nonArray() throws Exception {
    //        // ## Arrange ##
    //        ActionFormMapper mapper = createMapper();
    //        Map<String, Object> map = newLinkedHashMap();
    //        SeaForm seaForm = new SeaForm();
    //        String value = "a";
    //
    //        // ## Act ##
    //        mapper.setMapProperty(map, "oneman", value, seaForm, "landMap");
    //
    //        // ## Assert ##
    //        String[] actual = (String[]) map.get("oneman");
    //        assertEquals(value, actual[0]);
    //        assertEquals(1, actual.length);
    //    }
    //
    //    public void test_setMapProperty_genericScalar_array() throws Exception {
    //        // ## Arrange ##
    //        ActionFormMapper mapper = createMapper();
    //        Map<String, Object> map = newLinkedHashMap();
    //        SeaForm seaForm = new SeaForm();
    //        String[] value = new String[] { "a", "b" };
    //
    //        // ## Act ##
    //        mapper.setMapProperty(map, "dstore", value, seaForm, "iksMap");
    //
    //        // ## Assert ##
    //        Object actual = map.get("dstore");
    //        assertEquals("a", actual);
    //    }
    //
    //    public void test_setMapProperty_genericScalar_nonArray() throws Exception {
    //        // ## Arrange ##
    //        ActionFormMapper mapper = createMapper();
    //        Map<String, Object> map = newLinkedHashMap();
    //        SeaForm seaForm = new SeaForm();
    //        String value = "a";
    //
    //        // ## Act ##
    //        mapper.setMapProperty(map, "dstore", value, seaForm, "iksMap");
    //
    //        // ## Assert ##
    //        Object actual = map.get("dstore");
    //        assertEquals("a", actual);
    //    }
    //
    //    public static class SeaForm {
    //        public Map<String, String[]> landMap;
    //        public Map<String, String> iksMap;
    //    }
    //
    //    // ===================================================================================
    //    //                                                                         Test Helper
    //    //                                                                         ===========
    //    protected ActionFormMapper createMapper() {
    //        return new ActionFormMapper(null, null, null);
    //    }
}
