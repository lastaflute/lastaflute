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

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.transaction.xa.Xid;

import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.Srl;
import org.lastaflute.jta.dbcp.ConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class TransactionRomanticSnapshotBuilder {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(TransactionRomanticSnapshotBuilder.class);

    // ===================================================================================
    //                                                                            Romantic
    //                                                                            ========
    /**
     * @param tx The transaction it looks so romantic. (NotNull)
     * @param wrapper The wrapper of connection for the transaction to extract native process ID. (NotNull: but no check just in case)
     * @return The romantic expression for transaction snapshot. (NotNull)
     */
    public String buildRomanticSnapshot(RomanticTransaction tx, ConnectionWrapper wrapper) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{").append(currentElapsedTimeExp(tx));
        setupXidExp(sb, tx, wrapper);
        setupEntryMethodExp(sb, tx);
        setupUserBeanExp(sb, tx);
        setupTableCommandExp(sb, tx);
        setupRequestPathExp(sb, tx); // might have long query string so last
        sb.append("}@").append(toHexHashExp(tx)).append("@").append(toHexHashExp(wrapper));
        // SQL display has lines so close here
        setupCurrentSqlExp(sb, tx);
        return sb.toString();
    }

    protected String toHexHashExp(Object obj) {
        return obj != null ? Integer.toHexString(obj.hashCode()) : "null";
    }

    // ===================================================================================
    //                                                                        Elapsed Time
    //                                                                        ============
    protected String currentElapsedTimeExp(RomanticTransaction tx) {
        return tx.currentElapsedTimeExp();
    }

    // ===================================================================================
    //                                                                                XID
    //                                                                               =====
    protected void setupXidExp(StringBuilder sb, RomanticTransaction tx, ConnectionWrapper wrapper) {
        sb.append(", ");
        final Xid xid = tx.getXid();
        final byte[] globalId = xid.getGlobalTransactionId();
        final byte[] branchId = xid.getBranchQualifier();
        if (globalId != null) {
            sb.append(new String(globalId).trim());
        } else { // no way, just in case
            sb.append("*no globalId");
        }
        if (branchId != null && branchId.length > 0) {
            final String branchStr = new String(branchId).trim();
            if (!branchStr.isEmpty()) { // might be only spaces
                sb.append("-" + branchStr);
            }
        }
        final Object processId = extractDbmsNativeProcessId(tx, wrapper);
        if (processId != null) {
            sb.append("(").append(processId).append(")");
        }
    }

    protected Object extractDbmsNativeProcessId(RomanticTransaction tx, ConnectionWrapper wrapper) {
        if (wrapper == null) { // just in case
            return null;
        }
        final Connection physicalConn = wrapper.getPhysicalConnection();
        if (physicalConn == null) { // just in case
            return null;
        }
        try {
            return digUpDbmsNativeProcessIdOf(physicalConn);
        } catch (RuntimeException continued) {
            logger.debug("Failed to get the DBMS native process ID: " + physicalConn, continued);
        }
        return null;
    }

    protected Object digUpDbmsNativeProcessIdOf(Connection physicalConn) {
        final Class<?> connType = physicalConn.getClass();
        if (getMySQLConnectionClassFQCN().equals(connType.getName())) {
            final Method method = DfReflectionUtil.getPublicMethod(connType, "getId", (Class<?>[]) null);
            return DfReflectionUtil.invoke(method, physicalConn, (Object[]) null);
        }
        return null;
    }

    protected String getMySQLConnectionClassFQCN() {
        return "com.mysql.jdbc.JDBC4Connection";
    }

    // ===================================================================================
    //                                                                        Entry Method
    //                                                                        ============
    protected void setupEntryMethodExp(StringBuilder sb, RomanticTransaction tx) {
        final Method entryMethod = tx.getEntryMethod();
        if (entryMethod != null) {
            final String appPkg = getApplicationPackageKeyword();
            final String classExp = Srl.substringFirstRear(entryMethod.getDeclaringClass().getName(), appPkg);
            sb.append(", ").append(classExp).append("@").append(entryMethod.getName()).append("()");
        }
    }

    protected String getApplicationPackageKeyword() {
        return ".app.";
    }

    // ===================================================================================
    //                                                                           User Bean
    //                                                                           =========
    protected void setupUserBeanExp(StringBuilder sb, RomanticTransaction tx) {
        final Object userBean = tx.getUserBean();
        if (userBean != null) {
            sb.append(", ").append(userBean);
        }
    }

    // ===================================================================================
    //                                                                       Table Command
    //                                                                       =============
    protected void setupTableCommandExp(StringBuilder sb, RomanticTransaction tx) {
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
            sb.append(", ").append(mapSb.toString());
        }
    }

    // ===================================================================================
    //                                                                        Request Path
    //                                                                        ============
    protected void setupRequestPathExp(StringBuilder sb, RomanticTransaction tx) {
        final String requestPath = tx.getRequestPath();
        if (requestPath != null) {
            sb.append(", ").append(requestPath);
        }
    }

    // ===================================================================================
    //                                                                         Current SQL
    //                                                                         ===========
    protected void setupCurrentSqlExp(StringBuilder sb, RomanticTransaction tx) {
        final TransactionCurrentSqlBuilder currentSqlBuilder = tx.getCurrentSqlBuilder();
        if (currentSqlBuilder != null) {
            final String currentSql = currentSqlBuilder.buildSql();
            sb.append("\n/- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
            sb.append(" (SQL now: ");
            sb.append(tx.getCurrentTableName()).append("@").append(tx.getCurrentCommand());
            final Long currentSqlBeginMillis = tx.getCurrentSqlBeginMillis();
            if (currentSqlBeginMillis != null) {
                sb.append(" [").append(tx.buildElapsedTimeExp(currentSqlBeginMillis)).append("]");
            }
            sb.append(")\n");
            sb.append(currentSql);
            sb.append("\n- - - - - - - - - -/");
        }
    }
}
