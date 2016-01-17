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
package org.lastaflute.web.ruts.config;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.dbflute.util.DfTypeUtil;

/**
 * @author jflute
 */
public class UrlParamArgs implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final List<Class<?>> urlParamTypeList; // not null, read-only e.g. Integer.class, String.class
    protected final Map<Integer, Class<?>> optionalGenericTypeMap; // not null, read-only, has same size as parameters

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    /**
     * @param urlParamTypeList The read-only list of URL parameter types. (NotNull, EmptyAllowed)
     * @param optionalGenericTypeMap The read-only map of generic type used in URL parameters, keyed by argument index. (NotNull, EmptyAllowed)
     */
    public UrlParamArgs(List<Class<?>> urlParamTypeList, Map<Integer, Class<?>> optionalGenericTypeMap) {
        assertArgumentNotNull("urlParamTypeList", urlParamTypeList);
        assertArgumentNotNull("optionalGenericTypeMap", optionalGenericTypeMap);
        this.urlParamTypeList = urlParamTypeList; // already read-only
        this.optionalGenericTypeMap = optionalGenericTypeMap; // already read-only
    }

    // ===================================================================================
    //                                                                              Facade
    //                                                                              ======
    public boolean isNumberTypeParameter(int index) { // contains optional generic type
        if (urlParamTypeList.size() <= index) { // avoid out of bounds
            return false;
        }
        final Class<?> parameterType = urlParamTypeList.get(index);
        if (Number.class.isAssignableFrom(parameterType)) {
            return true;
        }
        final Class<?> genericType = optionalGenericTypeMap.get(index);
        return genericType != null && Number.class.isAssignableFrom(genericType);
    }

    public boolean isOptionalParameter(int index) {
        return optionalGenericTypeMap.get(index) != null;
    }

    public int size() {
        return urlParamTypeList.size();
    }

    public String toDisp() {
        final StringBuilder sb = new StringBuilder();
        int index = 0;
        for (Class<?> urlParamType : urlParamTypeList) {
            if (index > 0) {
                sb.append(", ");
            }
            sb.append(urlParamType.getSimpleName());
            final Class<?> genericType = optionalGenericTypeMap.get(index);
            if (genericType != null) {
                sb.append("<").append(genericType.getSimpleName()).append(">");
            }
            ++index;
        }
        return sb.toString();
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
        return DfTypeUtil.toClassTitle(this) + ":{" + toDisp() + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public List<Class<?>> getUrlParamTypeList() {
        return urlParamTypeList;
    }

    public Map<Integer, Class<?>> getOptionalGenericTypeMap() {
        return optionalGenericTypeMap;
    }
}