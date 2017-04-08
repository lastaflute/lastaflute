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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.bootstrap.GenericBootstrap;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import javax.validation.metadata.ConstraintDescriptor;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.jdbc.Classification;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.Srl;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.core.message.UserMessage;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.core.message.supplier.MessageLocaleProvider;
import org.lastaflute.core.message.supplier.UserMessagesCreator;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.PropertyDesc;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;
import org.lastaflute.web.validation.exception.ClientErrorByValidatorException;
import org.lastaflute.web.validation.exception.ValidationErrorException;
import org.lastaflute.web.validation.exception.ValidationStoppedException;
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
 * @param <MESSAGES> The type of user messages.
 * @author jflute
 */
public class ActionValidator<MESSAGES extends UserMessages> {

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
    //                                          Type Message
    //                                          ------------
    protected static final Map<Class<?>, Validator> cachedValidatorMap = new ConcurrentHashMap<Class<?>, Validator>();

    // -----------------------------------------------------
    //                                               Various
    //                                               -------
    protected static final String MESSAGE_HINT_DELIMITER = "$$df:messageKeyDelimiter$$";
    protected static final String LF = "\n";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final MessageManager messageManager; // not null
    protected final MessageLocaleProvider messageLocaleProvider; // not null
    protected final UserMessagesCreator<MESSAGES> messagesCreator; // not null
    protected final VaErrorHook apiFailureHook; // not null
    protected final Class<?>[] runtimeGroups; // not null
    protected final Validator hibernateValidator; // not null, validator is thread-safe

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionValidator(MessageManager messageManager // to get validation message
            , MessageLocaleProvider messageLocaleProvider // used with messageManager
            , UserMessagesCreator<MESSAGES> messagesCreator // for new user messages
            , VaErrorHook apiFailureHook // hook for API validation error
            , Class<?> hibernateCacheKey // hibernate cache key
            , VaConfigSetupper hibernateConfigSetupper // your configuration of hibernate validator
            , Class<?>... runtimeGroups // validator runtime groups
    ) {
        assertArgumentNotNull("messageManager", messageManager);
        assertArgumentNotNull("messageLocaleProvider", messageLocaleProvider);
        assertArgumentNotNull("messagesCreator", messagesCreator);
        assertArgumentNotNull("apiFailureHook", apiFailureHook);
        assertArgumentNotNull("hibernateCacheKey", hibernateCacheKey);
        assertArgumentNotNull("hibernateConfigSetupper", hibernateConfigSetupper);
        assertArgumentNotNull("runtimeGroups", runtimeGroups);
        this.messageManager = messageManager;
        this.messageLocaleProvider = messageLocaleProvider;
        this.messagesCreator = messagesCreator;
        this.apiFailureHook = apiFailureHook;
        this.runtimeGroups = runtimeGroups;
        assertGroupsNotContainsClientError(runtimeGroups);
        this.hibernateValidator = comeOnHibernateValidator(hibernateCacheKey, hibernateConfigSetupper);
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

    public void throwValidationError(UserMessagesCreator<MESSAGES> noArgInLambda, VaErrorHook validationErrorLambda) {
        assertArgumentNotNull("noArgInLambda", noArgInLambda);
        assertArgumentNotNull("validationErrorLambda", validationErrorLambda);
        throwValidationErrorException(noArgInLambda.create(), validationErrorLambda);
    }

    // -----------------------------------------------------
    //                                            API Facade
    //                                            ----------
    public ValidationSuccess validateApi(Object body, VaMore<MESSAGES> moreValidationLambda) {
        assertArgumentNotNull("body", body);
        assertArgumentNotNull("moreValidationLambda", moreValidationLambda);
        return doValidate(body, moreValidationLambda, apiFailureHook);
    }

    public void throwValidationErrorApi(UserMessagesCreator<MESSAGES> noArgInLambda) {
        assertArgumentNotNull("noArgInLambda", noArgInLambda);
        throwValidationErrorException(noArgInLambda.create(), apiFailureHook);
    }

    // -----------------------------------------------------
    //                                               Control
    //                                               -------

    protected ValidationSuccess doValidate(Object form, VaMore<MESSAGES> moreValidationLambda, VaErrorHook validationErrorLambda) {
        verifyFormType(form);
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
        final MESSAGES messages = resolveTypeFailure(form, toUserMessages(form, vioSet));
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

    // unknown if thread cache does not exist, so 'not' method is prepared like this
    public static boolean certainlyValidatorNotCalled() { // called by e.g. red-cardable assist
        return ThreadCacheContext.exists() && !ThreadCacheContext.isValidatorCalled();
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
    // -----------------------------------------------------
    //                                     Actually Validate
    //                                     -----------------
    protected Set<ConstraintViolation<Object>> hibernateValidate(Object form, Class<?>[] groups) {
        try {
            return hibernateValidator.validate(form, groups);
        } catch (RuntimeException e) {
            handleHibernateValidatorException(form, groups, e);
            return null; // unreachable
        }
    }

    protected void handleHibernateValidatorException(Object form, Class<?>[] groups, RuntimeException e) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Failed to validate so stop it.");
        br.addItem("Advice");
        br.addElement("Confirm the nested exception message.");
        br.addItem("Form or Body");
        br.addElement(form.getClass().getName());
        br.addItem("Groups");
        br.addElement(Stream.of(groups).map(gr -> gr.getSimpleName()).collect(Collectors.joining(", ")));
        final String msg = br.buildExceptionMessage();
        throw new ValidationStoppedException(msg, e);
    }

    // -----------------------------------------------------
    //                                     Prepare Validator
    //                                     -----------------
    protected Validator comeOnHibernateValidator(Class<?> hibernateCacheKey, VaConfigSetupper hibernateConfigSetupper) {
        Validator cached = cachedValidatorMap.get(hibernateCacheKey);
        if (cached != null) {
            return cached;
        }
        synchronized (cachedValidatorMap) {
            cached = cachedValidatorMap.get(hibernateCacheKey);
            if (cached != null) {
                return cached;
            }
            cached = buildValidatorFactory(hibernateConfigSetupper).getValidator(); // about 5ms
            cachedValidatorMap.put(hibernateCacheKey, cached);
            return cachedValidatorMap.get(hibernateCacheKey);
        }
    }

    protected ValidatorFactory buildValidatorFactory(VaConfigSetupper hibernateConfigSetupper) {
        final Configuration<?> configuration = createConfiguration();
        setupFrameworkConfiguration(configuration);
        setupYourConfiguration(configuration, hibernateConfigSetupper);
        return configuration.buildValidatorFactory();
    }

    protected void setupFrameworkConfiguration(Configuration<?> configuration) {
        configuration.messageInterpolator(newResourceBundleMessageInterpolator());
    }

    protected void setupYourConfiguration(Configuration<?> configuration, VaConfigSetupper hibernateConfigSetupper) {
        hibernateConfigSetupper.setup(configuration);
    }

    protected Configuration<?> createConfiguration() {
        return newGenericBootstrap().configure();
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
        return ignoredLocale -> {
            // not used default locacle managed in Hibernate validator,
            // all messages use provided locale (e.g. request locale)
            // to match with other message's locale
            final Locale provided = messageLocaleProvider.provide();
            return newHookedResourceBundle(provided);
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
            // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
            // if null (and token), find it from annotation attributes
            // (at heart, want to know whether token or not)
            // _/_/_/_/_/_/_/_/_/_/
            // *move filtering embedded domain to message manager
            //final String realKey = filterMessageKey(key);
            final OptionalThing<String> opt = messageManager.findMessage(locale, key);
            checkMainMessageNotFound(opt, key);
            return opt.map(msg -> toHintMessage(key, msg)).orElse(null); // filtered later
        }

        protected String toHintMessage(String key, String msg) {
            return key + MESSAGE_HINT_DELIMITER + msg;
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
    //                                                                       User Messages
    //                                                                       =============
    protected MESSAGES toUserMessages(Object form, Set<ConstraintViolation<Object>> vioSet) {
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
        return messagesCreator.create();
    }

    protected void registerActionMessage(UserMessages messages, ConstraintViolation<Object> vio) {
        final String propertyPath = extractPropertyPath(vio);
        final String plainMessage = filterMessageItem(extractMessage(vio), propertyPath);
        final String delimiter = MESSAGE_HINT_DELIMITER;
        final String messageItself;
        final String messageKey;
        if (plainMessage.contains(delimiter)) { // basically here
            messageItself = Srl.substringFirstRear(plainMessage, delimiter);
            messageKey = Srl.substringFirstFront(plainMessage, delimiter);
        } else { // just in case
            messageItself = plainMessage;
            messageKey = null;
        }
        final ConstraintDescriptor<?> descriptor = vio.getConstraintDescriptor();
        final Annotation annotation = descriptor.getAnnotation();
        final Set<Class<?>> groupSet = descriptor.getGroups();
        final Class<?>[] groups = groupSet.toArray(new Class<?>[groupSet.size()]);
        messages.add(propertyPath, createDirectMessage(messageItself, annotation, groups, messageKey));
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
        final MESSAGES messages = toUserMessages(form, clientErrorSet);
        messages.toPropertySet().forEach(property -> {
            sb.append(LF).append(" ").append(property);
            for (Iterator<UserMessage> ite = messages.silentAccessByIteratorOf(property); ite.hasNext();) {
                sb.append(LF).append("   ").append(ite.next());
            }
        });
        final String msg = sb.toString();
        throw new ClientErrorByValidatorException(msg, messages);
    }

    // ===================================================================================
    //                                                                        Type Failure
    //                                                                        ============
    protected MESSAGES resolveTypeFailure(Object form, MESSAGES messages) {
        final TypeFailureBean failureBean = findFailureBeanOnThread(form);
        if (failureBean == null || !failureBean.hasFailure()) {
            return messages;
        }
        final Map<String, TypeFailureElement> elementMap = failureBean.getElementMap();
        final MESSAGES newMsgs = prepareActionMessages();
        for (String property : messages.toPropertySet()) {
            final TypeFailureElement element = elementMap.get(property);
            if (element != null) { // already exists except type failure
                handleTypeFailureGroups(element); // may be bad request
                for (Iterator<UserMessage> ite = messages.silentAccessByIteratorOf(property); ite.hasNext();) {
                    final UserMessage current = ite.next();
                    if (current.getValidatorAnnotation().filter(anno -> {
                        return anno instanceof NotNull || anno instanceof Required;
                    }).isPresent()) {
                        continue; // remove required annotations because they were born by type failure's null
                    }
                    newMsgs.add(property, current);
                }
                newMsgs.add(property, createTypeFailureActionMessage(element)); // add to existing property
            } else { // properties not related with type failures
                for (Iterator<UserMessage> ite = messages.silentAccessByIteratorOf(property); ite.hasNext();) {
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

    protected TypeFailureBean findFailureBeanOnThread(Object form) {
        if (!ThreadCacheContext.exists()) { // basically no way, just in case
            return null;
        }
        final Class<? extends Object> keyType = form.getClass();
        final TypeFailureBean failureBean = (TypeFailureBean) ThreadCacheContext.findValidatorTypeFailure(keyType);
        ThreadCacheContext.removeValidatorTypeFailure(keyType);
        return failureBean;
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

    protected UserMessage createTypeFailureActionMessage(TypeFailureElement element) {
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
        return createDirectMessage(completeMsg, annotation, annotation.groups(), messageKey);
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
    protected void verifyFormType(Object form) {
        if (mightBeValidable(form)) {
            return;
        }
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The validate() argument 'form' should be form type.");
        br.addItem("Advice");
        br.addElement("The validate() or validateApi() first argument");
        br.addElement("should be object type. (not be e.g. String, Integer)");
        br.addElement("For example:");
        br.addElement("  (x): validate(\"sea\", ...) // *Bad");
        br.addElement("  (x): validate(1, ...) // *Bad");
        br.addElement("  (x): validate(date, ...) // *Bad");
        br.addElement("");
        br.addElement("  (o):");
        br.addElement("    public HtmlResponse index(SeaForm form) {");
        br.addElement("        validate(form, ...) // Good");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public JsonResponse<SeaBean> index(SeaBody body) {");
        br.addElement("        validate(body, ...) // Good");
        br.addElement("    }");
        br.addElement("");
        br.addElement("If that helps, URL parameters on execute method arguments");
        br.addElement("are unneeded to validate().");
        br.addElement("If the parameter type is not OptionalThing (e.g. String, Integer),");
        br.addElement("It has been already checked as required parameter in framework.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse index(String sea) {");
        br.addElement("        validate(sea, ...) // *Bad");
        br.addElement("        if (sea.length() > 3) ...");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public HtmlResponse index(String sea) {");
        br.addElement("        if (sea.length() > 3) ... // Good");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public HtmlResponse index(OptionalThing<String> sea) {");
        br.addElement("        sea.filter(...).map(...) // Good");
        br.addElement("    }");
        br.addItem("Specified Form");
        br.addElement(form.getClass().getName());
        br.addElement(form);
        final String msg = br.buildExceptionMessage();
        throw new IllegalArgumentException(msg);
    }

    protected boolean mightBeValidable(Object form) {
        return !cannotBeValidatable(form);
    }

    // similar logic is on action response reflector
    public static boolean cannotBeValidatable(Object value) { // called by e.g. ResponseBeanValidator
        return value instanceof String // yes-yes-yes 
                || value instanceof Number // e.g. Integer
                || DfTypeUtil.isAnyLocalDate(value) // e.g. LocalDate
                || value instanceof Boolean // of course
                || value instanceof Classification // e.g. CDef
                || value.getClass().isPrimitive() // probably no way, just in case
        ;
    }

    protected String convertToLabelKey(String name) {
        return LABELS_PREFIX + name;
    }

    protected OptionalThing<String> findMessage(String messageKey) {
        return messageManager.findMessage(messageLocaleProvider.provide(), messageKey);
    }

    protected UserMessage createDirectMessage(String msg, Annotation annotation, Class<?>[] groups, String messageKey) {
        return UserMessage.asDirectMessage(msg, annotation, groups, unbraceDirectMessageKey(messageKey));
    }

    protected String unbraceDirectMessageKey(String messageKey) {
        if (messageKey != null && Srl.isQuotedAnything(messageKey, "{", "}")) {
            return Srl.unquoteAnything(messageKey, "{", "}");
        } else {
            return messageKey;
        }
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
