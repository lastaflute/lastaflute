package org.lastaflute.web.ruts.process;

import org.dbflute.utflute.core.PlainTestCase;

/**
 * @author jflute
 */
public class JsonDebugChallengeTest extends PlainTestCase {

    // ===================================================================================
    //                                                                             Integer
    //                                                                             =======
    public void test_JsonDebugChallenge_Integer_zeroDecimal() throws Exception {
        // ## Arrange ##
        JsonDebugChallenge challenge = new JsonDebugChallenge("sea", Integer.class, "1.0", 0);

        // ## Act ##
        String disp = challenge.toChallengeDisp();

        // ## Assert ##
        log(disp);
        assertEquals("x Integer sea = (String) \"1.0\"", disp.trim());
    }

    public void test_JsonDebugChallenge_Integer_zeroDouble() throws Exception {
        // ## Arrange ##
        JsonDebugChallenge challenge = new JsonDebugChallenge("sea", Integer.class, (Double) 1.0, 0);

        // ## Act ##
        String disp = challenge.toChallengeDisp();

        // ## Assert ##
        log(disp);
        assertEquals("o Integer sea = (Integer) \"1\"", disp.trim());
    }

    // ===================================================================================
    //                                                                               Long
    //                                                                              ======
    public void test_JsonDebugChallenge_Long_zeroDecimal() throws Exception {
        // ## Arrange ##
        JsonDebugChallenge challenge = new JsonDebugChallenge("sea", Long.class, "1.0", 0);

        // ## Act ##
        String disp = challenge.toChallengeDisp();

        // ## Assert ##
        log(disp);
        assertEquals("x Long sea = (String) \"1.0\"", disp.trim());
    }

    public void test_JsonDebugChallenge_Long_zeroDouble() throws Exception {
        // ## Arrange ##
        JsonDebugChallenge challenge = new JsonDebugChallenge("sea", Long.class, (Double) 1.0, 0);

        // ## Act ##
        String disp = challenge.toChallengeDisp();

        // ## Assert ##
        log(disp);
        assertEquals("o Long sea = (Long) \"1\"", disp.trim());
    }
}
