/*
 * Copyright 2015-2021 the original author or authors.
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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.web.servlet.request.stream.WrittenStreamCall;

/**
 * The manager of response. (response facade)
 * @author jflute
 */
public interface ResponseManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    String HEADER_PRAGMA = "Pragma";
    String HEADER_CACHE_CONTROL = "Cache-Control";
    String HEADER_EXPIRES = "Expires";

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
     * Download the byte data. <br>
     * Default Content-type is 'application/octet-stream', Content-disposition is 'attachment'.
     * @param fileName The file name as 'filename' used in the header. (NotNull)
     * @param data The download data as bytes. (NotNull)
     */
    void download(String fileName, byte[] data);

    /**
     * Download by the stream callback. <br>
     * Default Content-type is 'application/octet-stream', Content-disposition is 'attachment'.
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
    void download(String fileName, WrittenStreamCall writtenStreamLambda);

    /**
     * Download by the stream callback with content length. <br>
     * Default Content-type is 'application/octet-stream', Content-disposition is 'attachment'.
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
    void download(String fileName, WrittenStreamCall writtenStreamLambda, int contentLength);

    /**
     * Download the resource by flexible settings. <br>
     * Default Content-type is 'application/octet-stream', Content-disposition is 'attachment'.
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

    /**
     * Get header value. (case insensitive)
     * @param headerKey The key of the header. (NotNull)
     * @return The optional value of specified header as string. (NotNull, EmptyAllowed)
     */
    OptionalThing<String> getHeader(String headerKey);

    /**
     * Get header values as list. (case insensitive)
     * @param headerKey The key of the header. (NotNull)
     * @return The read-only list of header value. (NotNull, EmptyAllowed)
     */
    List<String> getHeaderAsList(String headerKey);

    // ===================================================================================
    //                                                                        Client Error
    //                                                                        ============
    // -----------------------------------------------------
    //                                       400 Bad Request
    //                                       ---------------
    /**
     * Create exception of 400 Bad Request.
     * <pre>
     * <span style="color: #70226C">if</span> (...) {
     *     <span style="color: #70226C">throw</span> <span style="color: #0000C0">responseManager</span>.<span style="color: #CC4747">new400</span>("Bad product: " + productId);
     * }
     * 
     * ...
     * }).orElseThrow(() <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">return</span> <span style="color: #0000C0">responseManager</span>.<span style="color: #CC4747">new400</span>("Bad product: " + productId);
     * });
     * </pre>
     * @param debugMsg The debug message for developer (not user message). (NotNull)
     * @return The new-created exception of 400. (NotNull)
     */
    RuntimeException new400(String debugMsg);

    /**
     * Create exception of 400 Bad Request with options (e.g. messages, nested exception).
     * <pre>
     * <span style="color: #70226C">if</span> (...) {
     *     <span style="color: #70226C">throw</span> <span style="color: #0000C0">responseManager</span>.<span style="color: #CC4747">new400</span>("...", op <span style="font-size: 120%">-</span>&gt;</span> op.messages(createMessages().add...));
     * }
     * 
     * ...
     * }).orElseTranslatingThrow(cause <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">return</span> <span style="color: #0000C0">responseManager</span>.<span style="color: #CC4747">new400</span>("...", op <span style="font-size: 120%">-</span>&gt;</span> op.cause(cause));
     * });
     * </pre>
     * @param debugMsg The debug message for developer (not user message). (NotNull)
     * @param opLambda The callback for option of client error. (NotNull)
     * @return The new-created exception of 400. (NotNull)
     */
    RuntimeException new400(String debugMsg, ForcedClientErrorOpCall opLambda);

    // -----------------------------------------------------
    //                                         403 Forbidden
    //                                         -------------
    /**
     * Create exception of 403 Forbidden.
     * <pre>
     * <span style="color: #70226C">if</span> (...) {
     *     <span style="color: #70226C">throw</span> <span style="color: #0000C0">responseManager</span>.<span style="color: #CC4747">new403</span>("Cannot access the product: " + productId);
     * }
     * 
     * ...
     * }).orElseThrow(() <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">return</span> <span style="color: #0000C0">responseManager</span>.<span style="color: #CC4747">new403</span>("Cannot access the product: " + productId);
     * });
     * </pre>
     * @param debugMsg The debug message for developer (not user message). (NotNull)
     * @return The new-created exception of 403. (NotNull)
     */
    RuntimeException new403(String debugMsg);

    /**
     * Create exception of 403 Forbidden with options (e.g. messages, nested exception).
     * <pre>
     * <span style="color: #70226C">if</span> (...) {
     *     <span style="color: #70226C">throw</span> <span style="color: #0000C0">responseManager</span>.<span style="color: #CC4747">new403</span>("...", op <span style="font-size: 120%">-</span>&gt;</span> op.messages(createMessages().add...));
     * }
     * 
     * ...
     * }).orElseTranslatingThrow(cause <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">return</span> <span style="color: #0000C0">responseManager</span>.<span style="color: #CC4747">new403</span>("...", op <span style="font-size: 120%">-</span>&gt;</span> op.cause(cause));
     * });
     * </pre>
     * @param debugMsg The debug message for developer (not user message). (NotNull)
     * @param opLambda The callback for option of client error. (NotNull)
     * @return The new-created exception of 403. (NotNull)
     */
    RuntimeException new403(String debugMsg, ForcedClientErrorOpCall opLambda);

    // -----------------------------------------------------
    //                                         404 Not Found
    //                                         -------------
    /**
     * Create exception of 404 Not Found.
     * <pre>
     * <span style="color: #70226C">if</span> (...) {
     *     <span style="color: #70226C">throw</span> <span style="color: #0000C0">responseManager</span>.<span style="color: #CC4747">new404</span>("Not found the product: " + productId);
     * }
     * 
     * ...
     * }).orElseThrow(() <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">return</span> <span style="color: #0000C0">responseManager</span>.<span style="color: #CC4747">new404</span>("Not found the product: " + productId);
     * });
     * </pre>
     * @param debugMsg The debug message for developer (not user message). (NotNull)
     * @return The new-created exception of 404. (NotNull)
     */
    RuntimeException new404(String debugMsg);

    /**
     * Create exception of 404 Not Found with options (e.g. messages, nested exception).
     * <pre>
     * <span style="color: #70226C">if</span> (...) {
     *     <span style="color: #70226C">throw</span> <span style="color: #0000C0">responseManager</span>.<span style="color: #CC4747">new404</span>("...", op <span style="font-size: 120%">-</span>&gt;</span> op.messages(createMessages().add...));
     * }
     * 
     * ...
     * }).orElseTranslatingThrow(cause <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">return</span> <span style="color: #0000C0">responseManager</span>.<span style="color: #CC4747">new404</span>("...", op <span style="font-size: 120%">-</span>&gt;</span> op.cause(cause));
     * });
     * </pre>
     * @param debugMsg The debug message for developer (not user message). (NotNull)
     * @param opLambda The callback for option of client error. (NotNull)
     * @return The new-created exception of 404. (NotNull)
     */
    RuntimeException new404(String debugMsg, ForcedClientErrorOpCall opLambda);

    // -----------------------------------------------------
    //                                   Client Error Option
    //                                   -------------------
    @FunctionalInterface
    public interface ForcedClientErrorOpCall {

        void callback(ForcedClientErrorOption op);
    }

    public class ForcedClientErrorOption {

        protected UserMessages messages;
        protected Throwable cause;

        public ForcedClientErrorOption messages(UserMessages messages) {
            if (messages == null) {
                throw new IllegalArgumentException("The argument 'messages' should not be null.");
            }
            this.messages = messages;
            return this;
        }

        public ForcedClientErrorOption cause(Throwable cause) {
            if (cause == null) {
                throw new IllegalArgumentException("The argument 'cause' should not be null.");
            }
            this.cause = cause;
            return this;
        }

        @Override
        public String toString() {
            return "{" + messages + ", " + (cause != null ? cause.getClass() : null) + "}";
        }

        public OptionalThing<UserMessages> getMessages() {
            return OptionalThing.ofNullable(messages, () -> {
                throw new IllegalStateException("Not found the messages: " + this);
            });
        }

        public OptionalThing<Throwable> getCause() {
            return OptionalThing.ofNullable(cause, () -> {
                throw new IllegalStateException("Not found the cause: " + this);
            });
        }
    }
}
