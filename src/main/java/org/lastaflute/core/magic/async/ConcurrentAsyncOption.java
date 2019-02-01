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
package org.lastaflute.core.magic.async;

/**
 * @author jflute
 */
public class ConcurrentAsyncOption {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected ConcurrentAsyncInheritType behaviorCommandHookType;
    protected ConcurrentAsyncInheritType sqlFireHookType;
    protected ConcurrentAsyncInheritType sqlLogHandlerType;
    protected ConcurrentAsyncInheritType sqlResultHandlerType;
    protected ConcurrentAsyncInheritType sqlStringFilterType;

    public enum ConcurrentAsyncInheritType {
        INHERIT, SEPARATE
    }

    // ===================================================================================
    //                                                                       Determination
    //                                                                       =============
    // -----------------------------------------------------
    //                                               Inherit
    //                                               -------
    public boolean isInheritBehaviorCommandHook() {
        return determineInheritType(behaviorCommandHookType, ConcurrentAsyncInheritType.INHERIT);
    }

    public boolean isInheritSqlFireHookType() {
        return determineInheritType(sqlFireHookType, ConcurrentAsyncInheritType.INHERIT);
    }

    public boolean isInheritSqlLogHandlerType() {
        return determineInheritType(sqlLogHandlerType, ConcurrentAsyncInheritType.INHERIT);
    }

    public boolean isInheritSqlResultHandlerType() {
        return determineInheritType(sqlResultHandlerType, ConcurrentAsyncInheritType.INHERIT);
    }

    public boolean isInheritSqlStringFilterType() {
        return determineInheritType(sqlStringFilterType, ConcurrentAsyncInheritType.INHERIT);
    }

    // -----------------------------------------------------
    //                                              Separate
    //                                              --------
    public boolean isSeparateBehaviorCommandHook() {
        return determineInheritType(behaviorCommandHookType, ConcurrentAsyncInheritType.SEPARATE);
    }

    public boolean isSeparateSqlFireHookType() {
        return determineInheritType(sqlFireHookType, ConcurrentAsyncInheritType.SEPARATE);
    }

    public boolean isSeparateSqlLogHandlerType() {
        return determineInheritType(sqlLogHandlerType, ConcurrentAsyncInheritType.SEPARATE);
    }

    public boolean isSeparateSqlResultHandlerType() {
        return determineInheritType(sqlResultHandlerType, ConcurrentAsyncInheritType.SEPARATE);
    }

    public boolean isSeparateSqlStringFilterType() {
        return determineInheritType(sqlStringFilterType, ConcurrentAsyncInheritType.SEPARATE);
    }

    protected boolean determineInheritType(ConcurrentAsyncInheritType inheritType, ConcurrentAsyncInheritType targetType) {
        return inheritType != null && inheritType.equals(targetType);
    }

    // ===================================================================================
    //                                                                      Option Setting
    //                                                                      ==============
    // -----------------------------------------------------
    //                                               Inherit
    //                                               -------
    public ConcurrentAsyncOption inheritBehaviorCommandHook() {
        behaviorCommandHookType = ConcurrentAsyncInheritType.INHERIT;
        return this;
    }

    public ConcurrentAsyncOption inheritSqlFireHook() {
        sqlFireHookType = ConcurrentAsyncInheritType.INHERIT;
        return this;
    }

    public ConcurrentAsyncOption inheritSqlLogHandler() {
        sqlLogHandlerType = ConcurrentAsyncInheritType.INHERIT;
        return this;
    }

    public ConcurrentAsyncOption inheritSqlResultHandler() {
        sqlResultHandlerType = ConcurrentAsyncInheritType.INHERIT;
        return this;
    }

    public ConcurrentAsyncOption inheritSqlStringFilter() {
        sqlStringFilterType = ConcurrentAsyncInheritType.INHERIT;
        return this;
    }

    // -----------------------------------------------------
    //                                              Separate
    //                                              --------
    public ConcurrentAsyncOption separateBehaviorCommandHook() {
        behaviorCommandHookType = ConcurrentAsyncInheritType.SEPARATE;
        return this;
    }

    public ConcurrentAsyncOption separateSqlFireHook() {
        sqlFireHookType = ConcurrentAsyncInheritType.SEPARATE;
        return this;
    }

    public ConcurrentAsyncOption separateSqlLogHandler() {
        sqlLogHandlerType = ConcurrentAsyncInheritType.SEPARATE;
        return this;
    }

    public ConcurrentAsyncOption separateSqlResultHandler() {
        sqlResultHandlerType = ConcurrentAsyncInheritType.SEPARATE;
        return this;
    }

    public ConcurrentAsyncOption separateSqlStringFilter() {
        sqlStringFilterType = ConcurrentAsyncInheritType.SEPARATE;
        return this;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (behaviorCommandHookType != null) {
            sb.append(sb.length() > 0 ? ", " : "").append("behaviorCommandHook=").append(behaviorCommandHookType);
        }
        if (sqlFireHookType != null) {
            sb.append(sb.length() > 0 ? ", " : "").append("sqlFireHook=").append(sqlFireHookType);
        }
        if (sqlLogHandlerType != null) {
            sb.append(sb.length() > 0 ? ", " : "").append("sqlLogHandler=").append(sqlLogHandlerType);
        }
        if (sqlResultHandlerType != null) {
            sb.append(sb.length() > 0 ? ", " : "").append("sqlResultHandler=").append(sqlResultHandlerType);
        }
        if (sqlStringFilterType != null) {
            sb.append(sb.length() > 0 ? ", " : "").append("sqlStringFilter=").append(sqlStringFilterType);
        }
        if (sb.length() == 0) {
            sb.append("no option");
        }
        sb.insert(0, "{").append("}");
        return sb.toString();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public ConcurrentAsyncInheritType getBehaviorCommandHookType() {
        return behaviorCommandHookType;
    }

    public ConcurrentAsyncInheritType getSqlFireHookType() {
        return sqlFireHookType;
    }

    public ConcurrentAsyncInheritType getSqlLogHandlerType() {
        return sqlLogHandlerType;
    }

    public ConcurrentAsyncInheritType getSqlResultHandlerType() {
        return sqlResultHandlerType;
    }

    public ConcurrentAsyncInheritType getSqlStringFilterType() {
        return sqlStringFilterType;
    }
}
