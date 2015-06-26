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
package org.lastaflute.web.util;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lastaflute.web.ruts.VirtualActionForm;
import org.lastaflute.web.ruts.wrapper.ArrayWrapper;
import org.lastaflute.web.ruts.wrapper.BeanWrapper;
import org.lastaflute.web.ruts.wrapper.ListWrapper;
import org.lastaflute.web.ruts.wrapper.MapWrapper;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class LaParamWrapperUtil {

    protected static final Set<Class<?>> simpleTypeSet = new HashSet<Class<?>>();

    static {
        simpleTypeSet.add(String.class);
        simpleTypeSet.add(char.class);
        simpleTypeSet.add(Character.class);
        simpleTypeSet.add(byte.class);
        simpleTypeSet.add(Byte.class);
        simpleTypeSet.add(short.class);
        simpleTypeSet.add(Short.class);
        simpleTypeSet.add(int.class);
        simpleTypeSet.add(Integer.class);
        simpleTypeSet.add(long.class);
        simpleTypeSet.add(Long.class);
        simpleTypeSet.add(float.class);
        simpleTypeSet.add(Float.class);
        simpleTypeSet.add(double.class);
        simpleTypeSet.add(Double.class);
        simpleTypeSet.add(BigInteger.class);
        simpleTypeSet.add(BigDecimal.class);
        simpleTypeSet.add(LocalDate.class);
        simpleTypeSet.add(LocalDateTime.class);
        simpleTypeSet.add(LocalTime.class);
        simpleTypeSet.add(ZonedDateTime.class);
        simpleTypeSet.add(java.sql.Date.class);
        simpleTypeSet.add(java.sql.Time.class);
        simpleTypeSet.add(java.util.Date.class);
        simpleTypeSet.add(Timestamp.class);
        simpleTypeSet.add(Calendar.class);
        simpleTypeSet.add(new byte[0].getClass());
        simpleTypeSet.add(InputStream.class);
        simpleTypeSet.add(boolean.class);
        simpleTypeSet.add(Boolean.class);
    }

    @SuppressWarnings("unchecked")
    public static <WRAPPER> WRAPPER convert(Object value) {
        if (value == null) {
            return null;
        }
        Class<?> clazz = value.getClass();
        if (isSimpleType(clazz)) {
            return (WRAPPER) value;
        }
        if (clazz.isArray()) {
            return (WRAPPER) new ArrayWrapper(value);
        }
        if (VirtualActionForm.class.isAssignableFrom(clazz)) {
            return (WRAPPER) value;
        }
        if (List.class.isAssignableFrom(clazz)) {
            return (WRAPPER) new ListWrapper<Object>((List<Object>) value);
        }
        if (Map.class.isAssignableFrom(clazz)) {
            return (WRAPPER) new MapWrapper<Object, Object>((Map<Object, Object>) value);
        }
        if (clazz.getSuperclass().isEnum()) {
            return (WRAPPER) value;
        }
        return (WRAPPER) new BeanWrapper(value);
    }

    protected static boolean isSimpleType(Class<?> clazz) {
        return simpleTypeSet.contains(clazz);
    }
}
