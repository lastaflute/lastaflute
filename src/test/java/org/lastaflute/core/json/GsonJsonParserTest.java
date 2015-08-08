/*
 * Copyright 2014-2015 the original author or authors.
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
package org.lastaflute.core.json;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.dbflute.utflute.core.PlainTestCase;
import org.dbflute.utflute.core.cannonball.CannonballCar;
import org.dbflute.utflute.core.cannonball.CannonballOption;
import org.dbflute.utflute.core.cannonball.CannonballRun;
import org.lastaflute.core.json.exception.JsonPropertyNumberParseFailureException;
import org.lastaflute.unit.mock.db.MockCDef;

/**
 * @author jflute
 */
public class GsonJsonParserTest extends PlainTestCase {

    // ===================================================================================
    //                                                                          Java8 Time
    //                                                                          ==========
    public void test_java8time_toJson() throws Exception {
        // ## Arrange ##
        GsonJsonParser parser = new GsonJsonParser(builder -> {} , op -> {});
        LocalDate date = toLocalDate("2015/05/18");
        LocalDateTime dateTime = toLocalDateTime("2015/05/25 12:34:56.789");
        LocalTime time = toLocalTime("23:15:47.731");

        // ## Act ##
        String json = parser.toJson(new MockUser(2, "land", date, dateTime, time));

        // ## Assert ##
        log(json);
        assertContainsAll(json, "2015-05-18", "2015-05-25T12:34:56.789", "23:15:47.731");

        // ## Act ##
        MockUser fromJson = parser.fromJson(json, MockUser.class);

        // ## Assert ##
        log(fromJson);
        assertEquals("land", fromJson.name);
        assertEquals(toString(fromJson.birthdate, "yyyy-MM-dd"), "2015-05-18");
        assertEquals(toString(fromJson.formalizedDatetime, "yyyy-MM-dd HH:mm:ss.SSS"), "2015-05-25 12:34:56.789");
        assertEquals(toString(fromJson.morningCallTime, "HH:mm:ss.SSS"), "23:15:47.731");
    }

    // ===================================================================================
    //                                                                               CDef
    //                                                                              ======
    public void test_CDef_toJson() throws Exception {
        // ## Arrange ##
        GsonJsonParser parser = new GsonJsonParser(builder -> {} , op -> {});

        // ## Act ##
        String json = parser.toJson(new MockUser(2, "land", null, null, null, MockCDef.Flg.True));

        // ## Assert ##
        log(json);
        assertContainsAll(json, "1");

        // ## Act ##
        MockUser fromJson = parser.fromJson(json, MockUser.class);

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
        GsonJsonParser parser = new GsonJsonParser(builder -> builder.serializeNulls(), op -> {});
        String json = "{id:null,name:null,status:null,birthdate:null,formalizedDatetime:null,morningCallTime:null,anyFlg:null}";

        // ## Act ##
        MockUser user = parser.fromJson(json, MockUser.class);

        // ## Assert ##
        log(user);
        assertNull(user.id);
        assertNull(user.name);
        assertNull(user.birthdate);
        assertNull(user.validFlg);
    }

    public void test_emptyToNull_toJson_non_emptyProperty() throws Exception {
        // ## Arrange ##
        GsonJsonParser parser = new GsonJsonParser(builder -> builder.serializeNulls(), op -> {});
        String json = "{id:null,name:null,status:null,birthdate:null,formalizedDatetime:null,morningCallTime:null,anyFlg:\"\"}";

        // ## Act ##
        MockUser user = parser.fromJson(json, MockUser.class);

        // ## Assert ##
        log(user);
        assertNull(user.id);
        assertNull(user.name);
        assertNull(user.birthdate);
        assertNull(user.validFlg);
    }

    public void test_emptyToNull_toJson_valid_nullAll() throws Exception {
        // ## Arrange ##
        GsonJsonParser parser = new GsonJsonParser(builder -> builder.serializeNulls(), op -> op.asEmptyToNullReading());
        String json = "{id:null,name:null,status:null,birthdate:null,formalizedDatetime:null,morningCallTime:null,anyFlg:null}";

        // ## Act ##
        MockUser user = parser.fromJson(json, MockUser.class);

        // ## Assert ##
        log(user);
        assertNull(user.id);
        assertNull(user.name);
        assertNull(user.birthdate);
        assertNull(user.validFlg);
    }

    public void test_emptyToNull_toJson_valid_emptyProperty() throws Exception {
        // ## Arrange ##
        GsonJsonParser parser = new GsonJsonParser(builder -> builder.serializeNulls(), op -> op.asEmptyToNullReading());
        String json = "{id:\"\",name:\"\",status:null,birthdate:\"\",formalizedDatetime:\"\",morningCallTime:\"\",anyFlg:\"\"}";

        // ## Act ##
        MockUser user = parser.fromJson(json, MockUser.class);

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
        GsonJsonParser parser = new GsonJsonParser(builder -> builder.serializeNulls(), op -> {});

        // ## Act ##
        String json = parser.toJson(new MockUser(null, null, null, null, null, null));

        // ## Assert ##
        log(json);
        assertContains(json, "null");
        assertNotContains(json, "\"\"");

        // ## Act ##
        MockUser fromJson = parser.fromJson(json, MockUser.class);

        // ## Assert ##
        log(fromJson);
        assertNull(fromJson.name);
        assertNull(fromJson.validFlg);
    }

    public void test_nullToEmpty_toJson_valid() throws Exception {
        // ## Arrange ##
        GsonJsonParser parser = new GsonJsonParser(builder -> {} , op -> op.asNullToEmptyWriting());

        // ## Act ##
        String json = parser.toJson(new MockUser(null, null, null, null, null, null));

        // ## Assert ##
        log(json);
        assertContains(json, "\"\"");
        assertNotContains(json, "null");

        // ## Act ##
        try {
            parser.fromJson(json, MockUser.class);
            // ## Assert ##
            fail();
        } catch (JsonPropertyNumberParseFailureException e) { // cannot reverse
            log(e.getMessage());
        }
    }

    // ===================================================================================
    //                                                                         Thread Safe
    //                                                                         ===========
    public void test_threadSafe() throws Exception {
        // ## Arrange ##
        GsonJsonParser parser = new GsonJsonParser(builder -> {} , op -> {});

        // ## Act ##
        // ## Assert ##
        cannonball(new CannonballRun() {
            public void drive(CannonballCar car) {
                String toJson1 = parser.toJson(new MockUser(1, "sea"));
                String toJson2 = parser.toJson(new MockUser(2, "land", new MockUserStatus("fml")));
                MockUser fromJson1 = parser.fromJson(toJson1, MockUser.class);
                MockUser fromJson2 = parser.fromJson(toJson2, MockUser.class);
                String totoJson1 = parser.toJson(fromJson1);
                String totoJson2 = parser.toJson(fromJson2);
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

        public MockUser() {
        }

        public MockUser(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        public MockUser(Integer id, String name, MockUserStatus status) {
            this.id = id;
            this.name = name;
            this.status = status;
        }

        public MockUser(Integer id, String name, LocalDate birthdate, LocalDateTime formalizedDatetime) {
            this.id = id;
            this.name = name;
            this.birthdate = birthdate;
            this.formalizedDatetime = formalizedDatetime;
        }

        public MockUser(Integer id, String name, LocalDate birthdate, LocalDateTime formalizedDatetime, LocalTime callTime) {
            this.id = id;
            this.name = name;
            this.birthdate = birthdate;
            this.formalizedDatetime = formalizedDatetime;
            this.morningCallTime = callTime;
        }

        public MockUser(Integer id, String name, LocalDate birthdate, LocalDateTime formalizedDatetime, LocalTime callTime,
                MockCDef.Flg validFlg) {
            this.id = id;
            this.name = name;
            this.birthdate = birthdate;
            this.formalizedDatetime = formalizedDatetime;
            this.morningCallTime = callTime;
            this.validFlg = validFlg;
        }

        @Override
        public String toString() {
            return "{" + id + ", " + name + ", " + status + ", " + birthdate + ", " + formalizedDatetime + ", " + morningCallTime + ", "
                    + validFlg + "}";
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
