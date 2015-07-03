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
package org.lastaflute.web.ruts;

import java.io.Serializable;

import org.lastaflute.web.servlet.request.Forwardable;
import org.lastaflute.web.servlet.request.Redirectable;

/**
 * @author modified by jflute (originated in Struts)
 */
public class NextJourney implements Redirectable, Forwardable, Serializable {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = 1L;
    protected static final NextJourney EMPTY_INSTANCE = new NextJourney();

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String routingPath;
    protected final boolean redirectTo;
    protected final boolean asIs; // when redirect
    protected final boolean empty;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public NextJourney(String routingPath, boolean redirectTo, boolean asIs) {
        this.routingPath = routingPath;
        this.redirectTo = redirectTo;
        this.asIs = asIs;
        this.empty = false;
    }

    protected NextJourney() {
        this.routingPath = "empty";
        this.redirectTo = false;
        this.asIs = false;
        this.empty = true;
    }

    public static NextJourney empty() {
        return EMPTY_INSTANCE;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("transition:{");
        sb.append("path=").append(routingPath);
        sb.append(redirectTo ? ", redirect" : ", forward");
        sb.append(asIs ? ", asIs" : "");
        sb.append("}");
        return sb.toString();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getRoutingPath() {
        if (isEmpty()) {
            String msg = "The action transition is empty so you should not call getRoutingPath().";
            throw new IllegalStateException(msg);
        }
        return routingPath;
    }

    public boolean isRedirectTo() {
        return redirectTo;
    }

    public boolean isAsIs() {
        return asIs;
    }

    public boolean isEmpty() {
        return empty;
    }

    public boolean isPresent() {
        return !isEmpty();
    }
}
