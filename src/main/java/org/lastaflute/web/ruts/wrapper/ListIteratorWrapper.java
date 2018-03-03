/*
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
package org.lastaflute.web.ruts.wrapper;

import java.util.ListIterator;

import org.lastaflute.web.util.LaParamWrapperUtil;

/**
 * @param <ELEMENT> The type of element.
 * @author modified by jflute (originated in Seasar)
 */
public class ListIteratorWrapper<ELEMENT> implements ListIterator<ELEMENT> {

    protected ListIterator<ELEMENT> iterator;

    public ListIteratorWrapper(ListIterator<ELEMENT> iterator) {
        this.iterator = iterator;
    }

    public void add(ELEMENT o) {
        iterator.add(o);
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public boolean hasPrevious() {
        return iterator.hasPrevious();
    }

    public ELEMENT next() {
        return LaParamWrapperUtil.convert(iterator.next());
    }

    public int nextIndex() {
        return iterator.nextIndex();
    }

    public ELEMENT previous() {
        return LaParamWrapperUtil.convert(iterator.previous());
    }

    public int previousIndex() {
        return iterator.previousIndex();
    }

    public void remove() {
        iterator.remove();
    }

    public void set(ELEMENT o) {
        iterator.set(o);
    }
}