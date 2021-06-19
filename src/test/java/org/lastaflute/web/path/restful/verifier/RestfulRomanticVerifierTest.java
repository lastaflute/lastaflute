package org.lastaflute.web.path.restful.verifier;

import org.dbflute.utflute.core.PlainTestCase;
import org.lastaflute.web.RestfulAction;
import org.lastaflute.web.exception.ExecuteMethodIllegalDefinitionException;

/**
 * @author jflute
 * @since 1.2.1 (2021/06/19 Saturday at ikspiari)
 */
public class RestfulRomanticVerifierTest extends PlainTestCase {

    // ===================================================================================
    //                                                                    Cannot Hyphenate
    //                                                                    ================
    // -----------------------------------------------------
    //                                Hyphenated Name Format
    //                                ----------------------
    public void test_isHyphenatedNameFormatGood_basic() {
        // ## Arrange ##
        RestfulRomanticVerifier verifier = new RestfulRomanticVerifier();

        // ## Act ##
        // ## Assert ##
        assertTrue(verifier.isHyphenatedNameFormatGood("sea-land"));
        assertTrue(verifier.isHyphenatedNameFormatGood("sea-land-piari"));

        assertFalse(verifier.isHyphenatedNameFormatGood("sea"));
        assertFalse(verifier.isHyphenatedNameFormatGood("-sea"));
        assertFalse(verifier.isHyphenatedNameFormatGood("sea-"));
        assertFalse(verifier.isHyphenatedNameFormatGood("-sea-land-"));
        assertFalse(verifier.isHyphenatedNameFormatGood("sea--land"));
        assertFalse(verifier.isHyphenatedNameFormatGood("sea/land"));
        assertFalse(verifier.isHyphenatedNameFormatGood("Sea-Land"));
        assertFalse(verifier.isHyphenatedNameFormatGood("sea$land-piari"));
        assertFalse(verifier.isHyphenatedNameFormatGood("sea_land-piari"));
    }

    // -----------------------------------------------------
    //                               Hyphenated Name Linkage
    //                               -----------------------
    // deep logic with loop so using parent method here
    public void test_doVerifyRestfulHyphenateLinkage_containing() {
        // ## Arrange ##
        RestfulRomanticVerifier verifier = new RestfulRomanticVerifier();

        // ## Act ##
        // ## Assert ##
        {
            Class<?> actionType = MockBalletDancersAction.class;
            verifier.doVerifyRestfulHyphenateLinkage(actionType, new String[] { "mock-ballet" });
            verifier.doVerifyRestfulHyphenateLinkage(actionType, new String[] { "ballet-dancers" });
            verifier.doVerifyRestfulHyphenateLinkage(actionType, new String[] { "mock-ballet-dancers" });
            assertException(ExecuteMethodIllegalDefinitionException.class, () -> {
                verifier.doVerifyRestfulHyphenateLinkage(actionType, new String[] { "mock-ballet", "ballet-dancers" });
            });
            assertException(ExecuteMethodIllegalDefinitionException.class, () -> {
                verifier.doVerifyRestfulHyphenateLinkage(actionType, new String[] { "ballet-dancers", "mock-ballet" });
            });
        }
        {
            Class<?> actionType = MockBalletDancersGreatestFavoriteStudiosAction.class;
            verifier.doVerifyRestfulHyphenateLinkage(actionType, new String[] { "ballet-dancers", "greatest-favorite-studios" });
            verifier.doVerifyRestfulHyphenateLinkage(actionType, new String[] { "mock-ballet", "greatest-favorite-studios" });
            verifier.doVerifyRestfulHyphenateLinkage(actionType, new String[] { "mock-ballet", "dancers-greatest", "favorite-studios" });
            assertException(ExecuteMethodIllegalDefinitionException.class, () -> {
                verifier.doVerifyRestfulHyphenateLinkage(actionType, new String[] { "ballet-dancers", "dancers-greatest-favorite" });
            });
            assertException(ExecuteMethodIllegalDefinitionException.class, () -> {
                verifier.doVerifyRestfulHyphenateLinkage(actionType, new String[] { "ballet-dancers", "mock-greatest" });
            });
        }
        {
            Class<?> actionType = MockBalletDancersGreatestDancersGreatestAction.class;
            verifier.doVerifyRestfulHyphenateLinkage(actionType, new String[] { "ballet-dancers", "dancers-greatest" });
            assertException(ExecuteMethodIllegalDefinitionException.class, () -> {
                verifier.doVerifyRestfulHyphenateLinkage(actionType, new String[] { "dancers-greatest", "ballet-dancers" });
            });
        }
        {
            Class<?> actionType = MockBalletDancersGreatestBalletDancersStudiosAction.class;
            verifier.doVerifyRestfulHyphenateLinkage(actionType, new String[] { "ballet-dancers" });
            assertException(ExecuteMethodIllegalDefinitionException.class, () -> {
                verifier.doVerifyRestfulHyphenateLinkage(actionType, new String[] { "ballet-dancers", "greatest-ballet" });
            });
            assertException(ExecuteMethodIllegalDefinitionException.class, () -> {
                verifier.doVerifyRestfulHyphenateLinkage(actionType, new String[] { "ballet-dancers", "dancers-studios" });
            });
            assertException(ExecuteMethodIllegalDefinitionException.class, () -> {
                verifier.doVerifyRestfulHyphenateLinkage(actionType, new String[] { "ballet-dancers", "greatest-studios" });
            });
        }
    }

    @RestfulAction(hyphenate = "dummy-dummy")
    private static class MockBalletDancersAction {
    }

    @RestfulAction(hyphenate = "dummy-dummy")
    private static class MockBalletDancersGreatestFavoriteStudiosAction {
    }

    @RestfulAction(hyphenate = "dummy-dummy")
    private static class MockBalletDancersGreatestDancersGreatestAction {
    }

    @RestfulAction(hyphenate = "dummy-dummy")
    private static class MockBalletDancersGreatestBalletDancersStudiosAction {
    }
}
