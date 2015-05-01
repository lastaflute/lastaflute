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
package org.dbflute.lastaflute.web.callback;

/**
 * @author jflute
 */
public interface TypicalKey {

    String ERROR_MESSAGE_FORWARD_PATH = "/error/error_message.jsp";
    String ERRORS_LOGIN_FAILURE_KEY = "errors.login.failure";
    String ERRORS_APP_ALREADY_DELETED = "errors.app.already.deleted";
    String ERRORS_APP_ALREADY_UPDATED = "errors.app.already.updated";
    String ERRORS_APP_ALREADY_EXISTS = "errors.app.already.exists";
    String ERRORS_APP_ILLEGAL_TRANSITION = "errors.app.illegal.transition";

    public static class TypicalSimpleEmbeddedKeySupplier implements TypicalEmbeddedKeySupplier {
        public String getErrorMessageForwardPath() {
            return ERROR_MESSAGE_FORWARD_PATH;
        }

        public String getErrorsLoginFailureKey() {
            return ERRORS_LOGIN_FAILURE_KEY;
        }

        public String getErrorsAppAlreadyDeletedKey() {
            return ERRORS_APP_ALREADY_DELETED;
        }

        public String getErrorsAppAlreadyUpdatedKey() {
            return ERRORS_APP_ALREADY_UPDATED;
        }

        public String getErrorsAppAlreadyExistsKey() {
            return ERRORS_APP_ALREADY_EXISTS;
        }

        public String getErrorsAppIllegalTransitionKey() {
            return ERRORS_APP_ILLEGAL_TRANSITION;
        }
    }
}
