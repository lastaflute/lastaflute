/*
 * Copyright 2015-2018 the original author or authors.
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
package org.lastaflute.web.ruts.process.validatebean;

import java.util.function.Consumer;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.web.ruts.process.exception.ResponseBeanValidationErrorException;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author jflute
 * @since 0.8.5 (2016/11/14 Monday at showbase)
 */
public class ResponseSimpleBeanValidator extends ResponseBeanValidator {

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ResponseSimpleBeanValidator(RequestManager requestManager, Object actionExp, boolean warning) {
        super(requestManager, actionExp, warning);
    }

    // ===================================================================================
    //                                                                            Validate
    //                                                                            ========
    /**
     * @param bean The bean to be validated. (NotNull)
     * @throws ResponseBeanValidationErrorException When the validation error.
     */
    public void validate(Object bean) {
        doValidate(bean, br -> {});
    }

    @Override
    protected OptionalThing<Class<?>[]> getValidatorGroups() {
        return OptionalThing.empty();
    }

    @Override
    protected String buildValidationErrorMessage(Object bean, Consumer<ExceptionMessageBuilder> locationBuilder, UserMessages messages) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Validation error for the bean.");
        br.addItem("Advice");
        br.addElement("Make sure your bean property values.");
        br.addElement("For example:");
        br.addElement("  public class SeaBean {");
        br.addElement("      @Required");
        br.addElement("      public String dockside;");
        br.addElement("  }");
        br.addElement("  (x):");
        br.addElement("    public class SeaAction {");
        br.addElement("        @Execute");
        br.addElement("        public JsonResponse<SeaBean> index() {");
        br.addElement("            SeaBean bean = new SeaBean();");
        br.addElement("            return asJson(bean); // *Bad");
        br.addElement("        }");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public class SeaAction {");
        br.addElement("        @Execute");
        br.addElement("        public JsonResponse<SeaBean> index() {");
        br.addElement("            SeaBean bean = new SeaBean();");
        br.addElement("            bean.dockside = \"overthewaves\"; // Good");
        br.addElement("            return asJson(bean);");
        br.addElement("        }");
        br.addElement("    }");
        br.addItem("Action");
        br.addElement(actionExp);
        locationBuilder.accept(br); // basically do nothing when JSON
        setupItemValidatedBean(br, bean);
        setupItemMessages(br, messages);
        return br.buildExceptionMessage();
    }
}
