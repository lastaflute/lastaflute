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
package org.lastaflute.web.path.restful.router;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.lastaflute.web.path.UrlMappingOption;
import org.lastaflute.web.path.UrlMappingResource;
import org.lastaflute.web.path.UrlReverseOption;
import org.lastaflute.web.path.UrlReverseResource;
import org.lastaflute.web.path.restful.analyzer.RestfulComponentAnalyzer;

/**
 * @author jflute
 * @since 1.2.1 (2021/05/20)
 */
public abstract class AbstractBasedRestfulRouter implements RestfulRouter {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RestfulComponentAnalyzer restfulComponentAnalyzer = newRestfulComponentAnalyzer();

    protected RestfulComponentAnalyzer newRestfulComponentAnalyzer() {
        return new RestfulComponentAnalyzer();
    }

    // ===================================================================================
    //                                                                         URL Mapping
    //                                                                         ===========
    public OptionalThing<UrlMappingOption> toRestfulMappingPath(UrlMappingResource resource) {
        final String workingMappingPath = resource.getWorkingMappingPath(); // may be filtered before
        final List<String> elementList = splitPath(workingMappingPath); // only for determination

        if (!determineRestfulPath(resource, elementList)) { // e.g. /, /1/products/, /products/purchases/
            return OptionalThing.empty(); // no filter
        }
        // comment out because of virtual list handling and RESTful mapping message
        // so always call conversion process here even if e.g. /products/
        //if (elementList.size() <= 2) { // e.g. /products/, /products/1/
        //    return OptionalThing.empty(); // no filter
        //}
        return OptionalThing.of(createUrlMappingOption(resource, elementList));
    }

    protected void handleRestlessPath(UrlMappingResource resource, List<String> elementList) { // you can override
        // for example, you can log as debug here if RESTful only application
    }

    protected UrlMappingOption createUrlMappingOption(UrlMappingResource resource, List<String> elementList) {
        final UrlMappingOption option = new UrlMappingOption();
        option.filterRequestPath(requestPath -> { // is makingMappingPath
            return convertToMappingPath(requestPath);
        });
        option.tellRestfulMapping();
        return option;
    }

    // -----------------------------------------------------
    //                                 RESTful Determination
    //                                 ---------------------
    protected boolean determineRestfulPath(UrlMappingResource resource, List<String> elementList) {
        if (isRootAction(resource, elementList)) { // e.g. "/" (RootAction@index())
            return false;
        }
        if (isSwaggerPath(resource, elementList)) { // e.g. /swagger/ (not business request)
            return false;
        }
        if (isExceptPath(resource, elementList)) { // for application requirement
            return false;
        }
        return doDetermineRestfulPath(resource, elementList);
    }

    protected boolean isRootAction(UrlMappingResource resource, List<String> elementList) {
        return elementList.isEmpty();
    }

    protected boolean isSwaggerPath(UrlMappingResource resource, List<String> elementList) {
        return !elementList.isEmpty() && elementList.get(0).equals("swagger");
    }

    protected boolean isExceptPath(UrlMappingResource resource, List<String> elementList) { // you can override
        return false;
    }

    protected abstract boolean doDetermineRestfulPath(UrlMappingResource resource, List<String> elementList);

    // -----------------------------------------------------
    //                                          Convert Path
    //                                          ------------
    protected String convertToMappingPath(String requestPath) {
        return doConvertToMappingPath(requestPath);
    }

    protected abstract String doConvertToMappingPath(String requestPath);

    // ===================================================================================
    //                                                                         URL Reverse
    //                                                                         ===========
    public OptionalThing<UrlReverseOption> toRestfulReversePath(UrlReverseResource resource) {
        if (!determineRestfulAction(resource)) {
            return OptionalThing.empty();
        }
        final RestfulPathConvertingParam convertingParam = createRestfulPathConvertingParam(resource);
        final UrlReverseOption option = createUrlReverseOption(resource, convertingParam);
        return OptionalThing.of(option);
    }

    protected UrlReverseOption createUrlReverseOption(UrlReverseResource resource, RestfulPathConvertingParam convertingParam) {
        final UrlReverseOption option = new UrlReverseOption();
        final Class<?> actionType = resource.getActionType();
        option.filterActionUrl(actionUrl -> {
            return convertToRestfulPath(actionType, actionUrl, convertingParam);
        });
        return option;
    }

    // -----------------------------------------------------
    //                                  Converting Parameter
    //                                  --------------------
    protected RestfulPathConvertingParam createRestfulPathConvertingParam(UrlReverseResource resource) {
        final int businessElementCount = countActionBusinessElement(resource);
        final List<String> hyphenatedNameList = extractHyphenatedNameList(resource);
        final List<String> eventSuffixHyphenatedNameList = extractEventSuffixHyphenatedNameList(resource);
        return new RestfulPathConvertingParam(businessElementCount, hyphenatedNameList, eventSuffixHyphenatedNameList);
    }

    protected boolean determineRestfulAction(UrlReverseResource resource) {
        return restfulComponentAnalyzer.hasRestfulAnnotation(resource.getActionType());
    }

    protected int countActionBusinessElement(UrlReverseResource resource) {
        return restfulComponentAnalyzer.countActionBusinessElement(resource.getActionType());
    }

    protected List<String> extractHyphenatedNameList(UrlReverseResource resource) { // not null, empty allowed
        return restfulComponentAnalyzer.extractHyphenatedNameList(resource.getActionType());
    }

    protected List<String> extractEventSuffixHyphenatedNameList(UrlReverseResource resource) { // not null, empty allowed
        return restfulComponentAnalyzer.extractEventSuffixHyphenatedNameList(resource.getActionType());
    }

    // -----------------------------------------------------
    //                                            Convert to
    //                                            ----------
    protected String convertToRestfulPath(Class<?> actionType, String actionUrl, RestfulPathConvertingParam convertingParam) {
        final String withoutHash = Srl.substringLastFront(actionUrl, "#");
        final String actionPath = Srl.substringFirstFront(withoutHash, "?"); // without query parameter
        final List<String> elementList = splitPath(actionPath);
        final int businessElementCount = convertingParam.getBusinessElementCount();
        if (elementList.size() < businessElementCount) { // basically no way, at least out of target
            return null; // no filter
        }
        final List<String> hyphenatedNameList = convertingParam.getHyphenatedNameList();
        final List<String> classElementList = prepareClassElementList(elementList, businessElementCount, hyphenatedNameList);
        final LinkedList<String> rearElementList = prepareRearElementList(elementList, businessElementCount);
        final List<String> restfulList = new ArrayList<>();
        final List<String> methodKeywordList = new ArrayList<>(); // lazy loaded
        boolean parameterAppeared = false;
        for (String classElement : classElementList) {
            restfulList.add(classElement);
            while (true) { // for recursive
                if (rearElementList.isEmpty()) {
                    break;
                }
                final String first = String.valueOf(rearElementList.pollFirst());
                if (isParameterInRearPart(actionType, rearElementList, first)) { // e.g. number parameter, {} (urlPattern) if Lasta Meta
                    parameterAppeared = true;
                }
                if (parameterAppeared) {
                    restfulList.add(first);
                    break;
                } else { // before number parameter
                    final List<String> eventSuffixHyphenatedNameList = convertingParam.getEventSuffixHyphenatedNameList();
                    handleMethodKeyword(methodKeywordList, first, eventSuffixHyphenatedNameList); // e.g. sea (method keyword)
                    // no break, continue for next parts element
                }
            }
        }
        for (String methodKeyword : methodKeywordList) { // e.g. get$sea()
            restfulList.add(methodKeyword); // e.g. /products/{productId}/purchases/sea/
        }
        for (String remainingElement : rearElementList) { // basically empty if pure RESTful
            restfulList.add(remainingElement); // e.g. /products/1/purchases/2/3/4
        }
        return buildPath(restfulList);
    }

    // -----------------------------------------------------
    //                                         Class Element
    //                                         -------------
    protected List<String> prepareClassElementList(List<String> elementList, int classElementCount, List<String> hyphenatedNameList) {
        return resolveHyphenation(elementList.subList(0, classElementCount), hyphenatedNameList);
    }

    protected List<String> resolveHyphenation(List<String> classElementList, List<String> hyphenatedNameList) {
        if (hyphenatedNameList.isEmpty()) {
            return classElementList;
        }
        final String businessSnakeName = classElementList.stream().collect(Collectors.joining("_")); // e.g. ballet_dancers_studios
        return restfulComponentAnalyzer.deriveResourceNameListBySnakeName(businessSnakeName, hyphenatedNameList);
    }

    // -----------------------------------------------------
    //                                         Rear Handling
    //                                         -------------
    protected LinkedList<String> prepareRearElementList(List<String> elementList, int classElementCount) {
        return new LinkedList<>(elementList.subList(classElementCount, elementList.size()));
    }

    protected abstract boolean isParameterInRearPart(Class<?> actionType, LinkedList<String> rearElementList, String first);

    // -----------------------------------------------------
    //                                        Method Keyword
    //                                        --------------
    protected void handleMethodKeyword(List<String> methodKeywordList, String first, List<String> eventSuffixHyphenatedNameList) {
        final String resolved = resolveEventSuffixHyphenation(first, eventSuffixHyphenatedNameList);
        methodKeywordList.add(resolved); // event-suffix e.g. sea (method keyword)
    }

    protected String resolveEventSuffixHyphenation(String first, List<String> eventSuffixHyphenatedNameList) {
        if (!eventSuffixHyphenatedNameList.isEmpty()) { // e.g. eventSuffixHyphenate="mystic-hangar"
            if (Srl.isUpperCaseAny(first)) { // e.g. get$mysticHangar()
                final String demecaliedFirst = Srl.decamelize(first, "-").toLowerCase(); // e.g. mystic-hangar
                if (eventSuffixHyphenatedNameList.contains(demecaliedFirst)) {
                    return demecaliedFirst; // switched to hyphenated name
                }
            }
        }
        return first; // mainly here, no hyphenation
    }

    // -----------------------------------------------------
    //                                       Parameter Class
    //                                       ---------------
    protected static class RestfulPathConvertingParam {

        protected final int businessElementCount;
        protected final List<String> hyphenatedNameList; // basically immutable
        protected final List<String> eventSuffixHyphenatedNameList; // me too

        public RestfulPathConvertingParam(int businessElementCount, List<String> hyphenatedNameList,
                List<String> eventSuffixHyphenatedNameList) {
            this.businessElementCount = businessElementCount;
            this.hyphenatedNameList = hyphenatedNameList;
            this.eventSuffixHyphenatedNameList = eventSuffixHyphenatedNameList;
        }

        public int getBusinessElementCount() {
            return businessElementCount;
        }

        public List<String> getHyphenatedNameList() {
            return hyphenatedNameList;
        }

        public List<String> getEventSuffixHyphenatedNameList() {
            return eventSuffixHyphenatedNameList;
        }
    }

    // ===================================================================================
    //                                                                        Top Category
    //                                                                        ============
    // deleted by three reason: by jflute (2021/06/19)
    //  o to keep router logic simple
    //  o hyphenation is similar function
    //  o originally should be application logic
    //@Deprecated
    //protected boolean isTopCategorizedPath(String requestPath) { // you can override
    //    return false;
    //}

    // ===================================================================================
    //                                                                         Path Helper
    //                                                                         ===========
    protected String buildPath(List<String> elementList) {
        return Srl.quoteAnything(Srl.connectByDelimiter(elementList, "/"), "/");
    }

    protected List<String> splitPath(String path) {
        return Srl.splitList(path, "/").stream().filter(el -> !el.isEmpty()).collect(Collectors.toList());
    }

    protected List<String> splitResource(String resource, String delimiter) {
        return Srl.splitList(resource, delimiter).stream().filter(el -> !el.isEmpty()).collect(Collectors.toList());
    }
}