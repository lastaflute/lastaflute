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
package org.lastaflute.core.smartdeploy;

import org.lastaflute.di.core.smart.hot.HotdeployUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 * @since 0.7.9 (2016/01/13 Wednesday)
 */
public abstract class ManagedHotdeploy {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(ManagedHotdeploy.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected static volatile int hotdeployCount;

    // ===================================================================================
    //                                                                       Determination
    //                                                                       =============
    public static boolean isHotdeploy() {
        return HotdeployUtil.isHotdeploy();
    }

    public static boolean isAlreadyHotdeploy() {
        return HotdeployUtil.isAlreadyHotdeploy();
    }

    // ===================================================================================
    //                                                                          Start/Stop
    //                                                                          ==========
    public static synchronized void start() {
        if (isHotdeploy()) {
            HotdeployUtil.start();
            ++hotdeployCount;
        }
    }

    public static synchronized void stop() {
        if (isHotdeploy()) {
            --hotdeployCount;
            if (hotdeployCount > 0) { // anyone is hot yet, expects stop() called later
                logger.debug("...Keeping hot deploy for other hot process: {}", hotdeployCount);
            } else { // last one
                HotdeployUtil.stop();
            }
        }
    }

    // ===================================================================================
    //                                                                         Deserialize
    //                                                                         ===========
    public static Object deserializeInternal(final byte[] bytes) throws Exception {
        return HotdeployUtil.deserializeInternal(bytes);
    }
}
