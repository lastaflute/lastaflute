/*
 * Copyright 2015-2016 the original author or authors.
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
package org.lastaflute.web.ruts.wrapper;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.PropertyDesc;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;
import org.lastaflute.web.util.LaParamWrapperUtil;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class BeanWrapper implements Map<String, Object> {

    private static final char INDEXED_DELIM = '[';
    private static final char INDEXED_DELIM2 = ']';

    protected final Object bean;
    protected final BeanDesc beanDesc;

    public BeanWrapper(Object bean) {
        this.bean = bean;
        beanDesc = BeanDescFactory.getBeanDesc(bean.getClass());
    }

    public Object get(Object objKey) {
        if (objKey == null) {
            throw new IllegalArgumentException("The key parameter must not be null.");
        }
        final String key = objKey.toString();
        final int pos = key.lastIndexOf(INDEXED_DELIM);
        if (pos > 0) {
            final int endPos = key.lastIndexOf(INDEXED_DELIM2);
            if (pos >= endPos) {
                throw new IllegalArgumentException("The key(" + key + ") is invalid.");
            }
            final int index = Integer.valueOf(key.substring(pos + 1, endPos));
            final String name = key.substring(0, pos);
            final Object value;
            final Class<?> type;
            if (name.indexOf(INDEXED_DELIM) > 0) {
                value = get(name);
                type = value.getClass();
            } else {
                PropertyDesc pd = beanDesc.getPropertyDesc(name);
                if (!pd.isReadable()) {
                    return null;
                }
                value = pd.getValue(bean);
                if (value == null) {
                    return null;
                }
                type = pd.getPropertyType();
            }
            if (type.isArray()) {
                return LaParamWrapperUtil.convert(Array.get(value, index));
            }
            if (List.class.isAssignableFrom(type)) {
                return LaParamWrapperUtil.convert(((List<?>) value).get(index));
            }
            throw new IllegalStateException("Index property must be an array or a list.");
        }
        final PropertyDesc pd = beanDesc.getPropertyDesc(key);
        if (!pd.isReadable()) {
            return null;
        }
        return LaParamWrapperUtil.convert(pd.getValue(bean));
    }

    public String put(String key, Object value) {
        final PropertyDesc pd = beanDesc.getPropertyDesc(key.toString());
        pd.setValue(bean, value);
        return null;
    }

    public void clear() {
        throw new UnsupportedOperationException("clear");
    }

    public boolean containsKey(Object key) {
        if (key == null) {
            return false;
        }
        return beanDesc.hasPropertyDesc(key.toString());
    }

    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("containsValue");
    }

    public Set<Entry<String, Object>> entrySet() {
        final Set<Entry<String, Object>> set = new HashSet<Entry<String, Object>>();
        final int size = beanDesc.getPropertyDescSize();
        for (int i = 0; i < size; i++) {
            final PropertyDesc pd = beanDesc.getPropertyDesc(i);
            set.add(new BeanEntry(bean, pd));
        }
        return set;
    }

    public boolean isEmpty() {
        throw new UnsupportedOperationException("isEmpty");
    }

    public Set<String> keySet() {
        Set<String> set = new HashSet<String>();
        int size = beanDesc.getPropertyDescSize();
        for (int i = 0; i < size; i++) {
            PropertyDesc pd = beanDesc.getPropertyDesc(i);
            set.add(pd.getPropertyName());
        }
        return set;
    }

    public void putAll(Map<? extends String, ? extends Object> t) {
        throw new UnsupportedOperationException("putAll");
    }

    public String remove(Object key) {
        return put((String) key, null);
    }

    public int size() {
        return beanDesc.getPropertyDescSize();
    }

    public Collection<Object> values() {
        throw new UnsupportedOperationException("values");
    }

    @Override
    public String toString() {
        return bean.toString();
    }

    protected static class BeanEntry implements Entry<String, Object> {

        protected PropertyDesc propDesc;
        protected Object bean;

        public BeanEntry(Object bean, PropertyDesc propDesc) {
            this.propDesc = propDesc;
            this.bean = bean;
        }

        public String getKey() {
            return propDesc.getPropertyName();
        }

        public Object getValue() {
            if (!propDesc.isReadable()) {
                return null;
            }
            return LaParamWrapperUtil.convert(propDesc.getValue(bean));
        }

        public Object setValue(Object value) {
            propDesc.setValue(bean, value);
            return null;
        }
    }
}