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
package org.lastaflute.web.ruts.config.analyzer;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.util.DfReflectionUtil;
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
    public static final String FORM_SUFFIX = "Form";

    // ===================================================================================
    //                                                                             Analyze
    //                                                                             =======
    public void analyzeExecuteArg(Method executeMethod, ExecuteArgBox box) {
        List<Class<?>> urlParamTypeList = null; // lazy loaded
        Parameter formParam = null;
        final Parameter[] parameters = executeMethod.getParameters();
        if (parameters.length > 0) {
            boolean formEnd = false;
            for (Parameter parameter : parameters) {
                if (formEnd) {
                    throwActionFormNotLastParameterException(executeMethod);
                }
                if (isActionFormParameter(parameter)) {
                    formParam = parameter;
                    formEnd = true;
                } else {
                    if (urlParamTypeList == null) {
                        urlParamTypeList = new ArrayList<Class<?>>(4);
                    }
                    urlParamTypeList.add(parameter.getType());
                }
            }
        }
        box.setUrlParamTypeList(prepareurlParamTypeList(urlParamTypeList));
        box.setOptionalGenericTypeMap(prepareOptionalGenericTypeMap(executeMethod));
        box.setFormType(prepareFormType(formParam));
        box.setListFormGenericType(prepareListFormGenericType(formParam));
    }

    protected List<Class<?>> prepareurlParamTypeList(List<Class<?>> urlParamTypeList) {
        return urlParamTypeList != null ? Collections.unmodifiableList(urlParamTypeList) : Collections.emptyList();
    }

    // -----------------------------------------------------
    //                                        Form Parameter
    //                                        --------------
    public boolean isActionFormParameter(Parameter parameter) {
        return isBeanActionFormParameter(parameter) || isListActionFormParameter(parameter);
    }

    protected boolean isBeanActionFormParameter(Parameter parameter) {
        return isBeanActionFormType(parameter.getType());
    }

    protected boolean isBeanActionFormType(Type parameterType) {
        final String typeName = parameterType.getTypeName();
        return !typeName.startsWith("java.") && typeName.endsWith(getFormSuffix());
    }

    protected String getFormSuffix() {
        return FORM_SUFFIX;
    }

    protected boolean isListActionFormParameter(Parameter parameter) {
        return findListFormGenericType(parameter) != null;
    }

    protected Type findListFormGenericType(Parameter parameter) {
        if (List.class.isAssignableFrom(parameter.getType())) {
            final Type genericType = DfReflectionUtil.getGenericFirstClass(parameter.getParameterizedType());
            return genericType != null && isBeanActionFormType(genericType) ? genericType : null; // e.g. List<SeaForm>
        }
        return null;
    }

    protected void throwActionFormNotLastParameterException(Method executeMethod) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not allowed to define argument after ActionForm.");
        br.addItem("Advice");
        br.addElement("ActionForm should be defined at last parameter");
        br.addElement("at @Execute method.");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse index(SeaForm form, int pageNumber) { // *NG");
        br.addElement("  (o):");
        br.addElement("    public HtmlResponse index(int pageNumber, SeaForm form) { // OK");
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        final String msg = br.buildExceptionMessage();
        throw new ActionFormNotLastParameterException(msg);
    }

    protected Class<?> prepareFormType(Parameter formParam) {
        return formParam != null ? formParam.getType() : null;
    }

    protected Class<?> prepareListFormGenericType(Parameter formParam) {
        return formParam != null ? DfReflectionUtil.getGenericFirstClass(findListFormGenericType(formParam)) : null;
    }

    // -----------------------------------------------------
    //                                 Optional Generic Type
    //                                 ---------------------
    protected Map<Integer, Class<?>> prepareOptionalGenericTypeMap(Method executeMethod) {
        final Parameter[] parameters = executeMethod.getParameters();
        if (parameters.length == 0) {
            return Collections.emptyMap();
        }
        final Map<Integer, Class<?>> optionalGenericTypeMap = new LinkedHashMap<Integer, Class<?>>(4);
        int index = 0;
        for (Parameter parameter : parameters) {
            if (isOptionalParameterType(parameter.getType())) {
                final Type parameterizedType = parameter.getParameterizedType();
                final Class<?> genericType = DfReflectionUtil.getGenericFirstClass(parameterizedType);
                checkExecuteMethodOptionalParameter(executeMethod, parameterizedType, genericType);
                optionalGenericTypeMap.put(index, genericType);
            }
            ++index;
        }
        return Collections.unmodifiableMap(optionalGenericTypeMap);
    }

    protected void checkExecuteMethodOptionalParameter(Method executeMethod, final Type parameterizedType, final Class<?> genericType) {
        if (genericType == null) { // e.g. non-generic optional
            throwExecuteMethodOptionalParameterGenericNotFoundException(executeMethod, parameterizedType);
        }
        if (genericType.equals(Object.class)) { // e.g. wild-card generic or just Object
            throwExecuteMethodOptionalParameterGenericNotScalarException(executeMethod, parameterizedType, genericType);
        }
    }

    protected boolean isOptionalParameterType(Class<?> paramType) {
        return LaActionExecuteUtil.isOptionalParameterType(paramType);
    }

    protected void throwExecuteMethodOptionalParameterGenericNotFoundException(Method executeMethod, Type parameterizedType) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the generic type for the optional parameter.");
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        br.addItem("Parameterized Type");
        br.addElement(parameterizedType);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodOptionalParameterGenericNotFoundException(msg);
    }

    protected void throwExecuteMethodOptionalParameterGenericNotScalarException(Method executeMethod, Type parameterizedType,
            Class<?> genericType) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not scalar generic type for the optional parameter.");
        br.addItem("Advice");
        br.addElement("Optional generic type should be scalar type e.g. Integer, String");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse index(OptionalThing<?> opt) { // *NG");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse index(OptionalThing<Object> opt) { // *NG");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse index(OptionalThing<SeaBean> opt) { // *NG");
        br.addElement("  (o):");
        br.addElement("    public HtmlResponse index(OptionalThing<Integer> opt) { // OK");
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        if (parameterizedType != null) {
            br.addItem("Parameterized Type");
            br.addElement(parameterizedType);
        }
        br.addItem("Generic Type");
        br.addElement(genericType);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodOptionalParameterGenericNotScalarException(msg);
    }

    // ===================================================================================
    //                                                                Execute Argument Box
    //                                                                ====================
    public static class ExecuteArgBox {

        protected List<Class<?>> urlParamTypeList;
        protected Map<Integer, Class<?>> optionalGenericTypeMap;
        protected Class<?> formType; // null allowed
        protected Class<?> listFormGenericType; // null allowed for e.g. JSON list

        public List<Class<?>> getUrlParamTypeList() {
            return urlParamTypeList;
        }

        public void setUrlParamTypeList(List<Class<?>> urlParamTypeList) {
            this.urlParamTypeList = urlParamTypeList;
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

        public Class<?> getListFormGenericType() {
            return listFormGenericType;
        }

        public void setListFormGenericType(Class<?> listFormGenericType) {
            this.listFormGenericType = listFormGenericType;
        }
    }
}