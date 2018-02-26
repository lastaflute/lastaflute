/*
 * Copyright 2015-2018 the original author or authors.
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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author jflute
 * @since 0.6.0 (2015/08/06 Friday)
 */
public class FilterHookServletAdapter implements FilterHook {

    protected final Filter servletFilter;

    public FilterHookServletAdapter(Filter servletFilter) {
        this.servletFilter = servletFilter;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        servletFilter.init(filterConfig);
    }

    @Override
    public void hook(HttpServletRequest request, HttpServletResponse response, FilterHookChain chain) throws IOException, ServletException {
        servletFilter.doFilter(request, response, createChain(chain));
    }

    protected FilterChain createChain(FilterHookChain chain) {
        return (request, response) -> chain.doNext((HttpServletRequest) request, (HttpServletResponse) response);
    }

    @Override
    public void destroy() {
        servletFilter.destroy();
    }
}
