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

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.dbflute.mail.DeliveryCategory;
import org.dbflute.mail.Postcard;
import org.dbflute.mail.Postcard.BodyFileOption;
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
    protected final BodyFileOption bodyFileOption;
    protected boolean strictAddress = true; // as default

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LaTypicalPostcard() {
        postcard = createNativePostcard();
        variableMap = createVariableMap();
        bodyFileOption = useBodyFile();
    }

    protected Postcard createNativePostcard() {
        return new Postcard();
    }

    protected Map<String, Object> createVariableMap() {
        final String[] propertyNames = getPropertyNames();
        final Map<String, Object> variableMap = new LinkedHashMap<String, Object>(propertyNames.length);
        for (String propertyName : propertyNames) {
            variableMap.put(propertyName, null); // default elements for parameter comment
        }
        return variableMap;
    }

    protected BodyFileOption useBodyFile() {
        final BodyFileOption option = postcard.useBodyFile(getBodyFile());
        option.useTemplateText(variableMap);
        return option;
    }

    // -----------------------------------------------------
    //                                             Meta Data
    //                                             ---------
    protected abstract String getBodyFile();

    protected abstract String[] getPropertyNames();

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
    //                                          From Address
    //                                          ------------
    // public methods are prepared at sub-class
    protected void doSetFrom(String from, String personal) {
        assertArgumentNotEmpty("from", from);
        assertArgumentNotEmpty("personal", personal); // only from required
        postcard.setFrom(createAddress(from, personal));
    }

    // -----------------------------------------------------
    //                                            To Address
    //                                            ----------
    protected void doAddTo(String to) {
        assertArgumentNotEmpty("to", to);
        actuallyAddTo(to, null);
    }

    protected void doAddTo(String to, String personal) {
        assertArgumentNotEmpty("to", to);
        assertArgumentNotEmpty("personal", personal);
        actuallyAddTo(to, personal);
    }

    protected void actuallyAddTo(String to, String personal) {
        postcard.addTo(createAddress(to, personal));
    }

    // -----------------------------------------------------
    //                                            Cc Address
    //                                            ----------
    protected void doAddCc(String cc) {
        assertArgumentNotEmpty("cc", cc);
        actuallyAddCc(cc, null);
    }

    protected void doAddCc(String cc, String personal) {
        assertArgumentNotEmpty("cc", cc);
        assertArgumentNotEmpty("personal", personal);
        actuallyAddCc(cc, personal);
    }

    protected void actuallyAddCc(String cc, String personal) {
        postcard.addCc(createAddress(cc, personal));
    }

    // -----------------------------------------------------
    //                                           Bcc Address
    //                                           -----------
    protected void doAddBcc(String bcc) {
        assertArgumentNotEmpty("bcc", bcc);
        actuallyAddBcc(bcc, null);
    }

    protected void doAddBcc(String bcc, String personal) {
        assertArgumentNotEmpty("bcc", bcc);
        assertArgumentNotEmpty("personal", personal);
        actuallyAddBcc(bcc, personal);
    }

    protected void actuallyAddBcc(String bcc, String personal) {
        postcard.addBcc(createAddress(bcc, personal));
    }

    // -----------------------------------------------------
    //                                       ReplyTo Address
    //                                       ---------------
    protected void doAddReplyTo(String replyTo) {
        assertArgumentNotEmpty("replyTo", replyTo);
        actuallyAddReplyTo(replyTo, null);
    }

    protected void doAddReplyTo(String replyTo, String personal) {
        assertArgumentNotEmpty("replyTo", replyTo);
        assertArgumentNotEmpty("personal", personal);
        actuallyAddReplyTo(replyTo, personal);
    }

    protected void actuallyAddReplyTo(String replyTo, String personal) {
        postcard.addReplyTo(createAddress(replyTo, personal));
    }

    // -----------------------------------------------------
    //                                        Address Assist
    //                                        --------------
    protected Address createAddress(String address, String personal) {
        final String encoding = getPersonalEncoding();
        final InternetAddress internetAddress;
        try {
            internetAddress = newInternetAddress(address, isStrictAddress());
        } catch (AddressException e) {
            throw new IllegalStateException("Failed to create internet address: " + address, e);
        }
        if (personal != null) {
            try {
                internetAddress.setPersonal(personal, encoding);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("Unknown encoding for personal: encoding=" + encoding + " personal=" + personal, e);
            }
        }
        return internetAddress;
    }

    protected InternetAddress newInternetAddress(String address, boolean strict) throws AddressException {
        return new InternetAddress(address, strict);
    }

    protected String getPersonalEncoding() {
        return "UTF-8";
    }

    // -----------------------------------------------------
    //                                            Attachment
    //                                            ----------
    public void attachPlainText(String filenameOnHeader, InputStream resourceStream, String textEncoding) {
        assertArgumentNotEmpty("filenameOnHeader", filenameOnHeader);
        assertArgumentNotNull("resourceStream", resourceStream);
        assertArgumentNotEmpty("textEncoding", textEncoding);
        postcard.attachPlainText(filenameOnHeader, resourceStream, textEncoding);
    }

    public void attachVarious(String filenameOnHeader, String contentType, InputStream resourceStream) {
        assertArgumentNotEmpty("filenameOnHeader", filenameOnHeader);
        assertArgumentNotEmpty("contentType", contentType);
        assertArgumentNotNull("resourceStream", resourceStream);
        postcard.attachVarious(filenameOnHeader, contentType, resourceStream);
    }

    // -----------------------------------------------------
    //                                       Receiver Locale
    //                                       ---------------
    public void asReceiverLocale(Locale receiverLocale) {
        bodyFileOption.receiverLocale(receiverLocale);
    }

    // ===================================================================================
    //                                                                       Postie Option
    //                                                                       =============
    public void async() {
        postcard.async();
    }

    public void retry(int retryCount, long intervalMillis) {
        postcard.retry(retryCount, intervalMillis);
    }

    public void suppressSendFailure() {
        postcard.suppressSendFailure();
    }

    // ===================================================================================
    //                                                                   Template Variable
    //                                                                   =================
    protected void registerVariable(String key, Object value) {
        assertArgumentNotNull("key", key);
        assertArgumentNotNull("value", value);
        variableMap.put(key, value);
    }

    // ===================================================================================
    //                                                                      Pushed Logging
    //                                                                      ==============
    public void pushLogging(String key, Object value) {
        assertArgumentNotNull("key", key);
        assertArgumentNotNull("value", value);
        postcard.pushLogging(key, value);
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
    protected void assertArgumentNotEmpty(String variableName, String value) {
        assertArgumentNotNull(variableName, value);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be empty.");
        }
    }

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
