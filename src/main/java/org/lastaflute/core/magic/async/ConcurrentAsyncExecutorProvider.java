/*
 * Copyright 2015-2022 the original author or authors.
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
package org.lastaflute.core.magic.async;

/**
 * @author jflute
 */
public interface ConcurrentAsyncExecutorProvider {

    /**
     * @return The default option of concurrent asynchronous process. (NullAllowed: if null, as no option)
     */
    ConcurrentAsyncOption provideDefaultOption();

    /**
     * @return The max pool size of one thread pool. (NullAllowed: if null, as default)
     */
    default Integer provideMaxPoolSize() {
        return null;
    }
}
