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
package org.lastaflute.web.ruts.config.routing;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.dbflute.jdbc.Classification;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.lastaflute.core.util.LaClassificationUtil;
import org.lastaflute.web.path.RoutingParamPath;
import org.lastaflute.web.path.restful.analyzer.RestfulComponentAnalyzer;
import org.lastaflute.web.ruts.config.PathParamArgs;
import org.lastaflute.web.ruts.config.PreparedUrlPattern;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.util.LaActionExecuteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 * @since 1.0.1 (2017/10/16 Monday at el dorado)
 */
public class ActionRoutingByPathParamDeterminer {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(ActionRoutingByPathParamDeterminer.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Class<?> actionType; // not null, non-enhanced type
    protected final String mappingMethodName; // not null, e.g. index (even if get$index())
    protected final OptionalThing<String> restfulHttpMethod; // not null, empty allowed
    protected final boolean indexMethod; // e.g. index(), get$index()
    protected final OptionalThing<PathParamArgs> pathParamArgs; // not null, empty allowed
    protected final PreparedUrlPattern preparedUrlPattern; // not null
    protected final Supplier<RequestManager> requestManagerProvider; // not null
    protected final Supplier<String> callerExpProvider; // for debug, not null

    protected final RestfulComponentAnalyzer restfulComponentAnalyzer; // not null

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionRoutingByPathParamDeterminer(Class<?> actionType, String mappingMethodName, OptionalThing<String> restfulHttpMethod,
            boolean indexMethod, OptionalThing<PathParamArgs> pathParamArgs, PreparedUrlPattern preparedUrlPattern,
            Supplier<RequestManager> requestManagerProvider, Supplier<String> callerExpProvider) {
        this.actionType = actionType;
        this.mappingMethodName = mappingMethodName;
        this.restfulHttpMethod = restfulHttpMethod;
        this.indexMethod = indexMethod;
        this.pathParamArgs = pathParamArgs;
        this.preparedUrlPattern = preparedUrlPattern;
        this.requestManagerProvider = requestManagerProvider;
        this.callerExpProvider = callerExpProvider;

        this.restfulComponentAnalyzer = newRestfulComponentAnalyzer();
    }

    protected RestfulComponentAnalyzer newRestfulComponentAnalyzer() {
        return new RestfulComponentAnalyzer();
    }

    // ===================================================================================
    //                                                                           Determine
    //                                                                           =========
    public boolean determine(RoutingParamPath paramPath) {
        if (starndardDetermine(paramPath.getRequestParamPath())) {
            return true;
        }
        if (retryAsRestishEvent(paramPath)) {
            return true;
        }
        return false;
    }

    // ===================================================================================
    //                                                              Standard Determination
    //                                                              ======================
    protected boolean starndardDetermine(String paramPath) {
        if (restfulHttpMethod.filter(httpMethod -> {
            final RequestManager requestManager = requestManagerProvider.get();
            return !matchesWithRequestedHttpMethod(requestManager, httpMethod);
        }).isPresent()) {
            return false;
        }
        if (!isParameterEmpty(paramPath)) { // e.g. sea, sea/dockside
            final HandledOptionalPath handled = handleOptionalPathMapping(paramPath);
            if (HandledOptionalPath.CERTAINLY_MATCHES.equals(handled)) { // optional and e.g. count OK and type maybe OK
                return true;
            } else if (HandledOptionalPath.CERTAINLY_UNMATCHES.equals(handled)) { // optional and e.g. count OK but type difference
                return false;
            } else { // optional but maybe unmatched or non-optional first
                return preparedUrlPattern.matcher(paramPath).find();
            }
        } else { // no way
            // should not be called if param is empty, old code is like this:
            //return "index".equals(urlPattern);
            String msg = "The paramPath should not be null or empty: [" + paramPath + "], " + callerExpProvider.get();
            throw new IllegalStateException(msg);
        }
    }

    protected boolean matchesWithRequestedHttpMethod(RequestManager requestManager, String httpMethod) {
        return requestManager.isHttpMethod(httpMethod);
    }

    // -----------------------------------------------------
    //                                     Optional Handling
    //                                     -----------------
    protected HandledOptionalPath handleOptionalPathMapping(String paramPath) {
        // #for_now no considering type difference: index(String, OptionalThing<Integer>) but 'sea/land'
        // so cannot mapping to dockside(String hangar) if the index() exists now
        if (hasOptionalPathParameter()) { // e.g. any parameters are optional type
            if (indexMethod) { // e.g. index(String first, OptionalThing<String> second) with 'sea' or 'sea/land'
                final int paramCount = Srl.count(Srl.trim(paramPath, "/"), "/") + 1; // e.g. sea/land => 2
                return determineOptionalPath(paramPath, paramCount, /*namedMethod*/false);
            } else { // e.g. sea(String first, OptionalThing<String> second) with 'sea/dockside' or 'sea/dockside/hangar'
                // required parameter may not be specified but checked later as 404
                final String firstElement = Srl.substringFirstFront(paramPath, "/"); // e.g. sea (from sea/dockside)
                if (firstElement.equals(mappingMethodName)) { // e.g. sea(first) with sea/dockside
                    final int paramCount = Srl.count(Srl.trim(paramPath, "/"), "/"); // e.g. sea/dockside => 1
                    return determineOptionalPath(paramPath, paramCount, /*namedMethod*/true);
                }
                return HandledOptionalPath.MAYBE_UNMATCHES; // might be certainly unmatches, for compatible
            }
        } else {
            return HandledOptionalPath.NON_OPTIONAL;
        }
    }

    protected boolean hasOptionalPathParameter() {
        // already checked here that optional parameters are defined at rear arguments so no check about it here
        // simply determines that one-or-more optional parameters exist or not
        return pathParamArgs.map(args -> {
            return args.getPathParamTypeList().stream().anyMatch(tp -> isOptionalParameterType(tp));
        }).orElse(false);
    }

    protected HandledOptionalPath determineOptionalPath(String paramPath, int paramCount, boolean namedMethod) {
        if (matchesOptionalPathParameterCount(paramCount)) {
            if (unmatchesOptionalPathParameterType(paramPath, namedMethod)) {
                return HandledOptionalPath.CERTAINLY_UNMATCHES;
            } else {
                return HandledOptionalPath.CERTAINLY_MATCHES;
            }
        } else { // parameter count difference
            return HandledOptionalPath.MAYBE_UNMATCHES; // might be certainly unmatches, for compatible
        }
    }

    protected static enum HandledOptionalPath {
        CERTAINLY_MATCHES, CERTAINLY_UNMATCHES, MAYBE_UNMATCHES, NON_OPTIONAL
    }

    protected boolean matchesOptionalPathParameterCount(int paramCount) {
        return paramCount >= countRequiredParameter() && paramCount <= countAllParameter();
    }

    protected boolean unmatchesOptionalPathParameterType(String paramPath, boolean namedMethod) {
        if (!pathParamArgs.isPresent()) { // no way, already determined, just in case
            return false;
        }
        final PathParamArgs paramArgs = pathParamArgs.get();
        List<String> pathElementList = Srl.splitList(Srl.trim(paramPath, "/"), "/"); // trim just in case
        if (namedMethod) {
            pathElementList = pathElementList.subList(1, pathElementList.size()); // e.g. [sea, dockside] => [dockside]
        }
        if (pathElementList.isEmpty()) { // named method and first element only e.g. paramPath is sea
            return false;
        }
        // if it uses optional parameter, no urlPattern so the pathElementList has only variables
        final List<Class<?>> pathParamTypeList = paramArgs.getPathParamTypeList();
        final Map<Integer, Class<?>> optionalGenericTypeMap = paramArgs.getOptionalGenericTypeMap();
        int index = 0;
        for (Class<?> pathParamType : pathParamTypeList) {
            if (pathElementList.size() <= index) { // no way, just in case
                continue;
            }
            final String paramValue = pathElementList.get(index);
            if (paramValue == null) { // no way, just in case
                continue;
            }
            if (isOptionalParameterType(pathParamType)) { // e.g. OptionalThing<Integer>
                final Class<?> optionalGenericType = optionalGenericTypeMap.get(index);
                if (optionalGenericType == null) { // no way, just in case
                    continue;
                }
                if (certainlyUnmatchesOptionalPathParamValue(optionalGenericType, paramValue)) {
                    return true;
                }
            } else {
                if (certainlyUnmatchesOptionalPathParamValue(pathParamType, paramValue)) {
                    return true;
                }
            }
            ++index;
        }
        return false;
    }

    protected boolean certainlyUnmatchesOptionalPathParamValue(Class<?> pathParamType, String paramValue) {
        if (Integer.class.isAssignableFrom(pathParamType) || Long.class.isAssignableFrom(pathParamType)) {
            // quit Number.class because the type contains BigDecimal, which has complex condition
            final String comparedValue = paramValue.startsWith("-") ? Srl.substringFirstRear(paramValue, "-") : paramValue;
            if (!Srl.isNumberHarfAll(comparedValue)) { // number type variable but non-number value
                return true; // certainly unmatched
            }
        } else if (Classification.class.isAssignableFrom(pathParamType)) {
            if (!LaClassificationUtil.findByCode(pathParamType, paramValue).isPresent()) {
                return true; // certainly unmatched
            }
        }
        return false;
    }

    // -----------------------------------------------------
    //                                       Parameter Count
    //                                       ---------------
    protected int countAllParameter() {
        return pathParamArgs.map(args -> args.getPathParamTypeList().size()).orElse(0);
    }

    protected int countRequiredParameter() {
        return pathParamArgs.map(args -> {
            return args.getPathParamTypeList().stream().filter(tp -> {
                return !isOptionalParameterType(tp);
            }).count();
        }).orElse(0L).intValue();
    }

    // ===================================================================================
    //                                                                 RESTish Event Retry
    //                                                                 ===================
    protected boolean retryAsRestishEvent(RoutingParamPath paramPath) {
        if (isOutOfRestishEvent(paramPath)) {
            return false;
        }
        // here e.g. get$mysticHangar(), mystic/hangar/1
        final List<String> eventSuffixHyphenatedNameList = restfulComponentAnalyzer.extractEventSuffixHyphenatedNameList(actionType);
        if (eventSuffixHyphenatedNameList.isEmpty()) {
            return false; // hyphenation of event-suffix is explicit option
        }
        final String requestParamPath = paramPath.getRequestParamPath(); // e.g. mystic/hangar/1
        for (String hyphenatedName : eventSuffixHyphenatedNameList) {
            final String slashedEvent = Srl.replace(hyphenatedName, "-", "/"); // e.g. mystic/hangar
            if (requestParamPath.startsWith(slashedEvent)) { // e.g. mystic/hangar/1
                final String newParamPath = buildRestishEventParamPath(requestParamPath, hyphenatedName, slashedEvent); // e.g. mysticHangar/1
                logger.debug("...Retrying routing as RESTish event: method={}@{}, request={}, new={}", restfulHttpMethod, mappingMethodName,
                        requestParamPath, newParamPath);
                final boolean retryDetermination = starndardDetermine(newParamPath); // *retry here
                if (retryDetermination) {
                    paramPath.acceptMappingParamPath(newParamPath); // e.g. mysticHangar/1
                    return true; // found!
                }
            }
        }
        return false;
    }

    protected boolean isOutOfRestishEvent(RoutingParamPath paramPath) {
        // fast determination as possible for non-related mapping performance
        return indexMethod // index()|get$index()
                || !restfulHttpMethod.isPresent() // no HTTP method
                || !restfulComponentAnalyzer.hasRestfulAnnotation(actionType) // no RESTful Action
                || Srl.isLowerCaseAll(mappingMethodName) // e.g. get$index() or get$sea()
                || !paramPath.getRequestParamPath().contains("/") // e.g. sea (simple paramPath)
        ;
    }

    protected String buildRestishEventParamPath(String requestParamPath, String hyphenatedName, String slashedEvent) {
        final String camelizedEventSuffix = Srl.initUncap(Srl.camelize(hyphenatedName, "-")); // e.g. mysticHangar
        return camelizedEventSuffix + Srl.substringFirstRear(requestParamPath, slashedEvent); // e.g. mysticHangar/1
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected boolean isParameterEmpty(String str) {
        return str == null || str.isEmpty();
    }

    protected boolean isOptionalParameterType(Class<?> paramType) {
        return LaActionExecuteUtil.isOptionalParameterType(paramType);
    }
}
