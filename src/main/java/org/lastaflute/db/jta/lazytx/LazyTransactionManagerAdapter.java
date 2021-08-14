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
package org.lastaflute.db.jta.lazytx;

import javax.transaction.TransactionManager;

import org.lastaflute.db.jta.HookedTransactionManagerAdapter;
import org.lastaflute.di.tx.TransactionCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 * @since 0.8.4 (2016/09/03 Saturday)
 */
public class LazyTransactionManagerAdapter extends HookedTransactionManagerAdapter {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(LazyTransactionManagerAdapter.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final LazyUserTransaction lazyHookedUserTransaction;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LazyTransactionManagerAdapter(TransactionManager transactionManager, LazyUserTransaction lazyHookedUserTransaction) {
        super(transactionManager, lazyHookedUserTransaction);
        this.lazyHookedUserTransaction = lazyHookedUserTransaction; // keep as concrete type
    }

    // ===================================================================================
    //                                                                      Implementation
    //                                                                      ==============
    @Override
    public Object required(TransactionCallback callback) throws Throwable {
        if (isJustLazyNow()) {
            if (logger.isDebugEnabled()) {
                logger.debug("#lazyTx ...Taking over the lazy transaction for 'required' scope: {}", LazyUserTransaction.buildLazyTxExp());
            }
            return callback.execute(this); // no transaction handling means taking over lazy transaction
        } else {
            return super.required(callback);
        }
    }

    protected boolean isJustLazyNow() {
        return lazyHookedUserTransaction.isJustLazyNow();
    }
}
