/*
 * Copyright 2015-2024 the original author or authors.
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
package org.lastaflute.web.path;

import org.lastaflute.web.ruts.config.ActionExecute;

/**
 * @author jflute
 */
@FunctionalInterface
public interface ActionFoundPathHandler {

    /**
     * Handle the found action path. <br>
     * This is called back when the action execution is found. <br>
     * If the paramPath is empty, the configByParam is null. <br>
     * And if the paramPath is not empty, the configByParam is not null. <br>
     * @param pathResource The resource path having plain requestPath and customized mappingPath. (NotNull)
     * @param actionName The component name of found action. (NotNull)
     * @param paramPath The routing path for parameters. (NotNull, EmptyAllowed)
     * @param methodByParam The method of action execute found by parameters. (NullAllowed: when the path of parameter is empty)
     * @return Is it handled? (true if the action was found by the path)
     * @throws Exception When the handling process throws something.
     */
    boolean handleActionPath(MappingPathResource pathResource, String actionName, RoutingParamPath paramPath, ActionExecute methodByParam)
            throws Exception;
}
