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
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.jdbc.Classification;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.lastaflute.db.jta.stage.TransactionGenre;
import org.lastaflute.web.api.ApiAction;
import org.lastaflute.web.exception.ActionFormNotFoundException;
import org.lastaflute.web.exception.ActionUrlParameterDifferentArgsException;
import org.lastaflute.web.exception.ExecuteMethodFormPropertyConflictException;
import org.lastaflute.web.exception.ExecuteMethodFormPropertyValidationMismatchException;
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
    protected final boolean indexMethod;
    protected final TransactionGenre transactionGenre; // not null
    protected final boolean suppressValidatorCallCheck;
    protected final OptionalThing<Integer> sqlExecutionCountLimit;

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
    protected final String urlPattern; // not null, empty allowed e.g. [method] or [method]/{} or "" (when index())
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
        this.indexMethod = executeMethod.getName().equals("index");
        this.transactionGenre = chooseTransactionGenre(executeOption);
        this.suppressValidatorCallCheck = executeOption.isSuppressValidatorCallCheck();
        this.sqlExecutionCountLimit = createOptionalSqlExecutionCountLimit(executeOption);

        // defined parameter (needed in URL pattern analyzing)
        final ExecuteArgAnalyzer executeArgAnalyzer = newExecuteArgAnalyzer();
        final ExecuteArgBox executeArgBox = newExecuteArgBox();
        executeArgAnalyzer.analyzeExecuteArg(executeMethod, executeArgBox);
        this.urlParamTypeList = executeArgBox.getUrlParamTypeList(); // not null, empty allowed
        this.optionalGenericTypeList = executeArgBox.getOptionalGenericTypeMap();
        this.formMeta = analyzeFormMeta(executeMethod, executeArgBox);

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

    protected UrlPatternBox newUrlPatternBox() {
        return new UrlPatternBox();
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
        br.addElement("    @Execute(urlPattern = \"{}\") // *Bad");
        br.addElement("    public void index(int pageNumber) {");
        br.addElement("  (o):");
        br.addElement("    @Execute // Good: abbreviate it");
        br.addElement("    public void index(int pageNumber) {");
        br.addElement("  (x):");
        br.addElement("    @Execute(urlPattern = \"{}/{}\") // *Bad");
        br.addElement("    public void index(int pageNumber, String keyword) {");
        br.addElement("  (o):");
        br.addElement("    @Execute // Good: abbreviate it");
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
            } else { // e.g. index(), sea() *no parameter
                return adjustUrlPatternByMethodNameWithoutParam(methodName);
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

    protected String adjustUrlPatternByMethodNameWithoutParam(final String methodName) {
        return !methodName.equals("index") ? methodName : ""; // to avoid '/index/' hit
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
        br.addElement("    public HtmlResponse index(int land) { // *Bad");
        br.addElement("  (o):");
        br.addElement("    @Execute(\"{}/sea/{}\")");
        br.addElement("    public HtmlResponse index(int land, String ikspiary) { // Good");
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
        checkFormPropertyValidationMismatch();
        checkOptionalNotContinued(executeArgAnalyzer);
    }

    // -----------------------------------------------------
    //                                     Check Return Type
    //                                     -----------------
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
        br.addElement("    public String index(SeaForm form) { // *Bad");
        br.addElement("  (o):");
        br.addElement("    public HtmlResponse index(SeaForm form) { // Good");
        br.addElement("  (o):");
        br.addElement("    public JsonResponse index(SeaForm form) { // Good");
        br.addElement("  (o):");
        br.addElement("    public StreamResponse index(SeaForm form) { // Good");
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodReturnTypeNotResponseException(msg);
    }

    // -----------------------------------------------------
    //                                   Check Form Property
    //                                   -------------------
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
        br.addElement("        public Integer index; // *Bad");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public String index(SeaForm form) {");
        br.addElement("    ...");
        br.addElement("    public class SeaForm {");
        br.addElement("        public Integer dataIndex; // Good");
        br.addElement("    }");
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        br.addItem("Action Form");
        br.addElement(formMeta);
        br.addItem("Confliected Property");
        br.addElement(property);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodFormPropertyConflictException(msg);
    }

    protected void checkFormPropertyValidationMismatch() {
        formMeta.ifPresent(meta -> {
            for (ActionFormProperty property : meta.properties()) {
                final Field field = property.getPropertyDesc().getField();
                if (field != null) { // check only field
                    final Class<?> fieldType = field.getType();
                    // *depends on JSON rule so difficult, check only physical mismatch here
                    //if (isFormPropertyCannotNotNullType(fieldType)) {
                    //    final Class<NotNull> notNullType = NotNull.class;
                    //    if (field.getAnnotation(notNullType) != null) {
                    //        throwExecuteMethodFormPropertyValidationMismatchException(property, notNullType);
                    //    }
                    //}
                    if (isFormPropertyCannotNotEmptyType(fieldType)) {
                        final Class<NotEmpty> notEmptyType = NotEmpty.class;
                        if (field.getAnnotation(notEmptyType) != null) {
                            throwExecuteMethodFormPropertyValidationMismatchException(property, notEmptyType);
                        }
                    }
                    if (isFormPropertyCannotNotBlankType(fieldType)) {
                        final Class<NotBlank> notBlankType = NotBlank.class;
                        if (field.getAnnotation(notBlankType) != null) {
                            throwExecuteMethodFormPropertyValidationMismatchException(property, notBlankType);
                        }
                    }
                }
            }
        });
    }

    // *depends on JSON rule so difficult
    //protected boolean isFormPropertyCannotNotNullType(Class<?> fieldType) {
    //    return String.class.isAssignableFrom(fieldType) // everybody knows
    //            || isFormPropertyCollectionFamilyType(fieldType); // List, Set, Map, Array, ...
    //}

    protected boolean isFormPropertyCannotNotEmptyType(Class<?> fieldType) {
        return Number.class.isAssignableFrom(fieldType) // Integer, Long, ...
                || int.class.equals(fieldType) || long.class.equals(fieldType) // primitive numbers
                || TemporalAccessor.class.isAssignableFrom(fieldType) // LocalDate, ...
                || java.util.Date.class.isAssignableFrom(fieldType) // old date
                || Boolean.class.isAssignableFrom(fieldType) || boolean.class.equals(fieldType) // boolean
                || Classification.class.isAssignableFrom(fieldType); // CDef
    }

    protected boolean isFormPropertyCannotNotBlankType(Class<?> fieldType) {
        return isFormPropertyCannotNotEmptyType(fieldType) // cannot-NotEmpty types are also
                || isFormPropertyCollectionFamilyType(fieldType); // List, Set, Map, Array, ...
    }

    protected boolean isFormPropertyCollectionFamilyType(Class<?> fieldType) {
        return Collection.class.isAssignableFrom(fieldType) // List, Set, ...
                || Map.class.isAssignableFrom(fieldType) // mainly used in JSON
                || Object[].class.isAssignableFrom(fieldType); // also all arrays
    }

    protected void throwExecuteMethodFormPropertyValidationMismatchException(ActionFormProperty property,
            Class<? extends Annotation> annotation) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Mismatch validation annotation of form property.");
        br.addItem("Advice");
        br.addElement("The annotation setting has mismatch like this:");
        // *depends on JSON rule so difficult
        //br.addElement("  - String type cannot use @NotNull => use @NotEmpty or @NotBlank");
        br.addElement("  - Number types cannot use @NotEmpty, @NotBlank => use @NotNull");
        br.addElement("  - Date types cannot use @NotEmpty, @NotBlank => use @NotNull");
        br.addElement("  - Boolean types cannot use @NotEmpty, @NotBlank => use @NotNull");
        br.addElement("  - CDef types cannot use @NotEmpty, @NotBlank => use @NotNull");
        br.addElement("  - List/Map/... types cannot use @NotBlank => use @NotEmpty");
        // *no touch to @NotNull same reason as String
        //br.addElement("  - List/Map/... types cannot use @NotNull, @NotBlank => use @NotEmpty");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    @NotNull");
        br.addElement("    public String memberName; // *Bad: use @NotEmpty or @NotBlank");
        br.addElement("    @NotNull");
        br.addElement("    public List<SeaBean> seaList; // *Bad: use @NotEmpty");
        br.addElement("    @NotEmpty");
        br.addElement("    public Integer memberAge; // *Bad: use @NotNull");
        br.addElement("    @NotBlank");
        br.addElement("    public LocalDate birthdate; // *Bad: use @NotNull");
        br.addElement("    @NotEmpty");
        br.addElement("    public CDef.MemberStatus statusList; // *Bad: use @NotNull");
        br.addElement("  (o):");
        br.addElement("    @NotBlank");
        br.addElement("    public String memberName;");
        br.addElement("    @NotEmpty");
        br.addElement("    public List<SeaBean> seaList;");
        br.addElement("    @NotNull");
        br.addElement("    public Integer memberAge;");
        br.addElement("    @NotNull");
        br.addElement("    public LocalDate birthdate;");
        br.addElement("    @NotNull");
        br.addElement("    public CDef.MemberStatus statusList;");
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        br.addItem("Action Form");
        br.addElement(formMeta);
        br.addItem("Target Property");
        br.addElement(property);
        br.addItem("Mismatch Annotation");
        br.addElement(annotation);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodFormPropertyValidationMismatchException(msg);
    }

    // -----------------------------------------------------
    //                                        Check Optional
    //                                        --------------
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
        br.addElement("    public String index(OptionalThing<Integer> pageNumber, String keyword) { // *Bad");
        br.addElement("  (o):");
        br.addElement("    public String index(OptionalThing<Integer> pageNumber, OptionalThing<String> keyword) { // Good");
        br.addElement("  (o):");
        br.addElement("    public String index(Integer pageNumber, OptionalThing<String> keyword) { // Good");
        br.addElement("  (o):");
        br.addElement("    public String index(Integer pageNumber, String keyword) { // Good");
        br.addElement("  (o):");
        br.addElement("    public String index(OptionalThing<Integer> pageNumber, SeaForm form) { // Good");
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
            // should not be called if param is empty, old code is like this:
            //return "index".equals(urlPattern);
            String msg = "The paramPath should not be null or empty: [" + paramPath + "], " + toSimpleMethodExp();
            throw new IllegalStateException(msg);
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
    public String getUrlPattern() {
        return urlPattern;
    }

    public Pattern getUrlPatternRegexp() {
        return urlPatternRegexp;
    }
}
