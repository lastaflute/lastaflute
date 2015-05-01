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
package org.dbflute.lastaflute.web.ruts;

import java.io.Serializable;
import java.util.function.Supplier;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.lastaflute.web.exception.FormPropertyNotFoundException;
import org.dbflute.lastaflute.web.ruts.config.ActionFormMeta;
import org.dbflute.lastaflute.web.ruts.config.ActionFormProperty;
import org.dbflute.util.DfTypeUtil;

/**
 * @author modified by jflute (originated in Struts and Seasar)
 */
public class VirtualActionForm implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Supplier<Object> formSupplier;
    protected final ActionFormMeta formMeta;
    protected Object realForm; // lazy loaded

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public VirtualActionForm(Supplier<Object> formSupplier, ActionFormMeta formMeta) {
        this.formSupplier = formSupplier;
        this.formMeta = formMeta;
    }

    // ===================================================================================
    //                                                                Instantiate RealForm
    //                                                                ====================
    public void instantiateRealForm() { // mainly here
        checkAlreadyExistsRealForm();
        realForm = formSupplier.get();
    }

    public void acceptRealForm(Object realForm) { // e.g. JSON mapping
        checkAlreadyExistsRealForm();
        this.realForm = realForm;
    }

    protected void checkAlreadyExistsRealForm() {
        if (realForm != null) {
            throw new IllegalStateException("Alraedy exists the real form: " + realForm);
        }
    }

    // ===================================================================================
    //                                                                        Dynamic Bean
    //                                                                        ============
    /**
     * @param propertyName The name of the property of the action form. (NotNull)
     * @return The property value from the form. (NullAllowed)
     */
    public Object getPropertyValue(String propertyName) {
        return getFormProperty(propertyName).getPropertyValue(getRealForm());
    }

    protected ActionFormProperty getFormProperty(String propertyName) {
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
    public Supplier<Object> getFormSupplier() {
        return formSupplier;
    }

    public ActionFormMeta getFormMeta() {
        return formMeta;
    }

    public Object getRealForm() {
        if (realForm == null) {
            realForm = formSupplier.get();
        }
        return realForm;
    }
}