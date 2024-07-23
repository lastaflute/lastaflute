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
package org.lastaflute.core.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.dbflute.utflute.core.PlainTestCase;
import org.lastaflute.core.util.LatoTest.ToStrSeaBean.ToStrAmbaPart;
import org.lastaflute.core.util.LatoTest.ToStrSeaBean.ToStrBonvoPart;
import org.lastaflute.core.util.LatoTest.ToStrSeaBean.ToStrDstorePart;
import org.lastaflute.core.util.LatoTest.ToStrSeaBean.ToStrLandPart;
import org.lastaflute.core.util.LatoTest.ToStrSeaBean.ToStrLandPart.ToStrShowBase;
import org.lastaflute.core.util.LatoTest.ToStrSeaBean.ToStrPiaryPart;
import org.lastaflute.unit.mock.db.MockOldCDef;
import org.lastaflute.web.validation.Required;

import jakarta.validation.Valid;

/**
 * @author jflute
 */
public class LatoTest extends PlainTestCase {

    public void test_string_basic() {
        assertEquals("null", Lato.string(null));
        assertEquals("", Lato.string(""));
        assertEquals("1", Lato.string(1));
        assertEquals("2016-09-09", Lato.string(LocalDate.of(2016, 9, 9)));
        assertEquals(MockOldCDef.MemberStatus.Formalized.code(), Lato.string(MockOldCDef.MemberStatus.Formalized));
        assertContains(Lato.string(new ToStrSeaBean()), "{seaId=null, seaName=null, seaAccount=null, ");
        assertEquals(Lato.string(new ToStrSeaBean()), Lato.string(new ToStrSeaBean() {
        }));
        log(Lato.string(new ToStrSeaBean() {
        })); // expects no exception
        assertEquals(Lato.string(new ToStrSeaBean()), Lato.string(new ToStrSeaBean() {
            @Override
            public String toString() {
                return "ignored: not related";
            }
        }));
    }

    public void test_string_onparade() {
        // ## Arrange ##
        ToStrSeaBean sea = new ToStrSeaBean();
        sea.seaId = 3;
        sea.seaName = "mystic";
        sea.seaAccount = "rhythms";
        sea.birthdate = LocalDate.now();
        sea.formalizedDatetime = null;
        sea.updateDatetime = LocalDateTime.now().minusDays(7);
        ToStrLandPart land = new ToStrLandPart();
        land.landName = "oneman";
        land.landAccount = "minnie";
        land.showBase = new ToStrShowBase();
        land.showBase.showBaseName = "tommorrow";
        land.showBase.land = land;
        sea.land = land;
        ToStrPiaryPart piaryPart1 = new ToStrPiaryPart();
        piaryPart1.piaryName = "bonvo";
        piaryPart1.piaryAccount = "dstore";
        ToStrPiaryPart piaryPart2 = new ToStrPiaryPart();
        piaryPart2.piaryName = "amba";
        piaryPart2.piaryAccount = "miraco";
        sea.piaryList = Arrays.asList(piaryPart1, piaryPart2, null);
        piaryPart2.nestedPiaryList = sea.piaryList;
        sea.piaryAry = new ToStrPiaryPart[] { piaryPart1, null, piaryPart2 };
        sea.bonvo = new ToStrBonvoPart();
        sea.dstore = new ToStrDstorePart(7, sea, land);
        ToStrAmbaPart amba = new ToStrAmbaPart();
        amba.piaryList = sea.piaryList;
        sea.ambaList = Arrays.asList(amba, new ToStrAmbaPart());

        // ## Act ##
        String str = sea.toString();

        // ## Assert ##
        log(str);
        assertContains(str, sea.seaId.toString());
        assertContains(str, sea.seaName);
        assertContains(str, "land={");
        assertContains(str, sea.land.landName);
        assertContains(str, "piaryList=[");
        assertContains(str, sea.piaryList.get(0).piaryName);
        assertContains(str, sea.piaryList.get(1).piaryName);
        assertContains(str, "dstore={dstoreId=7, sea=(cyclic), land={landName=oneman, ");
        assertContains(str, "nestedPiaryList=(cyclic)");
        assertContains(str, "piaryAry=[{piaryName=bonvo, piaryAccount...(same)}, null, ");
    }

    public static class ToStrSeaBean {

        public Integer seaId;
        public String seaName;
        public String seaAccount;
        public LocalDate birthdate;
        public LocalDateTime formalizedDatetime;
        public LocalDateTime updateDatetime;

        public ToStrLandPart land;

        public static class ToStrLandPart {

            public String landName;
            public String landAccount;

            @Valid
            public ToStrShowBase showBase;

            public static class ToStrShowBase {

                @Required
                public String showBaseName;
                @Required
                public ToStrLandPart land;

                @Override
                public String toString() {
                    return Lato.string(this); // ignored when from sea
                }
            }
        }

        public List<ToStrPiaryPart> piaryList;

        public static class ToStrPiaryPart {

            public String piaryName;
            public String piaryAccount;
            public List<ToStrPiaryPart> nestedPiaryList;
        }

        public ToStrPiaryPart[] piaryAry;

        public ToStrBonvoPart bonvo;

        public static class ToStrBonvoPart { // no property
        }

        public ToStrDstorePart dstore;

        public static class ToStrDstorePart {

            public Integer dstoreId;
            public ToStrSeaBean sea;
            public ToStrLandPart land;

            public ToStrDstorePart(Integer dstoreId, ToStrSeaBean sea, ToStrLandPart land) {
                this.dstoreId = dstoreId;
                this.sea = sea;
                this.land = land;
            }
        }

        public List<ToStrAmbaPart> ambaList;

        public static class ToStrAmbaPart {

            public List<ToStrPiaryPart> piaryList;

            @Override
            public String toString() {
                return Lato.string(this);
            }
        }

        @Override
        public String toString() {
            return Lato.string(this);
        }
    }
}
