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

import java.util.Collections;
import java.util.List;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
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

    // ===================================================================================
    //                                                                Action Business Name
    //                                                                ====================
    public String extractActionBusinessDecamerizedName(Class<?> actionType) { // e.g. ballet_dancers if BalletDancersAction
        final String businessPartName = Srl.substringLastFront(actionType.getSimpleName(), "Action");
        return Srl.decamelize(businessPartName).toLowerCase();
    }

    public List<String> extractActionBusinessElementList(Class<?> actionType) { // e.g. [ballet, dancers] if BalletDancersAction
        final String decamerizedName = extractActionBusinessDecamerizedName(actionType);
        return Collections.unmodifiableList(Srl.splitList(decamerizedName, "_")); // snake case using "_"
    }

    // ===================================================================================
    //                                                                Action Resource Name
    //                                                                ====================
    public List<String> deriveResourceNameList(Class<?> actionType) { // resolves hyphenation
        if (actionType == null) {
            throw new IllegalArgumentException("The argument 'actionType' should not be null.");
        }
        if (!hasRestfulAnnotation(actionType)) {
            throw new IllegalArgumentException("The action is not RESTful, it needs RESTful annotation.");
        }
        String decamelizedName = extractActionBusinessDecamerizedName(actionType);
        final String[] hyphenatedNameList = getRestfulAnnotation(actionType).get().hyphenate(); // not null here
        if (hyphenatedNameList.length >= 1) {
            for (String hyphenatedName : hyphenatedNameList) { // e.g. ballet-dancers
                // always hit here because action customzer already check it so no check here
                final String hyphenatedSnakeName = Srl.replace(hyphenatedName, "-", "_"); // e.g. ballet_dancers
                decamelizedName = Srl.replace(decamelizedName, hyphenatedSnakeName, Srl.quoteAnything(hyphenatedName, "@"));
            }
        }
        // to avoid overlapped hit
        // e.g. Action=SeaLandPiariLandPiariAction, hyphenatedNameList=[sea-land, land-piari]
        // then it should be sea-land/piari/land-piari (not hit first LandPiari)
        decamelizedName = Srl.replace(decamelizedName, "@", "");
        return Srl.splitList(decamelizedName, "_");
    }
}