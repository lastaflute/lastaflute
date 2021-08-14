/*
 * Copyright 2015-2021 the original author or authors.
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
package org.lastaflute.db.jta.romanticist;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.dbflute.Entity;
import org.dbflute.bhv.core.BehaviorCommand;
import org.dbflute.bhv.core.BehaviorCommandMeta;
import org.dbflute.dbmeta.DBMeta;
import org.dbflute.util.DfCollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 * @since 0.7.2 (2015/12/22 Tuesday)
 */
public class TransactionSavedRecentResult {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(TransactionSavedRecentResult.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final long statementNo;
    protected final String tableName; // not null
    protected final String command; // not null
    protected final Long beginMillis; // null allowed when failure
    protected final Long endMillis; // null allowed when failure
    protected final Class<?> resultType; // not null e.g. Integer, Entity, List
    protected final Map<String, Object> resultMap; // not null, empty allowed

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public TransactionSavedRecentResult(long statementNo, String tableName, String command, Long beginMillis, Long endMillis,
            Class<?> resultType, Object resultValue, BehaviorCommandMeta meta) {
        // meta is not saved because of internal object (also may have update data)
        this.statementNo = statementNo;
        this.tableName = tableName;
        this.command = command;
        this.beginMillis = beginMillis;
        this.endMillis = endMillis;
        this.resultType = resultType;
        this.resultMap = convertToResultMap(resultValue, meta);
    }

    protected Map<String, Object> convertToResultMap(Object resultValue, BehaviorCommandMeta meta) {
        try {
            return doConvertToResultMap(resultValue, meta);
        } catch (RuntimeException continued) { // just in case
            logger.info("Failed to convert the resultValue to resultMap: value={} meta={}", resultValue, meta, continued);
            return Collections.emptyMap();
        }
    }

    protected Map<String, Object> doConvertToResultMap(Object resultValue, BehaviorCommandMeta meta) {
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
            } else { // e.g. Integer
                final String valueTitle = deriveOtherTypeValueTitle(meta);
                resultMap = new LinkedHashMap<String, Object>(2);
                resultMap.put(valueTitle, resultValue);
                if (meta.isEntityUpdateFamily()) {
                    resultMap.put("key", prepareEntityUpdateKeyMap(meta));
                }
            }
        } else { // e.g. selectCursor()
            resultMap = Collections.emptyMap();
        }
        return resultMap;
    }

    // ===================================================================================
    //                                                                       Entity Result
    //                                                                       =============
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
    //                                                                          Other Type
    //                                                                          ==========
    protected String deriveOtherTypeValueTitle(BehaviorCommandMeta meta) {
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
        return valueTitle;
    }

    // ===================================================================================
    //                                                                       Entity Update
    //                                                                       =============
    protected Map<String, Object> prepareEntityUpdateKeyMap(BehaviorCommandMeta meta) {
        final Entity entity = extractArgumentEntity(meta); // always can get if entity update
        if (entity == null) { // no way, just in case
            return Collections.emptyMap();
        }
        final DBMeta dbmeta = entity.asDBMeta();
        final Map<String, Object> keyMap;
        final Set<String> uniqueProps = entity.myuniqueDrivenProperties();
        if (!uniqueProps.isEmpty()) {
            final Map<String, Object> uniqueMap = uniqueProps.stream().map(prop -> {
                return dbmeta.findColumnInfo(prop);
            }).collect(Collectors.toMap(col -> col.getColumnDbName(), col -> col.read(entity)));
            keyMap = uniqueMap;
        } else if (dbmeta.hasPrimaryKey() && entity.hasPrimaryKeyValue()) {
            keyMap = dbmeta.extractPrimaryKeyMap(entity);
        } else { // no way if entity update, just in case
            keyMap = Collections.emptyMap();
        }
        return keyMap;
    }

    protected Entity extractArgumentEntity(BehaviorCommandMeta meta) {
        if (!(meta instanceof BehaviorCommand<?>)) { // no way, just in case
            return null;
        }
        final Object[] args = ((BehaviorCommand<?>) meta).getSqlExecutionArgument();
        if (args == null || args.length == 0) {
            return null;
        }
        final Object firstArg = args[0]; // update family commands have entity as the first argument
        if (firstArg instanceof Entity) {
            return (Entity) firstArg;
        }
        if (args.length >= 2) {
            final Object secondArg = args[1]; // just in case, to follow internal change
            if (secondArg instanceof Entity) {
                return (Entity) secondArg;
            }
        }
        return null;
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
