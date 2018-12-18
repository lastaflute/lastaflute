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
package org.lastaflute.core.json.control;

import org.dbflute.util.Srl;

/**
 * Basically for framework for now. <br>
 * It's only for keeping fixed control state.
 * @author jflute
 * @since 1.1.1 (2018/12/18 Tuesday at showbase)
 */
public class JsonPrintControlState implements JsonPrintControlMeta {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** Is null property suppressed (not displayed) in output JSON string? */
    protected boolean nullsSuppressed;

    /** Is pretty print suppressed (not line separating) in output JSON string? */
    protected boolean prettyPrintSuppressed;

    // ===================================================================================
    //                                                                      Option Setting
    //                                                                      ==============
    public JsonPrintControlState asNullsSuppressed(boolean nullsSuppressed) {
        this.nullsSuppressed = nullsSuppressed;
        return this;
    }

    public JsonPrintControlState asPrettyPrintSuppressed(boolean prettyPrintSuppressed) {
        this.prettyPrintSuppressed = prettyPrintSuppressed;
        return this;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final String delimiter = ", ";
        if (nullsSuppressed) {
            sb.append(delimiter).append("nullsSuppressed");
        }
        if (prettyPrintSuppressed) {
            sb.append(delimiter).append("prettyPrintSuppressed");
        }
        return "{" + Srl.ltrim(sb.toString(), delimiter) + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public boolean isNullsSuppressed() {
        return nullsSuppressed;
    }

    public boolean isPrettyPrintSuppressed() {
        return prettyPrintSuppressed;
    }
}
