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
package org.lastaflute.core.message;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;

/**
 * @author jflute
 */
public class UserMessage implements Serializable {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = 1L;
    protected static final String BEGIN_MARK = "{";
    protected static final String END_MARK = "}";
    protected static final Object[] EMPTY_VALUES = new Object[0];

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String messageKey; // not null, key or direct message
    protected final Object[] values; // not null, empty allowed when no parameter or direct message
    protected final boolean resource; // basically true here, false if direct message for framework

    // supplemental information
    protected final Annotation validatorAnnotation; // null allowed
    protected final Class<?>[] annotatedGroups; // null allowed

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public UserMessage(String messageKey, Object... values) { // for e.g. typesafe messages
        assertArgumentNotNull("messageKey", messageKey);
        assertArgumentNotNull("values", values);
        this.messageKey = filterMessageKey(messageKey);
        this.values = values != null ? filterValues(values) : EMPTY_VALUES;
        this.resource = true;
        this.validatorAnnotation = null;
        this.annotatedGroups = null;
    }

    protected UserMessage(String messageKey, Annotation validatorAnnotation, Class<?>[] annotatedGroups) { // for e.g. hibernate validator
        assertArgumentNotNull("messageKey", messageKey);
        assertArgumentNotNull("validatorAnnotation", validatorAnnotation);
        assertArgumentNotNull("annotationGroups", annotatedGroups);
        this.messageKey = messageKey;
        this.values = EMPTY_VALUES;
        this.resource = false;
        this.validatorAnnotation = validatorAnnotation;
        this.annotatedGroups = annotatedGroups;
    }

    // factory method to avoid argument mistake, and this is internal method for framework
    public static UserMessage asDirectMessage(String key, Annotation validatorAnnotation, Class<?>[] annotatedGroups) {
        return new UserMessage(key, validatorAnnotation, annotatedGroups);
    }

    protected String filterMessageKey(String messageKey) {
        return unquoteBracesIfQuoted(messageKey);
    }

    protected Object[] filterValues(Object[] values) {
        final Object[] filtered = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            final Object val = values[i];
            filtered[i] = val instanceof String ? unquoteBracesIfQuoted((String) val) : val;
        }
        return filtered;
    }

    protected String unquoteBracesIfQuoted(String key) {
        final String beginMark = BEGIN_MARK;
        final String endMark = END_MARK;
        if (Srl.isQuotedAnything(key, beginMark, endMark)) {
            return Srl.unquoteAnything(key, beginMark, endMark); // remove Hibernate Validator's braces
        } else {
            return key;
        }
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            throw new IllegalArgumentException("The variableName should not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
        }
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(messageKey);
        sb.append("[");
        for (int i = 0; i < values.length; i++) {
            sb.append(values[i]);
            if (i < values.length - 1) { // don't append comma to last entry
                sb.append(", ");
            }
        }
        sb.append("]");
        if (validatorAnnotation != null) { // because of option
            sb.append(" by @").append(validatorAnnotation.annotationType().getSimpleName());
            if (annotatedGroups != null) { // just in case
                sb.append(Stream.of(annotatedGroups).map(tp -> {
                    return tp.getSimpleName() + ".class";
                }).collect(Collectors.toList()));
            }
        }
        return sb.toString();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getMessageKey() { // not null
        return messageKey;
    }

    public Object[] getValues() { // not null
        return values;
    }

    public boolean isResource() {
        return resource;
    }

    public OptionalThing<Annotation> getValidatorAnnotation() {
        return OptionalThing.ofNullable(validatorAnnotation, () -> {
            throw new IllegalStateException("Not found the validator annotation: " + toString());
        });
    }

    public OptionalThing<Class<?>[]> getAnnotatedGroups() {
        return OptionalThing.ofNullable(annotatedGroups, () -> {
            throw new IllegalStateException("Not found the annotated groups: " + toString());
        });
    }
}
