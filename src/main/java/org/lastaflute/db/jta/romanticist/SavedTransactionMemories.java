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
package org.lastaflute.db.jta.romanticist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.magic.ThreadCompleted;

/**
 * @author jflute
 * @since 0.7.2 (2015/12/22 Tuesday)
 */
public class SavedTransactionMemories implements ThreadCompleted { // thread cached

    protected final List<TransactionMemoriesProvider> providerList = new ArrayList<>(4);

    public SavedTransactionMemories(TransactionMemoriesProvider firstProvider) {
        providerList.add(firstProvider);
    }

    public void registerNextProvider(TransactionMemoriesProvider nextProvider) {
        providerList.add(nextProvider);
    }

    public List<TransactionMemoriesProvider> getOrderedProviderList() {
        final List<TransactionMemoriesProvider> copied = new ArrayList<>(providerList);
        Collections.reverse(copied); // order by beginning (registered by ending)
        return Collections.unmodifiableList(copied);
    }

    @Override
    public String toString() {
        return DfTypeUtil.toClassTitle(this) + ":{providerList=" + providerList.size() + "}";
    }
}
