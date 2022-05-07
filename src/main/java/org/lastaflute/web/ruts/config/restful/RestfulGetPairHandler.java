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
package org.lastaflute.web.ruts.config.restful;

import java.util.Collections;
import java.util.List;

import org.lastaflute.web.ruts.config.ActionExecute;

/**
 * @author jflute
 * @since 1.2.1 (2021/05/16 at roppongi japanese)
 */
public class RestfulGetPairHandler {

    // ===================================================================================
    //                                                                Pair Demitermination
    //                                                                ====================
    public boolean determineRestfulGetPairExecute(ActionExecute currentExecute, ActionExecute existingExecute) {
        if (isRestfulGetCandidate(currentExecute) && isRestfulGetCandidate(existingExecute)) {
            return matchesPairParameterCount(currentExecute, existingExecute);
        }
        return false;
    }

    // ===================================================================================
    //                                                                      Pair Candidate
    //                                                                      ==============
    protected boolean isRestfulGetCandidate(ActionExecute execute) {
        if (isRestfulHttpGet(execute) && execute.isIndexMethod()) { // get$index(...)
            return isEmptyPathParam(execute) || isFixedParameterOnly(execute);
        }
        return false;
    }

    protected boolean isRestfulHttpGet(ActionExecute execute) {
        return execute.getRestfulHttpMethod().filter(mt -> "get".equals(mt)).isPresent();
    }

    protected boolean isEmptyPathParam(ActionExecute execute) {
        return !execute.getPathParamArgs().isPresent();
    }

    protected boolean isFixedParameterOnly(ActionExecute execute) {
        return execute.getPathParamArgs().filter(args -> {
            return args.getOptionalGenericTypeMap().isEmpty(); // no optional parameter
        }).isPresent();
    }

    // ===================================================================================
    //                                                                      Pair Parameter
    //                                                                      ==============
    protected boolean matchesPairParameterCount(ActionExecute currentExecute, ActionExecute existingExecute) {
        final List<Class<?>> currentPathParamTypeList = extractPathParamTypeList(currentExecute);
        final List<Class<?>> existingPathParamTypeList = extractPathParamTypeList(existingExecute);

        final Integer currentPathParamCount = currentPathParamTypeList.size();
        final Integer existingPathParamCount = existingPathParamTypeList.size();
        if (currentPathParamCount + 1 != existingPathParamCount && currentPathParamCount != existingPathParamCount + 1) {
            return false; // different parameter count
        }

        // e.g.
        //  list :: get$index(Integer seaId, Integer hangarId) // 2
        //  one  :: get$index(Integer seaId, Integer hangarId, Integer mysticId) // 3
        final int commonParamCount = Math.min(currentPathParamCount, existingPathParamCount); // e.g. 2
        int index = 0;
        for (Class<?> currentParamType : currentPathParamTypeList) {
            if (index == commonParamCount) {
                break;
            }
            final Class<?> existingParamType = existingPathParamTypeList.get(index);
            if (!currentParamType.equals(existingParamType)) {
                return false; // e.g. get$index(Integer seaId) and get$index(Long seaId, ...)
            }
            ++index;
        }
        return true; // congratulation!
    }

    protected List<Class<?>> extractPathParamTypeList(ActionExecute currentExecute) {
        return currentExecute.getPathParamArgs().map(args -> args.getPathParamTypeList()).orElseGet(() -> Collections.emptyList());
    }

    protected Integer countFixedPathParam(ActionExecute currentExecute) {
        return currentExecute.getPathParamArgs().map(args -> args.getPathParamTypeList().size()).orElse(0);
    }
}
