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
package org.lastaflute.web.response.render;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jflute
 */
public class RenderData {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected Map<String, Object> dataMap; // lazy loaded

    // ===================================================================================
    //                                                                            Register
    //                                                                            ========
    /**
     * @param key The key of the data. (NotNull)
     * @param value The value of the data for the key. (NotNull)
     */
    public void register(String key, Object value) {
        assertArgumentNotNull("key", key);
        assertArgumentNotNull("value", value);
        doRegister(key, value);
    }

    protected void doRegister(String key, Object value) {
        if (dataMap == null) {
            dataMap = new HashMap<String, Object>();
        }
        if (dataMap.containsKey(key)) {
            String msg = "Already registered the key: key=" + key + " rejected=" + value;
            throw new IllegalStateException(msg);
        }
        dataMap.put(key, value);
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
    //                                                                            Accessor
    //                                                                            ========
    public Map<String, Object> getDataMap() {
        return dataMap != null ? dataMap : Collections.emptyMap();
    }
}
