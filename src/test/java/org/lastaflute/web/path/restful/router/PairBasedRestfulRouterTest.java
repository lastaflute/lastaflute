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
 * @since 1.2.1 (2021/07/20 Tuesday at roppongi japanese)
 */
public class PairBasedRestfulRouterTest extends UnitLastaFluteTestCase {

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
        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // #pair_based the price is treated as path parameter
        // so filtered to "/products/purchases/1/price/", which cannot be mapped
        // _/_/_/_/_/_/_/_/_/_/
        String requestPath = "/products/1/purchases/price/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/products/purchases/1/price/", filter.apply(requestPath));
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

    public void test_toRestfulMappingPath_hyphenate_oneCharacter() {
        // ## Arrange ##
        String requestPath = "/a-dancers/1/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/a/dancers/1/", filter.apply(requestPath));
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

    public void test_toRestfulMappingPath_hyphenate_nested_withParam_basic() {
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

    public void test_toRestfulMappingPath_hyphenate_nested_withParam_reappeared_basic() {
        // ## Arrange ##
        String requestPath = "/ballet-dancers/1/favorite-ballet-dancers/2/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/ballet/dancers/favorite/ballet/dancers/1/2/", filter.apply(requestPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
    }

    public void test_toRestfulMappingPath_hyphenate_nested_withParam_reappeared_complex() {
        // ## Arrange ##
        String requestPath = "/ballet-dancers/1/greatest/2/favorite-ballet-dancers/3/studios/4/";

        // ## Act ##
        UrlMappingOption mappingOption = toRestfulMappingPath(requestPath);

        // ## Assert ##
        mappingOption.getRequestPathFilter().alwaysPresent(filter -> {
            assertEquals("/ballet/dancers/greatest/favorite/ballet/dancers/studios/1/2/3/4/", filter.apply(requestPath));
        });
        assertTrue(mappingOption.isRestfulMapping());
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
    private UrlMappingOption toRestfulMappingPath(String requestPath) {
        PairBasedRestfulRouter router = new PairBasedRestfulRouter();
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
    //                                          Assist Logic
    //                                          ------------
    private UrlReverseOption toRestfulReversePath(Class<?> actionType, UrlChain urlChain) {
        NumericBasedRestfulRouter router = new NumericBasedRestfulRouter();
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
}
