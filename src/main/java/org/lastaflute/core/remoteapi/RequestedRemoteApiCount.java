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
package org.lastaflute.core.remoteapi;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author jflute
 * @since 1.0.1 (2017/10/14 Saturday)
 */
public class RequestedRemoteApiCount {

    protected final Map<String, Integer> facadeCountMap; // not null

    public RequestedRemoteApiCount(CalledRemoteApiCounter counter) {
        this.facadeCountMap = counter.getCountMap();
    }

    @Override
    public String toString() {
        final Integer total = facadeCountMap.values().stream().collect(Collectors.summingInt(vl -> vl));
        final StringBuilder sb = new StringBuilder();
        sb.append("{total=").append(total);
        facadeCountMap.forEach((facadeName, count) -> {
            sb.append(", ").append(facadeName).append("=").append(count);
        });
        sb.append("}");
        return sb.toString();
    }

    public Map<String, Integer> getFacadeCountMap() {
        return facadeCountMap; // already read-only
    }
}
