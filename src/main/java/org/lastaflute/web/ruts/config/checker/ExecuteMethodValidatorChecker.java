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
package org.lastaflute.web.ruts.config.checker;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.jdbc.Classification;
import org.dbflute.util.DfReflectionUtil;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.PropertyDesc;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;
import org.lastaflute.web.exception.ExecuteMethodLonelyValidatorAnnotationException;
import org.lastaflute.web.exception.ExecuteMethodValidatorAnnotationMismatchedException;
import org.lastaflute.web.util.LaActionExecuteUtil;
import org.lastaflute.web.validation.ActionValidator;
import org.lastaflute.web.validation.Required;

/**
 * @author modified by jflute (originated in Sessar) at Miguel's El Dorado Cantina in Lost River
 */
public class ExecuteMethodValidatorChecker implements Serializable {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = 1L;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Method executeMethod; // not null
    protected final Supplier<Map<String, Object>> baseInfoMapSupplier; // not null
    protected final Deque<String> pathDeque; // not null, stateful
    protected final Set<Class<?>> checkedTypeSet; // not null, stateful

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ExecuteMethodValidatorChecker(Method executeMethod, Supplier<Map<String, Object>> baseInfoMapSupplier, Deque<String> pathDeque,
            Set<Class<?>> checkedTypeSet) {
        this.executeMethod = executeMethod;
        this.baseInfoMapSupplier = baseInfoMapSupplier;
        this.pathDeque = pathDeque;
        this.checkedTypeSet = checkedTypeSet;
    }

    // ===================================================================================
    //                                                     Mismatched Validator Annotation
    //                                                     ===============================
    public void checkMismatchedValidatorAnnotation(Field field, Map<String, Class<?>> genericMap) {
        doCheckMismatchedValidatorAnnotation(field, genericMap);
    }

    protected void doCheckMismatchedValidatorAnnotation(Field field, Map<String, Class<?>> genericMap) { // recursive point
        pathDeque.push(field.getName());
        checkedTypeSet.add(field.getDeclaringClass());
        final Class<?> fieldType = deriveFieldType(field, genericMap);
        // *depends on JSON rule so difficult, check only physical mismatch here
        //if (isFormPropertyCannotNotNullType(fieldType)) {
        //    final Class<NotNull> notNullType = NotNull.class;
        //    if (field.getAnnotation(notNullType) != null) {
        //        throwExecuteMethodFormPropertyValidationMismatchException(property, notNullType);
        //    }
        //}
        if (isCannotNotEmptyType(fieldType)) {
            final Class<NotEmpty> notEmptyType = NotEmpty.class;
            if (field.getAnnotation(notEmptyType) != null) {
                throwExecuteMethodNotEmptyValidationMismatchException(field, notEmptyType);
            }
        }
        if (isCannotNotBlankType(fieldType)) {
            final Class<NotBlank> notBlankType = NotBlank.class;
            if (field.getAnnotation(notBlankType) != null) {
                throwExecuteMethodNotEmptyValidationMismatchException(field, notBlankType);
            }
        }
        if (isFormPropertyCannotRequiredPrimitiveType(fieldType)) {
            final Class<Required> requiredType = Required.class;
            if (field.getAnnotation(requiredType) != null) {
                throwExecuteMethodPrimitiveValidationMismatchException(field, requiredType);
            }
            final Class<NotNull> notNullType = NotNull.class;
            if (field.getAnnotation(notNullType) != null) {
                throwExecuteMethodPrimitiveValidationMismatchException(field, notNullType);
            }
        }
        if (Collection.class.isAssignableFrom(fieldType)) { // only collection, except array and map, simply
            doCheckGenericBeanValidationMismatch(field);
        } else if (mayBeNestedBeanType(fieldType)) {
            doCheckNestedValidationMismatch(fieldType);
            doCheckGenericBeanValidationMismatch(field);
        }
        pathDeque.pop();
    }

    // -----------------------------------------------------
    //                                           Nested Bean
    //                                           -----------
    protected void doCheckGenericBeanValidationMismatch(Field field) {
        // #hope cannot check now: public List<ROOM> roomList;
        final Class<?> genericType = getFieldGenericType(field); // only first generic #for_now
        if (genericType == null) {
            return;
        }
        if (mayBeNestedBeanType(genericType)) { // e.g. LandBean<PiariBean>
            pathDeque.push("@generic");
            doCheckNestedValidationMismatch(genericType);
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
                        doCheckNestedValidationMismatch(elementType);
                        pathDeque.pop();
                    }
                }
            }
        }
    }

    protected void doCheckNestedValidationMismatch(Class<?> beanType) {
        if (checkedTypeSet.contains(beanType)) {
            return; // cannot check #for_now: SeaBean recursiveBean; in SeaBean
        }
        checkedTypeSet.add(beanType);
        final BeanDesc nestedDesc = BeanDescFactory.getBeanDesc(beanType);
        final int nestedPdSize = nestedDesc.getPropertyDescSize();
        for (int i = 0; i < nestedPdSize; i++) {
            final PropertyDesc pd = nestedDesc.getPropertyDesc(i);
            final Field nestedField = pd.getField();
            if (nestedField != null) {
                doCheckMismatchedValidatorAnnotation(nestedField, Collections.emptyMap()); // recursive call
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

    protected boolean isCannotNotEmptyType(Class<?> fieldType) {
        return Number.class.isAssignableFrom(fieldType) // Integer, Long, ...
                || int.class.equals(fieldType) || long.class.equals(fieldType) // primitive numbers
                || TemporalAccessor.class.isAssignableFrom(fieldType) // LocalDate, ...
                || java.util.Date.class.isAssignableFrom(fieldType) // old date
                || Boolean.class.isAssignableFrom(fieldType) || boolean.class.equals(fieldType) // boolean
                || Classification.class.isAssignableFrom(fieldType); // CDef
    }

    protected boolean isCannotNotBlankType(Class<?> fieldType) {
        return isCannotNotEmptyType(fieldType) // cannot-NotEmpty types are also
                || isCollectionFamilyType(fieldType); // List, Set, Map, Array, ...
    }

    protected void throwExecuteMethodNotEmptyValidationMismatchException(Field field, Class<? extends Annotation> annotation) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Mismatch validator annotation e.g. NotEmpty for property type.");
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
        baseInfoMapSupplier.get().forEach((key, value) -> {
            br.addItem(key);
            br.addElement(value);
        });
        br.addItem("Target Field");
        if (pathDeque.size() >= 2) {
            br.addElement("propertyPath: " + buildPushedOrderPathExp(pathDeque));
        }
        br.addElement(buildFieldExp(field));
        br.addItem("Mismatch Annotation");
        br.addElement(annotation);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodValidatorAnnotationMismatchedException(msg);
    }

    // -----------------------------------------------------
    //                                        Primitive Type
    //                                        --------------
    protected boolean isFormPropertyCannotRequiredPrimitiveType(final Class<?> fieldType) {
        return fieldType.isPrimitive() && !fieldType.isArray(); // except array just in case
    }

    protected void throwExecuteMethodPrimitiveValidationMismatchException(Field field, Class<? extends Annotation> annotation) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Mismatched validator annotation for primitive type property.");
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
        baseInfoMapSupplier.get().forEach((key, value) -> {
            br.addItem(key);
            br.addElement(value);
        });
        br.addItem("Target Field");
        if (pathDeque.size() >= 2) {
            br.addElement("propertyPath: " + buildPushedOrderPathExp(pathDeque));
        }
        br.addElement(buildFieldExp(field));
        br.addItem("Mismatch Annotation");
        br.addElement(annotation);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodValidatorAnnotationMismatchedException(msg);
    }

    // ===================================================================================
    //                                                         Lonely Validator Annotation
    //                                                         ===========================
    public void checkLonelyValidatorAnnotation(Field field, Map<String, Class<?>> genericMap) {
        doCheckLonelyValidatorAnnotation(field, deriveFieldType(field, genericMap));
    }

    protected void doCheckLonelyValidatorAnnotation(Field field, Class<?> fieldType) { // recursive point
        pathDeque.push(field.getName());
        if (Collection.class.isAssignableFrom(fieldType)) { // only collection, except array and map, simply
            final Class<?> genericType = getFieldGenericType(field);
            if (genericType != null && mayBeNestedBeanType(genericType)) {
                if (checkedTypeSet.contains(fieldType)) {
                    return; // cannot check #for_now: SeaBean recursiveBean; in SeaBean
                }
                checkedTypeSet.add(fieldType);
                pathDeque.push("@generic");
                detectLonelyAnnotatedField(field, genericType, Collections.emptyMap(), pathDeque, checkedTypeSet);
                pathDeque.pop();
            }
        } else if (mayBeNestedBeanType(fieldType)) { // single bean
            if (checkedTypeSet.contains(fieldType)) {
                return; // cannot check #for_now: SeaBean recursiveBean; in SeaBean
            }
            checkedTypeSet.add(fieldType);
            // can check: LandBean<PiariBean> landBean; LandBean<List<PiariBean>> landBean;
            final Class<?> genericType = getFieldGenericType(field);
            final Map<String, Class<?>> genericMap;
            if (genericType != null && mayBeNestedBeanType(genericType)) {
                genericMap = new LinkedHashMap<String, Class<?>>(1); // only first generic #for_now
                genericMap.put(field.getType().getTypeParameters()[0].getName(), genericType); // e.g. HAUNTED = PiariBean.class
            } else {
                genericMap = Collections.emptyMap();
            }
            detectLonelyAnnotatedField(field, fieldType, genericMap, pathDeque, checkedTypeSet);
        }
        pathDeque.pop();
    }

    protected void detectLonelyAnnotatedField(Field field, Class<?> beanType, Map<String, Class<?>> genericMap, Deque<String> pathDeque,
            Set<Class<?>> checkedTypeSet) {
        final boolean hasNestedBeanAnnotation = hasNestedBeanAnnotation(field);
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(beanType);
        for (int i = 0; i < beanDesc.getFieldSize(); i++) {
            final Field nestedField = beanDesc.getField(i);
            if (!hasNestedBeanAnnotation) {
                for (Annotation anno : nestedField.getAnnotations()) {
                    if (isValidatorAnnotation(anno.annotationType())) {
                        throwLonelyValidatorAnnotationException(field, nestedField); // only first level
                    }
                }
            }
            doCheckLonelyValidatorAnnotation(nestedField, deriveFieldType(nestedField, genericMap)); // recursive call
        }
    }

    protected boolean hasNestedBeanAnnotation(Field field) {
        return ActionValidator.hasNestedBeanAnnotation(field);
    }

    protected void throwLonelyValidatorAnnotationException(Field goofyField, Field lonelyField) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Lonely validator annotations, so add Valid annotation.");
        br.addItem("Adivce");
        br.addElement("When any property in nested bean has validator annotations,");
        br.addElement("The field for nested bean should have the Valid annotation.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public class SeaBean {");
        br.addElement("        public LandBean land; // *Bad: no annotation");
        br.addElement("");
        br.addElement("        public static class LandBean {");
        br.addElement("            @Required");
        br.addElement("            public String piari;");
        br.addElement("        }");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public class SeaBean {");
        br.addElement("        @Valid");
        br.addElement("        public LandBean land; // Good: javax.validation");
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
        throw new ExecuteMethodLonelyValidatorAnnotationException(msg);
    }

    protected boolean isValidatorAnnotation(Class<?> annoType) {
        return ActionValidator.isValidatorAnnotation(annoType);
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
                && !Boolean.class.isAssignableFrom(fieldType) // not boolean
                && !Classification.class.isAssignableFrom(fieldType) // not CDef
                && !isCollectionFamilyType(fieldType); // not Collection family
    }

    protected boolean isCollectionFamilyType(Class<?> fieldType) {
        return Collection.class.isAssignableFrom(fieldType) // List, Set, ...
                || Map.class.isAssignableFrom(fieldType) // mainly used in JSON
                || Object[].class.isAssignableFrom(fieldType); // also all arrays
    }

    protected Class<?> deriveFieldType(Field field, Map<String, Class<?>> genericMap) {
        final Class<?> fieldType;
        final Type genericType = field.getGenericType();
        if (genericType != null && !genericMap.isEmpty()) { // e.g. public HAUNTED haunted;
            final Class<?> translatedType = genericMap.get(genericType.getTypeName()); // e.g. PiariBean by HAUNTED
            fieldType = translatedType != null ? translatedType : field.getType();
        } else {
            fieldType = field.getType();
        }
        return fieldType;
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
        final Class<?> genericBeanType = getFieldGenericType(field);
        sb.append(genericBeanType != null ? "<" + genericBeanType.getSimpleName() + ">" : "");
        return sb.toString();
    }

    protected Class<?> getFieldGenericType(Field field) {
        final Type genericType = field.getGenericType();
        return genericType != null ? DfReflectionUtil.getGenericFirstClass(genericType) : null;
    }
}
