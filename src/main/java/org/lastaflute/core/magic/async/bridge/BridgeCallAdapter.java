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
package org.lastaflute.core.magic.async.bridge;

import org.dbflute.helper.function.IndependentProcessor;

/**
 * @author jflute
 * @since 1.1.5 (2019/12/22 Sunday at bay maihama)
 */
public class BridgeCallAdapter {

    protected IndependentProcessor appCall; // set in another thread

    public void adapt(IndependentProcessor appCall) {
        if (appCall == null) {
            throw new IllegalArgumentException("The argument 'appCall' should not be null.");
        }
        this.appCall = appCall;
    }

    public void delegate() {
        if (appCall == null) { // no way, just in case
            throw new IllegalStateException("Not found the appCall.");
        }
        appCall.process();
    }
}
