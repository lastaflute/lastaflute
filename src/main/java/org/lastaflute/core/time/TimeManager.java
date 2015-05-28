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
package org.lastaflute.core.time;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.TimeZone;

import org.dbflute.helper.HandyDate;

/**
 * The manager of time. e.g. to provide current date <br>
 * The word 'current' means transaction beginning in transaction if default setting of Framework.
 * The same date (but different instances) in the same transaction is handled
 * in all methods that use current date. <br>
 * @author jflute
 */
public interface TimeManager {

    // ===================================================================================
    //                                                                    Current Handling
    //                                                                    ================
    /**
     * Get the date that specifies current date for business. <br>
     * The word 'current' means transaction beginning in transaction if default setting of Framework. <br>
     * You can get the same date (but different instances) in the same transaction.
     * @return The local date that has current date. (NotNull)
     */
    LocalDate currentDate();

    /**
     * Get the date-time that specifies current time for business. <br>
     * The word 'current' means transaction beginning in transaction if default setting of Framework. <br>
     * You can get the same date (but different instances) in the same transaction.
     * @return The local date-time that has current time. (NotNull)
     */
    LocalDateTime currentDateTime();

    /**
     * Get the handy date that specifies current time for business. <br>
     * The word 'current' means transaction beginning in transaction if default setting of Framework. <br>
     * You can get the same date (but different instances) in the same transaction.
     * @return The handy date that has current time. (NotNull)
     */
    HandyDate currentHandyDate();

    /**
     * Get the time millisecond that specifies current time for business. <br>
     * The word 'current' means transaction beginning in transaction if default setting of Framework. <br>
     * You can get the same date (but different instances) in the same transaction.
     * @return The long value for current time. (NotNull)
     */
    long currentMillis();

    /**
     * Get the local date that specifies current time for business. <br>
     * The word 'current' means transaction beginning in transaction if default setting of Framework. <br>
     * You can get the same date (but different instances) in the same transaction.
     * @return The local date that has current time. (NotNull)
     */
    Date currentUtilDate();

    /**
     * Get the time-stamp that specifies current time for business. <br>
     * The word 'current' means transaction beginning in transaction if default setting of Framework. <br>
     * You can get the same date (but different instances) in the same transaction.
     * @return The time-stamp that has current time. (NotNull)
     */
    Timestamp currentTimestamp();

    /**
     * Get the date that specifies method-called time (flash time). <br>
     * This method ignores whether it is in transaction or not.
     * @return The date that has method-called time (flash time). (NotNull) 
     */
    Date flashDate();

    // ===================================================================================
    //                                                                   Business Handling
    //                                                                   =================
    /**
     * Is the target date business day? <br>
     * The determination accuracy is depends on logic of implementation class. <br>
     * You can adjust it by FwAssistantDirector.
     * @param targetDate The date for determination. (NotNull)
     * @return The determination, true or false.
     */
    boolean isBusinessDate(Date targetDate);

    /**
     * Get the next (added) date as business day. <br>
     * The determination logic uses isBusinessDate(Date).
     * @param baseDate The base date for calculation. (NotNull)
     * @param addedDay The count of added days.
     * @return The new-created instance of the next business date. (NotNull)
     */
    Date getNextBusinessDate(Date baseDate, int addedDay);

    /**
     * Get the time-zone for the business.
     * @return The time-zone instance. (NotNull)
     */
    TimeZone getBusinessTimeZone();
}
