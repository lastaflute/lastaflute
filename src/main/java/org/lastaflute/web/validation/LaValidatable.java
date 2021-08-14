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
package org.lastaflute.web.validation;

import org.lastaflute.core.message.UserMessages;

/**
 * @param <MESSAGES> The type of user messages.
 * @author jflute
 */
public interface LaValidatable<MESSAGES extends UserMessages> {

    // ===================================================================================
    //                                                                            Validate
    //                                                                            ========
    /**
     * Validate the form values. <br>
     * <pre>
     * <span style="color: #3F7E5E">// by-annotation only</span>
     * <span style="color: #CC4747">validate</span>(<span style="color: #553000">form</span>, <span style="color: #553000">messages</span> <span style="font-size: 120%">-</span>&gt;</span> {}, () <span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">return</span> asHtml(<span style="color: #0000C0">path_Sea_SeaListHtml</span>);
     * });
     * 
     * <span style="color: #3F7E5E">// by-annotation and by-program</span>
     * <span style="color: #CC4747">validate</span>(<span style="color: #553000">form</span>, <span style="color: #553000">messages</span> <span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">if</span> (...) {
     *         <span style="color: #553000">messages</span>.addConstraint...
     *     }
     * }, () <span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">return</span> asHtml(<span style="color: #0000C0">path_Sea_SeaListHtml</span>);
     * });
     * 
     * <span style="color: #3F7E5E">// recycle data from validation process</span>
     * ValidationSuccess <span style="color: #553000">success</span> = validate(<span style="color: #553000">form</span>, <span style="color: #553000">messages</span> <span style="font-size: 120%">-</span>&gt;</span> {
     *     Sea <span style="color: #553000">sea</span> = ... <span style="color: #3F7E5E">// something from validation process</span>
     *     <span style="color: #553000">messages</span>.<span style="color: #CC4747">saveSuccessAttribute</span>(<span style="color: #0000C0">"sea"</span>, <span style="color: #553000">sea</span>);
     * }, () <span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">return</span> asHtml(<span style="color: #0000C0">path_Sea_SeaListHtml</span>);
     * });
     * Sea <span style="color: #553000">sea</span> = <span style="color: #553000">success</span>.<span style="color: #994747">getAttribute</span>(<span style="color: #0000C0">"sea"</span>, Sea.<span style="color: #70226C">class</span>);
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
     * <span style="color: #CC4747">validateApi</span>(<span style="color: #553000">body</span>, <span style="color: #553000">messages</span> <span style="font-size: 120%">-</span>&gt;</span> {});
     * 
     * <span style="color: #3F7E5E">// by-annotation and by-program</span>
     * <span style="color: #CC4747">validateApi</span>(<span style="color: #553000">body</span>, <span style="color: #553000">messages</span> <span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">if</span> (...) {
     *         <span style="color: #553000">messages</span>.addConstraint...
     *     }
     * });
     * 
     * <span style="color: #3F7E5E">// recycle data from validation process</span>
     * ValidationSuccess <span style="color: #553000">success</span> = validateApi(<span style="color: #553000">form</span>, <span style="color: #553000">messages</span> <span style="font-size: 120%">-</span>&gt;</span> {
     *     Sea <span style="color: #553000">sea</span> = ... <span style="color: #3F7E5E">// something from validation process</span>
     *     <span style="color: #553000">messages</span>.<span style="color: #CC4747">saveSuccessAttribute</span>(<span style="color: #0000C0">"sea"</span>, <span style="color: #553000">sea</span>);
     * });
     * Sea <span style="color: #553000">sea</span> = <span style="color: #553000">success</span>.<span style="color: #994747">getAttribute</span>(<span style="color: #0000C0">"sea"</span>, Sea.<span style="color: #70226C">class</span>);
     * </pre>
     * @param body The form or body that has request parameters. (NotNull)
     * @param moreValidationLambda The callback for more validation, e.g. correlation rule, very complex rule. (NotNull)
     * @return The success information of validation, basically for success attribute. (NotNull)
     */
    default ValidationSuccess validateApi(Object body, VaMore<MESSAGES> moreValidationLambda) {
        return createValidator().validateApi(body, moreValidationLambda);
    }

    // ===================================================================================
    //                                                                         Throw Error
    //                                                                         ===========
    /**
     * Throw validation error exception immediately. <br>
     * <pre>
     * <span style="color: #70226C">if</span> (...) { <span style="color: #3F7E5E">// any invalid state</span> 
     *     <span style="color: #CC4747">throwValidationError</span>(<span style="color: #553000">messages</span> <span style="font-size: 120%">-</span>&gt;</span> <span style="color: #553000">messages</span>.addConstraints..., () <span style="font-size: 120%">-</span>&gt;</span> {
     *         <span style="color: #70226C">return</span> asHtml(<span style="color: #0000C0">path_Sea_SeaListJsp</span>);
     *     });
     * }
     * </pre>
     * @param validationMessagesLambda The callback for setting of validation error messages. (NotNull)
     * @param validationErrorLambda The callback for response when validation error. (NotNull)
     */
    default void throwValidationError(VaMessenger<MESSAGES> validationMessagesLambda, VaErrorHook validationErrorLambda) {
        createValidator().throwValidationError(() -> {
            final MESSAGES messages = createMessages();
            validationMessagesLambda.message(messages);
            return messages;
        }, validationErrorLambda);
    }

    /**
     * Throw validation error exception immediately, for API, when e.g. JsonResponse. <br>
     * <pre>
     * <span style="color: #70226C">if</span> (...) { <span style="color: #3F7E5E">// any invalid state</span> 
     *     <span style="color: #CC4747">throwValidationErrorApi</span>(<span style="color: #553000">messages</span> <span style="font-size: 120%">-</span>&gt;</span> <span style="color: #553000">messages</span>.addConstraints...);
     * }
     * </pre>
     * @param validationMessagesLambda The callback for setting of validation error messages. (NotNull)
     */
    default void throwValidationErrorApi(VaMessenger<MESSAGES> validationMessagesLambda) {
        createValidator().throwValidationErrorApi(() -> {
            final MESSAGES messages = createMessages();
            validationMessagesLambda.message(messages);
            return messages;
        });
    }

    // ===================================================================================
    //                                                                    Validation Parts
    //                                                                    ================
    /**
     * @return The new-created validator for action. (NotNull)
     */
    ActionValidator<MESSAGES> createValidator();

    /**
     * @return The new-created messages for action. (NotNull)
     */
    MESSAGES createMessages();
}
