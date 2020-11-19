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

/**
 * @param <RESULT> The type of transaction result.
 * @author jflute
 */
public class BegunTx<RESULT> {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final TransactionGenre genre; // not null
    protected RESULT result; // null allowed
    protected boolean rollbackOnly;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public BegunTx(TransactionGenre genre) {
        this.genre = genre;
    }

    // ===================================================================================
    //                                                                   Transaction Genre
    //                                                                   =================
    public boolean isRequired() {
        return TransactionGenre.REQUIRED.equals(genre);
    }

    public boolean isRequiresNew() {
        return TransactionGenre.REQUIRES_NEW.equals(genre);
    }

    // ===================================================================================
    //                                                                  Transaction Result
    //                                                                  ==================
    /**
     * Returns the result of the transaction process to caller.
     * <pre>
     * <span style="color: #3F7E5E">// if returns anything to caller</span> 
     * <span style="color: #994747">Member member</span> = (Member)requiresNew(<span style="color: #553000">tx</span> <span style="font-size: 120%">-</span>&gt;</span> {
     *     update(...);
     *     Member member = select(...);
     *     <span style="color: #553000">tx</span>.<span style="color: #CC4747">returns</span>(member); <span style="color: #3F7E5E">// for return</span>
     * }).<span style="color: #994747">get()</span>; <span style="color: #3F7E5E">// optional handling</span>
     * </pre>
     * @param result The result of the transaction process. (NullAllowed: means no result)
     */
    public void returns(RESULT result) {
        this.result = result;
    }

    // ===================================================================================
    //                                                                       Rollback Only
    //                                                                       =============
    public void rollbackOnly() {
        rollbackOnly = true;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public RESULT getResult() {
        return result;
    }

    public boolean isRollbackOnly() {
        return rollbackOnly;
    }
}
