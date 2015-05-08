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
package org.lastaflute.core.direction;

import org.lastaflute.db.direction.FwDbDirection;
import org.lastaflute.web.direction.FwWebDirection;

/**
 * @author jflute
 */
public abstract class CachedFwAssistantDirector implements FwAssistantDirector {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected FwAssistDirection assistDirection;
    protected FwCoreDirection coreDirection;
    protected FwDbDirection dbDirection;
    protected FwWebDirection webDirection;

    // ===================================================================================
    //                                                                              Assist
    //                                                                              ======
    public FwAssistDirection assistAssistDirection() {
        if (assistDirection != null) {
            return assistDirection;
        }
        synchronized (this) {
            if (assistDirection != null) {
                return assistDirection;
            }
            final FwAssistDirection direction = createAssistDirection();
            prepareAssistDirection(direction);
            assistDirection = direction;
        }
        return assistDirection;
    }

    protected FwAssistDirection createAssistDirection() {
        return new FwAssistDirection();
    }

    /**
     * Prepare the optional direction to assist. <br>
     * You cannot get configurations in this method
     * because the configuration component does not be injected to this yet.
     * @param direction The new-created instance of direction, will be prepared in this method. (NotNull)
     */
    protected abstract void prepareAssistDirection(FwAssistDirection direction);

    // ===================================================================================
    //                                                                                Core
    //                                                                                ====
    public FwCoreDirection assistCoreDirection() {
        if (coreDirection != null) {
            return coreDirection;
        }
        synchronized (this) {
            if (coreDirection != null) {
                return coreDirection;
            }
            final FwCoreDirection direction = createCoreDirection();
            prepareCoreDirection(direction);
            coreDirection = direction;
        }
        return coreDirection;
    }

    protected FwCoreDirection createCoreDirection() {
        return new FwCoreDirection();
    }

    protected abstract void prepareCoreDirection(FwCoreDirection direction);

    // ===================================================================================
    //                                                                                 DB
    //                                                                                ====
    public FwDbDirection assistDbDirection() {
        if (dbDirection != null) {
            return dbDirection;
        }
        synchronized (this) {
            if (dbDirection != null) {
                return dbDirection;
            }
            final FwDbDirection direction = createDbDirection();
            prepareDbDirection(direction);
            dbDirection = direction;
        }
        return dbDirection;
    }

    protected FwDbDirection createDbDirection() {
        return new FwDbDirection();
    }

    protected abstract void prepareDbDirection(FwDbDirection direction);

    // ===================================================================================
    //                                                                                Web
    //                                                                               =====
    public FwWebDirection assistWebDirection() {
        if (webDirection != null) {
            return webDirection;
        }
        synchronized (this) {
            if (webDirection != null) {
                return webDirection;
            }
            final FwWebDirection direction = createWebDirection();
            prepareWebDirection(direction);
            webDirection = direction;
        }
        return webDirection;
    }

    protected FwWebDirection createWebDirection() {
        return new FwWebDirection();
    }

    protected abstract void prepareWebDirection(FwWebDirection direction);
}
