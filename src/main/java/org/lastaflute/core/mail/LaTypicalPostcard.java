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
package org.lastaflute.core.mail;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.dbflute.mail.DeliveryCategory;
import org.dbflute.mail.Postcard;
import org.dbflute.util.DfTypeUtil;

/**
 * @author jflute
 * @since 0.6.0 (2015/05/09 Saturday)
 */
public abstract class LaTypicalPostcard implements LaMailPostcard {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Postcard postcard;
    protected final Map<String, Object> variableMap;
    protected boolean strictAddress = true; // as default

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LaTypicalPostcard() {
        postcard = createNativePostcard();
        variableMap = createVariableMap();
        postcard.useBodyFile(getBodyFile()).useTemplateText(variableMap);
    }

    protected Postcard createNativePostcard() {
        return new Postcard();
    }

    protected Map<String, Object> createVariableMap() {
        return new LinkedHashMap<String, Object>();
    }

    protected abstract String getBodyFile();

    // ===================================================================================
    //                                                                    Postcard Request
    //                                                                    ================
    // -----------------------------------------------------
    //                                              Category
    //                                              --------
    @SuppressWarnings("unchecked")
    public <SELF extends LaTypicalPostcard> SELF asDeliveryCategory(DeliveryCategory category) {
        postcard.asDeliveryCategory(category);
        return (SELF) this;
    }

    // -----------------------------------------------------
    //                                               Address
    //                                               -------
    public void setFrom(String from) {
        assertArgumentNotNull("from", from);
        postcard.setFrom(createAddress(from));
    }

    public void addTo(String to) {
        assertArgumentNotNull("to", to);
        postcard.addTo(createAddress(to));
    }

    public void addCc(String cc) {
        assertArgumentNotNull("cc", cc);
        postcard.addCc(createAddress(cc));
    }

    public void addBcc(String bcc) {
        assertArgumentNotNull("bcc", bcc);
        postcard.addBcc(createAddress(bcc));
    }

    protected Address createAddress(String address) {
        try {
            return new InternetAddress(address, isStrictAddress());
        } catch (AddressException e) {
            String msg = "Failed to create internet address: " + address;
            throw new IllegalStateException(msg, e);
        }
    }

    // ===================================================================================
    //                                                                        Variable Map
    //                                                                        ============
    protected void registerVariable(String key, String value) {
        assertArgumentNotNull("key", key);
        assertArgumentNotNull("value", value);
        variableMap.put(key, value);
    }

    // ===================================================================================
    //                                                                              Option
    //                                                                              ======
    public LaTypicalPostcard suppressStrictAddress() {
        strictAddress = false;
        return this;
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
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return DfTypeUtil.toClassTitle(this) + ":{" + postcard + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    @Override
    public Postcard toNativePostcard() {
        return postcard;
    }

    public Map<String, Object> getVariableMap() {
        return variableMap;
    }

    public boolean isStrictAddress() {
        return strictAddress;
    }
}
