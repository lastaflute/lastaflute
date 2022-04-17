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
package org.lastaflute.core.util;

import org.dbflute.utflute.core.PlainTestCase;
import org.lastaflute.core.util.LaClassificationUtil.ClassificationCodeOfMethodNotFoundException;
import org.lastaflute.core.util.LaClassificationUtil.ClassificationFindByCodeMethodNotFoundException;
import org.lastaflute.core.util.LaClassificationUtil.ClassificationMetaFindMethodNotFoundException;
import org.lastaflute.core.util.LaClassificationUtil.ClassificationUnknownCodeException;
import org.lastaflute.unit.mock.db.MockCDef;
import org.lastaflute.unit.mock.db.MockDepCDef;
import org.lastaflute.unit.mock.db.MockOldCDef;

/**
 * @author jflute
 * @since 1.2.4 (2022/04/17 Sunday at roppongi japanese)
 */
public class LaClassificationUtilTest extends PlainTestCase {

    // ===================================================================================
    //                                                                       Determination
    //                                                                       =============
    public void test_isCls() {
        assertTrue(LaClassificationUtil.isCls(MockCDef.MemberStatus.class));
        assertTrue(LaClassificationUtil.isCls(MockDepCDef.MemberStatus.class));
        assertTrue(LaClassificationUtil.isCls(MockOldCDef.MemberStatus.class));

        assertFalse(LaClassificationUtil.isCls(MockCDef.DefMeta.class));
        assertFalse(LaClassificationUtil.isCls(MockDepCDef.DefMeta.class));
        assertFalse(LaClassificationUtil.isCls(MockOldCDef.DefMeta.class));
    }

    // ===================================================================================
    //                                                                   to Classification
    //                                                                   =================
    public void test_toCls() throws Exception {
        assertEquals(MockCDef.MemberStatus.Formalized, LaClassificationUtil.toCls(MockCDef.MemberStatus.class, "FML"));
        assertEquals(MockDepCDef.MemberStatus.Formalized, LaClassificationUtil.toCls(MockDepCDef.MemberStatus.class, "FML"));
        assertEquals(MockOldCDef.MemberStatus.Formalized, LaClassificationUtil.toCls(MockOldCDef.MemberStatus.class, "FML"));

        try {
            LaClassificationUtil.toCls(MockCDef.MemberStatus.class, "none");
            fail();
        } catch (ClassificationUnknownCodeException e) {
            log(e);
        }
        try {
            LaClassificationUtil.toCls(MockDepCDef.MemberStatus.class, "none");
            fail();
        } catch (ClassificationUnknownCodeException e) {
            log(e);
        }
        try {
            LaClassificationUtil.toCls(MockOldCDef.MemberStatus.class, "none");
            fail();
        } catch (ClassificationUnknownCodeException e) {
            log(e);
        }
    }

    public void test_findByCode() {
        assertEquals(MockCDef.MemberStatus.Formalized, LaClassificationUtil.findByCode(MockCDef.MemberStatus.class, "FML").get());
        assertEquals(MockDepCDef.MemberStatus.Formalized, LaClassificationUtil.findByCode(MockDepCDef.MemberStatus.class, "FML").get());
        assertEquals(MockOldCDef.MemberStatus.Formalized, LaClassificationUtil.findByCode(MockOldCDef.MemberStatus.class, "FML").get());
        assertFalse(LaClassificationUtil.findByCode(MockCDef.MemberStatus.class, "none").isPresent());
        assertFalse(LaClassificationUtil.findByCode(MockDepCDef.MemberStatus.class, "none").isPresent());
        assertFalse(LaClassificationUtil.findByCode(MockOldCDef.MemberStatus.class, "none").isPresent());
    }

    public void test_findMeta() {
        assertEquals(MockCDef.DefMeta.MemberStatus, LaClassificationUtil.findMeta(MockCDef.DefMeta.class, "MemberStatus").get());
        assertEquals(MockDepCDef.DefMeta.MemberStatus, LaClassificationUtil.findMeta(MockDepCDef.DefMeta.class, "MemberStatus").get());
        assertEquals(MockOldCDef.DefMeta.MemberStatus, LaClassificationUtil.findMeta(MockOldCDef.DefMeta.class, "MemberStatus").get());
        assertFalse(LaClassificationUtil.findMeta(MockCDef.DefMeta.class, "none").isPresent());
        assertFalse(LaClassificationUtil.findMeta(MockDepCDef.DefMeta.class, "none").isPresent());
        assertFalse(LaClassificationUtil.findMeta(MockOldCDef.DefMeta.class, "none").isPresent());
    }

    // ===================================================================================
    //                                                                       Native Method
    //                                                                       =============
    public void test_nativeFindByCode() {
        assertEquals(MockCDef.MemberStatus.Formalized, LaClassificationUtil.nativeFindByCode(MockCDef.MemberStatus.class, "FML").get());
        assertEquals(MockDepCDef.MemberStatus.Formalized,
                LaClassificationUtil.nativeFindByCode(MockDepCDef.MemberStatus.class, "FML").get());
        assertException(ClassificationFindByCodeMethodNotFoundException.class, () -> {
            LaClassificationUtil.nativeFindByCode(MockOldCDef.MemberStatus.class, "FML");
        });
        assertFalse(LaClassificationUtil.nativeFindByCode(MockCDef.MemberStatus.class, "none").isPresent());
        assertFalse(LaClassificationUtil.nativeFindByCode(MockDepCDef.MemberStatus.class, "none").isPresent());
        assertException(ClassificationFindByCodeMethodNotFoundException.class, () -> {
            LaClassificationUtil.nativeFindByCode(MockOldCDef.MemberStatus.class, "none");
        });
    }

    public void test_nativeFindMeta() {
        assertEquals(MockCDef.DefMeta.MemberStatus, LaClassificationUtil.nativeFindMeta(MockCDef.DefMeta.class, "MemberStatus").get());
        assertEquals(MockDepCDef.DefMeta.MemberStatus,
                LaClassificationUtil.nativeFindMeta(MockDepCDef.DefMeta.class, "MemberStatus").get());
        assertException(ClassificationMetaFindMethodNotFoundException.class, () -> {
            LaClassificationUtil.nativeFindMeta(MockOldCDef.DefMeta.class, "none");
        });
        assertFalse(LaClassificationUtil.nativeFindMeta(MockCDef.DefMeta.class, "none").isPresent());
        assertFalse(LaClassificationUtil.nativeFindMeta(MockDepCDef.DefMeta.class, "none").isPresent());
        assertException(ClassificationMetaFindMethodNotFoundException.class, () -> {
            LaClassificationUtil.nativeFindMeta(MockOldCDef.DefMeta.class, "none");
        });
    }

    @SuppressWarnings("deprecation")
    public void test_nativeCodeOf() {
        assertException(ClassificationCodeOfMethodNotFoundException.class, () -> {
            LaClassificationUtil.nativeCodeOf(MockCDef.MemberStatus.class, "FML");
        });
        assertEquals(MockDepCDef.MemberStatus.Formalized, LaClassificationUtil.nativeCodeOf(MockDepCDef.MemberStatus.class, "FML"));
        assertEquals(MockOldCDef.MemberStatus.Formalized, LaClassificationUtil.nativeCodeOf(MockOldCDef.MemberStatus.class, "FML"));
        assertException(ClassificationCodeOfMethodNotFoundException.class, () -> {
            LaClassificationUtil.nativeCodeOf(MockCDef.MemberStatus.class, "none");
        });
        assertNull(LaClassificationUtil.nativeCodeOf(MockDepCDef.MemberStatus.class, "none"));
        assertNull(LaClassificationUtil.nativeCodeOf(MockOldCDef.MemberStatus.class, "none"));
    }

    @SuppressWarnings("deprecation")
    public void test_nativeMetaOf() {
        assertEquals(MockCDef.DefMeta.MemberStatus, LaClassificationUtil.nativeMetaOf(MockCDef.DefMeta.class, "MemberStatus"));
        assertEquals(MockDepCDef.DefMeta.MemberStatus, LaClassificationUtil.nativeMetaOf(MockDepCDef.DefMeta.class, "MemberStatus"));
        assertEquals(MockOldCDef.DefMeta.MemberStatus, LaClassificationUtil.nativeMetaOf(MockOldCDef.DefMeta.class, "MemberStatus"));
        assertException(IllegalStateException.class, () -> LaClassificationUtil.nativeMetaOf(MockCDef.DefMeta.class, "none"));
        assertException(IllegalStateException.class, () -> LaClassificationUtil.nativeMetaOf(MockDepCDef.DefMeta.class, "none"));
        assertException(IllegalStateException.class, () -> LaClassificationUtil.nativeMetaOf(MockOldCDef.DefMeta.class, "none"));
    }
}
