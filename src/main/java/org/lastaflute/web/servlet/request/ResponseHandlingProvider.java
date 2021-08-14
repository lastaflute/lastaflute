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

import java.util.Map;
import java.util.function.Supplier;

/**
 * The provider of response handling.
 * @author jflute
 */
public interface ResponseHandlingProvider {

    /**
     * @return The creator of performer for writing response e.g. JSON response. (NullAllowed)
     */
    default Supplier<ResponseWritePerformer> provideResponseWritePerformerCreator() {
        return null; // as default
    }

    /**
     * @return The creator of performer for downloading response e.g. stream response. (NullAllowed)
     */
    default Supplier<ResponseDownloadPerformer> provideResponseDownloadPerformerCreator() {
        return null; // as default
    }

    /**
     * @return The map of content type for extensions. (NullAllowed)
     */
    default Map<String, String> provideDownloadExtensionContentTypeMap() {
        return null; // as default
    }
}
