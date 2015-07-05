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

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.dbflute.util.DfTypeUtil;
import org.lastaflute.web.servlet.request.ResponseDownloadResource;

/**
 * The response of stream for action.
 * <pre>
 * e.g. simple (content-type is octet-stream or found by extension mapping)
 *  return new StreamResponse("classificationDefinitionMap.dfprop").stream(ins);
 * 
 * e.g. specify content-type
 *  return new StreamResponse("jflute.jpg").contentTypeJpeg().stream(ins);
 * </pre>
 * @author jflute
 */
public class StreamResponse implements ActionResponse {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final String DUMMY = "dummy";
    protected static final StreamResponse INSTANCE_OF_UNDEFINED = new StreamResponse(DUMMY).ofUndefined();

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String fileName;
    protected String contentType;
    protected final Map<String, String[]> headerMap = createHeaderMap(); // no lazy because of frequently used
    protected Integer httpStatus;

    protected byte[] byteData;
    protected InputStream inputStream;
    protected Integer contentLength;
    protected boolean undefined;
    protected boolean returnAsEmptyBody;

    protected Map<String, String[]> createHeaderMap() {
        return new LinkedHashMap<String, String[]>();
    }

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public StreamResponse(String fileName) {
        assertArgumentNotNull("fileName", fileName);
        this.fileName = fileName;
    }

    // ===================================================================================
    //                                                                        Content Type
    //                                                                        ============
    public StreamResponse contentType(String contentType) {
        assertArgumentNotNull("contentType", contentType);
        this.contentType = contentType;
        return this;
    }

    public StreamResponse contentTypeOctetStream() { // used as default
        contentType = "application/octet-stream";
        return this;
    }

    public StreamResponse contentTypeJpeg() {
        contentType = "image/jpeg";
        return this;
    }

    public String getContentType() {
        return contentType;
    }

    // ===================================================================================
    //                                                                              Header
    //                                                                              ======
    @Override
    public StreamResponse header(String name, String... values) {
        assertArgumentNotNull("name", name);
        assertArgumentNotNull("values", values);
        assertDefinedState("header");
        if (headerMap.containsKey(name)) {
            throw new IllegalStateException("Already exists the header: name=" + name + " existing=" + headerMap);
        }
        headerMap.put(name, values);
        return this;
    }

    @Override
    public Map<String, String[]> getHeaderMap() {
        return Collections.unmodifiableMap(headerMap);
    }

    public void headerContentDispositionAttachment() { // used as default
        headerMap.put("Content-disposition", new String[] { "attachment; filename=\"" + fileName + "\"" });
    }

    // ===================================================================================
    //                                                                         HTTP Status
    //                                                                         ===========
    public StreamResponse httpStatus(int httpStatus) {
        assertDefinedState("httpStatus");
        this.httpStatus = httpStatus;
        return this;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    // ===================================================================================
    //                                                                       Download Data
    //                                                                       =============
    public StreamResponse data(byte[] data) {
        assertArgumentNotNull("data", data);
        assertDefinedState("data");
        if (inputStream != null) {
            throw new IllegalStateException("The input stream already exists.");
        }
        this.byteData = data;
        return this;
    }

    public StreamResponse stream(InputStream stream) {
        assertArgumentNotNull("stream", stream);
        assertDefinedState("stream");
        if (byteData != null) {
            throw new IllegalStateException("The byte data already exists.");
        }
        this.inputStream = stream;
        return this;
    }

    public StreamResponse stream(InputStream stream, Integer contentLength) {
        assertArgumentNotNull("stream", stream);
        assertArgumentNotNull("contentLength", contentLength);
        assertDefinedState("stream");
        if (byteData != null) {
            throw new IllegalStateException("The byte data already exists.");
        }
        this.inputStream = stream;
        this.contentLength = contentLength;
        return this;
    }

    public byte[] getByteData() {
        return byteData;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public Integer getContentLength() {
        return contentLength;
    }

    // ===================================================================================
    //                                                                              Option
    //                                                                              ======
    // -----------------------------------------------------
    //                                            Empty Body
    //                                            ----------
    public static StreamResponse asEmptyBody() { // user interface
        return new StreamResponse(DUMMY).ofEmptyBody();
    }

    protected StreamResponse ofEmptyBody() { // internal use
        returnAsEmptyBody = true;
        return this;
    }

    // -----------------------------------------------------
    //                                     Undefined Control
    //                                     -----------------
    public static StreamResponse undefined() { // user interface
        return INSTANCE_OF_UNDEFINED;
    }

    protected StreamResponse ofUndefined() { // internal use
        undefined = true;
        return this;
    }

    // ===================================================================================
    //                                                                 Convert to Resource
    //                                                                 ===================
    public ResponseDownloadResource toDownloadResource() {
        final ResponseDownloadResource resource = createResponseDownloadResource();
        resource.contentType(contentType);
        for (Entry<String, String[]> entry : headerMap.entrySet()) {
            resource.header(entry.getKey(), entry.getValue());
        }
        if (byteData != null) {
            resource.data(byteData);
        }
        if (inputStream != null) {
            resource.stream(inputStream, contentLength);
        }
        if (returnAsEmptyBody) {
            resource.asEmptyBody();
        }
        return resource;
    }

    protected ResponseDownloadResource createResponseDownloadResource() {
        return new ResponseDownloadResource(fileName);
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
        return classTitle + ":{" + fileName + ", " + contentType + ", " + headerMap + emptyExp + undefinedExp + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    @Override
    public boolean isReturnAsEmptyBody() {
        return returnAsEmptyBody;
    }

    @Override
    public boolean isUndefined() {
        return undefined;
    }
}
