/*
 * Copyright 2015-2016 the original author or authors.
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
package org.lastaflute.web.response;

import java.util.Collections;
import java.util.Map;

import org.dbflute.optional.OptionalThing;

/**
 * @author jflute
 */
public class UndefinedResponse implements ActionResponse {

    protected static final UndefinedResponse INSTANCE_OF_UNDEFINED = new UndefinedResponse();

    public static UndefinedResponse instance() {
        return INSTANCE_OF_UNDEFINED;
    }

    protected UndefinedResponse() {
    }

    public UndefinedResponse header(String name, String... values) {
        throw new IllegalStateException("Cannot use header() for undefined response: " + name + ", " + values);
    }

    public Map<String, String[]> getHeaderMap() {
        return Collections.emptyMap();
    }

    public ActionResponse httpStatus(int httpStatus) {
        return this;
    }

    @Override
    public OptionalThing<Integer> getHttpStatus() {
        return OptionalThing.empty();
    }

    public boolean isUndefined() {
        return true;
    }

    public boolean isReturnAsEmptyBody() {
        return false;
    }

    @Override
    public UndefinedResponse afterTxCommit(ResponseHook noArgLambda) {
        throw new IllegalStateException("Cannot use afterTxCommit() for undefined response: " + noArgLambda);
    }

    @Override
    public OptionalThing<ResponseHook> getAfterTxCommitHook() {
        return OptionalThing.empty();
    }
}
