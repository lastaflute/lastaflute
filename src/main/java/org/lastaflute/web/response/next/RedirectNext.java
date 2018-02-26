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
package org.lastaflute.web.response.next;

/**
 * @author jflute
 */
public class RedirectNext implements RoutingNext {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String redirectPath;
    protected final RedirectPathStyle redirectPathDest;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public RedirectNext(String redirectPath, RedirectPathStyle redirectPathStyle) {
        if (redirectPath == null) {
            throw new IllegalArgumentException("The argument 'redirectPath' should not be null.");
        }
        if (redirectPathStyle == null) {
            throw new IllegalArgumentException("The argument 'redirectPathStyle' should not be null: " + redirectPath);
        }
        this.redirectPath = redirectPath;
        this.redirectPathDest = redirectPathStyle;
    }

    public enum RedirectPathStyle { // to avoid boolean argument
        INNER, AS_IS
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public int hashCode() {
        return 31 * redirectPath.hashCode() * redirectPathDest.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RedirectNext)) {
            return false;
        }
        final RedirectNext you = ((RedirectNext) obj);
        return redirectPath.equals(you.redirectPath) && redirectPathDest.equals(you.redirectPathDest);
    }

    @Override
    public String toString() {
        return "redirect:{" + redirectPath + ", " + redirectPathDest + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    @Override
    public String getRoutingPath() {
        return redirectPath;
    }

    @Override
    public boolean isAsIs() {
        return RedirectPathStyle.AS_IS.equals(redirectPathDest);
    }

    public String getRedirectPath() {
        return redirectPath;
    }
}
