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
package org.lastaflute.web.path.restful.analyzer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.lastaflute.di.util.LdiSrl;
import org.lastaflute.web.RestfulAction;

/**
 * @author jflute
 * @since 1.2.1 (2021/06/19 Saturday at ikspiari)
 */
public class RestfulComponentAnalyzer {

    // ===================================================================================
    //                                                                          Annotation
    //                                                                          ==========
    public boolean hasRestfulAnnotation(Class<?> actionType) {
        return actionType.getAnnotation(RestfulAction.class) != null;
    }

    public OptionalThing<RestfulAction> getRestfulAnnotation(Class<?> actionType) {
        final RestfulAction anno = actionType.getAnnotation(RestfulAction.class);
        return OptionalThing.ofNullable(anno, () -> {
            throw new IllegalStateException("Not found the RESTful annotation for the action: " + actionType);
        });
    }

    public List<String> extractHyphenatedNameList(Class<?> actionType) {
        return Arrays.asList(getRestfulAnnotation(actionType).get().hyphenate());
    }

    // ===================================================================================
    //                                                                Action Business Name
    //                                                                ====================
    public int countActionBusinessElement(Class<?> actionType) { // e.g. 2 if BalletDancersAction
        return Srl.count(extractActionBusinessSnakeName(actionType), "_") + 1;
    }

    public String extractActionBusinessSnakeName(Class<?> actionType) { // lower case e.g. ballet_dancers if BalletDancersAction
        // Srl.decamelize() at DBFlute old version has rare-case bug e.g. FooDName => FOOD_NAME (hope FOO_D_NAME)
        // so copy fixed logic to Lasta Di' one and use it here not to depend on DBFlute version 
        final String businessPartName = Srl.substringLastFront(actionType.getSimpleName(), "Action");
        return LdiSrl.decamelize(businessPartName).toLowerCase();
    }

    public List<String> extractActionBusinessElementList(Class<?> actionType) { // e.g. [ballet, dancers] if BalletDancersAction
        final String snakeName = extractActionBusinessSnakeName(actionType);
        return Collections.unmodifiableList(Srl.splitList(snakeName, "_")); // snake case using "_"
    }

    // ===================================================================================
    //                                                                Action Resource Name
    //                                                                ====================
    // resource name means hyphenation is resolved
    public List<String> deriveResourceNameListByActionType(Class<?> actionType) { // called by e.g. verifier
        if (actionType == null) {
            throw new IllegalArgumentException("The argument 'actionType' should not be null.");
        }
        if (!hasRestfulAnnotation(actionType)) {
            throw new IllegalArgumentException("The action is not RESTful, it needs RESTful annotation.");
        }
        final String businessSnakeName = extractActionBusinessSnakeName(actionType);
        final List<String> hyphenatedNameList = extractHyphenatedNameList(actionType);
        return deriveResourceNameListBySnakeName(businessSnakeName, hyphenatedNameList);
    }

    public List<String> deriveResourceNameListBySnakeName(String businessSnakeName, List<String> hyphenatedNameList) { // called by e.g. router
        if (businessSnakeName == null) {
            throw new IllegalArgumentException("The argument 'snakeName' should not be null.");
        }
        if (hyphenatedNameList == null) {
            throw new IllegalArgumentException("The argument 'hyphenatedNameList' should not be null.");
        }
        // a very few loop so ignore cost of string instances
        for (String hyphenatedName : hyphenatedNameList) { // e.g. ballet-dancers
            final String hyphenatedSnakeName = Srl.replace(hyphenatedName, "-", "_"); // e.g. ballet_dancers
            if (businessSnakeName.contains(hyphenatedSnakeName)) { // always hit here, already checked but just in case
                // replace first only because next same names may be connected by other names
                // e.g. /ballet-dancers/1/favorite-ballet-dancers/2/
                final String front = Srl.substringFirstFront(businessSnakeName, hyphenatedSnakeName);
                final String rear = Srl.substringFirstRear(businessSnakeName, hyphenatedSnakeName);
                businessSnakeName = front + "@" + hyphenatedName + "@" + rear;
            }
        }
        // to avoid overlapped hit
        // e.g. Action=SeaLandPiariLandPiariAction, hyphenatedNameList=[sea-land, land-piari]
        // then it should be sea-land/piari/land-piari (not hit first LandPiari)
        businessSnakeName = Srl.replace(businessSnakeName, "@", "");
        return Srl.splitList(businessSnakeName, "_");
    }
}