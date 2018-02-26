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
package org.lastaflute.web.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author jflute
 */
public class ApiFailureResource {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ActionRuntime runtime; // runtime for current action
    protected final OptionalThing<UserMessages> messages; // basically embedded in exception
    protected final RequestManager requestManager;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ApiFailureResource(ActionRuntime runtime, OptionalThing<UserMessages> messages, RequestManager requestManager) {
        this.runtime = runtime;
        this.messages = messages;
        this.requestManager = requestManager;
    }

    // ===================================================================================
    //                                                                            Messages
    //                                                                            ========
    /**
     * Get the list of message text for the errors by user locale. <br>
     * Basically these are embedded in exception.
     * @return The list of message, resolved by resource. (NotNull, EmptyAllowed)
     */
    public List<String> getMessageList() {
        return messages.map(er -> {
            return requestManager.getMessageManager().toMessageList(requestManager.getUserLocale(), er);
        }).orElse(Collections.emptyList());
    }

    /**
     * Get the map (property : list of message text) of message for the errors by user locale. <br>
     * Basically these are embedded in exception.
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
    public ActionRuntime getRuntime() { // exists in action, might be no action
        return runtime;
    }

    public OptionalThing<UserMessages> getMessages() {
        return messages;
    }

    public RequestManager getRequestManager() {
        return requestManager;
    }
}
