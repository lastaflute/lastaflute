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

import org.dbflute.optional.OptionalThing;
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
public class NumericBasedRestfulRouterZoneStringIdTest extends UnitLastaFluteTestCase {

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
        String requestPath = "/products/sea:mystic/";

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
        String requestPath = "/products/sea:mystic/purchases/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/products/purchases/sea:mystic/", filter.apply(requestPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    public void test_toRestfulMappingPath_level2_noParam_suffix() {
        // ## Arrange ##
        String requestPath = "/products/sea:mystic/purchases/price/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/products/purchases/price/sea:mystic/", filter.apply(requestPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    public void test_toRestfulMappingPath_level2_withParam() {
        // ## Arrange ##
        String requestPath = "/products/sea:mystic/purchases/sea:rhythms/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/products/purchases/sea:mystic/sea:rhythms/", filter.apply(requestPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    // -----------------------------------------------------
    //                                             Hyphenate
    //                                             ---------
    public void test_toRestfulMappingPath_hyphenate_basic() {
        // ## Arrange ##
        String requestPath = "/ballet-dancers/sea:mystic/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/ballet/dancers/sea:mystic/", filter.apply(requestPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    public void test_toRestfulMappingPath_hyphenate_oneCharacter() {
        // ## Arrange ##
        String requestPath = "/a-dancers/sea:mystic/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/a/dancers/sea:mystic/", filter.apply(requestPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    public void test_toRestfulMappingPath_hyphenate_nested_noParam() {
        // ## Arrange ##
        String requestPath = "/ballet-dancers/sea:mystic/favorite-studios/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/ballet/dancers/favorite/studios/sea:mystic/", filter.apply(requestPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    public void test_toRestfulMappingPath_hyphenate_nested_withParam_basic() {
        // ## Arrange ##
        String requestPath = "/ballet-dancers/sea:mystic/favorite-studios/sea:rhythms/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/ballet/dancers/favorite/studios/sea:mystic/sea:rhythms/", filter.apply(requestPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    public void test_toRestfulMappingPath_hyphenate_nested_withParam_reappeared_basic() {
        // ## Arrange ##
        String requestPath = "/ballet-dancers/sea:mystic/favorite-ballet-dancers/sea:rhythms/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/ballet/dancers/favorite/ballet/dancers/sea:mystic/sea:rhythms/", filter.apply(requestPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    public void test_toRestfulMappingPath_hyphenate_nested_withParam_reappeared_complex() {
        // ## Arrange ##
        String requestPath = "/ballet-dancers/1/greatest/sea:mystic/favorite-ballet-dancers/sea:rhythms/studios/4/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/ballet/dancers/greatest/favorite/ballet/dancers/studios/1/sea:mystic/sea:rhythms/4/", filter.apply(requestPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    // -----------------------------------------------------
    //                                            URL Prefix
    //                                            ----------
    public void test_toRestfulMappingPath_urlPrefix_basic() {
        // ## Arrange ##
        String pureRequestPath = "/api/products/";
        String workingMappingPath = "/products/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath_filtering(pureRequestPath, workingMappingPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals(workingMappingPath, filter.apply(workingMappingPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    public void test_toRestfulMappingPath_urlPrefix_nested() {
        // ## Arrange ##
        String pureRequestPath = "/api/products/sea:mystic/purchases/";
        String workingMappingPath = "/products/sea:mystic/purchases/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath_filtering(pureRequestPath, workingMappingPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/products/purchases/sea:mystic/", filter.apply(workingMappingPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    // -----------------------------------------------------
    //                                           Non Restful
    //                                           -----------
    public void test_toRestfulMappingPath_nonRestful_basic() {
        assertFalse(toRestfulMappingPath_option("/").isPresent());
        assertTrue(toRestfulMappingPath_option("/sea/").isPresent());
        assertTrue(toRestfulMappingPath_option("/sea/land/").isPresent());
        assertFalse(toRestfulMappingPath_option("/sea/land/piari/").isPresent());
        assertFalse(toRestfulMappingPath_option("/1/").isPresent());
        assertFalse(toRestfulMappingPath_option("/1/sea/").isPresent());
        assertFalse(toRestfulMappingPath_option("/sea/1/2/").isPresent());
        assertFalse(toRestfulMappingPath_option("/sea/1/2/land/").isPresent());
        assertTrue(toRestfulMappingPath_option("/sea/1/land/piari/").isPresent());
        assertTrue(toRestfulMappingPath_option("/sea/1/land/piari/dstore/").isPresent()); // cannot detect
    }

    // -----------------------------------------------------
    //                                          Assist Logic
    //                                          ------------
    private UrlMappingOption toRestfulMappingPath(String requestPath) {
        NumericBasedRestfulRouter router = newNumericBasedRestfulRouter();
        UrlMappingResource resource = new UrlMappingResource(requestPath, requestPath);
        return router.toRestfulMappingPath(resource).get();
    }

    private UrlMappingOption toRestfulMappingPath_filtering(String pureRequestPath, String makingMappingPath) {
        NumericBasedRestfulRouter router = newNumericBasedRestfulRouter();
        UrlMappingResource resource = new UrlMappingResource(pureRequestPath, makingMappingPath);
        return router.toRestfulMappingPath(resource).get();
    }

    private OptionalThing<UrlMappingOption> toRestfulMappingPath_option(String requestPath) {
        NumericBasedRestfulRouter router = newNumericBasedRestfulRouter();
        UrlMappingResource resource = new UrlMappingResource(requestPath, requestPath);
        return router.toRestfulMappingPath(resource);
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
        UrlChain urlChain = new UrlChain("{}");

        // ## Act ##
        UrlReverseOption reverseOption = toRestfulReversePath(MockballetDancersAction.class, urlChain);

        // ## Assert ##
        reverseOption.getActionUrlFilter().alwaysPresent(filter -> {
            assertEquals("/mockballet-dancers/{}/", filter.apply("/mockballet/dancers/{}/"));
        });
    }

    public void test_toRestfulReversePath_hyphenate_oneCharacter() {
        // ## Arrange ##
        UrlChain urlChain = new UrlChain("{}/{}");

        // ## Act ##
        UrlReverseOption reverseOption = toRestfulReversePath(MockballetADancersAction.class, urlChain);

        // ## Assert ##
        reverseOption.getActionUrlFilter().alwaysPresent(filter -> {
            assertEquals("/mockballet/{}/a-dancers/{}/", filter.apply("/mockballet/a/dancers/{}/{}/"));
        });
    }

    public void test_toRestfulReversePath_hyphenate_nested_basic() {
        // ## Arrange ##
        UrlChain urlChain = new UrlChain("{}/{}");

        // ## Act ##
        UrlReverseOption reverseOption = toRestfulReversePath(MockballetDancersFavoriteStudiosAction.class, urlChain);

        // ## Assert ##
        reverseOption.getActionUrlFilter().alwaysPresent(filter -> {
            assertEquals("/mockballet-dancers/{}/favorite-studios/{}/", filter.apply("/mockballet/dancers/favorite/studios/{}/{}/"));
        });
    }

    public void test_toRestfulReversePath_hyphenate_nested_threeElements() {
        // ## Arrange ##
        UrlChain urlChain = new UrlChain("{}/{}");

        // ## Act ##
        UrlReverseOption reverseOption = toRestfulReversePath(MockballetDancersGreatestFavoriteStudiosAction.class, urlChain);

        // ## Assert ##
        reverseOption.getActionUrlFilter().alwaysPresent(filter -> {
            assertEquals("/mockballet-dancers/{}/greatest-favorite-studios/{}/",
                    filter.apply("/mockballet/dancers/greatest/favorite/studios/{}/{}/"));
        });
    }

    public void test_toRestfulReversePath_hyphenate_nested_threeResources() {
        // ## Arrange ##
        UrlChain urlChain = new UrlChain("{}/{}/{}");

        // ## Act ##
        UrlReverseOption reverseOption = toRestfulReversePath(MockballetDancersGreatestFavoriteSuperStudiosAction.class, urlChain);

        // ## Assert ##
        reverseOption.getActionUrlFilter().alwaysPresent(filter -> {
            assertEquals("/mockballet-dancers/{}/greatest-favorite/{}/super-studios/{}/",
                    filter.apply("/mockballet/dancers/greatest/favorite/super/studios/{}/{}/{}/"));
        });
    }

    // #thinking jflute making now (2021/06/20)
    public void test_toRestfulReversePath_hyphenate_nested_reappeared_basic() {
        // ## Arrange ##
        UrlChain urlChain = new UrlChain("{}/{}");

        // ## Act ##
        UrlReverseOption reverseOption = toRestfulReversePath(MockballetDancersFavoriteMockballetDancersAction.class, urlChain);

        // ## Assert ##
        reverseOption.getActionUrlFilter().alwaysPresent(filter -> {
            assertEquals("/mockballet-dancers/{}/favorite-mockballet-dancers/{}/",
                    filter.apply("/mockballet/dancers/favorite/mockballet/dancers/{}/{}/"));
        });
    }

    public void test_toRestfulReversePath_hyphenate_nested_reappeared_complex() {
        // ## Arrange ##
        UrlChain urlChain = new UrlChain("{}/{}/{}/{}");

        // ## Act ##
        UrlReverseOption reverseOption =
                toRestfulReversePath(MockballetDancersGreatestFavoriteMockballetDancersStudiosAction.class, urlChain);

        // ## Assert ##
        reverseOption.getActionUrlFilter().alwaysPresent(filter -> {
            assertEquals("/mockballet-dancers/{}/greatest/{}/favorite-mockballet-dancers/{}/studios/{}/",
                    filter.apply("/mockballet/dancers/greatest/favorite/mockballet/dancers/studios/{}/{}/{}/{}/"));
        });
    }

    public void test_toRestfulReversePath_hyphenate_nested_reappeared_reversed() {
        // ## Arrange ##
        UrlChain urlChain = new UrlChain("{}/{}/{}/{}");

        // ## Act ##
        UrlReverseOption reverseOption =
                toRestfulReversePath(MockballetDancersGreatestFavoriteMockballetDancersStudiosReversedAction.class, urlChain);

        // ## Assert ##
        reverseOption.getActionUrlFilter().alwaysPresent(filter -> {
            assertEquals("/mockballet-dancers/{}/greatest/{}/favorite-mockballet-dancers/{}/studios/{}/",
                    filter.apply("/mockballet/dancers/greatest/favorite/mockballet/dancers/studios/{}/{}/{}/{}/"));
        });
    }

    // -----------------------------------------------------
    //                                          Assist Logic
    //                                          ------------
    private UrlReverseOption toRestfulReversePath(Class<?> actionType, UrlChain urlChain) {
        NumericBasedRestfulRouter router = newNumericBasedRestfulRouter();
        UrlReverseResource resource = new UrlReverseResource(actionType, urlChain);
        return router.toRestfulReversePath(resource).get();
    }

    // -----------------------------------------------------
    //                                           Mock Action
    //                                           -----------
    @RestfulAction
    private static class MocksRestfulsAction { // simple
    }

    @RestfulAction(hyphenate = "mockballet-dancers")
    private static class MockballetDancersAction { // simple hyphenate
    }

    @RestfulAction(hyphenate = "a-dancers")
    private static class MockballetADancersAction { // one character hyphenate
    }

    @RestfulAction(hyphenate = { "mockballet-dancers", "favorite-studios" })
    private static class MockballetDancersFavoriteStudiosAction { // multiple hyphenate
    }

    @RestfulAction(hyphenate = { "mockballet-dancers", "greatest-favorite-studios" })
    private static class MockballetDancersGreatestFavoriteStudiosAction { // multiple hyphenate
    }

    @RestfulAction(hyphenate = { "mockballet-dancers", "greatest-favorite", "super-studios" })
    private static class MockballetDancersGreatestFavoriteSuperStudiosAction { // many hyphenate
    }

    @RestfulAction(hyphenate = { "mockballet-dancers", "favorite-mockballet-dancers" })
    private static class MockballetDancersFavoriteMockballetDancersAction { // hyphenate reappeared
    }

    @RestfulAction(hyphenate = { "mockballet-dancers", "favorite-mockballet-dancers" })
    private static class MockballetDancersGreatestFavoriteMockballetDancersStudiosAction { // hyphenate reappeared
    }

    @RestfulAction(hyphenate = { "favorite-mockballet-dancers", "mockballet-dancers" })
    private static class MockballetDancersGreatestFavoriteMockballetDancersStudiosReversedAction { // hyphenate reappeared
    }

    // ===================================================================================
    //                                                                     Target Instance
    //                                                                     ===============
    private NumericBasedRestfulRouter newNumericBasedRestfulRouter() {
        return new NumericBasedRestfulRouter() {
            @Override
            protected boolean isIdElement(String element) {
                return super.isIdElement(element) || element.startsWith("sea:");
            }
        };
    }
}
