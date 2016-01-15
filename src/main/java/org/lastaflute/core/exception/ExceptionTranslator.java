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
package org.lastaflute.core.exception;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.dbflute.exception.AccessContextNotFoundException;
import org.dbflute.exception.EntityAlreadyExistsException;
import org.dbflute.exception.SQLFailureException;
import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.direction.FwCoreDirection;
import org.lastaflute.db.dbcp.ConnectionPoolViewBuilder;
import org.lastaflute.db.dbflute.exception.NonTransactionalUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class ExceptionTranslator {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(ExceptionTranslator.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant director (AD) for framework. (NotNull: after initialization) */
    @Resource
    private FwAssistantDirector assistantDirector;

    /** The provider of exception translation. (NullAllowed: not required) */
    protected ExceptionTranslationProvider exceptionTranslationProvider;

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    @PostConstruct
    public synchronized void initialize() {
        final FwCoreDirection direction = assistCoreDirection();
        exceptionTranslationProvider = direction.assistExceptionTranslationProvider();
        showBootLogging();
    }

    protected FwCoreDirection assistCoreDirection() {
        return assistantDirector.assistCoreDirection();
    }

    protected void showBootLogging() {
        if (logger.isInfoEnabled()) {
            logger.info("[Exception Translator]");
            logger.info(" exceptionTranslationProvider: " + exceptionTranslationProvider);
        }
    }

    // ===================================================================================
    //                                                                        Filter Cause
    //                                                                        ============
    /**
     * @param cause The (might be) translated exception. (NotNull)
     * @return The filtered cause or specified cause. (NotNull)
     */
    public Throwable filterCause(Throwable cause) {
        if (cause == null) {
            throw new IllegalArgumentException("The argument 'cause' should not be null.");
        }
        Throwable handled = null;
        if (cause instanceof RuntimeException) {
            try {
                translateException((RuntimeException) cause);
            } catch (RuntimeException e) {
                handled = e;
            }
        }
        if (handled == null) {
            handled = cause;
        }
        return handled;
    }

    // ===================================================================================
    //                                                                           Translate
    //                                                                           =========
    /**
     * @param cause The (might be) translated exception. (NotNull)
     */
    public void translateException(RuntimeException cause) {
        if (cause == null) {
            throw new IllegalArgumentException("The argument 'cause' should not be null.");
        }
        if (exceptionTranslationProvider != null) { // not required
            exceptionTranslationProvider.translateFirst(cause);
        }
        if (cause instanceof AccessContextNotFoundException) {
            throwNonTransactionalUpdateException(cause);
        }
        if (cause instanceof SQLFailureException) {
            warnSQLFailureState((SQLFailureException) cause);
        }
        if (exceptionTranslationProvider != null) { // not required
            exceptionTranslationProvider.translateLast(cause);
        }
    }

    // -----------------------------------------------------
    //                                       Non Transaction
    //                                       ---------------
    protected void throwNonTransactionalUpdateException(RuntimeException cause) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The update process without transaction was found.");
        br.addItem("Advice");
        br.addElement("Update (contains insert, delete) should be executed in transaction.");
        br.addElement("Check your settings and implementations of the process.");
        final String msg = br.buildExceptionMessage();
        throw new NonTransactionalUpdateException(msg, cause);
    }

    // ===================================================================================
    //                                                                   SQL Failure State
    //                                                                   =================
    protected void warnSQLFailureState(SQLFailureException cause) {
        if (cause instanceof EntityAlreadyExistsException) {
            return; // the exception is obviously application exception so no warning
        }
        try {
            final String msg = buildSQLFailureState(cause);
            logger.warn(msg); // only warning here, the cause will be caught by logging filter
        } catch (RuntimeException continued) {
            logger.info("Failed to show warning of SQL failure state: " + Integer.toHexString(cause.hashCode()), continued);
        }
    }

    protected String buildSQLFailureState(SQLFailureException cause) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("SQL Failure State, here!");
        br.addItem("Advice");
        br.addElement("This state is for the SQL failure exception: #" + Integer.toHexString(cause.hashCode()));
        prepareConnectionPoolView(br);
        return br.buildExceptionMessage();
    }

    protected void prepareConnectionPoolView(final ExceptionMessageBuilder br) {
        br.addItem("ConnectionPool View");
        br.addElement(createConnectionPoolViewBuilder().buildView());
    }

    protected ConnectionPoolViewBuilder createConnectionPoolViewBuilder() {
        return new ConnectionPoolViewBuilder();
    }
}
