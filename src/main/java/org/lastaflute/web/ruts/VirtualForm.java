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
package org.lastaflute.web.ruts;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.Srl;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.PropertyDesc;
import org.lastaflute.di.helper.beans.exception.BeanPropertyNotFoundException;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;
import org.lastaflute.web.exception.FormPropertyNotFoundException;
import org.lastaflute.web.ruts.config.ActionFormMeta;
import org.lastaflute.web.ruts.config.ActionFormProperty;
import org.lastaflute.web.ruts.wrapper.BeanWrapper;
import org.lastaflute.web.util.LaParamWrapperUtil;

/**
 * @author jflute
 */
public class VirtualForm implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RealFormSupplier formSupplier; // not null
    protected final ActionFormMeta formMeta; // not null
    protected Object realForm; // lazy loaded if exists
    protected Map<String, Object> typeFailureMap; // lazy loaded if exists

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public VirtualForm(RealFormSupplier formSupplier, ActionFormMeta formMeta) {
        this.formSupplier = formSupplier;
        this.formMeta = formMeta;
    }

    @FunctionalInterface
    public static interface RealFormSupplier {
        Object supply();
    }

    // ===================================================================================
    //                                                                Instantiate RealForm
    //                                                                ====================
    protected void instantiateRealForm() { // mainly here, called from getter
        checkAlreadyExistsRealForm();
        realForm = formSupplier.supply();
    }

    public void acceptRealForm(Object realForm) { // e.g. JSON mapping
        checkRealFormExistence(realForm);
        checkAlreadyExistsRealForm();
        this.realForm = realForm;
    }

    protected void checkRealFormExistence(Object realForm) {
        if (realForm == null) { // basically no way, JsonManager cannot return null, but just in case
            String msg = "Not found the real form to be accepted to virtual form: virtual=" + toString();
            throw new IllegalArgumentException(msg);
        }
    }

    protected void checkAlreadyExistsRealForm() {
        if (realForm != null) {
            throw new IllegalStateException("Alraedy exists the real form: " + realForm);
        }
    }

    // ===================================================================================
    //                                                                      Property Value
    //                                                                      ==============
    /**
     * @param propertyName The name of the property of the action form. (NotNull)
     * @return The property value from the form. (NullAllowed: if the property value is null)
     * @throws FormPropertyNotFoundException When the property is not found by the name.
     */
    public Object getPropertyValue(String propertyName) {
        if (propertyName.contains(".")) {
            return traceChainedPropertyValue(propertyName);
        } else { // mainly here
            return getPropertyValue(findProperty(propertyName));
        }
    }

    /**
     * @param property The property of the form. (NotNull)
     * @return The property value from the form. (NullAllowed: if the property value is null)
     */
    public Object getPropertyValue(ActionFormProperty property) {
        return findPropertyValue(property, getRealForm());
    }

    // -----------------------------------------------------
    //                                      Chained Property
    //                                      ----------------
    protected Object traceChainedPropertyValue(String chainedName) {
        final Object failureValue = getTypeFailureMap().get(chainedName);
        if (failureValue != null) {
            return failureValue;
        }
        final String firstName = Srl.substringFirstFront(chainedName, ".");
        final String nestedChain = Srl.substringFirstRear(chainedName, ".");
        // trace by only defined type,
        // instance of properties are created as (almost) defined type by framework
        // and you can check definition of nested property
        final ActionFormProperty property = findProperty(firstName);
        final List<String> nestedList = Srl.splitList(nestedChain, ".");
        Object currentObj = getPropertyValue(property);
        Class<?> currentType = property.getPropertyDesc().getPropertyType();
        Integer arrayIndex = extractArrayIndexIfExists(firstName);
        for (String nested : nestedList) {
            // no quit if value is null to check definition of nested property
            if (List.class.isAssignableFrom(currentType)) { // sea[0].dockside[1].waves
                currentObj = currentObj != null ? ((List<?>) currentObj).get(arrayIndex) : null;
                if (currentObj != null) {
                    currentType = currentObj.getClass();
                } else {
                    break; // cannot get type so cannot continue
                }
            }
            if (Map.class.isAssignableFrom(currentType)) {
                currentObj = currentObj != null ? ((Map<?, ?>) currentObj).get(nested) : null;
                currentType = currentObj != null ? currentObj.getClass() : null;
                if (currentObj != null) {
                    currentType = currentObj.getClass();
                } else {
                    break; // cannot get type so cannot continue
                }
            } else {
                final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(currentType);
                final PropertyDesc pd;
                try {
                    pd = beanDesc.getPropertyDesc(nested); // check here
                } catch (BeanPropertyNotFoundException e) {
                    throwNestedFormPropertyNotFoundException(chainedName, nested, e);
                    return null; // unreachable
                }
                if (currentObj != null) {
                    if (currentObj instanceof BeanWrapper) {
                        currentObj = ((BeanWrapper) currentObj).get(nested);
                    } else {
                        currentObj = pd.getValue(currentObj);
                    }
                }
                currentType = pd.getPropertyType();
            }
            arrayIndex = extractArrayIndexIfExists(nested);
        }
        return currentObj;
    }

    protected Integer extractArrayIndexIfExists(final String firstName) {
        final Integer rootArrayIndex;
        if (firstName.contains("[") && firstName.endsWith("]")) {
            rootArrayIndex = Integer.valueOf(Srl.extractScopeWide(firstName, "[", "]").getContent());
        } else {
            rootArrayIndex = null;
        }
        return rootArrayIndex;
    }

    protected void throwNestedFormPropertyNotFoundException(String propertyName, String nestedProp, BeanPropertyNotFoundException cause) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the nested property of the form.");
        br.addItem("Form Meta");
        br.addElement(formMeta);
        br.addItem("Form Instance");
        br.addElement(realForm);
        br.addItem("Chained Property");
        br.addElement(propertyName);
        br.addItem("NotFound Property");
        br.addElement(nestedProp);
        final String msg = br.buildExceptionMessage();
        throw new FormPropertyNotFoundException(msg, cause);
    }

    // ===================================================================================
    //                                                                       Find Property
    //                                                                       =============
    protected ActionFormProperty findProperty(String propertyName) {
        final ActionFormProperty property = formMeta.getProperty(propertyName);
        if (property == null) {
            throwFormPropertyNotFoundException(propertyName);
        }
        return property;
    }

    protected void throwFormPropertyNotFoundException(String propertyName) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the property of the form.");
        br.addItem("Form Meta");
        br.addElement(formMeta);
        br.addItem("Form Instance");
        br.addElement(realForm);
        br.addItem("NotFound Property");
        br.addElement(propertyName);
        final String msg = br.buildExceptionMessage();
        throw new FormPropertyNotFoundException(msg);
    }

    /**
     * @param property The property of form. (NotNull)
     * @param bean The instance of the action form. (NotNull)
     * @return The property value from the form. (NullAllowed)
     */
    protected Object findPropertyValue(ActionFormProperty property, Object bean) {
        assertArgumentNotNull("property", property);
        final Object failureValue = getTypeFailureMap().get(property.getPropertyName());
        if (failureValue != null) {
            return failureValue;
        }
        return LaParamWrapperUtil.convert(property.getPropertyDesc().getValue(bean));
    }

    // ===================================================================================
    //                                                                        Type Failure
    //                                                                        ============
    /**
     * @param propertyName The name of property as saving key, may be chained. (NotNull)
     * @param failureValue The value as type failure. (NullAllowed: but basically no way)
     */
    public void acceptTypeFailure(String propertyName, Object failureValue) {
        assertArgumentNotNull("propertyName", failureValue);
        initializeTypeFailureMapIfNeeds();
        typeFailureMap.put(propertyName, failureValue);
    }

    protected void initializeTypeFailureMapIfNeeds() {
        if (typeFailureMap == null) {
            typeFailureMap = new LinkedHashMap<String, Object>();
        }
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            throw new IllegalArgumentException("The variableName should not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
        }
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final String title = DfTypeUtil.toClassTitle(this);
        final String hash = Integer.toHexString(hashCode());
        return title + ":{" + formMeta + ", realForm=" + realForm + "}@" + hash;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public RealFormSupplier getFormSupplier() {
        return formSupplier;
    }

    public ActionFormMeta getFormMeta() {
        return formMeta;
    }

    public Object getRealForm() { // not null
        if (realForm == null) {
            instantiateRealForm(); // on demand here
        }
        return realForm;
    }

    public Map<String, Object> getTypeFailureMap() { // not null
        return typeFailureMap != null ? Collections.unmodifiableMap(typeFailureMap) : Collections.emptyMap();
    }
}