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
package org.lastaflute.web.servlet.session;

/**
 * @author jflute
 */
public interface SessionResourceProvider {

    /**
     * @return The storage instance for session sharing. (NullAllowed: then no sharing)
     */
    default SessionSharedStorage provideSharedStorage() {
        return null;
    }

    /**
     * @return The arranger instance of HTTP session. (NullAllowed: then use default)
     */
    default HttpSessionArranger provideHttpSessionArranger() {
        return null;
    }
}
