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
package org.lastaflute.web.token;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.web.token.exception.DoubleSubmittedRequestException;

/**
 * @author modified by jflute (originated in Struts)
 */
public interface DoubleSubmitManager {

    // ===================================================================================
    //                                                                  Token Manipulation
    //                                                                  ==================
    /**
     * Save the transaction token to session.
     * @param groupType The class type to identify group of transaction. (NotNull)
     * @return The generated token saved in session. (NotNull)
     */
    String saveToken(Class<?> groupType);

    /**
     * Generate the transaction token. (generation only)
     * @param groupType The class type to identify group of transaction. (NotNull)
     * @return The generated string as transaction token. (NotNull)
     */
    String generateToken(Class<?> groupType);

    // ===================================================================================
    //                                                                 Token Determination
    //                                                                 ===================
    /**
     * Is the requested token matched with the token saved in session? (determination only)
     * @param groupType The class type to identify group of transaction. (NotNull)
     * @return The determination, true or false.
     */
    boolean determineToken(Class<?> groupType);

    /**
     * Is the requested token matched with the token saved in session? <br>
     * And reset token after determination.
     * @param groupType The class type to identify group of transaction. (NotNull)
     * @return The determination, true or false.
     */
    boolean determineTokenWithReset(Class<?> groupType);

    // ===================================================================================
    //                                                                  Token Verification
    //                                                                  ==================
    /**
     * Verify the request token (whether the requested token is same as saved token) <br>
     * And reset the saved token, it can be used only one-time.
     * @param groupType The class type to identify group of transaction. (NotNull)
     * @param errorHook The hook to return action response when token error. (NotNull)
     * @throws DoubleSubmittedRequestException When the token is invalid. That has specified error hook.
     */
    void verifyToken(Class<?> groupType, TokenErrorHook errorHook);

    /**
     * Verify the request token (whether the request token is same as saved token) <br>
     * Keep the saved token, so this method is basically for intermediate request.
     * @param groupType The class type to identify group of transaction. (NotNull)
     * @param errorHook The hook to return action response when token error. (NotNull)
     * @throws DoubleSubmittedRequestException When the token is invalid.
     */
    void verifyTokenKeep(Class<?> groupType, TokenErrorHook errorHook);

    // ===================================================================================
    //                                                                       Token Closing
    //                                                                       =============
    void resetToken(Class<?> groupType);

    // ===================================================================================
    //                                                                        Token Access
    //                                                                        ============
    OptionalThing<String> getRequestedToken();

    OptionalThing<DoubleSubmitTokenMap> getSessionTokenMap();

    boolean isDoubleSubmittedRequest();
}
