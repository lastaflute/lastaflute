/*
 * Copyright 2015-2020 the original author or authors.
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
package org.lastaflute.db.jta.stage;

import javax.annotation.Resource;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.magic.destructive.BowgunDestructiveAdjuster;
import org.lastaflute.di.tx.TransactionManagerAdapter;

/**
 * @author jflute
 */
public class JTATransactionStage implements TransactionStage {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    @Resource
    private TransactionManagerAdapter transactionManagerAdapter;

    // ===================================================================================
    //                                                               Transaction Interface
    //                                                               =====================
    @SuppressWarnings("unchecked")
    @Override
    public <RESULT> OptionalThing<RESULT> required(TransactionShow<RESULT> txLambda) {
        try {
            return wrapOptional((RESULT) transactionManagerAdapter.required(adapter -> {
                return performTx(txLambda, adapter, TransactionGenre.REQUIRED);
            }), txLambda);
        } catch (Throwable e) {
            handleTransactionFailure(txLambda, e);
            return null; // unreachable
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <RESULT> OptionalThing<RESULT> requiresNew(TransactionShow<RESULT> txLambda) {
        if (isDestructiveRequiresNewToRequired()) { // destructive (for e.g. UnitTest)
            return required(txLambda); // use outer transaction if it exists
        } else { // basically here
            try {
                return wrapOptional((RESULT) transactionManagerAdapter.requiresNew(adapter -> {
                    return performTx(txLambda, adapter, TransactionGenre.REQUIRES_NEW);
                }), txLambda);
            } catch (Throwable e) {
                handleTransactionFailure(txLambda, e);
                return null; // unreachable
            }
        }
    }

    protected <RESULT> void handleTransactionFailure(TransactionShow<RESULT> txLambda, Throwable e) throws Error {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        if (e instanceof Error) {
            throw (Error) e;
        }
        String msg = "Failed to perform the transaction show (rollbacked): " + txLambda;
        throw new IllegalStateException(msg, e);
    }

    @Override
    public <RESULT> OptionalThing<RESULT> selectable(TransactionShow<RESULT> txLambda, TransactionGenre genre) {
        if (TransactionGenre.REQUIRED.equals(genre)) {
            return required(txLambda);
        } else if (TransactionGenre.REQUIRES_NEW.equals(genre)) {
            return requiresNew(txLambda);
        } else if (TransactionGenre.NONE.equals(genre)) {
            final BegunTx<RESULT> tx = newBegunTransaction(TransactionGenre.NONE);
            txLambda.perform(tx);
            return wrapOptional(tx.getResult(), txLambda);
        } else { // no way
            throw new IllegalStateException("Unknown genre: " + genre);
        }
    }

    // ===================================================================================
    //                                                                 Perform Transaction
    //                                                                 ===================
    protected <RESULT> RESULT performTx(TransactionShow<RESULT> txLambda, TransactionManagerAdapter adapter, TransactionGenre genre)
            throws Throwable {
        final BegunTx<RESULT> tx = newBegunTransaction(genre);
        BegunTxContext.setBegunTxOnThread(tx);
        try {
            txLambda.perform(tx);
            if (tx.isRollbackOnly()) { // e.g. when validation error
                adapter.setRollbackOnly();
            }
            return tx.getResult();
        } catch (Throwable e) {
            // same as DefaultTransactionCallback
            // forcedly roll-backed if exception in 'required' transaction scope
            adapter.setRollbackOnly();
            throw e;
        } finally {
            BegunTxContext.clearBegunTxOnThread();
        }
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected <RESULT> BegunTx<RESULT> newBegunTransaction(TransactionGenre genre) {
        return new BegunTx<RESULT>(genre);
    }

    protected <RESULT> OptionalThing<RESULT> wrapOptional(RESULT result, TransactionShow<RESULT> txLambda) {
        return OptionalThing.ofNullable(result, () -> {
            String msg = "Not found the transaction result: " + txLambda;
            throw new IllegalStateException(msg);
        });
    }

    // ===================================================================================
    //                                                                         Destructive
    //                                                                         ===========
    protected boolean isDestructiveRequiresNewToRequired() { // basically for UnitTest
        return BowgunDestructiveAdjuster.isRequiresNewToRequired();
    }
}
