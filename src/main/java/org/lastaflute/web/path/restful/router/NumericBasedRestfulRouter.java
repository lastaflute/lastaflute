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
import java.util.List;

import org.dbflute.util.Srl;
import org.lastaflute.web.path.UrlMappingResource;

/**
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

    // -----------------------------------------------------
    //                                          Convert Path
    //                                          ------------
    @Override
    protected String convertToMappingPath(String requestPath) {
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
        arrangedList.addAll(numberList); // e.g. /products/purchases/1/2/
        return buildPath(arrangedList);
    }
}