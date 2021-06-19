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
package org.lastaflute.web.path.restful.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.lastaflute.web.RestfulAction;
import org.lastaflute.web.path.UrlMappingOption;
import org.lastaflute.web.path.UrlMappingResource;
import org.lastaflute.web.path.UrlReverseOption;
import org.lastaflute.web.path.UrlReverseResource;

/**
 * @author jflute
 * @since 1.2.1 (2021/05/20)
 */
public abstract class AbstractBasedRestfulRouter implements RestfulRouter {

    // ===================================================================================
    //                                                                         URL Mapping
    //                                                                         ===========
    public OptionalThing<UrlMappingOption> toRestfulMappingPath(UrlMappingResource resource) {
        final String makingMappingPath = resource.getMakingMappingPath(); // may be filtered before
        final List<String> elementList = splitPath(makingMappingPath); // only for determination

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
        if (isRootAction(resource, elementList)) { // e.g. "/" (RootAction)
            return false;
        }
        if (isSwaggerPath(resource, elementList)) { // e.g. /swagger/ (not business request)
            return false;
        }
        if (isExceptPath(resource, elementList)) { // for application requirement
            return false;
        }
        final List<String> restfulList;
        if (isTopCategorizedPath(resource.getMakingMappingPath())) { // e.g. /mockhama/products/1/purchases/
            restfulList = elementList.subList(1, elementList.size());
        } else { // mainly here
            restfulList = elementList;
        }
        return doDetermineRestfulPath(resource, restfulList);
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
        if (isTopCategorizedPath(requestPath)) {
            final String ltrimmedPath = Srl.ltrim(requestPath, "/");
            final String topCategory = Srl.substringFirstFront(ltrimmedPath, "/");
            final String restfulPath = Srl.substringFirstRear(ltrimmedPath, "/");
            return "/" + topCategory + doConvertToMappingPath(restfulPath);
        } else { // mainly here
            return doConvertToMappingPath(requestPath);
        }
    }

    protected abstract String doConvertToMappingPath(String requestPath);

    // ===================================================================================
    //                                                                         URL Reverse
    //                                                                         ===========
    public OptionalThing<UrlReverseOption> toRestfulReversePath(UrlReverseResource resource) {
        if (!determineRestfulAction(resource)) {
            return OptionalThing.empty();
        }
        final List<String> hyphenatedNameList = extractHyphenatedNameList(resource);
        final int businessElementCount = countActionBusinessElement(resource);
        final UrlReverseOption option = new UrlReverseOption();
        final Class<?> actionType = resource.getActionType();
        option.filterActionUrl(actionUrl -> {
            return convertToRestfulPath(actionType, actionUrl, hyphenatedNameList, businessElementCount);
        });
        return OptionalThing.of(option);
    }

    protected boolean determineRestfulAction(UrlReverseResource resource) {
        // restful action's method verification is at action initialization
        return resource.getActionType().getAnnotation(RestfulAction.class) != null;
    }

    protected List<String> extractHyphenatedNameList(UrlReverseResource resource) {
        final RestfulAction restfulAction = resource.getActionType().getAnnotation(RestfulAction.class);
        if (restfulAction == null) { // basically no way, already checked here
            return Collections.emptyList();
        }
        return Arrays.asList(restfulAction.hyphenate()); // not null, empty allowed
    }

    protected int countActionBusinessElement(UrlReverseResource resource) {
        // not use analyzer here for router indepenency
        // (router has many router own logics so avoid harf recycle)
        final Class<?> actionType = resource.getActionType();
        final String actionName = Srl.substringLastFront(actionType.getSimpleName(), "Action");
        final String snakeCaseName = Srl.decamelize(actionName);
        return Srl.count(snakeCaseName, "_") + 1;
    }

    protected String convertToRestfulPath(Class<?> actionType, String actionUrl, List<String> hyphenatedNameList,
            int businessElementCount) {
        final String withoutHash = Srl.substringLastFront(actionUrl, "#");
        final String actionPath = Srl.substringFirstFront(withoutHash, "?"); // without query parameter
        String topCategory = null;
        final String restfulPath;
        if (isTopCategorizedPath(actionPath)) {
            final String ltrimmedPath = Srl.ltrim(actionPath, "/");
            topCategory = Srl.substringFirstFront(ltrimmedPath, "/");
            restfulPath = Srl.substringFirstRear(ltrimmedPath, "/");
            --businessElementCount;
        } else { // mainly here
            restfulPath = actionPath;
        }
        final List<String> elementList = splitPath(restfulPath);
        if (elementList.size() < businessElementCount) { // basically no way, at least out of target
            return null; // no filter
        }
        final List<String> classElementList = prepareClassElementList(elementList, businessElementCount, hyphenatedNameList);
        final LinkedList<String> rearElementList = prepareRearElementList(elementList, businessElementCount);
        final List<String> restfulList = new ArrayList<>();
        if (topCategory != null) {
            restfulList.add(topCategory);
        }
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
                    methodKeywordList.add(first); // e.g. sea (method keyword)
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

    protected List<String> prepareClassElementList(List<String> elementList, int classElementCount, List<String> hyphenatedNameList) {
        return resolveHyphenation(elementList.subList(0, classElementCount), hyphenatedNameList);
    }

    protected LinkedList<String> prepareRearElementList(List<String> elementList, int classElementCount) {
        return new LinkedList<>(elementList.subList(classElementCount, elementList.size()));
    }

    protected List<String> resolveHyphenation(List<String> classElementList, List<String> hyphenatedNameList) {
        if (hyphenatedNameList.isEmpty()) {
            return classElementList;
        }
        // not use analyzer here for router indepenency
        // (router has many router own logics so avoid harf recycle)
        String resolvedPath = classElementList.stream().collect(Collectors.joining("/")); // e.g. ballet/dancers/studios
        for (String hyphenatedName : hyphenatedNameList) { // always contains "-", not "/" (already checked at boot)
            // always hit here because action customzer already check it so no check here
            final String hyphenatedSlashName = Srl.replace(hyphenatedName, "-", "/"); // e.g. ballet-dancers
            resolvedPath = Srl.replace(resolvedPath, hyphenatedSlashName, hyphenatedName); // e.g. ballet-dancers/studios
        }
        return splitPath(resolvedPath);
    }

    protected abstract boolean isParameterInRearPart(Class<?> actionType, LinkedList<String> rearElementList, String first);

    // ===================================================================================
    //                                                                        Top Category
    //                                                                        ============
    // #thinking jflute may delete it, to keep simple and hyphenation is enough (2021/06/19)
    @Deprecated
    protected boolean isTopCategorizedPath(String requestPath) { // you can override
        return false;
    }

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