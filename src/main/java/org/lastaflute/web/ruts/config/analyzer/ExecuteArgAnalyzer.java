/*
 * Copyright 2015-2017 the original author or authors.
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
package org.lastaflute.web.ruts.config.analyzer;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.Srl;
import org.lastaflute.web.exception.ActionFormNotLastParameterException;
import org.lastaflute.web.exception.ExecuteMethodOptionalParameterGenericNotFoundException;
import org.lastaflute.web.exception.ExecuteMethodOptionalParameterGenericNotScalarException;
import org.lastaflute.web.util.LaActionExecuteUtil;

/**
 * @author jflute
 */
public class ExecuteArgAnalyzer {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    public static final String FORM_SUFFIX = "Form"; // for form parameters
    public static final String BODY_SUFFIX = "Body"; // for JSON body

    // ===================================================================================
    //                                                                             Analyze
    //                                                                             =======
    public void analyzeExecuteArg(Method executeMethod, ExecuteArgBox box) {
        List<Class<?>> pathParamTypeList = null; // lazy loaded
        Parameter formParam = null;
        final Parameter[] parameters = executeMethod.getParameters();
        if (parameters.length > 0) {
            boolean formEnd = false;
            for (Parameter parameter : parameters) {
                if (formEnd) {
                    throwActionFormNotLastParameterException(executeMethod);
                }
                checkNonGenericParameter(executeMethod, parameter);
                if (isActionFormParameter(parameter)) {
                    formParam = parameter;
                    formEnd = true;
                } else {
                    if (pathParamTypeList == null) {
                        pathParamTypeList = new ArrayList<Class<?>>(4);
                    }
                    pathParamTypeList.add(parameter.getType());
                }
            }
        }
        box.setPathParamTypeList(preparePathParamTypeList(pathParamTypeList));
        box.setOptionalGenericTypeMap(prepareOptionalGenericTypeMap(executeMethod));
        box.setFormType(prepareFormType(formParam));
        box.setListFormParameter(prepareListFormParameter(formParam));
    }

    // ===================================================================================
    //                                                                      Form Parameter
    //                                                                      ==============
    public boolean isActionFormParameter(Parameter parameter) {
        return isBeanActionFormParameter(parameter) || isListActionFormParameter(parameter);
    }

    // -----------------------------------------------------
    //                                             Bean Form
    //                                             ---------
    protected boolean isBeanActionFormParameter(Parameter parameter) {
        return isBeanActionFormType(parameter.getType());
    }

    protected boolean isBeanActionFormType(Type parameterType) {
        final String typeName = parameterType.getTypeName();
        return !typeName.startsWith("java.") && Srl.endsWith(typeName, getFormSuffix(), getBodySuffix());
    }

    protected String getFormSuffix() {
        return FORM_SUFFIX; // for form parameters
    }

    protected String getBodySuffix() {
        return BODY_SUFFIX; // for JSON body
    }

    // -----------------------------------------------------
    //                                             List Form
    //                                             ---------
    protected boolean isListActionFormParameter(Parameter parameter) {
        return findListFormGenericType(parameter) != null;
    }

    protected Class<?> findListFormGenericType(Parameter parameter) {
        if (List.class.equals(parameter.getType())) { // just List
            final Type pt = parameter.getParameterizedType();
            final Class<?> genericType = DfReflectionUtil.getGenericFirstClass(pt); // almost not null, already checked
            return genericType != null && isBeanActionFormType(genericType) ? genericType : null; // e.g. List<SeaForm>
        }
        return null;
    }

    // -----------------------------------------------------
    //                                            Check Form
    //                                            ----------
    protected void throwActionFormNotLastParameterException(Method executeMethod) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not allowed to define argument after ActionForm.");
        br.addItem("Advice");
        br.addElement("ActionForm should be defined at last parameter");
        br.addElement("at @Execute method.");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse index(SeaForm form, int pageNumber) { // *Bad");
        br.addElement("  (o):");
        br.addElement("    public HtmlResponse index(int pageNumber, SeaForm form) { // Good");
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        final String msg = br.buildExceptionMessage();
        throw new ActionFormNotLastParameterException(msg);
    }

    // -----------------------------------------------------
    //                                          Prepare Form
    //                                          ------------
    protected Class<?> prepareFormType(Parameter formParam) {
        return formParam != null ? formParam.getType() : null;
    }

    protected Parameter prepareListFormParameter(Parameter formParam) { // already checked but just in case
        return (formParam != null && formParam.getParameterizedType() instanceof ParameterizedType) ? formParam : null;
    }

    // ===================================================================================
    //                                                               Optional Generic Type
    //                                                               =====================
    protected Map<Integer, Class<?>> prepareOptionalGenericTypeMap(Method executeMethod) {
        final Parameter[] parameters = executeMethod.getParameters();
        if (parameters.length == 0) {
            return Collections.emptyMap();
        }
        final Map<Integer, Class<?>> optionalGenericTypeMap = new LinkedHashMap<Integer, Class<?>>(4);
        int index = 0;
        for (Parameter parameter : parameters) {
            if (isOptionalParameterType(parameter.getType())) {
                final Type paramedType = parameter.getParameterizedType();
                final Class<?> genericType = DfReflectionUtil.getGenericFirstClass(paramedType);
                checkExecuteMethodOptionalParameter(executeMethod, paramedType, genericType);
                optionalGenericTypeMap.put(index, genericType);
            }
            ++index;
        }
        return Collections.unmodifiableMap(optionalGenericTypeMap);
    }

    protected void checkExecuteMethodOptionalParameter(Method executeMethod, final Type paramedType, final Class<?> genericType) {
        if (genericType == null) { // e.g. non-generic optional
            throwExecuteMethodOptionalParameterGenericNotFoundException(executeMethod, paramedType);
        }
        if (genericType.equals(Object.class)) { // e.g. wild-card generic or just Object
            throwExecuteMethodOptionalParameterGenericNotScalarException(executeMethod, paramedType, genericType);
        }
    }

    protected boolean isOptionalParameterType(Class<?> paramType) {
        return LaActionExecuteUtil.isOptionalParameterType(paramType);
    }

    protected void throwExecuteMethodOptionalParameterGenericNotFoundException(Method executeMethod, Type paramedType) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the generic type for the optional parameter.");
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        br.addItem("Parameterized Type");
        br.addElement(paramedType);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodOptionalParameterGenericNotFoundException(msg);
    }

    protected void throwExecuteMethodOptionalParameterGenericNotScalarException(Method executeMethod, Type paramedType,
            Class<?> genericType) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not scalar generic type for the optional parameter.");
        br.addItem("Advice");
        br.addElement("Optional generic type should be scalar type e.g. Integer, String");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse index(OptionalThing<?> opt) { // *Bad");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse index(OptionalThing<Object> opt) { // *Bad");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse index(OptionalThing<SeaBean> opt) { // *Bad");
        br.addElement("  (o):");
        br.addElement("    public HtmlResponse index(OptionalThing<Integer> opt) { // Good");
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        if (paramedType != null) {
            br.addItem("Parameterized Type");
            br.addElement(paramedType);
        }
        br.addItem("Generic Type");
        br.addElement(genericType);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodOptionalParameterGenericNotScalarException(msg);
    }

    // ===================================================================================
    //                                                                NonGeneric Parameter
    //                                                                ====================
    protected void checkNonGenericParameter(Method executeMethod, Parameter parameter) {
        if (isNonGenericCheckTargetType(parameter.getType())) { // e.g. List
            final Type paramedType = parameter.getParameterizedType();
            if (paramedType == null) { // no way? no check just in case
                return;
            }
            if (paramedType instanceof ParameterizedType) {
                final Type[] typeArgs = ((ParameterizedType) paramedType).getActualTypeArguments();
                if (typeArgs != null && typeArgs.length > 0 && "?".equals(typeArgs[0].getTypeName())) { // e.g. List<?>
                    throwActionFormWildcardOnlyListParameterException(executeMethod, parameter);
                }
            } else {
                throwActionFormNonGenericListParameterException(executeMethod, parameter);
            }
        }
    }

    protected boolean isNonGenericCheckTargetType(Class<?> tp) {
        return List.class.equals(tp); // just type, interface only because of endless
    }

    protected void throwActionFormWildcardOnlyListParameterException(Method executeMethod, Parameter parameter) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Cannot use wildcard-only List or Set... e.g. List<?>, Set<?>.");
        br.addItem("Advice");
        br.addElement("Add explicit generic type to the collection type");
        br.addElement("of the @Execute method parameter.");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse index(List<?> formList) { // *Bad");
        br.addElement("  (o):");
        br.addElement("    public HtmlResponse index(List<SeaForm> formList) { // Good");
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        br.addItem("WildcardOnly Parameter");
        br.addElement(parameter);
        final String msg = br.buildExceptionMessage();
        throw new ActionFormNotLastParameterException(msg);
    }

    protected void throwActionFormNonGenericListParameterException(Method executeMethod, Parameter parameter) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Cannot use non-generic List or Set... e.g. List, Set.");
        br.addItem("Advice");
        br.addElement("Add explicit generic type to the collection type");
        br.addElement("of the @Execute method parameter.");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse index(List formList) { // *Bad");
        br.addElement("  (o):");
        br.addElement("    public HtmlResponse index(List<SeaForm> formList) { // Good");
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        br.addItem("NonGeneric Parameter");
        br.addElement(parameter);
        final String msg = br.buildExceptionMessage();
        throw new ActionFormNotLastParameterException(msg);
    }

    protected List<Class<?>> preparePathParamTypeList(List<Class<?>> pathParamTypeList) {
        return pathParamTypeList != null ? Collections.unmodifiableList(pathParamTypeList) : Collections.emptyList();
    }

    // ===================================================================================
    //                                                                Execute Argument Box
    //                                                                ====================
    public static class ExecuteArgBox {

        protected List<Class<?>> pathParamTypeList;
        protected Map<Integer, Class<?>> optionalGenericTypeMap;
        protected Class<?> formType; // null allowed
        protected Parameter listFormParameter; // null allowed for e.g. JSON list

        public List<Class<?>> getPathParamTypeList() {
            return pathParamTypeList;
        }

        public void setPathParamTypeList(List<Class<?>> pathParamTypeList) {
            this.pathParamTypeList = pathParamTypeList;
        }

        public Map<Integer, Class<?>> getOptionalGenericTypeMap() {
            return optionalGenericTypeMap;
        }

        public void setOptionalGenericTypeMap(Map<Integer, Class<?>> optionalGenericTypeMap) {
            this.optionalGenericTypeMap = optionalGenericTypeMap;
        }

        public Class<?> getFormType() {
            return formType;
        }

        public void setFormType(Class<?> formType) {
            this.formType = formType;
        }

        public Parameter getListFormParameter() {
            return listFormParameter;
        }

        public void setListFormParameter(Parameter listFormParameter) {
            this.listFormParameter = listFormParameter;
        }
    }
}