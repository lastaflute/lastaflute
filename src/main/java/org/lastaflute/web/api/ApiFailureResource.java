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
package org.lastaflute.web.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.web.callback.ActionRuntime;
import org.lastaflute.web.ruts.message.ActionMessages;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author jflute
 */
public class ApiFailureResource {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ActionRuntime runtime;
    protected final OptionalThing<ActionMessages> messages;
    protected final RequestManager requestManager;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ApiFailureResource(ActionRuntime runtime, OptionalThing<ActionMessages> messages, RequestManager requestManager) {
        this.runtime = runtime;
        this.messages = messages;
        this.requestManager = requestManager;
    }

    // ===================================================================================
    //                                                                            Messages
    //                                                                            ========
    /**
     * Get the list of message text for the errors by user locale.
     * @return The list of message, resolved by resource. (NotNull, EmptyAllowed)
     */
    public List<String> getMessageList() {
        return messages.map(er -> {
            return requestManager.getMessageManager().toMessageList(requestManager.getUserLocale(), er);
        }).orElse(Collections.emptyList());
    }

    /**
     * Get the map (property : list of message text) of message for the errors  by user locale.
     * @return The map of message, resolved by resource. (NotNull, EmptyAllowed)
     */
    public Map<String, List<String>> getPropertyMessageMap() {
        return messages.map(er -> {
            return requestManager.getMessageManager().toPropertyMessageMap(requestManager.getUserLocale(), er);
        }).orElse(Collections.emptyMap());
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public ActionRuntime getRuntime() {
        return runtime;
    }

    public OptionalThing<ActionMessages> getMessages() {
        return messages;
    }

    public RequestManager getRequestManager() {
        return requestManager;
    }
}
