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
package org.lastaflute.web.path;

/**
 * @author jflute
 * @since 1.1.0 (2018/09/17 Monday)
 */
public class UrlMappingResource {

    protected final String requestPath; // not null, e.g. /product/list/ (from HTTP client)
    protected final String workingMappingPath; // not null, e.g. /product/list/ (may be filtered before)

    public UrlMappingResource(String requestPath, String workingMappingPath) {
        this.requestPath = requestPath;
        this.workingMappingPath = workingMappingPath;
    }

    /**
     * Get the pure request path, which is not filtered. <br>
     * Basically use makingMappingPath to determine new filter instead of this. 
     * @return The path string requested from HTTP client. (NotNull)
     */
    public String getPureRequestPath() {
        return requestPath;
    }

    /**
     * Get the currently-working mapping path, which may be already filtered just now. <br>
     * Basically use this to determine new filter instead of requestPath. 
     * @return the path string for mapping, may be same as requestPath if no filter (NotNull)
     */
    public String getWorkingMappingPath() {
        return workingMappingPath;
    }
}
