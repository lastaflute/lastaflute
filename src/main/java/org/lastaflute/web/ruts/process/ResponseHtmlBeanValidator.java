/*
 * Copyright 2014-2015 the original author or authors.
 * Copyright 2014-2015 the original author or authors.
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
package org.lastaflute.web.ruts.process;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.ruts.message.ActionMessages;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author jflute
 * @since 0.7.6 (2016/01/04 Monday)
 */
public class ResponseHtmlBeanValidator extends ResponseBeanValidator {

    protected final HtmlResponse response;

    public ResponseHtmlBeanValidator(RequestManager requestManager, Object actionExp, boolean warning, HtmlResponse response) {
        super(requestManager, actionExp, warning);
        this.response = response;
    }

    @Override
    protected OptionalThing<Class<?>[]> getValidatorGroups() {
        return response.getValidatorGroups();
    }

    @Override
    protected String buildValidationErrorMessage(Object jsonBean, ActionMessages messages) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Validation error for the HTML response.");
        br.addItem("Advice");
        br.addElement("Make sure your HTML bean property values.");
        br.addElement("For example:");
        br.addElement("  public class SeaBean {");
        br.addElement("      @Required");
        br.addElement("      public String dockside;");
        br.addElement("  }");
        br.addElement("  (x):");
        br.addElement("    public class SeaAction {");
        br.addElement("        @Execute");
        br.addElement("        public HtmlResponse index() {");
        br.addElement("            SeaBean bean = new SeaBean();");
        br.addElement("            return asHtml(bean).renderWith(data -> { // *Bad");
        br.addElement("                data.register(\"bean\", bean);");
        br.addElement("            });");
        br.addElement("        }");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public class SeaAction {");
        br.addElement("        @Execute");
        br.addElement("        public HtmlResponse index() {");
        br.addElement("            SeaBean bean = new SeaBean();");
        br.addElement("            bean.dockside = \"overthewaves\"; // Good");
        br.addElement("            return asHtml(bean).renderWith(data -> {");
        br.addElement("                data.register(\"bean\", bean);");
        br.addElement("            });");
        br.addElement("        }");
        br.addElement("    }");
        br.addItem("Action");
        br.addElement(actionExp);
        setupItemValidatedBean(br, jsonBean);
        setupItemMessages(messages, br);
        return br.buildExceptionMessage();
    }

    public HtmlResponse getActionResponse() {
        return response;
    }
}
