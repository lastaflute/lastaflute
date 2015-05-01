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
package org.dbflute.lastaflute.db.jta.stage;

import javax.annotation.Resource;

import org.dbflute.lasta.di.tx.TransactionManagerAdapter;
import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 */
public class JTATransactionStage implements TransactionStage {

    @Resource
    protected TransactionManagerAdapter transactionManagerAdapter;

    @SuppressWarnings("unchecked")
    @Override
    public <RESULT> OptionalThing<RESULT> required(TransactionShow<RESULT> noArgLambda) {
        try {
            return wrapOptional((RESULT) transactionManagerAdapter.required(adapter -> {
                return doPerform(noArgLambda, adapter);
            }), noArgLambda);
        } catch (Throwable e) {
            handleTransactionFailure(noArgLambda, e);
            return null; // unreachable
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <RESULT> OptionalThing<RESULT> requiresNew(TransactionShow<RESULT> noArgLambda) {
        try {
            return wrapOptional((RESULT) transactionManagerAdapter.requiresNew(adapter -> {
                return doPerform(noArgLambda, adapter);
            }), noArgLambda);
        } catch (Throwable e) {
            handleTransactionFailure(noArgLambda, e);
            return null; // unreachable
        }
    }

    protected <RESULT> Object doPerform(TransactionShow<RESULT> noArgLambda, TransactionManagerAdapter adapter) throws Throwable {
        try {
            return noArgLambda.perform();
        } catch (Throwable e) {
            adapter.setRollbackOnly();
            throw e;
        }
    }

    protected <RESULT> void handleTransactionFailure(TransactionShow<RESULT> noArgLambda, Throwable e) throws Error {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        if (e instanceof Error) {
            throw (Error) e;
        }
        String msg = "Failed to perform the transaction show (rollbacked): " + noArgLambda;
        throw new IllegalStateException(msg, e);
    }

    @Override
    public <RESULT> OptionalThing<RESULT> selectable(TransactionShow<RESULT> noArgLambda, TransactionGenre genre) {
        if (TransactionGenre.REQUIRED.equals(genre)) {
            return required(noArgLambda);
        } else if (TransactionGenre.REQUIRES_NEW.equals(genre)) {
            return required(noArgLambda);
        } else if (TransactionGenre.NONE.equals(genre)) {
            return wrapOptional(noArgLambda.perform(), noArgLambda);
        } else { // no way
            throw new IllegalStateException("Unknown genre: " + genre);
        }
    }

    protected <RESULT> OptionalThing<RESULT> wrapOptional(RESULT result, TransactionShow<RESULT> noArgLambda) {
        return OptionalThing.ofNullable(result, () -> {
            String msg = "Not found the transaction result: " + noArgLambda;
            throw new IllegalStateException(msg);
        });
    }
}
