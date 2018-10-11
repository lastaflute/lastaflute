/*
 * Copyright 2015-2018 the original author or authors.
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

import org.dbflute.optional.OptionalThing;
import org.lastaflute.web.servlet.request.scoped.ScopedAttributeHolder;

/**
 * @author jflute
 * @since 0.7.8 (2016/01/10 Sunday)
 */
public interface SessionSharedStorage extends ScopedAttributeHolder {

    /**
     * Invalidate session of shared storage.
     */
    void invalidate();

    /**
     * Get session ID as alternative to native session ID.
     * @return The optional session ID. (NotNull, EmptyAllowed: if empty, use native session ID)
     */
    default OptionalThing<String> getSessionId() {
        return OptionalThing.empty();
    }

    /**
     * Regenerate session ID of shared storage. <br>
     * You should implement when you have original session ID for shared storage.
     */
    default void regenerateSessionId() {
    }

    /**
     * Does it suppress HTTP session? (means that you use shared storage only) <br>
     * The getSessionId() implementation is required if true.
     * @return The determination, true or false.
     */
    default boolean suppressesHttpSession() {
        return false;
    }
}
