/*
 * Copyright 2015-2020 the original author or authors.
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
        return doDetermineRestfulPath(resource, elementList);
    }

    protected abstract boolean doDetermineRestfulPath(UrlMappingResource resource, List<String> elementList);

    protected boolean isRootAction(UrlMappingResource resource, List<String> elementList) {
        return elementList.isEmpty();
    }

    protected boolean isSwaggerPath(UrlMappingResource resource, List<String> elementList) {
        return !elementList.isEmpty() && elementList.get(0).equals("swagger");
    }

    protected boolean isExceptPath(UrlMappingResource resource, List<String> elementList) { // you can override
        return false;
    }

    // -----------------------------------------------------
    //                                          Convert Path
    //                                          ------------
    protected abstract String convertToMappingPath(String requestPath);

    // ===================================================================================
    //                                                                         URL Reverse
    //                                                                         ===========
    public OptionalThing<UrlReverseOption> toRestfulReversePath(UrlReverseResource resource) {
        if (!determineRestfulAction(resource)) {
            return OptionalThing.empty();
        }
        final int classElementCount = countClassElement(resource);
        final UrlReverseOption option = new UrlReverseOption();
        final Class<?> actionType = resource.getActionType();
        option.filterActionUrl(actionUrl -> {
            return convertToRestfulPath(actionType, actionUrl, classElementCount);
        });
        return OptionalThing.of(option);
    }

    protected boolean determineRestfulAction(UrlReverseResource resource) {
        // restful action's method verification is at action initialization
        return resource.getActionType().getAnnotation(RestfulAction.class) != null;
    }

    protected int countClassElement(UrlReverseResource resource) {
        final Class<?> actionType = resource.getActionType();
        final String actionName = Srl.substringLastFront(actionType.getSimpleName(), "Action");
        final String snakeCaseName = Srl.decamelize(actionName);
        return Srl.count(snakeCaseName, "_") + 1;
    }

    protected String convertToRestfulPath(Class<?> actionType, String actionUrl, int classElementCount) {
        final String withoutHash = Srl.substringLastFront(actionUrl, "#");
        final String actionPath = Srl.substringFirstFront(withoutHash, "?"); // without query parameter
        final List<String> elementList = splitPath(actionPath);
        if (elementList.size() < classElementCount) { // basically no way, at least out of target
            return null; // no filter
        }
        final List<String> classElementList = elementList.subList(0, classElementCount);
        final LinkedList<String> rearElementList = new LinkedList<>(elementList.subList(classElementCount, elementList.size()));
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

    protected abstract boolean isParameterInRearPart(Class<?> actionType, LinkedList<String> rearElementList, String first);

    // ===================================================================================
    //                                                                         Path Helper
    //                                                                         ===========
    protected String buildPath(List<String> elementList) {
        return Srl.quoteAnything(Srl.connectByDelimiter(elementList, "/"), "/");
    }

    protected List<String> splitPath(String path) {
        return Srl.splitList(path, "/").stream().filter(el -> !el.isEmpty()).collect(Collectors.toList());
    }
}