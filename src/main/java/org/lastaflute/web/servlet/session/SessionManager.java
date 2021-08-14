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
package org.lastaflute.web.servlet.session;

import org.lastaflute.web.servlet.request.scoped.ScopedAttributeHolder;
import org.lastaflute.web.servlet.request.scoped.ScopedMessageHandler;

/**
 * The manager of session. (session facade)
 * @author jflute
 */
public interface SessionManager extends ScopedAttributeHolder {

    // ===================================================================================
    //                                                                    Session Handling
    //                                                                    ================
    /**
     * Get session ID of the current session.
     * @return The string of session ID. (NotNull)
     */
    String getSessionId();

    /**
     * Invalidate session.
     */
    void invalidate();

    /**
     * Regenerate session ID for security. <br>
     * call invalidate() but it inherits existing session attributes.
     */
    void regenerateSessionId();

    // ===================================================================================
    //                                                                    Message Handling
    //                                                                    ================
    /**
     * @return The handler of action errors on session. (NotNull)
     */
    ScopedMessageHandler errors();

    /**
     * @return The handler of action info on session. (NotNull)
     */
    ScopedMessageHandler info();
}
