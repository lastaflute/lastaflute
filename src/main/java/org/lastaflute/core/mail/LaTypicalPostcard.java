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
package org.lastaflute.core.mail;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.dbflute.mail.DeliveryCategory;
import org.dbflute.mail.Postcard;
import org.dbflute.mail.Postcard.BodyFileOption;
import org.dbflute.mail.send.SMailAddress;
import org.dbflute.mail.send.supplement.SMailPostingDiscloser;
import org.dbflute.optional.OptionalThing;
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
        final Postcard nativePostcard = newNativePostcard();
        nativePostcard.writeMessageTheme(getClass()); // as default
        return nativePostcard;
    }

    protected Postcard newNativePostcard() {
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
    //                                                Locale
    //                                                ------
    @SuppressWarnings("unchecked")
    public <SELF extends LaTypicalPostcard> SELF asReceiverLocale(Locale receiverLocale) {
        postcard.asReceiverLocale(receiverLocale);
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
    protected SMailAddress createAddress(String address, String personal) {
        return new SMailAddress(address, personal);
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

    // ===================================================================================
    //                                                                       Postie Option
    //                                                                       =============
    /**
     * Set up as asynchronous execution. <br>
     * Logging and sending are executed in asynchronous process.
     */
    public void async() {
        postcard.async();
    }

    /**
     * Retry sending if failure.
     * @param retryCount The count of retry. (NotZero, NotMinus)
     * @param intervalMillis The milliseconds to wait for retrying. (NotZero, NotMinus)
     */
    public void retry(int retryCount, long intervalMillis) {
        postcard.retry(retryCount, intervalMillis);
    }

    /**
     * Suppress sending failure. (logging only) <br>
     */
    public void suppressSendFailure() {
        postcard.suppressSendFailure();
    }

    /**
     * Set subject and plain body as forcedly-direct. <br>
     * It ignores body file settings. (conversely can keep them)
     * @param subject The subject to be sent plainly. (NotNull)
     * @param plainBody The plain body to be sent plainly. (NotNull)
     */
    public void forcedlyDirect(String subject, String plainBody) {
        postcard.useDirectBody(plainBody).useWholeFixedText().forcedlyDirect(subject);
    }

    // ===================================================================================
    //                                                                   Template Variable
    //                                                                   =================
    /**
     * @param key The key of variable. (NotNull)
     * @param value The value of variable. (NotNull, EmptyAllowed: you can put empty string)
     */
    protected void registerVariable(String key, Object value) {
        assertArgumentNotNull("key", key);
        assertArgumentNotNull("value", value);
        variableMap.put(key, value);
    }

    // ===================================================================================
    //                                                                      Pushed Logging
    //                                                                      ==============
    /**
     * Push element of mail logging.
     * @param key The key of the element. (NotNull)
     * @param value The value of the element. (NotNull)
     */
    public void pushLogging(String key, Object value) {
        assertArgumentNotNull("key", key);
        assertArgumentNotNull("value", value);
        postcard.pushLogging(key, value);
    }

    // ===================================================================================
    //                                                                              Option
    //                                                                              ======
    /**
     * Suppress strict check of address format.
     * @return this. (NotNull)
     */
    public LaTypicalPostcard suppressStrictAddress() {
        strictAddress = false;
        return this;
    }

    /**
     * Set up as dry-run. <br>
     * Not send it actually, only preparation, e.g. for preview. <br>
     * You can get complete plain or HTML text from postcard.
     */
    public void dryrun() {
        postcard.dryrun();
    }

    /**
     * Write message author on this postcard. (used in logging)
     * @param messageAuthor The instance that expresses the message author. (NotNull)
     */
    public void writeAuthor(Object messageAuthor) {
        assertArgumentNotNull("messageAuthor", messageAuthor);
        postcard.writeMessageAuthor(messageAuthor);
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
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

    /**
     * @return The discloser of posting state. (NotNull, EmptyAllowed: before postie process)
     */
    public OptionalThing<SMailPostingDiscloser> getPostingDiscloser() { // e.g. for preview
        return postcard.getOfficePostingDiscloser(); // always exists after postie process
    }

    /**
     * @return The read-only map of variable for template. (NotNull)
     */
    public Map<String, Object> getVariableMap() {
        return Collections.unmodifiableMap(variableMap);
    }

    public boolean isStrictAddress() {
        return strictAddress;
    }
}
