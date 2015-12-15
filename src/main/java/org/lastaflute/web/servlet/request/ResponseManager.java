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
package org.lastaflute.web.servlet.request;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The manager of response. (response facade)
 * @author jflute
 */
public interface ResponseManager {

    // ===================================================================================
    //                                                                      Basic Handling
    //                                                                      ==============
    /**
     * Get the current response.
     * @return The response object of HTTP Servlet. (NotNull: if not Web application, throws exception)
     * @throws IllegalStateException When the response is not found.
     */
    HttpServletResponse getResponse();

    /**
     * Is the current response committed?
     * @return The determination, true or false.
     */
    boolean isCommitted();

    // ===================================================================================
    //                                                                    Routing Response
    //                                                                    ================
    /**
     * Redirect to the path.
     * @param redirectable The redirect-able object that can provides e.g. redirect path. (NotNull)
     * @throws IOException When the IO failed.
     */
    void redirect(Redirectable redirectable) throws IOException;

    /**
     * Set status as MOVED_PERMANENTLY and add the URL to location.
     * @param redirectable The redirect-able object that can provides e.g. redirect path. (NotNull)
     */
    void movedPermanently(Redirectable redirectable);

    /**
     * Set status as MOVED_PERMANENTLY and add the URL to location.
     * @param redirectable The redirect-able object that can provides e.g. redirect path. (NotNull)
     * @param host The host name for SSL URL, e.g. dockside.org. (NotNull)
     */
    void movedPermanentlySsl(Redirectable redirectable, String host);

    /**
     * Forward to the path.
     * @param forwardable The forward-able object that can provide e.g. forward path without context path. (NotNull)
     * @throws ServletException When the servlet failed.
     * @throws IOException When the IO failed.
     */
    void forward(Forwardable forwardable) throws ServletException, IOException;

    // ===================================================================================
    //                                                                      Write Response
    //                                                                      ==============
    /**
     * @param text The written text to the response. (NotNull)
     * @param contentType The content type of the response. (NotNull)
     */
    void write(String text, String contentType);

    /**
     * @param text The written text to the response. (NotNull)
     * @param contentType The content type of the response. (NotNull)
     * @param encoding The encoding for the response. (NotNull)
     */
    void write(String text, String contentType, String encoding);

    /**
     * @param json The written JSON string to the response. (NotNull)
     */
    void writeAsJson(String json);

    /**
     * @param script The written script string to the response. (NotNull)
     */
    void writeAsJavaScript(String script);

    /**
     * @param xmlStr The written XML string to the response. (NotNull)
     * @param encoding The encoding for the response. (NotNull)
     */
    void writeAsXml(String xmlStr, String encoding);

    // ===================================================================================
    //                                                                   Download Response
    //                                                                   =================
    /**
     * @param fileName The file name as 'filename' used in the header. (NotNull)
     * @param data The download data as bytes. (NotNull)
     */
    void download(String fileName, byte[] data);

    /**
     * <pre>
     * responseManager.<span style="color: #CC4747">download</span>("sea.txt", <span style="color: #553000">out</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">try</span> (InputStream <span style="color: #553000">ins</span> = ...) {
     *         <span style="color: #553000">out</span>.write(<span style="color: #553000">ins</span>);
     *     }
     * });
     * </pre>
     * @param fileName The file name as 'filename' used in the header. (NotNull)
     * @param writtenStreamLambda The callback for writing stream of download data. (NotNull)
     */
    void download(String fileName, WritternStreamCall writtenStreamLambda);

    /**
     * <pre>
     * responseManager.<span style="color: #CC4747">download</span>("sea.txt", <span style="color: #553000">out</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">try</span> (InputStream <span style="color: #553000">ins</span> = ...) {
     *         <span style="color: #553000">out</span>.write(<span style="color: #553000">ins</span>);
     *     }
     * }, <span style="color: #553000">contentLength</span>);
     * </pre>
     * @param fileName The file name as 'filename' used in the header. (NotNull)
     * @param writtenStreamLambda The callback for writing stream of download data. (NotNull)
     * @param contentLength The content length of the response.
     */
    void download(String fileName, WritternStreamCall writtenStreamLambda, int contentLength);

    /**
     * @param resource The resource to download, contains file name and content type and so on... (NotNull)
     */
    void download(ResponseDownloadResource resource);

    // ===================================================================================
    //                                                                     Header Handling
    //                                                                     ===============
    /**
     * @param name The name of the header. (NotNull)
     * @param value The value of the header. (NotNull)
     */
    void addHeader(String name, String value);

    /**
     * Add no-cache to response by e.g. Pragma, Cache-Control, Expires.
     */
    void addNoCache();

    /**
     * Set status as MOVED_PERMANENTLY and add the URL to location.
     * @param url The redirect URL for location of header. (NotNull)
     */
    void setLocationPermanently(String url);

    /**
     * Set original status. <br>
     * if status set once, you can't other status.
     * @param sc HTTP response code. use HttpServletResponse.SC_...
     */
    void setResponseStatus(int sc);
}
