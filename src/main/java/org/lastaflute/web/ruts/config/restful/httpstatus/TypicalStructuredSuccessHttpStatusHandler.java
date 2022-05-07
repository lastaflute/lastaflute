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
package org.lastaflute.web.ruts.config.restful.httpstatus;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.servlet.http.HttpServletResponse;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.di.util.tiger.LdiGenericUtil;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.process.ActionRuntime;

/**
 * @author jflute
 * @since 1.2.1 (2021/05/21 Friday at roppongi japanese)
 */
public class TypicalStructuredSuccessHttpStatusHandler {

    // ===================================================================================
    //                                                                             Reflect
    //                                                                             =======
    public void reflectHttpStatusIfNeeds(ActionRuntime runtime) {
        askStructuredHttpStatus(runtime).ifPresent(status -> {
            runtime.getActionResponse().httpStatus(status); // response already exists here
        });
    }

    protected OptionalThing<Integer> askStructuredHttpStatus(ActionRuntime runtime) {
        if (!canOverrideSpecifiedHttpStatus(runtime) && hasAlreadyHttpStatus(runtime)) {
            return OptionalThing.empty(); // specified status is used
        }
        if (isAfterSuccessExecution(runtime)) {
            final ActionExecute execute = runtime.getActionExecute();
            return deriveSuccessStatus(execute);
        }
        // failure HTTP status is handled at ApiFailureHook so success only here
        return OptionalThing.empty();
    }

    // ===================================================================================
    //                                                                Runtime Precondition
    //                                                                ====================
    // you can use ActionRuntime here
    protected boolean canOverrideSpecifiedHttpStatus(ActionRuntime runtime) { // you can select it
        return false; // non-override as default
    }

    protected boolean hasAlreadyHttpStatus(ActionRuntime runtime) {
        final ActionResponse response = runtime.getActionResponse(); // null allowed
        return response != null && response.getHttpStatus().isPresent();
    }

    protected boolean isAfterSuccessExecution(ActionRuntime runtime) {
        return runtime.hasActionResponse() && runtime.withoutFailureAndError();
    }

    // ===================================================================================
    //                                                        HTTP Status (structure only)
    //                                                        ============================
    // only ActionExecute is allowed here (don't use ActionRuntime)
    public OptionalThing<Integer> deriveSuccessStatus(ActionExecute execute) { // may be called by swagger
        Integer httpStatus = null;
        if (canBeStatusCreated(execute)) {
            httpStatus = HttpServletResponse.SC_CREATED;
        } else if (canBeStatusNoContent(execute)) {
            httpStatus = HttpServletResponse.SC_NO_CONTENT;
        }
        return OptionalThing.ofNullable(httpStatus, () -> {
            throw new IllegalStateException("Not found the conventional HTTP status: " + execute);
        });
    }

    protected boolean canBeStatusCreated(ActionExecute execute) {
        return isCreatedTargetMethod(execute);
    }

    protected boolean canBeStatusNoContent(ActionExecute execute) {
        return isNoContentTargetMethod(execute) && isReturnVoidJsonResponse(execute);
    }

    // ===================================================================================
    //                                                        HTTP Method (structure only)
    //                                                        ============================
    // only ActionExecute is allowed here (don't use ActionRuntime)
    // -----------------------------------------------------
    //                                  Status Determination
    //                                  --------------------
    protected boolean isCreatedTargetMethod(ActionExecute execute) {
        return judgePostMethod(execute);
    }

    protected boolean isNoContentTargetMethod(ActionExecute execute) {
        return judgePutMethod(execute) || judgeDeleteMethod(execute) || judgePatchMethod(execute);
    }

    // -----------------------------------------------------
    //                                  Single Determination
    //                                  --------------------
    protected boolean judgePostMethod(ActionExecute execute) {
        return doJudgeHttpMethod(execute, "post");
    }

    protected boolean judgePutMethod(ActionExecute execute) {
        return doJudgeHttpMethod(execute, "put");
    }

    protected boolean judgePatchMethod(ActionExecute execute) {
        return doJudgeHttpMethod(execute, "patch");
    }

    protected boolean judgeDeleteMethod(ActionExecute execute) {
        return doJudgeHttpMethod(execute, "delete");
    }

    protected boolean doJudgeHttpMethod(ActionExecute execute, String httpMethod) {
        return execute.getRestfulHttpMethod().filter(mt -> mt.equalsIgnoreCase(httpMethod)).isPresent();
    }

    // ===================================================================================
    //                                                    Action Response (structure only)
    //                                                    ================================
    // only ActionExecute is allowed here (don't use ActionRuntime)
    protected boolean isReturnVoidJsonResponse(ActionExecute execute) {
        return judgeReturnJsonResponse(execute) && judgeReturnVoidGeneric(execute); // e.g. JsonResponse<Void>
    }

    protected boolean judgeReturnJsonResponse(ActionExecute execute) {
        final Method executeMethod = execute.getExecuteMethod(); // not null
        return JsonResponse.class.isAssignableFrom(executeMethod.getReturnType()); // extended class allowed
    }

    protected boolean judgeReturnVoidGeneric(ActionExecute execute) {
        // don't use e.g. class YourJsonResponse extends JsonResponse<Sea> (cannot work here)
        // use JSON response simply e.g. JsonResponse<Sea> or YourJsonResponse<Sea>
        // (maybe resolve fixed generic for super class by Class@getGenericSuperclass(), for future)
        final Method executeMethod = execute.getExecuteMethod(); // not null
        final Type genericReturnType = executeMethod.getGenericReturnType(); // not null
        final Type[] genericParameterTypes = LdiGenericUtil.getGenericParameterTypes(genericReturnType); // not null
        if (genericParameterTypes.length == 1) { // precondition
            final Class<?> genericFirstClass = LdiGenericUtil.getGenericFirstClass(genericReturnType); // not null here
            return genericFirstClass != null && Void.class.isAssignableFrom(genericFirstClass); // but just in case
        }
        return false;
    }
}
