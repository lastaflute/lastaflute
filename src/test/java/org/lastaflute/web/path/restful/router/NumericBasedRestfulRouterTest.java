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
package org.lastaflute.web.path.restful.router;

import org.lastaflute.unit.UnitLastaFluteTestCase;
import org.lastaflute.web.RestfulAction;
import org.lastaflute.web.UrlChain;
import org.lastaflute.web.path.UrlMappingOption;
import org.lastaflute.web.path.UrlMappingResource;
import org.lastaflute.web.path.UrlReverseOption;
import org.lastaflute.web.path.UrlReverseResource;

/**
 * @author jflute
 */
public class NumericBasedRestfulRouterTest extends UnitLastaFluteTestCase {

    // ===================================================================================
    //                                                                        Mapping Path
    //                                                                        ============
    // -----------------------------------------------------
    //                                               Level 1
    //                                               -------
    public void test_toRestfulMappingPath_level1_noParam() {
        // ## Arrange ##
        String requestPath = "/products/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals(requestPath, filter.apply(requestPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    public void test_toRestfulMappingPath_level1_withParam() {
        // ## Arrange ##
        String requestPath = "/products/1/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals(requestPath, filter.apply(requestPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    // -----------------------------------------------------
    //                                               Level 2
    //                                               -------
    public void test_toRestfulMappingPath_level2_noParam_basic() {
        // ## Arrange ##
        String requestPath = "/products/1/purchases/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/products/purchases/1/", filter.apply(requestPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    public void test_toRestfulMappingPath_level2_noParam_suffix() {
        // ## Arrange ##
        String requestPath = "/products/1/purchases/price/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/products/purchases/price/1/", filter.apply(requestPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    public void test_toRestfulMappingPath_level2_withParam() {
        // ## Arrange ##
        String requestPath = "/products/1/purchases/2/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/products/purchases/1/2/", filter.apply(requestPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    // -----------------------------------------------------
    //                                             Hyphenate
    //                                             ---------
    public void test_toRestfulMappingPath_hyphenate_basic() {
        // ## Arrange ##
        String requestPath = "/ballet-dancers/1/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/ballet/dancers/1/", filter.apply(requestPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    public void test_toRestfulMappingPath_hyphenate_nested_noParam() {
        // ## Arrange ##
        String requestPath = "/ballet-dancers/1/favorite-studios/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/ballet/dancers/favorite/studios/1/", filter.apply(requestPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    public void test_toRestfulMappingPath_hyphenate_nested_withParam() {
        // ## Arrange ##
        String requestPath = "/ballet-dancers/1/favorite-studios/2/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/ballet/dancers/favorite/studios/1/2/", filter.apply(requestPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    // -----------------------------------------------------
    //                                          Top Category
    //                                          ------------
    public void test_toRestfulMappingPath_topCategory_level2_noParam_basic() {
        // ## Arrange ##
        String requestPath = "/mockhama/products/1/purchases/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath_withTopCategory(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            String filtered = filter.apply(requestPath);
            assertEquals("/mockhama/products/purchases/1/", filtered);
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    public void test_toRestfulMappingPath_topCategory_level2_noParam_suffix() {
        // ## Arrange ##
        String requestPath = "/mockhama/products/1/purchases/sea/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath_withTopCategory(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            String filtered = filter.apply(requestPath);
            assertEquals("/mockhama/products/purchases/sea/1/", filtered);
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    public void test_toRestfulMappingPath_topCategory_level2_withParam() {
        // ## Arrange ##
        String requestPath = "/mockhama/products/1/purchases/2";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath_withTopCategory(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            String filtered = filter.apply(requestPath);
            assertEquals("/mockhama/products/purchases/1/2/", filtered);
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    // -----------------------------------------------------
    //                                          Assist Logic
    //                                          ------------
    private UrlMappingOption toRestfulMappingPath(String requestPath) {
        NumericBasedRestfulRouter router = new NumericBasedRestfulRouter();
        UrlMappingResource resource = new UrlMappingResource(requestPath, requestPath);
        return router.toRestfulMappingPath(resource).get();
    }

    private UrlMappingOption toRestfulMappingPath_withTopCategory(String requestPath) {
        NumericBasedRestfulRouter router = new NumericBasedRestfulRouter() {
            @Override
            protected boolean isTopCategorizedPath(String requestPath) {
                return requestPath.startsWith("/mockhama/");
            }
        };
        UrlMappingResource resource = new UrlMappingResource(requestPath, requestPath);
        return router.toRestfulMappingPath(resource).get();
    }

    // ===================================================================================
    //                                                                        Reverse Path
    //                                                                        ============
    // -----------------------------------------------------
    //                                               Level 2
    //                                               -------
    public void test_toRestfulReversePath_level2_noParam_basic() {
        // ## Arrange ##
        // ## Act ##
        UrlReverseOption reverseOption = toRestfulReversePath(MocksRestfulsAction.class, new UrlChain("{}"));

        // ## Assert ##
        reverseOption.getActionUrlFilter().alwaysPresent(filter -> {
            assertEquals("/mocks/{}/restfuls/", filter.apply("/mocks/restfuls/{}/"));
        });
    }

    public void test_toRestfulReversePath_level2_noParam_suffix() {
        // ## Arrange ##
        // ## Act ##
        UrlReverseOption reverseOption = toRestfulReversePath(MocksRestfulsAction.class, new UrlChain("sea/{}"));

        // ## Assert ##
        reverseOption.getActionUrlFilter().alwaysPresent(filter -> {
            assertEquals("/mocks/{}/restfuls/sea/", filter.apply("/mocks/restfuls/sea/{}/"));
        });
    }

    public void test_toRestfulReversePath_level2_withParam_basic() {
        // ## Arrange ##
        // ## Act ##
        UrlReverseOption reverseOption = toRestfulReversePath(MocksRestfulsAction.class, new UrlChain("{}/{}/"));

        // ## Assert ##
        reverseOption.getActionUrlFilter().alwaysPresent(filter -> {
            assertEquals("/mocks/{}/restfuls/{}/", filter.apply("/mocks/restfuls/{}/{}/"));
        });
    }

    public void test_toRestfulReversePath_level2_withParam_suffix() {
        // ## Arrange ##
        // ## Act ##
        UrlReverseOption reverseOption = toRestfulReversePath(MocksRestfulsAction.class, new UrlChain("sea/{}/{}/"));

        // ## Assert ##
        reverseOption.getActionUrlFilter().alwaysPresent(filter -> {
            assertEquals("/mocks/{}/restfuls/{}/sea/", filter.apply("/mocks/restfuls/sea/{}/{}/"));
        });
    }

    // -----------------------------------------------------
    //                                             Hyphenate
    //                                             ---------
    public void test_toRestfulReversePath_hyphenate_basic() {
        // ## Arrange ##
        UrlChain urlChain = new UrlChain("mockballet/dancers/{}/");

        // ## Act ##
        UrlReverseOption reverseOption = toRestfulReversePath(MockballetDancersAction.class, urlChain);

        // ## Assert ##
        reverseOption.getActionUrlFilter().alwaysPresent(filter -> {
            assertEquals("/mockballet-dancers/{}/", filter.apply("/mockballet/dancers/{}/"));
        });
    }

    public void test_toRestfulReversePath_hyphenate_nested_basic() {
        // ## Arrange ##
        UrlChain urlChain = new UrlChain("mockballet/dancers/favorite/studios/{}/{}/");

        // ## Act ##
        UrlReverseOption reverseOption = toRestfulReversePath(MockballetDancersFavoriteStudiosAction.class, urlChain);

        // ## Assert ##
        reverseOption.getActionUrlFilter().alwaysPresent(filter -> {
            assertEquals("/mockballet-dancers/{}/favorite-studios/{}/", filter.apply("/mockballet/dancers/favorite/studios/{}/{}/"));
        });
    }

    public void test_toRestfulReversePath_hyphenate_nested_three() {
        // ## Arrange ##
        UrlChain urlChain = new UrlChain("mockballet/dancers/greatest/favorite/studios/{}/{}/");

        // ## Act ##
        UrlReverseOption reverseOption = toRestfulReversePath(MockballetDancersGreatestFavoriteStudiosAction.class, urlChain);

        // ## Assert ##
        reverseOption.getActionUrlFilter().alwaysPresent(filter -> {
            assertEquals("/mockballet-dancers/{}/greatest-favorite-studios/{}/",
                    filter.apply("/mockballet/dancers/greatest/favorite/studios/{}/{}/"));
        });
    }

    // -----------------------------------------------------
    //                                          Top Category
    //                                          ------------
    public void test_toRestfulReversePath_topCategory_level2_noParam_basic() {
        // ## Arrange ##
        // ## Act ##
        UrlReverseOption reverseOption = toRestfulReversePath_withTopCategory(MockhamaMocksRestfulsAction.class, new UrlChain("{}"));

        // ## Assert ##
        reverseOption.getActionUrlFilter().alwaysPresent(filter -> {
            assertEquals("/mockhama/mocks/{}/restfuls/", filter.apply("/mockhama/mocks/restfuls/{}/"));
        });
    }

    public void test_toRestfulReversePath_topCategory_level2_noParam_suffix() {
        // ## Arrange ##
        // ## Act ##
        UrlReverseOption reverseOption = toRestfulReversePath_withTopCategory(MockhamaMocksRestfulsAction.class, new UrlChain("{}"));

        // ## Assert ##
        reverseOption.getActionUrlFilter().alwaysPresent(filter -> {
            assertEquals("/mockhama/mocks/{}/restfuls/sea/", filter.apply("/mockhama/mocks/restfuls/sea/{}/"));
        });
    }

    public void test_toRestfulReversePath_topCategory_level2_withParam_basic() {
        // ## Arrange ##
        // ## Act ##
        UrlReverseOption reverseOption = toRestfulReversePath_withTopCategory(MockhamaMocksRestfulsAction.class, new UrlChain("{}"));

        // ## Assert ##
        reverseOption.getActionUrlFilter().alwaysPresent(filter -> {
            assertEquals("/mockhama/mocks/{}/restfuls/{}/", filter.apply("/mockhama/mocks/restfuls/{}/{}/"));
        });
    }

    // -----------------------------------------------------
    //                                          Assist Logic
    //                                          ------------
    private UrlReverseOption toRestfulReversePath(Class<?> actionType, UrlChain urlChain) {
        NumericBasedRestfulRouter router = new NumericBasedRestfulRouter();
        UrlReverseResource resource = new UrlReverseResource(actionType, urlChain);
        return router.toRestfulReversePath(resource).get();
    }

    private UrlReverseOption toRestfulReversePath_withTopCategory(Class<?> actionType, UrlChain urlChain) {
        NumericBasedRestfulRouter router = new NumericBasedRestfulRouter() {
            @Override
            protected boolean isTopCategorizedPath(String requestPath) {
                return requestPath.startsWith("/mockhama/");
            }
        };
        UrlReverseResource resource = new UrlReverseResource(actionType, urlChain);
        return router.toRestfulReversePath(resource).get();
    }

    @RestfulAction
    private static class MocksRestfulsAction {
    }

    @RestfulAction(hyphenate = "mockballet-dancers")
    private static class MockballetDancersAction {
    }

    @RestfulAction(hyphenate = { "mockballet-dancers", "favorite-studios" })
    private static class MockballetDancersFavoriteStudiosAction {
    }

    @RestfulAction(hyphenate = { "mockballet-dancers", "greatest-favorite-studios" })
    private static class MockballetDancersGreatestFavoriteStudiosAction {
    }

    @RestfulAction
    private static class MockhamaMocksRestfulsAction {
    }
}