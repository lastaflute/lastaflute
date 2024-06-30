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
package org.lastaflute.core.message.resources;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class MessageResourcesHolder {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(MessageResourcesHolder.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The gateway of message resources. (NotNull: if accepted) */
    protected MessageResourcesGateway gateway;

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    /**
     * Initialize this component. <br>
     * This is basically called by DI setting file.
     */
    @PostConstruct
    public synchronized void initialize() {
        // empty for now
    }

    // ===================================================================================
    //                                                                              Accept
    //                                                                              ======
    /**
     * Accept the gateway for message resources. <br>
     * You should call this immediately after your application is initialized. <br>
     * And only one setting is allowed.
     * @param specified The instance of gateway. (NotNull)
     */
    public void acceptGateway(MessageResourcesGateway specified) {
        logger.info("...Accepting the gateway of message resources: " + specified);
        if (specified == null) {
            String msg = "The argument 'specified' should not be null.";
            throw new IllegalArgumentException(msg);
        }
        if (gateway != null) {
            String msg = "The gateway already exists: existing=" + gateway + " specified=" + specified;
            throw new IllegalStateException(msg);
        }
        gateway = specified;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return "holder:{" + gateway + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    /**
     * Get the gateway for message resources.
     * @return The instance of gateway. (NotNull: if accepted)
     */
    public MessageResourcesGateway getGateway() {
        return gateway;
    }
}
