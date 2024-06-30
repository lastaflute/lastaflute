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
package org.lastaflute.web.path.restful.verifier;

import java.util.List;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.web.RestfulAction;
import org.lastaflute.web.exception.RestfulMappingNonRestfulActionException;
import org.lastaflute.web.exception.RestfulMappingPlainPathRestfulActionException;
import org.lastaflute.web.path.MappingPathResource;
import org.lastaflute.web.path.RoutingParamPath;
import org.lastaflute.web.ruts.config.ActionExecute;

/**
 * @author jflute (2021/05/18 Tuesday at roppongi japanese)
 */
public class RestfulMappingVerifier {

    // ===================================================================================
    //                                                                     Restful Mapping
    //                                                                     ===============
    public void verifyRestfulMapping(MappingPathResource pathResource, ActionExecute execute, RoutingParamPath paramPath) {
        if (pathResource.isRestfulMapping()) {
            final RestfulAction anno = execute.getActionType().getAnnotation(RestfulAction.class);
            if (anno == null) { // don't forget RestfulAction annotation
                if (shouldBeRestful(pathResource, execute, paramPath)) {
                    throwRestfulMappingNonRestfulActionException(pathResource, execute, paramPath);
                }
            }
        } else {
            final RestfulAction anno = execute.getActionType().getAnnotation(RestfulAction.class);
            if (anno != null) { // don't use RestfulAction annotation for plain action
                // immediately unneeded because of fixing router by jflute (2021/08/22)
                //if (!isRootResourceEventSuffix(execute)) { // cannot detect restful mapping so except it
                throwRestfulMappingPlainPathRestfulActionException(pathResource, execute, paramPath, anno);
            }
        }
    }

    // ===================================================================================
    //                                                                   Non RestfulAction
    //                                                                   =================
    protected boolean shouldBeRestful(MappingPathResource pathResource, ActionExecute execute, RoutingParamPath paramPath) {
        // router treats /normal/ as restful by limitted logic
        // however non-restful actions in same application are allowed as possible
        // while restful actions without annotation should be checked here
        // so it uses this logic:
        //  o actions using HTTP method (at least one) should be restful actions
        //  o non-restful actions in same application should not use HTTP method
        final List<ActionExecute> groupedList = execute.getActionMapping().getExecuteList();
        return groupedList.stream().anyMatch(colleague -> colleague.getRestfulHttpMethod().isPresent());
    }

    protected void throwRestfulMappingNonRestfulActionException(MappingPathResource pathResource, ActionExecute execute,
            RoutingParamPath paramPath) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Restful mapping but non-restful action");
        br.addItem("Advice");
        br.addElement("Restful mapping path should be mapped to restful action.");
        br.addElement("(Restful mapping path means the path is provided by restful router)");
        br.addElement("(Restful action means the action has @RestfulAction annotation)");
        br.addElement("");
        br.addElement("So confirm your action's annotation or your router logic.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public class SeaAction {");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    @RestfulAction // Good");
        br.addElement("    public class SeaAction {");
        br.addElement("    }");
        br.addElement("");
        br.addElement("Or don't use HTTP method if the action is not for restful.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public JsonResponse<...> get$index() {");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public JsonResponse<...> index() {");
        br.addElement("    }");
        br.addItem("Request Path");
        br.addElement(pathResource.getRequestPath());
        br.addItem("Restful Mapping Path");
        br.addElement(pathResource.getMappingPath());
        br.addItem("Mapped Action");
        br.addElement(execute.getActionType().getName());
        br.addItem("Execute Method");
        br.addElement(execute);
        br.addItem("paramPath");
        br.addElement(paramPath);
        final String msg = br.buildExceptionMessage();
        throw new RestfulMappingNonRestfulActionException(msg);
    }

    // ===================================================================================
    //                                                                       RestfulAction
    //                                                                       =============
    // immediately unneeded because of fixing router by jflute (2021/08/22)
    //protected boolean isRootResourceEventSuffix(ActionExecute execute) {
    //    // #for_now jflute cannot detect restful mapping in case of event-suffix of root resource without ID (2021/08/22)
    //    //  e.g. ProductsAction@get$sea(as list), ProductsAction@post$sea()
    //    if (!execute.isIndexMethod()) { // means event-suffix
    //        final String resourceCamel = Srl.substringLastFront(execute.getActionType().getSimpleName(), "Action");
    //        return Srl.count(Srl.decamelize(resourceCamel, "_"), "_") == 0;
    //    }
    //    return false;
    //}

    protected void throwRestfulMappingPlainPathRestfulActionException(MappingPathResource pathResource, ActionExecute execute,
            RoutingParamPath paramPath, RestfulAction anno) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Restful action but non-restful mapping");
        br.addItem("Advice");
        br.addElement("Restful action should be mapped by restful mapping path.");
        br.addElement("(Restful action means the action has @RestfulAction annotation)");
        br.addElement("(Restful mapping path means the path is provided by restful router)");
        br.addElement("");
        br.addElement("So confirm that your action is really restful or your router logic.");
        br.addElement("Restful router is basically used at [App]ActionAdjustProvider.");
        br.addItem("Request Path");
        br.addElement(pathResource.getRequestPath());
        br.addItem("Plain Mapping Path");
        br.addElement(pathResource.getMappingPath());
        br.addItem("Mapped Action");
        br.addElement(execute.getActionType().getName());
        br.addItem("Execute Method");
        br.addElement(execute);
        br.addItem("paramPath");
        br.addElement(paramPath);
        br.addItem("Annotation");
        br.addElement(anno);
        final String msg = br.buildExceptionMessage();
        throw new RestfulMappingPlainPathRestfulActionException(msg);
    }
}