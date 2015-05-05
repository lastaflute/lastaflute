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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dbflute.util.Srl;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.di.util.LdiInputStreamUtil;
import org.lastaflute.di.util.LdiOutputStreamUtil;
import org.lastaflute.web.direction.OptionalWebDirection;
import org.lastaflute.web.path.ActionPathResolver;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.util.LaRequestUtil;
import org.lastaflute.web.util.LaResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class SimpleResponseManager implements ResponseManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(SimpleResponseManager.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant directory (AD) for framework. (NotNull: after initialization) */
    @Resource
    protected FwAssistantDirector assistantDirector;

    /** The resolver of action path. (NotNull: after initialization) */
    @Resource
    protected ActionPathResolver actionPathResolver;

    /** The map of content type for extensions. (NullAllowed) */
    protected Map<String, String> downloadExtensionContentTypeMap;

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    /**
     * Initialize this component. <br>
     * This is basically called by DI setting file.
     */
    @PostConstruct
    public void initialize() {
        final OptionalWebDirection direction = assistOptionalWebDirection();
        final ResponseHandlingProvider provider = direction.assistResponseHandlingProvider();
        if (provider != null) {
            downloadExtensionContentTypeMap = provider.provideDownloadExtensionContentTypeMap();
        }
        showBootLogging();
    }

    protected OptionalWebDirection assistOptionalWebDirection() {
        return assistantDirector.assistOptionalWebDirection();
    }

    protected void showBootLogging() {
        if (logger.isInfoEnabled()) {
            logger.info("[Response Manager]");
            logger.info(" downloadExtensionContentTypeMap: " + downloadExtensionContentTypeMap);
        }
    }

    // ===================================================================================
    //                                                                      Basic Handling
    //                                                                      ==============
    @Override
    public HttpServletResponse getResponse() {
        return LaResponseUtil.getResponse();
    }

    // ===================================================================================
    //                                                                   Redirect Response
    //                                                                   =================
    @Override
    public void redirect(String redirectPath) throws IOException {
        assertArgumentNotNull("redirectPath", redirectPath);
        doRedirect(redirectPath);
    }

    protected void doRedirect(String redirectPath) throws IOException {
        final HttpServletResponse response = getResponse();
        final String redirectUrl = buildRedirectUrl(response, redirectPath);
        response.sendRedirect(redirectUrl);
    }

    protected String buildRedirectUrl(HttpServletResponse response, String redirectPath) {
        final String redirectUrl;
        if (redirectPath.startsWith("/")) {
            redirectUrl = LaRequestUtil.getRequest().getContextPath() + redirectPath;
        } else {
            redirectUrl = redirectPath;
        }
        return response.encodeRedirectURL(redirectUrl);
    }

    @Override
    public void forward(String forwardPath) throws ServletException, IOException {
        assertArgumentNotNull("forwardPath", forwardPath);
        final HttpServletRequest request = getRequestManager().getRequest();
        final RequestDispatcher rd = request.getRequestDispatcher(forwardPath);
        if (rd == null) {
            String msg = "Not found the request dispatcher for the URI: " + forwardPath;
            throw new IllegalStateException(msg);
        }
        rd.forward(request, getResponse());
    }

    @Override
    public HtmlResponse movedPermanently(HtmlResponse response) {
        if (!response.isRedirectTo()) {
            throw new IllegalArgumentException("Not redirect response for permanently: " + response);
        }
        setLocationPermanently(actionPathResolver.removeRedirectMark(response.getRoutingPath())); // set up headers for 301
        return HtmlResponse.empty(); // because of already done about response process
    }

    // ===================================================================================
    //                                                                      Write Response
    //                                                                      ==============
    @Override
    public void write(String text, String contentType) {
        assertArgumentNotNull("text", text);
        assertArgumentNotNull("contentType", contentType);
        doWrite(text, contentType);
    }

    @Override
    public void write(String text, String contentType, String encoding) {
        assertArgumentNotNull("text", text);
        assertArgumentNotNull("contentType", contentType);
        assertArgumentNotNull("encoding", encoding);
        doWrite(text, contentType, encoding);
    }

    @Override
    public void writeAsJson(String json) {
        assertArgumentNotNull("json", json);
        final String contentType = "application/json";
        logger.debug("#flow ...Writing response as {}: \n{}", contentType, json);
        write(json, contentType);
    }

    @Override
    public void writeAsJavaScript(String script) {
        assertArgumentNotNull("script", script);
        final String contentType = "application/javascript";
        logger.debug("#flow ...Writing response as {}: \n{}", contentType, script);
        write(script, contentType);
    }

    @Override
    public void writeAsXml(String xmlStr, String encoding) {
        assertArgumentNotNull("xmlStr", xmlStr);
        assertArgumentNotNull("encoding", encoding);
        final String contentType = "text/xml";
        logger.debug("#flow ...Writing response as {}: \n", contentType, xmlStr);
        write(xmlStr, contentType, encoding);
    }

    // -----------------------------------------------------
    //                                        Actually Write
    //                                        --------------
    protected void doWrite(String text, String contentType) {
        doWrite(text, contentType, null);
    }

    protected void doWrite(String text, String contentType, String encoding) {
        if (contentType == null) {
            contentType = "text/plain";
        }
        if (encoding == null) {
            encoding = LaRequestUtil.getRequest().getCharacterEncoding();
            if (encoding == null) {
                encoding = "UTF-8";
            }
        }
        final HttpServletResponse response = getResponse();
        response.setContentType(contentType + "; charset=" + encoding);
        try {
            PrintWriter out = null;
            try {
                out = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), encoding));
                out.print(text);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        } catch (IOException e) {
            String msg = "Failed to write the text: contentType=" + contentType + ", encoding=" + encoding + ", text=" + text;
            throw new IllegalStateException(msg, e);
        }
    }

    // ===================================================================================
    //                                                                   Download Response
    //                                                                   =================
    @Override
    public void download(String fileName, byte[] data) {
        assertArgumentNotNull("fileName", fileName);
        assertArgumentNotNull("data", data);
        final ResponseDownloadResource resource = createResponseDownloadResource(fileName);
        resource.data(data);
        doDownload(resource);
    }

    @Override
    public void download(String fileName, InputStream stream) {
        assertArgumentNotNull("fileName", fileName);
        assertArgumentNotNull("stream", stream);
        final ResponseDownloadResource resource = createResponseDownloadResource(fileName);
        resource.stream(stream);
        doDownload(resource);
    }

    @Override
    public void download(String fileName, InputStream stream, int contentLength) {
        assertArgumentNotNull("fileName", fileName);
        assertArgumentNotNull("stream", stream);
        final ResponseDownloadResource resource = createResponseDownloadResource(fileName);
        resource.stream(stream, contentLength);
        doDownload(resource);
    }

    protected ResponseDownloadResource createResponseDownloadResource(String fileName) {
        final ResponseDownloadResource resource = new ResponseDownloadResource(fileName);
        if (resource.getContentType() != null) {
            return resource;
        }
        if (downloadExtensionContentTypeMap != null && fileName.contains(".")) {
            final String extension = Srl.substringLastRear(fileName, ".");
            final String contentType = downloadExtensionContentTypeMap.get(extension);
            if (contentType != null) {
                resource.contentType(contentType);
            }
        }
        return resource; // as default
    }

    @Override
    public void download(ResponseDownloadResource resource) {
        assertArgumentNotNull("resource", resource);
        doDownload(resource);
    }

    protected void doDownload(ResponseDownloadResource resource) {
        final HttpServletResponse response = getResponse();
        prepareDownloadResponse(resource, response);
        final byte[] byteData = resource.getByteData();
        if (byteData != null) {
            doDownloadByteData(resource, response, byteData);
        } else {
            doDownloadInputStream(resource, response);
        }
    }

    protected void prepareDownloadResponse(ResponseDownloadResource resource, HttpServletResponse response) {
        if (resource.getContentType() == null) {
            resource.contentTypeOctetStream(); // as default
            resource.headerContentDispositionAttachment(); // with header for the type
        }
        final String contentType = resource.getContentType();
        response.setContentType(contentType);
        final Map<String, String> headerMap = resource.getHeaderMap();
        for (Entry<String, String> entry : headerMap.entrySet()) {
            final String name = entry.getKey();
            final String value = entry.getValue();
            response.setHeader(name, value);
        }
    }

    protected void doDownloadByteData(ResponseDownloadResource resource, HttpServletResponse response, byte[] byteData) {
        try {
            final OutputStream out = response.getOutputStream();
            try {
                out.write(byteData);
            } finally {
                LdiOutputStreamUtil.close(out);
            }
        } catch (RuntimeException e) {
            String msg = "Failed to download the byte data: " + resource;
            throw new IllegalStateException(msg, e);
        } catch (IOException e) {
            String msg = "Failed to download the byte data: " + resource;
            throw new IllegalStateException(msg, e);
        }
    }

    protected void doDownloadInputStream(ResponseDownloadResource resource, HttpServletResponse response) {
        final InputStream ins = resource.getInputStream();
        if (ins == null) {
            String msg = "Either byte data or input stream is required: " + resource;
            throw new IllegalArgumentException(msg);
        }
        try {
            final Integer contentLength = resource.getContentLength();
            if (contentLength != null) {
                response.setContentLength(contentLength);
            }
            final OutputStream out = response.getOutputStream();
            try {
                LdiInputStreamUtil.copy(ins, out);
                LdiOutputStreamUtil.flush(out);
            } finally {
                LdiOutputStreamUtil.close(out);
            }
        } catch (RuntimeException e) {
            String msg = "Failed to download the input stream: " + resource;
            throw new IllegalStateException(msg, e);
        } catch (IOException e) {
            String msg = "Failed to download the input stream: " + resource;
            throw new IllegalStateException(msg, e);
        } finally {
            LdiInputStreamUtil.close(ins);
        }
    }

    // ===================================================================================
    //                                                                     Header Handling
    //                                                                     ===============
    @Override
    public void addHeader(String name, String value) {
        assertArgumentNotNull("name", name);
        assertArgumentNotNull("value", value);
        getResponse().addHeader(name, value);
    }

    @Override
    public void addNoCache() {
        addHeader("Pragma", "no-cache");
        addHeader("Cache-Control", "no-cache, no-store");
        addHeader("Expires", "Thu, 01 Dec 1994 16:00:00 GMT");
    }

    @Override
    public void setLocationPermanently(String url) {
        assertArgumentNotNull("url", url);
        getResponse().setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        getResponse().setHeader("Location", url);
    }

    @Override
    public void setResponseStatus(int sc) {
        getResponse().setStatus(sc);
    }

    // ===================================================================================
    //                                                                     Friends Gateway
    //                                                                     ===============
    protected RequestManager getRequestManager() {
        return ContainerUtil.getComponent(RequestManager.class);
    }

    // ===================================================================================
    //                                                                       Assist Helper
    //                                                                       =============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            throw new IllegalArgumentException("The variableName should not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
        }
    }
}
