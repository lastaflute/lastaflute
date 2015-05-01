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
package org.dbflute.lastaflute.web.ruts.message;

import java.io.Serializable;

import org.dbflute.util.Srl;

/**
 * @author modified by jflute (originated in Struts)
 */
public class ActionMessage implements Serializable {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = 1L;
    protected static final String BEGIN_MARK = "{";
    protected static final String END_MARK = "}";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String key;
    protected final Object[] values;
    protected final boolean resource;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionMessage(String key) {
        this(key, (Object[]) null);
    }

    public ActionMessage(String key, Object value0) {
        this(key, new Object[] { value0 });
    }

    public ActionMessage(String key, Object value0, Object value1) {
        this(key, new Object[] { value0, value1 });
    }

    public ActionMessage(String key, Object value0, Object value1, Object value2) {
        this(key, new Object[] { value0, value1, value2 });
    }

    public ActionMessage(String key, Object value0, Object value1, Object value2, Object value3) {
        this(key, new Object[] { value0, value1, value2, value3 });
    }

    public ActionMessage(String key, Object[] values) { // central kitchen
        this.key = filterKey(key);
        this.values = values;
        this.resource = true;
    }

    public ActionMessage(String key, boolean resource) { // basically for direct message
        this.key = resource ? filterKey(key) : key;
        this.values = null;
        this.resource = resource;
    }

    protected String filterKey(String key) {
        final String beginMark = BEGIN_MARK;
        final String endMark = END_MARK;
        if (Srl.isQuotedAnything(key, beginMark, endMark)) {
            return Srl.unquoteAnything(key, beginMark, endMark); // remove Hibernate Validator's braces
        } else {
            return key;
        }
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(key);
        sb.append("[");
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                sb.append(values[i]);
                // don't append comma to last entry
                if (i < values.length - 1) {
                    sb.append(", ");
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getKey() {
        return key;
    }

    public Object[] getValues() {
        return values;
    }

    public boolean isResource() {
        return resource;
    }
}
