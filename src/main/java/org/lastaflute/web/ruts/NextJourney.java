/*
 * Copyright 2015-2016 the original author or authors.
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
    protected final String routingPath; // not null if HTML
    protected final boolean redirectTo;
    protected final boolean asIs; // when redirect
    protected final OptionalThing<Object> viewObject; // not null, empty allowed, for e.g. mixer2
    protected final OriginalJourneyProvider originalJourneyProvider; // not null if original e.g. JSON, Stream

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public NextJourney(String routingPath, boolean redirectTo, boolean asIs, OptionalThing<Object> viewObject) { // for HTML
        this.routingPath = routingPath;
        this.redirectTo = redirectTo;
        this.asIs = asIs;
        this.viewObject = viewObject;
        this.originalJourneyProvider = null;
    }

    public NextJourney(OriginalJourneyProvider originalJourneyProvider) { // for e.g. JSON, Stream
        this.routingPath = null; // means no HTML
        this.redirectTo = false;
        this.asIs = false;
        this.viewObject = OptionalThing.empty();
        this.originalJourneyProvider = originalJourneyProvider;
    }

    @FunctionalInterface
    public static interface OriginalJourneyProvider {

        void bonVoyage();
    }

    protected NextJourney() {
        this.routingPath = null; // means no HTML
        this.redirectTo = false;
        this.asIs = false;
        this.viewObject = OptionalThing.empty();
        this.originalJourneyProvider = null;
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
        if (isHtmlJourney()) {
            sb.append("path=").append(routingPath);
            sb.append(redirectTo ? ", redirect" : ", forward");
            sb.append(asIs ? ", asIs" : "");
        } else if (isOriginalJourney()) {
            sb.append(originalJourneyProvider);
        } else {
            sb.append("undefined");
        }
        sb.append("}");
        return sb.toString();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    // -----------------------------------------------------
    //                                          HTML Journey
    //                                          ------------
    public boolean isHtmlJourney() {
        return routingPath != null;
    }

    public String getRoutingPath() { // not null or exception
        if (!isHtmlJourney()) {
            String msg = "The action transition is not HTML so cannot call getRoutingPath(): " + routingPath;
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

    public OptionalThing<Object> getViewObject() { // not null, empty allowed
        return viewObject;
    }

    // -----------------------------------------------------
    //                                      Original Journey
    //                                      ----------------
    public boolean isOriginalJourney() {
        return originalJourneyProvider != null;
    }

    public OriginalJourneyProvider getOriginalJourneyProvider() { // not null or exception
        if (!isOriginalJourney()) {
            String msg = "The action transition is not original so cannot call getOriginalJourneyProvider(): " + routingPath;
            throw new IllegalStateException(msg);
        }
        return originalJourneyProvider;
    }

    // -----------------------------------------------------
    //                                     Undefined Journey
    //                                     -----------------
    public boolean isUndefined() {
        return !isHtmlJourney() && !isOriginalJourney();
    }
}
