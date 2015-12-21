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

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.transaction.Transaction;

import org.lastaflute.db.jta.RomanticTransaction;
import org.lastaflute.jta.dbcp.ConnectionPool;
import org.lastaflute.jta.dbcp.ConnectionWrapper;
import org.lastaflute.jta.dbcp.SimpleConnectionPool;

/**
 * @author jflute
 */
public class HookedConnectionPool extends SimpleConnectionPool {

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
