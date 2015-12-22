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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dbflute.Entity;
import org.dbflute.bhv.core.BehaviorCommandMeta;
import org.dbflute.dbmeta.DBMeta;
import org.dbflute.util.DfCollectionUtil;

/**
 * @author jflute
 * @since 0.7.2 (2015/12/22 Tuesday)
 */
public class TransactionSavedRecentResult {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final long statementNo;
    protected final String tableName; // not null
    protected final String command; // not null
    protected final Long beginMillis; // null allowed when failure
    protected final Long endMillis; // null allowed when failure
    protected final Class<?> resultType; // not null e.g. Integer, Entity, List
    protected final Map<String, Object> resultMap; // null allowed

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public TransactionSavedRecentResult(long statementNo, String tableName, String command, Long beginMillis, Long endMillis,
            Class<?> resultType, Object resultValue, BehaviorCommandMeta meta) {
        final Map<String, Object> resultMap;
        if (resultValue != null) {
            if (resultValue instanceof Entity) { // e.g. selectEntity()
                final Entity entity = (Entity) resultValue;
                resultMap = prepareEntityResultMap(entity);
            } else if (resultValue instanceof List<?>) { // e.g. selectList()
                final List<?> list = (List<?>) resultValue;
                resultMap = new LinkedHashMap<String, Object>(2);
                resultMap.put("size", list.size());
                final Object firstExp;
                if (!list.isEmpty()) {
                    final Object firstElement = list.get(0);
                    if (firstElement instanceof Entity) {
                        firstExp = prepareEntityResultMap(((Entity) firstElement));
                    } else {
                        firstExp = prepareHashResultMap(firstElement.hashCode());
                    }
                } else {
                    firstExp = null;
                }
                resultMap.put("first", firstExp);
            } else { // basically no way
                // #hope want to get updated PK from meta, but after DBFlute-1.1.1
                final String valueTitle;
                if (meta.isInsert()) {
                    valueTitle = "inserted";
                } else if (meta.isUpdate()) {
                    valueTitle = "updated";
                } else if (meta.isDelete()) {
                    valueTitle = "deleted";
                } else if (meta.isSelectCount()) {
                    valueTitle = "count";
                } else {
                    valueTitle = "value";
                }
                resultMap = DfCollectionUtil.newHashMap(valueTitle, resultValue);
            }
        } else { // e.g. selectCursor()
            resultMap = null;
        }
        // meta is not saved because of internal object (also may have update data)
        this.statementNo = statementNo;
        this.tableName = tableName;
        this.command = command;
        this.beginMillis = beginMillis;
        this.endMillis = endMillis;
        this.resultType = resultType;
        this.resultMap = resultMap;
    }

    protected Map<String, Object> prepareEntityResultMap(Entity entity) {
        final Map<String, Object> resultMap;
        final DBMeta dbmeta = entity.asDBMeta();
        if (dbmeta.hasPrimaryKey() && entity.hasPrimaryKeyValue()) { // mainly here
            resultMap = dbmeta.extractPrimaryKeyMap(entity);
        } else { // no PK table
            resultMap = prepareHashResultMap(entity.instanceHash());
        }
        return resultMap;
    }

    protected Map<String, Object> prepareHashResultMap(int hash) {
        return DfCollectionUtil.newHashMap("hash", Integer.toHexString(hash));
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public long getStatementNo() {
        return statementNo;
    }

    public String getTableName() {
        return tableName;
    }

    public String getCommand() {
        return command;
    }

    public Long getBeginMillis() {
        return beginMillis;
    }

    public Long getEndMillis() {
        return endMillis;
    }

    public Class<?> getResultType() {
        return resultType;
    }

    public Map<String, Object> getResultMap() {
        return resultMap;
    }
}
