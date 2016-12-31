/*
 * Copyright 2015-2017 the original author or authors.
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
package org.lastaflute.core.json.engine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import org.dbflute.utflute.core.PlainTestCase;
import org.dbflute.utflute.core.cannonball.CannonballCar;
import org.dbflute.utflute.core.cannonball.CannonballOption;
import org.dbflute.utflute.core.cannonball.CannonballRun;
import org.lastaflute.core.json.exception.JsonPropertyNumberParseFailureException;
import org.lastaflute.unit.mock.db.MockCDef;

/**
 * @author jflute
 */
public class GsonJsonEngineTest extends PlainTestCase {

    // ===================================================================================
    //                                                                          Java8 Time
    //                                                                          ==========
    public void test_java8time_toJson_fromJson() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> {}, op -> {});
        LocalDate date = toLocalDate("2015/05/18");
        LocalDateTime dateTime = toLocalDateTime("2015/05/25 12:34:56.789");
        LocalTime time = toLocalTime("23:15:47.731");
        MockUser mockUser = new MockUser();
        mockUser.id = 2;
        mockUser.name = "land";
        mockUser.birthdate = date;
        mockUser.formalizedDatetime = dateTime;
        mockUser.morningCallTime = time;

        // ## Act ##
        String json = engine.toJson(mockUser);

        // ## Assert ##
        log(json);
        assertContainsAll(json, "2015-05-18", "2015-05-25T12:34:56.789", "23:15:47.731");

        // ## Act ##
        MockUser fromJson = engine.fromJson(json, MockUser.class);

        // ## Assert ##
        log(fromJson);
        assertEquals("land", fromJson.name);
        assertEquals(toString(fromJson.birthdate, "yyyy-MM-dd"), "2015-05-18");
        assertEquals(toString(fromJson.formalizedDatetime, "yyyy-MM-dd HH:mm:ss.SSS"), "2015-05-25 12:34:56.789");
        assertEquals(toString(fromJson.morningCallTime, "HH:mm:ss.SSS"), "23:15:47.731");
    }

    // ===================================================================================
    //                                                                             Boolean
    //                                                                             =======
    public void test_Boolean_toJson_fromJson_basic() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> {}, op -> {});
        MockUser mockUser = new MockUser();
        mockUser.id = 2;
        mockUser.name = "land";
        mockUser.primitiveFlg = true;
        mockUser.wrapperFlg = true;

        // ## Act ##
        String json = engine.toJson(mockUser);

        // ## Assert ##
        log(json);
        assertContainsAll(json, "true");

        // ## Act ##
        MockUser fromJson = engine.fromJson(json, MockUser.class);

        // ## Assert ##
        log(fromJson);
        assertEquals("land", fromJson.name);
        assertTrue(fromJson.primitiveFlg);
        assertTrue(fromJson.wrapperFlg);
    }

    public void test_Boolean_toJson_fromJson_null() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> {}, op -> {});
        MockUser mockUser = new MockUser();
        mockUser.id = 2;
        mockUser.name = "land";
        mockUser.primitiveFlg = true;
        mockUser.wrapperFlg = null;

        // ## Act ##
        String json = engine.toJson(mockUser);

        // ## Assert ##
        log(json);
        assertContainsAll(json, "true");

        // ## Act ##
        MockUser fromJson = engine.fromJson(json, MockUser.class);

        // ## Assert ##
        log(fromJson);
        assertEquals("land", fromJson.name);
        assertTrue(fromJson.primitiveFlg);
        assertNull(fromJson.wrapperFlg);
    }

    public void test_Boolean_toJson_fromJson_everywhereQuoteWriting_andOthers() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> {
            op.asEverywhereQuoteWriting().asNullToEmptyWriting().asEmptyToNullReading();
        });
        MockUser mockUser = new MockUser();
        mockUser.id = 2;
        mockUser.name = "land";
        mockUser.primitiveFlg = true;
        mockUser.wrapperFlg = null;

        // ## Act ##
        String json = engine.toJson(mockUser);

        // ## Assert ##
        log(json);
        assertContainsAll(json, "\"true\"");

        // ## Act ##
        MockUser fromJson = engine.fromJson(json, MockUser.class);

        // ## Assert ##
        log(fromJson);
        assertEquals("land", fromJson.name);
        assertTrue(fromJson.primitiveFlg);
        assertNull(fromJson.wrapperFlg);
    }

    public void test_Boolean_toJson_fromJson_booleanFilter_integer() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> {
            op.deserializeBooleanBy(exp -> exp.equals(1)).serializeBooleanBy(value -> value ? 1 : 0);
        });
        MockUser mockUser = new MockUser();
        mockUser.id = 2;
        mockUser.name = "land";
        mockUser.primitiveFlg = true;
        mockUser.wrapperFlg = null;

        // ## Act ##
        String json = engine.toJson(mockUser);

        // ## Assert ##
        log(json);
        assertContainsAll(json, "1");

        // ## Act ##
        MockUser fromJson = engine.fromJson(json, MockUser.class);

        // ## Assert ##
        log(fromJson);
        assertEquals("land", fromJson.name);
        assertTrue(fromJson.primitiveFlg);
        assertNull(fromJson.wrapperFlg);
    }

    public void test_Boolean_toJson_fromJson_booleanFilter_string() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> {
            op.deserializeBooleanBy(exp -> exp.equals("1")).serializeBooleanBy(value -> value ? "1" : "0");
        });
        MockUser mockUser = new MockUser();
        mockUser.id = 2;
        mockUser.name = "land";
        mockUser.primitiveFlg = true;
        mockUser.wrapperFlg = null;

        // ## Act ##
        String json = engine.toJson(mockUser);

        // ## Assert ##
        log(json);
        assertContainsAll(json, "\"1\"");

        // ## Act ##
        MockUser fromJson = engine.fromJson(json, MockUser.class);

        // ## Assert ##
        log(fromJson);
        assertEquals("land", fromJson.name);
        assertTrue(fromJson.primitiveFlg);
        assertNull(fromJson.wrapperFlg);
    }

    // ===================================================================================
    //                                                                               CDef
    //                                                                              ======
    public void test_CDef_toJson_fromJson() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> {}, op -> {});
        MockUser mockUser = new MockUser();
        mockUser.id = 2;
        mockUser.name = "land";
        mockUser.validFlg = MockCDef.Flg.True;

        // ## Act ##
        String json = engine.toJson(mockUser);

        // ## Assert ##
        log(json);
        assertContainsAll(json, "1");

        // ## Act ##
        MockUser fromJson = engine.fromJson(json, MockUser.class);

        // ## Assert ##
        log(fromJson);
        assertEquals("land", fromJson.name);
        assertEquals(MockCDef.Flg.True, fromJson.validFlg);
    }

    // ===================================================================================
    //                                                                       Empty to Null
    //                                                                       =============
    public void test_emptyToNull_toJson_non_nullAll() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> {});
        String json =
                "{id:null,name:null,status:null,birthdate:null,formalizedDatetime:null,morningCallTime:null,primitiveFlg:null,wrapperFlg:null,validFlg:null}";

        // ## Act ##
        MockUser user = engine.fromJson(json, MockUser.class);

        // ## Assert ##
        log(user);
        assertNull(user.id);
        assertNull(user.name);
        assertNull(user.birthdate);
        assertFalse(user.primitiveFlg);
        assertNull(user.wrapperFlg);
        assertNull(user.validFlg);
    }

    public void test_emptyToNull_toJson_non_emptyProperty() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> {});
        String json =
                "{id:null,name:null,status:null,birthdate:null,formalizedDatetime:null,morningCallTime:null,primitiveFlg:null,wrapperFlg:null,validFlg:\"\"}";

        // ## Act ##
        MockUser user = engine.fromJson(json, MockUser.class);

        // ## Assert ##
        log(user);
        assertNull(user.id);
        assertNull(user.name);
        assertNull(user.birthdate);
        assertFalse(user.primitiveFlg);
        assertNull(user.wrapperFlg);
        assertNull(user.validFlg);
    }

    public void test_emptyToNull_toJson_valid_nullAll() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> op.asEmptyToNullReading());
        String json = "{id:null,name:null,status:null,birthdate:null,formalizedDatetime:null,morningCallTime:null,validFlg:null}";

        // ## Act ##
        MockUser user = engine.fromJson(json, MockUser.class);

        // ## Assert ##
        log(user);
        assertNull(user.id);
        assertNull(user.name);
        assertNull(user.birthdate);
        assertNull(user.validFlg);
    }

    public void test_emptyToNull_toJson_valid_emptyProperty() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> op.asEmptyToNullReading());
        String json = "{id:\"\",name:\"\",status:null,birthdate:\"\",formalizedDatetime:\"\",morningCallTime:\"\",validFlg:\"\"}";

        // ## Act ##
        MockUser user = engine.fromJson(json, MockUser.class);

        // ## Assert ##
        log(user);
        assertNull(user.id);
        assertNull(user.name);
        assertNull(user.birthdate);
        assertNull(user.validFlg);
    }

    // ===================================================================================
    //                                                                       Null to Empty
    //                                                                       =============
    public void test_nullToEmpty_toJson_non() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> {});

        // ## Act ##
        String json = engine.toJson(new MockUser());

        // ## Assert ##
        log(json);
        assertContains(json, "null");
        assertNotContains(json, "\"\"");

        // ## Act ##
        MockUser fromJson = engine.fromJson(json, MockUser.class);

        // ## Assert ##
        log(fromJson);
        assertNull(fromJson.name);
        assertNull(fromJson.validFlg);
    }

    public void test_nullToEmpty_toJson_valid() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> {}, op -> op.asNullToEmptyWriting());

        // ## Act ##
        String json = engine.toJson(new MockUser());

        // ## Assert ##
        log(json);
        assertContains(json, "\"\"");
        assertNotContains(json, "null");

        // ## Act ##
        try {
            engine.fromJson(json, MockUser.class);
            // ## Assert ##
            fail();
        } catch (JsonPropertyNumberParseFailureException e) { // cannot reverse
            log(e.getMessage());
        }
    }

    // ===================================================================================
    //                                                                   Filter SimpleText
    //                                                                   =================
    public void test_filterSimpleText_fromJson_non() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> {
            op.filterSimpleTextReading(text -> text);
        });
        String json = "{id:\" 1 \",name:\" sea \",status:null,birthdate:\" 2015-12-15 \",validFlg:\" true \",primitiveFlg:\" true \"}";

        // ## Act ##
        try {
            engine.fromJson(json, MockUser.class);
            // ## Assert ##
            fail();
        } catch (JsonPropertyNumberParseFailureException e) {
            log(e.getMessage());
        }
    }

    public void test_filterSimpleText_fromJson_trim() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> {
            op.filterSimpleTextReading(text -> text.trim());
        });
        String json = "{id:\" 1 \",name:\" sea \",status:null,birthdate:\" 2015-12-15 \",validFlg:\" true \",primitiveFlg:\" true \"}";

        // ## Act ##
        MockUser user = engine.fromJson(json, MockUser.class);

        // ## Assert ##
        log(user);
        assertEquals(1, user.id);
        assertEquals("sea", user.name);
        assertNull(user.status);
        assertEquals(LocalDate.of(2015, 12, 15), user.birthdate);
        assertNull(user.formalizedDatetime);
        assertEquals(MockCDef.Flg.True, user.validFlg);
        assertTrue(user.primitiveFlg);
        log(engine.toJson(user)); // expect no exception
    }

    // ===================================================================================
    //                                                          List Null-to-Empty Reading
    //                                                          ==========================
    public void test_asListNullToEmptyReading_fromJson_noOption_null() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> {});
        String json = "{id:\"1\",stringList:null}";
        // ## Act ##
        MockUser user = engine.fromJson(json, MockUser.class);
        // ## Assert ##
        log(user);
        assertEquals(1, user.id);
        assertNull(user.stringList);
        assertContains(engine.toJson(user), "\"stringList\":null");
    }

    public void test_asListNullToEmptyReading_fromJson_noOption_hasElement() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> {});
        String json = "{id:\"1\",stringList:[\"over\",\"mystic\"]}";
        // ## Act ##
        MockUser user = engine.fromJson(json, MockUser.class);
        // ## Assert ##
        log(user);
        assertEquals(1, user.id);
        assertNotNull(user.stringList);
        assertEquals(Arrays.asList("over", "mystic"), user.stringList);
        assertContains(engine.toJson(user), "\"stringList\":[\"over\",\"mystic\"]");
    }

    public void test_asListNullToEmptyReading_fromJson_null() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> {
            op.asListNullToEmptyReading();
        });
        String json = "{id:\"1\",stringList:null}";
        // ## Act ##
        MockUser user = engine.fromJson(json, MockUser.class);
        // ## Assert ##
        log(user);
        assertEquals(1, user.id);
        assertNotNull(user.stringList);
        assertHasZeroElement(user.stringList);
        assertContains(engine.toJson(user), "\"stringList\":[]");
    }

    public void test_asListNullToEmptyReading_fromJson_emptyList() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> {
            op.asListNullToEmptyReading();
        });
        String json = "{id:\"1\",stringList:[]}";
        // ## Act ##
        MockUser user = engine.fromJson(json, MockUser.class);
        // ## Assert ##
        log(user);
        assertEquals(1, user.id);
        assertNotNull(user.stringList);
        assertHasZeroElement(user.stringList);
        assertContains(engine.toJson(user), "\"stringList\":[]");
    }

    public void test_asListNullToEmptyReading_fromJson_hasElement() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> {
            op.asListNullToEmptyReading();
        });
        String json = "{id:\"1\",stringList:[\"over\",\"mystic\"]}";
        // ## Act ##
        MockUser user = engine.fromJson(json, MockUser.class);
        // ## Assert ##
        log(user);
        assertEquals(1, user.id);
        assertNotNull(user.stringList);
        assertEquals(Arrays.asList("over", "mystic"), user.stringList);
        assertContains(engine.toJson(user), "over");
    }

    // ===================================================================================
    //                                                          List Null-to-Empty Writing
    //                                                          ==========================
    public void test_asListNullToEmptyWriting_toJson_noOption_null() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> {});
        String sourceJson = "{id:\"1\",stringList:null}";
        MockUser user = engine.fromJson(sourceJson, MockUser.class);
        // ## Act ##
        String json = engine.toJson(user);
        // ## Assert ##
        log(json);
        assertContains(json, "\"stringList\":null");
    }

    public void test_asListNullToEmptyWriting_toJson_noOption_hasElement() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> {});
        String sourceJson = "{id:\"1\",stringList:[\"over\",\"mystic\"]}";
        MockUser user = engine.fromJson(sourceJson, MockUser.class);
        // ## Act ##
        String json = engine.toJson(user);
        // ## Assert ##
        log(json);
        assertContains(json, "\"stringList\":[\"over\",\"mystic\"]");
    }

    public void test_asListNullToEmptyWriting_toJson_null() throws Exception {
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> {
            op.asListNullToEmptyWriting();
        });
        String sourceJson = "{id:\"1\",stringList:null}";
        MockUser user = engine.fromJson(sourceJson, MockUser.class);
        // ## Act ##
        String json = engine.toJson(user);
        // ## Assert ##
        log(json);
        assertContains(json, "\"stringList\":[]");
    }

    public void test_asListNullToEmptyWriting_toJson_emptyList() throws Exception {
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> {
            op.asListNullToEmptyWriting();
        });
        String sourceJson = "{id:\"1\",stringList:[]}";
        MockUser user = engine.fromJson(sourceJson, MockUser.class);
        // ## Act ##
        String json = engine.toJson(user);
        // ## Assert ##
        log(json);
        assertContains(json, "\"stringList\":[]");
    }

    public void test_asListNullToEmptyWriting_toJson_hasElement() throws Exception {
        GsonJsonEngine engine = new GsonJsonEngine(builder -> builder.serializeNulls(), op -> {
            op.asListNullToEmptyReading();
        });
        String sourceJson = "{id:\"1\",stringList:[\"over\",\"mystic\"]}";
        MockUser user = engine.fromJson(sourceJson, MockUser.class);
        // ## Act ##
        String json = engine.toJson(user);
        // ## Assert ##
        log(json);
        assertContains(json, "\"stringList\":[\"over\",\"mystic\"]");
    }

    // ===================================================================================
    //                                                                         Thread Safe
    //                                                                         ===========
    public void test_threadSafe() throws Exception {
        // ## Arrange ##
        GsonJsonEngine engine = new GsonJsonEngine(builder -> {}, op -> {});

        // ## Act ##
        // ## Assert ##
        cannonball(new CannonballRun() {
            public void drive(CannonballCar car) {
                MockUser first = new MockUser();
                first.id = 1;
                first.name = "sea";
                MockUser second = new MockUser();
                second.id = 2;
                second.name = "land";
                second.status = new MockUserStatus("fml");
                String toJson1 = engine.toJson(first);
                String toJson2 = engine.toJson(second);
                MockUser fromJson1 = engine.fromJson(toJson1, MockUser.class);
                MockUser fromJson2 = engine.fromJson(toJson2, MockUser.class);
                String totoJson1 = engine.toJson(fromJson1);
                String totoJson2 = engine.toJson(fromJson2);
                car.goal(totoJson1 + totoJson2);
            }
        }, new CannonballOption().expectSameResult().threadCount(20).repeatCount(30));
    }

    // ===================================================================================
    //                                                                         Test Helper
    //                                                                         ===========
    public static class MockUser {
        public Integer id;
        public String name;
        public MockUserStatus status;
        public LocalDate birthdate;
        public LocalDateTime formalizedDatetime;
        public LocalTime morningCallTime;
        public MockCDef.Flg validFlg;
        public boolean primitiveFlg;
        public Boolean wrapperFlg;
        public List<String> stringList;

        public MockUser() {
        }

        @Override
        public String toString() {
            return "{" + id + ", " + name + ", " + status + ", " + birthdate + ", " + formalizedDatetime + ", " + morningCallTime + ", "
                    + validFlg + ", " + primitiveFlg + ", " + wrapperFlg + ", " + stringList + "}";
        }
    }

    public static class MockUserStatus {
        public String status;

        public MockUserStatus() {
        }

        public MockUserStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return "{" + status + "}";
        }
    }
}
