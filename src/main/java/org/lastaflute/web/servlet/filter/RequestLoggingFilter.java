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
package org.lastaflute.web.servlet.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The filter for logging of request. <br>
 * Seasar's RequestDumpFilter is used as reference.
 * 
 * <p>This filter outputs request info as debug level in development
 * and outputs exception info as error level in development and production.
 * The error message contains request info, so you can see it.</p>
 * 
 * <p>The requests for resource files, e.g. JavaScript(.js) and CSS(.css), is out of target.
 * You can customize it by {@link FilterConfig}.</p>
 *
 * <p>And if no encoding in request, character encoding is set (default: UTF-8)
 * to handle request parameters correctly in this filter for debug.
 * e.g. Tomcat parses them as latin1 and keep parsed parameters in request object.
 * It is set in spite of log level for same behavior in several environments.</p>
 * @author jflute
 */
public class RequestLoggingFilter implements Filter {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    public static final String ERROR_ATTRIBUTE_KEY = "javax.servlet.error.exception";
    protected static final String LF = "\n";
    protected static final String IND = "  ";
    protected static final ThreadLocal<String> begunLocal = new ThreadLocal<String>();
    protected static final ThreadLocal<RequestClientErrorHandler> clientErrorHandlerLocal = new ThreadLocal<>();
    protected static final ThreadLocal<RequestServerErrorHandler> serverErrorHandlerLocal = new ThreadLocal<>();
    protected static final ThreadLocal<RequestAccessLogHandler> accessLogHandlerLocal = new ThreadLocal<>();

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected FilterConfig config;
    protected boolean errorLogging;
    protected Set<String> exceptExtSet;
    protected Pattern exceptUrlPattern;
    protected Pattern requestUriTitleUrlPattern;
    protected Pattern subRequestUrlPattern;
    protected String requestCharacterEncoding;

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    public void init(FilterConfig filterConfig) throws ServletException {
        this.config = filterConfig;
        this.errorLogging = isBooleanParameter(filterConfig, "errorLogging", true);
        setupExceptExtSet(filterConfig);
        setupExceptUrlPattern(filterConfig);
        setupRequestUriTitleUrlPattern(filterConfig);
        setupSubRequestUrlPatternUrlPattern(filterConfig);
        setupRequestCharacterEncoding(filterConfig);
    }

    protected boolean isBooleanParameter(FilterConfig filterConfig, String name, boolean defaultValue) {
        final String value = filterConfig.getInitParameter(name);
        return value != null ? value.trim().equalsIgnoreCase("true") : defaultValue;
    }

    protected void setupExceptExtSet(FilterConfig filterConfig) {
        final String value = filterConfig.getInitParameter("exceptExtSet");
        if (value != null) {
            final String[] splitAry = value.split(","); // e.g. js,css,png,gif,jpg,ico,svg,svgz,ttf
            exceptExtSet = new LinkedHashSet<String>();
            for (String element : splitAry) {
                exceptExtSet.add("." + element.trim());
            }
        } else { // as default
            final List<String> defaultExtList = getDefaultExceptExtSet();
            exceptExtSet = new LinkedHashSet<String>(defaultExtList);
        }
    }

    protected List<String> getDefaultExceptExtSet() {
        return Arrays.asList(".js", ".css", ".png", ".gif", ".jpg", ".ico", ".svg", ".svgz", ".ttf", ".woff");
    }

    protected void setupExceptUrlPattern(FilterConfig filterConfig) {
        String pattern = filterConfig.getInitParameter("exceptUrlPattern");
        if (pattern == null || pattern.trim().length() == 0) {
            pattern = filterConfig.getInitParameter("ignoreUrlPattern"); // for compatible
        }
        if (pattern != null && pattern.trim().length() > 0) {
            this.exceptUrlPattern = Pattern.compile(pattern);
        }
    }

    protected void setupRequestUriTitleUrlPattern(FilterConfig filterConfig) {
        final String pattern = filterConfig.getInitParameter("requestUriTitleUrlPattern");
        if (pattern != null && pattern.trim().length() > 0) {
            this.requestUriTitleUrlPattern = Pattern.compile(pattern);
        }
    }

    protected void setupSubRequestUrlPatternUrlPattern(FilterConfig filterConfig) {
        final String pattern = filterConfig.getInitParameter("subRequestUrlPattern");
        if (pattern != null && pattern.trim().length() > 0) {
            this.subRequestUrlPattern = Pattern.compile(pattern);
        }
    }

    protected void setupRequestCharacterEncoding(FilterConfig filterConfig) {
        this.requestCharacterEncoding = filterConfig.getInitParameter("requestCharacterEncoding");
    }

    // ===================================================================================
    //                                                                              Filter
    //                                                                              ======
    public void doFilter(ServletRequest servRequest, ServletResponse servResponse, FilterChain chain) throws IOException, ServletException {
        if (!isHttpServlet(servRequest, servResponse)) {
            chain.doFilter(servRequest, servResponse);
            return;
        }
        final HttpServletRequest request = (HttpServletRequest) servRequest;
        final HttpServletResponse response = (HttpServletResponse) servResponse;
        if (isAlreadyBegun() || !isTargetPath(request)) { // e.g. forwarding to JSP or .html
            chain.doFilter(request, response);
        } else { // target top level process
            actuallyFilter(chain, request, response);
        }
    }

    protected void actuallyFilter(FilterChain chain, HttpServletRequest request, HttpServletResponse response) throws IOException {
        prepareCharacterEncodingIfNeeds(request);
        final Long before = System.currentTimeMillis(); // used in not only debug but also error
        if (logger.isDebugEnabled()) {
            before(request, response);
        }
        String specifiedErrorTitle = null;
        boolean existsServerError = false;
        Throwable cause = null;
        try {
            markBegun();
            chain.doFilter(request, response);
            if (handleErrorAttribute(request, response)) {
                existsServerError = true;
            }
        } catch (RequestClientErrorException e) {
            specifiedErrorTitle = handleClientError(request, response, e);
            cause = e;
        } catch (RuntimeException e) {
            // no throw the exception to suppress duplicate error message
            // (Jetty's message doesn't have line separator so hard to see it)
            sendInternalServerError(request, response, e);
            logError(request, response, "*RuntimeException occurred.", before, e);
            existsServerError = true;
            cause = e;
        } catch (ServletException e) { // also no throw same reason as RuntimeException catch
            final Throwable rootCause = e.getRootCause();
            if (rootCause instanceof RequestClientErrorException) {
                specifiedErrorTitle = handleClientError(request, response, (RequestClientErrorException) rootCause);
                cause = rootCause;
            } else {
                final Throwable realCause = rootCause != null ? rootCause : e;
                sendInternalServerError(request, response, realCause);
                logError(request, response, "*ServletException occurred.", before, realCause);
                existsServerError = true;
                cause = realCause;
            }
        } catch (IOException e) { // also no throw
            sendInternalServerError(request, response, e);
            logError(request, response, "*IOException occurred.", before, e);
            existsServerError = true;
            cause = e;
        } catch (Error e) { // also no throw
            sendInternalServerError(request, response, e);
            logError(request, response, "*Error occurred.", before, e);
            existsServerError = true;
            cause = e;
        } finally {
            try {
                handleAccessLog(request, response, cause, before);
            } finally {
                clearMark();
                clearHandler();
                if (logger.isDebugEnabled()) {
                    if (existsServerError) {
                        attention(request, response);
                    } else {
                        // only when success request because error logging contains request info
                        final Long after = System.currentTimeMillis();
                        after(request, response, before, after, specifiedErrorTitle);
                    }
                }
            }
        }
    }

    protected boolean isHttpServlet(ServletRequest servletRequest, ServletResponse servletResponse) {
        return (servletRequest instanceof HttpServletRequest) && (servletResponse instanceof HttpServletResponse);
    }

    protected boolean isTargetPath(HttpServletRequest request) {
        if (exceptUrlPattern != null) {
            final String uri = getRequestURI(request);
            if (exceptUrlPattern.matcher(uri).find()) {
                return false;
            }
        }
        if (exceptExtSet != null) {
            // not use request URI because it might have noise e.g. jsessionID
            final String path = getServletPath(request);
            if (path != null && path.contains(".")) {
                final int indexOf = path.lastIndexOf(".");
                final String ext = path.substring(indexOf);
                if (exceptExtSet.contains(ext)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isAlreadyBegun() { // for e.g. enabling access log
        return begunLocal.get() != null;
    }

    protected void markBegun() {
        begunLocal.set("begun");
    }

    protected void clearMark() {
        begunLocal.set(null);
    }

    protected void clearHandler() {
        clientErrorHandlerLocal.set(null);
        serverErrorHandlerLocal.set(null);
        accessLogHandlerLocal.set(null);
    }

    protected void prepareCharacterEncodingIfNeeds(HttpServletRequest request) throws UnsupportedEncodingException {
        if (request.getCharacterEncoding() == null) {
            // logging filter calls parameters for debug
            // but if no setting of encoding, Tomcat parses parameters as latin1
            // and keep the parameters parsed by wrong encoding in request object
            // so needs to set encoding here
            // (it is set in spite of log level for same behavior in several environments)
            request.setCharacterEncoding(requestCharacterEncoding != null ? requestCharacterEncoding : "UTF-8");
        }
    }

    // -----------------------------------------------------
    //                                                Before
    //                                                ------
    protected void before(HttpServletRequest request, HttpServletResponse response) {
        final StringBuilder sb = new StringBuilder();
        final String beginDecoration;
        if (isSubRequestUrl(request)) {
            beginDecoration = "- - - - - - - - - - {SUB BEGIN}: ";
        } else { // mainly here
            beginDecoration = "* * * * * * * * * * {BEGIN}: ";
        }
        sb.append(beginDecoration);
        sb.append(getTitlePath(request));
        sb.append(LF).append(IND);
        buildRequestInfo(sb, request, response, false);
        logger.debug(sb.toString().trim());
    }

    protected void buildRequestInfo(StringBuilder sb, HttpServletRequest request, HttpServletResponse response, boolean showResponse) {
        sb.append("requestClass=" + request.getClass().getName());
        sb.append(" ; sessionId=").append(request.getRequestedSessionId());

        sb.append(LF).append(IND);
        final String queryString = request.getQueryString();
        sb.append("; url=").append(request.getRequestURL()).append(queryString != null ? "?" + queryString : "");
        sb.append(LF).append(IND);
        sb.append("; method=").append(request.getMethod());
        sb.append(" ; protocol=").append(request.getProtocol());
        sb.append(" ; scheme=").append(request.getScheme());
        sb.append(" ; secure=").append(request.isSecure());
        sb.append(" ; remoteAddr=").append(request.getRemoteAddr());
        sb.append(" ; remoteHost=").append(request.getRemoteHost());
        sb.append(LF).append(IND);
        sb.append("; characterEncoding=").append(request.getCharacterEncoding());
        sb.append(" ; contentLength=").append(request.getContentLength());
        sb.append(" ; contentType=").append(request.getContentType());
        sb.append(" ; locale=").append(request.getLocale());
        sb.append(" ; locales=").append(buildLocalesExp(request.getLocales()));

        sb.append(LF);
        buildRequestHeaders(sb, request);
        buildRequestParameters(sb, request);
        buildCookies(sb, request);
        buildRequestAttributes(sb, request);
        buildSessionAttributes(sb, request);
        if (showResponse) {
            sb.append(IND);
            buildResponseInfo(sb, request, response);
        }
    }

    protected String buildLocalesExp(Enumeration<Locale> locales) {
        final StringBuilder sb = new StringBuilder();
        while (locales.hasMoreElements()) {
            final Locale locale = locales.nextElement();
            sb.append(sb.length() > 0 ? "," : "");
            sb.append(locale.toString());
        }
        return sb.toString();
    }

    // -----------------------------------------------------
    //                                                 After
    //                                                 -----
    protected void after(HttpServletRequest request, HttpServletResponse response, Long before, Long after, String specifiedErrorTitle) {
        final StringBuilder sb = new StringBuilder();
        sb.append(LF).append(IND);
        buildResponseInfo(sb, request, response);
        // hope response cookie (not request cookie)
        //buildCookies(sb, request);
        buildRequestAttributes(sb, request);
        buildSessionAttributes(sb, request);

        final String endDecoration;
        if (isSubRequestUrl(request)) {
            endDecoration = "- - - - - - - - - - {SUB END}: ";
        } else { // mainly here
            endDecoration = "* * * * * * * * * * {END}: ";
        }
        sb.append(endDecoration);
        sb.append(getTitlePath(request));
        sb.append(" [" + convertToPerformanceView(after.longValue() - before.longValue()) + "]");
        sb.append(LF);
        if (specifiedErrorTitle != null) {
            sb.append(" *").append(specifiedErrorTitle).append(", read the message for the detail");
        }
        sb.append(LF);

        String logString = sb.toString();
        logger.debug(logString);
    }

    // -----------------------------------------------------
    //                                          Request Info
    //                                          ------------
    protected boolean isRequestUriTitleUrl(final String servletPath) {
        return requestUriTitleUrlPattern != null && requestUriTitleUrlPattern.matcher(servletPath).find();
    }

    protected boolean isSubRequestUrl(HttpServletRequest request) {
        final String servletPath = ((HttpServletRequest) request).getServletPath();
        return subRequestUrlPattern != null && subRequestUrlPattern.matcher(servletPath).find();
    }

    protected String getTitlePath(HttpServletRequest request) {
        final String servletPath = ((HttpServletRequest) request).getServletPath();
        if (isRequestUriTitleUrl(servletPath)) {
            return getRequestURI(request);
        }
        return servletPath;
    }

    protected String getServletPath(HttpServletRequest request) {
        return ((HttpServletRequest) request).getServletPath();
    }

    protected String getRequestURI(HttpServletRequest request) {
        return ((HttpServletRequest) request).getRequestURI();
    }

    protected void buildRequestHeaders(StringBuilder sb, HttpServletRequest request) {
        for (Iterator<?> it = toSortedSet(request.getHeaderNames()).iterator(); it.hasNext();) {
            final String name = (String) it.next();
            doBuildHeaderLine(sb, name, request.getHeader(name));
        }
    }

    protected void buildResponseHeaders(StringBuilder sb, HttpServletResponse response) {
        for (Iterator<?> it = toSortedSet(response.getHeaderNames()).iterator(); it.hasNext();) {
            final String name = (String) it.next();
            doBuildHeaderLine(sb, name, response.getHeader(name));
        }
    }

    protected void doBuildHeaderLine(StringBuilder sb, String name, String value) {
        sb.append(IND);
        sb.append("[header] ").append(name);
        sb.append("=").append(value);
        sb.append(LF);
    }

    protected void buildCookies(StringBuilder sb, HttpServletRequest request) {
        final Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return;
        }
        for (int i = 0; i < cookies.length; i++) {
            sb.append(IND);
            sb.append("[cookie] ").append(cookies[i].getName());
            sb.append("=").append(cookies[i].getValue());
            sb.append(LF);
        }
    }

    protected void buildRequestParameters(StringBuilder sb, HttpServletRequest request) {
        for (final Iterator<?> it = toSortedSet(request.getParameterNames()).iterator(); it.hasNext();) {
            final String name = (String) it.next();
            sb.append(IND);
            sb.append("[param] ").append(name).append("=");
            final String[] values = request.getParameterValues(name);
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(values[i]);
            }
            sb.append(LF);
        }
    }

    protected void buildRequestAttributes(StringBuilder sb, HttpServletRequest request) {
        for (Iterator<?> it = toSortedSet(request.getAttributeNames()).iterator(); it.hasNext();) {
            final String name = (String) it.next();
            if (ERROR_ATTRIBUTE_KEY.equals(name)) {
                continue; // because the error is handled in this filter
            }
            final Object attr = request.getAttribute(name);
            sb.append(IND);
            sb.append("[request] ").append(name).append("=");
            sb.append(filterAttributeDisp(attr));
            sb.append(LF);
        }
    }

    protected void buildSessionAttributes(StringBuilder sb, HttpServletRequest request) {
        final HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        for (Iterator<?> it = toSortedSet(session.getAttributeNames()).iterator(); it.hasNext();) {
            final String name = (String) it.next();
            final Object attr = session.getAttribute(name);
            sb.append(IND);
            sb.append("[session] ").append(name).append("=");
            sb.append(filterAttributeDisp(attr));
            sb.append(LF);
        }
    }

    protected String filterAttributeDisp(Object attr) {
        if (attr == null) {
            return null;
        }
        final String stringExp;
        if (attr instanceof Throwable) { // exception will be displayed in another way
            stringExp = ((Throwable) attr).getMessage();
        } else {
            stringExp = attr.toString();
        }
        // might contain line separator in the expression
        // and large display is noisy for debug so one liner
        return convertToOneLinerDisp(convertToCutDisp(stringExp));
    }

    protected String convertToCutDisp(String msg) {
        if (msg == null) {
            return null;
        }
        final int limit = 500;
        return msg.length() > limit ? (msg.substring(0, limit) + "...") : msg;
    }

    protected String convertToOneLinerDisp(String msg) {
        if (msg == null) {
            return null;
        }
        final String filtered;
        final String ln = "\n";
        if (msg.contains(ln)) {
            filtered = msg.substring(0, msg.indexOf(ln)) + "...";
        } else {
            filtered = msg;
        }
        return filtered;
    }

    protected void buildResponseInfo(StringBuilder sb, HttpServletRequest request, HttpServletResponse response) {
        sb.append("responseClass=").append(response.getClass().getName());
        sb.append(" ; committed=").append(response.isCommitted());
        sb.append(LF).append(IND);
        sb.append("; httpStatus=").append(response.getStatus());
        sb.append(" ; contentType=").append(response.getContentType());
        sb.append(" ; locale=").append(response.getLocale());
        sb.append(LF);
        buildResponseHeaders(sb, response);
    }

    // ===================================================================================
    //                                                                     Error Attribute
    //                                                                     ===============
    protected boolean handleErrorAttribute(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String attributeKey = ERROR_ATTRIBUTE_KEY;
        final Object errorObj = request.getAttribute(attributeKey);
        if (errorObj != null && errorObj instanceof Throwable) {
            if (errorObj instanceof RuntimeException) {
                throw (RuntimeException) errorObj;
            } else if (errorObj instanceof Throwable) {
                String msg = "Found the exception in " + attributeKey;
                throw new ServletException(msg, (Throwable) errorObj);
            }
            sendInternalServerError(request, response, null);
            final String msg = "The error attribute exists but unknown type: " + errorObj;
            if (errorLogging) {
                logger.error(msg);
            } else {
                logger.debug(msg);
            }
            return true;
        }
        return false;
    }

    // ===================================================================================
    //                                                               Client Error e.g. 404
    //                                                               =====================
    protected String handleClientError(HttpServletRequest request, HttpServletResponse response, RequestClientErrorException cause)
            throws IOException {
        final boolean beforeHandlingCommitted = response.isCommitted(); // basically false
        processClientErrorCallback(request, response, cause);
        final String title;
        if (response.isCommitted()) {
            title = response.getStatus() + " (thrown as " + cause.getTitle() + ")";
            if (beforeHandlingCommitted) { // basically no way but just in case
                showCliEx(cause, () -> {
                    final StringBuilder sb = new StringBuilder();
                    sb.append("*Cannot send error as '").append(title).append("' because of already committed:");
                    sb.append(" path=").append(request.getRequestURI());
                    return sb.toString();
                });
                return title; // cannot help it
            }
            // committed in callback process
        } else {
            title = cause.getTitle();
        }
        showCliEx(cause, () -> {
            final StringBuilder sb = new StringBuilder();
            sb.append(LF).append("_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/");
            sb.append(LF).append("...Sending error as '").append(title).append("' manually");
            sb.append(LF).append(" Request: ").append(request.getRequestURI());
            final String queryString = request.getQueryString();
            if (queryString != null && !queryString.isEmpty()) {
                sb.append("?").append(queryString);
            }
            sb.append(LF);
            buildRequestHeaders(sb, request);
            buildSessionAttributes(sb, request);
            sb.append(" Exception: ").append(cause.getClass().getName());
            sb.append(LF).append(" Message: ");
            final String causeMsg = cause.getMessage();
            if (causeMsg != null && causeMsg.contains(LF)) {
                sb.append(LF).append("/- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
                sb.append(LF).append(causeMsg);
                sb.append(LF).append("- - - - - - - - - -/");
            } else {
                sb.append(causeMsg);
            }
            sb.append(LF).append(" Stack Traces:");
            buildClientErrorStackTrace(cause, sb, 0);
            sb.append(LF);
            sb.append("_/_/_/_/_/_/_/_/_/_/");
            return sb.toString();
        });
        try {
            if (!response.isCommitted()) { // because may be committed in callback process
                response.sendError(cause.getErrorStatus());
            }
            return title;
        } catch (IOException sendEx) {
            final String msg = "Failed to send error as '" + title + "': " + sendEx.getMessage();
            if (errorLogging) {
                logger.error(msg);
            } else {
                showCliEx(cause, () -> msg);
            }
            return title; // cannot help it
        }
    }

    // -----------------------------------------------------
    //                                 Client Error Callback
    //                                 ---------------------
    protected void processClientErrorCallback(HttpServletRequest request, HttpServletResponse response, RequestClientErrorException cause) {
        final RequestClientErrorHandler clientErrorHandler = clientErrorHandlerLocal.get();
        if (clientErrorHandler == null) {
            return;
        }
        try {
            clientErrorHandler.handle(request, response, cause);
        } catch (Throwable handlingEx) {
            final String msg = "Failed to handle 'Client Error' by the handler: " + clientErrorHandler;
            if (errorLogging) {
                logger.error(msg, handlingEx);
            } else {
                logger.debug(msg, handlingEx);
            }
        }
    }

    /**
     * The handler of 'Client Error' in the request.
     */
    @FunctionalInterface
    public interface RequestClientErrorHandler {

        /**
         * Handle the 'Client Error' exception. <br>
         * The info logging is executed after your handling so basically you don't need logging.
         * @param request The request provided by caller. (NotNull)
         * @param response The response provided by caller, might be already committed. (NotNull)
         * @param cause The cause of this 'Client Error'. (NotNull)
         */
        void handle(HttpServletRequest request, HttpServletResponse response, RequestClientErrorException cause);
    }

    public static void setClientErrorHandlerOnThread(RequestClientErrorHandler handler) {
        clientErrorHandlerLocal.set(handler);
    }

    // -----------------------------------------------------
    //                                     Show Client Error
    //                                     -----------------
    protected void showCliEx(RequestClientErrorException cause, Supplier<String> msgSupplier) {
        final DelicateErrorLoggingLevel loggingLevel = cause.getLoggingLevel();
        if (DelicateErrorLoggingLevel.DEBUG.equals(loggingLevel)) {
            if (logger.isDebugEnabled()) {
                logger.debug(msgSupplier.get());
            }
        } else if (DelicateErrorLoggingLevel.INFO.equals(loggingLevel)) {
            if (logger.isInfoEnabled()) {
                logger.info(msgSupplier.get());
            }
        } else if (DelicateErrorLoggingLevel.WARN.equals(loggingLevel)) {
            if (logger.isWarnEnabled()) {
                logger.warn(msgSupplier.get());
            }
        } else if (DelicateErrorLoggingLevel.ERROR.equals(loggingLevel)) {
            if (logger.isErrorEnabled()) {
                logger.error(msgSupplier.get());
            }
        } else { // as default
            if (logger.isInfoEnabled()) {
                logger.info(msgSupplier.get());
            }
        }
    }

    protected void buildClientErrorStackTrace(Throwable cause, StringBuilder sb, int nestLevel) {
        if (nestLevel > 0) { // first level message already appended
            sb.append(LF).append("Caused by: ").append(cause.getClass().getName());
            sb.append(": ").append(cause.getMessage());
        }
        final StackTraceElement[] stackTrace = cause.getStackTrace();
        if (stackTrace == null) { // just in case
            return;
        }
        final int limit = nestLevel == 0 ? 10 : 3;
        int index = 0;
        for (StackTraceElement element : stackTrace) {
            if (index > limit) { // not all because it's not error
                sb.append(LF).append("  ...");
                break;
            }
            final String className = element.getClassName();
            final String fileName = element.getFileName(); // might be null
            final int lineNumber = element.getLineNumber();
            final String methodName = element.getMethodName();
            sb.append(LF).append("  at ").append(className).append(".").append(methodName);
            sb.append("(").append(fileName);
            if (lineNumber >= 0) {
                sb.append(":").append(lineNumber);
            }
            sb.append(")");
            ++index;
        }
        final Throwable nested = cause.getCause();
        if (nested != null && nested != cause) {
            buildClientErrorStackTrace(nested, sb, nestLevel + 1);
        }
    }

    // -----------------------------------------------------
    //                                Client Error Exception
    //                                ----------------------
    /**
     * The exception that means specified client error for the current request. <br>
     * You can send specified status e.g. 400, 404 by throwing this exception in your program.
     */
    public static class RequestClientErrorException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        protected final String title; // not null
        protected final int errorStatus; // one of HttpServletResponse.SC_...
        protected DelicateErrorLoggingLevel loggingLevel; // null allowed, INFO as default

        public RequestClientErrorException(String msg, String title, int errorStatus) {
            super(msg);
            this.title = title;
            this.errorStatus = errorStatus;
        }

        public RequestClientErrorException(String msg, String title, int errorStatus, Throwable cause) {
            super(msg, cause);
            this.title = title;
            this.errorStatus = errorStatus;
        }

        public RequestClientErrorException asLogging(DelicateErrorLoggingLevel loggingLevel) {
            this.loggingLevel = loggingLevel;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public int getErrorStatus() {
            return errorStatus;
        }

        public DelicateErrorLoggingLevel getLoggingLevel() {
            return loggingLevel;
        }
    }

    public static enum DelicateErrorLoggingLevel {
        DEBUG, INFO, WARN, ERROR
    }

    // ===================================================================================
    //                                                               Server Error e.g. 500
    //                                                               =====================
    protected void sendInternalServerError(HttpServletRequest request, HttpServletResponse response, Throwable cause) throws IOException {
        if (cause != null) {
            processServerErrorCallback(request, response, cause);
            request.setAttribute(ERROR_ATTRIBUTE_KEY, cause); // for something outer process
        }
        try {
            if (!response.isCommitted()) { // might be committed in callback
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (IOException sendEx) {
            final String msg = "Failed to send error as '500 Error': " + sendEx.getMessage();
            if (errorLogging) {
                logger.error(msg);
            } else {
                logger.debug(msg);
            }
            return; // cannot help it
        }
    }

    // -----------------------------------------------------
    //                                 Server Error Callback
    //                                 ---------------------
    protected void processServerErrorCallback(HttpServletRequest request, HttpServletResponse response, Throwable cause) {
        final RequestServerErrorHandler serverErrorHandler = serverErrorHandlerLocal.get();
        if (serverErrorHandler == null) {
            return;
        }
        try {
            serverErrorHandler.handle(request, response, cause);
        } catch (Throwable handlingEx) {
            final String msg = "Failed to handle '500 Error' by the handler: " + serverErrorHandler;
            if (errorLogging) {
                logger.error(msg, handlingEx);
            } else {
                logger.debug(msg, handlingEx);
            }
        }
    }

    /**
     * The handler of 'Server Error' in the request.
     */
    @FunctionalInterface
    public interface RequestServerErrorHandler {

        /**
         * Handle the 'Server Error' exception. <br>
         * The error logging is executed after your handling so basically you don't need logging.
         * @param request The request provided by caller. (NotNull)
         * @param response The response provided by caller, might be already committed. (NotNull)
         * @param cause The cause of this 'Server Error'. (NotNull)
         */
        void handle(HttpServletRequest request, HttpServletResponse response, Throwable cause);
    }

    public static void setServerErrorHandlerOnThread(RequestServerErrorHandler handler) {
        serverErrorHandlerLocal.set(handler);
    }

    // ===================================================================================
    //                                                                      Error Handling
    //                                                                      ==============
    protected void logError(HttpServletRequest request, HttpServletResponse response, String comment, Long before, Throwable cause) {
        final StringBuilder sb = new StringBuilder();
        sb.append(comment);
        sb.append(LF);
        sb.append("/= = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =: ").append(getTitlePath(request));
        sb.append(LF).append(IND);
        try {
            buildRequestInfo(sb, request, response, true);
        } catch (RuntimeException continued) {
            sb.append("*Failed to get request info: " + continued.getMessage());
            sb.append(LF);
        }
        final long after = System.currentTimeMillis();
        final String performanceView = convertToPerformanceView(after - before);
        sb.append("= = = = = = = = = =/ [").append(performanceView).append("] #").append(Integer.toHexString(cause.hashCode()));
        buildExceptionStackTrace(cause, sb); // extract stack trace manually
        final String msg = sb.toString().trim();
        if (errorLogging) {
            // not use second argument here
            // because Logback loads classes in stack trace destroying hot deploy
            // so show stack trace added to the logging message
            logger.error(msg);
        } else {
            logger.debug(msg);
        }
    }

    protected void buildExceptionStackTrace(Throwable cause, StringBuilder sb) {
        sb.append(LF);
        final ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        PrintStream ps = null;
        try {
            ps = new PrintStream(out);
            cause.printStackTrace(ps);
            final String encoding = "UTF-8";
            try {
                sb.append(out.toString(encoding));
            } catch (UnsupportedEncodingException continued) {
                logger.warn("Unknown encoding: " + encoding, continued);
                sb.append(out.toString()); // retry without encoding
            }
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    protected void attention(HttpServletRequest request, HttpServletResponse response) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{FAILURE}: ").append(getTitlePath(request));
        sb.append(LF);
        sb.append(" *Read the exception message!");
        sb.append(LF);
        logger.debug(sb.toString());
    }

    // ===================================================================================
    //                                                                          Access Log
    //                                                                          ==========
    protected void handleAccessLog(HttpServletRequest request, HttpServletResponse response, Throwable cause, Long before) {
        final RequestAccessLogHandler handler = accessLogHandlerLocal.get();
        if (handler != null) {
            handler.handle(request, response, cause, before);
        }
    }

    /**
     * The handler of 'Access Log' in the request.
     */
    @FunctionalInterface
    public interface RequestAccessLogHandler {

        /**
         * Handle the 'Access Log' process. <br>
         * The error logging is executed after your handling so basically you don't need logging.
         * @param request The request provided by caller. (NotNull)
         * @param response The response provided by caller, might be already committed. (NotNull)
         * @param cause The cause of this 'Access Log'. (NotNull)
         * @param before The time milliseconds before request process, you can derive performance cost of request.
         */
        void handle(HttpServletRequest request, HttpServletResponse response, Throwable cause, long before);
    }

    public static void setAccessLogHandlerOnThread(RequestAccessLogHandler handler) {
        accessLogHandlerLocal.set(handler);
    }

    // ===================================================================================
    //                                                                             Destroy
    //                                                                             =======
    public void destroy() {
        config = null;
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected SortedSet<?> toSortedSet(Enumeration<?> enu) {
        final SortedSet<Object> set = new TreeSet<Object>();
        set.addAll(Collections.list(enu));
        return set;
    }

    protected SortedSet<?> toSortedSet(Collection<?> enu) {
        return new TreeSet<Object>(enu);
    }

    protected String replaceString(String str, String fromStr, String toStr) {
        StringBuilder sb = null; // lazy load
        int basePos = 0;
        int nextPos = 0;
        do {
            basePos = str.indexOf(fromStr, nextPos);
            if (nextPos == 0 && basePos < 0) { // first loop and not found
                return str; // without creating StringBuilder
            }
            if (sb == null) {
                sb = new StringBuilder();
            }
            if (basePos == 0) {
                sb.append(toStr);
                nextPos = fromStr.length();
            } else if (basePos > 0) {
                sb.append(str.substring(nextPos, basePos));
                sb.append(toStr);
                nextPos = basePos + fromStr.length();
            } else { // (basePos < 0) second or after loop only
                sb.append(str.substring(nextPos));
                return sb.toString();
            }
        } while (true);
    }

    /**
     * Convert to performance view.
     * @param afterMinusBefore The difference between before time and after time.
     * @return The view string to show performance. e.g. 01m40s012ms (NotNull)
     */
    protected String convertToPerformanceView(long afterMinusBefore) { // from DfTraceViewUtil.java
        if (afterMinusBefore < 0) {
            return String.valueOf(afterMinusBefore);
        }

        long sec = afterMinusBefore / 1000;
        final long min = sec / 60;
        sec = sec % 60;
        final long mil = afterMinusBefore % 1000;

        final StringBuffer sb = new StringBuffer();
        if (min >= 10) { // Minute
            sb.append(min).append("m");
        } else if (min < 10 && min >= 0) {
            sb.append("0").append(min).append("m");
        }
        if (sec >= 10) { // Second
            sb.append(sec).append("s");
        } else if (sec < 10 && sec >= 0) {
            sb.append("0").append(sec).append("s");
        }
        if (mil >= 100) { // Millisecond
            sb.append(mil).append("ms");
        } else if (mil < 100 && mil >= 10) {
            sb.append("0").append(mil).append("ms");
        } else if (mil < 10 && mil >= 0) {
            sb.append("00").append(mil).append("ms");
        }

        return sb.toString();
    }
}
