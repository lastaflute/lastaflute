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
package org.lastaflute.db.jta;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import org.dbflute.helper.HandyDate;
import org.dbflute.optional.OptionalThing;
import org.dbflute.system.DBFluteSystem;
import org.dbflute.util.DfTraceViewUtil;
import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.Srl;

/**
 * @author jflute
 * @since 0.7.2 (2015/12/22 Tuesday)
 */
public class TransactionRomanticMemoriesBuilder {

    // ===================================================================================
    //                                                                            Romantic
    //                                                                            ========
    /**
     * @param tx The transaction it looks so romantic. (NotNull)
     * @return The romantic expression for transaction memories. (NotNull)
     */
    public OptionalThing<String> buildRomanticMemories(RomanticTransaction tx) {
        final List<TransactionSavedRecentResult> resultList = tx.getReadOnlyRecentResultList();
        if (resultList.isEmpty()) {
            return OptionalThing.empty();
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(DfTypeUtil.toClassTitle(tx)).append("@").append(toHexHashExp(tx));
        sb.append(ln()).append("<< Transaction Current State >>");
        final int beforeStateLength = sb.length();
        // error message already contains it
        //setupEntryMethodExp(sb, tx);
        //setupUserBeanExp(sb, tx);
        sb.append(ln()).append("beginning time: ").append(toDateExp(tx.getTransactionBeginMillis()));
        setupTableCommandExp(sb, tx);
        if (beforeStateLength == sb.length()) { // no change
            sb.append(ln()).append("*no info");
        }
        sb.append(ln()).append("<< Transaction Recent Result >>");
        final int precision = calculatePrecision(resultList);
        for (TransactionSavedRecentResult result : resultList) {
            final long statementNo = result.getStatementNo();
            final String tableName = result.getTableName();
            final String command = result.getCommand();
            // e.g.
            //  1. (2015/12/22 02:48:40.928) [00m00s023ms] PRODUCT@selectList => List:{size=4, first={PRODUCT_ID=6}} 
            //  2. (2015/12/22 02:48:40.954) [00m00s001ms] PRODUCT@selectCount => Integer:{count=20} 
            sb.append(ln()).append(Srl.lfill(String.valueOf(statementNo), precision, '0'));
            sb.append(". ");
            setupBeginTimeExp(sb, result);
            sb.append(" ");
            setupPerformanceView(sb, result);
            sb.append(" ").append(tableName).append("@").append(command);
            sb.append(" => ");
            setupResultExp(sb, result);
        }
        return OptionalThing.of(sb.toString());
    }

    protected int calculatePrecision(List<TransactionSavedRecentResult> resultList) {
        if (resultList.isEmpty()) {
            return 0; // unused
        }
        final long latestNo = resultList.get(resultList.size() - 1).getStatementNo();
        return String.valueOf(latestNo).length();
    }

    // ===================================================================================
    //                                                                       Current State
    //                                                                       =============
    // similar to current state, no recycle for independency
    protected void setupTableCommandExp(StringBuilder sb, RomanticTransaction tx) { // originated in current state
        final Map<String, Set<String>> tableCommandMap = tx.getReadOnlyTableCommandMap();
        if (!tableCommandMap.isEmpty()) {
            final StringBuilder mapSb = new StringBuilder();
            mapSb.append("map:{");
            int index = 0;
            for (Entry<String, Set<String>> entry : tableCommandMap.entrySet()) {
                final String tableName = entry.getKey();
                final Set<String> commandSet = entry.getValue();
                if (index > 0) {
                    mapSb.append(" ; ");
                }
                mapSb.append(tableName);
                mapSb.append(" = list:{").append(Srl.connectByDelimiter(commandSet, " ; ")).append("}");
                ++index;
            }
            mapSb.append("}");
            sb.append(ln()).append("table command: ").append(mapSb.toString());
        }
    }

    // ===================================================================================
    //                                                                              Result
    //                                                                              ======
    protected void setupResultExp(StringBuilder sb, TransactionSavedRecentResult result) {
        final Class<?> resultType = result.getResultType();
        final Map<String, Object> resultMap = result.getResultMap();
        sb.append(resultType.getSimpleName()).append(":").append(buildResultExp(resultMap));
    }

    protected String buildResultExp(Map<String, Object> resultMap) {
        return (resultMap != null ? resultMap : Collections.emptyMap()).toString();
    }

    // ===================================================================================
    //                                                                           Begin/End
    //                                                                           =========
    protected void setupBeginTimeExp(StringBuilder sb, TransactionSavedRecentResult result) {
        sb.append("(");
        final Long beginMillis = result.getBeginMillis();
        if (beginMillis != null) {
            sb.append(toDateExp(beginMillis));
        } else {
            sb.append("*unknown time");
        }
        sb.append(")");
    }

    protected void setupPerformanceView(StringBuilder sb, TransactionSavedRecentResult result) {
        sb.append("[");
        final Long beginMillis = result.getBeginMillis();
        final Long endMillis = result.getEndMillis();
        if (beginMillis != null && endMillis != null) {
            final String cost = DfTraceViewUtil.convertToPerformanceView(endMillis - beginMillis);
            sb.append(cost);
        } else {
            sb.append("*unknown cost");
        }
        sb.append("]");
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected String toDateExp(Long beginMillis) {
        final TimeZone timeZone = DBFluteSystem.getFinalTimeZone();
        return new HandyDate(new Date(beginMillis)).timeZone(timeZone).toDisp("yyyy/MM/dd HH:mm:ss.SSS");
    }

    protected String toHexHashExp(Object obj) {
        return obj != null ? Integer.toHexString(obj.hashCode()) : "null";
    }

    protected String ln() {
        return "\n";
    }
}
