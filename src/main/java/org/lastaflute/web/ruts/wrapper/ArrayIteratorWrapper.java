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
package org.lastaflute.web.ruts.wrapper;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.lastaflute.web.util.LaParamWrapperUtil;

/**
 * @param <ELEMENT> The type of array element.
 * @author modified by jflute (originated in Seasar)
 */
public class ArrayIteratorWrapper<ELEMENT> implements Iterator<ELEMENT> {

    protected Object array;
    protected int length;
    protected int index;

    public ArrayIteratorWrapper(Object array) {
        this.array = array;
        length = Array.getLength(array);
    }

    public ELEMENT next() {
        if (!hasNext()) {
            throw new NoSuchElementException("length:" + length + ", index:" + index);
        }
        return LaParamWrapperUtil.convert(Array.get(array, index++));
    }

    public boolean hasNext() {
        return index < length;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }
}