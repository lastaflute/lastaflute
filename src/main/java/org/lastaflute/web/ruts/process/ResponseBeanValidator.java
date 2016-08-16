/*
 * Copyright 2015-2016 the original author or authors.
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
package org.lastaflute.web.ruts.process;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.lastaflute.core.message.UserMessage;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.PropertyDesc;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;
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
        return new ActionValidator<>(requestManager, () -> {
            return new UserMessages();
        }, getValidatorGroups().orElse(ActionValidator.DEFAULT_GROUPS));
    }

    protected abstract OptionalThing<Class<?>[]> getValidatorGroups();

    protected void executeValidator(ActionValidator<UserMessages> validator, Object bean) {
        validator.validate(bean, more -> {}, () -> {
            throw new IllegalStateException("unused here, no way");
        });
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
        } else {
            throw new ResponseBeanValidationErrorException(msg);
        }
    }

    protected abstract String buildValidationErrorMessage(Object bean, Consumer<ExceptionMessageBuilder> locationBuilder,
            UserMessages messages);

    // -----------------------------------------------------
    //                                        Message Helper
    //                                        --------------
    protected void setupItemValidatedBean(ExceptionMessageBuilder br, Object bean) {
        final Class<?> beanType = bean.getClass();
        br.addItem("Validated Bean");
        br.addElement(beanType);
        final String jsonExp = bean.toString();
        br.addElement(jsonExp);
        if ((jsonExp == null || !jsonExp.contains("\n"))
                && !(List.class.isAssignableFrom(beanType) || Map.class.isAssignableFrom(beanType))) {
            br.addItem("Bean Property");
            try {
                final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(beanType);
                final int propertyDescSize = beanDesc.getPropertyDescSize();
                for (int i = 0; i < propertyDescSize; i++) {
                    final PropertyDesc pd = beanDesc.getPropertyDesc(i);
                    br.addElement(pd.getPropertyName() + ": " + pd.getValue(bean));
                }
            } catch (RuntimeException ignored) {
                br.addElement("*Failed to get field values by BeanDesc: " + Srl.cut(ignored.getMessage(), 50, "..."));
            }
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
