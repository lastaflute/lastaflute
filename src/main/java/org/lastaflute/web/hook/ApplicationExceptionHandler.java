/*
 * Copyright 2015-2022 the original author or authors.
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
package org.lastaflute.web.hook;

import org.lastaflute.core.message.UserMessages;
import org.lastaflute.web.response.ActionResponse;

/**
 * @author jflute
 * @since 0.9.5 (2017/04/15 Saturday)
 */
public class ApplicationExceptionHandler {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RuntimeException cause; // not null
    protected final HandledAppExMessagesSaver messagesSaver; // not null
    protected ActionResponse response = ActionResponse.undefined(); // not null

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ApplicationExceptionHandler(RuntimeException cause, HandledAppExMessagesSaver messagesSaver) {
        this.cause = cause;
        this.messagesSaver = messagesSaver;
    }

    @FunctionalInterface
    public static interface HandledAppExMessagesSaver {

        void save(UserMessages messages);
    }

    // ===================================================================================
    //                                                                              Handle
    //                                                                              ======
    /**
     * Handle application exception as your rule.
     * <pre>
     * {@literal @}Override
     * <span style="color: #77226C">protected</span> void handleApplicationException(ActionRuntime <span style="color: #553000">runtime</span>, ApplicationExceptionHandler <span style="color: #553000">handler</span>) {
     *     <span style="color: #77226C">super</span>.handleApplicationException(<span style="color: #553000">handler</span>);
     *     <span style="color: #553000">handler</span>.<span style="color: #CC4747">handle</span>(EntityAlreadyDeletedException.<span style="color: #77226C">class</span>, createMessages().addErrors...(<span style="color: #0000C0">GLOBAL</span>), <span style="color: #553000">cause</span> <span style="color: #90226C; font-weight: bold"><span style="font-size: 120%">-</span>&gt;</span> {
     *         <span style="color: #77226C">return</span> asHtml(<span style="color: #0000C0">path_...</span>);
     *     });
     * }
     * </pre>
     * @param <CAUSE> The type of application exception.
     * @param appExType The type of application exception. (NotNull)
     * @param messages The user messages for the application exception. (NotNull)
     * @param causeLambda The callback to provide action response for the application exception. (NotNull)
     */
    public <CAUSE extends RuntimeException> void handle(Class<CAUSE> appExType, UserMessages messages,
            HandledAppExResponseCall<CAUSE> causeLambda) {
        assertArgumentNotNull("appExType", appExType);
        assertArgumentNotNull("messages", messages);
        assertArgumentNotNull("causeLambda", causeLambda);
        if (appExType.isAssignableFrom(cause.getClass())) {
            messagesSaver.save(messages);
            @SuppressWarnings("unchecked")
            final CAUSE castCause = (CAUSE) cause; // safety because of in if-isAssignableFrom()
            response = causeLambda.callback(castCause); // not null, undefined allowed
            if (response == null) {
                throw new IllegalStateException("Cannot return null response from application handling.", cause);
            }
        }
    }

    @FunctionalInterface
    public static interface HandledAppExResponseCall<CAUSE extends RuntimeException> {

        /**
         * @param cause The thrown application exception. (NotNull)
         * @return The action response for the application exception. (NotNull, UndefinedAllowed: then no handling)
         */
        ActionResponse callback(CAUSE cause);
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            throw new IllegalArgumentException("The variableName should not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
        }
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public RuntimeException getCause() {
        return cause;
    }

    public ActionResponse getResponse() { // for framework
        return (response != null && response.isDefined()) ? response : ActionResponse.undefined();
    }
}
