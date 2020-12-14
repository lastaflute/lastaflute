/*
 * Copyright 2015-2020 the original author or authors.
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

/**
 * @author jflute
 * @since 0.7.6 (2016/01/07 Thursday)
 */
public class RequestedMailCount {

    protected final int countOfPosting;
    protected final int countOfAlsoHtml;
    protected final int countOfDryrun;
    protected final int countOfForcedlyDirect;

    public RequestedMailCount(PostedMailCounter counter) {
        this.countOfPosting = counter.getCountOfPosting();
        this.countOfAlsoHtml = counter.getCountOfAlsoHtml();
        this.countOfDryrun = counter.getCountOfDryrun();
        this.countOfForcedlyDirect = counter.getCountOfForcedlyDirect();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{posting=").append(countOfPosting);
        if (countOfAlsoHtml > 0) {
            sb.append(", alsoHtml=").append(countOfAlsoHtml);
        }
        if (countOfDryrun > 0) {
            sb.append(", dryrun=").append(countOfDryrun);
        }
        if (countOfForcedlyDirect > 0) {
            sb.append(", forcedlyDirect=").append(countOfForcedlyDirect);
        }
        sb.append("}");
        return sb.toString();
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
