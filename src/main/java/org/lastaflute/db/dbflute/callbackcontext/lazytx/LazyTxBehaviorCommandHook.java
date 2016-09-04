/*
 * Copyright 2015-2016 the original author or authors.
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
package org.lastaflute.db.dbflute.callbackcontext.lazytx;

import org.dbflute.bhv.core.BehaviorCommandHook;
import org.dbflute.bhv.core.BehaviorCommandMeta;
import org.lastaflute.db.jta.lazytx.LazyHookedUserTransaction;

/**
 * @author jflute
 * @since 0.8.4 (2016/09/03 Saturday)
 */
public class LazyTxBehaviorCommandHook implements BehaviorCommandHook {

    public void hookBefore(BehaviorCommandMeta meta) {
        if (canBeginTransactionLazily(meta)) {
            LazyHookedUserTransaction.beginRealTransactionLazily();
        }
    }

    protected boolean canBeginTransactionLazily(BehaviorCommandMeta meta) {
        // cannot determine that procedure has update so fixedly begin
        return !meta.isSelect(); // contains e.g. update, procedure
    }

    public void hookFinally(BehaviorCommandMeta meta, RuntimeException cause) {
    }
}
