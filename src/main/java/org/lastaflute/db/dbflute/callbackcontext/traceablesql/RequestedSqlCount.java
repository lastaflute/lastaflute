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
package org.lastaflute.db.dbflute.callbackcontext.traceablesql;

import org.dbflute.bhv.proposal.callback.ExecutedSqlCounter;

/**
 * @author jflute
 * @since 0.7.6 (2016/01/07 Thursday)
 */
public class RequestedSqlCount {

    protected final int countOfSelectCB;
    protected final int countOfEntityUpdate;
    protected final int countOfQueryUpdate;
    protected final int countOfOutsideSql;
    protected final int countOfProcedure;

    public RequestedSqlCount(ExecutedSqlCounter counter) {
        this.countOfSelectCB = counter.getCountOfSelectCB();
        this.countOfEntityUpdate = counter.getCountOfEntityUpdate();
        this.countOfQueryUpdate = counter.getCountOfQueryUpdate();
        this.countOfOutsideSql = counter.getCountOfOutsideSql();
        this.countOfProcedure = counter.getCountOfProcedure();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{total=").append(getTotalCountOfSql());
        if (countOfSelectCB > 0) {
            sb.append(", selectCB=").append(countOfSelectCB);
        }
        if (countOfEntityUpdate > 0) {
            sb.append(", entityUpdate=").append(countOfEntityUpdate);
        }
        if (countOfQueryUpdate > 0) {
            sb.append(", queryUpdate=").append(countOfQueryUpdate);
        }
        if (countOfOutsideSql > 0) {
            sb.append(", outsideSql=").append(countOfOutsideSql);
        }
        if (countOfProcedure > 0) {
            sb.append(", procedure=").append(countOfProcedure);
        }
        sb.append("}");
        return sb.toString();
    }

    public int getTotalCountOfSql() {
        return countOfSelectCB + countOfEntityUpdate + countOfQueryUpdate + countOfOutsideSql + countOfProcedure;
    }

    public int getCountOfSelectCB() {
        return countOfSelectCB;
    }

    public int getCountOfEntityUpdate() {
        return countOfEntityUpdate;
    }

    public int getCountOfQueryUpdate() {
        return countOfQueryUpdate;
    }

    public int getCountOfOutsideSql() {
        return countOfOutsideSql;
    }

    public int getCountOfProcedure() {
        return countOfProcedure;
    }
}
