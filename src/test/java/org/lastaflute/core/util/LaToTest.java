package org.lastaflute.core.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.dbflute.utflute.core.PlainTestCase;
import org.lastaflute.core.util.LaToTest.ToStrSeaBean.ToStrAmbaPart;
import org.lastaflute.core.util.LaToTest.ToStrSeaBean.ToStrBonvoPart;
import org.lastaflute.core.util.LaToTest.ToStrSeaBean.ToStrDstorePart;
import org.lastaflute.core.util.LaToTest.ToStrSeaBean.ToStrLandPart;
import org.lastaflute.core.util.LaToTest.ToStrSeaBean.ToStrPiaryPart;

/**
 * @author jflute
 */
public class LaToTest extends PlainTestCase {

    public void test_string() {
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
                return LaTo.string(this);
            }
        }

        @Override
        public String toString() {
            return LaTo.string(this);
        }
    }
}
