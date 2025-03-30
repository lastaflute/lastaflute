/*
 * Copyright 2015-2025 the original author or authors.
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
package org.lastaflute.web.servlet.filter.mask;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;

import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.Srl;
import org.lastaflute.web.util.LaServletContextUtil;

/**
 * @author jflute
 * @since 1.2.8 (2025/03/30 Sunday at nakameguro)
 */
public class MaskParamSetExtractor {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    /** same as RequestLoggingFilter's init parameter. (NotNull) */
    public static final String KEY_MASK_PARAM_SET = "maskParamSet";

    /**
     * The immutable set of permanent cache for Servlet's markParamSet. e.g. [password, pwd]
     * (basically NotNull: after caching)
     */
    protected static Set<String> cachedSet;

    // ===================================================================================
    //                                                                             Extract
    //                                                                             =======
    public Set<String> extractLoggingFilterMaskParamSet() { // not null, empty allowed, immutable
        if (cachedSet != null) {
            return cachedSet;
        }
        synchronized (MaskParamSetExtractor.class) { // static lock
            if (cachedSet != null) {
                return cachedSet;
            }
            cachedSet = doExtractMaskParamSet();
            return cachedSet;
        }
    }

    protected Set<String> doExtractMaskParamSet() { // immutable
        // the parameter setting may be set at LastaFilter or RequestLoggingFilter (directly) on web.xml
        // so it supports both pattern here (only naming determination) 
        final ServletContext servletContext = LaServletContextUtil.getServletContext();
        final Map<String, ? extends FilterRegistration> registrations = servletContext.getFilterRegistrations();
        for (FilterRegistration registration : registrations.values()) {
            final String exp = registration.getInitParameter(KEY_MASK_PARAM_SET);
            if (exp != null) { // e.g. "password, pwd"
                final List<String> maskParamList = Srl.splitListTrimmed(exp, ","); // e.g. [password, pwd]
                return Collections.unmodifiableSet(toMaskParamSet(maskParamList));
            }
        }
        return DfCollectionUtil.emptySet();
    }

    protected Set<String> toMaskParamSet(final List<String> maskParamList) {
        // intentionally case sensitive for strict user test (same as logging filter)
        return DfCollectionUtil.newLinkedHashSet(maskParamList);
    }
}
