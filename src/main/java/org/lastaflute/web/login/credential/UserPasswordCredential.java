/*
 * Copyright 2015-2024 the original author or authors.
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
package org.lastaflute.web.login.credential;

/**
 * @author jflute
 */
public class UserPasswordCredential implements LoginCredential {

    protected final String user;
    protected final String password;

    public UserPasswordCredential(String user, String password) {
        if (user == null || user.trim().isEmpty()) {
            throw new IllegalArgumentException("The argument 'user' should not be null or empty: " + user);
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("The argument 'password' should not be null or empty: " + password);
        }
        this.user = user;
        this.password = password;
    }

    @Override
    public String toString() {
        return "{" + user + "}";
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }
}
