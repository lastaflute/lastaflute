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
package org.lastaflute.web.login.option;

/**
 * @author jflute
 */
public class RememberMeLoginOption implements RememberMeLoginSpecifiedOption {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected boolean updateToken;
    protected boolean silentLogin;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public RememberMeLoginOption() {
    }

    // ===================================================================================
    //                                                                         Easy-to-Use
    //                                                                         ===========
    /**
     * Set the update token or not.
     * @param updateToken Does it update access token of auto-login? (true: e.g. increase expire days)
     * @return this. (NotNull)
     */
    public RememberMeLoginOption updateToken(boolean updateToken) {
        this.updateToken = updateToken;
        return this;
    }

    /**
     * Set the silent login or not.
     * @param silentLogin Is the login executed silently? (no saving history)
     * @return this. (NotNull)
     */
    public RememberMeLoginOption silentLogin(boolean silentLogin) {
        this.silentLogin = silentLogin;
        return this;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return "option:{updateToken=" + updateToken + ", silentLogin=" + silentLogin + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public boolean isUpdateToken() {
        return updateToken;
    }

    public boolean isSilentLogin() {
        return silentLogin;
    }
}
