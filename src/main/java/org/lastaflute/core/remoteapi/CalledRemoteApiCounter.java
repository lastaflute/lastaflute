/*
 * Copyright 2015-2020 the original author or authors.
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
package org.lastaflute.core.remoteapi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.magic.ThreadCompleted;

/**
 * @author jflute
 * @since 1.0.1 (2017/10/14 Saturday)
 */
public class CalledRemoteApiCounter implements ThreadCompleted, Consumer<String>, Supplier<String> { // thread cached

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected Map<String, Integer> facadeCountMap;

    // ===================================================================================
    //                                                                           Increment
    //                                                                           =========
    public CalledRemoteApiCounter increment(String facadeName) {
        if (facadeName == null) {
            throw new IllegalArgumentException("The argument 'facadeName' should not be null.");
        }
        if (facadeCountMap == null) {
            facadeCountMap = new LinkedHashMap<String, Integer>();
        }
        final Integer count = facadeCountMap.get(facadeName);
        if (count != null) {
            facadeCountMap.put(facadeName, count + 1);
        } else {
            facadeCountMap.put(facadeName, 1);
        }
        return this;
    }

    // ===================================================================================
    //                                                                          Compatible
    //                                                                          ==========
    // for e.g. Lasta RemoteApi-0.3.7 (depends on LastaFlute-1.0.0)
    @Override
    public void accept(String facadeName) {
        increment(facadeName);
    }

    @Override
    public String get() {
        return toLineDisp();
    }

    // ===================================================================================
    //                                                                             Display
    //                                                                             =======
    public String toLineDisp() { // basically format is same as requested mail count
        final Map<String, Integer> resolvedMap = facadeCountMap != null ? facadeCountMap : Collections.emptyMap();
        final Integer total = resolvedMap.values().stream().collect(Collectors.summingInt(vl -> vl));
        final StringBuilder sb = new StringBuilder();
        sb.append("{total=").append(total);
        resolvedMap.forEach((facadeName, count) -> {
            sb.append(", ").append(facadeName).append("=").append(count);
        });
        sb.append("}");
        return sb.toString();
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return DfTypeUtil.toClassTitle(this) + "@" + Integer.toHexString(hashCode());
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public Map<String, Integer> getCountMap() {
        return facadeCountMap != null ? Collections.unmodifiableMap(facadeCountMap) : Collections.emptyMap();
    }
}
