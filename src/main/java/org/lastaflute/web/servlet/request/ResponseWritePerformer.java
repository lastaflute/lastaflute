/*
 * Copyright 2015-2020 the original author or authors.
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
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.dbflute.util.Srl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class ResponseWritePerformer {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(ResponseWritePerformer.class);

    // ===================================================================================
    //                                                                           Byte Data
    //                                                                           =========
    public void write(HttpServletResponse response, String text, String contentType, String encoding) {
        assertArgumentNotNull("response", response);
        assertArgumentNotNull("text", text);
        assertArgumentNotNull("contentType", contentType);
        assertArgumentNotNull("encoding", encoding);
        final String contentTypeWithCharset = buildContentTypeWithCharset(contentType, encoding);
        showWritingResponse(text, contentTypeWithCharset);
        response.setContentType(contentTypeWithCharset);
        try {
            PrintWriter out = null;
            try {
                out = createPrintWriter(response, encoding);
                printPrintWriter(out, text);
            } finally {
                if (out != null) {
                    closePrintWriter(out);
                }
            }
        } catch (IOException e) {
            String msg = "Failed to write the text: contentType=" + contentType + ", encoding=" + encoding + ", text=" + text;
            throw new IllegalStateException(msg, e);
        }
    }

    protected String buildContentTypeWithCharset(String contentType, String encoding) {
        return contentType + "; charset=" + encoding;
    }

    protected void showWritingResponse(String value, String contentType) {
        if (logger.isDebugEnabled()) {
            // to suppress noisy big data (no need all data for debug: also you can see it by response)
            final String exp = buildResponseDebugDisplay(value);
            logger.debug("#flow ...Writing response as {}: \n{}", contentType, exp);
        }
    }

    protected String buildResponseDebugDisplay(String value) {
        return Srl.cut(value, getResponseDebugDisplayLimit(), "..."); // you can basically confirm it at front side so cut it here
    }

    protected int getResponseDebugDisplayLimit() {
        return 500;
    }

    // -----------------------------------------------------
    //                                       Writer Handling
    //                                       ---------------
    protected PrintWriter createPrintWriter(HttpServletResponse response, String encoding) throws IOException {
        return newPrintWriter(createOutputStreamWriter(response, encoding));
    }

    protected PrintWriter newPrintWriter(OutputStreamWriter outputStreamWriter) {
        return new PrintWriter(outputStreamWriter);
    }

    protected OutputStreamWriter createOutputStreamWriter(HttpServletResponse response, String encoding) throws IOException {
        return newOutputStreamWriter(response.getOutputStream(), encoding);
    }

    protected OutputStreamWriter newOutputStreamWriter(ServletOutputStream out, String encoding) throws UnsupportedEncodingException {
        return new OutputStreamWriter(out, encoding);
    }

    protected void printPrintWriter(PrintWriter out, String text) {
        out.print(text);
    }

    protected void closePrintWriter(PrintWriter out) {
        out.close();
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
