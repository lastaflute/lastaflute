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
package org.lastaflute.web.ruts.message.objective;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfStringUtil;
import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.Srl.ScopeInfo;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.di.Disposable;
import org.lastaflute.di.DisposableUtil;
import org.lastaflute.di.helper.message.MessageResourceBundle;
import org.lastaflute.di.helper.message.MessageResourceBundleFactory;
import org.lastaflute.di.util.LdiResourceUtil;
import org.lastaflute.web.direction.FwWebDirection;
import org.lastaflute.web.ruts.message.MessageResources;
import org.lastaflute.web.ruts.message.exception.MessageLabelByLabelParameterNotFoundException;
import org.lastaflute.web.ruts.message.exception.MessageLabelByLabelVariableInfinityLoopException;
import org.lastaflute.web.ruts.message.exception.MessageLabelByLabelVariableInvalidKeyException;
import org.lastaflute.web.ruts.message.exception.MessageLabelByLabelVariableNotFoundException;

/**
 * @author jflute
 */
public class ObjectiveMessageResources implements MessageResources, Disposable, Serializable {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = 1L;

    /** The key prefix for errors of message resources, which contains dot at last. */
    public static final String ERRORS_KEY_PREFIX = "errors.";

    /** The key prefix for labels, which contains dot at last. */
    public static final String LABELS_KEY_PREFIX = "labels.";

    /** The key prefix for messages of message resources, which contains dot at last. */
    public static final String MESSAGES_KEY_PREFIX = "messages.";

    /** The extension for properties, which contains dot at front. */
    public static final String PROPERTIES_EXT = ".properties";

    /** The begin mark of label variable. */
    public static final String LABEL_VARIABLE_BEGIN_MARK = "@[";

    /** The end mark of label variable. */
    public static final String LABEL_VARIABLE_END_MARK = "]";

    /** The cache map of bundle. The string key is message (bundle) name (NotNull) */
    protected static final Map<String, Map<Locale, MessageResourceBundle>> bundleCacheMap = newConcurrentHashMap();

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected boolean returnNull = true; // as default
    protected boolean escape = true; // as default
    protected final Map<String, MessageFormat> formatMap = new HashMap<String, MessageFormat>();

    /**
     * The cache of assistant director, which can be lazy-loaded when you get it.
     * Don't use these variables directly, you should use the getter.
     * (NotNull: after lazy-load)
     * */
    protected FwAssistantDirector cachedAssistantDirector;

    /**
     * The cache of application message name, which can be lazy-loaded when you get it. <br>
     * Don't use these variables directly, you should use the getter.
     * e.g. admin_message (NotNull: after lazy-load)
     */
    protected String cachedAppMessageName;

    /**
     * The cache of message name list for extends, which can be lazy-loaded when you get it.
     * Don't use these variables directly, you should use the getter.
     * e.g. list:{project_message ; common_message} (NotNull: after lazy-load)
     */
    protected List<String> cachedExtendsMessageNameList;

    /** Is it already initialized? (back to false when HotDeploy disposed) */
    protected volatile boolean initialized;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ObjectiveMessageResources() {
        initialize();
    }

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    public void initialize() {
        DisposableUtil.add(this);
        initialized = true;
    }

    // ===================================================================================
    //                                                                         Get Message
    //                                                                         ===========
    @Override
    public String getMessage(Locale locale, String key) {
        assertArgumentNotNull("locale", locale);
        assertArgumentNotNull("key", key);
        prepareDisposable();
        return doGetMessage(locale, key);
    }

    @Override
    public String getMessage(Locale locale, String key, Object arg0) {
        assertArgumentNotNull("locale", locale);
        assertArgumentNotNull("key", key);
        assertArgumentNotNull("arg0", arg0);
        return getMessage(locale, key, new Object[] { arg0 });
    }

    @Override
    public String getMessage(Locale locale, String key, Object arg0, Object arg1) {
        assertArgumentNotNull("locale", locale);
        assertArgumentNotNull("key", key);
        assertArgumentNotNull("arg0", arg0);
        assertArgumentNotNull("arg1", arg1);
        return getMessage(locale, key, new Object[] { arg0, arg1 });
    }

    @Override
    public String getMessage(Locale locale, String key, Object arg0, Object arg1, Object arg2) {
        assertArgumentNotNull("locale", locale);
        assertArgumentNotNull("key", key);
        assertArgumentNotNull("arg0", arg0);
        assertArgumentNotNull("arg1", arg1);
        assertArgumentNotNull("arg2", arg2);
        return getMessage(locale, key, new Object[] { arg0, arg1, arg2 });
    }

    @Override
    public String getMessage(Locale locale, String key, Object arg0, Object arg1, Object arg2, Object arg3) {
        assertArgumentNotNull("locale", locale);
        assertArgumentNotNull("key", key);
        assertArgumentNotNull("arg0", arg0);
        assertArgumentNotNull("arg1", arg1);
        assertArgumentNotNull("arg2", arg2);
        assertArgumentNotNull("arg3", arg3);
        return getMessage(locale, key, new Object[] { arg0, arg1, arg2, arg3 });
    }

    @Override
    public String getMessage(Locale locale, String key, Object[] args) {
        assertArgumentNotNull("locale", locale);
        assertArgumentNotNull("key", key);
        assertArgumentNotNull("args", args);
        prepareDisposable();
        return doGetMessage(locale, key, args);
    }

    protected void prepareDisposable() {
        if (!initialized) {
            initialize();
        }
    }

    protected String doGetMessage(Locale locale, String key, Object[] args) {
        final List<Object> resolvedList = resolveLabelParameter(locale, key, args);
        final String message = formatMessage(locale, key, resolvedList.toArray());
        final Set<String> callerKeySet = createCallerKeySet();
        return resolveLabelVariableMessage(locale, key, message, callerKeySet);
    }

    protected String formatMessage(Locale locale, String key, Object args[]) {
        MessageFormat format = null;
        final String formatKey = messageKey(locale, key);
        synchronized (formatMap) {
            format = (MessageFormat) formatMap.get(formatKey);
            if (format == null) {
                final String formatString = getMessage(locale, key);
                if (formatString == null) {
                    return returnNull ? null : ("???" + formatKey + "???");
                }
                format = new MessageFormat(escape(formatString));
                format.setLocale(locale);
                formatMap.put(formatKey, format);
            }
        }
        return format.format(args);
    }

    protected String doGetMessage(Locale locale, String key) {
        // almost same as super's (seasar's) process
        // only changed is how to get bundle
        final MessageResourceBundle bundle = getBundle(locale);
        final String message = bundle.get(key);
        final Set<String> callerKeySet = createCallerKeySet();
        return resolveLabelVariableMessage(locale, key, message, callerKeySet); // also resolve label variables
    }

    protected HashSet<String> createCallerKeySet() {
        return new LinkedHashSet<String>(4); // order for exception message
    }

    // ===================================================================================
    //                                                                             Present
    //                                                                             =======
    @Override
    public boolean isPresent(Locale locale, String key) {
        final String message = getMessage(locale, key);
        if (message == null) {
            return false;
        } else if (message.startsWith("???") && message.endsWith("???")) {
            return false;
        } else {
            return true;
        }
    }

    // ===================================================================================
    //                                                                    Extends Handling
    //                                                                    ================
    /**
     * Get the bundle by the locale and message names from the assistant director. <br>
     * Returned bundle has merged properties for application and extends messages like this:
     * <pre>
     * e.g. app_message extends common_message, locale = ja
     * common-ex3                 : common_message.properties *last search
     *  |-app-ex3                 : app_message.properties
     *    |-common-ex2            : common_message_ja_JP_xx.properties
     *      |-app-ex2             : app_message_ja_JP_xx.properties
     *        |-common-ex1        : common_message_ja_JP.properties
     *          |-app-ex1         : app_message_ja_JP.properties
     *            |-common-root   : common_message_ja.properties
     *              |-app-root    : app_message_ja.properties *first search
     * </pre>
     * @param locale The locale of current request. (NullAllowed: when system default locale)
     * @return The found bundle that has extends hierarchy. (NotNull)
     */
    protected MessageResourceBundle getBundle(Locale locale) {
        return getBundleResolvedExtends(getAppMessageName(), getExtendsMessageNameList(), locale);
    }

    /**
     * Resolve the extends bundle, basically called by {@link #getBundle(Locale)}. <br>
     * Returned bundle has merged properties for application and extends messages. <br>
     * You can get your message by normal way.
     * @param appMessageName The message name for application. (NotNull)
     * @param extendsNameList The list of extends-message name. (NotNull, EmptyAllowed)
     * @param locale The locale of current request. (NullAllowed: when system default locale)
     * @return The found bundle that has extends hierarchy. (NotNull)
     */
    protected MessageResourceBundle getBundleResolvedExtends(String appMessageName, List<String> extendsNameList, Locale locale) {
        final MessageResourceBundle appBundle = findBundleSimply(appMessageName, locale);
        if (extendsNameList.isEmpty()) { // no extends, no logic
            return appBundle;
        }
        if (isAlreadyExtends(appBundle)) { // means the bundle is cached
            return appBundle;
        }
        synchronized (this) { // synchronize to set up
            if (isAlreadyExtends(appBundle)) {
                return appBundle;
            }
            // set up extends references to the application bundle specified as argument
            // so the bundle has been resolved extends after calling
            setupExtendsReferences(appMessageName, extendsNameList, locale, appBundle);
        }
        return appBundle;
    }

    /**
     * Find The bundle simply (without extends handling), that may be cached. <br>
     * Returned bundle may have language, country, variant and default language's properties
     * for the message name and locale, as hierarchy like this:
     * <pre>
     * e.g. messageName = foo_message, locale = ja
     * parent3      : foo_message.properties *last search
     *  |-parent2   : foo_message_ja_JP_xx.properties
     *    |-parent1 : foo_message_ja_JP.properties
     *      |-root  : foo_message_ja.properties *first search
     *
     * MessageResourceBundle rootBundle = findBundleSimply("foo_message", locale);
     * MessageResourceBundle parent1 = rootBundle.getParent();
     * MessageResourceBundle parent2 = parent1.getParent();
     * MessageResourceBundle parent3 = parent2.getParent();
     * ...
     * </pre>
     * @param messageName The message name for the bundle. (NotNull)
     * @param locale The locale of current request. (NotNull)
     * @return The bundle that contains properties defined at specified message name.
     */
    protected MessageResourceBundle findBundleSimply(String messageName, Locale locale) {
        final Map<Locale, MessageResourceBundle> cachedMessageMap = bundleCacheMap.get(messageName);
        if (cachedMessageMap != null) {
            final MessageResourceBundle cachedBundle = cachedMessageMap.get(locale);
            if (cachedBundle != null) {
                return cachedBundle;
            }
        }
        synchronized (bundleCacheMap) {
            Map<Locale, MessageResourceBundle> localeKeyMap = bundleCacheMap.get(messageName);
            if (localeKeyMap != null) {
                final MessageResourceBundle retryBundle = localeKeyMap.get(locale);
                if (retryBundle != null) {
                    return retryBundle;
                }
            } else {
                localeKeyMap = newConcurrentHashMap(); // concurrent just in case
                bundleCacheMap.put(messageName, localeKeyMap);
            }
            // our hope would be that it has strict cache
            // because this cache is top-resource-driven cache
            // e.g. duplicate instance of default language bundle
            final MessageResourceBundle loadedBundle = loadBundle(messageName, locale);
            localeKeyMap.put(locale, loadedBundle);
        }
        return bundleCacheMap.get(messageName).get(locale);
    }

    protected MessageResourceBundle loadBundle(String messageName, Locale locale) {
        // you should not use MessageResourceBundleFactory directly
        // because logics here use the factory as simple utilities
        // but your operation to the factory directly have influences logics here
        final MessageResourceBundle bundle = MessageResourceBundleFactory.getBundle(messageName, locale);
        MessageResourceBundleFactory.clear();
        return bundle;
    }

    /**
     * Does the application bundle already have extends handling? <br>
     * It returns true if the bundle has a parent instance of {@link MessageResourceBundleObjectiveWrapper}.
     * @param appBundle The bundle for application for determination. (NotNull)
     * @return The determination, true or false.
     */
    protected boolean isAlreadyExtends(MessageResourceBundle appBundle) {
        MessageResourceBundle currentBundle = appBundle;
        boolean found = false;
        while (true) {
            MessageResourceBundle parentBundle = currentBundle.getParent();
            if (parentBundle == null) {
                break;
            }
            if (parentBundle instanceof MessageResourceBundleObjectiveWrapper) {
                found = true;
                break;
            }
            currentBundle = parentBundle;
        }
        return found;
    }

    /**
     * Set up extends references to the application bundle. <br>
     * @param appMessageName The message name for application properties. (NotNull)
     * @param extendsNameList The list of message name for extends properties. The first element is first extends (NotNull)
     * @param locale The locale of current request. (NotNull)
     * @param appBundle The bundle for application that does not set up extends handling yet. (NotNull)
     */
    protected void setupExtendsReferences(String appMessageName, List<String> extendsNameList, Locale locale,
            MessageResourceBundle appBundle) {
        final TreeSet<MessageResourceBundle> hierarchySet = new TreeSet<MessageResourceBundle>();
        final MessageResourceBundle wrappedAppBundle = wrapBundle(appMessageName, appBundle, null);
        hierarchySet.addAll(convertToHierarchyList(wrappedAppBundle));
        int extendsLevel = 1;
        for (String extendsName : extendsNameList) {
            final MessageResourceBundle extendsBundle = findBundleSimply(extendsName, locale);
            final MessageResourceBundle wrappedExtendsBundle = wrapBundle(extendsName, extendsBundle, extendsLevel);
            hierarchySet.addAll(convertToHierarchyList(wrappedExtendsBundle));
            ++extendsLevel;
        }
        for (MessageResourceBundle bundle : hierarchySet) {
            bundle.setParent(null); // initialize
        }
        MessageResourceBundle previousBundle = null;
        for (MessageResourceBundle bundle : hierarchySet) {
            if (previousBundle != null) {
                previousBundle.setParent(bundle);
            }
            previousBundle = bundle;
        }
    }

    /**
     * Wrap the bundle with detail info of message resource. <br>
     * The parents also wrapped.
     * @param messageName The message name for the bundle. (NotNull)
     * @param bundle The bundle of message resource. (NotNull)
     * @param extendsLevel The level as integer for extends. e.g. first extends is 1 (NullAllowed: when application)
     * @return The wrapper for the bundle. (NotNull)
     */
    protected MessageResourceBundleObjectiveWrapper wrapBundle(String messageName, MessageResourceBundle bundle, Integer extendsLevel) {
        final boolean existsDefaultLangProperties = existsDefaultLangProperties(messageName);
        final List<MessageResourceBundle> bundleList = new ArrayList<MessageResourceBundle>();
        bundleList.add(bundle);
        MessageResourceBundle currentBundle = bundle;
        int parentLevel = 1;
        while (true) {
            MessageResourceBundle parentBundle = currentBundle.getParent();
            if (parentBundle == null) {
                break;
            }
            final boolean defaultLang = isDefaultLangBundle(existsDefaultLangProperties, parentBundle);
            currentBundle.setParent(createBundleWrapper(parentBundle, defaultLang, parentLevel, extendsLevel));
            currentBundle = parentBundle;
            ++parentLevel;
        }
        return createBundleWrapper(bundle, isDefaultLangBundle(existsDefaultLangProperties, bundle), null, extendsLevel);
    }

    protected MessageResourceBundleObjectiveWrapper createBundleWrapper(MessageResourceBundle bundle, boolean defaultLang,
            Integer parentLevel, Integer extendsLevel) {
        return new MessageResourceBundleObjectiveWrapper(bundle, defaultLang, parentLevel, extendsLevel);
    }

    protected boolean existsDefaultLangProperties(String messageName) {
        final String path = messageName + PROPERTIES_EXT; // e.g. foo_message.properties
        return LdiResourceUtil.getResourceNoException(path) != null;
    }

    protected boolean isDefaultLangBundle(boolean existsDefaultLangProperties, MessageResourceBundle parentBundle) {
        // default language properties does not have parent (must be last of hierarchy element)
        return existsDefaultLangProperties && parentBundle.getParent() == null;
    }

    /**
     * Convert the bundle and its parents (hierarchy) to list.
     * <pre>
     * e.g. messageName = foo_message, locale = ja
     * parent3      : foo_message.properties *last search
     *  |-parent2   : foo_message_ja_JP_xx.properties
     *    |-parent1 : foo_message_ja_JP.properties
     *      |-root  : foo_message_ja.properties *first search
     *
     *  to
     *
     * list.get(0): foo_message_ja.properties (root)
     * list.get(1): foo_message_ja_JP.properties (parent1)
     * list.get(2): foo_message_ja_JP_xx.properties (parent2)
     * list.get(3): foo_message.properties (parent3)
     * </pre>
     * @param bundle The bundle of message resource. (NotNull)
     * @return The list of bundles. (NotNull)
     */
    protected List<MessageResourceBundle> convertToHierarchyList(MessageResourceBundle bundle) {
        final List<MessageResourceBundle> bundleList = new ArrayList<MessageResourceBundle>();
        bundleList.add(bundle);
        MessageResourceBundle currentBundle = bundle;
        while (true) {
            MessageResourceBundle parentBundle = currentBundle.getParent();
            if (parentBundle == null) {
                break;
            }
            bundleList.add(parentBundle);
            currentBundle = parentBundle;
        }
        return bundleList;
    }

    // ===================================================================================
    //                                                                      Label Handling
    //                                                                      ==============
    /**
     * Resolve label parameters in the arguments.
     * @param locale The locale of current request. (NullAllowed: when system default locale)
     * @param key The key of the message, basically for exception message. (NotNull)
     * @param args The array of arguments for message. (NullAllowed: if null, returns empty list)
     * @return The list of filtered parameters resolved label arguments. (NotNull, EmptyAllowed)
     */
    protected List<Object> resolveLabelParameter(Locale locale, String key, Object[] args) {
        final MessageResourceBundle bundle = getBundle(locale);
        if (args == null || args.length == 0) {
            return DfCollectionUtil.emptyList();
        }
        final List<Object> resolvedList = new ArrayList<Object>(args.length);
        for (Object arg : args) {
            if (canBeLabelKey(arg)) {
                final String labelKey = (String) arg;
                final String label = bundle.get(labelKey);
                if (label != null) {
                    resolvedList.add(label);
                    continue;
                } else {
                    throwMessageLabelByLabelParameterNotFoundException(locale, key, labelKey);
                }
            }
            resolvedList.add(arg);
        }
        return resolvedList;
    }

    protected boolean canBeLabelKey(Object arg) {
        return arg instanceof String && ((String) arg).startsWith(LABELS_KEY_PREFIX);
    }

    protected void throwMessageLabelByLabelParameterNotFoundException(Locale locale, String key, String labelKey) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the label by the label parameter.");
        br.addItem("Locale");
        br.addElement(locale);
        br.addItem("Message Key");
        br.addElement(key);
        br.addItem("Label Parameter");
        br.addElement(labelKey);
        final String msg = br.buildExceptionMessage();
        throw new MessageLabelByLabelParameterNotFoundException(msg);
    }

    /**
     * Resolve embedded label variables on the message.
     * <pre>
     * e.g. List of Member Purchase
     *  labels.memberPurchase = Member Purchase
     *  labels.list = List
     *  labels.memberPurchase.list = @[labels.list] of @[labels.memberPurchase]
     * </pre>
     * @param locale The locale of current request. (NullAllowed: when system default locale)
     * @param key The key of the message, basically for exception message. (NotNull)
     * @param message The plain message, might have label variables. (NullAllowed: if null, returns null)
     * @param callerKeySet The set of key that calls this to suppress infinity loop. (NotNull)
     * @return The resolved message. (NullAllowed: if no message, returns null)
     */
    protected String resolveLabelVariableMessage(Locale locale, String key, String message, Set<String> callerKeySet) {
        final String beginMark = LABEL_VARIABLE_BEGIN_MARK;
        final String endMark = LABEL_VARIABLE_END_MARK;
        if (message == null || !message.contains(beginMark) || !message.contains(endMark)) {
            return message;
        }
        final List<ScopeInfo> scopeList = DfStringUtil.extractScopeList(message, beginMark, endMark);
        if (scopeList.isEmpty()) {
            return message;
        }
        callerKeySet.add(key);
        final MessageResourceBundle bundle = getBundle(locale);
        String resolved = message;
        for (ScopeInfo scopeInfo : scopeList) {
            final String labelKey = scopeInfo.getContent();
            final String labelVar = scopeInfo.getScope();
            if (!canBeLabelKey(labelKey)) {
                throwMessageLabelByLabelVariableInvalidKeyException(locale, key, resolved, labelVar);
            }
            if (callerKeySet.contains(labelKey)) { // infinity loop
                throwMessageLabelByLabelVariableInfinityLoopException(locale, labelVar, callerKeySet);
            }
            String label = bundle.get(labelKey);
            if (label != null) {
                label = resolveLabelVariableMessage(locale, labelKey, label, callerKeySet);
                resolved = DfStringUtil.replace(resolved, labelVar, label);
            } else {
                throwMessageLabelByLabelVariableNotFoundException(locale, key, resolved, labelVar);
            }
        }
        callerKeySet.remove(key);
        return resolved;
    }

    protected void throwMessageLabelByLabelVariableInvalidKeyException(Locale locale, String key, String message, String labelVar) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The label key of the label variable was invalid.");
        br.addItem("Advice");
        br.addElement("Label key of label variable should start with 'labels.'");
        br.addElement("like this:");
        br.addElement("  (x): abc.foo");
        br.addElement("  (x): lable.bar");
        br.addElement("  (o): labels.foo");
        br.addItem("Locale");
        br.addElement(locale);
        br.addItem("Specified Key");
        br.addElement(key);
        br.addItem("Message");
        br.addElement(message);
        br.addItem("Label Variable");
        br.addElement(labelVar);
        final String msg = br.buildExceptionMessage();
        throw new MessageLabelByLabelVariableInvalidKeyException(msg);
    }

    protected void throwMessageLabelByLabelVariableInfinityLoopException(Locale locale, String labelVar, Set<String> callerKeySet) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Found the infinity loop in the message.");
        br.addItem("Locale");
        br.addElement(locale);
        br.addItem("Infinity Label");
        br.addElement(labelVar);
        br.addItem("Variable Tree");
        br.addElement(callerKeySet);
        final String msg = br.buildExceptionMessage();
        throw new MessageLabelByLabelVariableInfinityLoopException(msg);
    }

    protected void throwMessageLabelByLabelVariableNotFoundException(Locale locale, String key, String message, String labelVar) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the label by the label variable.");
        br.addItem("Locale");
        br.addElement(locale);
        br.addItem("Specified Key");
        br.addElement(key);
        br.addItem("Message");
        br.addElement(message);
        br.addItem("Label Variable");
        br.addElement(labelVar);
        final String msg = br.buildExceptionMessage();
        throw new MessageLabelByLabelVariableNotFoundException(msg);
    }

    // ===================================================================================
    //                                                                           HotDeploy
    //                                                                           =========
    @Override
    public void dispose() {
        bundleCacheMap.clear();
        formatMap.clear();
        initialized = false;
    }

    // ===================================================================================
    //                                                                           Component
    //                                                                           =========
    protected FwAssistantDirector getAssistantDirector() {
        if (cachedAssistantDirector != null) {
            return cachedAssistantDirector;
        }
        synchronized (this) {
            if (cachedAssistantDirector != null) {
                return cachedAssistantDirector;
            }
            cachedAssistantDirector = ContainerUtil.getComponent(FwAssistantDirector.class);
        }
        return cachedAssistantDirector;
    }

    protected String getAppMessageName() {
        if (cachedAppMessageName != null) {
            return cachedAppMessageName;
        }
        synchronized (this) {
            if (cachedAppMessageName != null) {
                return cachedAppMessageName;
            }
            final FwAssistantDirector assistantDirector = getAssistantDirector();
            final FwWebDirection direction = assistantDirector.assistWebDirection();
            cachedAppMessageName = direction.assistAppMessageName();
        }
        return cachedAppMessageName;
    }

    protected List<String> getExtendsMessageNameList() {
        if (cachedExtendsMessageNameList != null) {
            return cachedExtendsMessageNameList;
        }
        synchronized (this) {
            if (cachedExtendsMessageNameList != null) {
                return cachedExtendsMessageNameList;
            }
            final FwAssistantDirector assistantDirector = getAssistantDirector();
            final FwWebDirection direction = assistantDirector.assistWebDirection();
            cachedExtendsMessageNameList = direction.assistExtendsMessageNameList();
        }
        return cachedExtendsMessageNameList;
    }

    // ===================================================================================
    //                                                                      General Helper
    //                                                                      ==============
    protected static <KEY, VALUE> ConcurrentHashMap<KEY, VALUE> newConcurrentHashMap() {
        return DfCollectionUtil.newConcurrentHashMap();
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final String title = DfTypeUtil.toClassTitle(this);
        sb.append(title).append(":{");
        sb.append("application=");
        if (cachedAppMessageName != null) {
            sb.append(cachedAppMessageName);
        } else {
            sb.append("not initialized yet");
        }
        final Set<String> cachedSet = bundleCacheMap.keySet();
        sb.append(", cached=[");
        if (!cachedSet.isEmpty()) {
            buildCacheDisplay(sb);
        } else {
            sb.append("no cached bundle");
        }
        sb.append("]}");
        return sb.toString();
    }

    protected void buildCacheDisplay(StringBuilder sb) {
        int messageIndex = 0;
        for (Entry<String, Map<Locale, MessageResourceBundle>> entry : bundleCacheMap.entrySet()) {
            final String key = entry.getKey();
            final Map<Locale, MessageResourceBundle> localeBundleMap = entry.getValue();
            if (messageIndex > 0) {
                sb.append(", ");
            }
            sb.append(key);
            sb.append("(");
            int localeIndex = 0;
            for (Locale locale : localeBundleMap.keySet()) {
                if (localeIndex > 0) {
                    sb.append(", ");
                }
                sb.append(locale);
                ++localeIndex;
            }
            sb.append(")");
            ++messageIndex;
        }
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected String escape(String string) {
        if (!isEscape()) {
            return string;
        }
        if ((string == null) || (string.indexOf('\'') < 0)) {
            return string;
        }
        int n = string.length();
        StringBuffer sb = new StringBuffer(n);
        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);
            if (ch == '\'') {
                sb.append('\'');
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    protected String localeKey(Locale locale) {
        return (locale == null) ? "" : locale.toString();
    }

    protected String messageKey(Locale locale, String key) {
        return (localeKey(locale) + "." + key);
    }

    protected String messageKey(String localeKey, String key) {
        return (localeKey + "." + key);
    }

    // ===================================================================================
    //                                                                       Assist Helper
    //                                                                       =============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            throw new IllegalArgumentException("The variableName should not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
        }
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public boolean getReturnNull() {
        return returnNull;
    }

    public void setReturnNull(boolean returnNull) {
        this.returnNull = returnNull;
    }

    public boolean isEscape() {
        return escape;
    }

    public void setEscape(boolean escape) {
        this.escape = escape;
    }
}
