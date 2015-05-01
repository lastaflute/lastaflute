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
package org.dbflute.lastaflute.web.response;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.dbflute.lastaflute.web.servlet.request.ResponseDownloadResource;

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
    protected static final StreamResponse INSTANCE_OF_EMPTY = new StreamResponse(DUMMY).asEmptyResponse();
    protected static final StreamResponse INSTANCE_OF_SKIP = new StreamResponse(DUMMY).asSkipResponse();

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String fileName;
    protected String contentType;
    protected final Map<String, String> headerMap = createHeaderMap(); // no lazy because of frequently used
    protected Integer httpStatus;

    protected byte[] byteData;
    protected InputStream inputStream;
    protected Integer contentLength;
    protected boolean emptyResponse;
    protected boolean skipResponse;

    protected Map<String, String> createHeaderMap() {
        return new LinkedHashMap<String, String>();
    }

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public StreamResponse(String fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException("The argument 'fileName' should not be null.");
        }
        this.fileName = fileName;
    }

    // ===================================================================================
    //                                                                        Content Type
    //                                                                        ============
    public StreamResponse contentType(String contentType) {
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
    public StreamResponse header(String name, String value) {
        if (name == null) {
            throw new IllegalArgumentException("The argument 'name' should not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("The argument 'value' should not be null.");
        }
        headerMap.put(name, value);
        return this;
    }

    @Override
    public Map<String, String> getHeaderMap() {
        return Collections.unmodifiableMap(headerMap);
    }

    public void headerContentDispositionAttachment() { // used as default
        headerMap.put("Content-disposition", "attachment; filename=\"" + fileName + "\"");
    }

    // ===================================================================================
    //                                                                         HTTP Status
    //                                                                         ===========
    public StreamResponse httpStatus(int httpStatus) {
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
        if (data == null) {
            throw new IllegalArgumentException("The argument 'data' should not be null.");
        }
        if (inputStream != null) {
            throw new IllegalStateException("The input stream already exists.");
        }
        this.byteData = data;
        return this;
    }

    public StreamResponse stream(InputStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("The argument 'stream' should not be null.");
        }
        if (byteData != null) {
            throw new IllegalStateException("The byte data already exists.");
        }
        this.inputStream = stream;
        return this;
    }

    public StreamResponse stream(InputStream stream, Integer contentLength) {
        if (stream == null) {
            throw new IllegalArgumentException("The argument 'stream' should not be null.");
        }
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
    public static StreamResponse empty() { // user interface
        return INSTANCE_OF_EMPTY;
    }

    protected StreamResponse asEmptyResponse() { // internal use
        emptyResponse = true;
        return this;
    }

    public static StreamResponse skip() { // user interface
        return INSTANCE_OF_SKIP;
    }

    protected StreamResponse asSkipResponse() { // internal use
        skipResponse = true;
        return this;
    }

    // ===================================================================================
    //                                                                 Convert to Resource
    //                                                                 ===================
    public ResponseDownloadResource toDownloadResource() {
        final ResponseDownloadResource resource = new ResponseDownloadResource(fileName);
        resource.contentType(contentType);
        for (Entry<String, String> entry : headerMap.entrySet()) {
            resource.header(entry.getKey(), entry.getValue());
        }
        if (byteData != null) {
            resource.data(byteData);
        }
        if (inputStream != null) {
            resource.stream(inputStream, contentLength);
        }
        return resource;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return "{" + fileName + ", " + contentType + ", " + headerMap + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isSkip() {
        return skipResponse;
    }
}
