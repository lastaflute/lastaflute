/*
 * Copyright 2015-2016 the original author or authors.
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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dbflute.util.Srl;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.web.direction.FwWebDirection;
import org.lastaflute.web.path.ActionPathResolver;
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
    protected static final String LF = "\n";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant director (AD) for framework. (NotNull: after initialization) */
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
        final FwWebDirection direction = assistWebDirection();
        final ResponseHandlingProvider provider = direction.assistResponseHandlingProvider();
        if (provider != null) {
            downloadExtensionContentTypeMap = provider.provideDownloadExtensionContentTypeMap();
        }
        showBootLogging();
    }

    protected FwWebDirection assistWebDirection() {
        return assistantDirector.assistWebDirection();
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

    @Override
    public boolean isCommitted() {
        return getResponse().isCommitted();
    }

    // ===================================================================================
    //                                                                   Redirect Response
    //                                                                   =================
    @Override
    public void redirect(Redirectable redirectable) throws IOException {
        assertArgumentNotNull("redirectable", redirectable);
        doRedirect(redirectable);
    }

    protected void doRedirect(Redirectable redirectable) throws IOException {
        final HttpServletResponse response = getResponse();
        response.sendRedirect(buildRedirectUrl(response, redirectable));
    }

    protected String buildRedirectUrl(HttpServletResponse response, Redirectable redirectable) {
        final String routingPath = redirectable.getRoutingPath();
        final String redirectUrl;
        if (needsContextPathForRedirectPath(routingPath, redirectable.isAsIs())) {
            redirectUrl = getRequestManager().getContextPath() + routingPath;
        } else {
            redirectUrl = routingPath;
        }
        return response.encodeRedirectURL(redirectUrl);
    }

    protected boolean needsContextPathForRedirectPath(String redirectPath, boolean asIs) {
        return !asIs && redirectPath.startsWith("/");
    }

    @Override
    public void movedPermanently(Redirectable redirectable) {
        assertArgumentNotNull("redirectable", redirectable);
        setLocationPermanently(buildRedirectUrl(getResponse(), redirectable)); // set up headers for 301
    }

    @Override
    public void movedPermanentlySsl(Redirectable redirectable, String host) {
        assertArgumentNotNull("redirectable", redirectable);
        assertArgumentNotNull("host", host);
        final String redirectUrl = buildRedirectUrl(getResponse(), redirectable);
        final String delimiter = redirectUrl.startsWith("/") ? "" : "/";
        setLocationPermanently("https://" + host + delimiter + redirectUrl); // set up headers for 301
    }

    @Override
    public void forward(Forwardable forwardable) throws ServletException, IOException {
        assertArgumentNotNull("forwardable", forwardable);
        final String forwardPath = forwardable.getRoutingPath();
        final HttpServletRequest request = getRequestManager().getRequest();
        final RequestDispatcher rd = request.getRequestDispatcher(forwardPath); // same context
        if (rd == null) {
            String msg = "Not found the request dispatcher for the URI: " + forwardable;
            throw new IllegalStateException(msg);
        }
        rd.forward(request, getResponse());
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
        showWritingResponse(json, contentType);
        write(json, contentType);
    }

    @Override
    public void writeAsJavaScript(String script) {
        assertArgumentNotNull("script", script);
        final String contentType = "application/javascript";
        showWritingResponse(script, contentType);
        write(script, contentType);
    }

    @Override
    public void writeAsXml(String xmlStr, String encoding) {
        assertArgumentNotNull("xmlStr", xmlStr);
        assertArgumentNotNull("encoding", encoding);
        final String contentType = "text/xml";
        showWritingResponse(xmlStr, contentType);
        write(xmlStr, contentType, encoding);
    }

    protected void showWritingResponse(String value, String contentType) {
        if (logger.isDebugEnabled()) {
            // to suppress noisy big data (no need all data for debug: also you can see it by response)
            final String exp = buildApiResponseDebugDisplay(value);
            logger.debug("#flow ...Writing response as {}: \n{}", contentType, exp);
        }
    }

    protected String buildApiResponseDebugDisplay(String value) {
        return Srl.cut(value, 500, "..."); // you can basically confirm it at front side so cut it here
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
    public void download(String fileName, WritternStreamCall writtenStreamLambda) {
        assertArgumentNotNull("fileName", fileName);
        assertArgumentNotNull("writtenStreamLambda", writtenStreamLambda);
        doDownload(createResponseDownloadResource(fileName).stream(writtenStreamLambda));
    }

    @Override
    public void download(String fileName, WritternStreamCall writtenStreamLambda, int contentLength) {
        assertArgumentNotNull("fileName", fileName);
        assertArgumentNotNull("writtenStreamLambda", writtenStreamLambda);
        doDownload(createResponseDownloadResource(fileName).stream(writtenStreamLambda, contentLength));
    }

    protected ResponseDownloadResource createResponseDownloadResource(String fileName) {
        final ResponseDownloadResource resource = new ResponseDownloadResource(fileName);
        setupContentTypeByExtension(resource);
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
        if (resource.isReturnAsEmptyBody()) {
            return;
        }
        if (resource.hasByteData()) {
            doDownloadByteData(resource, response);
        } else {
            doDownloadStreamCall(resource, response);
        }
    }

    protected void prepareDownloadResponse(ResponseDownloadResource resource, HttpServletResponse response) {
        if (!resource.hasContentType()) {
            setupContentTypeByExtension(resource);
            if (!resource.hasContentType()) { // retry
                resource.contentTypeOctetStream(); // as default
            }
        }
        if (!resource.hasContentDisposition()) {
            resource.headerContentDispositionAttachment(); // as default
        }
        response.setContentType(resource.getContentType());
        resource.getHeaderMap().forEach((key, values) -> {
            for (String value : values) {
                response.addHeader(key, value); // added as array if already exists
            }
        });
    }

    protected void setupContentTypeByExtension(ResponseDownloadResource resource) {
        if (downloadExtensionContentTypeMap == null || downloadExtensionContentTypeMap.isEmpty()) {
            return;
        }
        final String fileName = resource.getFileName();
        if (fileName.contains(".")) {
            final String extension = Srl.substringLastRear(fileName, ".");
            final String contentType = downloadExtensionContentTypeMap.get(extension);
            if (contentType != null) {
                resource.contentType(contentType);
            }
        }
    }

    protected void doDownloadByteData(ResponseDownloadResource resource, HttpServletResponse response) {
        createResponseDownloadPerformer().downloadByteData(resource, response);
    }

    protected void doDownloadStreamCall(ResponseDownloadResource resource, HttpServletResponse response) {
        createResponseDownloadPerformer().downloadStreamCall(resource, response);
    }

    protected ResponseDownloadPerformer createResponseDownloadPerformer() {
        return new ResponseDownloadPerformer();
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
        addHeader(HEADER_PRAGMA, "no-cache");
        addHeader(HEADER_CACHE_CONTROL, "no-cache, no-store");
        addHeader(HEADER_EXPIRES, "Thu, 01 Dec 1994 16:00:00 GMT");
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
        return ContainerUtil.getComponent(RequestManager.class); // not to be cyclic reference
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            throw new IllegalArgumentException("The variableName should not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
        }
    }
}
