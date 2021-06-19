package org.lastaflute.web.path.restful.analyzer;

import java.util.Arrays;

import org.dbflute.utflute.core.PlainTestCase;
import org.lastaflute.web.RestfulAction;

/**
 * @author jflute
 * @since 1.2.1 (2021/06/19 Saturday at roppongi japanese)
 */
public class RestfulComponentAnalyzerTest extends PlainTestCase {

    public void test_deriveResourceNameList_basic() {
        // ## Arrange ##
        RestfulComponentAnalyzer analyzer = new RestfulComponentAnalyzer();

        // ## Act ##
        // ## Assert ##
        assertEquals(Arrays.asList("mock", "ballet-dancers"), analyzer.deriveResourceNameListByActionType(MockBalletDancersAction.class));
        assertEquals(Arrays.asList("mock", "ballet-dancers", "greatest-favorite-studios"),
                analyzer.deriveResourceNameListByActionType(MockBalletDancersGreatestFavoriteStudiosAction.class));
        assertEquals(Arrays.asList("mock", "ballet-dancers", "greatest", "dancers-greatest"),
                analyzer.deriveResourceNameListByActionType(MockBalletDancersGreatestDancersGreatestAction.class));
        assertEquals(Arrays.asList("mock", "ballet-dancers", "greatest", "ballet-dancers", "studios"),
                analyzer.deriveResourceNameListByActionType(MockBalletDancersGreatestBalletDancersStudiosAction.class));
    }

    @RestfulAction(hyphenate = "ballet-dancers")
    private static class MockBalletDancersAction {
    }

    @RestfulAction(hyphenate = { "ballet-dancers", "greatest-favorite-studios" })
    private static class MockBalletDancersGreatestFavoriteStudiosAction {
    }

    @RestfulAction(hyphenate = { "ballet-dancers", "dancers-greatest" })
    private static class MockBalletDancersGreatestDancersGreatestAction {
    }

    @RestfulAction(hyphenate = { "ballet-dancers" })
    private static class MockBalletDancersGreatestBalletDancersStudiosAction {
    }
}
