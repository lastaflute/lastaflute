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
package org.lastaflute.web.validation;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;

import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.bootstrap.GenericBootstrap;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.PropertyDesc;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;
import org.lastaflute.web.ruts.message.ActionMessage;
import org.lastaflute.web.ruts.message.ActionMessages;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.validation.exception.ValidationErrorException;

/**
 * @author jflute
 */
public class ActionValidator {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final String ITEM_VARIABLE = "{item}";
    protected static final String LABELS_PREFIX = "labels.";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RequestManager requestManager;
    protected final MessageManager messageManager;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionValidator(RequestManager requestManager, MessageManager messageManager) {
        assertArgumentNotNull("requestManager", requestManager);
        assertArgumentNotNull("messageManager", messageManager);
        this.requestManager = requestManager;
        this.messageManager = messageManager;
    }

    // ===================================================================================
    //                                                                            Validate
    //                                                                            ========
    // -----------------------------------------------------
    //                                       Annotation Only
    //                                       ---------------
    public void validate(Object form, ValidationErrorHandler validationErrorLambda) {
        assertArgumentNotNull("form", form);
        assertArgumentNotNull("validationErrorLambda", validationErrorLambda);
        handleValidationErrorExceptionIfNeeds(hibernateValidate(form), validationErrorLambda);
    }

    // -----------------------------------------------------
    //                                       More by Program
    //                                       ---------------
    public void validateMore(Object form, ValidationMoreHandler validationMoreLambda, ValidationErrorHandler validationErrorLambda) {
        assertArgumentNotNull("form", form);
        assertArgumentNotNull("validationMoreLambda", validationMoreLambda);
        assertArgumentNotNull("validationErrorLambda", validationErrorLambda);
        final ActionMessages messages = hibernateValidate(form);
        handleMoreValidation(validationMoreLambda, messages);
        handleValidationErrorExceptionIfNeeds(messages, validationErrorLambda);
    }

    protected void handleMoreValidation(ValidationMoreHandler doValidateLambda, final ActionMessages messages) {
        final ActionMessages moreMessages = doValidateLambda.callback();
        if (moreMessages == null) {
            String msg = "Cannot return null at more-validation callback of validateMore(): " + doValidateLambda;
            throw new IllegalStateException(msg);
        }
        messages.add(moreMessages);
    }

    // -----------------------------------------------------
    //                                         True or Error
    //                                         -------------
    public void validateTrue(boolean trueOrFalse, ValidationTrueMessenger messagesLambda, ValidationErrorHandler validationErrorLambda) {
        if (trueOrFalse) {
            return;
        }
        final ActionMessages messages = messagesLambda.callback();
        if (messages == null) {
            String msg = "Cannot return null at the messenger callback of validateTrue(): " + messagesLambda;
            throw new IllegalStateException(msg);
        }
        handleValidationErrorExceptionIfNeeds(messages, validationErrorLambda);
    }

    // -----------------------------------------------------
    //                                          Handle Error
    //                                          ------------
    protected void handleValidationErrorExceptionIfNeeds(ActionMessages messages, ValidationErrorHandler errorHandler) {
        if (!messages.isEmpty()) {
            throwValidationErrorException(messages, errorHandler);
        }
    }

    protected void throwValidationErrorException(ActionMessages messages, ValidationErrorHandler errorHandler) {
        throw new ValidationErrorException(messages, errorHandler);
    }

    // ===================================================================================
    //                                                                 Hibernate Validator
    //                                                                 ===================
    protected ActionMessages hibernateValidate(Object form) {
        final Validator validator = comeOnHibernateValidator();
        final Set<ConstraintViolation<Object>> vioSet = validator.validate(form);
        final TreeMap<String, Object> orderedMap = prepareOrderedMap(form, vioSet);
        final ActionMessages messages = newBaseActionMessages();
        for (Entry<String, Object> entry : orderedMap.entrySet()) {
            final Object holder = entry.getValue();
            if (holder instanceof ConstraintViolation) {
                @SuppressWarnings("unchecked")
                final ConstraintViolation<Object> vio = (ConstraintViolation<Object>) holder;
                registerActionMessage(messages, vio);
            } else if (holder instanceof List<?>) {
                @SuppressWarnings("unchecked")
                final List<ConstraintViolation<Object>> vioList = ((List<ConstraintViolation<Object>>) holder);
                for (ConstraintViolation<Object> vio : vioList) {
                    registerActionMessage(messages, vio);
                }
            } else {
                throw new IllegalStateException("Unknown type of holder: " + holder);
            }
        }
        return messages;
    }

    // -----------------------------------------------------
    //                                    Validator Settings
    //                                    ------------------
    protected Validator comeOnHibernateValidator() {
        final Configuration<?> configure = newGenericBootstrap().configure();
        configure.messageInterpolator(newResourceBundleMessageInterpolator());
        return configure.buildValidatorFactory().getValidator();
    }

    protected GenericBootstrap newGenericBootstrap() {
        return Validation.byDefaultProvider();
    }

    protected ResourceBundleMessageInterpolator newResourceBundleMessageInterpolator() {
        return new ResourceBundleMessageInterpolator(newResourceBundleLocator());
    }

    // -----------------------------------------------------
    //                                       Resource Bundle
    //                                       ---------------
    protected ResourceBundleLocator newResourceBundleLocator() {
        return new ResourceBundleLocator() {
            public ResourceBundle getResourceBundle(Locale locale) {
                return newHookedResourceBundle(locale);
            }
        };
    }

    protected ResourceBundle newHookedResourceBundle(Locale locale) {
        return new HookedResourceBundle(messageManager, locale);
    }

    protected static class HookedResourceBundle extends ResourceBundle {

        protected final MessageManager messageManager;
        protected final Locale locale;

        public HookedResourceBundle(MessageManager messageManager, Locale locale) {
            this.messageManager = messageManager;
            this.locale = locale;
        }

        @Override
        protected Object handleGetObject(String key) {
            // if null (and token), find it from annotation attributes
            // (at heart, want to know whether token or not)
            final String realKey = filterMessageKey(key);
            final OptionalThing<String> opt = messageManager.findMessage(locale, realKey);
            checkMainMessageNotFound(opt, key);
            return opt.orElse(null);
        }

        protected String filterMessageKey(String key) {
            final String javaxPackage = "javax.validation.";
            final String hibernatePackage = "org.hibernate.validator.";
            final String realKey;
            if (key.startsWith(javaxPackage)) {
                realKey = Srl.substringFirstRear(key, javaxPackage);
            } else if (key.startsWith(hibernatePackage)) {
                realKey = Srl.substringFirstRear(key, hibernatePackage);
            } else {
                realKey = key;
            }
            return realKey;
        }

        protected void checkMainMessageNotFound(OptionalThing<String> opt, String key) {
            if (mightBeMainMessage(key)) {
                opt.get(); // throw
            }
        }

        protected boolean mightBeMainMessage(String key) {
            return key.contains(".");
        }

        @Override
        public Enumeration<String> getKeys() {
            throw new IllegalStateException("Should not be called: getKeys()");
        }
    }

    // -----------------------------------------------------
    //                                           Ordered Map
    //                                           -----------
    // basically for batch display
    protected TreeMap<String, Object> prepareOrderedMap(Object form, Set<ConstraintViolation<Object>> vioSet) {
        final Map<String, Object> vioPropMap = new HashMap<>(vioSet.size());
        for (ConstraintViolation<Object> vio : vioSet) {
            final String propertyPath = extractPropertyPath(vio);
            final boolean nested = propertyPath.contains(".");
            final String propertyName = nested ? Srl.substringFirstFront(propertyPath, ".") : propertyPath;
            Object holder = vioPropMap.get(propertyName);
            if (holder == null) {
                holder = nested ? new ArrayList<>(4) : vio; // direct holder for performance
                vioPropMap.put(propertyName, holder);
            }
            if (holder instanceof List<?>) {
                @SuppressWarnings("unchecked")
                final List<Object> listHolder = (List<Object>) holder;
                listHolder.add(vio);
            }
        }
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(form.getClass());
        final int pdSize = beanDesc.getPropertyDescSize();
        final Map<String, Integer> priorityMap = new HashMap<>(vioPropMap.size());
        for (int i = 0; i < pdSize; i++) {
            final PropertyDesc pd = beanDesc.getPropertyDesc(i);
            final String propertyName = pd.getPropertyName();
            if (vioPropMap.containsKey(propertyName)) {
                priorityMap.put(propertyName, i);
            }
        }
        final TreeMap<String, Object> orderedMap = new TreeMap<String, Object>((o1, o2) -> {
            return priorityMap.getOrDefault(o1, Integer.MAX_VALUE).compareTo(priorityMap.getOrDefault(o2, Integer.MAX_VALUE));
        });
        orderedMap.putAll(vioPropMap);
        return orderedMap;
    }

    // -----------------------------------------------------
    //                                        Action Message
    //                                        --------------
    protected ActionMessages newBaseActionMessages() {
        return new ActionMessages();
    }

    protected void registerActionMessage(ActionMessages messages, ConstraintViolation<Object> vio) {
        final String propertyPath = extractPropertyPath(vio);
        final String message = filterMessage(extractMessage(vio), propertyPath);
        messages.add(propertyPath, newDirectActionMessage(message));
    }

    protected String extractPropertyPath(ConstraintViolation<Object> vio) {
        return vio.getPropertyPath().toString();
    }

    protected String extractMessage(ConstraintViolation<Object> vio) {
        return vio.getMessage();
    }

    protected ActionMessage newDirectActionMessage(String msg) {
        return new ActionMessage(msg, false);
    }

    // -----------------------------------------------------
    //                                        Filter Message
    //                                        --------------
    protected String filterMessage(String message, String propertyPath) {
        final String itemVariable = getItemVariable(message, propertyPath);
        if (message.contains(itemVariable)) {
            final Locale userLocale = requestManager.getUserLocale();
            final String labelKey = buildLabelKey(propertyPath);
            final String itemName = messageManager.findMessage(userLocale, labelKey).orElseGet(() -> {
                return getDefaultItem(message, propertyPath);
            });
            return Srl.replace(message, itemVariable, itemName);
        } else {
            return message;
        }
    }

    protected String getItemVariable(String message, String propertyPath) {
        return ITEM_VARIABLE; // as default
    }

    protected String getDefaultItem(String message, String propertyPath) {
        return propertyPath; // as default
    }

    protected String buildLabelKey(String propertyPath) {
        return LABELS_PREFIX + propertyPath; // as default
    }

    // ===================================================================================
    //                                                                       Assert Helper
    //                                                                       =============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            String msg = "The value should not be null: variableName=null value=" + value;
            throw new IllegalArgumentException(msg);
        }
        if (value == null) {
            String msg = "The value should not be null: variableName=" + variableName;
            throw new IllegalArgumentException(msg);
        }
    }
}
