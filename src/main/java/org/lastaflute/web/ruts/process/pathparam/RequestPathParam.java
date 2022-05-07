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
package org.lastaflute.web.ruts.process.pathparam;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author jflute
 */
public class RequestPathParam {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final List<Class<?>> pathParamTypeList; // not null, read-only
    protected final Map<Integer, Object> pathParamValueMap; // not null, read-only

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public RequestPathParam(List<Class<?>> pathParamTypeList, Map<Integer, Object> pathParamValueMap) {
        this.pathParamTypeList = toUnmodifiableList(pathParamTypeList);
        this.pathParamValueMap = toUnmodifiableMap(pathParamValueMap);
    }

    protected List<Class<?>> toUnmodifiableList(List<Class<?>> pathParamTypeList) {
        return Collections.unmodifiableList(pathParamTypeList);
    }

    protected Map<Integer, Object> toUnmodifiableMap(Map<Integer, Object> pathParamValueMap) {
        return Collections.unmodifiableMap(pathParamValueMap);
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return "pathParam:{" + pathParamValueMap + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public List<Class<?>> getPathParamTypeList() {
        return pathParamTypeList;
    }

    public Map<Integer, Object> getPathParamValueMap() {
        return pathParamValueMap;
    }
}
