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
package org.lastaflute.web.exception;

import java.util.Collections;
import java.util.List;

import org.lastaflute.core.message.UserMessages;
import org.lastaflute.web.ruts.process.debugchallenge.JsonDebugChallenge;

/**
 * @author jflute
 */
public class RequestJsonParseFailureException extends Forced400BadRequestException {

    private static final long serialVersionUID = 1L;

    protected List<JsonDebugChallenge> challengeList;

    public RequestJsonParseFailureException(String debugMsg, UserMessages messages) {
        super(debugMsg, messages);
    }

    public RequestJsonParseFailureException(String debugMsg, UserMessages messages, Throwable cause) {
        super(debugMsg, messages, cause);
    }

    public RequestJsonParseFailureException withChallengeList(List<JsonDebugChallenge> challengeList) {
        this.challengeList = challengeList != null ? Collections.unmodifiableList(challengeList) : null;
        return this;
    }

    public List<JsonDebugChallenge> getChallengeList() {
        return challengeList != null ? challengeList : Collections.emptyList();
    }
}
