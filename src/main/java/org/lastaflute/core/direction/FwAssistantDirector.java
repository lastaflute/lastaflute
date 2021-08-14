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
package org.lastaflute.core.direction;

import org.lastaflute.db.direction.FwDbDirection;
import org.lastaflute.web.direction.FwWebDirection;

/**
 * The assistant director (AD) for LastaFlute. <br>
 * This is extension point of LastaFlute for applications. <br>
 * You (applications) can change framework behaviors through this AD. <br>
 * You should make lastaflute_assist++.xml in your resources to use this as DI component like this:
 * <pre> e.g. if application name is Mythica
 * &lt;components&gt;
 *     &lt;component name="assistantDirector" class="...mylasta.direction.MythicaFwAssistantDirector"/&gt;
 *     &lt;component name="mythicaConfig" class="...mylasta.MythicaConfig$SimpleImpl"/&gt;
 * &lt;/components&gt;
 * </pre>
 * And the word 'Direction' means option that changes SAFlute behaviors. <br>
 * You should set directions at your implementation class of this interface.
 * @author jflute
 */
public interface FwAssistantDirector {

    /**
     * Assist the direction of basic assist. <br>
     * The direction provides configuration settings, e.g. file path. <br>
     * (Applications should have configuration by properties files for framework)
     * @return The direction instance for basic assist. (NotNull)
     */
    FwAssistDirection assistAssistDirection();

    /**
     * @return The direction instance for core assist. (NotNull)
     */
    FwCoreDirection assistCoreDirection();

    /**
     * @return The direction instance for DB assist. (NotNull)
     */
    FwDbDirection assistDbDirection();

    /**
     * @return The direction instance for web assist. (NotNull)
     */
    FwWebDirection assistWebDirection();
}
