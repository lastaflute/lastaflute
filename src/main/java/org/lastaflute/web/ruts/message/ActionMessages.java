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
import java.util.stream.Collectors;

/**
 * @author modified by jflute (originated in Struts)
 */
public class ActionMessages implements Serializable {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = 1L;

    public static final String GLOBAL_PROPERTY_KEY = "lastaflute.message.GLOBAL_PROPERTY";

    protected static final Comparator<ActionMessageItem> actionItemComparator = new Comparator<ActionMessageItem>() {
        public int compare(ActionMessageItem item1, ActionMessageItem item2) {
            return item1.getOrder() - item2.getOrder();
        }
    };

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Map<String, ActionMessageItem> messageMap = new LinkedHashMap<String, ActionMessageItem>();
    protected boolean accessed;
    protected int itemCount;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionMessages() {
    }

    public ActionMessages(ActionMessages messages) {
        add(messages);
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
        for (String property : messages.toPropertyList()) {
            for (Iterator<ActionMessage> ite = messages.accessByIteratorOf(property); ite.hasNext();) {
                add(property, ite.next());
            }
        }
    }

    // ===================================================================================
    //                                                                         Get Message
    //                                                                         ===========
    public Iterator<ActionMessage> accessByFlatIterator() {
        accessed = true;
        if (messageMap.isEmpty()) {
            return Collections.emptyIterator();
        }
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

    public List<String> toPropertyList() {
        if (messageMap.isEmpty()) {
            return Collections.emptyList();
        }
        final List<ActionMessageItem> itemList = new ArrayList<ActionMessageItem>(messageMap.values());
        Collections.sort(itemList, actionItemComparator);
        final List<String> propList = itemList.stream().map(item -> item.getProperty()).collect(Collectors.toList());
        return Collections.unmodifiableList(propList);
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
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return this.messageMap.toString();
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
            return this.messageList.toString();
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
