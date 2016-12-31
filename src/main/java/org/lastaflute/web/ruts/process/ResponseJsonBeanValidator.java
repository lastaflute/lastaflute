/*
 * Copyright 2015-2017 the original author or authors.
 * Copyright 2015-2017 the original author or authors.
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
package org.lastaflute.web.ruts.process;

import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author jflute
 * @deprecated for compatible (with e.g. UTFlute)
 */
public class ResponseJsonBeanValidator extends org.lastaflute.web.ruts.process.validatebean.ResponseJsonBeanValidator {

    // for compatible (with e.g. UTFlute)
    public ResponseJsonBeanValidator(RequestManager requestManager, Object actionExp, boolean warning, JsonResponse<?> response) {
        super(requestManager, actionExp, warning, response);
    }
}
