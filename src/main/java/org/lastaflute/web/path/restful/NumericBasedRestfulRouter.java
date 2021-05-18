/*
 * Copyright 2015-2018 the original author or authors.
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
package org.lastaflute.web.path.restful;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.web.RestfulAction;
import org.lastaflute.web.path.UrlMappingOption;
import org.lastaflute.web.path.UrlMappingResource;
import org.lastaflute.web.path.UrlReverseOption;
import org.lastaflute.web.path.UrlReverseResource;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author jflute (2021/05/18 Tuesday at roppongi japanese)
 */
public class NumericBasedRestfulRouter {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // #thinking jflute already unneeded? waiting for working of LastaFlute RESTful GET pair (2021/05/16)
    protected boolean virtualListHandling;

    // ===================================================================================
    //                                                                              Option
    //                                                                              ======
    public NumericBasedRestfulRouter enableVirtualListHandling() {
        virtualListHandling = true;
        return this;
    }

    // ===================================================================================
    //                                                                         URL Mapping
    //                                                                         ===========
    public OptionalThing<UrlMappingOption> toRestfulMappingPath(UrlMappingResource resource) {
        final String makingMappingPath = resource.getMakingMappingPath(); // may be filtered before
        final List<String> elementList = splitPath(makingMappingPath); // only for determination

        if (!isRestfulPath(elementList)) { // e.g. /, /1/products/, /products/purchases/
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
        final boolean listGetRequest = isVirtualListGetRequest(elementList); // e.g. GET /products/1/purchases/
        option.filterRequestPath(requestPath -> { // is makingMappingPaths
            return convertToMappingPath(requestPath, listGetRequest);
        });
        option.tellRestfulMapping();
        return option;
    }

    // -----------------------------------------------------
    //                                          Convert Path
    //                                          ------------
    protected String convertToMappingPath(String requestPath, boolean listGetRequest) {
        // e.g.
        //  /products/1/purchases/
        //  /products/1/purchases/2/
        //  /products/1/purchases/2/payments/
        final List<String> stringList = new ArrayList<>();
        final List<String> numberList = new ArrayList<>();
        final List<String> elementList = splitPath(requestPath);
        for (String element : elementList) {
            if (Srl.isNumberHarfAll(element)) {
                numberList.add(element);
            } else {
                stringList.add(element);
            }
        }
        final List<String> arrangedList = new ArrayList<>();
        arrangedList.addAll(stringList); // e.g. /products/purchases/
        if (listGetRequest) {
            arrangedList.add(getListGetMethodKeyword()); // e.g. get$list()
        }
        arrangedList.addAll(numberList); // e.g. /products/purchases/1/2/
        return buildPath(arrangedList);
    }

    // -----------------------------------------------------
    //                                 RESTful Determination
    //                                 ---------------------
    protected boolean isRestfulPath(List<String> elementList) {
        if (isRootAction(elementList)) { // e.g. "/" (RootAction)
            return false;
        }
        if (isSwaggerPath(elementList)) { // e.g. /swagger/ (not business request)
            return false;
        }
        int index = 0;
        boolean numberAppeared = false;
        for (String element : elementList) {
            if (Srl.isNumberHarfAll(element)) { // e.g. 1
                if (index % 2 == 0) { // first, third... e.g. /[1]/products/, /products/1/[2]/purchases
                    return false;
                }
                numberAppeared = true;
            } else { // e.g. products
                if (index % 2 == 1) { // second, fourth... e.g. /products/[purchases]/
                    // allows e.g. /products/1/purchases/[sea]
                    // one crossed number parameter is enough to judge RESTful
                    if (!numberAppeared) {
                        return false;
                    }
                }
            }
            ++index;
        }
        return true;
    }

    protected boolean isRootAction(List<String> elementList) {
        return elementList.isEmpty();
    }

    protected boolean isSwaggerPath(List<String> elementList) {
        return !elementList.isEmpty() && elementList.get(0).equals("swagger");
    }

    // ===================================================================================
    //                                                                         URL Reverse
    //                                                                         ===========
    public OptionalThing<UrlReverseOption> toRestfulReversePath(UrlReverseResource resource) {
        if (!isRestfulAction(resource)) {
            return OptionalThing.empty();
        }
        final int classElementCount = countClassElement(resource);
        final UrlReverseOption option = new UrlReverseOption();
        option.filterActionUrl(actionUrl -> {
            return convertToRestfulPath(actionUrl, classElementCount);
        });
        return OptionalThing.of(option);
    }

    protected String convertToRestfulPath(String actionUrl, int classElementCount) {
        final String withoutHash = Srl.substringLastFront(actionUrl, "#");
        final String actionPath = Srl.substringFirstFront(withoutHash, "?"); // without query parameter
        final List<String> elementList = splitPath(actionPath);
        if (elementList.size() < classElementCount) { // basically no way, at least out of target
            return null; // no filter
        }
        final List<String> classElementList = elementList.subList(0, classElementCount);
        final LinkedList<String> partsElementList = new LinkedList<>(elementList.subList(classElementCount, elementList.size()));
        if (isVirtualListGetNamingFirst(partsElementList)) {
            partsElementList.removeFirst();
        }
        final List<String> restfulList = new ArrayList<>();
        final List<String> methodKeywordList = new ArrayList<>(); // lazy loaded
        boolean numberAppeared = false;
        for (String classElement : classElementList) {
            restfulList.add(classElement);
            while (true) { // for recursive
                if (partsElementList.isEmpty()) {
                    break;
                }
                final String first = String.valueOf(partsElementList.pollFirst());
                if (Srl.isNumberHarfAll(first) || "{}".equals(first)) { // number parameter, {} (urlPattern) if Lasta Meta
                    // #for_now jflute thought rare case, dangerous if "{}" is real value for redirect (2021/05/17)
                    numberAppeared = true;
                }
                if (numberAppeared) {
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
        for (String remainingElement : partsElementList) { // basically empty if pure RESTful
            restfulList.add(remainingElement); // e.g. /products/1/purchases/2/3/4
        }
        return buildPath(restfulList);
    }

    protected int countClassElement(UrlReverseResource resource) {
        final Class<?> actionType = resource.getActionType();
        final String actionName = Srl.substringLastFront(actionType.getSimpleName(), "Action");
        final String snakeCaseName = Srl.decamelize(actionName);
        return Srl.count(snakeCaseName, "_") + 1;
    }

    // -----------------------------------------------------
    //                                 RESTful Determination
    //                                 ---------------------
    protected boolean isRestfulAction(UrlReverseResource resource) {
        // restful action's method verification is at action initialization
        return resource.getActionType().getAnnotation(RestfulAction.class) != null;
    }

    // ===================================================================================
    //                                                               Virtual List Handling
    //                                                               =====================
    protected boolean isVirtualListGetRequest(List<String> elementList) { // e.g. GET /products/1/purchases/
        return isVirtualListHandling() && isCurrentRequestGet() && isLastElementString(elementList);
    }

    protected boolean isCurrentRequestGet() {
        final RequestManager requestManager = getRequestManager();
        return requestManager.getHttpMethod().filter(mt -> mt.equalsIgnoreCase("get")).isPresent();
    }

    protected boolean isLastElementString(List<String> elementList) {
        String lastElement = elementList.get(elementList.size() - 1);
        return !Srl.isNumberHarfAll(lastElement);
    }

    protected boolean isVirtualListGetNamingFirst(LinkedList<String> partsElementList) {
        return isVirtualListHandling() && getListGetMethodKeyword().equals(partsElementList.getFirst());
    }

    protected String getListGetMethodKeyword() {
        return "list"; // as default
    }

    // ===================================================================================
    //                                                                           Component
    //                                                                           =========
    protected RequestManager getRequestManager() { // used by virtual list
        return ContainerUtil.getComponent(RequestManager.class);
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

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public boolean isVirtualListHandling() {
        return virtualListHandling;
    }
}