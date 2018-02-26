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
package org.lastaflute.web.ruts.process.populate;

/**
 * @author jflute
 * @since 0.7.0 (2015/12/05 Saturday)
 */
public class FormSimpleTextParameterMeta {

    protected final String propertyName; // not null
    protected final Class<?> propertyType; // not null

    public FormSimpleTextParameterMeta(String propertyName, Class<?> propertyType) {
        this.propertyName = propertyName;
        this.propertyType = propertyType;
    }

    @Override
    public String toString() {
        return "simpleText:{" + propertyName + ", " + propertyType + "}";
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Class<?> getPropertyType() {
        return propertyType;
    }
}
