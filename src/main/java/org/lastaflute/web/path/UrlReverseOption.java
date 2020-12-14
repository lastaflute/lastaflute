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

import java.util.function.Function;

import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 * @since 1.1.0 (2018/09/17 Monday)
 */
public class UrlReverseOption {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /**
     * The filter function for action name (that is without 'Action' suffix). <br>
     *  e.g. if ProductListSpAction, convert productListSp to spProductList (/sp/product/list/) <br>
     * (NullAllowed: if null, no filter, CannotNullReturn: function cannot return null)
     */
    protected Function<String, String> actionNameFilter;

    // ===================================================================================
    //                                                                              Facade
    //                                                                              ======
    public UrlReverseOption filterActionName(Function<String, String> filter) {
        if (filter == null) {
            throw new IllegalArgumentException("The argument 'filter' should not be null.");
        }
        actionNameFilter = filter;
        return this;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public OptionalThing<Function<String, String>> getActionNameFilter() {
        return OptionalThing.ofNullable(actionNameFilter, () -> {
            throw new IllegalStateException("Not found the actionNameFilter.");
        });
    }
}
