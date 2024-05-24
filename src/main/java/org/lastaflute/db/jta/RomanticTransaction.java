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
package org.lastaflute.db.jta;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.dbflute.bhv.core.BehaviorCommandMeta;
import org.dbflute.util.DfTraceViewUtil;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.db.jta.romanticist.SavedTransactionMemories;
import org.lastaflute.db.jta.romanticist.TransactionCurrentSqlBuilder;
import org.lastaflute.db.jta.romanticist.TransactionMemoriesProvider;
import org.lastaflute.db.jta.romanticist.TransactionRomanticMemoriesBuilder;
import org.lastaflute.db.jta.romanticist.TransactionRomanticSnapshotBuilder;
import org.lastaflute.db.jta.romanticist.TransactionSavedRecentResult;
import org.lastaflute.jta.core.LaTransaction;
import org.lastaflute.jta.dbcp.ConnectionWrapper;

/**
 * @author jflute
 */
public class RomanticTransaction extends LaTransaction {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                          Request Info
    //                                          ------------
    protected String requestPath;
    protected Method entryMethod;
    protected Object userBean; // object not to depend on web

    // -----------------------------------------------------
    //                                         Current State
    //                                         -------------
    // basically to tell its state when other transactions fail
    protected long transactionBeginMillis; // set when transaction begins
    protected Map<String, Set<String>> tableCommandMap; // lazy loaded, needs synchronized, e.g. map:{MEMBER = list:{selectList}}

    // current state: might be overridden many times, needs synchronized
    protected String currentTableName; // basically not null in command
    protected String currentCommand; // basically not null in command
    protected Long currentSqlBeginMillis; // null allowed (but almost not null)
    protected TransactionCurrentSqlBuilder currentSqlBuilder; // basically not null in command

    // -----------------------------------------------------
    //                                         Recent Result
    //                                         -------------
    // basically for simple debug of current tranasction
    protected LinkedList<TransactionSavedRecentResult> recentResultList; // lazy loaded, needs synchronized

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
        registerMemoriesProviderIfNeeds("rollback"); // to show romantic memories in error message
        clearRomanticTransactionFromThread();
        super.rollback();
    }

    protected void registerMemoriesProviderIfNeeds(String ending) {
        if (ThreadCacheContext.exists()) {
            final TransactionMemoriesProvider provider = toRomanticMemoriesProvider(ending);
            final SavedTransactionMemories existing = ThreadCacheContext.findTransactionMemories();
            if (existing != null) {
                existing.registerNextProvider(provider);
            } else {
                ThreadCacheContext.registerTransactionMemories(new SavedTransactionMemories(provider));
            }
        }
    }

    protected void clearRomanticTransactionFromThread() {
        TransactionRomanticContext.clear();
    }

    // ===================================================================================
    //                                                                            Romantic
    //                                                                            ========
    /**
     * @param wrapper The wrapper of connection for the transaction. (NotNull)
     * @return The romantic expression for transaction snapshot. (NotNull)
     */
    public String toRomanticSnapshot(ConnectionWrapper wrapper) { // called when other transactions fail
        synchronized (this) { // blocks registration
            return createRomanticSnapshotBuilder().buildRomanticSnapshot(this, wrapper);
        }
    }

    protected TransactionRomanticSnapshotBuilder createRomanticSnapshotBuilder() {
        return new TransactionRomanticSnapshotBuilder();
    }

    /**
     * @param ending The ending type of transaction, e.g. rollback. (NotNull)
     * @return The provider of optional romantic expression for transaction memories. (NotNull)
     */
    public TransactionMemoriesProvider toRomanticMemoriesProvider(String ending) { // called when this transaction fails
        synchronized (this) { // blocks registration
            return TransactionRomanticMemoriesBuilder.createMemoriesProvider(this, ending);
        }
    }

    // ===================================================================================
    //                                                                       Current State
    //                                                                       =============
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
    //                                         Clear Current
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
    //                                                                       Recent Result
    //                                                                       =============
    public void registerRecentResult(String tableName, String command, Long beginMillis, Long endMillis, Class<?> resultType,
            Object resultValue, BehaviorCommandMeta meta) {
        synchronized (this) { // toRomanticString() of exception thread looks the resources
            doRegisterRecentResult(tableName, command, beginMillis, endMillis, resultType, resultValue, meta);
        }
    }

    public void doRegisterRecentResult(String tableName, String command, Long beginMillis, Long endMillis, Class<?> resultType,
            Object resultValue, BehaviorCommandMeta meta) {
        if (recentResultList == null) {
            recentResultList = newRecentResult();
        }
        if (recentResultList.size() > getRecentResultSavingLimit()) {
            recentResultList.removeFirst();
        }
        final long statementNo = prepareRecentResultStatementNo();
        recentResultList
                .add(createSavedRecentResult(statementNo, tableName, command, beginMillis, endMillis, resultType, resultValue, meta));
    }

    protected LinkedList<TransactionSavedRecentResult> newRecentResult() {
        return new LinkedList<TransactionSavedRecentResult>(); // plain because of synchronized
    }

    protected int getRecentResultSavingLimit() {
        return 30;
    }

    protected long prepareRecentResultStatementNo() {
        return recentResultList.isEmpty() ? 1L : recentResultList.peekLast().getStatementNo() + 1;
    }

    protected TransactionSavedRecentResult createSavedRecentResult(long statementNo, String tableName, String command, Long beginMillis,
            Long endMillis, Class<?> resultType, Object resultValue, BehaviorCommandMeta meta) {
        return new TransactionSavedRecentResult(statementNo, tableName, command, beginMillis, endMillis, resultType, resultValue, meta);
    }

    // -----------------------------------------------------
    //                                          Clear Recent
    //                                          ------------
    public void clearRecent() {
        synchronized (this) { // toRomanticString() of exception thread looks the resources
            doClearRecent();
        }
    }

    protected void doClearRecent() {
        if (recentResultList != null) {
            recentResultList.clear();
        }
        recentResultList = null;
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

    public List<TransactionSavedRecentResult> getReadOnlyRecentResultList() {
        if (recentResultList != null) {
            return Collections.unmodifiableList(recentResultList);
        } else {
            return Collections.emptyList();
        }
    }
}
