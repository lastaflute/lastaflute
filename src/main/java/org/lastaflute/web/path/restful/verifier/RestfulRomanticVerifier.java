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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.lastaflute.web.RestfulAction;
import org.lastaflute.web.exception.ExecuteMethodIllegalDefinitionException;
import org.lastaflute.web.path.restful.analyzer.RestfulComponentAnalyzer;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.config.ActionMapping;
import org.lastaflute.web.ruts.config.analyzer.UrlPatternAnalyzer;

/**
 * @author jflute (2021/05/19 Wednesday at roppongi japanese)
 */
public class RestfulRomanticVerifier {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RestfulComponentAnalyzer restfulComponentAnalyzer = newRestfulComponentAnalyzer();

    protected RestfulComponentAnalyzer newRestfulComponentAnalyzer() {
        return new RestfulComponentAnalyzer();
    }

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
        final RestfulAction restfulAnno = getRestfulAnnotation(actionType).get(); // not null here
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
    //                                                                    Cannot Hyphenate
    //                                                                    ================
    public void verifyRestfulCannotHyphenate(ActionMapping actionMapping, Class<?> actionType) {
        if (!hasRestfulAnnotation(actionType)) {
            return;
        }
        final String[] specifiedHyphenate = getRestfulAnnotation(actionType).get().hyphenate();
        if (specifiedHyphenate.length == 0) {
            return;
        }
        // split loop to readable because of deep logic of linkage
        doVerifyRestfulHyphenateFormat(actionType, specifiedHyphenate);
        doVerifyRestfulHyphenateLinkage(actionType, specifiedHyphenate);
    }

    // -----------------------------------------------------
    //                                Hyphenated Name Format
    //                                ----------------------
    protected void doVerifyRestfulHyphenateFormat(Class<?> actionType, String[] specifiedHyphenate) { // for test-easy
        for (String hyphenatedName : Arrays.asList(specifiedHyphenate)) {
            if (!isHyphenatedNameFormatGood(hyphenatedName)) { // bad
                throwExecuteMethodRestfulHyphenateFormatBadException(actionType, specifiedHyphenate, hyphenatedName);
            }
        }
    }

    protected boolean isHyphenatedNameFormatGood(String hyphenatedName) {
        return hyphenatedName.contains("-") // e.g. not sea
                && !hyphenatedName.startsWith("-") // e.g. not -sea
                && !hyphenatedName.endsWith("-") // e.g. not sea-
                && !hyphenatedName.contains("--") // e.g. not sea--land
                && !hyphenatedName.contains("/") // e.g. not sea/land
                && !Srl.isUpperCaseAny(hyphenatedName) // e.g. not Sea-Land
                && Srl.isAlphabetNumberHarfAllOr(hyphenatedName, '-') // e.g. not sea$land_piari
        ;
    }

    protected void throwExecuteMethodRestfulHyphenateFormatBadException(Class<?> actionType, String[] specifiedHyphenate,
            String hyphenatedName) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Found the bad formatted hyphenated name in restful action.");
        br.addItem("Advice");
        br.addElement("You cannot define bad format at 'hyphenate' attribute in restful action.");
        br.addElement("For example:");
        br.addElement("  (x): 'sea'");
        br.addElement("  (x): '-sea'");
        br.addElement("  (x): 'sea-'");
        br.addElement("  (x): 'sea--land'");
        br.addElement("  (x): 'sea/land'");
        br.addElement("  (x): 'Sea-Land'");
        br.addElement("  (x): 'sea$land-piari'");
        br.addElement("  (x): 'sea_land-piari'");
        br.addElement("  (o): 'sea-land'");
        br.addElement("  (o): 'sea-land-piari'");
        br.addItem("Action");
        br.addElement(actionType);
        br.addItem("Specified Hyphenate");
        final String hyphenateDefExp = Arrays.asList(specifiedHyphenate).stream().map(hyp -> {
            return Srl.quoteDouble(hyp);
        }).collect(Collectors.joining(", "));
        br.addElement(hyphenateDefExp);
        br.addItem("Bad Formatted Hyphenated Name");
        br.addElement(hyphenatedName);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    // -----------------------------------------------------
    //                               Hyphenated Name Linkage
    //                               -----------------------
    protected void doVerifyRestfulHyphenateLinkage(Class<?> actionType, String[] specifiedHyphenate) { // for test-easy
        final List<String> businessElementList = restfulComponentAnalyzer.extractActionBusinessElementList(actionType);
        final List<Integer> alreadyFoundIndexList = new ArrayList<>();
        for (String hyphenatedName : Arrays.asList(specifiedHyphenate)) {
            final List<Integer> unmodifiableFoundIndexList = Collections.unmodifiableList(alreadyFoundIndexList); // for deep logic
            final HyphenateNameLinkageResult linkageResult =
                    judgeHyphenatedNameLinkage(businessElementList, hyphenatedName, unmodifiableFoundIndexList);
            if (!linkageResult.isLinkageFound()) {
                throwExecuteMethodRestfulHyphenateLinkageNotFoundException(actionType, specifiedHyphenate, hyphenatedName);
            }
            alreadyFoundIndexList.addAll(linkageResult.getFoundIndexList()); // not duplicated
        }
    }

    protected HyphenateNameLinkageResult judgeHyphenatedNameLinkage(List<String> businessElementList // read-only
            , String hyphenatedName // e.g. "ballet-dancers"
            , List<Integer> alreadyFoundIndexList) { // read-only
        final List<String> hyphenatedElementList = Srl.splitList(hyphenatedName, "-");
        if (hyphenatedElementList.size() <= 1) { // no way, already checked here, just in case because of next depp logic
            throw new IllegalArgumentException("The hyphenatedElementList should have two or more elements: " + hyphenatedElementList);
        }
        int skipIndex = -1;
        final List<Integer> certainFoundIndexList = new ArrayList<>(); // has only certain elements (not cleared)
        final List<Integer> workingFoundIndexList = new ArrayList<>(); // added per found element (and cleared if retry)
        while (true) {
            for (String hyphenatedElement : hyphenatedElementList) { // always two or more loop
                if (workingFoundIndexList.isEmpty()) { // first
                    final int firstIndex = indexOfListElement(businessElementList, hyphenatedElement, skipIndex);
                    if (firstIndex <= -1) { // ending here
                        final boolean linkageFound = !certainFoundIndexList.isEmpty();
                        return new HyphenateNameLinkageResult(linkageFound, certainFoundIndexList);
                    }
                    if (alreadyFoundIndexList.contains(firstIndex)) {
                        skipIndex = firstIndex; // to skip first element in retry
                        break; // retry
                    }
                    workingFoundIndexList.add(firstIndex);
                } else { // second or more
                    final Integer previousIndex = workingFoundIndexList.get(workingFoundIndexList.size() - 1);
                    final int nextIndex = previousIndex + 1;
                    if (!alreadyFoundIndexList.contains(nextIndex)) { // next is not other's 
                        if (businessElementList.size() > nextIndex) { // exists next element
                            final String nextResourceElement = businessElementList.get(nextIndex);
                            if (hyphenatedElement.equals(nextResourceElement)) {
                                workingFoundIndexList.add(nextIndex);
                                continue;
                            }
                        }
                    }
                    // not found (second or more element is differnet)
                    skipIndex = workingFoundIndexList.get(0); // to skip first element in retry
                    workingFoundIndexList.clear();
                    break; // retry
                }
            }
            if (!workingFoundIndexList.isEmpty()) { // means found
                certainFoundIndexList.addAll(workingFoundIndexList);
                skipIndex = workingFoundIndexList.get(workingFoundIndexList.size() - 1); // to search next same resource name
                workingFoundIndexList.clear();
            }
            // to next loop for retry
        }
    }

    protected int indexOfListElement(List<String> actionNameElementList, String hyphenatedElement, int skipIndex) {
        int index = 0;
        for (String nameElement : actionNameElementList) {
            if (skipIndex < index) {
                if (hyphenatedElement.equals(nameElement)) {
                    return index;
                }
            }
            ++index;
        }
        return -1;
    }

    protected static class HyphenateNameLinkageResult {

        protected final boolean linkageFound;
        protected final List<Integer> foundIndexList;

        public HyphenateNameLinkageResult(boolean linkageFound, List<Integer> foundIndexList) {
            this.linkageFound = linkageFound;
            this.foundIndexList = foundIndexList;
        }

        public boolean isLinkageFound() {
            return linkageFound;
        }

        public List<Integer> getFoundIndexList() {
            return foundIndexList;
        }
    }

    protected void throwExecuteMethodRestfulHyphenateLinkageNotFoundException(Class<?> actionType, String[] specifiedHyphenate,
            String hyphenatedName) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Found the unknown hyphenated name in restful action.");
        br.addItem("Advice");
        br.addElement("The hyphenated name should have linkage with the action name.");
        br.addElement("For example: BalletDancersAction");
        br.addElement("  (x): 'bal-let'");
        br.addElement("  (x): 'jazz-dancers'");
        br.addElement("  (x): 'dancers-action'");
        br.addElement("  (o): 'ballet-dancers'");
        br.addItem("Action");
        br.addElement(actionType);
        br.addItem("Specified Hyphenate");
        final String hyphenateDefExp = Arrays.asList(specifiedHyphenate).stream().map(hyp -> {
            return Srl.quoteDouble(hyp);
        }).collect(Collectors.joining(", "));
        br.addElement(hyphenateDefExp);
        br.addItem("No Linkage Hyphenated Name");
        br.addElement(hyphenatedName);
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
        return restfulComponentAnalyzer.hasRestfulAnnotation(actionType);
    }

    protected OptionalThing<RestfulAction> getRestfulAnnotation(Class<?> actionType) {
        return restfulComponentAnalyzer.getRestfulAnnotation(actionType);
    }
}