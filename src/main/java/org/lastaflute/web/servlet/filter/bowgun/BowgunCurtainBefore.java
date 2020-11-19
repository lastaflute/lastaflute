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
package org.lastaflute.web.servlet.filter.bowgun;

import java.util.ArrayList;
import java.util.List;

import org.lastaflute.core.direction.CurtainBeforeHook;
import org.lastaflute.core.direction.FwAssistantDirector;

/**
 * @author jflute
 * @since 0.7.9 (2016/01/14 Thursday)
 */
public class BowgunCurtainBefore {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected static List<CurtainBeforeHook> bowgunCurtainBeforeList;
    protected static boolean locked = true;

    // ===================================================================================
    //                                                                             Bowgun
    //                                                                            ========
    public static synchronized void handleBowgunCurtainBefore(FwAssistantDirector assistantDirector) {
        if (bowgunCurtainBeforeList != null) {
            bowgunCurtainBeforeList.forEach(bowgun -> bowgun.hook(assistantDirector));
        }
    }

    public static synchronized void shootBowgunCurtainBefore(CurtainBeforeHook oneArgLambda) {
        if (oneArgLambda == null) {
            throw new IllegalArgumentException("The argument 'oneArgLambda' should not be null.");
        }
        // basically for framework, so no info logging here
        assertUnlocked();
        if (bowgunCurtainBeforeList == null) {
            bowgunCurtainBeforeList = new ArrayList<CurtainBeforeHook>();
        }
        bowgunCurtainBeforeList.add(oneArgLambda);
        lock(); // auto-lock here, because of deep world
    }

    // ===================================================================================
    //                                                                               Lock
    //                                                                              ======
    // also no info logging here
    public static boolean isLocked() {
        return locked;
    }

    public static void lock() {
        if (locked) {
            return;
        }
        locked = true;
    }

    public static void unlock() {
        if (!locked) {
            return;
        }
        locked = false;
    }

    protected static void assertUnlocked() {
        if (!isLocked()) {
            return;
        }
        throw new IllegalStateException("The curtain-before bowgun is locked.");
    }
}
