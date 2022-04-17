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

/**
 * @author jflute
 */
public interface EmbeddedMessageKey {

    String ERRORS_LOGIN_FAILURE_KEY = "errors.login.failure"; // business but embedded
    String ERRORS_APP_ILLEGAL_TRANSITION = "errors.app.illegal.transition";
    String ERRORS_APP_DB_ALREADY_DELETED = "errors.app.db.already.deleted";
    String ERRORS_APP_DB_ALREADY_UPDATED = "errors.app.db.already.updated";
    String ERRORS_APP_DB_ALREADY_EXISTS = "errors.app.db.already.exists";
    String ERRORS_APP_DOUBLE_SUBMIT_REQUEST = "errors.app.double.submit.request";

    public static class SimpleEmbeddedMessageKeySupplier implements EmbeddedMessageKeySupplier {

        public String getErrorsLoginFailureKey() {
            return ERRORS_LOGIN_FAILURE_KEY;
        }

        public String getErrorsAppIllegalTransitionKey() {
            return ERRORS_APP_ILLEGAL_TRANSITION;
        }

        public String getErrorsAppDbAlreadyDeletedKey() {
            return ERRORS_APP_DB_ALREADY_DELETED;
        }

        public String getErrorsAppDbAlreadyUpdatedKey() {
            return ERRORS_APP_DB_ALREADY_UPDATED;
        }

        public String getErrorsAppDbAlreadyExistsKey() {
            return ERRORS_APP_DB_ALREADY_EXISTS;
        }

        public String getErrorsAppDoubleSubmitRequestKey() {
            return ERRORS_APP_DOUBLE_SUBMIT_REQUEST;
        }
    }
}
