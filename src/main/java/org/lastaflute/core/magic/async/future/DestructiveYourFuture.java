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
package org.lastaflute.core.magic.async.future;

import org.lastaflute.core.magic.async.waiting.WaitingAsyncResult;

/**
 * @author jflute
 * @since 0.9.6 (2017/04/24 Monday at showbase)
 */
public class DestructiveYourFuture implements YourFuture {

    public DestructiveYourFuture() {
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public WaitingAsyncResult waitForDone() {
        return new WaitingAsyncResult(); // as dummy
    }
}
