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
package org.lastaflute.db.dbflute.accesscontext;

import org.dbflute.hook.AccessContext;

/**
 * @author jflute
 */
public interface AccessContextArranger {

    /**
     * Arrange access context for DBFlute. <br>
     * This method is only creation (not set to thread local).
     * @param resource The resource of access context. (NotNull)
     * @return The arranged new-created instance of access context. (NotNull)
     */
    AccessContext arrangePreparedAccessContext(AccessContextResource resource);
}
