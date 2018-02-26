/*
 * Copyright 2015-2018 the original author or authors.
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
package org.lastaflute.web.servlet.filter.mdc;

import java.util.function.Function;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author jflute
 * @since 0.6.0 (2015/06/03 Wednesday)
 */
public class MDCSetupResource {

    protected final RequestManager requestManager;
    protected final Function<String, OptionalThing<String>> alreadyProvider;

    public MDCSetupResource(RequestManager requestManager, Function<String, OptionalThing<String>> alreadyProvider) {
        this.requestManager = requestManager;
        this.alreadyProvider = alreadyProvider;
    }

    public RequestManager getRequestManager() {
        return requestManager;
    }

    public OptionalThing<String> getAlreadyRegistered(String key) {
        return alreadyProvider.apply(key);
    }
}
