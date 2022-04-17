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

import org.dbflute.util.Srl;
import org.lastaflute.web.path.UrlMappingResource;

/**
 * The RESTful router on pair-elements basis.
 * <pre>
 * o you can use complete string ID as default
 * o you CANNOT use event-suffix e.g. get$sea()
 * </pre>
 * @author jflute
 * @since 1.2.1 (2021/05/20)
 */
public class PairBasedRestfulRouter extends AbstractBasedRestfulRouter {

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
        for (String element : elementList) {
            if (index % 2 == 0) { // first, third... e.g. /[products]/1/, /[products]/1/[purchases]/2/
                if (isIllegalResourceName(element)) { // e.g. /[1]/products/, /products/1/[2]/purchases/
                    return false;
                }
            }
            // string elements can be at any place so no more check
            ++index;
        }
        return true;
    }

    protected boolean isIllegalResourceName(String element) {
        return Srl.isNumberHarfAll(element); // cannot use numeric resource name
    }

    // -----------------------------------------------------
    //                                          Convert Path
    //                                          ------------
    @Override
    protected String doConvertToMappingPath(String requestPath) {
        final List<String> elementList = splitPath(requestPath);
        final List<String> resourceList = new ArrayList<>();
        final List<String> idList = new ArrayList<>();
        int index = 0;
        for (String element : elementList) {
            if (index % 2 == 0) { // first, third...
                handleResourceElementOnMappingPath(resourceList, element);
            } else { // second, fourth
                handleIdElementOnMappingPath(idList, element);
            }
            ++index;
        }
        final List<String> pathElementList = arrangePathElementList(resourceList, idList);
        return buildPath(pathElementList);
    }

    protected void handleResourceElementOnMappingPath(List<String> resourceList, String element) {
        if (element.contains("-")) { // e.g. ballet-dancers
            resourceList.addAll(splitResource(element, "-")); // for e.g. ballet/dancers
        } else {
            resourceList.add(element);
        }
    }

    protected void handleIdElementOnMappingPath(List<String> parameterList, String element) {
        parameterList.add(element);
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
        // #for_now cannot determine method name so treat all elements as parameter in rear part by jflute (2021/05/20)
        // (pair-based cannot support restish method e.g. get$sea())
        return true;
    }
}