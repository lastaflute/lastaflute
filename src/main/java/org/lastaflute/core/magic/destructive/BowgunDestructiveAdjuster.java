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
package org.lastaflute.core.magic.destructive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is basically for UnitTest. <br>
 * So don't use at production code.
 * @author jflute
 * @since 0.8.4 (2016/09/05 Monday)
 */
public class BowgunDestructiveAdjuster {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(BowgunDestructiveAdjuster.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected static boolean asyncToNormalSync; // async manager
    protected static boolean requiresNewToRequired; // transaction stage
    protected static boolean locked = true;

    // ===================================================================================
    //                                                                        Shoot Bowgun
    //                                                                        ============
    public static void shootBowgunAsyncToNormalSync() {
        assertUnlocked();
        if (logger.isInfoEnabled()) {
            logger.info("...Shooting bowgun: changing asynchronous to (normal) synchronous of AsyncManager");
        }
        asyncToNormalSync = true;
        lock();
    }

    public static void shootBowgunRequiresNewToRequired() {
        assertUnlocked();
        if (logger.isInfoEnabled()) {
            logger.info("...Shooting bowgun: changing requiresNew() to required() of TransactionStage");
        }
        requiresNewToRequired = true;
        lock();
    }

    // ===================================================================================
    //                                                                             Restore
    //                                                                             =======
    public static void restoreBowgunAll() {
        assertUnlocked();
        if (logger.isInfoEnabled()) {
            logger.info("...Restoring all bowgun destructive adjusters");
        }
        asyncToNormalSync = false;
        requiresNewToRequired = false;
        lock();
    }

    public static void restoreBowgunAsyncToNormalSync() {
        assertUnlocked();
        if (logger.isInfoEnabled()) {
            logger.info("...Restoring bowgun destructive adjuster of async");
        }
        asyncToNormalSync = false;
        lock();
    }

    public static void restoreBowgunRequiresNewToRequired() {
        assertUnlocked();
        if (logger.isInfoEnabled()) {
            logger.info("...Restoring bowgun destructive adjuster of requiresNew");
        }
        requiresNewToRequired = false;
        lock();
    }

    // ===================================================================================
    //                                                                       Determination
    //                                                                       =============
    public static boolean hasAnyBowgun() {
        return isAsyncToNormalSync() || isRequiresNewToRequired();
    }

    public static boolean isAsyncToNormalSync() {
        return asyncToNormalSync;
    }

    public static boolean isRequiresNewToRequired() {
        return requiresNewToRequired;
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
            logger.info("...Locking the destructive adjuster!");
        }
        locked = true;
    }

    public static void unlock() {
        if (!locked) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("...Unlocking the destructive adjuster!");
        }
        locked = false;
    }

    protected static void assertUnlocked() {
        if (!isLocked()) {
            return;
        }
        throw new IllegalStateException("The destructive adjuster is locked.");
    }
}
