/*
 * Copyright 2015-2019 the original author or authors.
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
package org.lastaflute.web.login.redirect;

import java.io.Serializable;

/**
 * @author jflute
 */
public class LoginRedirectBean implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final String redirectPath;

    public LoginRedirectBean(String redirectPath) {
        this.redirectPath = redirectPath;
    }

    @Override
    public String toString() {
        return "{redirectPath=" + redirectPath + "}";
    }

    public boolean hasRedirectPath() {
        return redirectPath != null && redirectPath.trim().length() > 0;
    }

    public String getRedirectPath() {
        return redirectPath;
    }
}
