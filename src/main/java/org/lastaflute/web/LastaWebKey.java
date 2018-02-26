/*
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
package org.lastaflute.web;

/**
 * @author jflute
 */
public interface LastaWebKey {

    String MODULE_CONFIG_KEY = "lastaflute.config.MODULE"; // ServletContext, Request
    String ACTION_MAPPING_KEY = "lastaflute.config.ACTION_MAPPING"; // Request
    String ACTION_EXECUTE_KEY = "lastaflute.config.ACTION_EXECUTE"; // Request

    String MESSAGE_RESOURCES_KEY = "lastaflute.message.RESOURCES"; // ServletContext
    String ACTION_ERRORS_KEY = "lastaflute.message.ACTION_ERRORS"; // Request or Session
    String ACTION_INFO_KEY = "lastaflute.message.ACTION_INFO"; // Request or Session

    String ACTION_PATH_KEY = "lastaflute.action.ACTION_PATH"; // Request
    String ACTION_RUNTIME_KEY = "lastaflute.action.ACTION_RUMTIME"; // Request
    String PUSHED_ACTION_FORM_KEY = "lastaflute.action.PUSHED_ACTION_FORM"; // Request

    String USER_BEAN_KEY = "lastaflute.action.USER_BEAN"; // Session (actually suffix added per assist)
    String USER_LOCALE_KEY = "lastaflute.action.USER_LOCALE"; // Request or Session
    String USER_TIMEZONE_KEY = "lastaflute.action.USER_TIMEZONE"; // Request or Session

    String CSRF_TOKEN_KEY = "lastaflute.action.CSRF_TOKEN"; // Session
    String TRANSACTION_TOKEN_KEY = "lastaflute.action.TRANSACTION_TOKEN"; // Parameter and Session
    String DOUBLE_SUBMITTED_KEY = "lastaflute.action.DOUBLE_SUBMITTED_MARK"; // Request
    String FIRST_SUBMITTED_KEY = "lastaflute.action.FIRST_SUBMITTED_MARK"; // Request
    String REQUEST_BODY_KEY = "lastaflute.action.REQUEST_BODY"; // Request
    String INOUT_LOGGING_KEY = "lastaflute.action.INOUT_LOGGING"; // Request

    String DBFLUTE_SQL_COUNT_KEY = "lastaflute.dbflute.SQL_COUNT"; // Request
    String DBFLUTE_TRANSACTION_MEMORIES_KEY = "lastaflute.dbflute.TRANSACTION_MEMORIES"; // Request
    String MAILFLUTE_MAIL_COUNT_KEY = "lastaflute.mailflute.MAIL_COUNT"; // Request
    String REMOTEAPI_COUNT_KEY = "lastaflute.remoteapi.REMOTE_API_COUNT"; // Request
}
