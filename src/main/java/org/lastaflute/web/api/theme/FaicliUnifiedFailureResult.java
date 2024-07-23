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
package org.lastaflute.web.api.theme;

import java.util.List;
import java.util.Map;

import org.lastaflute.core.util.Lato;
import org.lastaflute.web.validation.Required;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * @author jflute
 * @since 1.0.0 (2017/08/10 Thursday)
 */
public class FaicliUnifiedFailureResult {

    // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
    // [Reference Site]
    // http://dbflute.seasar.org/ja/lastaflute/howto/impldesign/jsonfaicli.html
    // _/_/_/_/_/_/_/_/_/_/

    @Required
    public final FaicliUnifiedFailureType cause;

    public static enum FaicliUnifiedFailureType {
        VALIDATION_ERROR, BUSINESS_ERROR, CLIENT_ERROR // used by application
        , SERVER_ERROR // basically used by 500.json
    }

    @NotNull
    @Valid
    public final List<FaicliFailureErrorPart> errors;

    public static class FaicliFailureErrorPart { // as client-managed message way

        @Required
        public final String field;

        @Required
        public final String code; // for client-managed message

        @NotNull
        public final Map<String, Object> data; // for client-managed message

        public FaicliFailureErrorPart(String field, String code, Map<String, Object> data) {
            this.field = field;
            this.code = code;
            this.data = data;
        }
    }

    public FaicliUnifiedFailureResult(FaicliUnifiedFailureType cause, List<FaicliFailureErrorPart> errors) {
        this.cause = cause;
        this.errors = errors;
    }

    @Override
    public String toString() {
        return Lato.string(this);
    }
}
