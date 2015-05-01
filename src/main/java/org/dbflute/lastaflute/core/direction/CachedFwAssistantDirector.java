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
package org.dbflute.lastaflute.core.direction;

import org.dbflute.lastaflute.db.direction.OptionalDbDirection;
import org.dbflute.lastaflute.web.direction.OptionalWebDirection;

/**
 * @author jflute
 */
public abstract class CachedFwAssistantDirector implements FwAssistantDirector {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected OptionalAssistDirection optionalAssistDirection;
    protected OptionalCoreDirection optionalCoreDirection;
    protected OptionalDbDirection optionalDbDirection;
    protected OptionalWebDirection optionalWebDirection;

    // ===================================================================================
    //                                                                              Assist
    //                                                                              ======
    public OptionalAssistDirection assistOptionalAssistDirection() {
        if (optionalAssistDirection != null) {
            return optionalAssistDirection;
        }
        synchronized (this) {
            if (optionalAssistDirection != null) {
                return optionalAssistDirection;
            }
            optionalAssistDirection = prepareOptionalAssistDirection();
        }
        if (optionalAssistDirection == null) {
            String msg = "Not found the optional core direction.";
            throw new IllegalStateException(msg);
        }
        return optionalAssistDirection;
    }

    /**
     * Prepare the optional direction to assist. <br>
     * You cannot get configurations in this method
     * because the configuration component does not be injected to this yet.
     * @return The new-created instance of direction. (NotNull)
     */
    protected abstract OptionalAssistDirection prepareOptionalAssistDirection();

    // ===================================================================================
    //                                                                                Core
    //                                                                                ====
    public OptionalCoreDirection assistOptionalCoreDirection() {
        if (optionalCoreDirection != null) {
            return optionalCoreDirection;
        }
        synchronized (this) {
            if (optionalCoreDirection != null) {
                return optionalCoreDirection;
            }
            optionalCoreDirection = prepareOptionalCoreDirection();
        }
        if (optionalCoreDirection == null) {
            String msg = "Not found the optional core direction.";
            throw new IllegalStateException(msg);
        }
        return optionalCoreDirection;
    }

    protected abstract OptionalCoreDirection prepareOptionalCoreDirection();

    // ===================================================================================
    //                                                                                 DB
    //                                                                                ====
    public OptionalDbDirection assistOptionalDbDirection() {
        if (optionalDbDirection != null) {
            return optionalDbDirection;
        }
        synchronized (this) {
            if (optionalDbDirection != null) {
                return optionalDbDirection;
            }
            optionalDbDirection = prepareOptionalDbDirection();
        }
        if (optionalDbDirection == null) {
            String msg = "Not found the optional DB direction.";
            throw new IllegalStateException(msg);
        }
        return optionalDbDirection;
    }

    protected abstract OptionalDbDirection prepareOptionalDbDirection();

    // ===================================================================================
    //                                                                                Web
    //                                                                               =====
    public OptionalWebDirection assistOptionalWebDirection() {
        if (optionalWebDirection != null) {
            return optionalWebDirection;
        }
        synchronized (this) {
            if (optionalWebDirection != null) {
                return optionalWebDirection;
            }
            optionalWebDirection = prepareOptionalWebDirection();
        }
        if (optionalWebDirection == null) {
            String msg = "Not found the optional web direction.";
            throw new IllegalStateException(msg);
        }
        return optionalWebDirection;
    }

    protected abstract OptionalWebDirection prepareOptionalWebDirection();
}
