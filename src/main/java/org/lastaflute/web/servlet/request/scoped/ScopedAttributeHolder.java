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
package org.lastaflute.web.servlet.request.scoped;

import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 */
public interface ScopedAttributeHolder {

    // second argument 'attributeType' is to write like this:
    // getAttribute("sea", SeaBean.class).ifPresent(seaBean -> ...)
    /**
     * Get the attribute value of the scope by the key.
     * @param <ATTRIBUTE> The type of attribute object.
     * @param key The string key of attribute saved in the scope. (NotNull)
     * @param attributeType The generic type of the result for the attribute. (NotNull)
     * @return The optional attribute object for the key. (NotNull, EmptyAllowed: when not found)
     */
    <ATTRIBUTE> OptionalThing<ATTRIBUTE> getAttribute(String key, Class<ATTRIBUTE> attributeType);

    /**
     * Set the attribute value to the scope by your original key.
     * @param key The key of the attribute. (NotNull)
     * @param value The attribute value added to the scope. (NotNull)
     */
    void setAttribute(String key, Object value);

    /**
     * Remove the attribute value by the key.
     * @param key The string key of attribute saved in the scope. (NotNull)
     */
    void removeAttribute(String key);

    // useful but dangerous so remove it at least in first release
    ///**
    // * Get the attribute value of the scope by the value's type.
    // * @param <ATTRIBUTE> The type of attribute object.
    // * @param typeKey The type key of attribute saved in the scope. (NotNull)
    // * @return The optional attribute object for the type. (NotNull, EmptyAllowed: when not found)
    // */
    //<ATTRIBUTE> OptionalThing<ATTRIBUTE> getAttribute(Class<ATTRIBUTE> typeKey);
    ///**
    // * Set the attribute value to the scope by the value's type. <br>
    // * You should not set string object to suppress mistake. <br>
    // * However you should not use this when the object might be extended. <br>
    // * (Then the key is changed to sub-class type so you might have mistakes...)
    // * @param value The attribute value added to the scope. (NotNull)
    // */
    //void setAttribute(Object value);
    ///**
    // * Remove the attribute value by the value's type.
    // * @param type The type of removed object. (NotNull)
    // */
    //void removeAttribute(Class<?> type);
}
