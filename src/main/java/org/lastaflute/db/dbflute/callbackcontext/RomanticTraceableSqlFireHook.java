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
package org.lastaflute.db.dbflute.callbackcontext;

import org.dbflute.bhv.core.BehaviorCommandMeta;
import org.dbflute.bhv.core.context.InternalMapContext;
import org.dbflute.hook.SqlFireHook;
import org.dbflute.hook.SqlFireReadyInfo;
import org.dbflute.hook.SqlFireResultInfo;
import org.dbflute.hook.SqlLogInfo;
import org.lastaflute.db.jta.RomanticTransaction;
import org.lastaflute.db.jta.TransactionCurrentSqlBuilder;
import org.lastaflute.db.jta.TransactionRomanticContext;

/**
 * @author jflute
 */
public class RomanticTraceableSqlFireHook implements SqlFireHook {

    @Override
    public void hookBefore(BehaviorCommandMeta meta, SqlFireReadyInfo fireReadyInfo) {
        saveCommandToRomanticTransaction(meta, fireReadyInfo);
    }

    @Override
    public void hookFinally(BehaviorCommandMeta meta, SqlFireResultInfo fireResultInfo) {
        tellCurrentCommandClosed(meta, fireResultInfo);
    }

    protected void saveCommandToRomanticTransaction(BehaviorCommandMeta meta, SqlFireReadyInfo fireReadyInfo) {
        final RomanticTransaction tx = TransactionRomanticContext.getRomanticTransaction();
        if (tx != null) {
            final String tableName = meta.getDBMeta().getTableDispName();
            final String commandName = meta.getCommandName();
            final Long beginMillis = InternalMapContext.getSqlBeforeTimeMillis(); // cannot get from ready info...
            final TransactionCurrentSqlBuilder currentSqlBuilder = createCurrentSqlBuilder(fireReadyInfo.getSqlLogInfo());
            tx.registerTableCommand(tableName, commandName, beginMillis, currentSqlBuilder);
        }
    }

    protected TransactionCurrentSqlBuilder createCurrentSqlBuilder(final SqlLogInfo sqlLogInfo) {
        return new TransactionCurrentSqlBuilder() {
            public String buildSql() {
                // to be exact, this is not perfectly thread-safe but no problem,
                // only possibility of no cache use (while, building process is thread-safe)
                return sqlLogInfo.getDisplaySql(); // lazily
            }
        };
    }

    protected void tellCurrentCommandClosed(BehaviorCommandMeta meta, SqlFireResultInfo fireResultInfo) {
        final RomanticTransaction tx = TransactionRomanticContext.getRomanticTransaction();
        if (tx != null) {
            tx.clearCurrent();
        }
    }
}
