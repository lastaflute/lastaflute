/*
 * Copyright 2015-2024 the original author or authors.
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.dbflute.util.DfReflectionUtil;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.db.jta.RomanticTransaction;
import org.lastaflute.jta.dbcp.ConnectionPool;
import org.lastaflute.jta.dbcp.ConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.transaction.Transaction;

/**
 * @author jflute
 */
public class ConnectionPoolViewBuilder {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolViewBuilder.class);

    // ===================================================================================
    //                                                                          Build View
    //                                                                          ==========
    public String buildView() {
        final StringBuilder sb = new StringBuilder();
        final ConnectionPool[] pools = ContainerUtil.searchComponentsAll(ConnectionPool.class); // plural if e.g. master/slave
        boolean firstDone = false;
        for (ConnectionPool pool : pools) {
            if (firstDone) {
                sb.append("\n");
            }
            sb.append(pool.getClass().getName() + "@" + Integer.toHexString(pool.hashCode()));
            synchronized (pool) { // just in case
                final int free = pool.getFreePoolSize();
                final int active = pool.getActivePoolSize();
                final int txActive = pool.getTxActivePoolSize();
                sb.append("\n").append("freePool=").append(free);
                sb.append(", activePool=").append(active).append(", txActivePool=").append(txActive);
            }
            final List<String> txViewList = findTransactionViewList(pool);
            if (!txViewList.isEmpty()) {
                for (String txView : txViewList) {
                    sb.append("\n").append(txView);
                }
            } else {
                sb.append("\n(no transaction)");
            }
            firstDone = true;
        }
        return sb.toString();
    }

    // ===================================================================================
    //                                                                    Transaction View
    //                                                                    ================
    protected List<String> findTransactionViewList(ConnectionPool pool) {
        final List<String> txViewList = new ArrayList<String>();
        try {
            if (pool instanceof HookedConnectionPool) {
                setupTransactionViewListByHooked(pool, txViewList);
            } else {
                setupTransactionViewListByReflection(pool, txViewList);
            }
        } catch (RuntimeException continued) {
            String msg = "*Failed to build transaction expression";
            logger.info(msg, continued);
            txViewList.add(msg + ": " + continued.getMessage());
        }
        return txViewList;
    }

    protected void setupTransactionViewListByHooked(ConnectionPool pool, List<String> txViewList) {
        txViewList.addAll(((HookedConnectionPool) pool).extractActiveTransactionExpList());
    }

    protected void setupTransactionViewListByReflection(ConnectionPool pool, List<String> txViewList) {
        final Field field = DfReflectionUtil.getWholeField(pool.getClass(), "txActivePool");
        @SuppressWarnings("unchecked")
        final Map<Transaction, ConnectionWrapper> txActivePool =
                (Map<Transaction, ConnectionWrapper>) DfReflectionUtil.getValueForcedly(field, pool);
        synchronized (pool) { // just in case
            for (Entry<Transaction, ConnectionWrapper> entry : txActivePool.entrySet()) {
                final Transaction tx = entry.getKey();
                final ConnectionWrapper wrapper = entry.getValue();
                final String romantic;
                if (tx instanceof RomanticTransaction) {
                    romantic = ((RomanticTransaction) tx).toRomanticSnapshot(wrapper);
                } else {
                    romantic = tx.toString();
                }
                txViewList.add(romantic);
            }
        }
    }
}
