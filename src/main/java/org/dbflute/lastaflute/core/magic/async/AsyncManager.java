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
package org.dbflute.lastaflute.core.magic.async;

/**
 * @author jflute
 */
public interface AsyncManager {

    /**
     * Execute asynchronous process by other thread. <br>
     * You can inherit...
     * <pre>
     * o ThreadCacheContext (copied plainly)
     * o AccessContext (copied as fixed value)
     * o CallbackContext (optional)
     * 
     * *attention: possibility of multiply threads access
     * </pre>
     * <p>Also you can change it from caller thread's one by interface default methods.</p>
     * @param noArgInLambda The callback for asynchronous process. (NotNull)
     */
    void async(ConcurrentAsyncCall noArgInLambda);
}
