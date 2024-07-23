/*
 * Copyright 2015-2024 the original author or authors.
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

import java.time.LocalDate;
import java.util.Date;
import java.util.TimeZone;

/**
 * The handler of business time. <br>
 * You can change the business logic of {@link TimeManager} by this interface.
 * @author jflute
 */
public interface BusinessTimeHandler {

    /**
     * Calculate age from the specified birth-date. 
     * @param birthDate The date as birth-date. (NotNull)
     * @return The calculated result as integer. (NotNull)
     */
    Integer calculateAge(Date birthDate);

    /**
     * Is the target date business day?
     * @param targetDate The local date for determination. (NotNull)
     * @return The determination, true or false.
     */
    boolean isBusinessDate(LocalDate targetDate);

    /**
     * Get the next (added) date as business day. <br>
     * The determination logic uses {@link #isBusinessDate(LocalDate)}.
     * @param baseDate The base local date for calculation. (NotNull)
     * @param addedDay The count of added days.
     * @return The new-created instance of the next business date. (NotNull)
     */
    Date getNextBusinessDate(LocalDate baseDate, int addedDay);

    /**
     * Get the time-zone for the business.
     * @return The time-zone instance. (NotNull)
     */
    TimeZone getBusinessTimeZone();
}