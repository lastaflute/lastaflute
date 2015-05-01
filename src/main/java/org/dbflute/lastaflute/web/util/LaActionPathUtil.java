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

/**
 * @author modified by jflute (originated in Seasar)
 */
public final class LaActionPathUtil {

    private static final String SUFFIX = "Action";

    private LaActionPathUtil() {
    }

    public static String fromPathToActionName(String actionPath) {
        return actionPath.substring(1).replace('/', '_') + SUFFIX;
    }

    public static String fromActionNameToPath(String actionName) {
        return "/" + actionName.replace('_', '/').substring(0, actionName.length() - SUFFIX.length());
    }
}
