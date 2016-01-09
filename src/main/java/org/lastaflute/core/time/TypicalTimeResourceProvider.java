/*
 * Copyright 2015-2016 the original author or authors.
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

import java.util.Date;
import java.util.TimeZone;

import org.dbflute.util.DfTypeUtil;

/**
 * The typical provider of time resource.
 * @author jflute
 */
public abstract class TypicalTimeResourceProvider implements TimeResourceProvider {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RelativeDateScript script = newRelativeDateScript();

    protected RelativeDateScript newRelativeDateScript() {
        return new RelativeDateScript();
    }

    // ===================================================================================
    //                                                                      Basic Handling
    //                                                                      ==============
    public BusinessTimeHandler provideBusinessTimeHandler(TimeManager timeManager) {
        return new TypicalBusinessTimeHandler(() -> {
            return timeManager.currentMillis();
        } , () -> {
            return getCentralTimeZone();
        });
    }

    protected abstract TimeZone getCentralTimeZone();

    public boolean isCurrentIgnoreTransaction() {
        // this project uses transaction time for current date
        return false; // fixedly
    }

    // ===================================================================================
    //                                                                     Time Adjustment
    //                                                                     ===============
    // *1: called per called for dynamic change in development
    public boolean isAdjustAbsoluteMode() { // *1
        return getTimeAdjustTimeMillis().startsWith("$"); // means absolute e.g. $(2014/07/10)
    }

    public long provideAdjustTimeMillis() { // *1
        final String exp = getTimeAdjustTimeMillis();
        try {
            return doProvideAdjustTimeMillis(exp);
        } catch (RuntimeException e) {
            String msg = "Illegal property for time.adjust.time.millis: " + exp;
            throw new IllegalStateException(msg);
        }
    }

    protected long doProvideAdjustTimeMillis(final String exp) {
        if (exp.startsWith("$")) { // absolute e.g. $(2014/07/10)
            return script.resolveHardCodingDate(exp).getTime();
        } else if (exp.contains("(")) { // relative e.g. addDay(3)
            final long current = System.currentTimeMillis();
            final Date resolved = script.resolveRelativeDate(exp, new Date(current));
            return resolved.getTime() - current;
        } else { // should be millisecond as relative
            return getTimeAdjustTimeMillisAsLong();
        }
    }

    protected abstract String getTimeAdjustTimeMillis();

    protected abstract Long getTimeAdjustTimeMillisAsLong();

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final String hash = Integer.toHexString(hashCode());
        return DfTypeUtil.toClassTitle(this) + ":{" + script.getClass().getSimpleName() + "}@" + hash;
    }
}
