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
package org.lastaflute.web.servlet.filter.hook;

import java.io.IOException;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author jflute
 * @since 0.6.2 (2015/09/18 Friday)
 */
public class FilterHookSimply implements FilterHook {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void hook(HttpServletRequest request, HttpServletResponse response, FilterHookChain chain) throws IOException, ServletException {
    }

    @Override
    public void destroy() {
    }
}
