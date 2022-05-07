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
package org.lastaflute.web.ruts.config;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lastaflute.web.ruts.config.analyzer.UrlPatternAnalyzer.UrlPatternChosenBox;
import org.lastaflute.web.ruts.config.analyzer.UrlPatternAnalyzer.UrlPatternRegexpBox;

/**
 * @author jflute
 * @since 0.7.6 (2016/01/05 Tuesday)
 */
public class PreparedUrlPattern implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // [method] on urlPattern does not contain HTTP Method
    protected final String resolvedUrlPattern; // not null, empty allowed e.g. [method] or [method]/{} or "" (when index())
    protected final String sourceUrlPattern; // not null, empty allowed, might be derived
    protected final boolean specified; // true if urlPattern is defined by annotation
    protected final Pattern regexpPattern; // not null e.g. ^([^/]+)$ or ^([^/]+)/([^/]+)$ or ^sea/([^/]+)$
    protected final boolean methodNamePrefix; // true if urlPattern is [method]/...

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public PreparedUrlPattern(UrlPatternChosenBox chosenBox, UrlPatternRegexpBox regexpBox) {
        assertArgumentNotNull("chosenBox", chosenBox);
        assertArgumentNotNull("regexpBox", regexpBox);
        this.resolvedUrlPattern = chosenBox.getResolvedUrlPattern();
        this.sourceUrlPattern = chosenBox.getSourceUrlPattern();
        this.specified = chosenBox.isSpecified();
        this.regexpPattern = regexpBox.getRegexpPattern();
        this.methodNamePrefix = chosenBox.isMethodNamePrefix();
        assertArgumentNotNull("resolvedUrlPattern of chosenBox", resolvedUrlPattern);
        assertArgumentNotNull("sourceUrlPattern of chosenBox", sourceUrlPattern);
        assertArgumentNotNull("regexpPattern of regexpBox", regexpPattern);
    }

    // ===================================================================================
    //                                                                              Facade
    //                                                                              ======
    public Matcher matcher(String paramPath) {
        assertArgumentNotNull("paramPath", paramPath);
        return regexpPattern.matcher(paramPath);
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            throw new IllegalArgumentException("The variableName should not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
        }
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return "urlPattern:{" + resolvedUrlPattern + ", " + regexpPattern + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getResolvedUrlPattern() {
        return resolvedUrlPattern;
    }

    public String getSourceUrlPattern() {
        return sourceUrlPattern;
    }

    @Deprecated
    public String getUrlPattern() { // for compatible of UTFlute
        return resolvedUrlPattern;
    }

    public boolean isSpecified() {
        return specified;
    }

    public Pattern getRegexpPattern() {
        return regexpPattern;
    }

    public boolean isMethodNamePrefix() {
        return methodNamePrefix;
    }
}