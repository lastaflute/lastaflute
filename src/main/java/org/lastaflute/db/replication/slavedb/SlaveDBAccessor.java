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
package org.lastaflute.db.replication.slavedb;

/**
 * @author jflute
 */
public interface SlaveDBAccessor {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    public static final String MASTER_DB = "master";
    public static final String SLAVE_DB = "slave";

    // ===================================================================================
    //                                                                      SlaveDB Access
    //                                                                      ==============
    // -----------------------------------------------------
    //                                               Fixedly
    //                                               -------
    /**
     * Access to SlaveDB fixedly, which means it always selects SlaveDB in the call-back.
     * <pre>
     * slaveDBAccessor.<span style="color: #CC4747">accessFixedly</span>(() <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     return memberBhv.selectList(...);
     * });
     * </pre>
     * <p>But if you have update accesses in the call-back, only the accesses are executed
     * to MasterDB forcedly because update statements should be executed to MasterDB.
     * And procedure calls are contained to update statements even if the procedure
     * has only select statements because of no determination factor.</p>
     * @param <RESULT> The type of result of call-back process
     * @param noArgLambda The implementation of call-back process for accessing to SlaveDB. (NotNull)
     * @return The result of call-back process. (NullAllowed)
     */
    <RESULT> RESULT accessFixedly(SlaveDBCallback<RESULT> noArgLambda);

    // -----------------------------------------------------
    //                                               IfNeeds
    //                                               -------
    /**
     * Access to SlaveDB if it needs, which means it selects SlaveDB in the call-back depending on the determination.
     * <pre>
     * slaveDBAccessor.<span style="color: #CC4747">accessFixedly</span>(() <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     return memberBhv.selectList(...);
     * }, isNotEmpty(seaCode));
     * </pre>
     * <p>But if you have update accesses in the call-back, only the accesses are executed
     * to MasterDB forcedly because update statements should be executed to MasterDB.
     * And procedure calls are contained to update statements even if the procedure
     * has only select statements because of no determination factor.</p>
     * @param <RESULT> The type of result of call-back process
     * @param noArgLambda The implementation of call-back process for accessing to SlaveDB. (NotNull)
     * @param toSlave Does it access to slave?
     * @return The result of call-back process. (NullAllowed)
     */
    <RESULT> RESULT accessIfNeeds(SlaveDBCallback<RESULT> noArgLambda, boolean toSlave);

    // -----------------------------------------------------
    //                                         Random Access
    //                                         -------------
    /**
     * Access to SlaveDB randomly, which means it selects SlaveDB fifty-fifty in the call-back.
     * <pre>
     * slaveDBAccessor.<span style="color: #CC4747">accessRandomFifty</span>(() <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     return memberBhv.selectList(...);
     * });
     * </pre>
     * <p>But if you have update accesses in the call-back, only the accesses are executed
     * to MasterDB forcedly because update statements should be executed to MasterDB.
     * And procedure calls are contained to update statements even if the procedure
     * has only select statements because of no determination factor.</p>
     * 
     * <p>Random access depends on whether specified determination number is even number or not.
     * It means same number provides same DB access.</p>
     * @param <RESULT> The type of result of call-back process
     * @param noArgLambda The implementation of call-back process for accessing to SlaveDB. (NotNull)
     * @param determinationNumber The number for determination of random access.
     * @return The result of call-back process. (NullAllowed)
     */
    <RESULT> RESULT accessRandomFifty(SlaveDBCallback<RESULT> noArgLambda, long determinationNumber);
}
