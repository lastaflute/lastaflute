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

    // supplemental information for e.g. unit test
    protected final Annotation validatorAnnotation; // null allowed (exists if annotation message)
    protected final Class<?>[] validatorGroups; // null allowed (exists if annotation message)
    protected final String validatorMessageKey; // null allowed (even if annotation exists)

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
        this.validatorGroups = null;
        this.validatorMessageKey = null;
    }

    protected UserMessage(String messageItself // direct message (not message key)
            , Annotation validatorAnnotation, Class<?>[] validatorGroups, String validatorMessageKey // annotation info
    ) { // for e.g. hibernate validator
        assertArgumentNotNull("messageItself", messageItself);
        this.messageKey = messageItself;
        this.values = EMPTY_VALUES;
        this.resource = false;
        this.validatorAnnotation = validatorAnnotation;
        this.validatorGroups = validatorGroups;
        this.validatorMessageKey = validatorMessageKey;
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

    // -----------------------------------------------------
    //                                        Factory Method
    //                                        --------------
    // factory methods to avoid argument mistake, and this is internal method for framework
    public static UserMessage asDirectMessage(String messageItself) { // for e.g. remote api
        return new UserMessage(messageItself, null, null, null);
    }

    public static UserMessage asDirectMessage(String messageItself, Annotation validatorAnnotation, Class<?>[] validatorGroups,
            String validatorMessageKey) { // for e.g. hibernate validator
        return new UserMessage(messageItself, validatorAnnotation, validatorGroups, validatorMessageKey);
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
            if (validatorGroups != null) { // just in case
                sb.append(Stream.of(validatorGroups).map(tp -> {
                    return tp.getSimpleName() + ".class";
                }).collect(Collectors.toList()));
            }
            if (validatorMessageKey != null) {
                sb.append(" (").append(filterEmbeddedDomainForDisplay(validatorMessageKey)).append(")");
            }
        }
        return sb.toString();
    }

    protected String filterEmbeddedDomainForDisplay(String key) { // copied from SimpleMessageManager
        final String javaxPackage = "javax.validation.";
        final String hibernatePackage = "org.hibernate.validator.";
        final String lastaflutePackage = "org.lastaflute.validator.";
        final String realKey;
        if (key.startsWith(javaxPackage)) {
            realKey = Srl.substringFirstRear(key, javaxPackage);
        } else if (key.startsWith(hibernatePackage)) {
            realKey = Srl.substringFirstRear(key, hibernatePackage);
        } else if (key.startsWith(lastaflutePackage)) {
            realKey = Srl.substringFirstRear(key, lastaflutePackage);
        } else {
            realKey = key;
        }
        return realKey;
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

    public OptionalThing<Class<?>[]> getValidatorGroups() {
        return OptionalThing.ofNullable(validatorGroups, () -> {
            throw new IllegalStateException("Not found the validator groups: " + toString());
        });
    }

    public OptionalThing<String> getValidatorMessageKey() {
        return OptionalThing.ofNullable(validatorMessageKey, () -> {
            throw new IllegalStateException("Not found the validator message key: " + toString());
        });
    }
}
