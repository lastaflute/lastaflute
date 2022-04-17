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
package org.lastaflute.core.time;

/**
 * The provider of time resource.
 * @author jflute
 */
public interface TimeResourceProvider {

    /**
     * Provide the handler of business time.
     * @param timeManager The manager of time to get current date. (NotNull)
     * @return The instance of the handler. (NotNull)
     */
    BusinessTimeHandler provideBusinessTimeHandler(TimeManager timeManager);

    /**
     * Does it ignore transaction for current date? <br>
     * If true, current time always be flash date.
     * @return The determination, true or false.
     */
    boolean isCurrentIgnoreTransaction();

    /**
     * Provide the milliseconds to adjust current time. <br>
     * See {@link TimeManager}'s implementation class for the detail.
     * @return The milliseconds to adjust current time.
     */
    long provideAdjustTimeMillis();

    /**
     * Does it adjust as absolute mode? <br>
     * See {@link TimeManager}'s implementation class for the detail.
     * @return The determination, true or false.
     */
    boolean isAdjustAbsoluteMode();

    /**
     * Provider the provider of real current time.
     * @return The instance of the provider. (NullAllowed: option, so normally null)
     */
    default CurrentTimeProvider provideRealCurrentTimeProvider() {
        return null; // use embedded as default
    }
}
