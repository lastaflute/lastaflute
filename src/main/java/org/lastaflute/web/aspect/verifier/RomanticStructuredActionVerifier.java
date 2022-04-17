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
package org.lastaflute.web.aspect.verifier;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.util.Srl;
import org.lastaflute.di.core.ComponentDef;
import org.lastaflute.web.exception.ActionPackageHasUpperCaseException;
import org.lastaflute.web.exception.ExecuteMethodIllegalDefinitionException;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.config.ActionMapping;
import org.lastaflute.web.ruts.config.restful.RestfulGetPairHandler;
import org.lastaflute.web.util.LaActionExecuteUtil;

/**
 * @author jflute
 * @since 1.2.1 (2021/06/14 Monday)
 */
public class RomanticStructuredActionVerifier {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RestfulGetPairHandler restfulGetPairHandler = newRestfulGetPairHandler();

    protected RestfulGetPairHandler newRestfulGetPairHandler() {
        return new RestfulGetPairHandler();
    }

    // ===================================================================================
    //                                                                  Package Convention
    //                                                                  ==================
    public void verifyPackageConvention(ComponentDef actionDef, String actionName) {
        if (actionName.contains("_")) {
            final String packageExp = Srl.substringLastFront(actionName, "_");
            if (containsNotAllowedCharacterAsActionPath(packageExp)) { // e.g. seaLand_seaLandAction
                throwActionPackageHasUpperCaseException(actionDef, actionName);
            }
        }
    }

    protected boolean containsNotAllowedCharacterAsActionPath(String packageExp) {
        return Srl.isUpperCaseAny(packageExp);
    }

    protected void throwActionPackageHasUpperCaseException(ComponentDef actionDef, String actionName) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The package name of the action has upper case.");
        br.addItem("Advice");
        br.addElement("Cannot use upper case in action package.");
        br.addElement("Lower cases are only allowed like this:");
        br.addElement("  (x):");
        br.addElement("    seaLand.SeaLandAction // *Bad: sea[L]and");
        br.addElement("  (o):");
        br.addElement("    sealand.SealandAction  => /sealand/");
        br.addElement("    sea.SeaLandAction      => /sea/land/");
        br.addElement("    sea.land.SeaLandAction => /sea/land/");
        br.addElement("    SeaLandAction          => /sea/land/");
        br.addItem("Illegal Action");
        br.addElement(actionName);
        br.addItem("Component Def");
        br.addElement(actionDef);
        final String msg = br.buildExceptionMessage();
        throw new ActionPackageHasUpperCaseException(msg);
    }

    // ===================================================================================
    //                                                                    Overload Execute
    //                                                                    ================
    public void verifyOverloadExecuteMethod(ActionMapping actionMapping, Class<?> actionType, ActionExecute currentExecute) {
        final String methodName = currentExecute.getExecuteMethod().getName(); // plain mapping name or containing HTTP method
        final List<ActionExecute> existingList = actionMapping.searchByMethodName(methodName);
        if (existingList.isEmpty() || isOverloadAllowedExecuteSet(currentExecute, existingList)) {
            return;
        }
        throwOverloadMethodCannotDefinedException(actionType, currentExecute, existingList);
    }

    protected boolean isOverloadAllowedExecuteSet(ActionExecute currentExecute, List<ActionExecute> existingList) {
        // pair of get$index([no-param] or Form) and get$index(ID (not optional)) is allowed
        // more detail check is verified by structured method verifier so simple here
        return isRestfulGetPairExecute(currentExecute, existingList);
    }

    protected void throwOverloadMethodCannotDefinedException(Class<?> actionType, ActionExecute currentExecute,
            List<ActionExecute> existingList) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Cannot define overload methods of action execute.");
        br.addItem("Advice");
        br.addElement("Same-name different-parameter methods cannot be defined as execute method.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse index() {");
        br.addElement("    public HtmlResponse index(String sea) { // *Bad");
        br.addElement("  (o):");
        br.addElement("    public HtmlResponse index() {");
        br.addElement("    public HtmlResponse land(String sea) { // Good");
        if (currentExecute.getRestfulHttpMethod().isPresent()) { // tell about RESTful GET pair
            br.addElement("");
            br.addElement("While only RESTful GET pair methods are allowed.");
            br.addElement(" o List GET: get$index(Form) e.g. /products/ (short parameter)");
            br.addElement(" o Single GET: get$index(ID) e.g. /products/1/ (full parameter)");
            br.addElement("");
            br.addElement("For example: ProductsAction /products/[1]/");
            br.addElement("  (x):");
            br.addElement("    get$index(no-param or Form) {");
            br.addElement("    get$index(OptionalThing<Integer> productId) { // *Bad: cannot use optional");
            br.addElement("  (x):");
            br.addElement("    get$index(no-param or Form) {");
            br.addElement("    get$index(Integer productId, Integer otherId) { // *Bad: too many parameter");
            br.addElement("  (x):");
            br.addElement("    get$index(Integer productId (+ Form)) { // *Bad: unneeded parameter (if list)");
            br.addElement("    get$index(Integer productId) {");
            br.addElement("  (o):");
            br.addElement("    get$index(no-param or Form) {");
            br.addElement("    get$index(Integer productId) {");
            br.addElement("");
            br.addElement("For example: ProductsPurchasesAction /products/1/purchases/[2]/");
            br.addElement("  (x):");
            br.addElement("    get$index(Integer productId (+ Form)) {");
            br.addElement("    get$index(Long productId, Integer purchaseId) { // *Bad: different type");
            br.addElement("  (o):");
            br.addElement("    get$index(Integer productId (+ Form)) {");
            br.addElement("    get$index(Integer productId, Integer purchaseId) {");
        }
        br.addItem("Action");
        br.addElement(actionType);
        br.addItem("Overload Methods");
        br.addElement(currentExecute.toSimpleMethodExp());
        for (ActionExecute existingExecute : existingList) {
            br.addElement(existingExecute.toSimpleMethodExp());
        }
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    // -----------------------------------------------------
    //                                      RESTful GET Pair
    //                                      ----------------
    protected boolean isRestfulGetPairExecute(ActionExecute currentExecute, List<ActionExecute> existingList) {
        if (existingList.size() != 1) { // basically no way here, just in case
            return false; // in case multiple, means not pair (then overload exception)
        }
        final ActionExecute existingExecute = existingList.get(0);
        return restfulGetPairHandler.determineRestfulGetPairExecute(currentExecute, existingExecute);
    }

    // ===================================================================================
    //                                                                  Verious Definition
    //                                                                  ==================
    // -----------------------------------------------------
    //                                           Method Size
    //                                           -----------
    public void verifyExecuteMethodSize(ActionMapping actionMapping, Class<?> actionType) {
        if (actionMapping.getExecuteList().isEmpty()) {
            throwExecuteMethodNotFoundInActionException(actionType);
        }
    }

    protected void throwExecuteMethodNotFoundInActionException(Class<?> actionType) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found execute method in the action class.");
        br.addItem("Advice");
        br.addElement("Action class needs at least one method.");
        br.addElement("Confirm the @Execute annotation of your execute method.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse index() {");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse index() {");
        br.addElement("    }");
        br.addItem("Action");
        br.addElement(actionType);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    // -----------------------------------------------------
    //                                      Shadowing Method
    //                                      ----------------
    public void verifyExecuteMethodNotShadowingOthers(ActionMapping actionMapping, Class<?> actionType) {
        final Collection<ActionExecute> executeList = actionMapping.getExecuteList();
        executeList.stream().filter(execute -> {
            // if urlPattern is specified, cannot determine shadowing so skip checking
            return execute.isIndexMethod() // e.g. index() or get$index() or ...
                    && execute.getPathParamArgs().isPresent() // no shadowing if no parameter
                    && !execute.getPreparedUrlPattern().isSpecified();// cannot determine shadowing if specified
        }).forEach(index -> {
            if (index.getPathParamArgs().get().isNumberTypeParameter(0)) { // Number has original pattern
                return;
            }
            // index may have String as the first argument without urlPattern here
            // e.g.
            //  index(String sea) or get$index(String sea) or index(String sea, Integer ...)
            //  or index(LocalDate sea) or index(CDef.MemberStatus sea) or ...
            executeList.stream().filter(execute -> !execute.isIndexMethod()).filter(named -> { // named execute
                return !isDifferentRestfulHttpMethod(index, named); // except e.g. index:GET, named:POST
            }).forEach(named -> { // e.g. dockside() or dockside(String hangar) or ...
                if (isShadowingExecuteMethod(index, named)) {
                    throwExecuteMethodShadowedByOtherExecuteMethodException(index, named);
                }
            });
        });
    }

    protected boolean isShadowingExecuteMethod(ActionExecute index, ActionExecute named) {
        // index() has one or more parameter(s) here
        // and the first argument is not number and no urlPattern
        //  e.g.
        //   index(String sea)
        //   index(String sea, Integer land) 
        // and should return true if 100% shadowing only
        if (named.getPreparedUrlPattern().isSpecified()) {
            return false; // #giveup cannot determine shadowing for urlPattern
        }
        // both no urlPattern here
        if (!named.getPathParamArgs().isPresent()) { // no parameter e.g. dockside()
            return true; // shadowing if no urlPattern
        }
        // both has any parameters
        return false; // #giveup also cannot determine shadowing for optional parameters
    }

    protected boolean isDifferentRestfulHttpMethod(ActionExecute index, ActionExecute named) {
        return index.getRestfulHttpMethod().isPresent() && named.getRestfulHttpMethod().isPresent() // both has it
                && !index.getRestfulHttpMethod().get().equals(named.getRestfulHttpMethod().get()); // different
    }

    protected void throwExecuteMethodShadowedByOtherExecuteMethodException(final ActionExecute index, final ActionExecute execute) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Shadowed the execute method by the other execute method.");
        br.addItem("Advice");
        br.addElement("Remove your the execute method shadowing others.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse index(String name) {");
        br.addElement("    }");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse sea() {");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse index(Integer id) {");
        br.addElement("    }");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse sea() {");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse index() {");
        br.addElement("    }");
        br.addElement("    @Execute");
        br.addElement("    public HtmlResponse sea() {");
        br.addElement("    }");
        br.addItem("Index Execute");
        br.addElement(index);
        br.addItem("Named Execute");
        br.addElement(execute);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }

    // -----------------------------------------------------
    //                                        Concrete Class
    //                                        --------------
    public void verifyExecuteMethodDefinedInConcreteClassOnly(ActionMapping actionMapping, Class<?> actionType) {
        for (Class<?> clazz = actionType.getSuperclass(); !Object.class.equals(clazz); clazz = clazz.getSuperclass()) {
            if (clazz == null) { // just in case
                break;
            }
            for (Method declaredMethod : clazz.getDeclaredMethods()) {
                if (isExecuteMethod(declaredMethod)) {
                    throwExecuteMethodAtSuperClassCannotBeDefinedException(clazz, declaredMethod);
                }
            }
        }
    }

    protected boolean isExecuteMethod(Method actionMethod) {
        return LaActionExecuteUtil.isExecuteMethod(actionMethod);
    }

    protected void throwExecuteMethodAtSuperClassCannotBeDefinedException(Class<?> clazz, Method declaredMethod) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Cannot define execute method at super class.");
        br.addItem("Advice");
        br.addElement("Execute method should be defined at concrete class.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public abstract class SeaBaseAction ... {");
        br.addElement("        @Execute");
        br.addElement("        public HtmlResponse index() {");
        br.addElement("        }");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public class SeaAction ... {");
        br.addElement("        @Execute");
        br.addElement("        public HtmlResponse index() {");
        br.addElement("        }");
        br.addElement("    }");
        br.addItem("Super Class");
        br.addElement(clazz);
        br.addItem("Illegal Execute");
        br.addElement(declaredMethod);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodIllegalDefinitionException(msg);
    }
}
