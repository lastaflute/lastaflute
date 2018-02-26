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
package org.lastaflute.web.servlet.external;

import org.lastaflute.di.core.LaContainer;
import org.lastaflute.di.core.external.ApplicationMapComponentDef;
import org.lastaflute.di.core.external.ExternalContextComponentDefRegister;
import org.lastaflute.di.core.external.InitParameterMapComponentDef;
import org.lastaflute.di.core.external.RequestHeaderMapComponentDef;
import org.lastaflute.di.core.external.RequestHeaderValuesMapComponentDef;
import org.lastaflute.di.core.external.RequestMapComponentDef;
import org.lastaflute.di.core.external.RequestParameterMapComponentDef;
import org.lastaflute.di.core.external.RequestParameterValuesMapComponentDef;
import org.lastaflute.di.core.external.SessionMapComponentDef;
import org.lastaflute.di.core.meta.impl.LaContainerImpl;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class HttpServletExternalContextComponentDefRegister implements ExternalContextComponentDefRegister {

    public void registerComponentDefs(LaContainer container) {
        LaContainerImpl impl = (LaContainerImpl) container;
        impl.register0(new HttpServletRequestComponentDef());
        impl.register0(new HttpServletResponseComponentDef());
        impl.register0(new HttpSessionComponentDef());
        impl.register0(new ServletContextComponentDef());
        impl.register0(new ApplicationMapComponentDef());
        impl.register0(new InitParameterMapComponentDef());
        impl.register0(new SessionMapComponentDef());
        impl.register0(new RequestMapComponentDef());
        impl.register0(new RequestHeaderMapComponentDef());
        impl.register0(new RequestHeaderValuesMapComponentDef());
        impl.register0(new RequestParameterMapComponentDef());
        impl.register0(new RequestParameterValuesMapComponentDef());
    }
}
