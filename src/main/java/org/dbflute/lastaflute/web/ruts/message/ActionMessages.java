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
package org.dbflute.lastaflute.web.ruts.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    protected boolean accessed;
    protected final Map<String, ActionMessageItem> messages = new HashMap<String, ActionMessageItem>();
    protected int itemCount;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionMessages() {
        super();
    }

    public ActionMessages(ActionMessages messages) {
        super();
        this.add(messages);
    }

    // ===================================================================================
    //                                                                         Add Message
    //                                                                         ===========
    public void add(String property, ActionMessage message) {
        ActionMessageItem item = messages.get(property);
        final List<ActionMessage> list;
        if (item == null) {
            ++itemCount;
            list = new ArrayList<ActionMessage>();
            item = new ActionMessageItem(list, itemCount, property);
            messages.put(property, item);
        } else {
            list = item.getList();
        }
        list.add(message);
    }

    public void add(ActionMessages messages) {
        if (messages == null) {
            return;
        }
        final Iterator<String> props = messages.properties();
        while (props.hasNext()) {
            final String property = (String) props.next();
            final Iterator<ActionMessage> msgs = messages.get(property);
            while (msgs.hasNext()) {
                add(property, (ActionMessage) msgs.next());
            }
        }
    }

    // ===================================================================================
    //                                                                         Get Message
    //                                                                         ===========
    public Iterator<ActionMessage> get() {
        accessed = true;
        if (messages.isEmpty()) {
            return Collections.emptyIterator();
        }
        final List<ActionMessage> results = new ArrayList<ActionMessage>();
        final List<ActionMessageItem> actionItems = new ArrayList<ActionMessageItem>();
        for (Iterator<ActionMessageItem> ite = messages.values().iterator(); ite.hasNext();) {
            actionItems.add(ite.next());
        }
        Collections.sort(actionItems, actionItemComparator);
        for (Iterator<ActionMessageItem> ite = actionItems.iterator(); ite.hasNext();) {
            final ActionMessageItem ami = ite.next();
            for (Iterator<ActionMessage> messages = ami.getList().iterator(); messages.hasNext();) {
                results.add(messages.next());
            }
        }
        return results.iterator();
    }

    public Iterator<ActionMessage> get(String property) {
        accessed = true;
        final ActionMessageItem item = messages.get(property);
        if (item == null) {
            return Collections.emptyIterator();
        } else {
            return item.getList().iterator();
        }
    }

    // ===================================================================================
    //                                                                   Various Operation
    //                                                                   =================
    public void clear() {
        messages.clear();
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public boolean isAccessed() {
        return accessed;
    }

    public Iterator<String> properties() {
        if (messages.isEmpty()) {
            return Collections.emptyIterator();
        }
        final List<String> results = new ArrayList<String>();
        final List<ActionMessageItem> actionItems = new ArrayList<ActionMessageItem>();
        for (Iterator<ActionMessageItem> ite = messages.values().iterator(); ite.hasNext();) {
            actionItems.add(ite.next());
        }
        Collections.sort(actionItems, actionItemComparator);
        for (Iterator<ActionMessageItem> ite = actionItems.iterator(); ite.hasNext();) {
            results.add(ite.next().getProperty());
        }
        return results.iterator();
    }

    public int size() {
        int total = 0;
        for (Iterator<ActionMessageItem> ite = messages.values().iterator(); ite.hasNext();) {
            total += ite.next().getList().size();
        }
        return total;
    }

    public int size(String property) {
        final ActionMessageItem item = messages.get(property);
        return (item == null) ? 0 : item.getList().size();
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return this.messages.toString();
    }

    // ===================================================================================
    //                                                                        Message Item
    //                                                                        ============
    protected class ActionMessageItem implements Serializable {

        private static final long serialVersionUID = 1L;

        protected final List<ActionMessage> messageList;
        protected final int itemOrder;
        protected final String property;

        public ActionMessageItem(List<ActionMessage> messageList, int itemOrder, String property) {
            this.messageList = messageList;
            this.itemOrder = itemOrder;
            this.property = property;
        }

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
