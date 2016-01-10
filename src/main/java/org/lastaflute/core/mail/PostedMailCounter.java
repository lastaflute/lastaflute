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
package org.lastaflute.core.mail;

import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.magic.ThreadCompleted;

/**
 * @author jflute
 * @since 0.7.6 (2016/01/07 Thursday)
 */
public class PostedMailCounter implements ThreadCompleted { // thread cached

    protected int countOfPosting;
    protected int countOfAlsoHtml;
    protected int countOfDryrun;
    protected int countOfForcedlyDirect;

    public PostedMailCounter incrementPosting() {
        ++countOfPosting;
        return this;
    }

    public PostedMailCounter incrementAlsoHtml() {
        ++countOfAlsoHtml;
        return this;
    }

    public PostedMailCounter incrementDryrun() {
        ++countOfDryrun;
        return this;
    }

    public PostedMailCounter incrementForcedlyDirect() {
        ++countOfForcedlyDirect;
        return this;
    }

    public String toLineDisp() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{posting=").append(countOfPosting);
        sb.append(", alsoHtml=").append(countOfAlsoHtml);
        sb.append(", dryrun=").append(countOfDryrun);
        sb.append(", forcedlyDirect=").append(countOfForcedlyDirect);
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String toString() {
        return DfTypeUtil.toClassTitle(this) + "@" + Integer.toHexString(hashCode());
    }

    public int getCountOfPosting() {
        return countOfPosting;
    }

    public int getCountOfAlsoHtml() {
        return countOfAlsoHtml;
    }

    public int getCountOfDryrun() {
        return countOfDryrun;
    }

    public int getCountOfForcedlyDirect() {
        return countOfForcedlyDirect;
    }
}
