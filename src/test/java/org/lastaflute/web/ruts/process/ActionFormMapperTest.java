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
package org.lastaflute.web.ruts.process;

import java.util.Map;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.unit.UnitLastaFluteTestCase;
import org.lastaflute.unit.mock.web.MockRequestManager;
import org.lastaflute.web.path.FormMappingOption;
import org.lastaflute.web.ruts.VirtualForm;
import org.lastaflute.web.ruts.config.ActionFormMeta;
import org.lastaflute.web.ruts.process.formcoins.FormCoinsHelper;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author jflute
 */
public class ActionFormMapperTest extends UnitLastaFluteTestCase {

    // ===================================================================================
    //                                                                           Multipart
    //                                                                           =========
    public void test_multipart_different_contentType() throws Exception {
        // ## Arrange ##
        ActionFormMapper mapper = createRequestedMapper(new MockRequestManager() {
            public OptionalThing<String> getContentType() {
                return OptionalThing.of("text/html");
            }

            public OptionalThing<String> getHttpMethod() {
                return OptionalThing.of("post");
            }

            public boolean isHttpMethodPost() {
                return true;
            }
        });

        // ## Act ##
        boolean result = mapper.determineMultipartRequest();

        // ## Assert ##
        assertFalse(result);
    }

    public void test_multipart_different_httpMethod() throws Exception {
        // ## Arrange ##
        ActionFormMapper mapper = createRequestedMapper(new MockRequestManager() {
            public OptionalThing<String> getContentType() {
                return OptionalThing.of(ActionFormMapper.MULTIPART_CONTENT_TYPE);
            }

            public OptionalThing<String> getHttpMethod() {
                return OptionalThing.of("put");
            }

            public boolean isHttpMethodPost() {
                return false;
            }
        });

        // ## Act ##
        boolean result = mapper.determineMultipartRequest();

        // ## Assert ##
        assertFalse(result); // and watch debug log by visual check
    }

    public void test_multipart_target_httpMethod() throws Exception {
        // ## Arrange ##
        ActionFormMapper mapper = createRequestedMapper(new MockRequestManager() {
            public OptionalThing<String> getContentType() {
                return OptionalThing.of(ActionFormMapper.MULTIPART_CONTENT_TYPE);
            }

            public OptionalThing<String> getHttpMethod() {
                return OptionalThing.of("post");
            }

            public boolean isHttpMethodPost() {
                return true;
            }
        });

        // ## Act ##
        boolean result = mapper.determineMultipartRequest();

        // ## Assert ##
        assertTrue(result);
    }

    // ===================================================================================
    //                                                                       setProperty()
    //                                                                       =============
    public void test_setProperty_map_genericArray_array() throws Exception {
        // ## Arrange ##
        ActionFormMapper mapper = createMapper();
        SeaForm seaForm = new SeaForm();
        String[] value = new String[] { "a", "b" };

        // ## Act ##
        mapper.setProperty(seaForm, "landMap.oneman", value, null, null, null);

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
        mapper.setMapProperty(map, "oneman", value, seaForm, "landMap");

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
        mapper.setMapProperty(map, "oneman", value, seaForm, "landMap");

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
        mapper.setMapProperty(map, "dstore", value, seaForm, "iksMap");

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
        mapper.setMapProperty(map, "dstore", value, seaForm, "iksMap");

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
    protected ActionFormMapper createMapper() { // simple mock
        VirtualForm virtualForm = new VirtualForm(() -> "", (ActionFormMeta) null); // dummy
        return new ActionFormMapper(null, null, null, virtualForm) { // to avoid required components

            protected FormMappingOption adjustFormMapping() {
                return new FormMappingOption();
            };

            protected FormCoinsHelper createFormCoinsHelper() {
                return new FormCoinsHelper(null, requestManager);
            };
        };
    }

    protected ActionFormMapper createRequestedMapper(RequestManager requestManager) {
        VirtualForm virtualForm = new VirtualForm(() -> "", (ActionFormMeta) null); // dummy
        return new ActionFormMapper(null, requestManager, null, virtualForm) { // to avoid required components

            protected FormMappingOption adjustFormMapping() {
                return new FormMappingOption();
            };

            protected FormCoinsHelper createFormCoinsHelper() {
                return new FormCoinsHelper(null, requestManager);
            };
        };
    }
}
