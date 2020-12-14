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
package org.lastaflute.web.path;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;

/**
 * @author jflute
 * @since 0.9.5 (2017/04/15 Saturday at rainbow bird rendezvous)
 */
public class ApplicationExceptionOption {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected OptionalThing<AppExInfoSuppressor> appExInfoSuppressor = OptionalThing.empty();

    // ===================================================================================
    //                                                                              Facade
    //                                                                              ======
    // -----------------------------------------------------
    //                                       Info Suppressor
    //                                       ---------------
    public ApplicationExceptionOption suppressInfo(AppExInfoSuppressor causeLambda) {
        if (causeLambda == null) {
            throw new IllegalArgumentException("The argument 'causeLambda' should not be null.");
        }
        this.appExInfoSuppressor = OptionalThing.of(causeLambda);
        return this;
    }

    @FunctionalInterface
    public static interface AppExInfoSuppressor {

        boolean isSuppress(RuntimeException cause);
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final String title = DfTypeUtil.toClassTitle(this);
        return title + ":{" + appExInfoSuppressor + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    // -----------------------------------------------------
    //                                       Info Suppressor
    //                                       ---------------
    public OptionalThing<AppExInfoSuppressor> getAppExInfoSuppressor() { // not null
        return appExInfoSuppressor;
    }
}
