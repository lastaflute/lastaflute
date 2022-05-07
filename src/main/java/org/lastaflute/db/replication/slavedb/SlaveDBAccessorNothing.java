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

// once made this deprecated, but feedback from user who uses this by jflute (2021/06/10)
/**
 * @author jflute
 */
public class SlaveDBAccessorNothing implements SlaveDBAccessor {

    @Override
    public <RESULT> RESULT accessFixedly(SlaveDBCallback<RESULT> noArgLambda) {
        assertCallbackNotNull(noArgLambda);
        return noArgLambda.callback();
    }

    @Override
    public <RESULT> RESULT accessIfNeeds(SlaveDBCallback<RESULT> noArgLambda, boolean toSlave) {
        assertCallbackNotNull(noArgLambda);
        return noArgLambda.callback();
    }

    @Override
    public <RESULT> RESULT accessRandomFifty(SlaveDBCallback<RESULT> noArgLambda, long determinationNumber) {
        assertCallbackNotNull(noArgLambda);
        return noArgLambda.callback();
    }

    protected <RESULT> void assertCallbackNotNull(SlaveDBCallback<RESULT> callback) {
        if (callback == null) {
            String msg = "The argument 'noArgLambda' should not be null.";
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    public String prepareMasterDataSourceKey() { // unused
        return MASTER_DB;
    }

    @Override
    public String prepareSlaveDataSourceKey() { // unused
        return SLAVE_DB;
    }
}
