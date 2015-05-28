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
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.lastaflute.web.response.HtmlResponse;

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
     * Forward to the path.
     * @param forwardPath The path of forward without context path, will be adjusted in this method. (NotNull)
     * @throws ServletException When the servlet failed.
     * @throws IOException When the IO failed.
     */
    void forward(String forwardPath) throws ServletException, IOException;

    /**
     * Redirect to the path. (adjusted for context path)
     * @param redirectPath The path of redirect without context path, will be adjusted in this method. (NotNull)
     * @throws IOException When the IO failed.
     */
    void redirect(String redirectPath) throws IOException;

    /**
     * Redirect to the path as-is. (no adjustment for context path)
     * @param redirectPath The path of redirect without context path, plainly used in this method. (NotNull)
     * @throws IOException When the IO failed.
     */
    void redirectAsIs(String redirectPath) throws IOException;

    /**
     * Set status as MOVED_PERMANENTLY and add the URL to location.
     * @param response The HTML response for redirect. (NotNull)
     * @param url The redirect URL for location of header. (NotNull)
     * @return The result response, basically empty because of response already committed. (NotNull)
     */
    HtmlResponse movedPermanently(HtmlResponse response);

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
     * @param fileName The file name as 'filename' used in the header. (NotNull)
     * @param ins The download data as stream. (NotNull)
     */
    void download(String fileName, InputStream ins);

    /**
     * @param fileName The file name as 'filename' used in the header. (NotNull)
     * @param ins The download data as stream. (NotNull)
     * @param length The content length of the response.
     */
    void download(String fileName, InputStream ins, int length);

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
