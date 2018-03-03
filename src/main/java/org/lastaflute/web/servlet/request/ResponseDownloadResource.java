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
package org.lastaflute.web.servlet.request;

import java.util.Map;

import org.dbflute.helper.StringKeyMap;
import org.lastaflute.web.servlet.request.stream.WrittenStreamCall;
import org.lastaflute.web.servlet.request.stream.WritternZipStreamCall;

/**
 * @author jflute
 */
public class ResponseDownloadResource {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String fileName;
    protected String contentType;
    protected final Map<String, String[]> headerMap = createHeaderMap(); // no lazy because of frequently used
    protected byte[] byteData;
    protected WrittenStreamCall streamCall;
    protected WritternZipStreamCall zipStreamCall;
    protected Integer contentLength;
    protected boolean returnAsEmptyBody;

    protected Map<String, String[]> createHeaderMap() {
        return StringKeyMap.createAsCaseInsensitiveOrdered();
    }

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ResponseDownloadResource(String fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException("The argument 'fileName' should not be null.");
        }
        this.fileName = fileName;
    }

    // ===================================================================================
    //                                                                        Content Type
    //                                                                        ============
    public ResponseDownloadResource contentType(String contentType) {
        assertArgumentNotNull("contentType", contentType);
        this.contentType = contentType;
        return this;
    }

    public ResponseDownloadResource contentTypeOctetStream() { // used as default
        contentType = "application/octet-stream";
        return this;
    }

    public ResponseDownloadResource contentTypeJpeg() {
        contentType = "image/jpeg";
        return this;
    }

    public boolean hasContentType() {
        return contentType != null;
    }

    // ===================================================================================
    //                                                                              Header
    //                                                                              ======
    public void header(String name, String[] values) {
        assertArgumentNotNull("name", name);
        assertArgumentNotNull("values", values);
        headerMap.put(name, values);
    }

    public void headerContentDispositionAttachment() { // used as default
        headerContentDisposition("attachment; filename=\"" + fileName + "\"");
    }

    public void headerContentDispositionInline() {
        headerContentDisposition("inline; filename=\"" + fileName + "\"");
    }

    protected void headerContentDisposition(String disposition) {
        headerMap.put(ResponseManager.HEADER_CONTENT_DISPOSITION, new String[] { disposition });
    }

    public boolean hasContentDisposition() {
        return headerMap.containsKey(ResponseManager.HEADER_CONTENT_DISPOSITION);
    }

    // ===================================================================================
    //                                                                       Download Data
    //                                                                       =============
    public ResponseDownloadResource data(byte[] data) {
        doData(data);
        return this;
    }

    protected void doData(byte[] data) {
        assertArgumentNotNull("data", data);
        if (streamCall != null) {
            throw new IllegalStateException("The streamCall already exists: " + streamCall);
        }
        if (zipStreamCall != null) {
            throw new IllegalStateException("The zipStreamCall already exists: " + zipStreamCall);
        }
        this.byteData = data;
    }

    public ResponseDownloadResource stream(WrittenStreamCall streamCall) {
        doStream(streamCall);
        return this;
    }

    public ResponseDownloadResource stream(WrittenStreamCall streamCall, int contentLength) {
        doStream(streamCall);
        this.contentLength = contentLength;
        return this;
    }

    protected void doStream(WrittenStreamCall streamCall) {
        assertArgumentNotNull("streamCall", streamCall);
        if (byteData != null) {
            throw new IllegalStateException("The byte data already exists: " + byteData);
        }
        if (zipStreamCall != null) {
            throw new IllegalStateException("The zipStreamCall already exists: " + zipStreamCall);
        }
        this.streamCall = streamCall;
    }

    public ResponseDownloadResource zipStreamChunked(WritternZipStreamCall zipStreamCall) {
        doZipStreamChunked(zipStreamCall);
        return this;
    }

    protected void doZipStreamChunked(WritternZipStreamCall zipStreamCall) {
        assertArgumentNotNull("zipStreamCall", zipStreamCall);
        if (byteData != null) {
            throw new IllegalStateException("The byte data already exists: " + byteData);
        }
        if (streamCall != null) {
            throw new IllegalStateException("The streamCall already exists: " + streamCall);
        }
        this.zipStreamCall = zipStreamCall;
    }

    public boolean hasByteData() {
        return byteData != null;
    }

    public boolean hasStreamCall() {
        return streamCall != null;
    }

    public boolean hasZipStreamCall() {
        return zipStreamCall != null;
    }

    // ===================================================================================
    //                                                                          Empty Body
    //                                                                          ==========
    public void asEmptyBody() {
        returnAsEmptyBody = true;
    }

    public boolean isReturnAsEmptyBody() {
        return returnAsEmptyBody;
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
        return "download:{" + fileName + ", " + contentType + ", " + headerMap + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public Map<String, String[]> getHeaderMap() {
        return headerMap;
    }

    public byte[] getByteData() {
        return byteData;
    }

    public WrittenStreamCall getStreamCall() {
        return streamCall;
    }

    public WritternZipStreamCall getZipStreamCall() {
        return zipStreamCall;
    }

    public Integer getContentLength() {
        return contentLength;
    }
}
