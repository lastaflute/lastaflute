package org.lastaflute.web.ruts.process;

import java.util.Map;

import org.lastaflute.unit.UnitLastaFluteTestCase;
import org.lastaflute.web.path.FormMappingOption;
import org.lastaflute.web.ruts.VirtualForm;
import org.lastaflute.web.ruts.config.ActionFormMeta;

/**
 * @author jflute
 */
public class ActionFormMapperTest extends UnitLastaFluteTestCase {

    // ===================================================================================
    //                                                                       setProperty()
    //                                                                       =============
    public void test_setProperty_map_genericArray_array() throws Exception {
        // ## Arrange ##
        ActionFormMapper mapper = createMapper();
        VirtualForm virtualForm = new VirtualForm(() -> "", (ActionFormMeta) null); // dummy
        SeaForm seaForm = new SeaForm();
        String[] value = new String[] { "a", "b" };

        // ## Act ##
        mapper.setProperty(null, virtualForm, seaForm, "landMap.oneman", value, null, new FormMappingOption(), null, null);

        // ## Assert ##
        Object actual = seaForm.landMap.get("oneman");
        assertSame(value, actual);
    }

    // ===================================================================================
    //                                                                    setMapProperty()
    //                                                                    ================
    public void test_setMapProperty_genericArray_array() throws Exception {
        // ## Arrange ##
        ActionFormMapper mapper = createMapper();
        Map<String, Object> map = newLinkedHashMap();
        SeaForm seaForm = new SeaForm();
        String[] value = new String[] { "a", "b" };

        // ## Act ##
        mapper.setMapProperty(map, "oneman", value, new FormMappingOption(), seaForm, "landMap");

        // ## Assert ##
        Object actual = map.get("oneman");
        assertEquals(value, actual);
    }

    public void test_setMapProperty_genericArray_nonArray() throws Exception {
        // ## Arrange ##
        ActionFormMapper mapper = createMapper();
        Map<String, Object> map = newLinkedHashMap();
        SeaForm seaForm = new SeaForm();
        String value = "a";

        // ## Act ##
        mapper.setMapProperty(map, "oneman", value, new FormMappingOption(), seaForm, "landMap");

        // ## Assert ##
        String[] actual = (String[]) map.get("oneman");
        assertEquals(value, actual[0]);
        assertEquals(1, actual.length);
    }

    public void test_setMapProperty_genericScalar_array() throws Exception {
        // ## Arrange ##
        ActionFormMapper mapper = createMapper();
        Map<String, Object> map = newLinkedHashMap();
        SeaForm seaForm = new SeaForm();
        String[] value = new String[] { "a", "b" };

        // ## Act ##
        mapper.setMapProperty(map, "dstore", value, new FormMappingOption(), seaForm, "iksMap");

        // ## Assert ##
        Object actual = map.get("dstore");
        assertEquals("a", actual);
    }

    public void test_setMapProperty_genericScalar_nonArray() throws Exception {
        // ## Arrange ##
        ActionFormMapper mapper = createMapper();
        Map<String, Object> map = newLinkedHashMap();
        SeaForm seaForm = new SeaForm();
        String value = "a";

        // ## Act ##
        mapper.setMapProperty(map, "dstore", value, new FormMappingOption(), seaForm, "iksMap");

        // ## Assert ##
        Object actual = map.get("dstore");
        assertEquals("a", actual);
    }

    public static class SeaForm {
        public Map<String, String[]> landMap;
        public Map<String, String> iksMap;
    }

    // ===================================================================================
    //                                                                         Test Helper
    //                                                                         ===========
    protected ActionFormMapper createMapper() {
        return new ActionFormMapper(null, null, null);
    }
}
