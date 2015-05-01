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
package org.dbflute.lastaflute.web.util;

import javax.servlet.ServletRequest;

import org.dbflute.lastaflute.web.LastaWebKey;
import org.dbflute.lastaflute.web.ruts.config.ModuleConfig;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class LaModuleConfigUtil {

    protected static final String KEY = LastaWebKey.MODULE_CONFIG_KEY;

    public static ModuleConfig getModuleConfig() {
        final ModuleConfig config = (ModuleConfig) LaServletContextUtil.getServletContext().getAttribute(KEY);
        if (config == null) {
            String msg = "Not found the module config in the servlet context: " + KEY;
            throw new IllegalStateException(msg);
        }
        return config;
    }

    /**
     * @param request The request to find. (NotNull)
     * @return The found module configuration in the request or context. (NotNull: if not found, throws exception)
     */
    public static ModuleConfig findModuleConfig(ServletRequest request) {
        final ModuleConfig config = (ModuleConfig) request.getAttribute(KEY);
        return config != null ? config : getModuleConfig();
    }
}
