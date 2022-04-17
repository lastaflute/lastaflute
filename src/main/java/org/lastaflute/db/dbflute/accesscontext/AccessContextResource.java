/*
 * Copyright 2015-2022 the original author or authors.
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
package org.lastaflute.db.dbflute.accesscontext;

import java.lang.reflect.Method;
import java.util.Map;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 */
public class AccessContextResource {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The name of access module. (NotNull) */
    protected final String moduleName;

    /** The method object of access process. (NullAllowed: means unknown) */
    protected final Method method;

    /** The map of runtime attributes from e.g. ActionRuntime, LaJobRuntime. (NotNull, EmptyAllowed: not required) */
    protected final Map<String, Object> runtimeAttributeMap; // final weapon for access context

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    /**
     * @param moduleName The name of access module. (NotNull)
     * @param method The method object of access process. (NullAllowed: means unknown)
     * @param runtimeAttributeMap The map of runtime values from e.g. ActionRuntime, LaJobRuntime. (NotNull, EmptyAllowed: not required)
     */
    public AccessContextResource(String moduleName, Method method, Map<String, Object> runtimeAttributeMap) {
        this.moduleName = moduleName;
        this.method = method;
        this.runtimeAttributeMap = runtimeAttributeMap;
    }

    // ===================================================================================
    //                                                                   Runtime Attribute
    //                                                                   =================
    /**
     * Retrieve the runtime attribute by the specified type. <br>
     * Basically from ActionRuntime if web world, and from LaJobRuntime if job world. <br>
     * However you should not depend on this existence, so it returns optional.
     * @param key The key of the attribute. (NotNull)
     * @param attributeType The type of runtime attribute for typed result. (NotNull)
     * @return The optional runtime attribute as the type. (NotNull, EmptyAllowed: not found)
     * @throws ClassCastException When the actual attribute cannot be cast to the specified type.
     */
    public <ATTR> OptionalThing<ATTR> retrieveRuntimeAttribute(String key, Class<ATTR> attributeType) {
        if (key == null) {
            throw new IllegalArgumentException("The argument 'key' should not be null.");
        }
        if (attributeType == null) {
            throw new IllegalArgumentException("The argument 'attributeType' should not be null.");
        }
        final Object value = runtimeAttributeMap.get(key);
        if (value != null && !attributeType.isAssignableFrom(value.getClass())) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Cannot cast the attribute by the type.");
            br.addItem("Specified Type");
            br.addElement(attributeType.getSimpleName());
            br.addItem("Actual Value");
            br.addElement(value);
            br.addElement(value.getClass());
            final String msg = br.buildExceptionMessage();
            throw new ClassCastException(msg);
        }
        @SuppressWarnings("unchecked")
        final ATTR attribute = (ATTR) value;
        return OptionalThing.ofNullable(attribute, () -> {
            throw new IllegalStateException("Not found the attribute: key=" + key);
        });
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    /**
     * @return The name of access module. (NotNull)
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * @return The method object of access process. (NullAllowed: means unknown)
     */
    public Method getMethod() {
        return method;
    }
}
