/*
 * Copyright 2015-2024 the original author or authors.
 * Copyright 2015-2024 the original author or authors.
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
package org.lastaflute.web.ruts.process.validatebean;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.lastaflute.core.message.UserMessage;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.core.util.Lato;
import org.lastaflute.web.ruts.process.exception.ResponseBeanValidationErrorException;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.validation.ActionValidator;
import org.lastaflute.web.validation.exception.ClientErrorByValidatorException;
import org.lastaflute.web.validation.exception.ValidationErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 * @since 0.7.6 (2016/01/04 Monday)
 */
public abstract class ResponseBeanValidator {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(ResponseBeanValidator.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RequestManager requestManager;
    protected final Object actionExp; // used as only exception message, Object for unit test
    protected final boolean warning;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ResponseBeanValidator(RequestManager requestManager, Object actionExp, boolean warning) {
        this.requestManager = requestManager;
        this.actionExp = actionExp;
        this.warning = warning;
    }

    // ===================================================================================
    //                                                                            Validate
    //                                                                            ========
    protected void doValidate(Object bean, Consumer<ExceptionMessageBuilder> locationBuilder) {
        if (bean == null) {
            throw new IllegalStateException("The argument 'bean' should not be null.");
        }
        if (mightBeValidatable(bean)) {
            final ActionValidator<UserMessages> validator = createActionValidator();
            try {
                executeValidator(validator, bean);
            } catch (ValidationErrorException e) {
                handleResponseBeanValidationErrorException(bean, locationBuilder, e.getMessages(), e);
            } catch (ClientErrorByValidatorException e) {
                handleResponseBeanValidationErrorException(bean, locationBuilder, e.getMessages(), e);
            }
        }
    }

    protected boolean mightBeValidatable(Object value) {
        return !ActionValidator.cannotBeValidatable(value);
    }

    protected ActionValidator<UserMessages> createActionValidator() {
        final Class<?>[] groups = getValidatorGroups().orElse(ActionValidator.DEFAULT_GROUPS);
        return new ActionValidator<UserMessages>(requestManager, () -> new UserMessages(), groups);
    }

    protected abstract OptionalThing<Class<?>[]> getValidatorGroups();

    protected void executeValidator(ActionValidator<UserMessages> validator, Object bean) {
        validator.simplyValidate(bean);
    }

    // ===================================================================================
    //                                                                    Validation Error
    //                                                                    ================
    protected void handleResponseBeanValidationErrorException(Object bean, Consumer<ExceptionMessageBuilder> locationBuilder,
            UserMessages messages, RuntimeException cause) {
        // cause is completely framework info so not show it
        final String msg = buildValidationErrorMessage(bean, locationBuilder, messages);
        if (warning) {
            logger.warn(msg);
        } else { // but keep framework information for various purpose of client
            throw new ResponseBeanValidationErrorException(msg, bean, messages);
        }
    }

    protected abstract String buildValidationErrorMessage(Object bean, Consumer<ExceptionMessageBuilder> locationBuilder,
            UserMessages messages);

    // -----------------------------------------------------
    //                                        Message Helper
    //                                        --------------
    protected void setupItemValidatedBean(ExceptionMessageBuilder br, Object bean) {
        br.addItem("Validated Bean");
        br.addElement(bean.getClass().getName());
        br.addElement(bean.toString()); // don't know whether overridden method or not
        br.addItem("Property Values");
        try {
            br.addElement(Lato.string(bean)); // contains nested bean

            // cannot support nested bean's properties
            //final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(beanType);
            //final int propertyDescSize = beanDesc.getPropertyDescSize();
            //for (int i = 0; i < propertyDescSize; i++) {
            //    final PropertyDesc pd = beanDesc.getPropertyDesc(i);
            //    br.addElement(pd.getPropertyName() + ": " + pd.getValue(bean));
            //}
        } catch (RuntimeException ignored) {
            br.addElement("*Failed to get field values: " + Srl.cut(ignored.getMessage(), 50, "..."));
        }
    }

    protected void setupItemMessages(ExceptionMessageBuilder br, UserMessages messages) {
        br.addItem("Messages");
        final Set<String> propertySet = messages.toPropertySet();
        for (String property : propertySet) {
            br.addElement(property);
            for (Iterator<UserMessage> ite = messages.accessByIteratorOf(property); ite.hasNext();) {
                br.addElement("  " + ite.next());
            }
        }
    }
}
