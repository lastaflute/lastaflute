/*
 * Copyright 2015-2021 the original author or authors.
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

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.dbflute.helper.HandyDate;
import org.dbflute.system.DBFluteSystem;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.direction.FwCoreDirection;
import org.lastaflute.core.direction.exception.FwRequiredAssistNotFoundException;
import org.lastaflute.core.magic.TransactionTimeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class SimpleTimeManager implements TimeManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(SimpleTimeManager.class);

    // -----------------------------------------------------
    //                                              Stateful
    //                                              --------
    protected static CurrentTimeProvider bowgunCurrentTimeProvider; // used for current
    protected static boolean locked = true;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant director (AD) for framework. (NotNull: after initialization) */
    @Resource
    private FwAssistantDirector assistantDirector;

    /** The handler of business time. (NotNull: after initialization) */
    protected BusinessTimeHandler businessTimeHandler;

    /** The provider of time resource for development. (NotNull: only when development) */
    protected TimeResourceProvider developmentProvider;

    /** Does it ignore transaction time when the time manager returns current date? (not used if development) */
    protected boolean currentIgnoreTransaction;

    /** if adjustAbsoluteMode is true, absolute milliseconds, else relative milliseconds. (not used if development) */
    protected long adjustTimeMillis;

    /** Is it absolute time mode when using adjustTimeMillis? (not used if development) */
    protected boolean adjustAbsoluteMode;

    /** The provider of current time as real time. (NullAllowed: option, so normally null) */
    protected CurrentTimeProvider realCurrentTimeProvider;

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    /**
     * Initialize this component. <br>
     * This is basically called by DI setting file.
     */
    @PostConstruct
    public synchronized void initialize() {
        final FwCoreDirection direction = assistCoreDirection();
        final TimeResourceProvider provider = direction.assistTimeResourceProvider();
        businessTimeHandler = provider.provideBusinessTimeHandler(this);
        if (businessTimeHandler == null) {
            String msg = "The provider returned null business-time handler: " + provider;
            throw new FwRequiredAssistNotFoundException(msg);
        }
        if (direction.isDevelopmentHere()) {
            developmentProvider = provider;
        } else {
            currentIgnoreTransaction = provider.isCurrentIgnoreTransaction();
            adjustTimeMillis = provider.provideAdjustTimeMillis();
            adjustAbsoluteMode = provider.isAdjustAbsoluteMode();
        }
        realCurrentTimeProvider = provider.provideRealCurrentTimeProvider(); // null allowed
        showBootLogging();
    }

    protected FwCoreDirection assistCoreDirection() {
        return assistantDirector.assistCoreDirection();
    }

    protected void showBootLogging() {
        if (logger.isInfoEnabled()) {
            logger.info("[Time Manager]");
            logger.info(" businessTimeHandler: " + businessTimeHandler);
            if (developmentProvider != null) { // in development
                logger.info(" developmentProvider: " + developmentProvider);
            } else {
                logger.info(" currentIgnoreTransaction: " + currentIgnoreTransaction);
                logger.info(" adjustTimeMillis: " + adjustTimeMillis);
                logger.info(" adjustAbsoluteMode: " + adjustAbsoluteMode);
            }
            if (realCurrentTimeProvider != null) {
                logger.info(" realCurrentTimeProvider: " + realCurrentTimeProvider);
            }
        }
    }

    // ===================================================================================
    //                                                                             Current
    //                                                                             =======
    // don't use business-time handler in current-time process
    // the handler uses these processes...
    @Override
    public LocalDate currentDate() {
        return DfTypeUtil.toLocalDate(currentUtilDate(), getBusinessTimeZone());
    }

    @Override
    public LocalDateTime currentDateTime() {
        return DfTypeUtil.toLocalDateTime(currentUtilDate(), getBusinessTimeZone());
    }

    @Override
    public HandyDate currentHandyDate() {
        return new HandyDate(currentUtilDate());
    }

    @Override
    public long currentMillis() {
        return currentTimeMillis();
    }

    @Override
    public Date currentUtilDate() {
        return new Date(currentTimeMillis());
    }

    @Override
    public Timestamp currentTimestamp() {
        return new Timestamp(currentTimeMillis());
    }

    @Override
    public Date flashDate() {
        return new Date(flashTimeMillis());
    }

    // -----------------------------------------------------
    //                                         Adjusted Time
    //                                         -------------
    protected long currentTimeMillis() {
        return deriveNow(true);
    }

    protected long flashTimeMillis() {
        return deriveNow(false);
    }

    protected long deriveNow(boolean syncWithTx) {
        if (bowgunCurrentTimeProvider != null) { // most prior for e.g. UnitTest
            synchronized (SimpleTimeManager.class) { // because it may be set as null
                if (bowgunCurrentTimeProvider != null) {
                    return bowgunCurrentTimeProvider.currentTimeMillis();
                }
            }
        }
        if (syncWithTx) {
            if (TransactionTimeContext.exists()) {
                final Date transactionTime = TransactionTimeContext.getTransactionTime();
                return transactionTime.getTime();
            }
        }
        if (developmentProvider != null) {
            final boolean dynamicAbsolute = developmentProvider.isAdjustAbsoluteMode();
            final long dynamicAdjust = developmentProvider.provideAdjustTimeMillis();
            return doDeriveNow(dynamicAbsolute, dynamicAdjust);
        } else {
            return doDeriveNow(adjustAbsoluteMode, adjustTimeMillis);
        }
    }

    protected long doDeriveNow(boolean absolute, long adjust) {
        if (absolute) {
            return adjust;
        } else {
            return realCurrentTimeMillis() + adjust;
        }
    }

    protected long realCurrentTimeMillis() {
        if (realCurrentTimeProvider != null) {
            return realCurrentTimeProvider.currentTimeMillis();
        } else {
            return DBFluteSystem.currentTimeMillis();
        }
    }

    // ===================================================================================
    //                                                                       Business Date
    //                                                                       =============
    /** {@inheritDoc} */
    public boolean isBusinessDate(LocalDate targetDate) {
        assertBusinessTimeHandler();
        return businessTimeHandler.isBusinessDate(targetDate);
    }

    /** {@inheritDoc} */
    public Date getNextBusinessDate(LocalDate baseDate, int addedDay) {
        assertBusinessTimeHandler();
        return businessTimeHandler.getNextBusinessDate(baseDate, addedDay);
    }

    // ===================================================================================
    //                                                                   Business TimeZone
    //                                                                   =================
    /** {@inheritDoc} */
    public TimeZone getBusinessTimeZone() {
        return businessTimeHandler.getBusinessTimeZone();
    }

    // ===================================================================================
    //                                                          Bowgun CurrentTimeProvider
    //                                                          ==========================
    public static void shootBowgunCurrentTimeProvider(CurrentTimeProvider currentTimeProvider) {
        synchronized (SimpleTimeManager.class) { // to block while using provider
            assertUnlocked();
            if (logger.isInfoEnabled()) {
                logger.info("...Shooting bowgun current-time provider: " + currentTimeProvider);
            }
            bowgunCurrentTimeProvider = currentTimeProvider;
            lock(); // auto-lock here, because of deep world
        }
    }

    // ===================================================================================
    //                                                                         Config Lock
    //                                                                         ===========
    public static boolean isLocked() {
        return locked;
    }

    public static void lock() {
        if (locked) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("...Locking the time manager!");
        }
        locked = true;
    }

    public static void unlock() {
        if (!locked) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("...Unlocking the time manager!");
        }
        locked = false;
    }

    protected static void assertUnlocked() {
        if (!isLocked()) {
            return;
        }
        throw new IllegalStateException("The time manager is locked.");
    }

    // ===================================================================================
    //                                                                       Assert Helper
    //                                                                       =============
    protected void assertBusinessTimeHandler() {
        if (businessTimeHandler == null) {
            String msg = "Not found the business-time handler in time manager.";
            throw new IllegalStateException(msg);
        }
    }
}
