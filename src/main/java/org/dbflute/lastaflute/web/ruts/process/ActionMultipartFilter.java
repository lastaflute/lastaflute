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
package org.dbflute.lastaflute.web.ruts.process;

import javax.servlet.http.HttpServletRequest;

import org.dbflute.lastaflute.web.ruts.multipart.MultipartRequestWrapper;

/**
 * @author modified by jflute (originated in Seasar and Struts)
 */
public class ActionMultipartFilter {

    public static final String MULTIPART_FORM_DATA_TYPE = "multipart/form-data";

    public HttpServletRequest filterRequestAsMultipartIfNeeds(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return request;
        }
        final String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith(MULTIPART_FORM_DATA_TYPE)) {
            return new MultipartRequestWrapper(request);
        } else {
            return request;
        }
    }
}
