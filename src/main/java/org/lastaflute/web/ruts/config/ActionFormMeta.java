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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.Srl;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.PropertyDesc;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;
import org.lastaflute.web.exception.ActionFormCreateFailureException;
import org.lastaflute.web.ruts.VirtualForm;
import org.lastaflute.web.ruts.VirtualForm.RealFormSupplier;
import org.lastaflute.web.util.LaActionExecuteUtil;
import org.lastaflute.web.validation.ActionValidator;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class ActionFormMeta {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ActionExecute execute; // not null
    protected final String formKey; // not null
    protected final Class<?> rootFormType; // not null, e.g. SeaForm or SeaBody or java.util.List
    protected final OptionalThing<Parameter> listFormParameter; // not null, empty allowed
    protected final OptionalThing<Consumer<Object>> formSetupper; // not null, empty allowed
    protected final boolean jsonBodyMapping;
    protected final Map<String, ActionFormProperty> propertyMap; // not null
    protected final boolean validatorAnnotated;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionFormMeta(ActionExecute execute, String formKey, Class<?> rootFormType // required
            , OptionalThing<Parameter> listFormParameter, OptionalThing<Consumer<Object>> formSetupper, boolean jsonBodyMapping) {
        this.execute = execute;
        this.formKey = formKey;
        this.rootFormType = rootFormType;
        this.listFormParameter = listFormParameter;
        this.formSetupper = formSetupper;
        this.jsonBodyMapping = jsonBodyMapping;
        this.propertyMap = setupProperties(rootFormType);
        this.validatorAnnotated = mightBeValidatorAnnotated();
    }

    protected Map<String, ActionFormProperty> setupProperties(Class<?> formType) {
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(formType);
        final int propertyDescSize = beanDesc.getPropertyDescSize();
        final Map<String, ActionFormProperty> map = new HashMap<String, ActionFormProperty>(propertyDescSize);
        for (int i = 0; i < propertyDescSize; i++) {
            final PropertyDesc pd = beanDesc.getPropertyDesc(i);
            if (pd.isReadable()) {
                final ActionFormProperty property = newActionFormProperty(pd);
                addProperty(map, property);
            }
        }
        return map;
    }

    protected ActionFormProperty newActionFormProperty(PropertyDesc pd) {
        return new ActionFormProperty(pd);
    }

    protected void addProperty(Map<String, ActionFormProperty> map, ActionFormProperty property) {
        map.put(property.getPropertyName(), property);
    }

    // ===================================================================================
    //                                                                  Validator Handling
    //                                                                  ==================
    protected boolean mightBeValidatorAnnotated() {
        for (ActionFormProperty property : propertyMap.values()) {
            final Field field = property.getPropertyDesc().getField();
            if (field == null) { // not field property
                continue;
            }
            for (Annotation anno : field.getAnnotations()) {
                if (isValidatorAnnotation(anno.annotationType())) {
                    return true; // first level only
                }
            }
        }
        return false;
    }

    protected boolean isValidatorAnnotation(Class<?> annoType) {
        return ActionValidator.isValidatorAnnotation(annoType);
    }

    // ===================================================================================
    //                                                                   Property Handling
    //                                                                   =================
    public Collection<ActionFormProperty> properties() {
        return propertyMap.values();
    }

    /**
     * Does the form have the property?
     * @param propertyName The name of property to find, filtered about '.' and []. (NotNull)
     * @return The determination, true or false.
     */
    public boolean hasProperty(String propertyName) {
        return getProperty(propertyName) != null;
    }

    /**
     * @param propertyName The name of property to find, filtered about '.' and []. (NotNull)
     * @return The property of action form. (NullAllowed: when not found)
     */
    public ActionFormProperty getProperty(String propertyName) {
        return propertyMap.get(filterPropertyNameToFind(propertyName));
    }

    protected String filterPropertyNameToFind(String propertyName) {
        final String firstName = Srl.substringFirstFront(propertyName, "."); // first element if dot chain
        final String realName;
        if (firstName.contains("[") && firstName.endsWith("]")) {
            realName = Srl.substringFirstFront(firstName, "[");
        } else {
            realName = firstName;
        }
        return realName;
    }

    // ===================================================================================
    //                                                                        Virtual Form
    //                                                                        ============
    public VirtualForm createActionForm() {
        return newVirtualActionForm(getActionFormSupplier(), this);
    }

    protected VirtualForm newVirtualActionForm(RealFormSupplier formSupplier, ActionFormMeta formMeta) {
        return new VirtualForm(formSupplier, formMeta);
    }

    protected RealFormSupplier getActionFormSupplier() {
        return () -> {
            try {
                checkInstantiatedFormType();
                final Object formInstance = rootFormType.newInstance();
                formSetupper.ifPresent(setupper -> setupper.accept(formInstance));
                return formInstance;
            } catch (Exception e) {
                throwActionFormCreateFailureException(e);
                return null; // unreachable
            }
        };
    }

    protected void checkInstantiatedFormType() {
        if (List.class.isAssignableFrom(rootFormType)) { // e.g. List<SeaForm>, JSON body of list type
            String msg = "Cannot instantiate the form because of list type, should not come here:" + rootFormType;
            throw new IllegalStateException(msg);
        }
    }

    protected void throwActionFormCreateFailureException(Exception cause) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Failed to create the action form (or body) for the type.");
        br.addItem("Advice");
        br.addElement("Confirm your action form definition,");
        br.addElement("e.g. action form should be located under 'web' package");
        br.addElement("and the name should end with 'Form' or 'Body'.");
        if (LaActionExecuteUtil.hasActionExecute()) { // just in case
            br.addItem("Action Execute");
            br.addElement(LaActionExecuteUtil.getActionExecute());
        }
        br.addItem("Form Type");
        br.addElement(rootFormType);
        final String msg = br.buildExceptionMessage();
        throw new ActionFormCreateFailureException(msg, cause);
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("formMeta:{").append(formKey);
        sb.append(", ").append(listFormParameter.map(pm -> {
            return pm.getParameterizedType().getTypeName();
        }).orElse(rootFormType.getName()));
        sb.append(", props=").append(propertyMap.size());
        sb.append("}");
        return sb.toString();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    // -----------------------------------------------------
    //                                                 Basic
    //                                                 -----
    /**
     * @return The unique key of action form. (NotNull) 
     */
    public String getFormKey() {
        return formKey;
    }

    /**
     * @return The type of action form as root parameter type. (NotNull)
     * @deprecated use getRootFormType() or getSymbolFormType()
     */
    public Class<?> getFormType() {
        return getRootFormType();
    }

    /**
     * @return The type of action form as root parameter type, may be java.util.List. (NotNull)
     */
    public Class<?> getRootFormType() { // e.g. SeaForm or SeaBody or java.util.List
        return rootFormType;
    }

    /**
     * @return The type of action form as resolved definition location, not java.util.List. (NotNull) 
     */
    public Class<?> getSymbolFormType() { // e.g. SeaForm or "SeaBody of java.util.List<SeaBody>"
        return getListFormParameterGenericType().orElse(rootFormType);
    }

    /**
     * Is the symbol form defined at root parameter?
     * <pre>
     * public HtmlResponse index(SeaForm form) {
     * }
     * 
     * public JsonResponse&lt;...&gt; index(SeaBody body) {
     * }
     * </pre>
     * @return The determination, true or false.
     */
    public boolean isRootSymbolForm() {
        return !isTypedListForm(); // now only list-form pattern exists
    }

    /**
     * Is the form defined as typed list?
     * <pre>
     * public JsonResponse&lt;...&gt; index(List&lt;SeaBody&gt; bodyList) {
     * }
     * </pre>
     * @return The determination, true or false.
     */
    public boolean isTypedListForm() {
        return getListFormParameter().isPresent(); // listFormParameter is present only when list form
    }

    // -----------------------------------------------------
    //                                             List Form
    //                                             ---------
    public OptionalThing<Parameter> getListFormParameter() {
        return listFormParameter;
    }

    public OptionalThing<Class<?>> getListFormParameterGenericType() {
        return listFormParameter.map(pm -> {
            /* always exists, already checked in romantic action customizer */
            return DfReflectionUtil.getGenericFirstClass(pm.getParameterizedType());
        });
    }

    public OptionalThing<ParameterizedType> getListFormParameterParameterizedType() {
        return listFormParameter.map(pm -> {
            /* always parameterized, already checked in romantic action customizer */
            return (ParameterizedType) pm.getParameterizedType();
        });
    }

    // -----------------------------------------------------
    //                                             JSON Body
    //                                             ---------
    public boolean isJsonBodyMapping() {
        return jsonBodyMapping;
    }

    // -----------------------------------------------------
    //                                              Analyzed
    //                                              --------
    public boolean isValidatorAnnotated() {
        return validatorAnnotated;
    }
}