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
package org.lastaflute.unit.mock.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dbflute.jdbc.Classification;
import org.dbflute.jdbc.ClassificationCodeType;
import org.dbflute.jdbc.ClassificationMeta;
import org.dbflute.jdbc.ClassificationUndefinedHandlingType;

/**
 * @author jflute
 */
public interface MockCDef extends Classification {

    /** The empty array for no sisters. */
    String[] EMPTY_SISTERS = new String[] {};

    /** The empty map for no sub-items. */
    @SuppressWarnings("unchecked")
    Map<String, Object> EMPTY_SUB_ITEM_MAP = (Map<String, Object>) Collections.EMPTY_MAP;

    public enum Flg implements MockCDef {
        True("1", "Yes", new String[] { "true" }), False("0", "No", new String[] { "false" });
        private static final Map<String, Flg> _codeValueMap = new HashMap<String, Flg>();

        static {
            for (Flg value : values()) {
                _codeValueMap.put(value.code().toLowerCase(), value);
                for (String sister : value.sisterSet()) {
                    _codeValueMap.put(sister.toLowerCase(), value);
                }
            }
        }

        private String _code;
        private String _alias;
        private Set<String> _sisterSet;

        private Flg(String code, String alias, String[] sisters) {
            _code = code;
            _alias = alias;
            _sisterSet = Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(sisters)));
        }

        public String code() {
            return _code;
        }

        public String alias() {
            return _alias;
        }

        public Set<String> sisterSet() {
            return _sisterSet;
        }

        public Map<String, Object> subItemMap() {
            return EMPTY_SUB_ITEM_MAP;
        }

        public ClassificationMeta meta() {
            return MockCDef.DefMeta.Flg;
        }

        public boolean inGroup(String groupName) {
            return false;
        }

        /**
         * Get the classification by the code. (CaseInsensitive)
         * @param code The value of code, which is case-insensitive. (NullAllowed: if null, returns null)
         * @return The instance of the corresponding classification to the code. (NullAllowed: if not found, returns null)
         */
        public static Flg codeOf(Object code) {
            if (code == null) {
                return null;
            }
            if (code instanceof Flg) {
                return (Flg) code;
            }
            return _codeValueMap.get(code.toString().toLowerCase());
        }

        /**
         * Get the classification by the name (also called 'value' in ENUM world).
         * @param name The string of name, which is case-sensitive. (NullAllowed: if null, returns null)
         * @return The instance of the corresponding classification to the name. (NullAllowed: if not found, returns null)
         */
        public static Flg nameOf(String name) {
            if (name == null) {
                return null;
            }
            try {
                return valueOf(name);
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        /**
         * Get the list of all classification elements. (returns new copied list)
         * @return The list of all classification elements. (NotNull)
         */
        public static List<Flg> listAll() {
            return new ArrayList<Flg>(Arrays.asList(values()));
        }

        /**
         * Get the list of classification elements in the specified group. (returns new copied list) <br>
         * @param groupName The string of group name, which is case-sensitive. (NullAllowed: if null, returns empty list)
         * @return The list of classification elements in the group. (NotNull)
         */
        public static List<Flg> groupOf(String groupName) {
            return new ArrayList<Flg>(4);
        }

        @Override
        public String toString() {
            return code();
        }
    }

    public enum MemberStatus implements MockCDef {
        Formalized("FML", "Formalized", EMPTY_SISTERS), Withdrawal("WDL", "Withdrawal", EMPTY_SISTERS), Provisional("PRV", "Provisional",
                EMPTY_SISTERS);
        private static final Map<String, MemberStatus> _codeValueMap = new HashMap<String, MemberStatus>();

        static {
            for (MemberStatus value : values()) {
                _codeValueMap.put(value.code().toLowerCase(), value);
                for (String sister : value.sisterSet()) {
                    _codeValueMap.put(sister.toLowerCase(), value);
                }
            }
        }

        private String _code;
        private String _alias;
        private Set<String> _sisterSet;

        private MemberStatus(String code, String alias, String[] sisters) {
            _code = code;
            _alias = alias;
            _sisterSet = Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(sisters)));
        }

        public String code() {
            return _code;
        }

        public String alias() {
            return _alias;
        }

        public Set<String> sisterSet() {
            return _sisterSet;
        }

        public Map<String, Object> subItemMap() {
            return EMPTY_SUB_ITEM_MAP;
        }

        public ClassificationMeta meta() {
            return MockCDef.DefMeta.MemberStatus;
        }

        /**
         * Is the classification in the group? <br>
         * Members that can use the service, can sign in <br>
         * The group elements:[Formalized, Provisional]
         * @return The determination, true or false.
         */
        public boolean isServiceAvailable() {
            return Formalized.equals(this) || Provisional.equals(this);
        }

        /**
         * Is the classification in the group? <br>
         * Members are not formalized yet <br>
         * The group elements:[Provisional]
         * @return The determination, true or false.
         */
        public boolean isShortOfFormalized() {
            return Provisional.equals(this);
        }

        public boolean inGroup(String groupName) {
            if ("serviceAvailable".equals(groupName)) {
                return isServiceAvailable();
            }
            if ("shortOfFormalized".equals(groupName)) {
                return isShortOfFormalized();
            }
            return false;
        }

        /**
         * Get the classification by the code. (CaseInsensitive)
         * @param code The value of code, which is case-insensitive. (NullAllowed: if null, returns null)
         * @return The instance of the corresponding classification to the code. (NullAllowed: if not found, returns null)
         */
        public static MemberStatus codeOf(Object code) {
            if (code == null) {
                return null;
            }
            if (code instanceof MemberStatus) {
                return (MemberStatus) code;
            }
            return _codeValueMap.get(code.toString().toLowerCase());
        }

        /**
         * Get the classification by the name (also called 'value' in ENUM world).
         * @param name The string of name, which is case-sensitive. (NullAllowed: if null, returns null)
         * @return The instance of the corresponding classification to the name. (NullAllowed: if not found, returns null)
         */
        public static MemberStatus nameOf(String name) {
            if (name == null) {
                return null;
            }
            try {
                return valueOf(name);
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        /**
         * Get the list of all classification elements. (returns new copied list)
         * @return The list of all classification elements. (NotNull)
         */
        public static List<MemberStatus> listAll() {
            return new ArrayList<MemberStatus>(Arrays.asList(values()));
        }

        /**
         * Get the list of group classification elements. (returns new copied list) <br>
         * Members that can use the service, can sign in <br>
         * The group elements:[Formalized, Provisional]
         * @return The list of classification elements in the group. (NotNull)
         */
        public static List<MemberStatus> listOfServiceAvailable() {
            return new ArrayList<MemberStatus>(Arrays.asList(Formalized, Provisional));
        }

        /**
         * Get the list of group classification elements. (returns new copied list) <br>
         * Members are not formalized yet <br>
         * The group elements:[Provisional]
         * @return The list of classification elements in the group. (NotNull)
         */
        public static List<MemberStatus> listOfShortOfFormalized() {
            return new ArrayList<MemberStatus>(Arrays.asList(Provisional));
        }

        /**
         * Get the list of classification elements in the specified group. (returns new copied list) <br>
         * @param groupName The string of group name, which is case-sensitive. (NullAllowed: if null, returns empty list)
         * @return The list of classification elements in the group. (NotNull)
         */
        public static List<MemberStatus> groupOf(String groupName) {
            if ("serviceAvailable".equals(groupName)) {
                return listOfServiceAvailable();
            }
            if ("shortOfFormalized".equals(groupName)) {
                return listOfShortOfFormalized();
            }
            return new ArrayList<MemberStatus>(4);
        }

        @Override
        public String toString() {
            return code();
        }
    }

    public enum DefMeta implements ClassificationMeta {
        Flg, MemberStatus, ServiceRank, Region, WithdrawalReason, ProductCategory, ProductStatus;
        public String classificationName() {
            return name(); // same as definition name
        }

        public Classification codeOf(Object code) {
            if ("Flg".equals(name())) {
                return MockCDef.Flg.codeOf(code);
            }
            if ("MemberStatus".equals(name())) {
                return MockCDef.MemberStatus.codeOf(code);
            }
            throw new IllegalStateException("Unknown definition: " + this); // basically unreachable
        }

        public Classification nameOf(String name) {
            if ("Flg".equals(name())) {
                return MockCDef.Flg.valueOf(name);
            }
            if ("MemberStatus".equals(name())) {
                return MockCDef.MemberStatus.valueOf(name);
            }
            throw new IllegalStateException("Unknown definition: " + this); // basically unreachable
        }

        public List<Classification> listAll() {
            if ("Flg".equals(name())) {
                return toClassificationList(MockCDef.Flg.listAll());
            }
            if ("MemberStatus".equals(name())) {
                return toClassificationList(MockCDef.MemberStatus.listAll());
            }
            throw new IllegalStateException("Unknown definition: " + this); // basically unreachable
        }

        public List<Classification> groupOf(String groupName) {
            if ("Flg".equals(name())) {
                return toClassificationList(MockCDef.Flg.groupOf(groupName));
            }
            if ("MemberStatus".equals(name())) {
                return toClassificationList(MockCDef.MemberStatus.groupOf(groupName));
            }
            throw new IllegalStateException("Unknown definition: " + this); // basically unreachable
        }

        @SuppressWarnings("unchecked")
        private List<Classification> toClassificationList(List<?> clsList) {
            return (List<Classification>) clsList;
        }

        public ClassificationCodeType codeType() {
            if ("Flg".equals(name())) {
                return ClassificationCodeType.Number;
            }
            if ("MemberStatus".equals(name())) {
                return ClassificationCodeType.String;
            }
            return ClassificationCodeType.String; // as default
        }

        public ClassificationUndefinedHandlingType undefinedHandlingType() {
            if ("Flg".equals(name())) {
                return ClassificationUndefinedHandlingType.EXCEPTION;
            }
            if ("MemberStatus".equals(name())) {
                return ClassificationUndefinedHandlingType.EXCEPTION;
            }
            return ClassificationUndefinedHandlingType.LOGGING; // as default
        }
    }
}
