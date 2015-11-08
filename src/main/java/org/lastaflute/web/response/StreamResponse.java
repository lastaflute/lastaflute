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
import java.util.Map.Entry;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.web.servlet.request.ResponseDownloadResource;
import org.lastaflute.web.servlet.request.WritternStreamCall;

/**
 * The response of stream for action.
 * <pre>
 * e.g. simple (content-type is octet-stream or found by extension mapping)
 *  <span style="color: #70226C">return new</span> StreamResponse("classificationDefinitionMap.dfprop").stream(ins);
 * 
 * e.g. specify content-type
 *  <span style="color: #70226C">return new</span> StreamResponse("jflute.jpg").contentTypeJpeg().stream(ins);
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
    protected WritternStreamCall streamCall;
    protected Integer contentLength;
    protected boolean undefined;
    protected boolean returnAsEmptyBody;
    protected ResponseHook afterTxCommitHook;

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
    @Override
    public StreamResponse httpStatus(int httpStatus) {
        assertDefinedState("httpStatus");
        this.httpStatus = httpStatus;
        return this;
    }

    @Override
    public OptionalThing<Integer> getHttpStatus() {
        return OptionalThing.ofNullable(httpStatus, () -> {
            throw new IllegalStateException("Not found the http status in the response: " + StreamResponse.this.toString());
        });
    }

    // ===================================================================================
    //                                                                       Download Data
    //                                                                       =============
    /**
     * @param data The download data as bytes. (NotNull)
     * @return this. (NotNull)
     */
    public StreamResponse data(byte[] data) {
        assertArgumentNotNull("data", data);
        assertDefinedState("data");
        if (streamCall != null) {
            throw new IllegalStateException("The stream call already exists.");
        }
        this.byteData = data;
        return this;
    }

    /**
     * <pre>
     * <span style="color: #70226C">return</span> asStream("sea.txt").<span style="color: #CC4747">stream</span>(<span style="color: #553000">out</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">try</span> (InputStream <span style="color: #553000">ins</span> = ...) {
     *         <span style="color: #553000">out</span>.write(<span style="color: #553000">ins</span>);
     *     }
     * });
     * </pre>
     * @param writtenStreamLambda The callback for writing stream of download data. (NotNull)
     * @return this. (NotNull)
     */
    public StreamResponse stream(WritternStreamCall writtenStreamLambda) {
        doStream(writtenStreamLambda);
        return this;
    }

    /**
     * <pre>
     * <span style="color: #70226C">return</span> asStream("sea.txt").<span style="color: #CC4747">stream</span>(<span style="color: #553000">out</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">try</span> (InputStream <span style="color: #553000">ins</span> = ...) {
     *         <span style="color: #553000">out</span>.write(<span style="color: #553000">ins</span>);
     *     }
     * }, <span style="color: #553000">contentLength</span>);
     * @param writtenStreamLambda The callback for writing stream of download data. (NotNull)
     * @param contentLength The length of the content.
     * @return this. (NotNull)
     */
    public StreamResponse stream(WritternStreamCall writtenStreamLambda, int contentLength) {
        doStream(writtenStreamLambda);
        this.contentLength = contentLength;
        return this;
    }

    protected void doStream(WritternStreamCall writtenStreamLambda) {
        assertArgumentNotNull("writtenStreamLambda", writtenStreamLambda);
        assertDefinedState("stream");
        if (byteData != null) {
            throw new IllegalStateException("The byte data already exists.");
        }
        streamCall = writtenStreamLambda;
    }

    public byte[] getByteData() {
        return byteData;
    }

    public WritternStreamCall getStreamCall() {
        return streamCall;
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

    // -----------------------------------------------------
    //                                         Response Hook
    //                                         -------------
    public StreamResponse afterTxCommit(ResponseHook noArgLambda) {
        assertArgumentNotNull("noArgLambda", noArgLambda);
        afterTxCommitHook = noArgLambda;
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
        if (streamCall != null) {
            if (contentLength != null) {
                resource.stream(streamCall, contentLength);
            } else {
                resource.stream(streamCall);
            }
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

    // -----------------------------------------------------
    //                                         Response Hook
    //                                         -------------
    public OptionalThing<ResponseHook> getAfterTxCommitHook() {
        return OptionalThing.ofNullable(afterTxCommitHook, () -> {
            String msg = "Not found the response hook: " + StreamResponse.this.toString();
            throw new IllegalStateException(msg);
        });
    }
}
