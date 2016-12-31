/*
 * Copyright 2015-2017 the original author or authors.
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
package org.lastaflute.web.ruts.renderer;

import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.ruts.NextJourney;
import org.lastaflute.web.ruts.process.ActionRuntime;

/**
 * @author jflute
 * @since 0.6.7 (2015/11/22 Sunday)
 */
public class JspHtmlRenderingProvider implements HtmlRenderingProvider {

    @Override
    public HtmlRenderer provideRenderer(ActionRuntime runtime, NextJourney journey) {
        return DEFAULT_RENDERER;
    }

    @Override
    public HtmlResponse provideShowErrorsResponse(ActionRuntime runtime) {
        return HtmlResponse.fromForwardPath(getShowErrorsForwardPath(runtime));
    }

    protected String getShowErrorsForwardPath(ActionRuntime runtime) {
        return "/error/show_errors.jsp";
    }
}
