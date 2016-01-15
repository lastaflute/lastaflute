/*
 * Copyright 2015-2016 the original author or authors.
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
package org.lastaflute.web.ruts.config;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.servlet.http.HttpServletRequest;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.lastaflute.db.jta.stage.TransactionGenre;
import org.lastaflute.web.api.ApiAction;
import org.lastaflute.web.exception.ActionFormNotFoundException;
import org.lastaflute.web.exception.UrlParamArgsNotFoundException;
import org.lastaflute.web.response.ApiResponse;
import org.lastaflute.web.ruts.VirtualForm;
import org.lastaflute.web.ruts.config.analyzer.ExecuteArgAnalyzer;
import org.lastaflute.web.ruts.config.analyzer.ExecuteArgAnalyzer.ExecuteArgBox;
import org.lastaflute.web.ruts.config.analyzer.UrlPatternAnalyzer;
import org.lastaflute.web.ruts.config.analyzer.UrlPatternAnalyzer.UrlPatternChosenBox;
import org.lastaflute.web.ruts.config.analyzer.UrlPatternAnalyzer.UrlPatternRegexpBox;
import org.lastaflute.web.ruts.config.checker.ExecuteMethodChecker;
import org.lastaflute.web.util.LaActionExecuteUtil;

/**
 * @author modified by jflute (originated in Sessar)
 */
public class ActionExecute implements Serializable {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = 1L;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ActionMapping actionMapping; // not null
    protected final Method executeMethod; // not null
    protected final boolean indexMethod;
    protected final TransactionGenre transactionGenre; // not null
    protected final boolean suppressValidatorCallCheck;
    protected final OptionalThing<Integer> sqlExecutionCountLimit;

    // -----------------------------------------------------
    //                                     Defined Parameter
    //                                     -----------------
    protected final List<Class<?>> urlParamTypeList; // not null, read-only e.g. Integer.class, String.class
    protected final Map<Integer, Class<?>> optionalGenericTypeMap; // not null, read-only, key is argument index
    protected final OptionalThing<UrlParamArgs> urlParamArgs;
    protected final OptionalThing<ActionFormMeta> formMeta;

    // -----------------------------------------------------
    //                                           URL Pattern
    //                                           -----------
    protected final PreparedUrlPattern preparedUrlPattern; // not null

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    /**
     * @param actionMapping The mapping of action related to this execute. (NotNull)
     * @param executeMethod The execute method of action. (NotNull)
     * @param executeOption The user option of action execute. (NotNull)
     */
    public ActionExecute(ActionMapping actionMapping, Method executeMethod, ExecuteOption executeOption) {
        this.actionMapping = actionMapping;
        this.executeMethod = executeMethod;
        this.indexMethod = executeMethod.getName().equals("index");
        this.transactionGenre = chooseTransactionGenre(executeOption);
        this.suppressValidatorCallCheck = executeOption.isSuppressValidatorCallCheck();
        this.sqlExecutionCountLimit = createOptionalSqlExecutionCountLimit(executeOption);

        // defined parameter (needed in URL pattern analyzing)
        final ExecuteArgAnalyzer executeArgAnalyzer = newExecuteArgAnalyzer();
        final ExecuteArgBox executeArgBox = newExecuteArgBox();
        executeArgAnalyzer.analyzeExecuteArg(executeMethod, executeArgBox);
        this.urlParamTypeList = executeArgBox.getUrlParamTypeList(); // not null, empty allowed
        this.optionalGenericTypeMap = executeArgBox.getOptionalGenericTypeMap();
        this.formMeta = analyzeFormMeta(executeMethod, executeArgBox);

        // URL pattern (using urlParamTypeList)
        final String specifiedUrlPattern = executeOption.getSpecifiedUrlPattern(); // null allowed
        final UrlPatternAnalyzer urlPatternAnalyzer = newUrlPatternAnalyzer();
        final UrlPatternChosenBox chosenBox = urlPatternAnalyzer.choose(executeMethod, specifiedUrlPattern, this.urlParamTypeList);
        final UrlPatternRegexpBox regexpBox =
                urlPatternAnalyzer.toRegexp(executeMethod, chosenBox.getUrlPattern(), this.urlParamTypeList, this.optionalGenericTypeMap);
        urlPatternAnalyzer.checkUrlPatternVariableCount(executeMethod, regexpBox.getVarList(), this.urlParamTypeList);
        this.preparedUrlPattern = newPreparedUrlPattern(chosenBox, regexpBox);

        // defined parameter again (uses URL pattern result)
        this.urlParamArgs = prepareUrlParamArgs(this.urlParamTypeList, this.optionalGenericTypeMap);

        // check finally
        checkExecuteMethod(executeArgAnalyzer);
    }

    // -----------------------------------------------------
    //                                           Transaction
    //                                           -----------
    protected TransactionGenre chooseTransactionGenre(ExecuteOption executeOption) {
        return executeOption.isSuppressTransaction() ? TransactionGenre.NONE : getDefaultTransactionGenre();
    }

    protected TransactionGenre getDefaultTransactionGenre() {
        return TransactionGenre.REQUIRES_NEW;
    }

    // -----------------------------------------------------
    //                                       SQL Count Limit
    //                                       ---------------
    protected OptionalThing<Integer> createOptionalSqlExecutionCountLimit(ExecuteOption executeOption) {
        final int specifiedLimit = executeOption.getSqlExecutionCountLimit();
        return OptionalThing.ofNullable(specifiedLimit >= 0 ? specifiedLimit : null, () -> {
            throw new IllegalStateException("Not found the specified SQL execution count limit: " + toSimpleMethodExp());
        });
    }

    // -----------------------------------------------------
    //                                              Analyzer
    //                                              --------
    protected ExecuteArgAnalyzer newExecuteArgAnalyzer() {
        return new ExecuteArgAnalyzer();
    }

    protected ExecuteArgBox newExecuteArgBox() {
        return new ExecuteArgBox();
    }

    protected UrlPatternAnalyzer newUrlPatternAnalyzer() {
        return new UrlPatternAnalyzer();
    }

    protected PreparedUrlPattern newPreparedUrlPattern(UrlPatternChosenBox chosenBox, UrlPatternRegexpBox regexpBox) {
        return new PreparedUrlPattern(chosenBox.getUrlPattern(), regexpBox.getRegexpPattern(), chosenBox.isMethodNamePrefix());
    }

    // -----------------------------------------------------
    //                                           Action Form
    //                                           -----------
    protected OptionalThing<ActionFormMeta> analyzeFormMeta(Method executeMethod, ExecuteArgBox executeArgBox) {
        return prepareFormMeta(OptionalThing.ofNullable(executeArgBox.getFormType(), () -> {
            throw new IllegalStateException("Not found the form type: " + executeMethod);
        }), OptionalThing.ofNullable(executeArgBox.getListFormParameter(), () -> {
            throw new IllegalStateException("Not found the parameter of list form: " + executeMethod);
        }), OptionalThing.empty());
    }

    // public for pushed form
    /**
     * @param formType The optional type of action form. (NotNull, EmptyAllowed: if empty, no form for the method)
     * @param listFormParameter The optional parameter of list form. (NotNull, EmptyAllowed: normally empty, for e.g. JSON list)
     * @param formSetupper The optional set-upper of new-created form. (NotNull, EmptyAllowed: normally empty, foro pushed form)
     * @return The optional form meta to be prepared. (NotNull)
     */
    public OptionalThing<ActionFormMeta> prepareFormMeta(OptionalThing<Class<?>> formType, OptionalThing<Parameter> listFormParameter,
            OptionalThing<Consumer<Object>> formSetupper) {
        final ActionFormMeta meta = formType.map(tp -> createFormMeta(tp, listFormParameter, formSetupper)).orElse(null);
        return OptionalThing.ofNullable(meta, () -> {
            String msg = "Not found the form meta as parameter for the execute method: " + executeMethod;
            throw new ActionFormNotFoundException(msg);
        });
    }

    protected ActionFormMeta createFormMeta(Class<?> formType, OptionalThing<Parameter> listFormParameter,
            OptionalThing<Consumer<Object>> formSetupper) {
        return newActionFormMeta(buildFormKey(), formType, listFormParameter, formSetupper);
    }

    protected String buildFormKey() {
        return actionMapping.getActionDef().getComponentName() + "_" + executeMethod.getName() + "_Form";
    }

    protected ActionFormMeta newActionFormMeta(String formKey, Class<?> formType, OptionalThing<Parameter> listFormParameter,
            OptionalThing<Consumer<Object>> formSetupper) {
        return new ActionFormMeta(this, formKey, formType, listFormParameter, formSetupper);
    }

    // -----------------------------------------------------
    //                               URL Parameter Arguments
    //                               -----------------------
    protected OptionalThing<UrlParamArgs> prepareUrlParamArgs(List<Class<?>> urlParamTypeList,
            Map<Integer, Class<?>> optionalGenericTypeMap) {
        final UrlParamArgs args = !urlParamTypeList.isEmpty() ? newUrlParamArgs(urlParamTypeList, optionalGenericTypeMap) : null;
        return OptionalThing.ofNullable(args, () -> throwUrlParamArgsNotFoundException());
    }

    protected UrlParamArgs newUrlParamArgs(List<Class<?>> urlParamTypeList, Map<Integer, Class<?>> optionalGenericTypeMap) {
        return new UrlParamArgs(urlParamTypeList, optionalGenericTypeMap);
    }

    protected void throwUrlParamArgsNotFoundException() {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the URL parameter arguments for the execute method.");
        br.addItem("Execute Method");
        br.addElement(toSimpleMethodExp());
        final String msg = br.buildExceptionMessage();
        throw new UrlParamArgsNotFoundException(msg);
    }

    // -----------------------------------------------------
    //                                      Check Definition
    //                                      ----------------
    protected void checkExecuteMethod(ExecuteArgAnalyzer executeArgAnalyzer) {
        new ExecuteMethodChecker(executeMethod, formMeta).checkAll(executeArgAnalyzer);
    }

    // ===================================================================================
    //                                                                    Determine Target
    //                                                                    ================
    // -----------------------------------------------------
    //                                      by URL Parameter
    //                                      ----------------
    public boolean determineTargetByUrlParameter(String paramPath) {
        if (!isParameterEmpty(paramPath)) {
            return handleOptionalParameterMapping(paramPath) || preparedUrlPattern.matcher(paramPath).find();
        } else {
            // should not be called if param is empty, old code is like this:
            //return "index".equals(urlPattern);
            String msg = "The paramPath should not be null or empty: [" + paramPath + "], " + toSimpleMethodExp();
            throw new IllegalStateException(msg);
        }
    }

    protected boolean handleOptionalParameterMapping(String paramPath) {
        if (hasOptionalUrlParameter()) { // e.g. any parameters are optional type
            if (indexMethod) { // e.g. index(String first, OptionalThing<String> second) with 'sea' or 'sea/land'
                final int paramCount = Srl.count(Srl.trim(paramPath, "/"), "/") + 1; // e.g. sea/land => 2
                return matchesParameterCount(paramCount);
            } else { // e.g. sea(String first, OptionalThing<String> second) with 'sea/dockside' or 'sea/dockside/hangar'
                // required parameter may not be specified but checked later as 404
                final String firstElement = Srl.substringFirstFront(paramPath, "/"); // e.g. sea (from sea/dockside)
                if (firstElement.equals(getExecuteMethod().getName())) { // e.g. sea(first) with sea/dockside
                    final int paramCount = Srl.count(Srl.trim(paramPath, "/"), "/"); // e.g. sea/dockside => 1
                    return matchesParameterCount(paramCount);
                }
                return false;
            }
        } else {
            return false;
        }
    }

    protected boolean hasOptionalUrlParameter() {
        // already checked here that optional parameters are defined at rear arguments
        return urlParamArgs.map(args -> {
            return args.getUrlParamTypeList().stream().anyMatch(tp -> isOptionalParameterType(tp));
        }).orElse(false);
    }

    protected boolean matchesParameterCount(int paramCount) {
        return paramCount >= countRequiredParameter() && paramCount <= countAllParameter();
    }

    protected int countAllParameter() {
        return urlParamArgs.map(args -> args.getUrlParamTypeList().size()).orElse(0);
    }

    protected int countRequiredParameter() {
        return urlParamArgs.map(args -> {
            return args.getUrlParamTypeList().stream().filter(tp -> {
                return !isOptionalParameterType(tp);
            }).count();
        }).orElse(0L).intValue();
    }

    protected boolean isOptionalParameterType(Class<?> paramType) {
        return LaActionExecuteUtil.isOptionalParameterType(paramType);
    }

    // -----------------------------------------------------
    //                                  by Request Parameter
    //                                  --------------------
    public boolean determineTargetByRequestParameter(HttpServletRequest request) {
        final String methodName = executeMethod.getName();
        return !isParameterEmpty(request.getParameter(methodName)) // e.g. doUpdate=update
                || !isParameterEmpty(request.getParameter(methodName + ".x")) // e.g. doUpdate.x=update
                || !isParameterEmpty(request.getParameter(methodName + ".y")); // e.g. doUpdate.y=update
    }

    protected boolean isParameterEmpty(String str) {
        return str == null || str.isEmpty();
    }

    // ===================================================================================
    //                                                                          API Action
    //                                                                          ==========
    /**
     * Is the action execute for API request? (contains e.g. JSON response return type)
     * @return The determination, true or false.
     */
    public boolean isApiExecute() {
        return isReturnApiResponse() || isImpelementApiAction();
    }

    protected boolean isReturnApiResponse() {
        return ApiResponse.class.isAssignableFrom(getExecuteMethod().getReturnType());
    }

    protected boolean isImpelementApiAction() {
        return ApiAction.class.isAssignableFrom(getActionMapping().getActionDef().getComponentClass());
    }

    // ===================================================================================
    //                                                                         Action Form
    //                                                                         ===========
    public OptionalThing<VirtualForm> createActionForm() {
        return formMeta.map(meta -> meta.createActionForm());
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("execute:{");
        sb.append(toSimpleMethodExp());
        sb.append(", ").append(preparedUrlPattern);
        sb.append("}@").append(Integer.toHexString(hashCode()));
        return sb.toString();
    }

    public String toSimpleMethodExp() {
        return LaActionExecuteUtil.buildSimpleMethodExp(executeMethod);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    /**
     * @return The action mapping for the execute method. (NotNull)
     */
    public ActionMapping getActionMapping() {
        return actionMapping;
    }

    /**
     * @return The type object of action, non enhanced. (NotNull)
     */
    public Class<?> getActionType() {
        return actionMapping.getActionDef().getComponentClass();
    }

    /**
     * @return The reflection method of action execute. (NotNull)
     */
    public Method getExecuteMethod() {
        return executeMethod;
    }

    public boolean isIndexMethod() {
        return indexMethod;
    }

    public TransactionGenre getTransactionGenre() {
        return transactionGenre;
    }

    public boolean isSuppressValidatorCallCheck() {
        return suppressValidatorCallCheck;
    }

    public OptionalThing<Integer> getSqlExecutionCountLimit() {
        return sqlExecutionCountLimit;
    }

    // -----------------------------------------------------
    //                                     Defined Parameter
    //                                     -----------------
    public List<Class<?>> getUrlParamTypeList() {
        return urlParamTypeList;
    }

    /**
     * @return The optional arguments of URL parameter for the method. (NotNull, EmptyAllowed: when no URL parameter)
     */
    public OptionalThing<UrlParamArgs> getUrlParamArgs() {
        return urlParamArgs;
    }

    /**
     * @return The optional meta of action form. (NotNull, EmptyAllowed: when no form in parameter)
     */
    public OptionalThing<ActionFormMeta> getFormMeta() {
        return formMeta;
    }

    // -----------------------------------------------------
    //                                           URL Pattern
    //                                           -----------
    public PreparedUrlPattern getPreparedUrlPattern() {
        return preparedUrlPattern;
    }

    /**
     * @return The prepared expression of URL pattern. (NotNull)
     * @deprecated use getPreparedUrlPattern()
     */
    public String getUrlPattern() { // for compatible, already UTFlute uses
        return preparedUrlPattern.getUrlPattern();
    }
}
