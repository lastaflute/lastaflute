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
package org.lastaflute.web.servlet.request.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author jflute
 */
public interface WrittenStreamOut {

    /**
     * @return The output stream of response, not committed yet. (NotNull)
     */
    OutputStream stream();

    /**
     * @param ins The input stream provided by application. (NotNull)
     * @throws IOException When writing stream failed.
     */
    void write(InputStream ins) throws IOException;
}
