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
package org.lastaflute.db.jta.stage;

import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 */
public interface TransactionStage {

    /**
     * Execute the show in transaction (inherits outer transaction), roll-backed if exception.
     * <pre>
     * <span style="color: #3F7E5E">// if no return</span> 
     * required(<span style="color: #553000">tx</span> <span style="font-size: 120%">-</span>&gt;</span> {
     *     update(...); <span style="color: #3F7E5E">// already in transaction</span>
     *     insert(...); <span style="color: #3F7E5E">// also here</span>
     * });
     * 
     * <span style="color: #3F7E5E">// if returns anything to caller</span> 
     * <span style="color: #994747">Member member</span> = (Member)required(<span style="color: #553000">tx</span> <span style="font-size: 120%">-</span>&gt;</span> {
     *     update(...);
     *     Member member = select(...);
     *     <span style="color: #553000">tx</span>.<span style="color: #CC4747">returns</span>(member); <span style="color: #3F7E5E">// for return</span>
     * }).<span style="color: #994747">get()</span>; <span style="color: #3F7E5E">// optional handling</span>
     * </pre>
     * @param txLambda The callback for your transaction show on the stage. (NotNull)
     * @return The optional result of the transaction show. (NullAllowed)
     */
    <RESULT> OptionalThing<RESULT> required(TransactionShow<RESULT> txLambda);

    /**
     * Execute the show in transaction (always new transaction), roll-backed if exception.
     * <pre>
     * <span style="color: #3F7E5E">// if no return</span> 
     * requiresNew(<span style="color: #553000">tx</span> <span style="font-size: 120%">-</span>&gt;</span> {
     *     update(...); <span style="color: #3F7E5E">// already in transaction</span>
     *     insert(...); <span style="color: #3F7E5E">// also here</span>
     * });
     * 
     * <span style="color: #3F7E5E">// if returns anything to caller</span> 
     * <span style="color: #994747">Member member</span> = (Member)requiresNew(<span style="color: #553000">tx</span> <span style="font-size: 120%">-</span>&gt;</span> {
     *     update(...);
     *     Member member = select(...);
     *     <span style="color: #553000">tx</span>.<span style="color: #CC4747">returns</span>(member); <span style="color: #3F7E5E">// for return</span>
     * }).<span style="color: #994747">get()</span>; <span style="color: #3F7E5E">// optional handling</span>
     * </pre>
     * @param txLambda The callback for your transaction show on the stage. (NotNull)
     * @return The optional result of the transaction show. (NullAllowed)
     */
    <RESULT> OptionalThing<RESULT> requiresNew(TransactionShow<RESULT> txLambda);

    /**
     * Execute the show in transaction by selected genre, roll-backed if exception.
     * @param txLambda The callback for your transaction show on the stage. (NotNull)
     * @param genre The genre of transaction, also contains non transaction. (NotNull)
     * @return The optional result of the transaction show. (NullAllowed)
     */
    <RESULT> OptionalThing<RESULT> selectable(TransactionShow<RESULT> txLambda, TransactionGenre genre);
}
