/*
 * Copyright 2015-2017 the original author or authors.
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
package org.lastaflute.web.response.pushed;

import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 * @since 0.6.4 (2015/09/30 Wednesday)
 */
public class PushedFormInfo {

    protected final Class<?> formType; // not null
    protected final PushedFormOption<?> formOption; // null allowed

    public PushedFormInfo(Class<?> formType) {
        this.formType = formType;
        this.formOption = null;
    }

    public PushedFormInfo(Class<?> formType, PushedFormOption<?> formOption) {
        this.formType = formType;
        this.formOption = formOption;
    }

    public Class<?> getFormType() {
        return formType;
    }

    public OptionalThing<PushedFormOption<?>> getFormOption() {
        return OptionalThing.ofNullable(formOption, () -> {
            throw new IllegalStateException("Not found the pushed form option: " + formType);
        });
    }
}
