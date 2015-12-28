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

import java.util.Iterator;
import java.util.Set;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.PropertyDesc;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.ruts.message.ActionMessage;
import org.lastaflute.web.ruts.message.ActionMessages;
import org.lastaflute.web.ruts.process.exception.ResponseJsonBeanValidationErrorException;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.validation.ActionValidator;
import org.lastaflute.web.validation.exception.ClientErrorByValidatorException;
import org.lastaflute.web.validation.exception.ValidationErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 * @since 0.7.1 (2015/12/14 Monday)
 */
public class JsonBeanValidator {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(JsonBeanValidator.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RequestManager requestManager;
    protected final Object actionExp; // used as only exception message, Object for unit test
    protected final JsonResponse<?> response;
    protected final boolean warning;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public JsonBeanValidator(RequestManager requestManager, Object actionExp, JsonResponse<?> response, boolean warning) {
        this.requestManager = requestManager;
        this.response = response;
        this.actionExp = actionExp;
        this.warning = warning;
    }

    // ===================================================================================
    //                                                                            Validate
    //                                                                            ========
    /**
     * @param jsonBean The bean of JSON. (NotNull)
     * @throws ResponseJsonBeanValidationErrorException When the validation error.
     */
    public void validate(Object jsonBean) {
        if (jsonBean == null) {
            throw new IllegalStateException("The argument 'jsonBean' should not be null.");
        }
        final ActionValidator<ActionMessages> validator = createActionValidator();
        try {
            executeValidator(validator, jsonBean);
        } catch (ValidationErrorException e) {
            handleResponseJsonBeanValidationErrorException(jsonBean, e.getMessages(), e);
        } catch (ClientErrorByValidatorException e) {
            handleResponseJsonBeanValidationErrorException(jsonBean, e.getMessages(), e);
        }
    }

    protected ActionValidator<ActionMessages> createActionValidator() {
        return new ActionValidator<>(requestManager, () -> {
            return new ActionMessages();
        } , response.getValidatorGroups().orElse(ActionValidator.DEFAULT_GROUPS));
    }

    protected void executeValidator(ActionValidator<ActionMessages> validator, Object jsonBean) {
        validator.validate(jsonBean, more -> {} , () -> {
            throw new IllegalStateException("unused here, no way");
        });
    }

    protected void handleResponseJsonBeanValidationErrorException(Object jsonBean, ActionMessages messages, RuntimeException cause) {
        // cause is completely framework info so not show it
        final String msg = buildJsonBeanValidationErrorMessage(jsonBean, messages);
        if (warning) {
            logger.warn(msg);
        } else {
            throw new ResponseJsonBeanValidationErrorException(msg);
        }
    }

    protected String buildJsonBeanValidationErrorMessage(Object jsonBean, ActionMessages messages) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Validation error for the JSON response.");
        br.addItem("Advice");
        br.addElement("Make sure your JSON bean property values.");
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
        br.addElement("            reurn asJson(bean); // *Bad");
        br.addElement("        }");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public class SeaAction {");
        br.addElement("        @Execute");
        br.addElement("        public JsonResponse<SeaBean> index() {");
        br.addElement("            SeaBean bean = new SeaBean();");
        br.addElement("            bean.dockside = \"overthewaves\"; // Good");
        br.addElement("            reurn asJson(bean);");
        br.addElement("        }");
        br.addElement("    }");
        br.addItem("Action");
        br.addElement(actionExp);
        br.addItem("JSON Bean");
        br.addElement(jsonBean.getClass());
        final String jsonExp = jsonBean.toString();
        br.addElement(jsonExp);
        if (jsonExp == null || !jsonExp.contains("\n")) {
            br.addItem("Bean Property");
            try {
                final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(jsonBean.getClass());
                final int propertyDescSize = beanDesc.getPropertyDescSize();
                for (int i = 0; i < propertyDescSize; i++) {
                    final PropertyDesc pd = beanDesc.getPropertyDesc(i);
                    br.addElement(pd.getPropertyName() + ": " + pd.getValue(jsonBean));
                }
            } catch (RuntimeException ignored) {
                br.addElement("*Failed to get field values by BeanDesc");
            }
        }
        br.addItem("Messages");
        final Set<String> propertySet = messages.toPropertySet();
        for (String property : propertySet) {
            br.addElement(property);
            for (Iterator<ActionMessage> ite = messages.accessByIteratorOf(property); ite.hasNext();) {
                br.addElement("  " + ite.next());
            }
        }
        return br.buildExceptionMessage();
    }
}
