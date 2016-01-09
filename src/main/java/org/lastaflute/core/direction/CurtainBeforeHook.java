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
package org.lastaflute.core.direction;

/**
 * The hook (callback) for curtain before the application performance. <br>
 * You can add your own process when your application is booting.
 * @author jflute
 */
public interface CurtainBeforeHook {

    /**
     * Hook your process when curtain before the application performance. <br>
     * You can know whether the current environment is development or not by the assistant director.
     * @param assistantDirector The assistant director for the framework. (NotNull)
     */
    void hook(FwAssistantDirector assistantDirector);
}
