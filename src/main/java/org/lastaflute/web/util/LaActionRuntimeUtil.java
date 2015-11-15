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
package org.lastaflute.web.util;

import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.ruts.process.ActionRuntime;

/**
 * @author jflute
 */
public class LaActionRuntimeUtil {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final String KEY = LastaWebKey.ACTION_RUNTIME_KEY;

    // ===================================================================================
    //                                                             Access to RuntimeConfig
    //                                                             =======================
    public static boolean hasActionRuntime() {
        return doGetActionRuntime() != null;
    }

    public static ActionRuntime getActionRuntime() {
        final ActionRuntime method = doGetActionRuntime();
        if (method == null) {
            String msg = "Not found the execute config for the request: key=" + KEY;
            throw new IllegalStateException(msg);
        }
        return method;
    }

    protected static ActionRuntime doGetActionRuntime() {
        return (ActionRuntime) LaRequestUtil.getRequest().getAttribute(KEY);
    }

    public static void setActionRuntime(ActionRuntime runtime) {
        LaRequestUtil.getRequest().setAttribute(KEY, runtime);
    }
}
