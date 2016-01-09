/*
 * Copyright 2015-2016 the original author or authors.
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
package org.lastaflute.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.lastaflute.web.servlet.filter.LastaPrepareFilter;
import org.lastaflute.web.servlet.filter.LastaShowbaseFilter;
import org.lastaflute.web.servlet.filter.LastaToActionFilter;

/**
 * @author jflute
 */
public class LastaFilter implements Filter {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected LastaPrepareFilter lastaPrepareFilter;
    protected LastaShowbaseFilter lastaShowbaseFilter;
    protected LastaToActionFilter lastaToActionFilter;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LastaFilter() {
    }

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    public void init(FilterConfig filterConfig) throws ServletException {
        initElementFilter(filterConfig);
    }

    protected void initElementFilter(FilterConfig filterConfig) throws ServletException {
        lastaPrepareFilter = createLastaPrepareFilter();
        lastaPrepareFilter.init(filterConfig);
        lastaShowbaseFilter = createLastaShowbaseFilter();
        lastaShowbaseFilter.init(filterConfig);
        lastaToActionFilter = createLastaToActionFilter();
        lastaToActionFilter.init(filterConfig);
    }

    protected LastaPrepareFilter createLastaPrepareFilter() {
        return new LastaPrepareFilter();
    }

    protected LastaShowbaseFilter createLastaShowbaseFilter() {
        return new LastaShowbaseFilter();
    }

    protected LastaToActionFilter createLastaToActionFilter() {
        return new LastaToActionFilter();
    }

    // ===================================================================================
    //                                                                          doFilter()
    //                                                                          ==========
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        lastaPrepareFilter.doFilter(request, response, (nextreq, nextres) -> {
            lastaShowbaseFilter.doFilter(nextreq, nextres, (nexnextreq, nexnextres) -> {
                lastaToActionFilter.doFilter(nexnextreq, nexnextres, chain); // #to_action
            });
        });
    }

    // ===================================================================================
    //                                                                             Destroy
    //                                                                             =======
    @Override
    public void destroy() {
        if (lastaToActionFilter != null) {
            lastaToActionFilter.destroy();
        }
        if (lastaShowbaseFilter != null) {
            lastaShowbaseFilter.destroy();
        }
        if (lastaPrepareFilter != null) {
            lastaPrepareFilter.destroy(); // Lasta Di is destroyed
        }
    }
}