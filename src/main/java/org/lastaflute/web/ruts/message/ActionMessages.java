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
package org.lastaflute.web.ruts.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author modified by jflute (originated in Struts)
 */
public class ActionMessages implements Serializable {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = 1L;

    /** The property key for global property (non-specific property) for protocol with HTML, JavaScipt. */
    public static final String GLOBAL_PROPERTY_KEY = "_global";

    protected static final Comparator<ActionMessageItem> actionItemComparator = (item1, item2) -> {
        return item1.getOrder() - item2.getOrder();
    };

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Map<String, ActionMessageItem> messageMap = new LinkedHashMap<String, ActionMessageItem>();
    protected boolean accessed;
    protected int itemCount;
    protected Map<String, Object> successAttributeMap; // lazy loaded

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionMessages() {
    }

    // ===================================================================================
    //                                                                         Add Message
    //                                                                         ===========
    public void add(String property, ActionMessage message) {
        ActionMessageItem item = messageMap.get(property);
        final List<ActionMessage> list;
        if (item == null) {
            ++itemCount;
            list = new ArrayList<ActionMessage>();
            item = new ActionMessageItem(list, itemCount, property);
            messageMap.put(property, item);
        } else {
            list = item.getList();
        }
        list.add(message);
    }

    public void add(ActionMessages messages) {
        if (messages == null) {
            return;
        }
        for (String property : messages.toPropertySet()) {
            for (Iterator<ActionMessage> ite = messages.accessByIteratorOf(property); ite.hasNext();) {
                add(property, ite.next());
            }
        }
    }

    // ===================================================================================
    //                                                                      Access Message
    //                                                                      ==============
    public Iterator<ActionMessage> accessByFlatIterator() {
        accessed = true;
        if (messageMap.isEmpty()) {
            return Collections.emptyIterator();
        }
        return doAccessByFlatIterator();
    }

    protected Iterator<ActionMessage> doAccessByFlatIterator() {
        final List<ActionMessage> msgList = new ArrayList<ActionMessage>();
        final List<ActionMessageItem> itemList = new ArrayList<ActionMessageItem>(messageMap.size());
        for (Iterator<ActionMessageItem> ite = messageMap.values().iterator(); ite.hasNext();) {
            itemList.add(ite.next());
        }
        Collections.sort(itemList, actionItemComparator);
        for (Iterator<ActionMessageItem> ite = itemList.iterator(); ite.hasNext();) {
            for (Iterator<ActionMessage> messages = ite.next().getList().iterator(); messages.hasNext();) {
                msgList.add(messages.next());
            }
        }
        return msgList.iterator();
    }

    public Iterator<ActionMessage> accessByIteratorOf(String property) {
        accessed = true;
        return doAccessByIteratorOf(property);
    }

    public Iterator<ActionMessage> nonAccessByIteratorOf(String property) { // e.g. for logging
        return doAccessByIteratorOf(property);
    }

    protected Iterator<ActionMessage> doAccessByIteratorOf(String property) {
        final ActionMessageItem item = messageMap.get(property);
        return item != null ? item.getList().iterator() : Collections.emptyIterator();
    }

    // ===================================================================================
    //                                                                   Property Handling
    //                                                                   =================
    public boolean hasMessageOf(String property) {
        final ActionMessageItem item = messageMap.get(property);
        return item != null && !item.getList().isEmpty();
    }

    public boolean hasMessageOf(String property, String key) {
        final ActionMessageItem item = messageMap.get(property);
        return item != null && item.getList().stream().anyMatch(message -> message.getKey().equals(key));
    }

    public Set<String> toPropertySet() {
        return !messageMap.isEmpty() ? Collections.unmodifiableSet(messageMap.keySet()) : Collections.emptySet();
    }

    // ===================================================================================
    //                                                                   Various Operation
    //                                                                   =================
    public void clear() {
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
        for (Iterator<ActionMessageItem> ite = messageMap.values().iterator(); ite.hasNext();) {
            total += ite.next().getList().size();
        }
        return total;
    }

    public int size(String property) {
        final ActionMessageItem item = messageMap.get(property);
        return (item == null) ? 0 : item.getList().size();
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
    protected static class ActionMessageItem implements Serializable {

        private static final long serialVersionUID = 1L;

        protected final List<ActionMessage> messageList;
        protected final int itemOrder;
        protected final String property;

        public ActionMessageItem(List<ActionMessage> messageList, int itemOrder, String property) {
            this.messageList = messageList;
            this.itemOrder = itemOrder;
            this.property = property;
        }

        @Override
        public String toString() {
            return messageList.toString();
        }

        public List<ActionMessage> getList() {
            return messageList;
        }

        public int getOrder() {
            return itemOrder;
        }

        public String getProperty() {
            return property;
        }
    }
}
