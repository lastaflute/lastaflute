/*
 * Copyright 2015-2024 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.dbflute.exception.AccessContextNotFoundException;
import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.web.exception.ResponseClientAbortIOException;
import org.lastaflute.web.exception.ResponseDownloadFailureException;
import org.lastaflute.web.exception.ResponseDownloadStreamCallUpdateException;
import org.lastaflute.web.servlet.request.stream.WrittenStreamCall;
import org.lastaflute.web.servlet.request.stream.WrittenStreamOut;
import org.lastaflute.web.servlet.request.stream.WritternZipStreamCall;
import org.lastaflute.web.servlet.request.stream.WritternZipStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class ResponseDownloadPerformer {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(ResponseDownloadPerformer.class);
    protected static final String LF = "\n";

    // ===================================================================================
    //                                                                           Byte Data
    //                                                                           =========
    public void downloadByteData(ResponseDownloadResource resource, HttpServletResponse response) {
        final byte[] byteData = resource.getByteData();
        if (byteData == null) {
            String msg = "Either byte data or input stream is required: " + resource;
            throw new IllegalArgumentException(msg);
        }
        try {
            final OutputStream out = response.getOutputStream();
            try {
                out.write(byteData);
            } finally {
                closeDownloadStream(out);
            }
        } catch (RuntimeException e) {
            throw new ResponseDownloadFailureException("Failed to download the byte data: " + resource, e);
        } catch (IOException e) {
            handleDownloadIOException(resource, e);
        }
    }

    // ===================================================================================
    //                                                                        Input Stream
    //                                                                        ============
    // switched to stream call way for closing headache
    public void downloadStreamCall(ResponseDownloadResource resource, HttpServletResponse response) {
        final WrittenStreamCall streamCall = resource.getStreamCall();
        if (streamCall == null) {
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
                callbackStream(streamCall, createWrittenStreamOut(out), /*for logging*/resource);
                flushDownloadStream(out);
            } finally {
                closeDownloadStream(out);
            }
        } catch (RuntimeException e) {
            throw new ResponseDownloadFailureException("Failed to download the input stream: " + resource, e);
        } catch (IOException e) {
            handleDownloadIOException(resource, e);
        }
    }

    protected void callbackStream(WrittenStreamCall streamCall, WrittenStreamOut streamOut, ResponseDownloadResource resource)
            throws IOException {
        try {
            streamCall.callback(streamOut);
        } catch (AccessContextNotFoundException e) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Could not DB-update statement in stream callback.");
            br.addItem("Advice");
            br.addElement("You cannot DB-update in stream callback as default.");
            br.addElement("So call() inActionTransaction() if you need it.");
            br.addElement("(Or split process if you doesn't need it)");
            br.addElement("  (x):");
            br.addElement("    return asStream(\"sea.txt\").stream(out -> { // *Bad");
            br.addElement("        memberBhv.insert(...);");
            br.addElement("        out.write(ins);");
            br.addElement("    });");
            br.addElement("  (o):");
            br.addElement("    return asStream(\"sea.txt\").inActionTransaction().stream(out -> { // Good");
            br.addElement("        memberBhv.insert(...);");
            br.addElement("        out.write(ins);");
            br.addElement("    });");
            br.addElement("  (o):");
            br.addElement("    memberBhv.insert(...); // Good");
            br.addElement("    return asStream(\"sea.txt\").stream(out -> {");
            br.addElement("        out.write(ins);");
            br.addElement("    });");
            br.addElement("");
            br.addElement("However response is already committed in transaction");
            br.addElement("so e.g. you cannot add headers in hookFinally()");
            br.addElement("if you call inActionTransaction().");
            br.addElement("");
            br.addElement("While, the method is valid in (transactional) execute method.");
            br.addElement("(no meaning in non-transactional process e.g. hookBefore())");
            br.addItem("Download Info");
            br.addElement(resource);
            final String msg = br.buildExceptionMessage();
            throw new ResponseDownloadStreamCallUpdateException(msg, e);
        }
    }

    protected WrittenStreamOut createWrittenStreamOut(OutputStream out) {
        return new WrittenStreamOut() {
            public OutputStream stream() {
                return new WrittenOutputStreamWrapper(out); // delegator, you cannot close
            }

            public void write(InputStream ins) throws IOException {
                writeDownloadStream(ins, out);
            }
        };
    }

    // ===================================================================================
    //                                                                          Zip Stream
    //                                                                          ==========
    public void downloadZipStreamCall(ResponseDownloadResource resource, HttpServletResponse response) {
        try (ZipOutputStream zipOus = new ZipOutputStream(response.getOutputStream(), getZipOutputCharset(resource))) {
            final Map<String, WritternZipStreamWriter> zipWriterMap = createZipWriterMap(resource);
            zipWriterMap.forEach((fileName, writer) -> {
                try (ByteArrayOutputStream byteOus = new ByteArrayOutputStream()) {
                    writer.write(byteOus);
                    zipOus.putNextEntry(new ZipEntry(fileName));
                    byteOus.writeTo(zipOus);
                } catch (IOException e) {
                    handleDownloadIOException(resource, fileName, e);
                } finally {
                    try {
                        zipOus.closeEntry();
                    } catch (IOException e) {
                        handleDownloadIOException(resource, fileName, e);
                    }
                }
            });
        } catch (IOException e) {
            handleDownloadIOException(resource, e);
        }
    }

    protected Charset getZipOutputCharset(ResponseDownloadResource resource) {
        return Charset.forName(resource.getZipStreamCall().zipStreamEncoding());
    }

    protected Map<String, WritternZipStreamWriter> createZipWriterMap(ResponseDownloadResource resource) throws IOException {
        final WritternZipStreamCall zipStreamCall = resource.getZipStreamCall();
        final Map<String, WritternZipStreamWriter> writerMap = new LinkedHashMap<String, WritternZipStreamWriter>();
        zipStreamCall.callback((fileName, writer) -> {
            assertArgumentNotNull("fileName", fileName);
            assertArgumentNotNull("writer", writer);
            writerMap.put(fileName, writer);
        });
        if (writerMap.isEmpty()) {
            throw new IllegalStateException("The callback of zip stream should have at least one writer: " + zipStreamCall);
        }
        return writerMap;
    }

    // ===================================================================================
    //                                                                        Stream Logic
    //                                                                        ============
    protected void writeDownloadStream(InputStream ins, OutputStream out) throws IOException {
        try {
            fromInputStreamToOutputStream(ins, out);
        } catch (IOException e) {
            throwDownloadIOException(e);
        }
    }

    protected void fromInputStreamToOutputStream(InputStream ins, OutputStream out) throws IOException {
        final byte[] buf = new byte[8192];
        int n = 0;
        while ((n = ins.read(buf, 0, buf.length)) != -1) {
            out.write(buf, 0, n);
        }
    }

    protected void flushDownloadStream(OutputStream out) throws IOException {
        try {
            out.flush();
        } catch (IOException e) {
            throwDownloadIOException(e);
        }
    }

    protected void closeDownloadStream(OutputStream out) throws IOException {
        try {
            out.close();
        } catch (IOException e) {
            throwDownloadIOException(e);
        }
    }

    // ===================================================================================
    //                                                                   Throw IOException
    //                                                                   =================
    protected void throwDownloadIOException(IOException e) throws IOException {
        if (isClientAbortIOException(e)) { // will catched immediately so simple message
            throw new ResponseClientAbortIOException("Download was aborted by client.", e);
        } else {
            throw e;
        }
    }

    protected boolean isClientAbortIOException(IOException cause) {
        return isJettyClientAbort(cause) || isTomcatClientAbort(cause);
    }

    protected boolean isJettyClientAbort(IOException cause) {
        if ("org.eclipse.jetty.io.EofException".equals(cause.getClass().getName())) { // yelp
            final Throwable nestedEx = cause.getCause();
            if (nestedEx != null) {
                final String nestedMsg = nestedEx.getMessage();
                if (nestedMsg != null && nestedMsg.contains("Broken pipe")) { // just in case
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean isTomcatClientAbort(IOException cause) {
        return "org.apache.catalina.connector.ClientAbortException".equals(cause.getClass().getName());
    }

    // ===================================================================================
    //                                                                  Handle IOException
    //                                                                  ==================
    protected void handleDownloadIOException(ResponseDownloadResource resource, IOException e) {
        doHandleDownloadIOException(resource, null, e);
    }

    protected void handleDownloadIOException(ResponseDownloadResource resource, String nestedFile, IOException e) {
        doHandleDownloadIOException(resource, nestedFile, e);
    }

    protected void doHandleDownloadIOException(ResponseDownloadResource resource, String nestedFile, IOException e) {
        if (e instanceof ResponseClientAbortIOException) {
            handleClientAbortIOException(resource, (ResponseClientAbortIOException) e);
        } else {
            String msg = "Failed to download the file: " + (nestedFile != null ? nestedFile + " in " : "") + resource;
            throw new ResponseDownloadFailureException(msg, e);
        }
    }

    protected void handleClientAbortIOException(ResponseDownloadResource resource, ResponseClientAbortIOException cause) {
        if (logger.isDebugEnabled()) {
            logger.debug(buildClientAbortMessage(resource, cause));
        }
    }

    protected String buildClientAbortMessage(ResponseDownloadResource resource, ResponseClientAbortIOException cause) {
        final StringBuilder sb = new StringBuilder();
        sb.append("...Handling client abort of download (but continue):");
        sb.append("\n_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/");
        sb.append("\n[Client Abort Exception]");
        sb.append("\n download file : ").append(resource.getFileName());
        sb.append("\n content type  : ").append(resource.getContentType());
        sb.append("\n stream call   : ").append(resource.getStreamCall());
        sb.append("\n byte data     : ").append(resource.getByteData());
        sb.append("\n header map    : ").append(resource.getHeaderMap());
        buildClientAbortIOExceptionStackTrace(cause, sb, 0);
        sb.append("\n_/_/_/_/_/_/_/_/_/_/");
        return sb.toString();
    }

    protected void buildClientAbortIOExceptionStackTrace(Throwable cause, StringBuilder sb, int nestLevel) {
        sb.append(LF).append(nestLevel > 0 ? "Caused by: " : "");
        sb.append(cause.getClass().getName()).append(": ").append(cause.getMessage());
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
            buildClientAbortIOExceptionStackTrace(nested, sb, nestLevel + 1);
        }
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
