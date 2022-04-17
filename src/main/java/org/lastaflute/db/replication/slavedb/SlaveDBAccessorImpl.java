/*
 * Copyright 2015-2022 the original author or authors.
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
import org.lastaflute.di.util.LdiSrl;
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
    private SelectableDataSourceHolder selectableDataSourceHolder; // needs selectable_datasource.xml

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
            final String slaveKey = prepareSlaveDataSourceKey();
            if (logger.isDebugEnabled()) {
                logger.debug(buildSlaveDBAccessDebugMessage(slaveKey));
            }
            setupForcedMasterCallback();
            selectableDataSourceHolder.switchSelectableDataSourceKey(slaveKey);
            return callback.callback();
        } finally {
            selectableDataSourceHolder.switchSelectableDataSourceKey(currentKey);
            clearForcedMasterCallback();
        }
    }

    protected String buildSlaveDBAccessDebugMessage(String slaveKey) {
        return "...Accessing to SlaveDB for " + mySchemaDisp() + " by the key: " + slaveKey;
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
        final String masterKey = prepareMasterDataSourceKey();
        if (logger.isDebugEnabled()) {
            logger.debug(buildMasterAccessFixedlyDebugMessage(masterKey));
        }
        selectableDataSourceHolder.switchSelectableDataSourceKey(masterKey);
        try {
            return noArgLambda.callback();
        } finally {
            selectableDataSourceHolder.switchSelectableDataSourceKey(currentKey);
        }
    }

    protected String buildMasterAccessFixedlyDebugMessage(String masterKey) {
        return "...Accessing to MasterDB for " + mySchemaDisp() + " fixedly by the key: " + masterKey;
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
                    final String masterKey = prepareMasterDataSourceKey();
                    currentKey = selectableDataSourceHolder.getCurrentSelectableDataSourceKey();
                    if (!masterKey.equals(currentKey)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug(buildForcedMasterHookDebugMessage(masterKey));
                        }
                        selectableDataSourceHolder.switchSelectableDataSourceKey(masterKey);
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

    protected String buildForcedMasterHookDebugMessage(String masterKey) {
        return "...Accessing to MasterDB for " + mySchemaDisp() + " forcedly by the key: " + masterKey;
    }

    protected boolean isExistingHookInherited() {
        return true;
    }

    protected void clearForcedMasterCallback() {
        CallbackContext.clearBehaviorCommandHookOnThread();
    }

    // ===================================================================================
    //                                                                         Schema Info
    //                                                                         ===========
    protected String mySchemaDisp() { // for debug log, not null
        final String mySchemaKeyword = mySchemaKeyword();
        return mySchemaKeyword != null ? mySchemaKeyword : getDefalutSchemaDisp();
    }

    protected String getDefalutSchemaDisp() {
        return "main schema";
    }

    // ===================================================================================
    //                                                                      DataSource Key
    //                                                                      ==============
    @Override
    public String prepareMasterDataSourceKey() {
        return MASTER_DB + getSchemaSuffix();
    }

    @Override
    public String prepareSlaveDataSourceKey() {
        return SLAVE_DB + getSchemaSuffix();
    }

    protected String getSchemaSuffix() {
        final String keyword = mySchemaKeyword(); // null allowed
        return keyword != null ? LdiSrl.initCap(keyword) : ""; // e.g. seadb to Seadb (for masterSeadb)
    }

    protected String mySchemaKeyword() { // you can override (if e.g. multiple DB)
        return null; // no schema keyword as default
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
}
