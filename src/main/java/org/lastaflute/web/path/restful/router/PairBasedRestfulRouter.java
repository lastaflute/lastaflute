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
        int index = 0;
        for (String element : elementList) {
            if (Srl.isNumberHarfAll(element)) { // e.g. 1
                if (index % 2 == 0) { // first, third... e.g. /[1]/products/, /products/1/[2]/purchases
                    return false;
                }
            }
            // string elements can be at any place so no more check
            ++index;
        }
        return true;
    }

    // -----------------------------------------------------
    //                                          Convert Path
    //                                          ------------
    @Override
    protected String convertToMappingPath(String requestPath) {
        final List<String> elementList = splitPath(requestPath);
        final List<String> arrangedList = new ArrayList<>();
        final List<String> parameterList = new ArrayList<>();
        int index = 0;
        for (String element : elementList) {
            if (index % 2 == 0) { // first, third...
                arrangedList.add(element);
            } else { // second, fourth
                parameterList.add(element);
            }
            ++index;
        }
        arrangedList.addAll(parameterList); // e.g. /products/purchases/1/2/
        return buildPath(arrangedList);
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