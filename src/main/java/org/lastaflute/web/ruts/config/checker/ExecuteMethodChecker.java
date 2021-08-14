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
package org.lastaflute.web.ruts.config.checker;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.jdbc.Classification;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfReflectionUtil;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.PropertyDesc;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;
import org.lastaflute.web.exception.ExecuteMethodFormPropertyConflictException;
import org.lastaflute.web.exception.ExecuteMethodOptionalNotContinuedException;
import org.lastaflute.web.exception.ExecuteMethodReturnTypeNotResponseException;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.ruts.config.ActionFormMeta;
import org.lastaflute.web.ruts.config.ActionFormProperty;
import org.lastaflute.web.ruts.config.analyzer.ExecuteArgAnalyzer;
import org.lastaflute.web.util.LaActionExecuteUtil;

/**
 * @author modified by jflute (originated in Sessar)
 */
public class ExecuteMethodChecker implements Serializable {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = 1L;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Method executeMethod; // not null
    protected final OptionalThing<ActionFormMeta> formMeta;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ExecuteMethodChecker(Method executeMethod, OptionalThing<ActionFormMeta> formMeta) {
        this.executeMethod = executeMethod;
        this.formMeta = formMeta;
    }

    // ===================================================================================
    //                                                                           Check All
    //                                                                           =========
    public void checkAll(ExecuteArgAnalyzer executeArgAnalyzer) {
        checkReturnTypeNotAllowed();
        checkOptionalNotContinued(executeArgAnalyzer);
        checkFormPropertyConflict();
        checkFormValidator();
        checkJsonBeanValidator();
    }

    // ===================================================================================
    //                                                               ReturnType NotAllowed
    //                                                               =====================
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

    // ===================================================================================
    //                                                               Optional NotContinued
    //                                                               =====================
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
    //                                                               FormProperty Conflict
    //                                                               =====================
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

    // ===================================================================================
    //                                                                      Form Validator
    //                                                                      ==============
    protected void checkFormValidator() {
        formMeta.ifPresent(meta -> {
            final Deque<String> pathDeque = new LinkedList<String>(); // recycled
            // meta.properties() is for root form type, so use it here
            // while, this cannot work if List<SeaBody> because of List properties
            // #hope jflute will handle properties of List generic type to check here (2019/01/08)
            final Set<Class<?>> mismatchedCheckedTypeSet = DfCollectionUtil.newHashSet(meta.getRootFormType());
            final Set<Class<?>> lonelyCheckedTypeSet = DfCollectionUtil.newHashSet(meta.getRootFormType());
            for (ActionFormProperty property : meta.properties()) {
                final Field field = property.getPropertyDesc().getField();
                if (field != null) { // check only field, simply
                    pathDeque.clear(); // to recycle
                    checkFormMismatchedValidatorAnnotation(property, field, pathDeque, mismatchedCheckedTypeSet);
                    pathDeque.clear(); // to recycle
                    checkFormLonelyValidatorAnnotation(property, field, pathDeque, lonelyCheckedTypeSet);
                }
            }
        });
    }

    protected void checkFormMismatchedValidatorAnnotation(ActionFormProperty property, Field field, Deque<String> pathDeque,
            Set<Class<?>> checkedTypeSet) {
        createFormValidatorChecker(property, pathDeque, checkedTypeSet).checkMismatchedValidatorAnnotation(field, Collections.emptyMap());
    }

    protected void checkFormLonelyValidatorAnnotation(ActionFormProperty property, Field field, Deque<String> pathDeque,
            Set<Class<?>> checkedTypeSet) {
        createFormValidatorChecker(property, pathDeque, checkedTypeSet).checkLonelyValidatorAnnotation(field, Collections.emptyMap());
    }

    protected ExecuteMethodValidatorChecker createFormValidatorChecker(ActionFormProperty property, Deque<String> pathDeque,
            Set<Class<?>> checkedTypeSet) {
        return new ExecuteMethodValidatorChecker(executeMethod, () -> {
            final Map<String, Object> baseInfoMap = new LinkedHashMap<String, Object>(2);
            baseInfoMap.put("ActionForm", formMeta);
            baseInfoMap.put("Root Property", property);
            return baseInfoMap;
        }, pathDeque, checkedTypeSet);
    }

    // ===================================================================================
    //                                                                 JSON Bean Validator
    //                                                                 ===================
    protected void checkJsonBeanValidator() {
        final Class<?> returnType = executeMethod.getReturnType();
        if (!JsonResponse.class.isAssignableFrom(returnType)) {
            return;
        }
        final Type genericReturnType = executeMethod.getGenericReturnType();
        if (genericReturnType == null || !(genericReturnType instanceof ParameterizedType)) { // just in case
            return;
        }
        final Class<?> jsonBeanType = DfReflectionUtil.getGenericFirstClass(genericReturnType);
        if (jsonBeanType == null) { // just in case
            return;
        }
        if (Collection.class.isAssignableFrom(jsonBeanType)) { // e.g. JsonResponse<List<SeaBean>>
            final Type[] resopnseArgTypes = DfReflectionUtil.getGenericParameterTypes(genericReturnType);
            if (resopnseArgTypes.length > 0) { // just in case
                final Class<?> elementBeanType = DfReflectionUtil.getGenericFirstClass(resopnseArgTypes[0]);
                if (elementBeanType != null && mayBeJsonBeanType(elementBeanType)) { // just in case
                    doCheckJsonBeanValidator(elementBeanType, Collections.emptyMap()); // can check JsonResponse<List<SeaBean>>
                }
            }
        } else if (mayBeJsonBeanType(jsonBeanType)) { // e.g. JsonResponse<SeaBean>, JsonResponse<WholeBean<SeaBean>>
            doCheckJsonBeanValidator(jsonBeanType, prepareJsonBeanGenericMap(genericReturnType, jsonBeanType));
        }
    }

    protected boolean mayBeJsonBeanType(Class<?> fieldType) {
        return !fieldType.isPrimitive() // not primitive types
                && !fieldType.isArray() // not array type, array's nested bean is unsupported for the check, simply
                && !Object.class.equals(fieldType) // e.g. generic type
                && !String.class.isAssignableFrom(fieldType) // not String
                && !Number.class.isAssignableFrom(fieldType) // not Integer, Long, ...
                && !TemporalAccessor.class.isAssignableFrom(fieldType) // not LocalDate, ...
                && !java.util.Date.class.isAssignableFrom(fieldType) // not old date
                && !Boolean.class.isAssignableFrom(fieldType) // not boolean
                && !Classification.class.isAssignableFrom(fieldType) // not CDef
                && !isCollectionFamilyType(fieldType); // not Collection family
    }

    protected boolean isCollectionFamilyType(Class<?> fieldType) {
        return Collection.class.isAssignableFrom(fieldType) // List, Set, ...
                || Map.class.isAssignableFrom(fieldType) // mainly used in JSON
                || Object[].class.isAssignableFrom(fieldType); // also all arrays
    }

    protected Map<String, Class<?>> prepareJsonBeanGenericMap(Type genericReturnType, Class<?> jsonBeanType) {
        // can check: JsonResponse<LandBean<PiariBean>> landBean;
        final Type[] resopnseArgTypes = DfReflectionUtil.getGenericParameterTypes(genericReturnType);
        if (resopnseArgTypes.length > 0) { // just in case
            final Type firstGenericType = resopnseArgTypes[0];
            final Class<?> elementBeanType = DfReflectionUtil.getGenericFirstClass(firstGenericType);
            if (elementBeanType != null && mayBeJsonBeanType(elementBeanType)) { // just in case
                final Map<String, Class<?>> genericMap = new LinkedHashMap<String, Class<?>>(1); // only first generic #for_now
                final TypeVariable<?>[] typeParameters = jsonBeanType.getTypeParameters();
                if (typeParameters != null && typeParameters.length > 0) { // just in case
                    genericMap.put(typeParameters[0].getName(), elementBeanType); // e.g. DATA = PiariBean.class
                    return genericMap;
                }
            }
        }
        return Collections.emptyMap();
    }

    protected void doCheckJsonBeanValidator(Class<?> jsonBeanType, Map<String, Class<?>> genericMap) {
        final Deque<String> pathDeque = new LinkedList<String>(); // recycled
        final Set<Class<?>> mismatchedCheckedTypeSet = DfCollectionUtil.newHashSet(jsonBeanType);
        final Set<Class<?>> lonelyCheckedTypeSet = DfCollectionUtil.newHashSet(jsonBeanType);
        final BeanDesc beanDesc;
        try {
            beanDesc = BeanDescFactory.getBeanDesc(jsonBeanType);
        } catch (RuntimeException e) { // may be setAccessible(true) failure
            throwExecuteMethodJsonBeanDescFailureException(jsonBeanType, genericMap, e);
            return; // unreachable
        }
        final int pdSide = beanDesc.getPropertyDescSize();
        for (int i = 0; i < pdSide; i++) {
            final PropertyDesc pd = beanDesc.getPropertyDesc(i);
            final Field field = pd.getField();
            if (field != null) {
                pathDeque.clear(); // to recycle
                checkJsonBeanMismatchedValidatorAnnotation(jsonBeanType, pd, field, pathDeque, mismatchedCheckedTypeSet, genericMap);
                pathDeque.clear(); // to recycle
                checkJsonBeanLonelyValidatorAnnotation(jsonBeanType, pd, field, pathDeque, lonelyCheckedTypeSet, genericMap);
            }
        }
    }

    protected void throwExecuteMethodJsonBeanDescFailureException(Class<?> jsonBeanType, Map<String, Class<?>> genericMap,
            RuntimeException e) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Failed to get bean description of the JSON bean.");
        br.addItem("Execute Method");
        br.addElement(executeMethod);
        br.addItem("JSON Bean");
        br.addElement(jsonBeanType);
        if (!genericMap.isEmpty()) {
            br.addItem("Generic Map");
            genericMap.forEach((key, value) -> {
                br.addElement(key + " = " + value);
            });
        }
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg, e);
    }

    protected void checkJsonBeanMismatchedValidatorAnnotation(Class<?> jsonBeanType, PropertyDesc pd, Field field, Deque<String> pathDeque,
            Set<Class<?>> checkedTypeSet, Map<String, Class<?>> genericMap) {
        createJsonBeanValidatorChecker(jsonBeanType, pd, pathDeque, checkedTypeSet).checkMismatchedValidatorAnnotation(field, genericMap);
    }

    protected void checkJsonBeanLonelyValidatorAnnotation(Class<?> jsonBeanType, PropertyDesc pd, Field field, Deque<String> pathDeque,
            Set<Class<?>> checkedTypeSet, Map<String, Class<?>> genericMap) {
        createJsonBeanValidatorChecker(jsonBeanType, pd, pathDeque, checkedTypeSet).checkLonelyValidatorAnnotation(field, genericMap);
    }

    protected ExecuteMethodValidatorChecker createJsonBeanValidatorChecker(Class<?> jsonBeanType, PropertyDesc pd, Deque<String> pathDeque,
            Set<Class<?>> checkedTypeSet) {
        return new ExecuteMethodValidatorChecker(executeMethod, () -> {
            final Map<String, Object> baseInfoMap = new LinkedHashMap<String, Object>(2);
            baseInfoMap.put("JsonBean", jsonBeanType);
            baseInfoMap.put("Root Property", pd);
            return baseInfoMap;
        }, pathDeque, checkedTypeSet);
    }
}
