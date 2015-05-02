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
package org.lastaflute.web.ruts.wrapper;

import java.util.Iterator;

import org.lastaflute.web.util.LaParamWrapperUtil;

/**
 * @param <ELEMENT> The type of element.
 * @author modified by jflute (originated in Seasar)
 */
public class IteratorWrapper<ELEMENT> implements Iterator<ELEMENT> {

    protected final Iterator<ELEMENT> iterator;

    public IteratorWrapper(Iterator<ELEMENT> iterator) {
        this.iterator = iterator;
    }

    public ELEMENT next() {
        return LaParamWrapperUtil.convert(iterator.next());
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public void remove() {
        iterator.remove();
    }
}