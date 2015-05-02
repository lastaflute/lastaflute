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
package org.lastaflute.db.jta;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.dbflute.util.DfTraceViewUtil;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.jta.core.TransactionImpl;
import org.lastaflute.jta.dbcp.ConnectionWrapper;

/**
 * @author jflute
 */
public class RomanticTransaction extends TransactionImpl {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                         Request State
    //                                         -------------
    protected String requestPath;
    protected Method entryMethod;
    protected Object userBean; // object not to depend on web

    // -----------------------------------------------------
    //                                     Transaction State
    //                                     -----------------
    protected long transactionBeginMillis; // set when transaction begins
    protected Map<String, Set<String>> tableCommandMap; // lazy loaded, needs synchronized

    // current state: might be overridden many times, needs synchronized
    protected String currentTableName; // basically not null in command
    protected String currentCommand; // basically not null in command
    protected Long currentSqlBeginMillis; // null allowed (but almost not null)
    protected TransactionCurrentSqlBuilder currentSqlBuilder; // basically not null in command

    // ===================================================================================
    //                                                                               Begin
    //                                                                               =====
    @Override
    public void begin() throws NotSupportedException, SystemException {
        if (ThreadCacheContext.exists()) { // in action or task
            requestPath = ThreadCacheContext.findRequestPath();
            entryMethod = ThreadCacheContext.findEntryMethod();
            userBean = ThreadCacheContext.findUserBean();
        }
        transactionBeginMillis = System.currentTimeMillis();
        super.begin(); // actually begin here
        saveRomanticTransactionToThread();
    }

    protected void saveRomanticTransactionToThread() {
        TransactionRomanticContext.setRomanticTransaction(this);
    }

    // ===================================================================================
    //                                                                     Commit/Rollback
    //                                                                     ===============
    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException,
            IllegalStateException, SystemException {
        clearRomanticTransactionFromThread();
        super.commit();
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        clearRomanticTransactionFromThread();
        super.rollback();
    }

    protected void clearRomanticTransactionFromThread() {
        TransactionRomanticContext.clear();
    }

    // ===================================================================================
    //                                                                            Romantic
    //                                                                            ========
    /**
     * @param wrapper The wrapper of connection for the transaction. (NotNull)
     * @return The romantic expression for transaction state. (NotNull)
     */
    public String toRomanticString(ConnectionWrapper wrapper) {
        synchronized (this) { // blocks registration
            final TransactionRomanticStringBuilder builder = createRomanticStringBuilder();
            return builder.buildRomanticString(this, wrapper);
        }
    }

    protected TransactionRomanticStringBuilder createRomanticStringBuilder() {
        return new TransactionRomanticStringBuilder();
    }

    // ===================================================================================
    //                                                                   Transaction State
    //                                                                   =================
    // -----------------------------------------------------
    //                                          Elapsed Time
    //                                          ------------
    public String currentElapsedTimeExp() {
        return buildElapsedTimeExp(transactionBeginMillis);
    }

    public String buildElapsedTimeExp(Long millis) {
        if (millis != null && millis > 0) {
            final long after = System.currentTimeMillis();
            return DfTraceViewUtil.convertToPerformanceView(after - millis);
        } else {
            return "*no begun";
        }
    }

    // -----------------------------------------------------
    //                                         Table Command
    //                                         -------------
    public void registerTableCommand(String tableName, String command, Long beginMillis, TransactionCurrentSqlBuilder sqlBuilder) {
        synchronized (this) { // toRomanticString() of exception thread looks the resources
            doRegisterTableCommand(tableName, command, beginMillis, sqlBuilder);
        }
    }

    protected void doRegisterTableCommand(String tableName, String command, Long beginMillis, TransactionCurrentSqlBuilder sqlBuilder) {
        if (tableCommandMap == null) {
            tableCommandMap = newTableCommandMap();
        }
        Set<String> commandSet = tableCommandMap.get(tableName);
        if (commandSet == null) {
            commandSet = newCommandSet();
            tableCommandMap.put(tableName, commandSet);
        }
        commandSet.add(command);
        currentTableName = tableName;
        currentCommand = command;
        currentSqlBeginMillis = beginMillis;
        currentSqlBuilder = sqlBuilder;
    }

    protected Map<String, Set<String>> newTableCommandMap() {
        return new LinkedHashMap<String, Set<String>>(); // plain because of synchronized
    }

    protected Set<String> newCommandSet() {
        return new LinkedHashSet<String>();
    }

    // -----------------------------------------------------
    //                                         Current State
    //                                         -------------
    public void clearCurrent() {
        synchronized (this) { // toRomanticString() of exception thread looks the resources
            doClearCurrent();
        }
    }

    protected void doClearCurrent() {
        currentTableName = null;
        currentCommand = null;
        currentSqlBeginMillis = null;
        currentSqlBuilder = null;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getRequestPath() {
        return requestPath;
    }

    public Method getEntryMethod() {
        return entryMethod;
    }

    public Object getUserBean() {
        return userBean;
    }

    public long getTransactionBeginMillis() {
        return transactionBeginMillis;
    }

    public Map<String, Set<String>> getReadOnlyTableCommandMap() {
        if (tableCommandMap != null) {
            return Collections.unmodifiableMap(tableCommandMap);
        } else {
            return Collections.emptyMap();
        }
    }

    public String getCurrentTableName() {
        return currentTableName;
    }

    public String getCurrentCommand() {
        return currentCommand;
    }

    public Long getCurrentSqlBeginMillis() {
        return currentSqlBeginMillis;
    }

    public TransactionCurrentSqlBuilder getCurrentSqlBuilder() {
        return currentSqlBuilder;
    }
}
