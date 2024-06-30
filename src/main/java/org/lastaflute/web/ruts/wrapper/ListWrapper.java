/*
 * Copyright 2015-2024 the original author or authors.
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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.lastaflute.web.util.LaParamWrapperUtil;

/**
 * @param <ELEMENT> The type of element.
 * @author modified by jflute (originated in Seasar)
 */
public class ListWrapper<ELEMENT> implements List<ELEMENT> {

    protected final List<ELEMENT> list;

    public ListWrapper(List<ELEMENT> list) {
        this.list = list;
    }

    public boolean add(ELEMENT o) {
        return list.add(o);
    }

    public void add(int index, ELEMENT element) {
        list.add(index, element);
    }

    public boolean addAll(Collection<? extends ELEMENT> c) {
        return list.addAll(c);
    }

    public boolean addAll(int index, Collection<? extends ELEMENT> c) {
        return list.addAll(index, c);
    }

    public void clear() {
        list.clear();
    }

    public boolean contains(Object o) {
        return list.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    public ELEMENT get(int index) {
        return LaParamWrapperUtil.convert(list.get(index));
    }

    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public Iterator<ELEMENT> iterator() {
        return new IteratorWrapper<ELEMENT>(list.iterator());
    }

    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    public ListIterator<ELEMENT> listIterator() {
        return new ListIteratorWrapper<ELEMENT>(list.listIterator());
    }

    public ListIterator<ELEMENT> listIterator(int index) {
        return new ListIteratorWrapper<ELEMENT>(list.listIterator(index));
    }

    public boolean remove(Object o) {
        return list.remove(o);
    }

    public ELEMENT remove(int index) {
        return list.remove(index);
    }

    public boolean removeAll(Collection<?> c) {
        return list.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return list.retainAll(c);
    }

    public ELEMENT set(int index, ELEMENT element) {
        return list.set(index, element);
    }

    public int size() {
        return list.size();
    }

    public List<ELEMENT> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    public Object[] toArray() {
        final Object[] array = new Object[size()];
        int i = 0;
        for (Iterator<ELEMENT> ite = iterator(); ite.hasNext();) {
            array[i++] = ite.next();
        }
        return array;
    }

    @SuppressWarnings("unchecked")
    public ELEMENT[] toArray(Object[] a) {
        return (ELEMENT[]) toArray();
    }

    @Override
    public String toString() {
        return list.toString();
    }
}