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
package org.lastaflute.web.path.restful.verifier;

import java.util.Collection;
import java.util.List;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.web.RestfulAction;
import org.lastaflute.web.exception.ExecuteMethodIllegalDefinitionException;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.config.ActionMapping;
import org.lastaflute.web.ruts.config.analyzer.UrlPatternAnalyzer;

/**
 * @author jflute (2021/05/19 Wednesday at roppongi japanese)
 */
public class RestfulRomanticVerifier {

    // ===================================================================================
    //                                                                         Independent
    //                                                                         ===========
    // moved from customizer to here 
    public void verifyRestfulIndependent(ActionMapping actionMapping, Class<?> actionType) {
        // regardless restful annotation, check here
        final Collection<ActionExecute> executeList = actionMapping.getExecuteList();
        for (ActionExecute execute : executeList) {
            if (execute.getRestfulHttpMethod().isPresent()) { // e.g. get$index
                final List<ActionExecute> plainList = actionMapping.searchByMethodName(execute.getMappingMethodName()); // e.g. index
                if (!plainList.isEmpty()) { // conflict, e.g. both index() and get$index() exist
                    throwExecuteMethodRestfulConflictException(actionType, execute, plainList);
                }
            }
        }
    }

    protected void throwExecuteMethodRestfulConflictException(Class<?> actionType, ActionExecute restfulExecute,
            List<ActionExecute> plainList) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Conflicted the execute methods between restful and plain.");
        br.addItem("Advice");
        br.addElement("You cannot define restful method with same-name plain method.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse index() { // *Bad");
        br.addElement("    }");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse get$index() { // *Bad");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse sea() { // Good");
        br.addElement("    }");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse get$index() {");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse index() {");
        br.addElement("    }");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse get$sea() { // Good");
        br.addElement("    }");
        br.addItem("Action");
        br.addElement(actionType);
        br.addItem("Restful Method");
        br.addElement(restfulExecute.toSimpleMethodExp());
        br.addItem("Plain Method");
        for (ActionExecute plain : plainList) { // basically one loop
            br.addElement(plain.toSimpleMethodExp());
        }
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    // ===================================================================================
    //                                                                     HTTP Method All
    //                                                                     ===============
    public void verifyRestfulHttpMethodAll(ActionMapping actionMapping, Class<?> actionType) {
        if (!hasRestfulAnnotation(actionType)) {
            return;
        }
        final Collection<ActionExecute> executeList = actionMapping.getExecuteList();
        for (ActionExecute execute : executeList) {
            if (!execute.getRestfulHttpMethod().isPresent()) { // e.g. index()
                throwExecuteMethodRestfulNonHttpMethodException(actionType, execute);
            }
        }
    }

    protected void throwExecuteMethodRestfulNonHttpMethodException(Class<?> actionType, ActionExecute execute) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Found the execute method without HTTP method in restful action.");
        br.addItem("Advice");
        br.addElement("Execute methods in restful action should have HTTP method.");
        br.addElement("Add HTTP method to your execute method.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public JsonResponse<...> index() { // *bad");
        br.addElement("  (o):");
        br.addElement("    public JsonResponse<...> get$index() { // Good");
        br.addElement("  (o):");
        br.addElement("    public JsonResponse<...> post$index() { // Good");
        br.addItem("Action");
        br.addElement(actionType);
        br.addItem("Execute Method");
        br.addElement(execute.toSimpleMethodExp());
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    // ===================================================================================
    //                                                                        Cannot @word
    //                                                                        ============
    public void verifyRestfulCannotAtWord(ActionMapping actionMapping, Class<?> actionType) {
        if (!hasRestfulAnnotation(actionType)) {
            return;
        }
        final Collection<ActionExecute> executeList = actionMapping.getExecuteList();
        for (ActionExecute execute : executeList) {
            execute.getExecuteOption().getSpecifiedUrlPattern().ifPresent(pattern -> {
                if (pattern.getPatternValue().contains(UrlPatternAnalyzer.METHOD_KEYWORD_MARK)) {
                    // to begin with, index() cannot use @word by other check
                    // so actually this check is only for e.g. get$sea() that is restish
                    throwExecuteMethodRestfulCannotAtWordException(actionType, execute);
                }
            });
        }
    }

    protected void throwExecuteMethodRestfulCannotAtWordException(Class<?> actionType, ActionExecute execute) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Found the @word at urlPattern in restful action.");
        br.addItem("Advice");
        br.addElement("You cannot use @word at urlPattern in restful action.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    @Execute(urlPattern=\"{}/@word\") // *bad");
        br.addElement("    public JsonResponse<...> get$sea() {");
        br.addElement("  (o):");
        br.addElement("    @Execute(urlPattern=\"{}/@word\") // Good");
        br.addElement("    public JsonResponse<...> get$sea() {");
        br.addItem("Action");
        br.addElement(actionType);
        br.addItem("Execute Method");
        br.addElement(execute.toSimpleMethodExp());
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    // ===================================================================================
    //                                                                     Cannot Optional
    //                                                                     ===============
    public void verifyRestfulCannotOptional(ActionMapping actionMapping, Class<?> actionType) {
        if (!hasRestfulAnnotation(actionType)) {
            return;
        }
        final Collection<ActionExecute> executeList = actionMapping.getExecuteList();
        for (ActionExecute execute : executeList) {
            if (execute.getPathParamArgs().filter(args -> !args.getOptionalGenericTypeMap().isEmpty()).isPresent()) {
                throwExecuteMethodRestfulCannotOptionalException(actionType, execute);
            }
        }
    }

    protected void throwExecuteMethodRestfulCannotOptionalException(Class<?> actionType, ActionExecute execute) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Found the optional parameter in restful action.");
        br.addItem("Advice");
        br.addElement("You cannot define optional parameter in restful action.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public JsonResponse<...> get$index(OptionalThing<Integer> productId) { // *Bad");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public JsonResponse<...> get$sea(Integer productId) { // Good");
        br.addElement("    }");
        br.addItem("Action");
        br.addElement(actionType);
        br.addItem("Execute Method");
        br.addElement(execute.toSimpleMethodExp());
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    // ===================================================================================
    //                                                                  Cannot EventSuffix
    //                                                                  ==================
    public void verifyRestfulCannotEventSuffix(ActionMapping actionMapping, Class<?> actionType) {
        if (!hasRestfulAnnotation(actionType)) {
            return;
        }
        final RestfulAction restfulAnno = getRestfulAnnotation(actionType); // not null here
        final Collection<ActionExecute> executeList = actionMapping.getExecuteList();
        if (restfulAnno.allowEventSuffix()) { // OK but needs precondition
            for (ActionExecute execute : executeList) {
                if (!execute.isIndexMethod()) { // not (e.g. index(), get$index())
                    if (execute.getRestfulHttpMethod().isPresent()) { // basically true here, just in case
                        final String indexMethodName = execute.getRestfulHttpMethod().get() + "$index";
                        if (actionMapping.searchByMethodName(indexMethodName).isEmpty()) {
                            throwExecuteMethodRestfulAloneEventSuffixException(actionType, execute);
                        }
                    }
                }
            }
        } else { // cannot use event suffix
            for (ActionExecute execute : executeList) {
                if (!execute.isIndexMethod()) { // not (e.g. index(), get$index())
                    throwExecuteMethodRestfulCannotEventSuffixException(actionType, execute);
                }
            }
        }
    }

    protected void throwExecuteMethodRestfulAloneEventSuffixException(Class<?> actionType, ActionExecute execute) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Found the alone event suffix in restful action.");
        br.addItem("Advice");
        br.addElement("Event suffix method needs the corresponding index method.");
        br.addElement("(It assumes that event suffix method is only for supplemental business)");
        br.addElement("For example:");
        br.addElement("  (x): no index");
        br.addElement("    get$sea(ProductsSearchForm form) { // *Bad");
        br.addElement("  (o):");
        br.addElement("    get$index(ProductsSearchForm form) { // Good");
        br.addElement("    get$sea(ProductsSearchForm form) {");
        br.addItem("Action");
        br.addElement(actionType);
        br.addItem("Execute Method");
        br.addElement(execute.toSimpleMethodExp());
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    protected void throwExecuteMethodRestfulCannotEventSuffixException(Class<?> actionType, ActionExecute execute) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Found the event suffix (disallowed) in restful action.");
        br.addItem("Advice");
        br.addElement("You cannot define event suffix in restful action.");
        br.addElement("Or use allowEventSuffix attribute of @RestfulAction.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public JsonResponse<...> get$sea(ProductsSearchForm form) { // *Bad");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public JsonResponse<...> get$index(ProductsSearchForm form) { // Good");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    @RestfulAction(allowEventSuffix=true) // Good");
        br.addElement("    public class ProductsAction extends ... {");
        br.addItem("Action");
        br.addElement(actionType);
        br.addItem("Execute Method");
        br.addElement(execute.toSimpleMethodExp());
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    // ===================================================================================
    //                                                                   Structured Method
    //                                                                   =================
    public void verifyRestfulStructuredMethod(ActionMapping actionMapping, Class<?> actionType) {
        if (!hasRestfulAnnotation(actionType)) {
            return;
        }
        newRestfulStructuredMethodVerifier(actionType, actionMapping.getExecuteList()).verify();
    }

    protected RestfulStructuredMethodVerifier newRestfulStructuredMethodVerifier(Class<?> actionType, List<ActionExecute> executeList) {
        return new RestfulStructuredMethodVerifier(actionType, executeList);
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected boolean hasRestfulAnnotation(Class<?> actionType) {
        return actionType.getAnnotation(RestfulAction.class) != null;
    }

    protected RestfulAction getRestfulAnnotation(Class<?> actionType) {
        return actionType.getAnnotation(RestfulAction.class);
    }
}