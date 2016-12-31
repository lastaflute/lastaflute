/*
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
package org.lastaflute.web.ruts.config.analyzer;

import java.lang.reflect.Method;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.lastaflute.web.exception.ExecuteMethodHttpMethodUnsupportedException;
import org.lastaflute.web.util.LaActionExecuteUtil;

/**
 * @author jflute
 * @since 0.8.0 (2016/01/17 Sunday)
 */
public class MethodNameAnalyzer {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    public static final String REST_DELIMITER = "$";

    // ===================================================================================
    //                                                                             Mapping
    //                                                                             =======
    public String analyzeMappingMethodName(Method executeMethod) {
        return extractMappingMethodName(executeMethod);
    }

    protected String extractMappingMethodName(Method executeMethod) {
        final String methodName = executeMethod.getName();
        return methodName.contains(REST_DELIMITER) ? Srl.substringFirstRear(methodName, REST_DELIMITER) : methodName;
    }

    // ===================================================================================
    //                                                                             Restful
    //                                                                             =======
    public OptionalThing<String> analyzeRestfulHttpMethod(Method executeMethod) {
        final String extracted = extractRestfulHttpMethod(executeMethod);
        assertResfulHttpMethodIfExists(executeMethod, extracted);
        return OptionalThing.ofNullable(extracted, () -> {
            throw new IllegalStateException("Not found RESTful HTTP method: " + toSimpleMethodExp(executeMethod));
        });
    }

    protected String extractRestfulHttpMethod(Method executeMethod) {
        final String methodName = executeMethod.getName();
        return methodName.contains(REST_DELIMITER) ? Srl.substringFirstFront(methodName, REST_DELIMITER) : null;
    }

    protected void assertResfulHttpMethodIfExists(Method executeMethod, String specified) {
        if (specified == null) {
            return;
        }
        if (!isSupportedHttpMethod(specified)) {
            throwExecuteMethodHttpMethodUnsupportedException(executeMethod, specified);
        }
    }

    protected boolean isSupportedHttpMethod(String extracted) {
        return Srl.equalsPlain(extracted, "get", "post", "put", "delete", "patch"); // not all methods here
    }

    protected void throwExecuteMethodHttpMethodUnsupportedException(Method executeMethod, String specified) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Unsupported restful HTTP method in your execute method.");
        br.addItem("Advice");
        br.addElement("The '$' in execute method name is special mark for HTTP method.");
        br.addElement("e.g. get$index() means index() for GET http method.");
        br.addElement("You can use the following HTTP methods:");
        br.addElement("  get, post, put, delete, patch");
        br.addElement("For example:");
        br.addElement("  (o):");
        br.addElement("    @Execute");
        br.addElement("    public JsonResponse<...> get$index(Integer seaId) {");
        br.addElement("    @Execute");
        br.addElement("    public JsonResponse<...> post$index(SeaBody body) {");
        br.addItem("Execute Method");
        br.addElement(toSimpleMethodExp(executeMethod));
        br.addItem("Specified HTTP method");
        br.addElement(specified);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodHttpMethodUnsupportedException(msg);
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    public String toSimpleMethodExp(Method executeMethod) {
        return LaActionExecuteUtil.buildSimpleMethodExp(executeMethod);
    }
}