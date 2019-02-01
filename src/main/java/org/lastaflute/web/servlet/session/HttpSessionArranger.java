/*
 * Copyright 2015-2019 the original author or authors.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author jflute
 * @since 0.9.7 (2017/05/13 Saturday at kamogawa)
 */
public interface HttpSessionArranger {

    /**
     * @param request The request of servlet. (NotNull)
     * @param create Does it create new session when no-existing?
     * @return The new-created or existing HTTP session. (NotNull: if create, NullAllowed: if not create)
     */
    HttpSession create(HttpServletRequest request, boolean create);
}
