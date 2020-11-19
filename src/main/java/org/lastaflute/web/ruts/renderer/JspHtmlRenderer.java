/*
 * Copyright 2015-2020 the original author or authors.
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

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.di.helper.beans.PropertyDesc;
import org.lastaflute.web.exception.RequestForwardFailureException;
import org.lastaflute.web.ruts.NextJourney;
import org.lastaflute.web.ruts.config.ActionFormMeta;
import org.lastaflute.web.ruts.config.ActionFormProperty;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author jflute
 * @since 0.6.4 (2015/10/01 Thursday)
 */
public class JspHtmlRenderer implements HtmlRenderer {

    @Override
    public void render(RequestManager requestManager, ActionRuntime runtime, NextJourney journey) throws IOException, ServletException {
        handleHtmlTemplate(requestManager, runtime, journey);
    }

    protected void handleHtmlTemplate(RequestManager requestManager, ActionRuntime runtime, NextJourney journey)
            throws IOException, ServletException {
        exportFormPropertyToRequest(requestManager, runtime); // for e.g. EL expression in JSP
        doForward(requestManager, runtime, journey);
    }

    protected void exportFormPropertyToRequest(RequestManager requestManager, ActionRuntime runtime) {
        runtime.getActionForm().ifPresent(virtualForm -> { // also contains pushed
            final ActionFormMeta meta = virtualForm.getFormMeta();
            final Collection<ActionFormProperty> properties = meta.properties();
            if (properties.isEmpty()) {
                return;
            }
            for (ActionFormProperty property : properties) {
                if (isExportableProperty(property.getPropertyDesc())) {
                    final Object propertyValue = virtualForm.getPropertyValue(property);
                    if (propertyValue != null) {
                        requestManager.setAttribute(property.getPropertyName(), propertyValue);
                    }
                }
            }
        });
    }

    protected boolean isExportableProperty(PropertyDesc pd) {
        return !pd.getPropertyType().getName().startsWith("javax.servlet");
    }

    protected void doForward(RequestManager requestManager, ActionRuntime runtime, NextJourney journey)
            throws IOException, ServletException {
        try {
            requestManager.getResponseManager().forward(journey);
        } catch (RuntimeException | IOException | ServletException e) { // because of e.g. compile error may be poor
            throwRequestForwardFailureException(runtime, journey, e);
        }
    }

    protected void throwRequestForwardFailureException(ActionRuntime runtime, NextJourney journey, Exception e) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Failed to forward the request to the path.");
        br.addItem("Advice");
        br.addElement("Read the nested exception message.");
        br.addItem("Action Runtime");
        br.addElement(runtime);
        br.addItem("Forward Journey");
        br.addElement(journey);
        final String msg = br.buildExceptionMessage();
        throw new RequestForwardFailureException(msg, e);
    }
}
