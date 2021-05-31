/*
 * Copyright 2015-2021 the original author or authors.
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
package org.lastaflute.db.dbflute.callbackcontext.traceablesql;

import org.dbflute.bhv.core.BehaviorCommandMeta;
import org.dbflute.hook.SqlResultHandler;
import org.dbflute.hook.SqlResultInfo;
import org.dbflute.jdbc.ExecutionTimeInfo;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.db.jta.RomanticTransaction;
import org.lastaflute.db.jta.TransactionRomanticContext;

/**
 * @author jflute
 * @since 0.7.2 (2015/12/22 Tuesday)
 */
public class RomanticTraceableSqlResultHandler implements SqlResultHandler {

    @Override
    public void handle(SqlResultInfo info) {
        final RomanticTransaction tx = TransactionRomanticContext.getRomanticTransaction();
        if (tx != null) {
            final BehaviorCommandMeta meta = info.getMeta(); // not saved because of internal object
            final String tableName = meta.getDBMeta().getTableDispName();
            final String command = meta.getCommandName();
            final ExecutionTimeInfo timeInfo = info.getExecutionTimeInfo();
            final Long beginMillis = timeInfo.getCommandBeforeTimeMillis();
            final Long endMillis = timeInfo.getCommandAfterTimeMillis();
            final Class<?> resultType = meta.getCommandReturnType();
            final Object resultValue = info.getResult();
            tx.registerRecentResult(tableName, command, beginMillis, endMillis, resultType, resultValue, meta);
        }
    }

    @Override
    public String toString() {
        return DfTypeUtil.toClassTitle(this) + "@" + Integer.toHexString(hashCode());
    }
}
