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
package org.lastaflute.web.ruts.config.checker;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.jdbc.Classification;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfReflectionUtil;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.PropertyDesc;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;
import org.lastaflute.web.exception.ExecuteMethodFormPropertyConflictException;
import org.lastaflute.web.exception.ExecuteMethodFormPropertyValidationMismatchException;
import org.lastaflute.web.exception.ExecuteMethodOptionalNotContinuedException;
import org.lastaflute.web.exception.ExecuteMethodReturnTypeNotResponseException;
import org.lastaflute.web.exception.LonelyValidatorAnnotationException;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.ruts.config.ActionFormMeta;
import org.lastaflute.web.ruts.config.ActionFormProperty;
import org.lastaflute.web.ruts.config.analyzer.ExecuteArgAnalyzer;
import org.lastaflute.web.util.LaActionExecuteUtil;
import org.lastaflute.web.validation.ActionValidator;
import org.lastaflute.web.validation.Required;

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
        checkFormPropertyConflict();
        checkFormPropertyValidationMismatch();
        checkOptionalNotContinued(executeArgAnalyzer);
        checkNestedBeanValidAnnotaed();
    }

    // ===================================================================================
    //                                                                   Check Return Type
    //                                                                   =================
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
    //                                                                 Check Form Property
    //                                                                 ===================
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
            final Set<Class<?>> checkedTypeSet = new HashSet<Class<?>>();
            checkedTypeSet.add(meta.getFormType());
            for (ActionFormProperty property : meta.properties()) {
                final Field field = property.getPropertyDesc().getField();
                if (field != null) { // check only field, simply
                    final Deque<String> pathDeque = new LinkedList<String>(); // begin
                    doCheckFormPropertyValidationMismatch(property, field, pathDeque, checkedTypeSet);
                }
            }
        });
    }

    protected void doCheckFormPropertyValidationMismatch(ActionFormProperty property, Field field, Deque<String> pathDeque,
            Set<Class<?>> checkedTypeSet) {
        pathDeque.push(field.getName());
        checkedTypeSet.add(field.getDeclaringClass());
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
                throwExecuteMethodFormPropertyNotEmptyValidationMismatchException(property, field, pathDeque, notEmptyType);
            }
        }
        if (isFormPropertyCannotNotBlankType(fieldType)) {
            final Class<NotBlank> notBlankType = NotBlank.class;
            if (field.getAnnotation(notBlankType) != null) {
                throwExecuteMethodFormPropertyNotEmptyValidationMismatchException(property, field, pathDeque, notBlankType);
            }
        }
        if (isFormPropertyCannotRequiredPrimitiveType(fieldType)) {
            final Class<Required> requiredType = Required.class;
            if (field.getAnnotation(requiredType) != null) {
                throwExecuteMethodFormPropertyPrimitiveValidationMismatchException(property, field, pathDeque, requiredType);
            }
            final Class<NotNull> notNullType = NotNull.class;
            if (field.getAnnotation(notNullType) != null) {
                throwExecuteMethodFormPropertyPrimitiveValidationMismatchException(property, field, pathDeque, notNullType);
            }
        }
        if (Collection.class.isAssignableFrom(fieldType)) { // only collection, except array and map, simply
            doCheckGenericBeanFormPropertyValidationMismatch(property, field, pathDeque, checkedTypeSet);
        } else if (mayBeNestedBeanType(fieldType)) {
            doCheckNestedFormPropertyValidationMismatch(property, fieldType, pathDeque, checkedTypeSet);
            doCheckGenericBeanFormPropertyValidationMismatch(property, field, pathDeque, checkedTypeSet);
        }
        pathDeque.pop();
    }

    // -----------------------------------------------------
    //                                           Nested Bean
    //                                           -----------
    protected void doCheckGenericBeanFormPropertyValidationMismatch(ActionFormProperty property, Field field, Deque<String> pathDeque,
            Set<Class<?>> checkedTypeSet) {
        // #hope cannot check now: public List<ROOM> roomList;
        final Class<?> genericType = getGenericType(field); // only first generic #for_now
        if (genericType == null) {
            return;
        }
        if (mayBeNestedBeanType(genericType)) { // e.g. LandBean<PiariBean>
            pathDeque.push("@generic");
            doCheckNestedFormPropertyValidationMismatch(property, genericType, pathDeque, checkedTypeSet);
            pathDeque.pop();
        } else if (Collection.class.isAssignableFrom(genericType)) {
            final Type fieldGenericType = field.getGenericType();
            if (fieldGenericType != null && fieldGenericType instanceof ParameterizedType) {
                final Type[] typeArguments = ((ParameterizedType) fieldGenericType).getActualTypeArguments();
                if (typeArguments != null && typeArguments.length > 0) {
                    final Class<?> elementType = DfReflectionUtil.getGenericFirstClass(typeArguments[0]);
                    if (elementType != null && mayBeNestedBeanType(elementType)) {
                        // e.g. public LandBean<List<PiariBean>> landBean;
                        pathDeque.push("@generic");
                        doCheckNestedFormPropertyValidationMismatch(property, elementType, pathDeque, checkedTypeSet);
                        pathDeque.pop();
                    }
                }
            }
        }
    }

    protected void doCheckNestedFormPropertyValidationMismatch(ActionFormProperty property, Class<?> beanType, Deque<String> pathDeque,
            Set<Class<?>> checkedTypeSet) {
        if (checkedTypeSet.contains(beanType)) {
            return;
        }
        checkedTypeSet.add(beanType);
        final BeanDesc nestedDesc = BeanDescFactory.getBeanDesc(beanType);
        final int nestedPdSize = nestedDesc.getPropertyDescSize();
        for (int i = 0; i < nestedPdSize; i++) {
            final PropertyDesc pd = nestedDesc.getPropertyDesc(i);
            final Field nestedField = pd.getField();
            if (nestedField != null) {
                doCheckFormPropertyValidationMismatch(property, nestedField, pathDeque, checkedTypeSet);
            }
        }
    }

    // -----------------------------------------------------
    //                                     NotNull, NotEmpty
    //                                     -----------------
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
                || isCollectionFamilyType(fieldType); // List, Set, Map, Array, ...
    }

    protected void throwExecuteMethodFormPropertyNotEmptyValidationMismatchException(ActionFormProperty property, Field field,
            Deque<String> pathDeque, Class<? extends Annotation> annotation) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Mismatch validator annotation e.g. NotEmpty for form property type.");
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
        br.addItem("Root Property");
        br.addElement(property);
        br.addItem("Target Field");
        if (pathDeque.size() >= 2) {
            br.addElement("propertyPath: " + buildPushedOrderPathExp(pathDeque));
        }
        br.addElement(buildFieldExp(field));
        br.addItem("Mismatch Annotation");
        br.addElement(annotation);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodFormPropertyValidationMismatchException(msg);
    }

    // -----------------------------------------------------
    //                                        Primitive Type
    //                                        --------------
    protected boolean isFormPropertyCannotRequiredPrimitiveType(final Class<?> fieldType) {
        return fieldType.isPrimitive() && !fieldType.isArray(); // except array just in case
    }

    protected void throwExecuteMethodFormPropertyPrimitiveValidationMismatchException(ActionFormProperty property, Field field,
            Deque<String> pathDeque, Class<? extends Annotation> annotation) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Mismatch validator annotation for form property of primitive type.");
        br.addItem("Advice");
        br.addElement("Primitive types e.g. int, long, boolean");
        br.addElement("cannot use e.g. @Required, @NotNull");
        br.addElement("so use Wrapper types e.g. Integer, Long, Boolean.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    @Required");
        br.addElement("    public int memberId; // *Bad: use Integer");
        br.addElement("    @Required");
        br.addElement("    public boolean paymentComplete; // *Bad: use Boolean");
        br.addElement("  (o):");
        br.addElement("    @Required");
        br.addElement("    public Integer memberId; // Good");
        br.addElement("    @Required");
        br.addElement("    public Boolean paymentComplete; // Good");
        br.addElement("");
        br.addElement("If primitive types, @Required cannot detect no value,");
        br.addElement("e.g. front-side misspelling, treated as default value.");
        br.addElement("So wrapper types (also Boolean) are recommended as property.");
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        br.addItem("Action Form");
        br.addElement(formMeta);
        br.addItem("Root Property");
        br.addElement(property);
        br.addItem("Target Field");
        if (pathDeque.size() >= 2) {
            br.addElement("propertyPath: " + buildPushedOrderPathExp(pathDeque));
        }
        br.addElement(buildFieldExp(field));
        br.addItem("Mismatch Annotation");
        br.addElement(annotation);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodFormPropertyValidationMismatchException(msg);
    }

    // -----------------------------------------------------
    //                                       Valid Annotated
    //                                       ---------------
    protected void checkNestedBeanValidAnnotaed() {
        formMeta.ifPresent(meta -> {
            final Set<Class<?>> checkedTypeSet = new HashSet<Class<?>>();
            checkedTypeSet.add(meta.getFormType());
            meta.properties().forEach(property -> {
                final Field field = property.getPropertyDesc().getField();
                if (field != null) { // only field property
                    final Class<?> fieldType = field.getType();
                    final Deque<String> pathDeque = new LinkedList<String>();
                    doCheckNestedBeanValidAnnotaed(field, fieldType, pathDeque, checkedTypeSet);
                }
            });
        });
    }

    protected void doCheckNestedBeanValidAnnotaed(Field field, Class<?> fieldType, Deque<String> pathDeque, Set<Class<?>> checkedTypeSet) {
        pathDeque.push(field.getName());
        if (Collection.class.isAssignableFrom(fieldType)) { // only collection, except array and map, simply
            final Class<?> genericType = getGenericType(field);
            if (genericType != null && mayBeNestedBeanType(genericType)) {
                if (checkedTypeSet.contains(fieldType)) {
                    return;
                }
                checkedTypeSet.add(fieldType);
                pathDeque.push("@generic");
                detectLonelyNestedBean(field, genericType, Collections.emptyMap(), pathDeque, checkedTypeSet);
                pathDeque.pop();
            }
        } else if (mayBeNestedBeanType(fieldType)) { // single bean
            if (checkedTypeSet.contains(fieldType)) {
                return;
            }
            checkedTypeSet.add(fieldType);
            // can check public LandBean<List<PiariBean>> landBean;
            final Class<?> genericType = getGenericType(field);
            final Map<String, Class<?>> genericMap;
            if (genericType != null && mayBeNestedBeanType(genericType)) {
                genericMap = new LinkedHashMap<String, Class<?>>(1); // only first generic #for_now
                genericMap.put(field.getType().getTypeParameters()[0].getName(), genericType); // e.g. HAUNTED = PiariBean.class
            } else {
                genericMap = Collections.emptyMap();
            }
            detectLonelyNestedBean(field, fieldType, genericMap, pathDeque, checkedTypeSet);
        }
        pathDeque.pop();
    }

    protected void detectLonelyNestedBean(Field field, Class<?> beanType, Map<String, Class<?>> genericMap, Deque<String> pathDeque,
            Set<Class<?>> checkedTypeSet) {
        final boolean hasNestedBeanAnnotation = hasNestedBeanAnnotation(field);
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(beanType);
        for (int i = 0; i < beanDesc.getFieldSize(); i++) {
            final Field nestedField = beanDesc.getField(i);
            if (!hasNestedBeanAnnotation) {
                for (Annotation anno : nestedField.getAnnotations()) {
                    if (isValidatorAnnotation(anno.annotationType())) {
                        throwLonelyValidatorAnnotationException(field, nestedField, pathDeque); // only first level
                    }
                }
            }
            final Class<?> nestedFieldType;
            final Type definedGenericType = nestedField.getGenericType();
            if (definedGenericType != null && !genericMap.isEmpty()) { // e.g. public HAUNTED haunted;
                final Class<?> translatedType = genericMap.get(definedGenericType.getTypeName()); // e.g. PiariBean by HAUNTED
                nestedFieldType = translatedType != null ? translatedType : nestedField.getType();
            } else {
                nestedFieldType = nestedField.getType();
            }
            doCheckNestedBeanValidAnnotaed(nestedField, nestedFieldType, pathDeque, checkedTypeSet);
        }
    }

    protected boolean hasNestedBeanAnnotation(Field field) {
        return ActionValidator.hasNestedBeanAnnotation(field);
    }

    protected void throwLonelyValidatorAnnotationException(Field goofyField, Field lonelyField, Deque<String> pathDeque) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Lonely validator annotations, so add Valid annotation.");
        br.addItem("Adivce");
        br.addElement("When any property in nested bean has validator annotations,");
        br.addElement("The field for nested bean should have the Valid annotation.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public class SeaForm {");
        br.addElement("        public LandBean land; // *Bad: no annotation");
        br.addElement("");
        br.addElement("        public static class LandBean {");
        br.addElement("            @Required");
        br.addElement("            public String piari;");
        br.addElement("        }");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public class SeaForm {");
        br.addElement("        @Valid                // Good: javax.validation");
        br.addElement("        public LandBean land;");
        br.addElement("");
        br.addElement("        public static class LandBean {");
        br.addElement("            @Required");
        br.addElement("            public String piari;");
        br.addElement("        }");
        br.addElement("    }");
        br.addItem("Action Execute");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        br.addItem("Field that needs Valid annotation");
        if (pathDeque.size() >= 2) {
            br.addElement("propertyPath: " + buildPushedOrderPathExp(pathDeque));
        }
        br.addElement(buildFieldExp(goofyField));
        br.addItem("Lonely Field");
        br.addElement(Arrays.asList(lonelyField.getAnnotations()).stream().map(anno -> {
            return "@" + anno.annotationType().getSimpleName();
        }).collect(Collectors.joining(" "))); // has at least one annotation
        br.addElement(buildFieldExp(lonelyField));
        final String msg = br.buildExceptionMessage();
        throw new LonelyValidatorAnnotationException(msg);
    }

    protected boolean isValidatorAnnotation(Class<?> annoType) {
        return ActionValidator.isValidatorAnnotation(annoType);
    }

    // ===================================================================================
    //                                                                      Check Optional
    //                                                                      ==============
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
    //                                                                        Assist Logic
    //                                                                        ============
    protected boolean mayBeNestedBeanType(Class<?> fieldType) {
        return !fieldType.isPrimitive() // not primitive types
                && !fieldType.isArray() // not array type, array's nested bean is unsupported for the check, simply
                && !Object.class.equals(fieldType) // e.g. generic type
                && !String.class.isAssignableFrom(fieldType) // not String
                && !Number.class.isAssignableFrom(fieldType) // not Integer, Long, ...
                && !TemporalAccessor.class.isAssignableFrom(fieldType) // not LocalDate, ...
                && !java.util.Date.class.isAssignableFrom(fieldType) // not old date
                && !Boolean.class.isAssignableFrom(fieldType) && !boolean.class.equals(fieldType) // not boolean
                && !Classification.class.isAssignableFrom(fieldType) // not CDef
                && !isCollectionFamilyType(fieldType); // not Collection family
    }

    protected boolean isCollectionFamilyType(Class<?> fieldType) {
        return Collection.class.isAssignableFrom(fieldType) // List, Set, ...
                || Map.class.isAssignableFrom(fieldType) // mainly used in JSON
                || Object[].class.isAssignableFrom(fieldType); // also all arrays
    }

    protected Class<?> getGenericType(Field field) {
        final Type genericType = field.getGenericType();
        return genericType != null ? DfReflectionUtil.getGenericFirstClass(genericType) : null;
    }

    protected String buildPushedOrderPathExp(Deque<String> pathDeque) {
        final List<String> pathList = pathDeque.stream().collect(Collectors.toList());
        Collections.reverse(pathList);
        return pathList.stream().collect(Collectors.joining("."));
    }

    protected String buildFieldExp(Field field) {
        final StringBuilder sb = new StringBuilder();
        sb.append(field.getDeclaringClass().getSimpleName());
        final Type genericType = field.getGenericType();
        final String typeExp = genericType != null ? genericType.getTypeName() : field.getType().getSimpleName();
        sb.append("@").append(field.getName()).append(": ").append(typeExp);
        final Class<?> genericBeanType = getGenericType(field);
        sb.append(genericBeanType != null ? "<" + genericBeanType.getSimpleName() + ">" : "");
        return sb.toString();
    }
}
