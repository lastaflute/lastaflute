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
package org.lastaflute.web.validation;

import java.util.Map;
import java.util.Set;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.web.ruts.message.ActionMessages;
import org.lastaflute.web.validation.exception.ValidationSuccessAttributeCannotCastException;
import org.lastaflute.web.validation.exception.ValidationSuccessAttributeNotFoundException;

/**
 * @author jflute
 * @since 0.6.0 (2015/07/02 Thursday)
 */
public class ValidationSuccess {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ActionMessages messages;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ValidationSuccess(ActionMessages messages) {
        assertArgumentNotNull("messages", messages);
        this.messages = messages;
    }

    // ===================================================================================
    //                                                                   Success Attribute
    //                                                                   =================
    // second argument 'attributeType' is to write like this:
    // getAttribute("sea", SeaBean.class).ifPresent(seaBean -> ...)
    /**
     * Get the attribute value of the validation success by the key. <br>
     * Basically saved by ActionMessages#saveSuccessAttribute().
     * @param <ATTRIBUTE> The type of attribute object.
     * @param key The string key of attribute saved in the scope. (NotNull)
     * @param attributeType The generic type of the result for the attribute. (NotNull)
     * @return The optional attribute object for the key. (NotNull, EmptyAllowed: when not found)
     */
    public <ATTRIBUTE> OptionalThing<ATTRIBUTE> getAttribute(String key, Class<ATTRIBUTE> attributeType) {
        assertArgumentNotNull("key", key);
        assertArgumentNotNull("attributeType", attributeType);
        final Object original = findSuccessAttribute(key);
        final ATTRIBUTE attribute;
        if (original != null) {
            try {
                attribute = attributeType.cast(original);
            } catch (ClassCastException e) {
                final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
                br.addNotice("Cannot cast the validation success attribute");
                br.addItem("Attribute Key");
                br.addElement(key);
                br.addItem("Specified Type");
                br.addElement(attributeType);
                br.addItem("Existing Attribute");
                br.addElement(original.getClass());
                br.addElement(original);
                br.addItem("Attribute Map");
                br.addElement(getSuccessAttributeMap());
                final String msg = br.buildExceptionMessage();
                throw new ValidationSuccessAttributeCannotCastException(msg);
            }
        } else {
            attribute = null;
        }
        return OptionalThing.ofNullable(attribute, () -> {
            final Set<String> keySet = getSuccessAttributeMap().keySet();
            final String msg = "Not found the validation success attribute by the string key: " + key + " existing=" + keySet;
            throw new ValidationSuccessAttributeNotFoundException(msg);
        });
    }

    @SuppressWarnings("unchecked")
    protected <ATTRIBUTE> ATTRIBUTE findSuccessAttribute(String key) {
        return (ATTRIBUTE) getSuccessAttributeMap().get(key);
    }

    protected Map<String, Object> getSuccessAttributeMap() {
        return messages.getSuccessAttributeMap();
    }

    // same reason as request/session attribute
    ///**
    // * Get the attribute value of the validation success by the value's type. <br>
    // * Basically saved by ActionMessages#saveSuccessAttribute().
    // * @param <ATTRIBUTE> The type of attribute object.
    // * @param typeKey The type key of attribute saved in the scope. (NotNull)
    // * @return The optional attribute object for the type. (NotNull, EmptyAllowed: when not found)
    // */
    //public <ATTRIBUTE> OptionalThing<ATTRIBUTE> getAttribute(Class<ATTRIBUTE> typeKey) {
    //    assertArgumentNotNull("typeKey", typeKey);
    //    final String key = typeKey.getName();
    //    final ATTRIBUTE attribute = findSuccessAttribute(key);
    //    return OptionalThing.ofNullable(attribute, () -> {
    //        final Set<String> keySet = getSuccessAttributeMap().keySet();
    //        final String msg = "Not found the validation success attribute by the typed key: " + key + " existing=" + keySet;
    //        throw new ValidationSuccessAttributeNotFoundException(msg);
    //    });
    //}

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
}
