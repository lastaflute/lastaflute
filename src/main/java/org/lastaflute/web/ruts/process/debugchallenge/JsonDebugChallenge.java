/*
 * Copyright 2015-2018 the original author or authors.
 * Copyright 2015-2018 the original author or authors.
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
package org.lastaflute.web.ruts.process.debugchallenge;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.util.LaClassificationUtil;
import org.lastaflute.core.util.LaClassificationUtil.ClassificationUnknownCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class JsonDebugChallenge {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(JsonDebugChallenge.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final JsonManager jsonManager;
    protected final String propertyName;
    protected final Class<?> propertyType;
    protected final Object mappedValue; // null allowed
    protected final Integer elementIndex; // null allowed, exists if the property is on bean in list

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public JsonDebugChallenge(JsonManager jsonManager, String propertyName, Class<?> propertyType, Object mappedValue,
            Integer elementIndex) {
        this.jsonManager = jsonManager;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.mappedValue = filterMappedValue(propertyType, mappedValue);
        this.elementIndex = elementIndex;
    }

    protected Object filterMappedValue(Class<?> propertyType, Object mappedValue) {
        if (mappedValue == null) {
            return null;
        }
        final Class<?> valueType = mappedValue.getClass();
        if (propertyType.isAssignableFrom(valueType)) { // e.g. String to String
            return mappedValue;
        }
        try {
            if (mappedValue instanceof String) {
                final String plainStr = (String) mappedValue;
                if (LocalDate.class.isAssignableFrom(propertyType)) {
                    return parseStr("seaDate", plainStr, bean -> bean.seaDate);
                } else if (LocalDateTime.class.isAssignableFrom(propertyType)) {
                    return parseStr("seaDateTime", plainStr, bean -> bean.seaDateTime);
                } else if (LocalTime.class.isAssignableFrom(propertyType)) {
                    return parseStr("seaTime", plainStr, bean -> bean.seaTime);
                } else if (LaClassificationUtil.isCls(propertyType)) {
                    return parseCls(propertyType, plainStr);
                }
            } else if (mappedValue instanceof Double) { // Gson converts to double if "20" when map
                final Double plainDouble = (Double) mappedValue;
                if (Integer.class.isAssignableFrom(propertyType)) {
                    if (((double) plainDouble.intValue()) == plainDouble.doubleValue()) { // no digits
                        return Integer.valueOf(plainDouble.intValue());
                    }
                } else if (Long.class.isAssignableFrom(propertyType)) {
                    if (((double) plainDouble.longValue()) == plainDouble.doubleValue()) { // no digits
                        return Long.valueOf(plainDouble.longValue());
                    }
                } else if (BigDecimal.class.isAssignableFrom(propertyType)) {
                    try {
                        return new BigDecimal(plainDouble.toString()); // to avoid long digits
                    } catch (RuntimeException ignored) { // just in case
                        return new BigDecimal(plainDouble); // allow long digits
                    }
                }
            }
        } catch (RuntimeException continued) { // just in case
            String msg = "*Cannot filter the mapped value: mappedValue={}, valueType={}";
            logger.debug(msg, mappedValue, valueType, continued);
        }
        return mappedValue;
    }

    // ===================================================================================
    //                                                                     Checking Helper
    //                                                                     ===============
    protected Object parseStr(String key, String plainStr, Function<MappingCheckingBean, Object> parsedProvider) {
        return fromJson(jsonManager, key, plainStr).map(bean -> parsedProvider.apply(bean)).orElse(plainStr);
    }

    protected Object parseCls(Class<?> propertyType, String plainStr) {
        try {
            return LaClassificationUtil.toCls(propertyType, plainStr);
        } catch (ClassificationUnknownCodeException e) { // simple message because of catched later
            return plainStr;
        }
    }

    protected OptionalThing<MappingCheckingBean> fromJson(JsonManager jsonManager, String key, String plainStr) {
        final String json = "{\"" + key + "\":\"" + plainStr + "\"}";
        try {
            return OptionalThing.of(jsonManager.fromJson(json, MappingCheckingBean.class));
        } catch (RuntimeException ignored) {
            return OptionalThing.empty();
        }
    }

    public static class MappingCheckingBean {

        public LocalDate seaDate;
        public LocalDateTime seaDateTime;
        public LocalTime seaTime;
    }

    // ===================================================================================
    //                                                                   Challenge Display
    //                                                                   =================
    public String toChallengeDisp() {
        final StringBuilder sb = new StringBuilder();
        sb.append("\n ").append(toTypeAssignableMark()).append(" ");
        sb.append(propertyType.getSimpleName()).append(" ").append(propertyName);
        if (mappedValue != null) {
            final Class<?> valueType = getValueType().get();
            if (isNestedProperty(valueType)) {
                sb.append(" = ... (unknown if assignable)"); // nested is unsupported about challenge for now (so difficult)
            } else {
                sb.append(" = (").append(valueType.getSimpleName()).append(") \"").append(mappedValue).append("\"");
            }
        } else {
            sb.append(" = null");
        }
        return sb.toString();
    }

    public String toTypeAssignableMark() {
        return getValueType().map(valueType -> {
            if (isNestedProperty(valueType)) {
                return "?";
            } else {
                return isAssignableFrom(valueType) ? "o" : "x";
            }
        }).orElse("v");
    }

    protected boolean isNestedProperty(Class<?> valueType) {
        return List.class.isAssignableFrom(valueType) || Map.class.isAssignableFrom(valueType);
    }

    protected boolean isAssignableFrom(Class<?> valueType) {
        return propertyType.isAssignableFrom(valueType);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getPropertyName() {
        return propertyName;
    }

    public Class<?> getPropertyType() {
        return propertyType;
    }

    public OptionalThing<Object> getMappedValue() {
        return OptionalThing.ofNullable(mappedValue, () -> {
            throw new IllegalStateException("Not found the mapped value: property=" + propertyName);
        });
    }

    public OptionalThing<Class<?>> getValueType() {
        return OptionalThing.ofNullable(mappedValue != null ? mappedValue.getClass() : null, () -> {
            throw new IllegalStateException("Not found the mapped value: property=" + propertyName);
        });
    }

    public OptionalThing<Integer> getElementIndex() {
        return OptionalThing.ofNullable(elementIndex, () -> {
            throw new IllegalStateException("Not found the element index: property=" + propertyName);
        });
    }
}
