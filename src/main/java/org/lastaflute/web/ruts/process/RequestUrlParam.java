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
package org.lastaflute.web.ruts.process;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author jflute
 */
public class RequestUrlParam {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final List<Class<?>> urlParamTypeList; // not null, read-only
    protected final Map<Integer, Object> urlParamValueMap; // not null, read-only

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public RequestUrlParam(List<Class<?>> urlParamTypeList, Map<Integer, Object> urlParamValueMap) {
        this.urlParamTypeList = toUnmodifiableList(urlParamTypeList);
        this.urlParamValueMap = toUnmodifiableMap(urlParamValueMap);
    }

    protected List<Class<?>> toUnmodifiableList(List<Class<?>> urlParamTypeList) {
        return Collections.unmodifiableList(urlParamTypeList);
    }

    protected Map<Integer, Object> toUnmodifiableMap(Map<Integer, Object> urlParamValueMap) {
        return Collections.unmodifiableMap(urlParamValueMap);
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return "urlParam:{" + urlParamValueMap + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public List<Class<?>> getUrlParamTypeList() {
        return urlParamTypeList;
    }

    public Map<Integer, Object> getUrlParamValueMap() {
        return urlParamValueMap;
    }
}
