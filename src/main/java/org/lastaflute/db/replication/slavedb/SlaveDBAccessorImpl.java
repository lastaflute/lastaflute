/*
 * Copyright 2015-2018 the original author or authors.
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
package org.lastaflute.db.replication.slavedb;

import javax.annotation.Resource;

import org.dbflute.bhv.core.BehaviorCommandHook;
import org.dbflute.bhv.core.BehaviorCommandMeta;
import org.dbflute.hook.CallbackContext;
import org.lastaflute.db.replication.selectable.SelectableDataSourceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class SlaveDBAccessorImpl implements SlaveDBAccessor {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(SlaveDBAccessorImpl.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    @Resource
    protected SelectableDataSourceHolder selectableDataSourceHolder;

    // ===================================================================================
    //                                                                      SlaveDB Access
    //                                                                      ==============
    // -----------------------------------------------------
    //                                               Fixedly
    //                                               -------
    @Override
    public <RESULT> RESULT accessFixedly(SlaveDBCallback<RESULT> noArgLambda) {
        assertCallbackNotNull(noArgLambda);
        return doAccessFixedly(noArgLambda);
    }

    protected <RESULT> RESULT doAccessFixedly(SlaveDBCallback<RESULT> callback) {
        assertCallbackNotNull(callback);
        final String currentKey = selectableDataSourceHolder.getCurrentSelectableDataSourceKey();
        try {
            final String slaveDB = SLAVE_DB;
            if (logger.isDebugEnabled()) {
                logger.debug(buildSlaveDBAccessDebugMessage(slaveDB));
            }
            setupForcedMasterCallback();
            selectableDataSourceHolder.switchSelectableDataSourceKey(slaveDB);
            return callback.callback();
        } finally {
            selectableDataSourceHolder.switchSelectableDataSourceKey(currentKey);
            clearForcedMasterCallback();
        }
    }

    protected String buildSlaveDBAccessDebugMessage(String slaveDB) {
        return "...Accessing to SlaveDB for " + mySchemaDisp() + ": " + slaveDB;
    }

    // -----------------------------------------------------
    //                                               IfNeeds
    //                                               -------
    @Override
    public <RESULT> RESULT accessIfNeeds(SlaveDBCallback<RESULT> noArgLambda, boolean toSlave) {
        assertCallbackNotNull(noArgLambda);
        if (toSlave) {
            return doAccessFixedly(noArgLambda);
        } else {
            return noArgLambda.callback();
        }
    }

    // -----------------------------------------------------
    //                                         Random Access
    //                                         -------------
    @Override
    public <RESULT> RESULT accessRandomFifty(SlaveDBCallback<RESULT> noArgLambda, long determinationNumber) {
        assertCallbackNotNull(noArgLambda);
        if (isRandomFiftyHit(determinationNumber)) {
            return doAccessFixedly(noArgLambda);
        } else {
            return noArgLambda.callback();
        }
    }

    protected boolean isRandomFiftyHit(long determinationNumber) {
        return (determinationNumber % 2) == 0;
    }

    // ===================================================================================
    //                                                                        Fixed Master
    //                                                                        ============
    // you can use in public methods in your project component that inherits this
    // (it might be available in your UnitTest, so this method was prepared)
    protected <RESULT> RESULT doMasterAccessFixedly(SlaveDBCallback<RESULT> noArgLambda) {
        assertCallbackNotNull(noArgLambda);
        final String currentKey = selectableDataSourceHolder.getCurrentSelectableDataSourceKey();
        final String masterDB = MASTER_DB;
        if (logger.isDebugEnabled()) {
            logger.debug(buildMasterAccessFixedlyDebugMessage(masterDB));
        }
        selectableDataSourceHolder.switchSelectableDataSourceKey(masterDB);
        try {
            return noArgLambda.callback();
        } finally {
            selectableDataSourceHolder.switchSelectableDataSourceKey(currentKey);
        }
    }

    protected String buildMasterAccessFixedlyDebugMessage(String masterDB) {
        return "...Accessing to MasterDB for " + mySchemaDisp() + " fixedly: " + masterDB;
    }

    // ===================================================================================
    //                                                                       Forced Master
    //                                                                       =============
    protected void setupForcedMasterCallback() {
        CallbackContext.setBehaviorCommandHookOnThread(createForcedMasterHook());
    }

    protected BehaviorCommandHook createForcedMasterHook() {
        return new BehaviorCommandHook() {

            protected String currentKey;
            protected boolean forcedSet;

            public void hookBefore(BehaviorCommandMeta meta) {
                if (needsForcedMasterCommand(meta)) {
                    final String masterDB = MASTER_DB;
                    currentKey = selectableDataSourceHolder.getCurrentSelectableDataSourceKey();
                    if (!masterDB.equals(currentKey)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug(buildForcedMasterHookDebugMessage(masterDB));
                        }
                        selectableDataSourceHolder.switchSelectableDataSourceKey(masterDB);
                        forcedSet = true;
                    }
                }
            }

            public void hookFinally(BehaviorCommandMeta meta, RuntimeException cause) {
                if (forcedSet) {
                    selectableDataSourceHolder.switchSelectableDataSourceKey(currentKey);
                }
            }

            @Override
            public boolean inheritsExistingHook() {
                return isExistingHookInherited(); // not override existing hook
            }
        };
    }

    protected boolean needsForcedMasterCommand(BehaviorCommandMeta meta) {
        return !meta.isSelect();
    }

    protected String buildForcedMasterHookDebugMessage(String masterDB) {
        return "...Accessing to MasterDB for " + mySchemaDisp() + " forcedly: " + masterDB;
    }

    protected boolean isExistingHookInherited() {
        return true;
    }

    protected void clearForcedMasterCallback() {
        CallbackContext.clearBehaviorCommandHookOnThread();
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected <RESULT> void assertCallbackNotNull(SlaveDBCallback<RESULT> callback) {
        if (callback == null) {
            String msg = "The argument 'noArgLambda' should not be null.";
            throw new IllegalArgumentException(msg);
        }
    }

    // ===================================================================================
    //                                                                         Schema Info
    //                                                                         ===========
    protected String mySchemaDisp() {
        return "main schema";
    }
}
