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
package org.dbflute.lastaflute.db.jta.lazy;

import javax.annotation.Resource;

import org.dbflute.lasta.di.core.aop.frame.MethodInvocation;
import org.dbflute.lasta.di.tx.DefaultTransactionCallback;
import org.dbflute.lasta.di.tx.RequiredTxInterceptor;

/**
 * @author jflute
 */
public class LazyRequiredInterceptor extends RequiredTxInterceptor {

    // by name because it might be extended in application
    @Resource(name = "UserTransaction")
    protected LazyHookedUserTransaction lazyHookedUserTransaction;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (isJustLazyNow()) {
            return executeCallback(invocation);
        } else {
            return superInvoke(invocation);
        }
    }

    protected Object executeCallback(MethodInvocation invocation) throws Throwable {
        return new DefaultTransactionCallback(invocation, txRules).execute(transactionManagerAdapter);
    }

    protected boolean isJustLazyNow() {
        return lazyHookedUserTransaction.isJustLazyNow();
    }

    protected final Object superInvoke(MethodInvocation invocation) throws Throwable {
        return super.invoke(invocation);
    }
}
