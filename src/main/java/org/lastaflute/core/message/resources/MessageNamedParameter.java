/*
 * Copyright 2015-2021 the original author or authors.
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
package org.lastaflute.core.message.resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;

/**
 * The parameter value for user message as named variable. <br>
 * Basically for UserMessage's argument values.
 * 
 * <p>Created to resolve mismatch between failure client-message and UserMessage interface.</p>
 * 
 * @author jflute
 * @since 1.2.3 (2021/10/28 Thursday)
 */
public class MessageNamedParameter {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String name; // not null
    protected final Object value; // null allowed

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public MessageNamedParameter(String name, Object value) {
        if (name == null) {
            throw new IllegalArgumentException("The argument 'name' should not be null.");
        }
        this.name = name;
        this.value = value;
    }

    // -----------------------------------------------------
    //                                        Facade Factory
    //                                        --------------
    public static List<MessageNamedParameter> listOf(Map<String, Object> namedValueMap) { // read-only
        if (namedValueMap == null) {
            throw new IllegalArgumentException("The argument 'namedValueMap' should not be null.");
        }
        final List<MessageNamedParameter> parameterList = new ArrayList<MessageNamedParameter>();
        namedValueMap.forEach((name, value) -> {
            parameterList.add(new MessageNamedParameter(name, value));
        });
        return Collections.unmodifiableList(parameterList);
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return DfTypeUtil.toClassTitle(this) + ":{" + name + ", " + value + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getName() {
        return name;
    }

    public OptionalThing<Object> getValue() {
        return OptionalThing.ofNullable(value, () -> {
            throw new IllegalStateException("Not found the value: name=" + name);
        });
    }
}
