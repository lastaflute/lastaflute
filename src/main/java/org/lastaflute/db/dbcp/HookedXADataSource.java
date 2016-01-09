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
package org.lastaflute.db.dbcp;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.XAConnection;

import org.dbflute.util.DfResourceUtil;
import org.dbflute.util.Srl;
import org.dbflute.util.Srl.ScopeInfo;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.db.direction.FwDbDirection;
import org.lastaflute.di.util.LdiClassUtil;
import org.lastaflute.jta.dbcp.SimpleXADataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class HookedXADataSource extends SimpleXADataSource {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(HookedXADataSource.class);
    protected static final String CLASSES_BEGIN_MARK = "$classes(";
    protected static final String CLASSES_END_MARK = ".class)";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant director (AD) for framework. (NotNull: after initialization) */
    @Resource
    protected FwAssistantDirector assistantDirector;

    /** The hook of newborn XA connection. (NullAllowed: option) */
    protected XAConnectionHook newbornConnectionHook;

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    /**
     * Initialize this component. <br>
     * This is basically called by DI setting file.
     */
    @PostConstruct
    public synchronized void initialize() {
        final FwDbDirection direction = assistDbDirection();
        newbornConnectionHook = direction.assistNewbornConnectionHook();
        showBootLogging();
    }

    protected FwDbDirection assistDbDirection() {
        return assistantDirector.assistDbDirection();
    }

    protected void showBootLogging() {
        if (logger.isInfoEnabled()) {
            logger.info("[XA DataSource]");
            logger.info(" driver: " + driverClassName);
            logger.info(" url: " + url);
            logger.info(" newbornConnectionHook: " + newbornConnectionHook);
        }
    }

    // ===================================================================================
    //                                                                     Hooked Override
    //                                                                     ===============
    @Override
    public XAConnection getXAConnection() throws SQLException {
        final XAConnection xaconn = super.getXAConnection();
        if (newbornConnectionHook != null) {
            newbornConnectionHook.hook(xaconn);
        }
        return xaconn;
    }

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
