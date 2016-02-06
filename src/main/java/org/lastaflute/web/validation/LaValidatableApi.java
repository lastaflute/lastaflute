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
package org.lastaflute.web.validation;

import org.lastaflute.core.message.UserMessages;

/**
 * @param <MESSAGES> The type of action message.
 * @author jflute
 */
public interface LaValidatableApi<MESSAGES extends UserMessages> {

    // ===================================================================================
    //                                                                            Validate
    //                                                                            ========
    /**
     * Validate the JSON body values for API, when e.g. JsonResponse. <br>
     * The validation error handling is in ApiFailureHook.
     * <pre>
     * <span style="color: #3F7E5E">// by-annotation only</span>
     * <span style="color: #CC4747">validate</span>(<span style="color: #553000">body</span>, <span style="color: #553000">messages</span> <span style="font-size: 120%">-</span>&gt;</span> {});
     * 
     * <span style="color: #3F7E5E">// by-annotation and by-program</span>
     * <span style="color: #CC4747">validate</span>(<span style="color: #553000">body</span>, <span style="color: #553000">messages</span> <span style="font-size: 120%">-</span>&gt;</span> {
     *     <span style="color: #70226C">if</span> (...) {
     *         <span style="color: #553000">messages</span>.addConstraint...
     *     }
     * });
     * </pre>
     * @param body The JSON body (or action form) that has request parameters. (NotNull)
     * @param moreValidationLambda The callback for more validation, e.g. correlation rule, very complex rule. (NotNull)
     * @return The success information of validation, basically for success attribute. (NotNull)
     */
    default ValidationSuccess validate(Object body, VaMore<MESSAGES> moreValidationLambda) {
        return createValidator().validateApi(body, moreValidationLambda);
    }

    // ===================================================================================
    //                                                                         Throw Error
    //                                                                         ===========
    /**
     * Throw validation error exception immediately, for API, when e.g. JsonResponse. <br>
     * <pre>
     * <span style="color: #70226C">if</span> (...) { <span style="color: #3F7E5E">// any invalid state</span> 
     *     <span style="color: #CC4747">throwValidationError</span>(<span style="color: #553000">messages</span> <span style="font-size: 120%">-</span>&gt;</span> <span style="color: #553000">messages</span>.addConstraints...);
     * }
     * </pre>
     * @param validationMessagesLambda The callback for setting of validation error messages. (NotNull)
     */
    default void throwValidationError(VaMessenger<MESSAGES> validationMessagesLambda) {
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
