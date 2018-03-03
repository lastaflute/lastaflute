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
package org.lastaflute.web.login.option;

/**
 * @author jflute
 */
public class LoginOption implements LoginSpecifiedOption {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected boolean rememberMe;
    protected boolean silentLogin;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LoginOption() {
    }

    // ===================================================================================
    //                                                                         Easy-to-Use
    //                                                                         ===========
    /**
     * Set the remember-me login or not.
     * @param rememberMe Does it use remember-me for next time login?
     * @return this. (NotNull)
     */
    public LoginOption rememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
        return this;
    }

    /**
     * Set the silent login or not.
     * @param silentLogin Is the login executed silently? (no saving history)
     * @return this. (NotNull)
     */
    public LoginOption silentLogin(boolean silentLogin) {
        this.silentLogin = silentLogin;
        return this;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return "option:{rememberMe=" + rememberMe + ", silentLogin=" + silentLogin + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public boolean isRememberMe() {
        return rememberMe;
    }

    public boolean isSilentLogin() {
        return silentLogin;
    }
}
