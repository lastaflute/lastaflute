/*
 * Copyright 2015-2022 the original author or authors.
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

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.json.engine.RealJsonEngine;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.ruts.process.populate.FormSimpleTextParameterFilter;
import org.lastaflute.web.ruts.process.populate.FormYourCollectionResource;

// package is a little strange (path!? near adjustment provider...)
// but no change for compatible
/**
 * @author jflute
 */
public class FormMappingOption {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected boolean keepEmptyStringParameter;
    protected OptionalThing<FormSimpleTextParameterFilter> simpleTextParameterFilter = OptionalThing.empty();
    protected boolean undefinedParameterError;
    protected Set<String> indefinableParameterSet; // null allowed
    protected List<FormYourCollectionResource> yourCollectionResourceList; // null allowed
    protected OptionalThing<DateTimeFormatter> zonedDateTimeFormatter = OptionalThing.empty();
    protected OptionalThing<Function<Map<String, Object>, Map<String, Object>>> requestParameterMapFilter = OptionalThing.empty();
    protected OptionalThing<Function<ActionRuntime, RealJsonEngine>> requestJsonEngineProvider = OptionalThing.empty();

    // ===================================================================================
    //                                                                              Facade
    //                                                                              ======
    // -----------------------------------------------------
    //                                          Empty String
    //                                          ------------
    public FormMappingOption asKeepEmptyStringParameter() {
        keepEmptyStringParameter = true;
        return this;
    }

    // -----------------------------------------------------
    //                                           Simple Text
    //                                           -----------
    public FormMappingOption filterSimpleTextParameter(FormSimpleTextParameterFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("The argument 'filter' should not be null.");
        }
        this.simpleTextParameterFilter = OptionalThing.of(filter);
        return this;
    }

    // -----------------------------------------------------
    //                                   Undefined Parameter
    //                                   -------------------
    public FormMappingOption asUndefinedParameterError() {
        undefinedParameterError = true;
        return this;
    }

    public FormMappingOption indefinableParameters(String... indefinableParameters) {
        indefinableParameterSet = createIndefinableParameterSet(indefinableParameters);
        return this;
    }

    protected Set<String> createIndefinableParameterSet(String... indefinableParameters) {
        final Set<String> specifiedSet = new HashSet<String>(Arrays.asList(indefinableParameters));
        setupDefaultIndefinableParameter(specifiedSet);
        return Collections.unmodifiableSet(specifiedSet);
    }

    protected void setupDefaultIndefinableParameter(Set<String> specifiedSet) {
        specifiedSet.add(LastaWebKey.TRANSACTION_TOKEN_KEY);
    }

    // -----------------------------------------------------
    //                                       Your Collection
    //                                       ---------------
    public FormMappingOption yourCollection(FormYourCollectionResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("The argument 'resource' should not be null.");
        }
        if (yourCollectionResourceList == null) {
            yourCollectionResourceList = new ArrayList<>();
        }
        yourCollectionResourceList.add(resource);
        return this;
    }

    // -----------------------------------------------------
    //                                         ZonedDateTime
    //                                         -------------
    public FormMappingOption formatZonedDateTime(DateTimeFormatter zonedDateTimeFormatter) {
        if (zonedDateTimeFormatter == null) {
            throw new IllegalArgumentException("The argument 'zonedDateTimeFormatter' should not be null.");
        }
        this.zonedDateTimeFormatter = OptionalThing.of(zonedDateTimeFormatter);
        return this;
    }

    // -----------------------------------------------------
    //                                         Parameter Map
    //                                         -------------
    public FormMappingOption filterRequestParameterMap(Function<Map<String, Object>, Map<String, Object>> requestParameterMapFilter) {
        if (requestParameterMapFilter == null) {
            throw new IllegalArgumentException("The argument 'requestParameterMapFilter' should not be null.");
        }
        this.requestParameterMapFilter = OptionalThing.of(requestParameterMapFilter);
        return this;
    }

    // -----------------------------------------------------
    //                                           JSON Engine
    //                                           -----------
    public FormMappingOption parseJsonBy(Function<ActionRuntime, RealJsonEngine> requestJsonEngineProvider) {
        if (requestJsonEngineProvider == null) {
            throw new IllegalArgumentException("The argument 'requestJsonEngineProvider' should not be null.");
        }
        this.requestJsonEngineProvider = OptionalThing.of(requestJsonEngineProvider);
        return this;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final String title = DfTypeUtil.toClassTitle(this);
        return title + ":{" + keepEmptyStringParameter + ", " + simpleTextParameterFilter + ", " + undefinedParameterError + ", "
                + indefinableParameterSet + ", " + yourCollectionResourceList + ", " + zonedDateTimeFormatter + ", "
                + requestParameterMapFilter + ", " + requestJsonEngineProvider + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    // -----------------------------------------------------
    //                                          Empty String
    //                                          ------------
    public boolean isKeepEmptyStringParameter() {
        return keepEmptyStringParameter;
    }

    // -----------------------------------------------------
    //                                           Simple Text
    //                                           -----------
    public OptionalThing<FormSimpleTextParameterFilter> getSimpleTextParameterFilter() { // not null
        return simpleTextParameterFilter;
    }

    // -----------------------------------------------------
    //                                   Undefined Parameter
    //                                   -------------------
    public boolean isUndefinedParameterError() {
        return undefinedParameterError;
    }

    public Set<String> getIndefinableParameterSet() { // not null
        return indefinableParameterSet != null ? indefinableParameterSet : Collections.emptySet();
    }

    // -----------------------------------------------------
    //                                       Your Collection
    //                                       ---------------
    public List<FormYourCollectionResource> getYourCollections() { // not null
        return yourCollectionResourceList != null ? yourCollectionResourceList : Collections.emptyList();
    }

    // -----------------------------------------------------
    //                                         ZonedDateTime
    //                                         -------------
    public OptionalThing<DateTimeFormatter> getZonedDateTimeFormatter() {
        return zonedDateTimeFormatter;
    }

    // -----------------------------------------------------
    //                                         Parameter Map
    //                                         -------------
    public OptionalThing<Function<Map<String, Object>, Map<String, Object>>> getRequestParameterMapFilter() {
        return requestParameterMapFilter;
    }

    // -----------------------------------------------------
    //                                           JSON Engine
    //                                           -----------
    public OptionalThing<Function<ActionRuntime, RealJsonEngine>> getRequestJsonEngineProvider() {
        return requestJsonEngineProvider;
    }
}
