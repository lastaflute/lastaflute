/*
 * Copyright 2015-2019 the original author or authors.
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
package org.lastaflute.core.template;

import java.util.Map;

/**
 * The manager of template.
 * @author jflute
 * @since 0.6.0 (2015/05/23 Saturday)
 */
public interface TemplateManager {

    /**
     * Parse the template by parameter bean.
     * @param pmb The parameter bean for template, that can provide template path. (NotNull)
     * @return The parsed text. (NotNull: if not found, throws exception)
     */
    String parse(TemplatePmb pmb);

    /**
     * Parse the template file.
     * @param templatePath The path of template as basically classpath, can be changed by AD. (NotNull)
     * @param variableMap The variable data for template as map. (NotNull)
     * @return The parsed text. (NotNull: if not found, throws exception)
     */
    String parse(String templatePath, Map<String, Object> variableMap);
}
