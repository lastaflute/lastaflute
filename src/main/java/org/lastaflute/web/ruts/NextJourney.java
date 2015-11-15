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

import org.dbflute.optional.OptionalThing;
import org.lastaflute.web.servlet.request.Forwardable;
import org.lastaflute.web.servlet.request.Redirectable;

/**
 * @author jflute
 */
public class NextJourney implements Redirectable, Forwardable, Serializable {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = 1L;
    protected static final NextJourney UNDEFINED_INSTANCE = new NextJourney();

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String routingPath;
    protected final boolean redirectTo;
    protected final boolean asIs; // when redirect
    protected final boolean undefined;
    protected final OptionalThing<Object> viewObject; // not null, empty allowed, for e.g. mixer2

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public NextJourney(String routingPath, boolean redirectTo, boolean asIs, OptionalThing<Object> viewObject) {
        this.routingPath = routingPath;
        this.redirectTo = redirectTo;
        this.asIs = asIs;
        this.undefined = false;
        this.viewObject = viewObject;
    }

    protected NextJourney() {
        this.routingPath = "undefined";
        this.redirectTo = false;
        this.asIs = false;
        this.undefined = true;
        this.viewObject = OptionalThing.empty();
    }

    public static NextJourney undefined() {
        return UNDEFINED_INSTANCE;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("journey:{");
        sb.append("path=").append(routingPath);
        sb.append(redirectTo ? ", redirect" : ", forward");
        sb.append(asIs ? ", asIs" : "");
        sb.append(undefined ? ", undefined" : "");
        sb.append("}");
        return sb.toString();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getRoutingPath() {
        assertRoutingPathCalled();
        return routingPath;
    }

    protected void assertRoutingPathCalled() {
        if (isUndefined()) {
            String msg = "The action transition is undefined so cannot call getRoutingPath(): " + routingPath;
            throw new IllegalStateException(msg);
        }
    }

    public boolean isRedirectTo() {
        return redirectTo;
    }

    public boolean isAsIs() {
        return asIs;
    }

    public boolean isDefined() {
        return !isUndefined();
    }

    public boolean isUndefined() {
        return undefined;
    }

    public OptionalThing<Object> getViewObject() {
        return viewObject;
    }
}
