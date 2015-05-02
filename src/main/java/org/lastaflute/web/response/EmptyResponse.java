/*
 * Copyright 2014-2015 the original author or authors.
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
package org.lastaflute.web.response;

import java.util.Collections;
import java.util.Map;

/**
 * @author jflute
 */
public class EmptyResponse implements ActionResponse {

    protected static final EmptyResponse INSTANCE_OF_EMPTY = new EmptyResponse();

    public static EmptyResponse instance() {
        return INSTANCE_OF_EMPTY;
    }

    protected EmptyResponse() {
    }

    public EmptyResponse header(String name, String value) {
        throw new IllegalStateException("Cannot use header() for empty response: " + name + ", " + value);
    }

    public Map<String, String> getHeaderMap() {
        return Collections.emptyMap();
    }

    public boolean isEmpty() {
        return true;
    }

    public boolean isPresent() {
        return false;
    }

    public boolean isSkip() {
        return false;
    }
}
