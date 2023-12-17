/*
 * Copyright 2015-2022 the original author or authors.
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
package org.lastaflute.db.dbcp;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.XAConnection;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.system.DBFluteSystem;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.jta.dbcp.ConnectionPool;
import org.lastaflute.jta.dbcp.ConnectionWrapper;
import org.lastaflute.jta.dbcp.impl.ConnectionWrapperImpl;

import jakarta.transaction.Transaction;

/**
 * @author jflute (originated in Seasar)
 * @since 0.6.5 (2015/11/03 Tuesday at the front of showbase)
 */
public class HookedConnectionWrapper extends ConnectionWrapperImpl {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                          Check Out/In
    //                                          ------------
    // all null allowed, overridden many times
    protected String checkingOutRequestPath; // key item
    protected String checkingOutEntryExp;
    protected String checkingOutUserExp;
    protected Long checkingOutMillis;
    protected String checkingInRequestPath; // key item
    protected String checkingInEntryExp;
    protected String checkingInUserExp;
    protected Long checkingInMillis;

    // -----------------------------------------------------
    //                                          Close Really
    //                                          ------------
    // all null allowed, only once be set
    protected String closingReallyRequestPath; // key item
    protected String closingReallyEntryExp;
    protected String closingReallyUserExp;
    protected Long closingReallyMillis;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public HookedConnectionWrapper(XAConnection xaConnection, Connection physicalConnection, ConnectionPool connectionPool, Transaction tx)
            throws SQLException {
        super(xaConnection, physicalConnection, connectionPool, tx);
    }

    // ===================================================================================
    //                                                                           Traceable
    //                                                                           =========
    // -----------------------------------------------------
    //                                          Save History
    //                                          ------------
    @Override
    public void saveCheckOutHistory() {
        if (ThreadCacheContext.exists()) { // e.g. in action
            checkingOutRequestPath = ThreadCacheContext.findRequestPath();
            checkingOutEntryExp = convertMethodToMethodExp(ThreadCacheContext.findEntryMethod());
            checkingOutUserExp = convertUserBeanToUserExp(ThreadCacheContext.findUserBean());
            checkingOutMillis = currentTimeMillis();
        }
        super.saveCheckOutHistory();
    }

    @Override
    public void saveCheckInHistory() {
        if (ThreadCacheContext.exists()) { // e.g. in action
            checkingInRequestPath = ThreadCacheContext.findRequestPath();
            checkingInEntryExp = convertMethodToMethodExp(ThreadCacheContext.findEntryMethod());
            checkingInUserExp = convertUserBeanToUserExp(ThreadCacheContext.findUserBean());
            checkingInMillis = currentTimeMillis();
        }
        super.saveCheckInHistory();
    }

    // -----------------------------------------------------
    //                                       Inherit History
    //                                       ---------------
    @Override
    public void inheritHistory(ConnectionWrapper wrapper) {
        super.inheritHistory(wrapper);
        if (wrapper instanceof HookedConnectionWrapper) {
            final HookedConnectionWrapper inherited = (HookedConnectionWrapper) wrapper;
            checkingOutRequestPath = inherited.checkingOutRequestPath;
            checkingOutEntryExp = inherited.checkingOutEntryExp;
            checkingOutUserExp = inherited.checkingOutUserExp;
            checkingOutMillis = inherited.checkingOutMillis;
            checkingInRequestPath = inherited.checkingInRequestPath;
            checkingInEntryExp = inherited.checkingInEntryExp;
            checkingInUserExp = inherited.checkingInUserExp;
            checkingInMillis = inherited.checkingInMillis;
            closingReallyRequestPath = inherited.closingReallyRequestPath;
            closingReallyEntryExp = inherited.closingReallyEntryExp;
            closingReallyUserExp = inherited.closingReallyUserExp;
            closingReallyMillis = inherited.closingReallyMillis;
        }
    }

    // -----------------------------------------------------
    //                                        Traceable View
    //                                        --------------
    @Override
    public String toTraceableView() {
        final String baseView = super.toTraceableView();
        final StringBuilder sb = new StringBuilder();
        sb.append(baseView); // same as toString()
        if (checkingOutRequestPath != null) {
            sb.append("\n latest checkOut(): ").append(checkingOutRequestPath).append(", ");
            sb.append(checkingOutEntryExp).append(", ").append(checkingOutUserExp).append(", ").append(checkingOutMillis);
        }
        if (checkingInRequestPath != null) {
            sb.append("\n latest checkIn(): ").append(checkingInRequestPath).append(", ");
            sb.append(checkingInEntryExp).append(", ").append(checkingInUserExp).append(", ").append(checkingInMillis);
        }
        if (closingReallyRequestPath != null) {
            sb.append("\n closeReally(): ").append(closingReallyRequestPath).append(", ");
            sb.append(closingReallyEntryExp).append(", ").append(closingReallyUserExp).append(", ").append(closingReallyMillis);
        }
        return sb.toString();
    }

    // -----------------------------------------------------
    //                                          Close Really
    //                                          ------------
    @Override
    public void closeReally() {
        saveClosingHistory();
        super.closeReally();
    }

    protected void saveClosingHistory() {
        if (ThreadCacheContext.exists()) { // e.g. in action
            closingReallyRequestPath = ThreadCacheContext.findRequestPath();
            closingReallyEntryExp = convertMethodToMethodExp(ThreadCacheContext.findEntryMethod());
            closingReallyUserExp = convertUserBeanToUserExp(ThreadCacheContext.findUserBean());
            closingReallyMillis = currentTimeMillis();
        }
        if (closingReallyRequestPath == null || closingReallyEntryExp == null) { // e.g. by timer
            final StackTraceElement[] stackTrace = new Exception().getStackTrace();
            final int requestCallerLevel = 1;
            if (closingReallyRequestPath == null && stackTrace != null && stackTrace.length > requestCallerLevel) {
                closingReallyRequestPath = buildCallerExp(requestCallerLevel, stackTrace[requestCallerLevel]);
            }
            final int entryCallerLevel = 5;
            if (closingReallyEntryExp == null && stackTrace != null && stackTrace.length > entryCallerLevel) {
                closingReallyEntryExp = buildCallerExp(entryCallerLevel, stackTrace[entryCallerLevel]);
            }
        }
    }

    protected String buildCallerExp(int requestCallerLevel, StackTraceElement caller) {
        return "caller" + requestCallerLevel + "::" + caller.getClassName() + "@" + caller.getMethodName() + "()";
    }

    // -----------------------------------------------------
    //                                         Assert Opened
    //                                         -------------
    @Override
    protected String buildAlreadyClosedMessage() { // no way but big trouble so rich view
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Already closed the connection.");
        br.addItem("Advice");
        br.addElement("You cannot use the closed connection. (no way)");
        br.addItem("Closing Request");
        br.addElement("requestPath: " + closingReallyRequestPath);
        br.addElement("entryMethod: " + closingReallyEntryExp);
        br.addElement("userBean: " + closingReallyUserExp);
        br.addElement("closingMillis: " + closingReallyMillis);
        br.addItem("Current Request");
        if (ThreadCacheContext.exists()) { // e.g. in action
            br.addElement("requestPath: " + ThreadCacheContext.findRequestPath());
            br.addElement("entryMethod: " + convertMethodToMethodExp(ThreadCacheContext.findEntryMethod()));
            br.addElement("userBean: " + convertUserBeanToUserExp(ThreadCacheContext.findUserBean()));
            br.addElement("currentMillis: " + currentTimeMillis());
        } else {
            br.addElement("*no info");
        }
        br.addItem("Latest CheckOut");
        br.addElement("requestPath: " + checkingOutRequestPath);
        br.addElement("entryMethod: " + checkingOutEntryExp);
        br.addElement("userBean: " + checkingOutUserExp);
        br.addElement("checkOutMillis: " + checkingOutMillis);
        br.addItem("Latest CheckIn");
        br.addElement("requestPath: " + checkingInRequestPath);
        br.addElement("entryMethod: " + checkingInEntryExp);
        br.addElement("userBean: " + checkingInUserExp);
        br.addElement("checkInMillis: " + checkingInMillis);
        br.addItem("XA Connection");
        br.addElement(xaConnection);
        br.addItem("Transaction");
        br.addElement(tx);
        br.addItem("Connection Wrapper");
        br.addElement(toString());
        return br.buildExceptionMessage();
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected String convertMethodToMethodExp(Method method) {
        if (method == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        final Class<?> returnType = method.getReturnType();
        sb.append(returnType != null ? returnType.getSimpleName() : "void"); // just in case
        sb.append(" ").append(method.getName()).append("(");
        final Class<?>[] parameterTypes = method.getParameterTypes();
        int parameterIndex = 0;
        for (Class<?> parameterType : parameterTypes) {
            if (parameterIndex > 0) {
                sb.append(", ");
            }
            sb.append(parameterType.getSimpleName());
            ++parameterIndex;
        }
        sb.append(")");
        return sb.toString();
    }

    protected String convertUserBeanToUserExp(Object userBean) {
        return userBean != null ? userBean.toString() : null;
    }

    protected long currentTimeMillis() {
        return DBFluteSystem.currentTimeMillis();
    }
}
