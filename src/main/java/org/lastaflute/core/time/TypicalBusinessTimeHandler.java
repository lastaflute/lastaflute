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
package org.lastaflute.core.time;

import java.time.LocalDate;
import java.util.Date;
import java.util.TimeZone;

import org.dbflute.util.DfTypeUtil;

/**
 * @author jflute
 */
public class TypicalBusinessTimeHandler implements BusinessTimeHandler {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The provider of current time. (NotNull) */
    protected final CurrentTimeProvider currentTimeProvider;

    /** The provider of final time-zone. (NotNull) */
    protected final FinalTimeZoneProvider finalTimeZoneProvider;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    /**
     * @param currentTimeProvider The provider of current time. (NotNull)
     * @param finalTimeZoneProvider The provider of final time-zone. (NotNull)
     */
    public TypicalBusinessTimeHandler(CurrentTimeProvider currentTimeProvider, FinalTimeZoneProvider finalTimeZoneProvider) {
        this.currentTimeProvider = currentTimeProvider;
        this.finalTimeZoneProvider = finalTimeZoneProvider;
    }

    // ===================================================================================
    //                                                                          Birth Date
    //                                                                          ==========
    /** {@inheritDoc} */
    public Integer calculateAge(Date birthDate) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    // ===================================================================================
    //                                                                       Business Date
    //                                                                       =============
    /** {@inheritDoc} */
    public boolean isBusinessDate(LocalDate targetDate) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    /** {@inheritDoc} */
    public Date getNextBusinessDate(LocalDate baseDate, int addedDay) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    // ===================================================================================
    //                                                                   Business TimeZone
    //                                                                   =================
    /** {@inheritDoc} */
    public TimeZone getBusinessTimeZone() {
        return finalTimeZoneProvider.finalTimeZone();
    }

    // ===================================================================================
    //                                                                       Assist Helper
    //                                                                       =============
    protected long currentTimeMillis() {
        return currentTimeProvider.currentTimeMillis();
    }

    // ===================================================================================
    //                                                                        Xxx Override
    //                                                                        ============
    @Override
    public String toString() {
        final String classTitle = DfTypeUtil.toClassTitle(this);
        final String currentTimeExp = DfTypeUtil.toClassTitle(currentTimeProvider);
        final String finalZoneExp = DfTypeUtil.toClassTitle(finalTimeZoneProvider);
        return classTitle + ":{" + currentTimeExp + ", " + finalZoneExp + "}";
    }
}