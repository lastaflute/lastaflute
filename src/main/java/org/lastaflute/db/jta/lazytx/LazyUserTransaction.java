/*
 * Copyright 2015-2017 the original author or authors.
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
package org.lastaflute.db.jta.lazytx;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.dbflute.helper.function.IndependentProcessor;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.db.jta.HookedUserTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The user transaction for lazy transaction.
 * <pre>
 * [Restriction]
 * MandatoryTx and NeverTx treats lazy transaction as no transaction.
 * Because getStatus() returns real status (cannot return lazy status).
 * </pre>
 * @author jflute
 */
public class LazyUserTransaction extends HookedUserTransaction {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(LazyUserTransaction.class);

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LazyUserTransaction(TransactionManager transactionManager) {
        super(transactionManager);
    }

    // ===================================================================================
    //                                                                               Begin
    //                                                                               =====
    @Override
    protected void doBegin() throws NotSupportedException, SystemException {
        if (canLazyTransaction()) {
            if (!isLazyTransactionLazyBegun()) { // first transaction
                incrementHierarchyLevel();
                toBeLazyTransaction(); // not begin real transaction here for lazy
            } else { // lazy now, this begin() means nested transaction
                if (!isLazyTransactionRealBegun()) { // not begun lazy transaction yet
                    beginRealTransactionLazily(); // forcedly begin outer transaction before e.g. 'requiresNew' scope
                    suspendForcedlyBegunLazyTransactionIfNeeds(); // like requires new transaction
                }
                incrementHierarchyLevel();
                superDoBegin(); // nested transaction is not lazy fixedly
            }
        } else { // normal transaction
            superDoBegin();
        }
    }

    protected void toBeLazyTransaction() {
        if (logger.isDebugEnabled()) {
            logger.debug("#lazyTx ...Being lazyBegun");
        }
        markLazyTransactionLazyBegun();
        arrangeLazyProcessIfAllowed(() -> {
            if (logger.isDebugEnabled()) {
                logger.debug("#lazyTx ...Being realBegun");
            }
            superDoBegin();
        });
    }

    protected final void superDoBegin() {
        try {
            super.doBegin();
        } catch (NotSupportedException e) {
            String msg = "Not supported the transaction.";
            throw new IllegalStateException(msg, e);
        } catch (SystemException e) {
            String msg = "Failed to begin the transaction.";
            throw new IllegalStateException(msg, e);
        }
    }

    // ===================================================================================
    //                                                                              Commit
    //                                                                              ======
    @Override
    protected void doCommit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException, RollbackException,
            SecurityException, SystemException {
        if (canTerminateTransactionReally()) {
            superDoCommit();
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("#lazyTx *No commit because of non-begun transaction");
            }
        }
        if (canLazyTransaction()) {
            decrementHierarchyLevel();
            resumeForcedlyBegunLazyTransactionIfNeeds(); // when nested transaction
        }
    }

    protected boolean canTerminateTransactionReally() {
        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // [not lazyAllowed]:
        // all begin() begin real transaction
        // so it can commit() really
        // 
        // [not readyLazy]
        // lazy allowed but not ready lazy transaction, it means normal transaction
        // so it can commit() really
        // 
        // [realBegun using Lazy]:
        // using lazy transaction but real transaction has been already begun
        // and it also might be nested transaction, it begins outer transaction forcedly before nested
        // so it can commit() really
        // _/_/_/_/_/_/_/_/_/_/
        return !isLazyTxAllowed() || !isLazyTransactionReadyLazy() || isLazyTransactionRealBegun();
    }

    protected final void superDoCommit()
            throws HeuristicMixedException, HeuristicRollbackException, RollbackException, SecurityException, SystemException {
        super.doCommit();
    }

    // ===================================================================================
    //                                                                           Roll-back
    //                                                                           =========
    @Override
    protected void doRollback() throws IllegalStateException, SecurityException, SystemException {
        if (canTerminateTransactionReally()) {
            superDoRollback();
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("#lazyTx *No rollback because of non-begun transaction");
            }
        }
        if (canLazyTransaction()) {
            decrementHierarchyLevel();
            resumeForcedlyBegunLazyTransactionIfNeeds(); // when nested transaction
        }
    }

    protected final void superDoRollback() throws IllegalStateException, SecurityException, SystemException {
        super.doRollback();
    }

    // ===================================================================================
    //                                                                     Hierarchy Level
    //                                                                     ===============
    protected void incrementHierarchyLevel() {
        doInOrDecrementHierarchyLevel(true);
    }

    protected void decrementHierarchyLevel() {
        doInOrDecrementHierarchyLevel(false);
    }

    protected void doInOrDecrementHierarchyLevel(boolean increment) {
        final Integer currentLevel = getCurrentHierarchyLevel();
        final int nextLevel;
        if (currentLevel != null) {
            nextLevel = currentLevel + (increment ? +1 : -1);
        } else {
            nextLevel = getFirstHierarchyLevel();
        }
        final String hierarchyLevelKey = generateHierarchyLevelKey();
        if (nextLevel > 0) {
            ThreadCacheContext.setObject(hierarchyLevelKey, nextLevel);
        } else { // zero, last decrement
            ThreadCacheContext.removeObject(hierarchyLevelKey);
        }
    }

    public Integer getCurrentHierarchyLevel() {
        return ThreadCacheContext.getObject(generateHierarchyLevelKey());
    }

    protected boolean isHerarchyLevelFirst() {
        final Integer currentLevel = ThreadCacheContext.getObject(generateHierarchyLevelKey());
        return currentLevel != null && currentLevel.equals(getFirstHierarchyLevel());
    }

    protected int getFirstHierarchyLevel() {
        return 1;
    }

    protected String generateHierarchyLevelKey() {
        return "lazyTx:hierarchyLevel";
    }

    // ===================================================================================
    //                                                                      Suspend/Resume
    //                                                                      ==============
    protected void suspendForcedlyBegunLazyTransactionIfNeeds() throws SystemException {
        final Transaction suspended = transactionManager.suspend();
        arrangeForcedlyBegunResumer(() -> {
            if (isHerarchyLevelFirst()) {
                doResumeForcedlyBegunLazyTransaction(suspended);
                return true;
            } else {
                return false;
            }
        });
    }

    protected void arrangeForcedlyBegunResumer(ForcedlyBegunResumer resumer) {
        ThreadCacheContext.setObject(generateResumeKey(), resumer);
    }

    protected void doResumeForcedlyBegunLazyTransaction(Transaction suspended) {
        try {
            transactionManager.resume(suspended);
        } catch (InvalidTransactionException e) {
            String msg = "Invalid the transaction: " + suspended;
            throw new IllegalStateException(msg, e);
        } catch (SystemException e) {
            String msg = "Failed to resume the transaction: " + suspended;
            throw new IllegalStateException(msg, e);
        }
    }

    protected void resumeForcedlyBegunLazyTransactionIfNeeds() {
        final String resumeKey = generateResumeKey();
        final ForcedlyBegunResumer resumer = ThreadCacheContext.getObject(resumeKey);
        if (resumer != null) {
            final boolean resumed = resumer.resume();
            if (resumed) {
                ThreadCacheContext.removeObject(resumeKey);
            }
        }
    }

    @FunctionalInterface
    protected static interface ForcedlyBegunResumer {
        boolean resume();
    }

    protected static String generateResumeKey() {
        return "lazyTx:resumer";
    }

    // ===================================================================================
    //                                                                      Roll-back Only
    //                                                                      ==============
    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        if (isJustLazyNow()) {
            arrangeLazyProcessIfAllowed(() -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("#lazyTx ...Setting transaction roll-back only");
                }
                doSuperSetRollbackOnly();
            });
        } else {
            doSuperSetRollbackOnly();
        }
    }

    protected void doSuperSetRollbackOnly() {
        try {
            super.setRollbackOnly();
        } catch (SystemException e) {
            String msg = "Failed to set roll-back only";
            throw new IllegalStateException(msg, e);
        }
    }

    // ===================================================================================
    //                                                                 Transaction Timeout
    //                                                                 ===================
    @Override
    public void setTransactionTimeout(int timeout) throws SystemException {
        if (isJustLazyNow()) {
            arrangeLazyProcessIfAllowed(() -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("#lazyTx ...Setting transaction timeout: {}", timeout);
                }
                doSuperSetTransactionTimeout(timeout);
            });
        } else {
            doSuperSetTransactionTimeout(timeout);
        }
    }

    protected void doSuperSetTransactionTimeout(int timeout) {
        try {
            super.setTransactionTimeout(timeout);
        } catch (SystemException e) {
            String msg = "Failed to set transaction timeout: " + timeout;
            throw new IllegalStateException(msg, e);
        }
    }

    // ===================================================================================
    //                                                                  Transaction Status
    //                                                                  ==================
    // *getStatus() should return real status of transaction
    // because transaction manager may expect correct status
    // so no extension for lazy

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    public boolean canLazyTransaction() {
        return isLazyTxAllowed() && isLazyTransactionReadyLazy();
    }

    public boolean isJustLazyNow() {
        return isLazyTransactionLazyBegun() && !isLazyTransactionRealBegun();
    }

    protected boolean isLazyTxAllowed() { // you can override
        return true;
    }

    protected void arrangeLazyProcessIfAllowed(IndependentProcessor processor) {
        final String lazyKey = generateLazyProcessListKey();
        List<IndependentProcessor> lazyList = ThreadCacheContext.getObject(lazyKey);
        if (lazyList == null) {
            lazyList = new ArrayList<IndependentProcessor>();
            ThreadCacheContext.setObject(lazyKey, lazyList);
        }
        lazyList.add(processor);
    }

    // ===================================================================================
    //                                                                static Lazy Handling
    //                                                                ====================
    // -----------------------------------------------------
    //                                            Controller
    //                                            ----------
    public static void readyLazyTransaction() {
        if (logger.isDebugEnabled()) {
            logger.debug("#lazyTx ...Being readyLazy");
        }
        markLazyTransactionReadyLazy();
    }

    public static void beginRealTransactionLazily() {
        final String lazyKey = generateLazyProcessListKey();
        final List<IndependentProcessor> lazyList = ThreadCacheContext.getObject(lazyKey);
        if (lazyList != null) {
            markLazyRealBegun();
            for (IndependentProcessor processor : lazyList) {
                processor.process(); // with logging
            }
            ThreadCacheContext.removeObject(lazyKey);
        }
    }

    public static void closeLazyTransaction() {
        if (logger.isDebugEnabled()) {
            logger.debug("#lazyTx ...Being over");
        }
        ThreadCacheContext.removeObject(generateReadyLazyKey());
        ThreadCacheContext.removeObject(generateLazyBegunKey());
        ThreadCacheContext.removeObject(generateRealBegunKey());
        ThreadCacheContext.removeObject(generateLazyProcessListKey());
        ThreadCacheContext.removeObject(generateResumeKey()); // just in case
    }

    // -----------------------------------------------------
    //                                            Lazy Ready
    //                                            ----------
    protected static void markLazyTransactionReadyLazy() {
        ThreadCacheContext.setObject(generateReadyLazyKey(), true);
    }

    protected static boolean isLazyTransactionReadyLazy() {
        return ThreadCacheContext.determineObject(generateReadyLazyKey());
    }

    public static String generateReadyLazyKey() {
        return "lazyTx:readyLazy";
    }

    // -----------------------------------------------------
    //                                            Lazy Begun
    //                                            ----------
    protected static void markLazyTransactionLazyBegun() {
        ThreadCacheContext.setObject(generateLazyBegunKey(), true);
    }

    protected static boolean isLazyTransactionLazyBegun() {
        return ThreadCacheContext.determineObject(generateLazyBegunKey());
    }

    public static String generateLazyBegunKey() {
        return "lazyTx:lazyBegun";
    }

    // -----------------------------------------------------
    //                                            Real Begun
    //                                            ----------
    protected static boolean isLazyTransactionRealBegun() {
        return ThreadCacheContext.determineObject(generateRealBegunKey());
    }

    protected static void markLazyRealBegun() {
        ThreadCacheContext.setObject(generateRealBegunKey(), true);
    }

    public static String generateRealBegunKey() {
        return "lazyTx:realBegun";
    }

    // -----------------------------------------------------
    //                                          Lazy Process
    //                                          ------------
    public static String generateLazyProcessListKey() {
        return "lazyTx:lazyProcessList";
    }
}
