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
package org.lastaflute.web.container;

import org.lastaflute.di.core.factory.SingletonLaContainerFactory;
import org.lastaflute.di.helper.timer.TimeoutManager;
import org.lastaflute.di.util.LdiDriverManagerUtil;
import org.lastaflute.jta.helper.timer.LjtTimeoutManager;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class WebLastaContainerDestroyer {

    public static void destroy() {
        SingletonLaContainerFactory.destroy();
        LdiDriverManagerUtil.deregisterAllDrivers();
        try {
            TimeoutManager.getInstance().stop(1000); // lasta_di's
        } catch (Throwable ignored) {}
        try {
            LjtTimeoutManager.getInstance().stop(1000); // lasta_jta's
        } catch (Throwable ignored) {}
    }
}
