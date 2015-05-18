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

import org.dbflute.utflute.core.PlainTestCase;
import org.dbflute.utflute.core.cannonball.CannonballCar;
import org.dbflute.utflute.core.cannonball.CannonballOption;
import org.dbflute.utflute.core.cannonball.CannonballRun;

/**
 * @author jflute
 */
public class GsonJsonParserTest extends PlainTestCase {

    public void test_java8time_toJson() throws Exception {
        // ## Arrange ##
        GsonJsonParser parser = new GsonJsonParser(builder -> {});
        LocalDate currentDate = toLocalDate("2015/05/18");
        LocalDateTime currentDateTime = toLocalDateTime("2015/05/25 12:34:56.789");

        // ## Act ##
        String json = parser.toJson(new MockUser("2", "land", currentDate, currentDateTime, new MockUserStatus("fml")));

        // ## Assert ##
        log(json);
        assertContainsAll(json, "2015-05-18", "2015-05-25T12:34:56.789");

        // ## Act ##
        MockUser fromJson = parser.fromJson(json, MockUser.class);

        // ## Assert ##
        log(fromJson);
        assertEquals(toString(fromJson.birthdate, "yyyy-MM-dd"), "2015-05-18");
        assertEquals(toString(fromJson.formalizedDatetime, "yyyy-MM-dd HH:mm:ss.SSS"), "2015-05-25 12:34:56.789");
    }

    public void test_threadSafe() throws Exception {
        // ## Arrange ##
        GsonJsonParser parser = new GsonJsonParser(builder -> {});
        LocalDate currentDate = currentLocalDate();
        LocalDateTime currentDateTime = currentLocalDateTime();

        // ## Act ##
        // ## Assert ##
        cannonball(new CannonballRun() {
            public void drive(CannonballCar car) {
                String toJson1 = parser.toJson(new MockUser("1", "sea"));
                String toJson2 = parser.toJson(new MockUser("2", "land", currentDate, currentDateTime, new MockUserStatus("fml")));
                MockUser fromJson1 = parser.fromJson(toJson1, MockUser.class);
                MockUser fromJson2 = parser.fromJson(toJson2, MockUser.class);
                String totoJson1 = parser.toJson(fromJson1);
                String totoJson2 = parser.toJson(fromJson2);
                car.goal(totoJson1 + totoJson2);
            }
        }, new CannonballOption().expectSameResult().threadCount(20).repeatCount(30));
    }

    public static class MockUser {
        public String id;
        public String name;
        public MockUserStatus status;
        public LocalDate birthdate;
        public LocalDateTime formalizedDatetime;

        public MockUser() {
        }

        public MockUser(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public MockUser(String id, String name, MockUserStatus status) {
            this.id = id;
            this.name = name;
            this.status = status;
        }

        public MockUser(String id, String name, LocalDate birthdate, LocalDateTime formalizedDatetime, MockUserStatus status) {
            this.id = id;
            this.name = name;
            this.status = status;
            this.birthdate = birthdate;
            this.formalizedDatetime = formalizedDatetime;
        }

        @Override
        public String toString() {
            return "{" + id + ", " + name + ", " + status + "}";
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
