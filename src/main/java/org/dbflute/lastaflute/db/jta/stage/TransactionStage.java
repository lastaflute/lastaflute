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

import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 */
public interface TransactionStage {

    /**
     * Execute the show in transaction (inherits outer transaction), roll-backed if exception.
     * @param noArgLambda The callback for your transaction show on the stage. (NotNull)
     * @return The optional result of the transaction show. (NullAllowed)
     */
    <RESULT> OptionalThing<RESULT> required(TransactionShow<RESULT> noArgLambda);

    /**
     * Execute the show in transaction (always new transaction), roll-backed if exception.
     * @param noArgLambda The callback for your transaction show on the stage. (NotNull)
     * @return The optional result of the transaction show. (NullAllowed)
     */
    <RESULT> OptionalThing<RESULT> requiresNew(TransactionShow<RESULT> noArgLambda);

    /**
     * Execute the show in transaction by selected genre, roll-backed if exception.
     * @param noArgLambda The callback for your transaction show on the stage. (NotNull)
     * @param genre The genre of transaction, also contains non transaction. (NotNull)
     * @return The optional result of the transaction show. (NullAllowed)
     */
    <RESULT> OptionalThing<RESULT> selectable(TransactionShow<RESULT> noArgLambda, TransactionGenre genre);
}
