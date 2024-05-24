/*
 * Copyright 2015-2024 the original author or authors.
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
 * @since 1.2.1 (2021/05/16 Sunday)
 */
public class MappingResolutionResult {

    protected final MappingPathResource pathResource; // not null
    protected final boolean pathHandled;

    public MappingResolutionResult(MappingPathResource pathResource, boolean pathHandled) {
        this.pathResource = pathResource;
        this.pathHandled = pathHandled;
    }

    public MappingPathResource getPathResource() {
        return pathResource;
    }

    public boolean isPathHandled() {
        return pathHandled;
    }
}
