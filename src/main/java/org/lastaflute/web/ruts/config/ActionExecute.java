/*
 * Copyright 2015-2020 the original author or authors.
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
import org.dbflute.util.DfReflectionUtil;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.db.jta.stage.TransactionGenre;
import org.lastaflute.web.api.ApiAction;
import org.lastaflute.web.exception.ActionFormNotFoundException;
import org.lastaflute.web.exception.PathParamArgsNotFoundException;
import org.lastaflute.web.response.ApiResponse;
import org.lastaflute.web.ruts.VirtualForm;
import org.lastaflute.web.ruts.config.analyzer.ExecuteArgAnalyzer;
import org.lastaflute.web.ruts.config.analyzer.ExecuteArgAnalyzer.ExecuteArgBox;
import org.lastaflute.web.ruts.config.analyzer.MethodNameAnalyzer;
import org.lastaflute.web.ruts.config.analyzer.UrlPatternAnalyzer;
import org.lastaflute.web.ruts.config.analyzer.UrlPatternAnalyzer.UrlPatternChosenBox;
import org.lastaflute.web.ruts.config.analyzer.UrlPatternAnalyzer.UrlPatternRegexpBox;
import org.lastaflute.web.ruts.config.checker.ExecuteMethodChecker;
import org.lastaflute.web.ruts.config.routing.ActionRoutingByPathParamDeterminer;
import org.lastaflute.web.ruts.config.routing.ActionRoutingByRequestParamDeterminer;
import org.lastaflute.web.servlet.request.RequestManager;
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
    protected final String mappingMethodName; // not null
    protected final OptionalThing<String> restfulHttpMethod; // not null, empty allowed
    protected final boolean indexMethod;
    protected final TransactionGenre transactionGenre; // not null
    protected final boolean suppressValidatorCallCheck;
    protected final OptionalThing<Integer> sqlExecutionCountLimit; // not null, empty allowed

    // -----------------------------------------------------
    //                                     Defined Parameter
    //                                     -----------------
    protected final ExecuteArgAnalyzer executeArgAnalyzer; // not null, keep for general use
    protected final OptionalThing<PathParamArgs> pathParamArgs; // not null, empty allowed (at least has one if present)
    protected final OptionalThing<ActionFormMeta> formMeta; // not null, empty allowed

    // -----------------------------------------------------
    //                                           URL Pattern
    //                                           -----------
    protected final PreparedUrlPattern preparedUrlPattern; // not null

    // -----------------------------------------------------
    //                                    Routing Determiner
    //                                    ------------------
    protected final ActionRoutingByPathParamDeterminer routingByPathParamDeterminer; // not null
    protected final ActionRoutingByRequestParamDeterminer routingByRequestParamDeterminer; // not null

    // -----------------------------------------------------
    //                                     Lazy-Loaded Cache
    //                                     -----------------
    // don't use directly
    /** The cache of request manager, just same as cachedAssistantDirector. (NotNull: after lazy-load) */
    protected RequestManager cachedRequestManager; // null allowed until lazy-loaded

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
        final MethodNameAnalyzer methodNameAnalyzer = newMethodNameAnalyzer();
        this.mappingMethodName = methodNameAnalyzer.analyzeMappingMethodName(executeMethod);
        this.restfulHttpMethod = methodNameAnalyzer.analyzeRestfulHttpMethod(executeMethod);
        this.indexMethod = this.mappingMethodName.equals("index");
        this.transactionGenre = chooseTransactionGenre(executeOption);
        this.suppressValidatorCallCheck = executeOption.isSuppressValidatorCallCheck();
        this.sqlExecutionCountLimit = createOptionalSqlExecutionCountLimit(executeOption);

        // defined parameter (needed in URL pattern analyzing)
        this.executeArgAnalyzer = newExecuteArgAnalyzer();
        final ExecuteArgBox executeArgBox = newExecuteArgBox();
        this.executeArgAnalyzer.analyzeExecuteArg(executeMethod, executeArgBox);
        this.formMeta = analyzeFormMeta(executeMethod, executeArgBox);

        // URL pattern (using pathParamTypeList)
        final List<Class<?>> pathParamTypeList = executeArgBox.getPathParamTypeList(); // not null, empty allowed
        final Map<Integer, Class<?>> optionalGenericTypeMap = executeArgBox.getOptionalGenericTypeMap();
        final String specifiedUrlPattern = executeOption.getSpecifiedUrlPattern(); // null allowed
        final UrlPatternAnalyzer urlPatternAnalyzer = newUrlPatternAnalyzer();
        final UrlPatternChosenBox chosenBox =
                urlPatternAnalyzer.choose(executeMethod, this.mappingMethodName, specifiedUrlPattern, pathParamTypeList);
        final UrlPatternRegexpBox regexpBox =
                urlPatternAnalyzer.toRegexp(executeMethod, chosenBox.getResolvedUrlPattern(), pathParamTypeList, optionalGenericTypeMap);
        urlPatternAnalyzer.checkUrlPatternVariableCount(executeMethod, regexpBox.getVarList(), pathParamTypeList);
        this.preparedUrlPattern = newPreparedUrlPattern(chosenBox, regexpBox);

        // defined parameter again (uses URL pattern result)
        this.pathParamArgs = preparePathParamArgs(pathParamTypeList, optionalGenericTypeMap);

        // check finally
        checkExecuteMethod();

        // routing determiner (should be last in constructor because of dependencies to instance variables)
        this.routingByPathParamDeterminer = createRoutingByPathParamDeterminer();
        this.routingByRequestParamDeterminer = createRoutingByRequestParamDeterminer();
    }

    protected ActionRoutingByPathParamDeterminer createRoutingByPathParamDeterminer() {
        return new ActionRoutingByPathParamDeterminer(mappingMethodName, restfulHttpMethod, indexMethod, pathParamArgs, preparedUrlPattern,
                () -> getRequestManager(), () -> toSimpleMethodExp());
    }

    protected ActionRoutingByRequestParamDeterminer createRoutingByRequestParamDeterminer() {
        return new ActionRoutingByRequestParamDeterminer(mappingMethodName);
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
    protected MethodNameAnalyzer newMethodNameAnalyzer() {
        return new MethodNameAnalyzer();
    }

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
        return new PreparedUrlPattern(chosenBox, regexpBox);
    }

    // -----------------------------------------------------
    //                                           Action Form
    //                                           -----------
    protected OptionalThing<ActionFormMeta> analyzeFormMeta(Method executeMethod, ExecuteArgBox executeArgBox) {
        final OptionalThing<Class<?>> rootFormType = OptionalThing.ofNullable(executeArgBox.getRootFormType(), () -> {
            throw new IllegalStateException("Not found the form type: " + executeMethod);
        });
        final OptionalThing<Parameter> listFormParameter = OptionalThing.ofNullable(executeArgBox.getListFormParameter(), () -> {
            throw new IllegalStateException("Not found the parameter of list form: " + executeMethod);
        });
        final OptionalThing<Consumer<Object>> formSetupper = OptionalThing.empty();
        return prepareFormMeta(rootFormType, listFormParameter, formSetupper);
    }

    // public for pushed form
    /**
     * @param rootFormType The optional type of action form defined as root parameter. (NotNull, EmptyAllowed: if empty, no form for the method)
     * @param listFormParameter The optional parameter of list form. (NotNull, EmptyAllowed: normally empty, for e.g. JSON list)
     * @param formSetupper The optional set-upper of new-created form. (NotNull, EmptyAllowed: normally empty, for pushed form)
     * @return The optional form meta to be prepared. (NotNull)
     */
    public OptionalThing<ActionFormMeta> prepareFormMeta(OptionalThing<Class<?>> rootFormType, OptionalThing<Parameter> listFormParameter,
            OptionalThing<Consumer<Object>> formSetupper) {
        final ActionFormMeta meta = rootFormType.map(tp -> createFormMeta(tp, listFormParameter, formSetupper)).orElse(null);
        return OptionalThing.ofNullable(meta, () -> {
            String msg = "Not found the form meta as parameter for the execute method: " + executeMethod;
            throw new ActionFormNotFoundException(msg);
        });
    }

    protected ActionFormMeta createFormMeta(Class<?> rootFormType, OptionalThing<Parameter> listFormParameter,
            OptionalThing<Consumer<Object>> formSetupper) {
        final String formKey = buildFormKey();
        final boolean jsonBodyMapping = isFormJsonBodyMapping(rootFormType, listFormParameter);
        return newActionFormMeta(formKey, rootFormType, listFormParameter, formSetupper, jsonBodyMapping);
    }

    protected String buildFormKey() {
        return actionMapping.getActionDef().getComponentName() + "_" + mappingMethodName + "_Form";
    }

    protected boolean isFormJsonBodyMapping(Class<?> formType, OptionalThing<Parameter> listFormParameter) {
        if (executeArgAnalyzer.isJsonBodyMappingParameter(formType)) {
            return true; // e.g. SeaBody defined as parameter of execute method
        }
        return listFormParameter.map(pm -> {
            // always exists, already checked in romantic action customizer
            return DfReflectionUtil.getGenericFirstClass(pm.getParameterizedType());
        }).map(genericType -> { // e.g. SeaBody of List<SeaBody>
            return executeArgAnalyzer.isJsonBodyMappingParameter(genericType);
        }).orElse(false);
    }

    protected ActionFormMeta newActionFormMeta(String formKey, Class<?> rootFormType, OptionalThing<Parameter> listFormParameter,
            OptionalThing<Consumer<Object>> formSetupper, boolean jsonBodyMapping) {
        return new ActionFormMeta(this, formKey, rootFormType, listFormParameter, formSetupper, jsonBodyMapping);
    }

    // -----------------------------------------------------
    //                               URL Parameter Arguments
    //                               -----------------------
    protected OptionalThing<PathParamArgs> preparePathParamArgs(List<Class<?>> pathParamTypeList,
            Map<Integer, Class<?>> optionalGenericTypeMap) {
        final PathParamArgs args = !pathParamTypeList.isEmpty() ? newPathParamArgs(pathParamTypeList, optionalGenericTypeMap) : null;
        return OptionalThing.ofNullable(args, () -> throwPathParamArgsNotFoundException());
    }

    protected PathParamArgs newPathParamArgs(List<Class<?>> pathParamTypeList, Map<Integer, Class<?>> optionalGenericTypeMap) {
        return new PathParamArgs(pathParamTypeList, optionalGenericTypeMap);
    }

    protected void throwPathParamArgsNotFoundException() {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the path parameter arguments for the execute method.");
        br.addItem("Execute Method");
        br.addElement(toSimpleMethodExp());
        final String msg = br.buildExceptionMessage();
        throw new PathParamArgsNotFoundException(msg);
    }

    // -----------------------------------------------------
    //                                      Check Definition
    //                                      ----------------
    protected void checkExecuteMethod() {
        new ExecuteMethodChecker(executeMethod, formMeta).checkAll(executeArgAnalyzer);
    }

    // ===================================================================================
    //                                                                    Determine Target
    //                                                                    ================
    // -----------------------------------------------------
    //                                      by URL Parameter
    //                                      ----------------
    public boolean determineTargetByPathParameter(String paramPath) {
        return routingByPathParamDeterminer.determine(paramPath);
    }

    // -----------------------------------------------------
    //                                  by Request Parameter
    //                                  --------------------
    public boolean determineTargetByRequestParameter(HttpServletRequest request) {
        return routingByRequestParamDeterminer.determine(request);
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

    public String getMappingMethodName() {
        return mappingMethodName;
    }

    public OptionalThing<String> getRestfulHttpMethod() {
        return restfulHttpMethod;
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
    /**
     * @return The optional arguments of path parameter for the method. (NotNull, EmptyAllowed: no parameter)
     */
    public OptionalThing<PathParamArgs> getPathParamArgs() {
        return pathParamArgs; // at least has one if present
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
        return preparedUrlPattern.getResolvedUrlPattern();
    }

    // -----------------------------------------------------
    //                                     Lazy-Loaded Cache
    //                                     -----------------
    // #hope no use DI container here
    protected RequestManager getRequestManager() {
        if (cachedRequestManager != null) {
            return cachedRequestManager;
        }
        synchronized (this) {
            if (cachedRequestManager != null) {
                return cachedRequestManager;
            }
            cachedRequestManager = ContainerUtil.getComponent(RequestManager.class);
        }
        return cachedRequestManager;
    }
}
