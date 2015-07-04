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
package org.lastaflute.web.response;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
    protected static final XmlResponse INSTANCE_OF_EMPTY = new XmlResponse(DUMMY).asEmpty();
    protected static final XmlResponse INSTANCE_OF_SKIP = new XmlResponse(DUMMY).asSkip();

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String xmlStr;
    protected Map<String, String[]> headerMap; // lazy loaded (for when no use)
    protected Integer httpStatus;
    protected String encoding = DEFAULT_ENCODING;
    protected boolean empty;
    protected boolean skip;

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
            headerMap = new LinkedHashMap<String, String[]>(4);
        }
        return headerMap;
    }

    // ===================================================================================
    //                                                                         HTTP Status
    //                                                                         ===========
    @Override
    public XmlResponse httpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
        return this;
    }

    @Override
    public Integer getHttpStatus() {
        return httpStatus;
    }

    // ===================================================================================
    //                                                                              Option
    //                                                                              ======
    public XmlResponse encodeAsUTF8() {
        encoding = DEFAULT_ENCODING;
        return this;
    }

    public XmlResponse encodeAsWindows31J() {
        encoding = ENCODING_WINDOWS_31J;
        return this;
    }

    public static XmlResponse empty() { // user interface
        return INSTANCE_OF_EMPTY;
    }

    protected XmlResponse asEmpty() { // internal use
        empty = true;
        return this;
    }

    public static XmlResponse skip() { // user interface
        return INSTANCE_OF_SKIP;
    }

    protected XmlResponse asSkip() { // internal use
        skip = true;
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

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final String classTitle = DfTypeUtil.toClassTitle(this);
        final String emptyExp = empty ? ", empty" : "";
        final String skipExp = skip ? ", skip" : "";
        return classTitle + ":{" + encoding + emptyExp + skipExp + "}";
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
    public boolean isEmpty() {
        return empty;
    }

    @Override
    public boolean isSkip() {
        return skip;
    }
}
