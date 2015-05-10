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
import java.util.ArrayList;
import java.util.List;

import org.dbflute.utflute.core.PlainTestCase;
import org.dbflute.utflute.core.cannonball.CannonballCar;
import org.dbflute.utflute.core.cannonball.CannonballOption;
import org.dbflute.utflute.core.cannonball.CannonballRun;

import com.google.gson.Gson;

/**
 * @author jflute
 */
public class GsonJsonParserTest extends PlainTestCase {

    public void test_threadSafe() throws Exception {
        // ## Arrange ##
        List<MockUser> userList1 = new ArrayList<MockUser>();
        {
            userList1.add(new MockUser("1", "sea"));
            userList1.add(new MockUser("2", "land", new MockUserStatus("fml")));
        }
        List<MockUser> userList2 = new ArrayList<MockUser>();
        {
            userList2.add(new MockUser("1", "iks"));
            userList2.add(new MockUser("2", "amba", new MockUserStatus("prv")));
        }
        Gson gson = new Gson();
        GsonJsonParser parser = new GsonJsonParser(gson);

        // ## Act ##
        // ## Assert ##
        cannonball(new CannonballRun() {
            public void drive(CannonballCar car) {
                String toJson1 = parser.toJson(new MockUser("1", "sea"));
                String toJson2 = parser.toJson(new MockUser("2", "land", new MockUserStatus("fml")));
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
