/*
 * Copyright 2015-2016 the original author or authors.
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
package org.lastaflute.db.replication.slavedb;

/**
 * @author jflute
 * @param <RESULT> The type of result of call-back process.
 */
@FunctionalInterface
public interface SlaveDBCallback<RESULT> {

    /**
     * Call-back the process for DB access to master or slave DB.
     * @return The result of call-back process. (NullAllowed)
     */
    RESULT callback();
}
