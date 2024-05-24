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
package org.lastaflute.web.ruts.renderer;

import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.ruts.NextJourney;
import org.lastaflute.web.ruts.process.ActionRuntime;

/**
 * @author jflute
 * @since 0.6.4 (2015/10/01 Thursday)
 */
public interface HtmlRenderingProvider {

    HtmlRenderer DEFAULT_RENDERER = new JspHtmlRenderer();
    HtmlRenderingProvider DEFAULT_RENDERING_PROVIDER = new JspHtmlRenderingProvider();

    /**
     * @param runtime The runtime of current requested action. (NotNull)
     * @param journey The journey to next stage. (NotNull)
     * @return The renderer to render HTML. (NotNull)
     */
    HtmlRenderer provideRenderer(ActionRuntime runtime, NextJourney journey);

    /**
     * @param runtime The runtime of current requested action. (NotNull)
     * @return The response for show errors HTML. (NotNull, ShouldBeDefined)
     */
    HtmlResponse provideShowErrorsResponse(ActionRuntime runtime);
}
