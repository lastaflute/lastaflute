/*
 * Copyright 2014-2015 the original author or authors.
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
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.lastaflute.db.jta.stage.TransactionGenre;
import org.lastaflute.web.api.ApiAction;
import org.lastaflute.web.exception.ActionFormNotFoundException;
import org.lastaflute.web.exception.ActionUrlParameterDifferentArgsException;
import org.lastaflute.web.exception.ExecuteMethodOptionalNotContinuedException;
import org.lastaflute.web.exception.ExecuteMethodReturnTypeNotResponseException;
import org.lastaflute.web.exception.UrlParamArgsNotFoundException;
import org.lastaflute.web.exception.UrlPatternNonsenseSettingException;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.response.ApiResponse;
import org.lastaflute.web.ruts.VirtualActionForm;
import org.lastaflute.web.ruts.config.analyzer.ExecuteArgAnalyzer;
import org.lastaflute.web.ruts.config.analyzer.ExecuteArgAnalyzer.ExecuteArgBox;
import org.lastaflute.web.ruts.config.analyzer.UrlPatternAnalyzer;
import org.lastaflute.web.ruts.config.analyzer.UrlPatternAnalyzer.UrlPatternBox;
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
    protected final TransactionGenre transactionGenre; // not null
    protected final boolean indexMethod;

    // -----------------------------------------------------
    //                                     Defined Parameter
    //                                     -----------------
    protected final List<Class<?>> urlParamTypeList; // not null, read-only e.g. Integer.class, String.class
    protected final Map<Integer, Class<?>> optionalGenericTypeList; // not null, read-only, key is argument index
    protected final OptionalThing<UrlParamArgs> urlParamArgs;
    protected final OptionalThing<ActionFormMeta> formMeta;

    // -----------------------------------------------------
    //                                           URL Pattern
    //                                           -----------
    protected final String urlPattern; // not null e.g. [method] or [method]/{}
    protected final Pattern urlPatternRegexp; // not null e.g. ^([^/]+)$ or ^([^/]+)/([^/]+)$ or ^sea/([^/]+)$

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
        this.transactionGenre = chooseTransactionGenre(executeOption);
        this.indexMethod = executeMethod.getName().equals("index");

        // defined parameter (needed in URL pattern analyzing)
        final ExecuteArgAnalyzer executeArgAnalyzer = newExecuteArgAnalyzer();
        final ExecuteArgBox executeArgBox = newExecuteArgBox();
        executeArgAnalyzer.analyzeExecuteArg(executeMethod, executeArgBox);
        this.urlParamTypeList = executeArgBox.getUrlParamTypeList(); // not null, empty allowed
        this.optionalGenericTypeList = executeArgBox.getOptionalGenericTypeMap();
        this.formMeta = prepareFormMeta(executeArgBox.getFormType(), executeArgBox.getListFormParameter());

        // URL pattern (using urlParamTypeList)
        final String specifiedUrlPattern = executeOption.getSpecifiedUrlPattern(); // null allowed
        checkSpecifiedUrlPattern(specifiedUrlPattern);
        this.urlPattern = chooseUrlPattern(specifiedUrlPattern, this.urlParamTypeList);
        final UrlPatternAnalyzer urlPatternAnalyzer = newUrlPatternAnalyzer();
        final UrlPatternBox urlPatternBox = newUrlPatternBox();
        final String pattern = urlPatternAnalyzer.analyzeUrlPattern(executeMethod, this.urlPattern, urlPatternBox);
        this.urlPatternRegexp = buildUrlPatternRegexp(pattern);
        checkUrlPatternVariableAndDefinedTypeCount(urlPatternBox.getUrlPatternVarList(), this.urlParamTypeList);

        // defined parameter again (uses URL pattern result)
        this.urlParamArgs = prepareUrlParamArgs(this.urlParamTypeList, this.optionalGenericTypeList);

        // check finally
        checkExecuteMethod(executeArgAnalyzer);
    }

    protected TransactionGenre chooseTransactionGenre(ExecuteOption executeOption) {
        return executeOption.isSuppressTransaction() ? TransactionGenre.NONE : getDefaultTransactionGenre();
    }

    protected TransactionGenre getDefaultTransactionGenre() {
        return TransactionGenre.REQUIRES_NEW;
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

    protected UrlPatternBox newUrlPatternBox() {
        return new UrlPatternBox();
    }

    // -----------------------------------------------------
    //                                           Action Form
    //                                           -----------
    // public for pushed form
    /**
     * @param formType The type of action form. (NullAllowed: if null, no form for the method)
     * @param listFormParameter The parameter of list form. (NullAllowed: normally null, for e.g. JSON list)
     * @return The optional form meta to be prepared. (NotNull)
     */
    public OptionalThing<ActionFormMeta> prepareFormMeta(Class<?> formType, Parameter listFormParameter) {
        final ActionFormMeta meta = formType != null ? createFormMeta(formType, listFormParameter) : null;
        return OptionalThing.ofNullable(meta, () -> {
            String msg = "Not found the form meta as parameter for the execute method: " + executeMethod;
            throw new ActionFormNotFoundException(msg);
        });
    }

    protected ActionFormMeta createFormMeta(Class<?> formType, Parameter listFormParameter) {
        return newActionFormMeta(buildFormKey(), formType, OptionalThing.ofNullable(listFormParameter, () -> {
            String msg = "Not found the listFormGenericType: execute=" + toSimpleMethodExp() + " form=" + formType;
            throw new IllegalStateException(msg);
        }));
    }

    protected String buildFormKey() {
        return actionMapping.getActionDef().getComponentName() + "_" + executeMethod.getName() + "_Form";
    }

    protected ActionFormMeta newActionFormMeta(String formKey, Class<?> formType, OptionalThing<Parameter> listFormParameter) {
        return new ActionFormMeta(formKey, formType, listFormParameter);
    }

    // -----------------------------------------------------
    //                                 Specified URL Pattern
    //                                 ---------------------
    protected void checkSpecifiedUrlPattern(String specifiedUrlPattern) {
        if (specifiedUrlPattern != null && canBeAbbreviatedUrlPattern(specifiedUrlPattern)) {
            throwUrlPatternNonsenseSettingException(specifiedUrlPattern);
        }
    }

    protected boolean canBeAbbreviatedUrlPattern(String str) { // format check so simple logic
        return Srl.equalsPlain(str, "{}", "{}/{}", "{}/{}/{}", "{}/{}/{}/{}", "{}/{}/{}/{}/{}", "{}/{}/{}/{}/{}/{}");
    }

    protected void throwUrlPatternNonsenseSettingException(String specifiedUrlPattern) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The urlPattern was non-sense.");
        br.addItem("Advice");
        br.addElement("You can abbreviate the urlPattern attribute");
        br.addElement("because it is very simple pattern.");
        br.addElement("  (x):");
        br.addElement("    @Execute(urlPattern = \"{}\") // *NG");
        br.addElement("    public void index(int pageNumber) {");
        br.addElement("  (o):");
        br.addElement("    @Execute // OK: abbreviate it");
        br.addElement("    public void index(int pageNumber) {");
        br.addElement("  (x):");
        br.addElement("    @Execute(urlPattern = \"{}/{}\") // *NG");
        br.addElement("    public void index(int pageNumber, String keyword) {");
        br.addElement("  (o):");
        br.addElement("    @Execute // OK: abbreviate it");
        br.addElement("    public void index(int pageNumber, String keyword) {");
        br.addItem("Execute Method");
        br.addElement(toSimpleMethodExp());
        br.addItem("Specified urlPattern");
        br.addElement(specifiedUrlPattern);
        final String msg = br.buildExceptionMessage();
        throw new UrlPatternNonsenseSettingException(msg);
    }

    // -----------------------------------------------------
    //                               URL Parameter Arguments
    //                               -----------------------
    protected OptionalThing<UrlParamArgs> prepareUrlParamArgs(List<Class<?>> urlParamTypeList,
            Map<Integer, Class<?>> optionalGenericTypeList) {
        final UrlParamArgs args = !urlParamTypeList.isEmpty() ? newUrlParamArgs(urlParamTypeList, optionalGenericTypeList) : null;
        return OptionalThing.ofNullable(args, () -> throwUrlParamArgsNotFoundException());
    }

    protected UrlParamArgs newUrlParamArgs(List<Class<?>> urlParamTypeList, Map<Integer, Class<?>> optionalGenericTypeList) {
        return new UrlParamArgs(urlParamTypeList, optionalGenericTypeList);
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
    //                                           URL Pattern
    //                                           -----------
    protected String chooseUrlPattern(String specifiedUrlPattern, List<Class<?>> urlParamTypeList) {
        final String methodName = executeMethod.getName();
        if (specifiedUrlPattern != null && !specifiedUrlPattern.isEmpty()) { // e.g. urlPattern="{}"
            return adjustUrlPatternMethodPrefix(specifiedUrlPattern, methodName);
        } else { // urlPattern=[no definition]
            if (!urlParamTypeList.isEmpty()) { // e.g. sea(int pageNumber)
                return adjustUrlPatternMethodPrefix(buildDerivedUrlPattern(urlParamTypeList), methodName);
            } else { // e.g. sea() *no parameter
                return methodName;
            }
        }
    }

    protected String adjustUrlPatternMethodPrefix(String specifiedUrlPattern, String methodName) {
        if (methodName.equals("index")) { // e.g. index(pageNumber), urlPattern="{}"
            return specifiedUrlPattern;
        } else { // e.g. sea(pageNumber), urlPattern="{}"
            return methodName + "/" + specifiedUrlPattern;
        }
    }

    protected String buildDerivedUrlPattern(List<Class<?>> urlParamTypeList) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < urlParamTypeList.size(); i++) {
            sb.append(i > 0 ? "/" : "").append("{}");
        }
        return sb.toString();
    }

    protected Pattern buildUrlPatternRegexp(String pattern) {
        return Pattern.compile("^" + pattern + "$");
    }

    protected void checkUrlPatternVariableAndDefinedTypeCount(List<String> urlPatternVarList, List<Class<?>> urlParamTypeList) {
        if (urlPatternVarList.size() != urlParamTypeList.size()) {
            throwActionUrlParameterDifferentArgsException(urlPatternVarList, urlParamTypeList);
        }
    }

    protected void throwActionUrlParameterDifferentArgsException(List<String> urlPatternVarList, List<Class<?>> urlParamTypeList) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Different number of argument for URL parameter.");
        br.addItem("Advice");
        br.addElement("Make sure your urlPattern or arguments.");
        br.addElement("  (x):");
        br.addElement("    @Execute(\"{}/sea/{}\")");
        br.addElement("    public HtmlResponse index(int land) { // *NG");
        br.addElement("  (o):");
        br.addElement("    @Execute(\"{}/sea/{}\")");
        br.addElement("    public HtmlResponse index(int land, String ikspiary) { // OK");
        br.addItem("Execute Method");
        br.addElement(toSimpleMethodExp());
        br.addItem("urlPattern Variable List");
        br.addElement(urlPatternVarList);
        br.addItem("Defined Argument List");
        br.addElement(urlParamTypeList);
        final String msg = br.buildExceptionMessage();
        throw new ActionUrlParameterDifferentArgsException(msg);
    }

    // -----------------------------------------------------
    //                                      Check Definition
    //                                      ----------------
    protected void checkExecuteMethod(ExecuteArgAnalyzer executeArgAnalyzer) {
        checkReturnTypeNotAllowed();
        checkFormPropertyConflict();
        checkOptionalNotContinued(executeArgAnalyzer);
    }

    protected void checkReturnTypeNotAllowed() {
        if (!isAllowedReturnType()) {
            throwExecuteMethodReturnTypeNotResponseException();
        }
    }

    protected boolean isAllowedReturnType() {
        return ActionResponse.class.isAssignableFrom(executeMethod.getReturnType());
    }

    protected void throwExecuteMethodReturnTypeNotResponseException() {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not response return type of the execute method.");
        br.addItem("Advice");
        br.addElement("Execute method should be return action response.");
        br.addElement("  (x):");
        br.addElement("    public String index(SeaForm form) { // *NG");
        br.addElement("  (o):");
        br.addElement("    public HtmlResponse index(SeaForm form) { // OK");
        br.addElement("  (o):");
        br.addElement("    public JsonResponse index(SeaForm form) { // OK");
        br.addElement("  (o):");
        br.addElement("    public StreamResponse index(SeaForm form) { // OK");
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodReturnTypeNotResponseException(msg);
    }

    protected void checkFormPropertyConflict() {
        formMeta.ifPresent(meta -> {
            final String methodName = executeMethod.getName();
            for (ActionFormProperty property : meta.properties()) {
                if (methodName.equalsIgnoreCase(property.getPropertyName())) { // ignore case more strict
                    throwExecuteMethodFormPropertyConflictException(property);
                }
            }
        });
    }

    protected void throwExecuteMethodFormPropertyConflictException(ActionFormProperty property) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Conflicted execute method name with form property name.");
        br.addItem("Advice");
        br.addElement("Execute method should be unique");
        br.addElement("in action methods and action form properties.");
        br.addElement("  (x):");
        br.addElement("    public String index(SeaForm form) {");
        br.addElement("    ...");
        br.addElement("    public class SeaForm {");
        br.addElement("        public Integer index; // *NG");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public String index(SeaForm form) {");
        br.addElement("    ...");
        br.addElement("    public class SeaForm {");
        br.addElement("        public Integer dataIndex; // OK");
        br.addElement("    }");
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        br.addItem("Action Form");
        br.addElement(formMeta);
        br.addItem("Confliected Property");
        br.addElement(property);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodReturnTypeNotResponseException(msg);
    }

    public void checkOptionalNotContinued(ExecuteArgAnalyzer executeArgAnalyzer) {
        boolean opt = false;
        int index = 0;
        for (Parameter parameter : executeMethod.getParameters()) {
            final Class<?> paramType = parameter.getType();
            final boolean currentOpt = isOptionalParameterType(paramType);
            if (opt) {
                if (!currentOpt && !executeArgAnalyzer.isActionFormParameter(parameter)) {
                    throwExecuteMethodOptionalNotContinuedException(index, paramType);
                }
            } else {
                if (currentOpt) {
                    opt = true;
                }
            }
            ++index;
        }
    }

    protected boolean isOptionalParameterType(Class<?> paramType) {
        return LaActionExecuteUtil.isOptionalParameterType(paramType);
    }

    protected void throwExecuteMethodOptionalNotContinuedException(int index, Class<?> paramType) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not continued optional parameter for the execute method.");
        br.addItem("Advice");
        br.addElement("Arguments after optional argument should be optional parameter.");
        br.addElement("  (x):");
        br.addElement("    public String index(OptionalThing<Integer> pageNumber, String keyword) { // *NG");
        br.addElement("  (o):");
        br.addElement("    public String index(OptionalThing<Integer> pageNumber, OptionalThing<String> keyword) { // OK");
        br.addElement("  (o):");
        br.addElement("    public String index(Integer pageNumber, OptionalThing<String> keyword) { // OK");
        br.addElement("  (o):");
        br.addElement("    public String index(Integer pageNumber, String keyword) { // OK");
        br.addElement("  (o):");
        br.addElement("    public String index(OptionalThing<Integer> pageNumber, SeaForm form) { // OK");
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        br.addItem("Not Continued Parameter");
        br.addElement("index : " + index);
        br.addElement("type  : " + paramType);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodOptionalNotContinuedException(msg);
    }

    // ===================================================================================
    //                                                                    Determine Target
    //                                                                    ================
    // -----------------------------------------------------
    //                                      by URL Parameter
    //                                      ----------------
    public boolean determineTargetByUrlParameter(String paramPath) {
        if (!isParameterEmpty(paramPath)) {
            return handleOptionalParameterMapping(paramPath) || urlPatternRegexp.matcher(paramPath).find();
        } else {
            return "index".equals(urlPattern);
        }
    }

    protected boolean handleOptionalParameterMapping(String paramPath) {
        if (!indexMethod && hasOptionalUrlParameter()) { // e.g. sea() and any parameters are optional
            // required parameter may not be specified but checked later as 404
            final String firstElement = Srl.substringFirstFront(paramPath, "/"); // e.g. sea from sea or sea/3/
            return firstElement.equals(getExecuteMethod().getName());
        } else {
            return false;
        }
    }

    protected boolean hasOptionalUrlParameter() {
        return urlParamArgs.map(args -> {
            return args.getUrlParamTypeList().stream().anyMatch(tp -> isOptionalParameterType(tp));
        }).orElse(false);
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
    public OptionalThing<VirtualActionForm> createActionForm() {
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
        sb.append(", urlPattern=").append(urlPattern);
        sb.append(", regexp=").append(urlPatternRegexp);
        sb.append("}@").append(Integer.toHexString(hashCode()));
        return sb.toString();
    }

    public String toSimpleMethodExp() {
        return LaActionExecuteUtil.buildSimpleMethodExp(executeMethod);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public ActionMapping getActionMapping() {
        return actionMapping;
    }

    public Method getExecuteMethod() {
        return executeMethod;
    }

    public TransactionGenre getTransactionGenre() {
        return transactionGenre;
    }

    public boolean isIndexMethod() {
        return indexMethod;
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
    public String getUrlPattern() {
        return urlPattern;
    }

    public Pattern getUrlPatternRegexp() {
        return urlPatternRegexp;
    }
}
