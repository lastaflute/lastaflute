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
package org.lastaflute.db.direction;

import org.lastaflute.core.direction.exception.FwRequiredAssistNotFoundException;
import org.lastaflute.db.dbcp.ConnectionPoolAdjustmentProvider;
import org.lastaflute.db.dbcp.XAConnectionHook;
import org.lastaflute.db.dbflute.classification.ListedClassificationProvider;
import org.lastaflute.db.jta.stage.VestibuleTxProvider;

/**
 * @author jflute
 */
public class FwDbDirection {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                        Classification
    //                                        --------------
    protected ListedClassificationProvider listedClassificationProvider;

    // -----------------------------------------------------
    //                                                 JDBC
    //                                                ------
    protected ConnectionPoolAdjustmentProvider connectionPoolAdjustmentProvider;
    protected XAConnectionHook newbornConnectionHook;
    protected VestibuleTxProvider vestibuleTxProvider;

    // ===================================================================================
    //                                                                     Direct Property
    //                                                                     ===============
    // -----------------------------------------------------
    //                                        Classification
    //                                        --------------
    public void directClassification(ListedClassificationProvider listedClassificationProvider) {
        assertArgumentNotNull("listedClassificationProvider", listedClassificationProvider);
        this.listedClassificationProvider = listedClassificationProvider;
    }

    // -----------------------------------------------------
    //                                                 JDBC
    //                                                ------
    public void directConnectionPool(ConnectionPoolAdjustmentProvider connectionPoolAdjustmentProvider) {
        this.connectionPoolAdjustmentProvider = connectionPoolAdjustmentProvider;
    }

    public void directNewbornConnection(XAConnectionHook newbornConnectionHook) {
        this.newbornConnectionHook = newbornConnectionHook;
    }

    public void directVestibuleTx(VestibuleTxProvider vestibuleTxProvider) {
        this.vestibuleTxProvider = vestibuleTxProvider;
    }

    // ===================================================================================
    //                                                                              Assist
    //                                                                              ======
    // -----------------------------------------------------
    //                                        Classification
    //                                        --------------
    public ListedClassificationProvider assistListedClassificationProvider() {
        assertAssistObjectNotNull(listedClassificationProvider, "Not found the provider for listed classification.");
        return listedClassificationProvider;
    }

    // -----------------------------------------------------
    //                                                 JDBC
    //                                                ------
    public ConnectionPoolAdjustmentProvider assistConnectionPoolAdjustmentProvider() {
        return connectionPoolAdjustmentProvider; // not required, it's optional assist
    }

    public XAConnectionHook assistNewbornConnectionHook() {
        return newbornConnectionHook; // not required, it's optional assist
    }

    public VestibuleTxProvider assistVestibuleTxProvider() {
        return vestibuleTxProvider; // not required, it's optional assist
    }

    // ===================================================================================
    //                                                                       Assert Helper
    //                                                                       =============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            throw new IllegalArgumentException("The variableName should not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
        }
    }

    protected void assertAssistObjectNotNull(Object obj, String msg) {
        if (obj == null) {
            throw new FwRequiredAssistNotFoundException(msg);
        }
    }
}
