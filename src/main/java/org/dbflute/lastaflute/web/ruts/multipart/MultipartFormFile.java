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
package org.dbflute.lastaflute.web.ruts.multipart;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author modified by jflute (originated in Struts)
 */
public interface MultipartFormFile {

    String getContentType();

    void setContentType(String contentType);

    int getFileSize();

    void setFileSize(int fileSize);

    String getFileName();

    void setFileName(String fileName);

    byte[] getFileData() throws FileNotFoundException, IOException;

    InputStream getInputStream() throws FileNotFoundException, IOException;

    void destroy();
}
