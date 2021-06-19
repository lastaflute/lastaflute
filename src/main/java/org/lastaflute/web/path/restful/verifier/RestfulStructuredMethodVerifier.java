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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.lastaflute.di.util.tiger.LdiGenericUtil;
import org.lastaflute.web.exception.ExecuteMethodIllegalDefinitionException;
import org.lastaflute.web.path.restful.analyzer.RestfulComponentAnalyzer;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.config.ActionFormMeta;

/**
 * @author jflute (2021/06/13 Sunday)
 */
public class RestfulStructuredMethodVerifier {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                           Action Info
    //                                           -----------
    protected final Class<?> actionType; // not null, is RESTful action (alreadh checked)
    protected final List<ActionExecute> executeList; // not null

    // -----------------------------------------------------
    //                                        Basic Analyzed
    //                                        --------------
    protected final List<String> resourceNameList; // not null, not empty (having at least one element)

    // may have different GET methods
    // extract certain list from possible list after analyzing GET method 
    protected final List<ActionExecute> possibleGetExecuteList; // not null
    protected final List<ActionExecute> possibleQueryableExecuteList; // not null
    protected final List<ActionExecute> possibleBodyableExecuteList; // not null
    protected final List<ActionExecute> possibleFullParameterExecuteList; // not null
    protected final List<ActionExecute> possibleShortParameterExecuteList; // not null

    // -----------------------------------------------------
    //                                              Verified
    //                                              --------
    protected OptionalThing<ActionExecute> listGetExecute = OptionalThing.empty(); // not null
    protected OptionalThing<ActionExecute> singleGetExecute = OptionalThing.empty(); // not null

    // -----------------------------------------------------
    //                                              Analyzer
    //                                              --------
    protected final RestfulComponentAnalyzer restfulComponentAnalyzer = newRestfulComponentAnalyzer();

    protected RestfulComponentAnalyzer newRestfulComponentAnalyzer() {
        return new RestfulComponentAnalyzer();
    }

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public RestfulStructuredMethodVerifier(Class<?> actionType, List<ActionExecute> executeList) {
        if (!restfulComponentAnalyzer.hasRestfulAnnotation(actionType)) {
            throw new IllegalArgumentException("The action type should be RESTful action.");
        }
        this.actionType = actionType;
        this.executeList = executeList;

        this.resourceNameList = restfulComponentAnalyzer.deriveResourceNameListByActionType(actionType);
        this.possibleGetExecuteList = searchByHttpMethod(executeList, "get");
        this.possibleQueryableExecuteList = searchByHttpMethod(executeList, "get", "delete");
        this.possibleBodyableExecuteList = searchByHttpMethod(executeList, "post", "put", "patch");
        this.possibleFullParameterExecuteList = searchByHttpMethod(executeList, "get", "put", "patch", "delete");
        this.possibleShortParameterExecuteList = searchByHttpMethod(executeList, "get", "post");
    }

    protected List<ActionExecute> searchByHttpMethod(List<ActionExecute> executeList, String... httpMethod) {
        return executeList.stream()
                .filter(ex -> ex.getRestfulHttpMethod().isPresent()) // always true here, but just in case
                .filter(ex -> Srl.equalsPlain(ex.getRestfulHttpMethod().get(), httpMethod))
                .filter(ex -> ex.isIndexMethod())
                .collect(Collectors.toList());
    }

    // ===================================================================================
    //                                                                              Verify
    //                                                                              ======
    // #hope jflute pure analyzing logic is moved to under config package (2021/06/13)
    public void verify() {
        // determine List GET and Single GET here
        analyzeGetExecute(); // should be first

        // Form and Body
        analyzeQueryableExecuute();
        analyzeBodyableExecuute();

        // Path Parameter
        analyzeFullParameterExecute();
        analyzeShortParameterExecute();
        analyzeEventSuffixParameterExecute(); // should be after full/short parameter
    }

    // ===================================================================================
    //                                                                         GET Execute
    //                                                                         ===========
    protected void analyzeGetExecute() {
        // several cases may be already checked in romantic customizer
        // (but no problem, this class is independent)
        if (possibleGetExecuteList.size() >= 3) {
            throwRestfulStructureTooManyGetMethodException(possibleGetExecuteList);
        } else if (possibleGetExecuteList.size() == 2) {
            final ActionExecute first = possibleGetExecuteList.get(0);
            final ActionExecute second = possibleGetExecuteList.get(1);

            final int resourceCount = resourceNameList.size();
            final Integer firstParamCount = countPathParameter(first);
            final Integer secondParamCount = countPathParameter(second);

            if (firstParamCount == resourceCount - 1) { // first is list GET
                if (secondParamCount != resourceCount) { // second should be single GET but...?
                    throwRestfulStructureDifferentPathParameterCountException("List GET?", first, "Single GET?", second);
                }
                // count is correct here
                // maybe also single GET needs form... so no check
                //if (second.getFormMeta().isPresent()) { // single GET should not have form but...?
                listGetExecute = OptionalThing.of(first);
                singleGetExecute = OptionalThing.of(second);
            } else if (secondParamCount == resourceCount - 1) { // second is list GET
                if (firstParamCount != resourceCount) { // first should be single GET but...?
                    throwRestfulStructureDifferentPathParameterCountException("Single GET?", first, "List GET?", second);
                }
                // count is correct here
                // maybe also single GET needs form... so no check
                //if (first.getFormMeta().isPresent()) { // single GET should not have form but...?
                listGetExecute = OptionalThing.of(second);
                singleGetExecute = OptionalThing.of(first);
            } else { // both unexpected parameter count
                throwRestfulStructureDifferentPathParameterCountException("Unknown GET1", first, "Unknown GET2", second);
            }
        } else if (possibleGetExecuteList.size() == 1) {
            final ActionExecute onlyOne = possibleGetExecuteList.get(0);
            final int resourceCount = resourceNameList.size();
            final Integer onlyOneParamCount = countPathParameter(onlyOne);
            if (onlyOneParamCount == resourceCount - 1) { // list GET
                listGetExecute = OptionalThing.of(onlyOne);
            } else if (onlyOneParamCount == resourceCount) { // single GET
                // maybe also single GET needs form... so no check
                //if (onlyOne.getFormMeta().isPresent()) { // single GET should not have form but...?
                singleGetExecute = OptionalThing.of(onlyOne);
            } else { // unexpected parameter count
                throwRestfulStructureDifferentPathParameterCountException("Unknown GET1", onlyOne);
            }
        }
        singleGetExecute.ifPresent(execute -> {
            final Method executeMethod = execute.getExecuteMethod();
            if (JsonResponse.class.isAssignableFrom(executeMethod.getReturnType())) {
                final Type genericReturnType = executeMethod.getGenericReturnType(); // not null
                final Class<?> genericFirstClass = LdiGenericUtil.getGenericFirstClass(genericReturnType);
                if (genericFirstClass != null) {
                    if (List.class.isAssignableFrom(genericFirstClass)) { // single GET but list
                        throwRestfulStructureSingleGetReturningListException(execute);
                    }
                }
            }
        });
        // return of list GET cannot be checked because it may be wrapped
    }

    protected void throwRestfulStructureTooManyGetMethodException(List<ActionExecute> executeList) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Too many GET methods of RESTful action.");
        br.addItem("Advice");
        br.addElement("Only List GET and Single GET are allowed.");
        br.addElement("For example: ProductsAction");
        br.addElement("  (x):");
        br.addElement("    get$index(...Form form) { // List GET");
        br.addElement("    get$index(Integer productId) { // Single GET");
        br.addElement("    get$index(String productCode) { // *Bad");
        br.addElement("  (o):");
        br.addElement("    get$index(...Form form) { // List GET");
        br.addElement("    get$index(Integer productId) { // Single GET");
        br.addItem("RESTful Action");
        br.addElement(actionType);
        br.addItem("GET Methods");
        for (ActionExecute execute : executeList) {
            br.addElement(execute.toSimpleMethodExp());
        }
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    protected void throwRestfulStructureSingleGetReturningListException(ActionExecute execute) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Single GET method of RESTful action should not return list.");
        br.addItem("Advice");
        br.addElement("Only List GET is allowed to return list.");
        br.addElement("For example: ProductsAction");
        br.addElement("  (x):");
        br.addElement("    JsonResponse<List<...Result>> get$index(Integer productId) { // *Bad");
        br.addElement("  (o):");
        br.addElement("    JsonResponse<List<...>> get$index(...Form form) { // as List GET");
        br.addElement("    JsonResponse<...Result> get$index(Integer productId) { // as Single GET");
        br.addItem("RESTful Action");
        br.addElement(actionType);
        br.addItem("Single GET?");
        br.addElement(execute.toSimpleMethodExp());
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    // ===================================================================================
    //                                                                       Form and Body
    //                                                                       =============
    // -----------------------------------------------------
    //                                             Queryable
    //                                             ---------
    protected void analyzeQueryableExecuute() {
        possibleQueryableExecuteList.stream().filter(ex -> ex.getFormMeta().isPresent()).forEach(execute -> {
            final ActionFormMeta formMeta = execute.getFormMeta().get();
            if (formMeta.isJsonBodyMapping()) {
                throwRestfulStructureQueryableButBodyException(execute);
            }
        });
    }

    protected void throwRestfulStructureQueryableButBodyException(ActionExecute execute) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Queryable methods of RESTful action should not use JSON body.");
        br.addItem("Advice");
        br.addElement("Queryable methods cannot use JSON body, use query form.");
        br.addElement("For example: ProductsAction");
        br.addElement("  (x):");
        br.addElement("    get$index(...Body body) { // *Bad");
        br.addElement("    delete$index(...Body body) { // *Bad");
        br.addElement("  (o):");
        br.addElement("    get$index(...Form form) { // Good");
        br.addElement("    delete$index(...Form form) { // Good");
        br.addItem("RESTful Action");
        br.addElement(actionType);
        br.addItem("Queryable Method?");
        br.addElement(execute.toSimpleMethodExp());
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    // -----------------------------------------------------
    //                                              Bodyable
    //                                              --------
    protected void analyzeBodyableExecuute() {
        possibleBodyableExecuteList.stream()
                .filter(ex -> ex.isApiExecute()) // except HTML response, which can use form
                .filter(ex -> ex.getFormMeta().isPresent())
                .forEach(execute -> {
                    final ActionFormMeta formMeta = execute.getFormMeta().get();
                    if (!formMeta.isJsonBodyMapping()) {
                        throwRestfulStructureBodyableButBodyException(execute);
                    }
                });
    }

    protected void throwRestfulStructureBodyableButBodyException(ActionExecute execute) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("JSON Body-able methods of RESTful action should not use query form.");
        br.addItem("Advice");
        br.addElement("JSON Body-able methods cannot use query form, use JSON body.");
        br.addElement("For example: ProductsAction");
        br.addElement("  (x):");
        br.addElement("    post$index(...Form form) { // *Bad");
        br.addElement("    put$index(Integer productId, ...Form form) { // *Bad");
        br.addElement("    patch$index(Integer productId, ...Form form) { // *Bad");
        br.addElement("  (o):");
        br.addElement("    post$index(...Body body) { // Good");
        br.addElement("    put$index(Integer productId, ...Body body) { // Good");
        br.addElement("    patch$index(Integer productId, ...Body body) { // Good");
        br.addItem("RESTful Action");
        br.addElement(actionType);
        br.addItem("Body-able Method?");
        br.addElement(execute.toSimpleMethodExp());
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    // ===================================================================================
    //                                                                      Path Parameter
    //                                                                      ==============
    // -----------------------------------------------------
    //                                        Full Parameter
    //                                        --------------
    protected void analyzeFullParameterExecute() {
        final List<ActionExecute> certainList =
                possibleFullParameterExecuteList.stream().filter(ex -> exceptsListGet(ex)).collect(Collectors.toList());
        certainList.forEach(execute -> {
            final Integer pathParameterCount = countPathParameter(execute);
            if (pathParameterCount != resourceNameList.size()) {
                throwRestfulStructureDifferentPathParameterCountException("Should be FullParameter?", execute);
            }
        });
        ActionExecute previous = null;
        for (ActionExecute current : certainList) {
            if (previous != null && !matchesPathParameterType(previous, current)) {
                throwRestfulStructureDifferentPathParameterTypeException(previous, current);
            }
            previous = current;
        }
    }

    // -----------------------------------------------------
    //                                       Short Parameter
    //                                       ---------------
    protected void analyzeShortParameterExecute() {
        final List<ActionExecute> certainList =
                possibleShortParameterExecuteList.stream().filter(ex -> exceptsOneGet(ex)).collect(Collectors.toList());
        certainList.forEach(execute -> {
            final Integer pathParameterCount = countPathParameter(execute);
            if (pathParameterCount != resourceNameList.size() - 1) {
                throwRestfulStructureDifferentPathParameterCountException("Should be ShortParameter?", execute);
            }
        });
        ActionExecute previous = null;
        for (ActionExecute current : certainList) {
            if (previous != null && !matchesPathParameterType(previous, current)) {
                throwRestfulStructureDifferentPathParameterTypeException(previous, current);
            }
            previous = current;
        }
    }

    // -----------------------------------------------------
    //                                          Event Suffix
    //                                          ------------
    protected void analyzeEventSuffixParameterExecute() {
        final List<ActionExecute> eventSuffixList = executeList.stream()
                .filter(ex -> ex.getRestfulHttpMethod().isPresent()) // basically true here, already checked, just in case
                .filter(ex -> !ex.isIndexMethod()) // having event suffix
                .collect(Collectors.toList());
        for (ActionExecute eventSuffixExecute : eventSuffixList) {
            final String httpMethod = eventSuffixExecute.getRestfulHttpMethod().get();
            final String indexName = httpMethod + "$index";

            // parameters of index execute method are already checked here
            // so it treats it as expected definition
            final List<ActionExecute> indexList =
                    executeList.stream().filter(ex -> indexName.equals(ex.getExecuteMethod().getName())).collect(Collectors.toList());

            final int eventSuffixParamCount = countPathParameter(eventSuffixExecute);
            final Optional<ActionExecute> optIndex = indexList.stream().filter(ex -> {
                return eventSuffixParamCount == countPathParameter(ex);
            }).findAny(); // basically zero or one here, because of RESTful definition logic
            if (!optIndex.isPresent()) { // index whose parameter count is same is not found
                throwRestfulStructureDifferentPathParameterCountException("EventSuffix Method", eventSuffixExecute);
            }
            final ActionExecute indexExecute = optIndex.get();
            if (!matchesPathParameterType(indexExecute, eventSuffixExecute)) {
                throwRestfulStructureDifferentPathParameterTypeException(indexExecute, eventSuffixExecute);
            }
        }
    }

    // -----------------------------------------------------
    //                                          Assist Logic
    //                                          ------------
    protected void throwRestfulStructureDifferentPathParameterTypeException(ActionExecute previous, ActionExecute current) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Different path parameter types of RESTful action methods.");
        br.addItem("Advice");
        br.addElement("Path parameter types should be same in one RESTful action.");
        br.addElement("For example: ProductsAction");
        br.addElement("  (x):");
        br.addElement("    get$index(Integer productId) {");
        br.addElement("    put$index(Long productId, ...Body) { // *Bad");
        br.addElement("  (o):");
        br.addElement("    get$index(Integer productId) {");
        br.addElement("    put$index(Integer productId, ...Body) { // Good");
        br.addItem("RESTful Action");
        br.addElement(actionType);
        br.addItem("Different1 Method");
        br.addElement(previous.toSimpleMethodExp());
        br.addItem("Different2 Method");
        br.addElement(current.toSimpleMethodExp());
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    // ===================================================================================
    //                                                             Different PathParameter
    //                                                             =======================
    protected void throwRestfulStructureDifferentPathParameterCountException(String title, ActionExecute execute) {
        throwRestfulStructureDifferentPathParameterCountException(title, execute, null, null);
    }

    protected void throwRestfulStructureDifferentPathParameterCountException(String firstTitle, ActionExecute firstExecute,
            String secondTitle, ActionExecute secondExecute) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Different path parameter count of RESTful action method.");
        br.addItem("Advice");
        br.addElement("Make sure your parameter definition of RESTful action methods.");
        br.addElement(" o Full parameter method: Single GET, PUT, PATCH e.g. /products/1/");
        br.addElement(" o Short parameter method: List GET, DELETE e.g. /products/");
        br.addElement("");
        br.addElement("For example: ProductsAction /products/[1]/");
        br.addElement("  (x):");
        br.addElement("    get$index(Integer productId, ...Form form) { // *Bad (if list)");
        br.addElement("  (x):");
        br.addElement("    get$index() { // *Bad (if single)");
        br.addElement("  (x):");
        br.addElement("    post$index(Integer productId, ...Body body) { // *Bad");
        br.addElement("  (x):");
        br.addElement("    put$index(...Body body) { // *Bad");
        br.addElement("  (o):");
        br.addElement("    get$index(...Form form) { // list");
        br.addElement("    get$index(Integer productId) { // single");
        br.addElement("    post$index(...Body body) {");
        br.addElement("    put$index(Integer productId, ...Body body) {");
        br.addElement("");
        br.addElement("For example: ProductsPurchasesAction /products/1/purchases/[2]");
        br.addElement("  (x):");
        br.addElement("    get$index(Integer productId, Integer purchaseId, ...Form form) { // *Bad (if list)");
        br.addElement("  (x):");
        br.addElement("    get$index(Integer productId) { // *Bad (if single)");
        br.addElement("  (x):");
        br.addElement("    post$index(Integer productId, Integer purchaseId, ...Body body) { // *Bad");
        br.addElement("  (x):");
        br.addElement("    put$index(Integer productId, ...Body body) { // *Bad");
        br.addElement("  (o):");
        br.addElement("    get$index(Integer productId, ...Form form) { // list");
        br.addElement("    get$index(Integer productId, Integer purchaseId) { // single");
        br.addElement("    post$index(Integer productId, ...Body body) {");
        br.addElement("    put$index(Integer productId, Integer purchaseId, ...Body body) {");
        br.addItem("RESTful Action");
        br.addElement(actionType.getSimpleName());
        restfulComponentAnalyzer.getRestfulAnnotation(actionType).ifPresent(restfulAnno -> {
            // always here but just in case (because in exception handling)
            final String[] hyphenate = restfulAnno.hyphenate();
            if (hyphenate.length >= 1) {
                br.addElement(" hyphenate: " + Arrays.asList(hyphenate));
            }
        });
        br.addElement(" resource names: " + resourceNameList);
        br.addElement(" full parameter count: " + resourceNameList.size());
        br.addElement(" short parameter count: " + (resourceNameList.size() - 1));
        br.addItem(firstTitle);
        br.addElement(firstExecute.toSimpleMethodExp());
        if (secondTitle != null && secondExecute != null) {
            br.addItem(secondTitle);
            br.addElement(secondExecute.toSimpleMethodExp());
        }
        br.addItem("Supplement");
        br.addElement("This verifier judges by parameter definition.");
        br.addElement("(not see return type because of free expression for application)");
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    // -----------------------------------------------------
    //                                            GET Method
    //                                            ----------
    protected boolean exceptsListGet(ActionExecute ex) {
        return listGetExecute.map(one -> !one.equals(ex)).orElse(true);
    }

    protected boolean exceptsOneGet(ActionExecute ex) {
        return singleGetExecute.map(one -> !one.equals(ex)).orElse(true);
    }

    // -----------------------------------------------------
    //                                        Path Parameter
    //                                        --------------
    protected int countPathParameter(ActionExecute execute) {
        return execute.getPathParamArgs().map(args -> args.getPathParamTypeList().size()).orElse(0);
    }

    protected boolean matchesPathParameterType(ActionExecute previous, ActionExecute current) {
        return extractPathParamTypeList(previous).equals(extractPathParamTypeList(current));
    }

    protected List<Class<?>> extractPathParamTypeList(ActionExecute execute) {
        return execute.getPathParamArgs().map(args -> args.getPathParamTypeList()).orElseGet(() -> Collections.emptyList());
    }
}