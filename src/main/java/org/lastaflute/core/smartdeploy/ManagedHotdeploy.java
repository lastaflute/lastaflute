/*
 * Copyright 2015-2020 the original author or authors.
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

/**
 * @author jflute
 * @since 0.7.9 (2016/01/13 Wednesday)
 */
public abstract class ManagedHotdeploy {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected static volatile int hotdeployCount;

    // ===================================================================================
    //                                                                  HotDeploy Resource
    //                                                                  ==================
    public static ClassLoader getLaContainerClassLoader() {
        return HotdeployUtil.getLaContainerClassLoader();
    }

    public static ClassLoader getThreadContextClassLoader() {
        return HotdeployUtil.getThreadContextClassLoader();
    }

    // ===================================================================================
    //                                                                       Determination
    //                                                                       =============
    public static boolean isHotdeploy() {
        return HotdeployUtil.isHotdeploy();
    }

    public static boolean isLaContainerHotdeploy() {
        return HotdeployUtil.isLaContainerHotdeploy();
    }

    public static boolean isThreadContextHotdeploy() {
        return HotdeployUtil.isThreadContextHotdeploy();
    }

    // ===================================================================================
    //                                                                          Start/Stop
    //                                                                          ==========
    public static synchronized ClassLoader start() {
        if (isHotdeploy()) {
            final ClassLoader originalLoader = getThreadContextClassLoader();
            if (isAnotherThreadHotdeploy()) { // e.g. job started
                inheritAnotherThreadClassLoader(); // to use same loader
            } else { // normally here
                // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
                // remove this if-statement to avoid context class-loader being null by jflute (2017/12/17) 
                // if stop() without start(), context class-loader becomes null
                // if hot-deploy process makes new thread, the thread inherits
                // hot-deploy class-loader as context class-loader
                // so this if-statement causes stop() without start()
                // (though hot-deploy class-loader may wrap hot-deploy class-loader, but no problem?)
                // _/_/_/_/_/_/_/_/_/_/
                //if (!isThreadContextHotdeploy()) {
                HotdeployUtil.start();
            }
            ++hotdeployCount;
            return originalLoader;
        } else {
            return null;
        }
    }

    protected static boolean isAnotherThreadHotdeploy() {
        return isLaContainerHotdeploy() && !isThreadContextHotdeploy();
    }

    protected static void inheritAnotherThreadClassLoader() {
        HotdeployUtil.setThreadContextClassLoader(getLaContainerClassLoader());
    }

    public static synchronized void stop(ClassLoader originalLoader) {
        if (isHotdeploy()) {
            --hotdeployCount;
            if (hotdeployCount <= 0) { // nobody is hot (stop or keep hot), also minus just in case
                HotdeployUtil.stop(); // with restoring thread class loader
                hotdeployCount = 0; // if minus
            } else {
                HotdeployUtil.setThreadContextClassLoader(originalLoader);
            }
        }
    }

    // ===================================================================================
    //                                                                         Deserialize
    //                                                                         ===========
    public static Object deserializeInternal(byte[] bytes) throws Exception {
        return HotdeployUtil.deserializeInternal(bytes);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public static synchronized int getHotdeployCount() {
        return hotdeployCount;
    }
}
