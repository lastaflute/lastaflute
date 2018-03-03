/*
 * Copyright 2015-2018 the original author or authors.
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
package org.lastaflute.web.response;

import java.util.Collections;
import java.util.Map;

import org.dbflute.helper.StringKeyMap;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.web.aspect.RomanticActionCustomizer;

/**
 * @author jflute
 */
public class XmlResponse implements ApiResponse {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final String ENCODING_UTF8 = "UTF-8";
    protected static final String ENCODING_WINDOWS_31J = "Windows-31J";
    protected static final String DEFAULT_ENCODING = ENCODING_UTF8;
    protected static final String DUMMY = "dummy";
    protected static final XmlResponse INSTANCE_OF_UNDEFINED = new XmlResponse(DUMMY).ofUndefined();

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String xmlStr;
    protected Map<String, String[]> headerMap; // lazy loaded (for when no use)
    protected Integer httpStatus;
    protected String encoding = DEFAULT_ENCODING;
    protected boolean undefined;
    protected boolean returnAsEmptyBody;
    protected ResponseHook afterTxCommitHook;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    /**
     * Construct XML response. (default encoding is UTF-8, you can change it) <br>
     * This needs {@link RomanticActionCustomizer} in your customizer.dicon.
     * <pre>
     * <span style="color: #3F7E5E">// e.g. XML string response (UTF-8)</span>
     * return new XmlResponse(xmlStr);
     * 
     * <span style="color: #3F7E5E">// e.g. XML string response (Windows-31J)</span>
     * return new XmlResponse(xmlStr).encodeAsWindows31J();
     * </pre>
     * @param xmlStr The string of XML to send response. (NotNull)
     */
    public XmlResponse(String xmlStr) {
        assertArgumentNotNull("xmlStr", xmlStr);
        this.xmlStr = xmlStr;
    }

    // ===================================================================================
    //                                                                              Header
    //                                                                              ======
    @Override
    public XmlResponse header(String name, String... values) {
        assertArgumentNotNull("name", name);
        assertArgumentNotNull("values", values);
        assertDefinedState("header");
        final Map<String, String[]> headerMap = prepareHeaderMap();
        if (headerMap.containsKey(name)) {
            throw new IllegalStateException("Already exists the header: name=" + name + " existing=" + headerMap);
        }
        headerMap.put(name, values);
        return this;
    }

    @Override
    public Map<String, String[]> getHeaderMap() {
        return headerMap != null ? Collections.unmodifiableMap(headerMap) : DfCollectionUtil.emptyMap();
    }

    protected Map<String, String[]> prepareHeaderMap() {
        if (headerMap == null) {
            headerMap = StringKeyMap.createAsCaseInsensitiveOrdered();
        }
        return headerMap;
    }

    // ===================================================================================
    //                                                                         HTTP Status
    //                                                                         ===========
    @Override
    public XmlResponse httpStatus(int httpStatus) {
        assertDefinedState("httpStatus");
        this.httpStatus = httpStatus;
        return this;
    }

    @Override
    public OptionalThing<Integer> getHttpStatus() {
        return OptionalThing.ofNullable(httpStatus, () -> {
            throw new IllegalStateException("Not found the http status in the response: " + XmlResponse.this.toString());
        });
    }

    // ===================================================================================
    //                                                                              Option
    //                                                                              ======
    // -----------------------------------------------------
    //                                              Encoding
    //                                              --------
    public XmlResponse encodeAsUTF8() {
        encoding = ENCODING_UTF8;
        return this;
    }

    public XmlResponse encodeAsWindows31J() {
        encoding = ENCODING_WINDOWS_31J;
        return this;
    }

    // -----------------------------------------------------
    //                                            Empty Body
    //                                            ----------
    public static XmlResponse asEmptyBody() { // user interface
        return new XmlResponse(DUMMY).ofEmptyBody();
    }

    protected XmlResponse ofEmptyBody() { // internal use
        returnAsEmptyBody = true;
        return this;
    }

    // -----------------------------------------------------
    //                                     Undefined Control
    //                                     -----------------
    public static XmlResponse undefined() { // user interface
        return INSTANCE_OF_UNDEFINED;
    }

    protected XmlResponse ofUndefined() { // internal use
        undefined = true;
        return this;
    }

    // -----------------------------------------------------
    //                                         Response Hook
    //                                         -------------
    public XmlResponse afterTxCommit(ResponseHook noArgLambda) {
        assertArgumentNotNull("noArgLambda", noArgLambda);
        afterTxCommitHook = noArgLambda;
        return this;
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected void assertArgumentNotNull(String title, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + title + "' should not be null.");
        }
    }

    protected void assertDefinedState(String methodName) {
        if (undefined) {
            throw new IllegalStateException("undefined response: method=" + methodName + "() this=" + toString());
        }
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final String classTitle = DfTypeUtil.toClassTitle(this);
        final String emptyExp = returnAsEmptyBody ? ", emptyBody" : "";
        final String undefinedExp = undefined ? ", undefined" : "";
        return classTitle + ":{" + encoding + emptyExp + undefinedExp + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getXmlStr() {
        return xmlStr;
    }

    public String getEncoding() {
        return encoding;
    }

    @Override
    public boolean isReturnAsEmptyBody() {
        return returnAsEmptyBody;
    }

    @Override
    public boolean isUndefined() {
        return undefined;
    }

    // -----------------------------------------------------
    //                                         Response Hook
    //                                         -------------
    public OptionalThing<ResponseHook> getAfterTxCommitHook() {
        return OptionalThing.ofNullable(afterTxCommitHook, () -> {
            String msg = "Not found the response hook: " + XmlResponse.this.toString();
            throw new IllegalStateException(msg);
        });
    }
}
