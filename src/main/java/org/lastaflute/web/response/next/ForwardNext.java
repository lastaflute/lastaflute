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
public class ForwardNext implements RoutingNext {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final String forwardPath;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ForwardNext(String forwardPath) {
        if (forwardPath == null) {
            throw new IllegalArgumentException("The argument 'forwardPath' should not be null.");
        }
        this.forwardPath = forwardPath;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public int hashCode() {
        return 31 * forwardPath.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ForwardNext)) {
            return false;
        }
        final ForwardNext you = ((ForwardNext) obj);
        return forwardPath.equals(you.forwardPath);
    }

    @Override
    public String toString() {
        return "forward:{" + forwardPath + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    @Override
    public String getRoutingPath() {
        return forwardPath;
    }

    @Override
    public boolean isAsIs() {
        return false;
    }

    public String getForwardPath() {
        return forwardPath;
    }
}
