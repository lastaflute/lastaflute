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
package org.lastaflute.db.jta;

import java.util.Date;

import org.lastaflute.core.magic.TransactionTimeContext;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.db.dbflute.accesscontext.PreparedAccessContext;
import org.lastaflute.jta.core.LaUserTransaction;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

/**
 * @author jflute
 */
public class HookedUserTransaction extends LaUserTransaction {

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public HookedUserTransaction(TransactionManager transactionManager) {
        super(transactionManager);
    }

    // ===================================================================================
    //                                                                               Begin
    //                                                                               =====
    @Override
    public void begin() throws NotSupportedException, SystemException {
        hookBeforeBegin();
        doBegin();
    }

    protected void hookBeforeBegin() {
        prepareContext();
    }

    protected void doBegin() throws NotSupportedException, SystemException {
        superBegin();
    }

    protected final void superBegin() throws NotSupportedException, SystemException {
        super.begin();
    }

    // ===================================================================================
    //                                                                              Commit
    //                                                                              ======
    @Override
    public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException, RollbackException,
            SecurityException, SystemException {
        doCommit();
        hookAfterCommit();
    }

    protected void doCommit() throws HeuristicMixedException, HeuristicRollbackException, RollbackException, SystemException {
        superCommit();
    }

    protected final void superCommit() throws HeuristicMixedException, HeuristicRollbackException, RollbackException, SystemException {
        super.commit();
    }

    protected void hookAfterCommit() {
        clearContext();
    }

    // ===================================================================================
    //                                                                           Roll-back
    //                                                                           =========
    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        doRollback();
        hookAfterRollback();
    }

    protected void doRollback() throws SystemException {
        superRollback();
    }

    protected void superRollback() throws SystemException {
        super.rollback();
    }

    protected void hookAfterRollback() {
        clearContext();
    }

    // ===================================================================================
    //                                                                           Side Menu
    //                                                                           =========
    protected void prepareContext() {
        // including dicon gives you the unnatural error in cool deploy
        // (action's container have this dicon's container only in cool deploy)
        // so it gets dependencies in the extension class
        // by directly getting component from singleton container
        // *see the blog for the details:
        //   http://d.hatena.ne.jp/jflute/20130129/1359432974
        final TimeManager timeManager = getTimeManager();
        final Date transactionTime = timeManager.flashDate();
        TransactionTimeContext.setTransactionTime(transactionTime);
        if (PreparedAccessContext.existsAccessContextOnThread()) {
            PreparedAccessContext.beginAccessContext();
        }
    }

    protected TimeManager getTimeManager() {
        return ContainerUtil.getComponent(TimeManager.class);
    }

    protected void clearContext() {
        if (PreparedAccessContext.existsAccessContextOnThread()) {
            PreparedAccessContext.endAccessContext();
        }
        TransactionTimeContext.clear();
    }
}
