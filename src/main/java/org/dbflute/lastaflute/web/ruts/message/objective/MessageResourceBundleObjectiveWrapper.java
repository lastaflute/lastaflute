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
package org.dbflute.lastaflute.web.ruts.message.objective;

import org.dbflute.lasta.di.helper.message.MessageResourceBundle;

/**
 * The wrapper of message resource bundle for objective handling. <br>
 * This has detail info of message resource, e.g. parent and extends level. <br>
 * And is comparable by the detail info.
 * @author jflute
 */
public class MessageResourceBundleObjectiveWrapper implements MessageResourceBundle, Comparable<MessageResourceBundle> {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final MessageResourceBundle wrappedBundle;
    protected final boolean defaultLang;
    protected final Integer parentLevel;
    protected final Integer extendsLevel;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public MessageResourceBundleObjectiveWrapper(MessageResourceBundle wrappedBundle, boolean defaultLang, Integer parentLevel,
            Integer extendsLevel) {
        this.wrappedBundle = wrappedBundle;
        this.defaultLang = defaultLang;
        this.parentLevel = parentLevel;
        this.extendsLevel = extendsLevel;
    }

    // ===================================================================================
    //                                                                    Wrapper Delegate
    //                                                                    ================
    public String get(String key) {
        return wrappedBundle.get(key);
    }

    public MessageResourceBundle getParent() {
        return wrappedBundle.getParent();
    }

    public void setParent(MessageResourceBundle parent) {
        wrappedBundle.setParent(parent);
    }

    // ===================================================================================
    //                                                                           CompareTo
    //                                                                           =========
    public int compareTo(MessageResourceBundle you) {
        final MessageResourceBundleObjectiveWrapper wrappedYou = (MessageResourceBundleObjectiveWrapper) you;
        final int byDefaultLang = compareToByDefaultLang(wrappedYou);
        if (byDefaultLang != 0) {
            return byDefaultLang;
        }
        final int byParent = compareToByParent(wrappedYou);
        if (byParent != 0) {
            return byParent;
        }
        final int byExtends = compareToByExtends(wrappedYou);
        if (byExtends != 0) {
            return byExtends;
        }
        return 0;
    }

    protected int compareToByDefaultLang(MessageResourceBundleObjectiveWrapper bundle) {
        // ordered: specified language is prior
        final boolean iamSpecifiedLang = !defaultLang;
        final boolean youAreSpecifiedLang = !bundle.isDefaultLang();
        if (iamSpecifiedLang) {
            return youAreSpecifiedLang ? 0 : -1;
        } else {
            return youAreSpecifiedLang ? 1 : 0;
        }
    }

    protected int compareToByParent(MessageResourceBundleObjectiveWrapper bundle) {
        // ordered: null or smaller
        final boolean iamRoot = parentLevel == null;
        final boolean youAreRoot = bundle.getParentLevel() == null;
        if (iamRoot) {
            return youAreRoot ? 0 : -1;
        } else {
            return youAreRoot ? 1 : parentLevel - bundle.getParentLevel();
        }
    }

    protected int compareToByExtends(MessageResourceBundleObjectiveWrapper bundle) {
        // ordered: null or smaller
        final boolean iamDomain = extendsLevel == null;
        final boolean youAreDomain = bundle.getExtendsLevel() == null;
        if (iamDomain) {
            return youAreDomain ? 0 : -1;
        } else {
            return youAreDomain ? 1 : extendsLevel - bundle.getExtendsLevel();
        }
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return "{defaultLang=" + defaultLang + ", parent=" + parentLevel + ", extends=" + extendsLevel + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public boolean isDefaultLang() {
        return defaultLang;
    }

    public Integer getParentLevel() {
        return parentLevel;
    }

    public Integer getExtendsLevel() {
        return extendsLevel;
    }
}
