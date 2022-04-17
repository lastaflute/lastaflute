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
package org.lastaflute.db.dbcp;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.sql.XAConnection;
import javax.transaction.Transaction;

import org.lastaflute.db.jta.RomanticTransaction;
import org.lastaflute.jta.dbcp.ConnectionPool;
import org.lastaflute.jta.dbcp.ConnectionWrapper;
import org.lastaflute.jta.dbcp.SimpleConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class HookedConnectionPool extends SimpleConnectionPool {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(HookedConnectionPool.class);

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    /**
     * Initialize this component. <br>
     * This is basically called by DI setting file.
     */
    @PostConstruct
    public synchronized void initialize() { // called after property set of Di xml
        // nothing for now, for future
        //final FwDbDirection direction = assistDbDirection();
        //final ConnectionPoolAdjustmentProvider provider = direction.assistConnectionPoolAdjustmentProvider();
        showBootLogging();
    }

    protected void showBootLogging() {
        if (logger.isInfoEnabled()) {
            final String bigTell = (readOnly ? " *readOnly" : "") + (suppressLocalTx ? " *suppressLocalTx" : "");
            logger.info("[Connection Pool]" + bigTell);
            logger.info(" maxPoolSize: " + maxPoolSize);
            logger.info(" minPoolSize: " + minPoolSize);
            logger.info(" maxWait: " + maxWait + " milliseconds");
            logger.info(" timeout: " + timeout + " seconds");
            if (validationQuery != null) {
                logger.info(" validationQuery: \"" + validationQuery + "\"");
                logger.info(" validationInterval: " + validationInterval + " milliseconds");
            }
        }
    }

    // ===================================================================================
    //                                                                           Extension
    //                                                                           =========
    @Override
    protected String buildRomanticExp(Transaction tx, ConnectionWrapper wrapper) {
        final String romantic;
        if (tx instanceof RomanticTransaction) {
            romantic = ((RomanticTransaction) tx).toRomanticSnapshot(wrapper);
        } else {
            romantic = super.buildRomanticExp(tx, wrapper);
        }
        return romantic;
    }

    @Override
    protected ConnectionWrapper createConnectionWrapper(XAConnection xaConnection, Connection physicalConnection,
            ConnectionPool connectionPool, Transaction tx) throws SQLException {
        return new HookedConnectionWrapper(xaConnection, physicalConnection, connectionPool, tx);
    }
}
