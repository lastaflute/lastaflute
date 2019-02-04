/*
 * Copyright 2015-2019 the original author or authors.
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
package org.lastaflute.web.ruts.process.validatebean;

import java.util.function.Consumer;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.ruts.process.exception.ResponseBeanValidationErrorException;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author jflute
 * @since 0.7.6 (2016/01/04 Monday)
 */
public class ResponseHtmlBeanValidator extends ResponseBeanValidator {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final HtmlResponse response;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ResponseHtmlBeanValidator(RequestManager requestManager, Object actionExp, boolean warning, HtmlResponse response) {
        super(requestManager, actionExp, warning);
        this.response = response;
    }

    // ===================================================================================
    //                                                                            Validate
    //                                                                            ========
    /**
     * @param htmlBean The HTML bean to be validated. (NotNull)
     * @param dataKey The data key for rendering. (NotNull)
     * @throws ResponseBeanValidationErrorException When the validation error.
     */
    public void validate(String dataKey, Object htmlBean) {
        doValidate(htmlBean, br -> {
            br.addItem("Data Key");
            br.addElement(dataKey);
        });
    }

    @Override
    protected OptionalThing<Class<?>[]> getValidatorGroups() {
        return response.getValidatorGroups();
    }

    @Override
    protected String buildValidationErrorMessage(Object bean, Consumer<ExceptionMessageBuilder> locationBuilder, UserMessages messages) {
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
        locationBuilder.accept(br);
        setupItemValidatedBean(br, bean);
        setupItemMessages(br, messages);
        return br.buildExceptionMessage();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public HtmlResponse getActionResponse() {
        return response;
    }
}
