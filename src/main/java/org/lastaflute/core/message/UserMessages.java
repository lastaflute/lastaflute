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
package org.lastaflute.core.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dbflute.util.Srl;

/**
 * @author jflute
 */
public class UserMessages implements Serializable {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = 1L;

    /** The property key for global property (non-specific property) for protocol with HTML, JavaScipt. */
    public static final String GLOBAL_PROPERTY_KEY = "_global";

    protected static final String LABELS_PREFIX = "labels.";
    protected static final String LABELS_BEGIN_MARK = "{" + LABELS_PREFIX;
    protected static final String LABELS_END_MARK = "}";

    protected static final UserMessages EMPTY_MESSAGES = new UserMessages().lock();

    protected static final Comparator<UserMessageItem> actionItemComparator = (item1, item2) -> {
        return item1.getItemOrder() - item2.getItemOrder();
    };

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Map<String, UserMessageItem> messageMap = new LinkedHashMap<String, UserMessageItem>();
    protected boolean accessed;
    protected int itemCount;
    protected Map<String, Object> successAttributeMap; // lazy loaded
    protected boolean locked;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public UserMessages() {
    }

    public static UserMessages createAsOneGlobal(String messageKey, Object... args) {
        final UserMessages messages = new UserMessages();
        messages.add(GLOBAL_PROPERTY_KEY, new UserMessage(messageKey, args));
        return messages;
    }

    public static UserMessages empty() {
        return EMPTY_MESSAGES;
    }

    // ===================================================================================
    //                                                                         Add Message
    //                                                                         ===========
    public void add(String property, UserMessage message) {
        assertArgumentNotNull("property", property);
        assertArgumentNotNull("message", message);
        assertLocked();
        final UserMessageItem item = getPropertyItem(property);
        final List<UserMessage> messageList;
        if (item == null) {
            ++itemCount;
            messageList = new ArrayList<UserMessage>();
            final String filtered = filterProperty(property);
            messageMap.put(filtered, newUserMessageItem(messageList, itemCount, filtered));
        } else {
            messageList = item.getMessageList();
        }
        messageList.add(message);
    }

    protected String filterProperty(String property) {
        final String resolved = resolveLabelProperty(property);
        return resolved != null ? resolved : property;
    }

    protected UserMessageItem newUserMessageItem(List<UserMessage> messageList, int itemCount, String property) {
        return new UserMessageItem(messageList, itemCount, property);
    }

    public void add(UserMessages messages) {
        assertArgumentNotNull("messages", messages);
        assertLocked();
        for (String property : messages.toPropertySet()) {
            for (Iterator<UserMessage> ite = messages.accessByIteratorOf(property); ite.hasNext();) {
                add(property, ite.next());
            }
        }
    }

    // ===================================================================================
    //                                                                      Access Message
    //                                                                      ==============
    public Iterator<UserMessage> accessByFlatIterator() {
        assertLocked();
        accessed = true;
        return doAccessByFlatIterator();
    }

    public Iterator<UserMessage> accessByIteratorOf(String property) {
        assertArgumentNotNull("property", property);
        assertLocked();
        accessed = true;
        return doAccessByIteratorOf(property);
    }

    public Iterator<UserMessage> silentAccessByFlatIterator() { // e.g. for logging
        return doAccessByFlatIterator();
    }

    public Iterator<UserMessage> silentAccessByIteratorOf(String property) { // e.g. for logging
        assertArgumentNotNull("property", property);
        return doAccessByIteratorOf(property);
    }

    protected Iterator<UserMessage> doAccessByFlatIterator() {
        if (messageMap.isEmpty()) {
            return Collections.emptyIterator();
        }
        final List<UserMessage> msgList = new ArrayList<UserMessage>();
        final List<UserMessageItem> itemList = new ArrayList<UserMessageItem>(messageMap.size());
        for (Iterator<UserMessageItem> ite = messageMap.values().iterator(); ite.hasNext();) {
            itemList.add(ite.next());
        }
        Collections.sort(itemList, actionItemComparator);
        for (Iterator<UserMessageItem> ite = itemList.iterator(); ite.hasNext();) {
            for (Iterator<UserMessage> messages = ite.next().getMessageList().iterator(); messages.hasNext();) {
                msgList.add(messages.next());
            }
        }
        return msgList.iterator();
    }

    protected Iterator<UserMessage> doAccessByIteratorOf(String property) {
        if (messageMap.isEmpty()) {
            return Collections.emptyIterator();
        }
        final UserMessageItem item = getPropertyItem(property);
        return item != null ? item.getMessageList().iterator() : Collections.emptyIterator();
    }

    // ===================================================================================
    //                                                                   Property Handling
    //                                                                   =================
    public boolean hasMessageOf(String property) {
        final UserMessageItem item = getPropertyItem(property);
        return item != null && !item.getMessageList().isEmpty();
    }

    public boolean hasMessageOf(String property, String key) {
        final UserMessageItem item = getPropertyItem(property);
        return item != null && item.getMessageList().stream().anyMatch(message -> message.getMessageKey().equals(key));
    }

    // #pending implement with type-safe function by jflute (2017/02/03)
    ///**
    // * Is the property non-error? (has no message? correct property?)
    // * <pre>
    // * private void moreValidate(SeaForm form, LandMessages messages) {
    // *     if (messages.isNonError(form.piari)) {
    // *         // ...validating by program for form.piari here
    // *     }
    // * }
    // * </pre>
    // * @param property the name of property, which may have user messages. (NotNull)
    // * @return The determination, true or false.
    // */
    //public boolean isNonError(String property) {
    //    return !hasMessageOf(property);
    //}

    protected UserMessageItem getPropertyItem(String property) {
        final UserMessageItem item = messageMap.get(property);
        if (item != null) {
            return item;
        } else {
            final String resolved = resolveLabelProperty(property);
            return resolved != null ? messageMap.get(resolved) : null;
        }
    }

    public Set<String> toPropertySet() {
        return !messageMap.isEmpty() ? Collections.unmodifiableSet(messageMap.keySet()) : Collections.emptySet();
    }

    protected String resolveLabelProperty(String property) {
        final String beginMark = LABELS_BEGIN_MARK;
        final String endMark = LABELS_END_MARK;
        if (Srl.isQuotedAnything(property, beginMark, endMark)) {
            return Srl.unquoteAnything(property, beginMark, endMark);
        } else {
            final String labelsPrefix = LABELS_PREFIX;
            if (property.startsWith(labelsPrefix)) {
                return Srl.removePrefix(property, labelsPrefix);
            } else {
                return null;
            }
        }
    }

    // ===================================================================================
    //                                                                   Various Operation
    //                                                                   =================
    public void clear() {
        assertLocked();
        messageMap.clear();
    }

    public boolean isEmpty() {
        return messageMap.isEmpty();
    }

    public boolean isAccessed() {
        return accessed;
    }

    public int size() {
        int total = 0;
        for (Iterator<UserMessageItem> ite = messageMap.values().iterator(); ite.hasNext();) {
            total += ite.next().getMessageList().size();
        }
        return total;
    }

    public int size(String property) {
        assertArgumentNotNull("property", property);
        final UserMessageItem item = getPropertyItem(property);
        return item != null ? item.getMessageList().size() : 0;
    }

    // ===================================================================================
    //                                                                        Success Data
    //                                                                        ============
    /**
     * Save validation success attribute, derived in validation process, e.g. selected data, <br>
     * to avoid duplicate database access between validation and main logic.
     * <pre>
     * String <span style="color: #553000">keyOfProduct</span> = Product.<span style="color: #70226C">class</span>.getName();
     * ValidationSuccess <span style="color: #553000">success</span> = validate(<span style="color: #553000">form</span>, <span style="color: #553000">messages</span> <span style="font-size: 120%">-</span>&gt;</span> {
     *     ...
     *     selectProduct().ifPresent(<span style="color: #553000">product</span> <span style="font-size: 120%">-</span>&gt;</span> {}, () <span style="font-size: 120%">-</span>&gt;</span> {
     *         <span style="color: #553000">messages</span>.<span style="color: #CC4747">saveSuccessData</span>(<span style="color: #553000">keyOfProduct</span>, <span style="color: #553000">product</span>);
     *     }).orElse(() <span style="font-size: 120%">-</span>&gt;</span> {}, () <span style="font-size: 120%">-</span>&gt;</span> {
     *         <span style="color: #553000">messages</span>.addConstraint...();
     *     });
     * }, () <span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">return</span> asHtml(path_Product_ProductListJsp);
     * });
     * <span style="color: #553000">success</span>.<span style="color: #994747">getAttribute</span>(keyOfProduct).alwaysPresent(product <span style="font-size: 120%">-</span>&gt;</span> {
     *     ...
     * });
     * </pre>
     * @param key The string key of the success attribute. (NotNull)
     * @param value The value of success attribute. (NotNull)
     */
    public void saveSuccessAttribute(String key, Object value) {
        assertArgumentNotNull("key", key);
        assertArgumentNotNull("value", value);
        assertLocked();
        if (successAttributeMap == null) {
            successAttributeMap = new LinkedHashMap<String, Object>(4);
        }
        successAttributeMap.put(key, value);
    }

    /**
     * @return The read-only map of success attribute. (NotNull)
     */
    public Map<String, Object> getSuccessAttributeMap() {
        return successAttributeMap != null ? Collections.unmodifiableMap(successAttributeMap) : Collections.emptyMap();
    }

    // ===================================================================================
    //                                                                               Lock
    //                                                                              ======
    protected UserMessages lock() {
        locked = true;
        return this;
    }

    protected void assertLocked() {
        if (locked) {
            throw new IllegalStateException("Cannot change the status of the user messages: " + this);
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
        return messageMap.toString();
    }

    // ===================================================================================
    //                                                                        Message Item
    //                                                                        ============
    protected static class UserMessageItem implements Serializable {

        private static final long serialVersionUID = 1L;

        protected final List<UserMessage> messageList;
        protected final int itemOrder;
        protected final String property;

        public UserMessageItem(List<UserMessage> messageList, int itemOrder, String property) {
            this.messageList = messageList;
            this.itemOrder = itemOrder;
            this.property = property;
        }

        @Override
        public String toString() {
            return messageList.toString();
        }

        public List<UserMessage> getMessageList() {
            return messageList;
        }

        public int getItemOrder() {
            return itemOrder;
        }

        public String getProperty() {
            return property;
        }
    }
}
