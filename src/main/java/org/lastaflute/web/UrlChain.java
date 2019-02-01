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
package org.lastaflute.web;

import java.util.Arrays;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.web.exception.WrongUrlChainUseException;

/**
 * @author jflute
 */
public class UrlChain {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The action instance that uses this chain, basically for logging. (NullAllowed: when empty chain) */
    protected final Object action;

    protected Object[] urlParts;
    protected Object[] paramsOnGet;
    protected Object hashOnUrl;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public UrlChain(Object action) {
        this.action = action;
    }

    // ===================================================================================
    //                                                                        Chain Method
    //                                                                        ============
    // method names and specifications should be synchronized with action's methods
    /**
     * Set up more URL parts as URL chain.
     * @param urlParts The varying array of URL parts. (NotNull)
     * @return The created instance of URL chain. (NotNull)
     */
    public UrlChain moreUrl(Object... urlParts) {
        final String argTitle = "urlParts";
        assertArgumentNotNull(argTitle, urlParts);
        checkWrongUrlChainUse(argTitle, urlParts);
        this.urlParts = urlParts;
        return this;
    }

    /**
     * Set up parameters on GET as URL chain.
     * @param paramsOnGet The varying array of parameters on GET. (NotNull)
     * @return The created instance of URL chain. (NotNull)
     */
    public UrlChain params(Object... paramsOnGet) {
        final String argTitle = "paramsOnGet";
        assertArgumentNotNull(argTitle, paramsOnGet);
        checkWrongUrlChainUse(argTitle, paramsOnGet);
        this.paramsOnGet = paramsOnGet;
        return this;
    }

    /**
     * Set up hash on URL as URL chain.
     * The name and specification of this method is synchronized with hash().
     * @param hashOnUrl The value of hash on URL. (NotNull)
     * @return The created instance of URL chain. (NotNull)
     */
    public UrlChain hash(Object hashOnUrl) {
        final String argTitle = "hashOnUrl";
        assertArgumentNotNull(argTitle, hashOnUrl);
        checkWrongUrlChainUse(argTitle, hashOnUrl);
        this.hashOnUrl = hashOnUrl;
        return this;
    }

    // ===================================================================================
    //                                                                       Assert Helper
    //                                                                       =============
    protected void checkWrongUrlChainUse(String argTitle, Object... args) {
        for (Object obj : args) {
            if (obj instanceof UrlChain) {
                throwWrongUrlChainUseException(argTitle, args);
            }
        }
    }

    protected void throwWrongUrlChainUseException(String argTitle, Object[] args) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Wrong URL chain use was found.");
        br.addItem("Advice");
        br.addElement("Maybe your method call is e.g. like this:");
        br.addElement("  (x): return redirectByParam(FooAction.class, moreUrl(...));");
        br.addElement("  (x): return redirectByParam(FooAction.class, params(...));");
        br.addElement("  (x): return redirectWith(FooAction.class, params(moreUrl(...)));");
        br.addElement("");
        br.addElement("Use redirectByParam() if you use only GET parameter.");
        br.addElement("Use redirectWith() if you need flexible redirect call.");
        br.addElement("No chain method in chain method arguments.");
        br.addElement("Anyway, make sure your method call is correct.");
        br.addElement("");
        br.addElement("The redirectByParam() is like this:");
        br.addElement("  (o): return redirectByParam(FooAction.class, \"memberId\", member.getMemberId());");
        br.addElement("");
        br.addElement("The redirectWith() is like this:");
        br.addElement("  (o): return redirectWith(FooAction.class, moreUrl(foo.getBarId()).hash(\"qux\"));");
        br.addElement("  (o): return redirectWith(FooAction.class, moreUrl(\"edit\").params(\"fooId\", foo.getFooId()));");
        br.addItem("Action");
        br.addElement(action != null ? action.getClass() : null); // null check just in case
        br.addItem("Arguments as " + argTitle);
        br.addElement(Arrays.asList(args));
        final String msg = br.buildExceptionMessage();
        throw new WrongUrlChainUseException(msg);
    }

    /**
     * Assert that the argument is not null.
     * @param argumentName The name of assert-target argument. (NotNull)
     * @param value The value of argument. (NotNull)
     * @throws IllegalArgumentException When the value is null.
     */
    protected void assertArgumentNotNull(String argumentName, Object value) {
        if (argumentName == null) {
            String msg = "The argument name should not be null: argName=null value=" + value;
            throw new IllegalArgumentException(msg);
        }
        if (value == null) {
            String msg = "The value should not be null: argName=" + argumentName;
            throw new IllegalArgumentException(msg);
        }
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public Object[] getUrlParts() {
        return urlParts;
    }

    public Object[] getParamsOnGet() {
        return paramsOnGet;
    }

    public Object getHashOnUrl() {
        return hashOnUrl;
    }
}
