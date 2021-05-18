/*
 * Copyright 2015-2018 the original author or authors.
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
package org.lastaflute.web.path.restful;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.web.RestfulAction;
import org.lastaflute.web.exception.RestfulMappingNonRestfulActionException;
import org.lastaflute.web.exception.RestfulMappingPlainPathRestfulActionException;
import org.lastaflute.web.path.MappingPathResource;
import org.lastaflute.web.ruts.config.ActionExecute;

/**
 * @author jflute (2021/05/18 Tuesday at roppongi japanese)
 */
public class RestfulMappingVerifier {

    public void verifyRestfulMapping(MappingPathResource pathResource, ActionExecute execute, String paramPath) {
        if (pathResource.isRestfulMapping()) {
            final RestfulAction anno = execute.getActionType().getAnnotation(RestfulAction.class);
            if (anno == null) { // don't forget RestfulAction annotation
                throwRestfulMappingNonRestfulActionException(pathResource, execute, paramPath);
            }
        } else {
            final RestfulAction anno = execute.getActionType().getAnnotation(RestfulAction.class);
            if (anno != null) { // don't use RestfulAction annotation for plain action
                throwRestfulMappingPlainPathRestfulActionException(pathResource, execute, paramPath, anno);
            }
        }
    }

    protected void throwRestfulMappingNonRestfulActionException(MappingPathResource pathResource, ActionExecute execute, String paramPath) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Restful mapping but non-restful action");
        br.addItem("[Advice]");
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

    protected void throwRestfulMappingPlainPathRestfulActionException(MappingPathResource pathResource, ActionExecute execute,
            String paramPath, RestfulAction anno) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Restful action but non-restful mapping");
        br.addItem("[Advice]");
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