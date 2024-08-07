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

/**
 * The redirect-able response type e.g. HtmlResponse.
 * @author jflute
 */
public interface Redirectable {

    /**
     * Get the routing path for redirect.
     * @return The string path. (NotNull)
     */
    String getRoutingPath();

    /**
     * Does it redirect the path as is?
     * @return The determination, true or false.
     */
    boolean isAsIs();
}
