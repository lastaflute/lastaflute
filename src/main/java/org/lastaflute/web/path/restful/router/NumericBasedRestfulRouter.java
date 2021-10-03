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
import java.util.LinkedList;
import java.util.List;

import org.dbflute.util.Srl;
import org.lastaflute.web.path.UrlMappingResource;

/**
 * The RESTful router on numeric ID basis.
 * <pre>
 * o you can use conventional string ID with your overriding
 * o you can use event-suffix e.g. get$sea() as default
 * </pre>
 * @author jflute
 * @since 1.2.1 (2021/05/18 Tuesday at roppongi japanese)
 */
public class NumericBasedRestfulRouter extends AbstractBasedRestfulRouter {

    // ===================================================================================
    //                                                                         URL Mapping
    //                                                                         ===========
    // -----------------------------------------------------
    //                                 RESTful Determination
    //                                 ---------------------
    @Override
    protected boolean doDetermineRestfulPath(UrlMappingResource resource, List<String> elementList) {
        // best effort logic and RESTful is prior in the application when using restful router
        int index = 0;
        boolean idAppeared = false;
        boolean secondString = false;
        for (String element : elementList) {
            if (isIdElement(element)) { // e.g. 1
                if (index % 2 == 0) { // first, third... e.g. /[1]/products/, /products/1/[2]/purchases/
                    return false;
                }
                idAppeared = true;
            } else { // e.g. products
                if (index % 2 == 1) { // second, fourth... e.g. /products/[purchases]/
                    secondString = true;
                    if (isIdLocationStringElementNonRestful(index, idAppeared)) {
                        return false;
                    }
                } else { // first, third... e.g. /[products]/..., /products/1/[purchases]/, /products/sea/[purchases]/
                    if (secondString && index == 2) { // e.g. /products/sea/[purchases]/
                        return false;
                    }
                }
            }
            ++index;
        }
        return true; // also contains /products/
    }

    protected boolean isIdLocationStringElementNonRestful(int index, boolean idAppeared) { // best effort logic
        // allows /products/[sea]/ for event-suffix of root resource (RESTful is prior when using router)
        // and allows e.g. /products/1/purchases/[sea]
        // (while /products/1/purchases/[sea]/[land] is allowed, but enough to judge RESTful)
        return index >= 2 && !idAppeared; // e.g. /products/sea/land/
    }

    // -----------------------------------------------------
    //                                          Convert Path
    //                                          ------------
    @Override
    protected String doConvertToMappingPath(String requestPath) {
        // e.g.
        //  /products/1/purchases/
        //  /products/1/purchases/2/
        //  /products/1/purchases/2/payments/
        final List<String> resourceList = new ArrayList<>();
        final List<String> idList = new ArrayList<>();
        final List<String> elementList = splitPath(requestPath);
        for (String element : elementList) {
            if (isIdElement(element)) {
                idList.add(element);
            } else {
                if (element.contains("-")) { // e.g. ballet-dancers
                    resourceList.addAll(splitResource(element, "-")); // e.g. /ballet/dancers/
                } else {
                    resourceList.add(element);
                }
            }
        }
        final List<String> pathElementList = arrangePathElementList(resourceList, idList);
        return buildPath(pathElementList);
    }

    protected List<String> arrangePathElementList(List<String> resourceList, List<String> idList) {
        final List<String> pathElementList = new ArrayList<>();
        pathElementList.addAll(resourceList); // e.g. /products/purchases/
        pathElementList.addAll(idList); // e.g. /products/purchases/1/2/
        return pathElementList;
    }

    // ===================================================================================
    //                                                                         URL Reverse
    //                                                                         ===========
    @Override
    protected boolean isParameterInRearPart(Class<?> actionType, LinkedList<String> rearElementList, String first) {
        return isIdElement(first) || "{}".equals(first);
    }

    // ===================================================================================
    //                                                                          ID Element
    //                                                                          ==========
    protected boolean isIdElement(String element) { // you can override
        return Srl.isNumberHarfAll(element);
    }
}