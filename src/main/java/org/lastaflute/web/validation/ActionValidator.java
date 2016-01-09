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
package org.lastaflute.web.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.bootstrap.GenericBootstrap;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import javax.validation.metadata.ConstraintDescriptor;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.Srl;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.PropertyDesc;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;
import org.lastaflute.web.api.ApiFailureResource;
import org.lastaflute.web.response.ApiResponse;
import org.lastaflute.web.ruts.message.ActionMessage;
import org.lastaflute.web.ruts.message.ActionMessages;
import org.lastaflute.web.ruts.message.MessagesCreator;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.util.LaActionRuntimeUtil;
import org.lastaflute.web.validation.exception.ClientErrorByValidatorException;
import org.lastaflute.web.validation.exception.ValidationErrorException;
import org.lastaflute.web.validation.theme.conversion.TypeFailureBean;
import org.lastaflute.web.validation.theme.conversion.TypeFailureElement;
import org.lastaflute.web.validation.theme.conversion.ValidateTypeFailure;
import org.lastaflute.web.validation.theme.typed.TypeBigDecimal;
import org.lastaflute.web.validation.theme.typed.TypeBoolean;
import org.lastaflute.web.validation.theme.typed.TypeDouble;
import org.lastaflute.web.validation.theme.typed.TypeFloat;
import org.lastaflute.web.validation.theme.typed.TypeInteger;
import org.lastaflute.web.validation.theme.typed.TypeLocalDate;
import org.lastaflute.web.validation.theme.typed.TypeLocalDateTime;
import org.lastaflute.web.validation.theme.typed.TypeLocalTime;
import org.lastaflute.web.validation.theme.typed.TypeLong;

/**
 * @param <MESSAGES> The type of action messages.
 * @author jflute
 */
public class ActionValidator<MESSAGES extends ActionMessages> {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    // -----------------------------------------------------
    //                                       Javax/Hibernate
    //                                       ---------------
    public static final String JAVAX_CONSTRAINTS_PKG = NotNull.class.getPackage().getName();
    public static final String HIBERNATE_CONSTRAINTS_PKG = NotEmpty.class.getPackage().getName();

    // -----------------------------------------------------
    //                                                Groups
    //                                                ------
    public static final Class<?> DEFAULT_GROUP_TYPE = Default.class;
    public static final Class<?>[] DEFAULT_GROUPS = new Class<?>[] { DEFAULT_GROUP_TYPE };
    public static final Class<?> CLIENT_ERROR_TYPE = ClientError.class;
    public static final Class<?>[] CLIENT_ERROR_GROUPS = new Class<?>[] { CLIENT_ERROR_TYPE };

    // -----------------------------------------------------
    //                                            Item Label
    //                                            ----------
    protected static final String ITEM_VARIABLE = "{item}";
    protected static final String ITEM_LABEL_DELIMITER = "/";
    protected static final String PROPERTY_TYPE_VARIABLE = "{propertyType}";
    protected static final String LABELS_PREFIX = "labels.";

    // -----------------------------------------------------
    //                                          Type Message
    //                                          ------------
    protected static final Map<Class<?>, String> typeMessageMap;

    static {
        final Map<Class<?>, String> readyMap = new HashMap<Class<?>, String>();
        readyMap.put(Integer.class, TypeInteger.DEFAULT_MESSAGE);
        readyMap.put(int.class, TypeInteger.DEFAULT_MESSAGE);
        readyMap.put(Long.class, TypeLong.DEFAULT_MESSAGE);
        readyMap.put(long.class, TypeLong.DEFAULT_MESSAGE);
        readyMap.put(Float.class, TypeFloat.DEFAULT_MESSAGE);
        readyMap.put(float.class, TypeFloat.DEFAULT_MESSAGE);
        readyMap.put(Double.class, TypeDouble.DEFAULT_MESSAGE);
        readyMap.put(double.class, TypeDouble.DEFAULT_MESSAGE);
        readyMap.put(BigDecimal.class, TypeBigDecimal.DEFAULT_MESSAGE);
        readyMap.put(LocalDate.class, TypeLocalDate.DEFAULT_MESSAGE);
        readyMap.put(LocalDateTime.class, TypeLocalDateTime.DEFAULT_MESSAGE);
        readyMap.put(LocalTime.class, TypeLocalTime.DEFAULT_MESSAGE);
        readyMap.put(Boolean.class, TypeBoolean.DEFAULT_MESSAGE);
        readyMap.put(boolean.class, TypeBoolean.DEFAULT_MESSAGE);
        typeMessageMap = Collections.unmodifiableMap(readyMap);
    }

    // -----------------------------------------------------
    //                                               Various
    //                                               -------
    protected static final String LF = "\n";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RequestManager requestManager;
    protected final MessagesCreator<MESSAGES> messageCreator;
    protected final Class<?>[] runtimeGroups; // not null

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionValidator(RequestManager requestManager, MessagesCreator<MESSAGES> noArgInLambda, Class<?>... runtimeGroups) {
        assertArgumentNotNull("requestManager", requestManager);
        assertArgumentNotNull("noArgInLambda", noArgInLambda);
        assertArgumentNotNull("runtimeGroups", runtimeGroups);
        this.requestManager = requestManager;
        this.messageCreator = noArgInLambda;
        this.runtimeGroups = runtimeGroups;
        assertGroupsNotContainsClientError(runtimeGroups);
    }

    protected void assertGroupsNotContainsClientError(Class<?>... groups) {
        Stream.of(groups).filter(tp -> tp.equals(CLIENT_ERROR_TYPE)).findAny().ifPresent(groupType -> {
            String msg = "Cannot specify client error as group, you can use it only at annotation: ";
            throw new IllegalStateException(msg + Arrays.asList(groups));
        });
    }

    // ===================================================================================
    //                                                                            Validate
    //                                                                            ========
    // -----------------------------------------------------
    //                                           Form Facade
    //                                           -----------
    public ValidationSuccess validate(Object form, VaMore<MESSAGES> moreValidationLambda, VaErrorHook validationErrorLambda) {
        assertArgumentNotNull("form", form);
        assertArgumentNotNull("moreValidationLambda", moreValidationLambda);
        assertArgumentNotNull("validationErrorLambda", validationErrorLambda);
        return doValidate(form, moreValidationLambda, validationErrorLambda);
    }

    public void throwValidationError(MessagesCreator<MESSAGES> noArgInLambda, VaErrorHook validationErrorLambda) {
        assertArgumentNotNull("noArgInLambda", noArgInLambda);
        assertArgumentNotNull("validationErrorLambda", validationErrorLambda);
        throwValidationErrorException(noArgInLambda.provide(), validationErrorLambda);
    }

    // -----------------------------------------------------
    //                                            API Facade
    //                                            ----------
    public ValidationSuccess validateApi(Object body, VaMore<MESSAGES> moreValidationLambda) {
        assertArgumentNotNull("body", body);
        assertArgumentNotNull("moreValidationLambda", moreValidationLambda);
        return doValidate(body, moreValidationLambda, () -> hookApiValidationError());
    }

    public void throwValidationErrorApi(MessagesCreator<MESSAGES> noArgInLambda) {
        assertArgumentNotNull("noArgInLambda", noArgInLambda);
        throwValidationErrorException(noArgInLambda.provide(), () -> hookApiValidationError());
    }

    protected ApiResponse hookApiValidationError() { // for API
        final ApiFailureResource resource = createApiFailureResource(requestManager.errors().get(), requestManager);
        return requestManager.getApiManager().handleValidationError(resource);
    }

    protected ApiFailureResource createApiFailureResource(OptionalThing<ActionMessages> errors, RequestManager requestManager) {
        return new ApiFailureResource(getActionRuntime(), errors, requestManager);
    }

    protected ActionRuntime getActionRuntime() {
        return LaActionRuntimeUtil.getActionRuntime();
    }

    // -----------------------------------------------------
    //                                               Control
    //                                               -------

    protected ValidationSuccess doValidate(Object form, VaMore<MESSAGES> moreValidationLambda, VaErrorHook validationErrorLambda) {
        return actuallyValidate(wrapAsValidIfNeeds(form), moreValidationLambda, validationErrorLambda);
    }

    protected Object wrapAsValidIfNeeds(Object form) {
        if (form instanceof List<?>) {
            return new VaValidListBean<>((List<?>) form);
        } else if (form instanceof Map<?, ?>) {
            return new VaValidMapBean<>((Map<?, ?>) form);
        } else {
            return form;
        }
    }

    protected ValidationSuccess actuallyValidate(Object form, VaMore<MESSAGES> moreValidationLambda, VaErrorHook validationErrorLambda) {
        markValidationCalled();
        final boolean implicitGroup = containsRuntimeGroup(runtimeGroups, DEFAULT_GROUP_TYPE);
        if (implicitGroup) {
            verifyDefaultGroupClientError(form);
        }
        final Set<ConstraintViolation<Object>> vioSet = hibernateValidate(form, runtimeGroups);
        if (!implicitGroup) {
            verifyExplicitGroupClientError(form, vioSet);
        }
        final MESSAGES messages = resolveTypeFailure(toActionMessages(form, vioSet));
        moreValidationLambda.more(messages);
        if (!messages.isEmpty()) {
            throwValidationErrorException(messages, validationErrorLambda);
        }
        return createValidationSuccess(messages);
    }

    protected boolean containsRuntimeGroup(Class<?>[] groups, Class<?> groupType) {
        return Stream.of(groups).filter(tp -> tp.equals(groupType)).findAny().isPresent();
    }

    protected void markValidationCalled() {
        if (ThreadCacheContext.exists()) {
            ThreadCacheContext.markValidatorCalled();
        }
    }

    protected void throwValidationErrorException(MESSAGES messages, VaErrorHook validationErrorLambda) {
        throw new ValidationErrorException(runtimeGroups, messages, validationErrorLambda);
    }

    protected ValidationSuccess createValidationSuccess(MESSAGES messages) {
        return new ValidationSuccess(messages);
    }

    // ===================================================================================
    //                                                                 Hibernate Validator
    //                                                                 ===================
    protected Set<ConstraintViolation<Object>> hibernateValidate(Object form, Class<?>[] groups) {
        return comeOnHibernateValidator().validate(form, groups);
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
        return locale -> newHookedResourceBundle(locale);
    }

    protected ResourceBundle newHookedResourceBundle(Locale locale) {
        return new HookedResourceBundle(requestManager.getMessageManager(), locale);
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
            // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
            // if null (and token), find it from annotation attributes
            // (at heart, want to know whether token or not)
            // _/_/_/_/_/_/_/_/_/_/
            // *move filtering embedded domain to message manager
            //final String realKey = filterMessageKey(key);
            final OptionalThing<String> opt = messageManager.findMessage(locale, key);
            checkMainMessageNotFound(opt, key);
            return opt.orElse(null);
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

    // ===================================================================================
    //                                                                     Action Messages
    //                                                                     ===============
    protected MESSAGES toActionMessages(Object form, Set<ConstraintViolation<Object>> vioSet) {
        final TreeMap<String, Object> orderedMap = prepareOrderedMap(form, vioSet);
        final MESSAGES messages = prepareActionMessages();
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
            } else if (holder instanceof ConstraintViolation<?>) {
                @SuppressWarnings("unchecked")
                final ConstraintViolation<Object> existing = ((ConstraintViolation<Object>) holder);
                final List<Object> listHolder = new ArrayList<>(4);
                listHolder.add(existing);
                listHolder.add(vio);
                vioPropMap.put(propertyName, listHolder); // override
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
        final TreeMap<String, Object> orderedMap = new TreeMap<String, Object>((key1, key2) -> {
            final String rootProperty1 = Srl.substringFirstFront(key1, "[", ".");
            final String rootProperty2 = Srl.substringFirstFront(key2, "[", ".");
            final Integer priority1 = priorityMap.getOrDefault(rootProperty1, Integer.MAX_VALUE);
            final Integer priority2 = priorityMap.getOrDefault(rootProperty2, Integer.MAX_VALUE);
            if (priority1 > priority2) {
                return 1;
            } else if (priority2 > priority1) {
                return -1;
            } else { /* same group */
                return key1.compareTo(key2);
            }
        });
        orderedMap.putAll(vioPropMap);
        return orderedMap;
    }

    // -----------------------------------------------------
    //                                       Messages Assist
    //                                       ---------------
    protected MESSAGES prepareActionMessages() {
        return messageCreator.provide();
    }

    protected void registerActionMessage(ActionMessages messages, ConstraintViolation<Object> vio) {
        final String propertyPath = extractPropertyPath(vio);
        final String message = filterMessageItem(extractMessage(vio), propertyPath);
        final ConstraintDescriptor<?> descriptor = vio.getConstraintDescriptor();
        final Annotation annotation = descriptor.getAnnotation();
        final Set<Class<?>> groups = descriptor.getGroups();
        messages.add(propertyPath, newDirectActionMessage(message, annotation, groups.toArray(new Class<?>[groups.size()])));
    }

    protected String extractPropertyPath(ConstraintViolation<Object> vio) {
        return vio.getPropertyPath().toString();
    }

    protected String extractMessage(ConstraintViolation<Object> vio) {
        return vio.getMessage();
    }

    protected String filterMessageItem(String message, String propertyPath) {
        final String itemVariable = getItemVariable(message, propertyPath);
        return message.contains(itemVariable) ? Srl.replace(message, itemVariable, deriveItemValue(propertyPath)) : message;
    }

    protected String deriveItemValue(String propertyPath) {
        final List<String> elementList = Srl.splitList(propertyPath, ".");
        final List<String> fullList = new ArrayList<String>();
        final List<Supplier<String>> individualList = new ArrayList<Supplier<String>>();
        for (String element : elementList) {
            final String resolvedKey; // e.g. seaList[]
            final String withoutIndex; // e.g. seaList
            final String rowNumberExp; // e.g. (1) or (a) or ""
            if (element.contains("[") && element.endsWith("]")) { // e.g. seaList[0]
                final String front = Srl.substringLastFront(element, "[");
                final String index = Srl.rtrim(Srl.substringLastRear(element, "["), "]");
                resolvedKey = buildItemListKeyExp(front);
                withoutIndex = front; // e.g. seaList
                rowNumberExp = buildRowNumberExp(index); // e.g. seaList(1) or seaList(a)
            } else {
                resolvedKey = element;
                withoutIndex = element;
                rowNumberExp = "";
            }
            fullList.add(resolvedKey);
            individualList.add(() -> findMessage(buildItemLabelKey(resolvedKey)).orElse(withoutIndex) + rowNumberExp);
        }
        final String fullPath = fullList.stream().collect(Collectors.joining(".")); // e.g. seaList[].land
        return findMessage(buildItemLabelKey(fullPath)).orElseGet(() -> {
            return individualList.stream().map(detail -> detail.get()).collect(Collectors.joining(getItemLabelDelimiter()));
        });
    }

    protected String getItemVariable(String message, String propertyPath) {
        return ITEM_VARIABLE; // as default
    }

    protected String buildItemListKeyExp(String front) {
        // not use "[]" for now, may be no problem so simply
        //return front + "[]"; // e.g. seaList[]
        return front; // no filter e.g. seaList
    }

    protected String getItemLabelDelimiter() {
        return ITEM_LABEL_DELIMITER;
    }

    protected String buildRowNumberExp(String index) {
        return "(" + (Srl.isNumberHarfAll(index) ? String.valueOf(DfTypeUtil.toInteger(index) + 1) : index) + ")";
    }

    protected String buildItemLabelKey(String propertyPath) {
        return convertToLabelKey(propertyPath); // as default
    }

    protected String buildDefaultItemName(String propertyPath) {
        return propertyPath; // as default
    }

    // ===================================================================================
    //                                                                        Client Error
    //                                                                        ============
    protected void verifyDefaultGroupClientError(Object form) {
        // e.g. @Required(groups = { ClientError.class }) (when default group specified)
        if (containsRuntimeGroup(runtimeGroups, DEFAULT_GROUP_TYPE)) {
            final Set<ConstraintViolation<Object>> clientErrorSet = hibernateValidate(form, CLIENT_ERROR_GROUPS);
            handleClientErrorViolation(form, clientErrorSet);
        }
    }

    protected void verifyExplicitGroupClientError(Object form, Set<ConstraintViolation<Object>> vioSet) {
        // e.g. @Required(groups = { Sea.class, ClientError.class }) (when Sea group specified)
        final Set<ConstraintViolation<Object>> clientErrorSet = vioSet.stream().filter(vio -> {
            return containsDefinedGroup(vio.getConstraintDescriptor().getGroups(), CLIENT_ERROR_TYPE);
        }).collect(Collectors.toSet());
        handleClientErrorViolation(form, clientErrorSet);
    }

    protected boolean containsDefinedGroup(Set<Class<?>> groups, Class<?> groupType) {
        return groups.stream().filter(tp -> tp.equals(groupType)).findAny().isPresent();
    }

    protected void handleClientErrorViolation(Object form, Set<ConstraintViolation<Object>> clientErrorSet) {
        if (!clientErrorSet.isEmpty()) {
            throwClientErrorByValidatorException(form, clientErrorSet);
        }
    }

    protected void throwClientErrorByValidatorException(Object form, Set<ConstraintViolation<Object>> clientErrorSet) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Client Error detected by validator: runtimeGroups="); // similar to normal validation error logging
        sb.append(Stream.of(runtimeGroups).map(tp -> {
            return tp.getSimpleName() + ".class";
        }).collect(Collectors.toList()));
        final MESSAGES messages = toActionMessages(form, clientErrorSet);
        messages.toPropertySet().forEach(property -> {
            sb.append(LF).append(" ").append(property);
            for (Iterator<ActionMessage> ite = messages.nonAccessByIteratorOf(property); ite.hasNext();) {
                sb.append(LF).append("   ").append(ite.next());
            }
        });
        final String msg = sb.toString();
        throw new ClientErrorByValidatorException(msg, messages);
    }

    // ===================================================================================
    //                                                                        Type Failure
    //                                                                        ============
    protected MESSAGES resolveTypeFailure(MESSAGES messages) {
        if (!ThreadCacheContext.exists()) { // basically no way, just in case
            return messages;
        }
        final TypeFailureBean failureBean = (TypeFailureBean) ThreadCacheContext.findValidatorTypeFailure();
        if (failureBean == null || !failureBean.hasFailure()) {
            return messages;
        }
        final Map<String, TypeFailureElement> elementMap = failureBean.getElementMap();
        final MESSAGES newMsgs = prepareActionMessages();
        for (String property : messages.toPropertySet()) {
            final TypeFailureElement element = elementMap.get(property);
            if (element != null) { // already exists except type failure
                handleTypeFailureGroups(element); // may be bad request
                for (Iterator<ActionMessage> ite = messages.nonAccessByIteratorOf(property); ite.hasNext();) {
                    final ActionMessage current = ite.next();
                    final Annotation anno = current.getValidatorAnnotation();
                    if (anno instanceof NotNull || anno instanceof Required) {
                        continue; // remove required annotations because they were born by type failure's null
                    }
                    newMsgs.add(property, current);
                }
                newMsgs.add(property, createTypeFailureActionMessage(element)); // add to existing property
            } else { // properties not related with type failures
                for (Iterator<ActionMessage> ite = messages.nonAccessByIteratorOf(property); ite.hasNext();) {
                    newMsgs.add(property, ite.next()); // add no-related property
                }
            }
        }
        for (TypeFailureElement element : elementMap.values()) {
            final String property = element.getPropertyPath();
            if (!messages.hasMessageOf(property)) { // no other validation error
                handleTypeFailureGroups(element); // may be bad request
                newMsgs.add(property, createTypeFailureActionMessage(element)); // add as new message for the proeprty
            }
        }
        return newMsgs;
    }

    protected void handleTypeFailureGroups(TypeFailureElement element) {
        final Class<?>[] annotatedGroups = element.getAnnotation().groups();
        if (annotatedGroups == null || annotatedGroups.length == 0) { // means default group
            if (!Stream.of(runtimeGroups).anyMatch(tp -> Default.class.equals(tp))) { // but no default
                element.getBadRequestThrower().throwBadRequest();
            }
        } else {
            if (!Stream.of(runtimeGroups).anyMatch(runType -> {
                return Stream.of(annotatedGroups).anyMatch(annoType -> annoType.equals(runType));
            })) {
                element.getBadRequestThrower().throwBadRequest();
            }
        }
    }

    protected ActionMessage createTypeFailureActionMessage(TypeFailureElement element) {
        final String propertyPath = element.getPropertyPath();
        final Class<?> propertyType = element.getPropertyType();
        final String messageKey = typeMessageMap.get(propertyType);
        final String completeMsg;
        if (messageKey != null) {
            completeMsg = findMessage(messageKey).map(message -> {
                return filterTypeFailureMessage(message, propertyPath, propertyType);
            }).orElseGet(() -> {
                return buildDefaultTypeMessage(propertyPath, propertyType);
            });
        } else {
            completeMsg = buildDefaultTypeMessage(propertyPath, propertyType);
        }
        final ValidateTypeFailure annotation = element.getAnnotation();
        return newDirectActionMessage(completeMsg, annotation, annotation.groups());
    }

    protected String buildDefaultTypeMessage(String propertyPath, Class<?> propertyType) {
        return findMessage(ValidateTypeFailure.DEFAULT_MESSAGE).map(message -> {
            return filterTypeFailureMessage(message, propertyPath, propertyType);
        }).orElseGet(() -> {
            return "Cannot convert as " + propertyType.getSimpleName();
        });
    }

    protected String filterTypeFailureMessage(String message, String propertyPath, Class<?> propertyType) {
        return filterMessageItem(filterMessagePropertyType(message, propertyPath, propertyType), propertyPath);
    }

    protected String filterMessagePropertyType(String message, String propertyPath, Class<?> propertyType) {
        final String propertyTypeVariable = getPropertyTypeVariable();
        if (message.contains(propertyTypeVariable)) {
            final String labelKey = buildPropertyTypeLabelKey(propertyPath, propertyType);
            final String typeName = findMessage(labelKey).orElseGet(() -> {
                return buildDefaultPropertyTypeName(propertyPath, propertyType);
            });
            return Srl.replace(message, propertyTypeVariable, typeName);
        } else {
            return message;
        }
    }

    protected String getPropertyTypeVariable() {
        return PROPERTY_TYPE_VARIABLE; // as default
    }

    protected String buildPropertyTypeLabelKey(String propertyPath, Class<?> propertyType) {
        return convertToLabelKey(propertyType.getSimpleName()); // as default
    }

    protected String buildDefaultPropertyTypeName(String propertyPath, Class<?> propertyType) {
        return propertyType.getSimpleName(); // as default
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected String convertToLabelKey(String name) {
        return LABELS_PREFIX + name;
    }

    protected OptionalThing<String> findMessage(String messageKey) {
        return requestManager.getMessageManager().findMessage(requestManager.getUserLocale(), messageKey);
    }

    protected ActionMessage newDirectActionMessage(String msg, Annotation annotation, Class<?>[] groups) {
        return ActionMessage.asDirectMessage(msg, annotation, groups);
    }

    // ===================================================================================
    //                                                            Annotation Determination
    //                                                            ========================
    public static boolean isValidatorAnnotation(Class<?> annoType) { // as utility
        final String annoName = annoType.getName();
        return Srl.startsWith(annoName, JAVAX_CONSTRAINTS_PKG, HIBERNATE_CONSTRAINTS_PKG) // normal annotations
                || isNestedBeanAnnotation(annoType) // has validation nested bean
                || isLastaPresentsAnnotation(annoType); // LastaFlute provides
    }

    public static boolean isNestedBeanAnnotation(Class<?> annoType) {
        return annoType.equals(Valid.class);
    }

    public static boolean hasNestedBeanAnnotation(Field field) {
        return field.getAnnotation(Valid.class) != null;
    }

    public static boolean isLastaPresentsAnnotation(Class<?> annoType) {
        final Annotation[] annotations = annoType.getAnnotations(); // not null
        return Stream.of(annotations).filter(anno -> anno instanceof LastaPresentsValidator).findAny().isPresent();
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
