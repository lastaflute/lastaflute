/*
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
package org.lastaflute.web.validation;

import org.lastaflute.web.ruts.message.ActionMessages;

/**
 * @param <MESSAGES> The type of action messages.
 * @author jflute
 */
public interface LaValidatable<MESSAGES extends ActionMessages> {

    /**
     * Validate the form values. <br>
     * <pre>
     * <span style="color: #3F7E5E">// by-annotation only</span>
     * <span style="color: #CC4747">validate</span>(<span style="color: #553000">form</span>, <span style="color: #553000">messages</span> <span style="font-size: 120%">-</span>&gt;</span> {}, () <span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">return</span> asHtml(<span style="color: #0000C0">path_Sea_SeaListJsp</span>);
     * });
     * 
     * <span style="color: #3F7E5E">// by-annotation and by-program</span>
     * <span style="color: #CC4747">validate</span>(<span style="color: #553000">form</span>, <span style="color: #553000">messages</span> <span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">if</span> (...) {
     *         <span style="color: #553000">messages</span>.addConstraint...
     *     }
     * }, () <span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">return</span> asHtml(<span style="color: #0000C0">path_Sea_SeaListJsp</span>);
     * });
     * </pre>
     * @param form The form that has request parameters. (NotNull)
     * @param moreValidationLambda The callback for more validation, e.g. correlation rule. (NotNull)
     * @param validationErrorLambda The callback for response when validation error. (NotNull)
     * @return The success information of validation, basically for success attribute. (NotNull)
     */
    default ValidationSuccess validate(Object form, VaMore<MESSAGES> moreValidationLambda, VaErrorHook validationErrorLambda) {
        return createValidator().validate(form, moreValidationLambda, validationErrorLambda);
    }

    /**
     * Validate the form values for API, when e.g. JsonResponse. <br>
     * The validation error handling is in ApiFailureHook.
     * <pre>
     * <span style="color: #3F7E5E">// by-annotation only</span>
     * <span style="color: #CC4747">validate</span>(<span style="color: #553000">form</span>, <span style="color: #553000">messages</span> <span style="font-size: 120%">-</span>&gt;</span> {});
     * 
     * <span style="color: #3F7E5E">// by-annotation and by-program</span>
     * <span style="color: #CC4747">validate</span>(<span style="color: #553000">form</span>, <span style="color: #553000">messages</span> <span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">if</span> (...) {
     *         <span style="color: #553000">messages</span>.addConstraint...
     *     }
     * });
     * </pre>
     * @param form The form that has request parameters. (NotNull)
     * @param moreValidationLambda The callback for more validation, e.g. correlation rule, very complex rule. (NotNull)
     * @return The success information of validation, basically for success attribute. (NotNull)
     */
    default ValidationSuccess validateApi(Object form, VaMore<MESSAGES> moreValidationLambda) {
        return createValidator().validateApi(form, moreValidationLambda);
    }

    default void throwValidationError(VaMessenger<MESSAGES> validationMessagesLambda, VaErrorHook validationErrorLambda) {
        createValidator().throwValidationError(() -> {
            final MESSAGES messages = createMessages();
            validationMessagesLambda.message(messages);
            return messages;
        } , validationErrorLambda);
    }

    default void throwValidationErrorApi(VaMessenger<MESSAGES> validationMessagesLambda) {
        createValidator().throwValidationErrorApi(() -> {
            final MESSAGES messages = createMessages();
            validationMessagesLambda.message(messages);
            return messages;
        });
    }

    ActionValidator<MESSAGES> createValidator();

    MESSAGES createMessages();
}
