/*
 * Copyright 2015-2019 the original author or authors.
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

import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 */
public class NoneTransactionStage implements TransactionStage {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    public static final NoneTransactionStage DEFAULT_INSTANCE = new NoneTransactionStage();

    // ===================================================================================
    //                                                                         Transaction
    //                                                                         ===========
    @Override
    public <RESULT> OptionalThing<RESULT> required(TransactionShow<RESULT> txLambda) {
        return wrapOptional(doPerform(txLambda), txLambda);
    }

    @Override
    public <RESULT> OptionalThing<RESULT> requiresNew(TransactionShow<RESULT> txLambda) {
        return wrapOptional(doPerform(txLambda), txLambda);
    }

    protected <RESULT> RESULT doPerform(TransactionShow<RESULT> txLambda) {
        final BegunTx<RESULT> tx = newBegunTransaction();
        txLambda.perform(tx);
        return tx.getResult();
    }

    @Override
    public <RESULT> OptionalThing<RESULT> selectable(TransactionShow<RESULT> txLambda, TransactionGenre genre) {
        return required(txLambda);
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected <RESULT> BegunTx<RESULT> newBegunTransaction() {
        return new BegunTx<RESULT>(TransactionGenre.NONE);
    }

    protected <RESULT> OptionalThing<RESULT> wrapOptional(RESULT result, TransactionShow<RESULT> txLambda) {
        return OptionalThing.ofNullable(result, () -> {
            String msg = "Not found the transaction result: " + txLambda;
            throw new IllegalStateException(msg);
        });
    }
}
