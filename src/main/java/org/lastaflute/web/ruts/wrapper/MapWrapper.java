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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.lastaflute.web.util.LaParamWrapperUtil;

/**
 * @param <KEY> The type of key.
 * @param <VALUE> The type of value.
 * @author modified by jflute (originated in Seasar)
 */
public class MapWrapper<KEY, VALUE> implements Map<KEY, VALUE> {

    protected final Map<KEY, VALUE> map;

    public MapWrapper(Map<KEY, VALUE> map) {
        this.map = map;
    }

    public VALUE get(Object key) {
        return LaParamWrapperUtil.convert(map.get(key));
    }

    public VALUE put(KEY key, VALUE value) {
        return map.put(key, value);
    }

    public void clear() {
        map.clear();
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    public Set<Entry<KEY, VALUE>> entrySet() {
        return map.entrySet();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Set<KEY> keySet() {
        return map.keySet();
    }

    public void putAll(Map<? extends KEY, ? extends VALUE> t) {
        map.putAll(t);
    }

    public VALUE remove(Object key) {
        return map.remove(key);
    }

    public int size() {
        return map.size();
    }

    public Collection<VALUE> values() {
        return map.values();
    }

    @Override
    public String toString() {
        return map.toString();
    }
}