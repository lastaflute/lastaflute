/*
 * Copyright 2015-2021 the original author or authors.
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
    // -----------------------------------------------------
    //                                      Journey Provider
    //                                      ----------------
    protected final PlannedJourneyProvider journeyProvider; // not null if defined (e.g. HTML, JSON)

    // -----------------------------------------------------
    //                                          View Routing
    //                                          ------------
    protected final String routingPath; // not null if HTML
    protected final boolean redirectTo;
    protected final boolean asIs; // when redirect
    protected final OptionalThing<Object> viewObject; // not null, empty allowed, for e.g. mixer2

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public NextJourney(PlannedJourneyProvider journeyProvider // fixed resource
            , String routingPath, boolean redirectTo, boolean asIs // routing resources
            , OptionalThing<Object> viewObject // for e.g. mixer2 
    ) { // for HTML (except empty HTML)
        this.journeyProvider = journeyProvider;
        this.routingPath = routingPath;
        this.redirectTo = redirectTo;
        this.asIs = asIs;
        this.viewObject = viewObject;
    }

    public NextJourney(PlannedJourneyProvider journeyProvider) { // for e.g. JSON, Stream, also empty HTML
        this.journeyProvider = journeyProvider;
        this.routingPath = null; // means no HTML
        this.redirectTo = false;
        this.asIs = false;
        this.viewObject = OptionalThing.empty();
    }

    @FunctionalInterface
    public static interface PlannedJourneyProvider {

        void bonVoyage();
    }

    protected NextJourney() { // as undefined
        this.routingPath = null; // means no HTML
        this.redirectTo = false;
        this.asIs = false;
        this.viewObject = OptionalThing.empty();
        this.journeyProvider = null;
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
        if (isUndefined()) {
            sb.append("undefined");
        } else {
            sb.append(journeyProvider != null ? journeyProvider : null);
            if (hasViewRouting()) {
                sb.append(", routing=").append(routingPath);
                sb.append(redirectTo ? ", redirect" : ", forward");
                sb.append(asIs ? ", asIs" : "");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    // -----------------------------------------------------
    //                                      Journey Provider
    //                                      ----------------
    public boolean hasJourneyProvider() {
        return journeyProvider != null;
    }

    public PlannedJourneyProvider getJourneyProvider() { // not null or exception
        if (!hasJourneyProvider()) {
            String msg = "The action transition is not original so cannot call getOriginalJourneyProvider(): " + routingPath;
            throw new IllegalStateException(msg);
        }
        return journeyProvider;
    }

    // -----------------------------------------------------
    //                                          View Routing
    //                                          ------------
    public boolean hasViewRouting() {
        return routingPath != null;
    }

    public String getRoutingPath() { // not null or exception
        if (!hasViewRouting()) {
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
    //                                     Undefined Journey
    //                                     -----------------
    public boolean isUndefined() {
        return !hasJourneyProvider() && !hasViewRouting();
    }
}
