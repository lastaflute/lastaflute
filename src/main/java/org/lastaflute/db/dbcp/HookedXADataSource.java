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
package org.lastaflute.db.dbcp;

import java.io.File;
import java.io.IOException;

import org.dbflute.util.DfResourceUtil;
import org.dbflute.util.Srl;
import org.dbflute.util.Srl.ScopeInfo;
import org.lastaflute.di.util.LdiClassUtil;
import org.lastaflute.jta.dbcp.SimpleXADataSource;

/**
 * @author jflute
 */
public class HookedXADataSource extends SimpleXADataSource {

    protected static final String CLASSES_BEGIN_MARK = "$classes(";
    protected static final String CLASSES_END_MARK = ".class)";

    @Override
    public void setURL(String url) {
        super.setURL(resolveClassesUrl(url));
    }

    protected String resolveClassesUrl(String url) { // for e.g. H2 local file
        final String beginMark = CLASSES_BEGIN_MARK;
        final String endMark = CLASSES_END_MARK;
        final ScopeInfo classesScope = Srl.extractScopeFirst(url, beginMark, endMark);
        if (classesScope == null) {
            return url;
        }
        final String className = classesScope.getContent().trim();
        final String front = Srl.substringFirstFront(url, beginMark);
        final String rear = Srl.substringFirstRear(url, endMark);
        final Class<?> lardmarkType = LdiClassUtil.forName(className);
        final File buildDir = DfResourceUtil.getBuildDir(lardmarkType);
        try {
            final String canonicalPath = buildDir.getCanonicalPath();
            return front + canonicalPath.replace('\\', '/') + rear;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to get canonical path: " + buildDir, e);
        }
    }
}
