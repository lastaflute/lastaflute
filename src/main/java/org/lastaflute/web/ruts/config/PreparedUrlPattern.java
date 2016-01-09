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
package org.lastaflute.web.ruts.config;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jflute
 * @since 0.7.6 (2016/01/05 Tuesday)
 */
public class PreparedUrlPattern implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String urlPattern; // not null, empty allowed e.g. [method] or [method]/{} or "" (when index())
    protected final Pattern regexpPattern; // not null e.g. ^([^/]+)$ or ^([^/]+)/([^/]+)$ or ^sea/([^/]+)$
    protected final boolean methodNamePrefix; // true if urlPattern is [method]/...

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public PreparedUrlPattern(String urlPattern, Pattern regexpPattern, boolean methodNamePrefix) {
        this.urlPattern = urlPattern;
        this.regexpPattern = regexpPattern;
        this.methodNamePrefix = methodNamePrefix;
    }

    // ===================================================================================
    //                                                                              Facade
    //                                                                              ======
    public Matcher matcher(String paramPath) {
        if (paramPath == null) {
            throw new IllegalArgumentException("The argument 'paramPath' should not be null.");
        }
        return regexpPattern.matcher(paramPath);
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return "urlPattern:{" + urlPattern + ", " + regexpPattern + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getUrlPattern() {
        return urlPattern;
    }

    public Pattern getRegexpPattern() {
        return regexpPattern;
    }

    public boolean isMethodNamePrefix() {
        return methodNamePrefix;
    }
}