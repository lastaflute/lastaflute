/*
 * Copyright 2015-2017 the original author or authors.
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
package org.lastaflute.web.ruts.inoutlogging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTraceViewUtil;
import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.Srl;
import org.lastaflute.core.magic.async.ConcurrentAsyncCall;
import org.lastaflute.core.mail.RequestedMailCount;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.db.dbflute.callbackcontext.traceablesql.RequestedSqlCount;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.servlet.request.RequestManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 * @since 1.0.0 (2017/08/11 Friday)
 */
public class InOutLogger {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    public static final String LOGGER_NAME = "lastaflute.inout";
    protected static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
    protected static final DateTimeFormatter beginTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // ===================================================================================
    //                                                                             Logging
    //                                                                             =======
    protected void log(String msg) { // define at top for small line number
        logger.info(msg);
    }

    public static boolean isLoggerEnabled() { // used by keeper's determination
        return logger.isInfoEnabled();
    }

    // ===================================================================================
    //                                                                               Show
    //                                                                              ======
    public void show(RequestManager requestManager, ActionRuntime runtime, InOutLogKeeper keeper) {
        if (!isLoggerEnabled()) { // e.g. option is true but no logger settings
            return;
        }
        // no async for now, because this process is after response committed by jflute (2017/08/11)
        try {
            doShowInOutLog(requestManager, runtime, keeper);
        } catch (RuntimeException continued) { // not main process
            logger.info("*Failed to show in-out log: " + runtime.getRequestPath(), continued);
        }
    }

    protected void doShowInOutLog(RequestManager requestManager, ActionRuntime runtime, InOutLogKeeper keeper) {
        final String whole = buildWhole(requestManager, runtime, keeper);
        if (keeper.getOption().isAsync()) {
            asyncShow(requestManager, whole);
        } else { // basically here
            log(whole); // also no wait because of after writing response (except redirection)
        }
    }

    // ===================================================================================
    //                                                                         Build Whole
    //                                                                         ===========
    protected String buildWhole(RequestManager requestManager, ActionRuntime runtime, InOutLogKeeper keeper) {
        final InOutLogOption option = keeper.getOption();
        final StringBuilder sb = new StringBuilder();
        setupBasic(sb, requestManager, runtime);
        setupBegin(sb, keeper);
        setupPerformance(sb, requestManager, keeper);
        setupProcess(sb, keeper);
        setupUserAgent(sb, requestManager);
        setupCause(sb, runtime);

        // in-out data here
        boolean alreadyLineSep = false;

        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // Request: requestParameter, requestBody
        // _/_/_/_/_/_/_/_/_/_/
        final String paramsExp = buildRequestParameterExp(keeper);
        if (paramsExp != null) {
            if (willBeLineSeparatedLater(keeper) && !paramsExp.contains("\n") && !alreadyLineSep) {
                sb.append("\n"); // line-separate request beginning point for view
                alreadyLineSep = true;
            }
            final String realExp = option.getRequestParameterFilter().map(filter -> filter.apply(paramsExp)).orElse(paramsExp);
            alreadyLineSep = buildInOut(sb, "requestParameter", realExp, alreadyLineSep);
        }
        if (keeper.getRequestBodyContent().isPresent()) {
            final String body = keeper.getRequestBodyContent().get();
            final String title = "requestBody(" + keeper.getRequestBodyType().orElse("unknown") + ")";
            final String realExp = option.getRequestBodyFilter().map(filter -> filter.apply(body)).orElse(body);
            alreadyLineSep = buildInOut(sb, title, realExp, alreadyLineSep);
        }

        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // Response: responseBody
        // _/_/_/_/_/_/_/_/_/_/
        if (!keeper.getOption().isSuppressResponseBody()) {
            if (keeper.getResponseBodyContent().isPresent()) {
                final String body = keeper.getResponseBodyContent().get();
                final String title = "responseBody(" + keeper.getResponseBodyType().orElse("unknown") + ")";
                final String realExp = option.getResponseBodyFilter().map(filter -> filter.apply(body)).orElse(body);
                alreadyLineSep = buildInOut(sb, title, realExp, alreadyLineSep);
            }
        }

        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // Various: sqlCount, mailCount
        // _/_/_/_/_/_/_/_/_/_/
        final OptionalThing<RequestedSqlCount> optSql =
                requestManager.getAttribute(LastaWebKey.DBFLUTE_SQL_COUNT_KEY, RequestedSqlCount.class);
        if (optSql.isPresent()) {
            final RequestedSqlCount count = optSql.get();
            if (count.getTotalCountOfSql() > 0) {
                alreadyLineSep = buildInOut(sb, "sqlCount", count.toString(), alreadyLineSep);
            }
        }
        final OptionalThing<RequestedMailCount> optMail =
                requestManager.getAttribute(LastaWebKey.MAILFLUTE_MAIL_COUNT_KEY, RequestedMailCount.class);
        if (optMail.isPresent()) {
            final RequestedMailCount count = optMail.get();
            if (count.getCountOfPosting() > 0) {
                alreadyLineSep = buildInOut(sb, "mailCount", count.toString(), alreadyLineSep);
            }
        }

        return sb.toString();
    }

    protected void setupBasic(StringBuilder sb, RequestManager requestManager, ActionRuntime runtime) {
        final String requestPath = requestManager.getRequestPath();
        final String httpMethod = requestManager.getHttpMethod().orElse("unknown");
        sb.append(httpMethod).append(" ").append(requestPath);
        // not use HTTP status because of not fiexed yet here when e.g. exception
        // (and in-out logging is not access log and you can derive it by exception type)
        //requestManager.getResponseManager().getResponse().getStatus();
        final String actionName = runtime.getActionType().getSimpleName();
        final String methodName = runtime.getActionExecute().getExecuteMethod().getName();
        sb.append(" ").append(actionName).append("@").append(methodName).append("()");
    }

    protected void setupBegin(StringBuilder sb, InOutLogKeeper keeper) {
        final String beginExp = keeper.getBeginDateTime().map(begin -> {
            return beginTimeFormatter.format(begin);
        }).orElse("no begun"); // basically no way, just in case
        sb.append(" (").append(beginExp).append(")");
    }

    protected void setupPerformance(StringBuilder sb, RequestManager requestManager, InOutLogKeeper keeper) {
        final String performanceCost = keeper.getBeginDateTime().map(begin -> {
            final long before = DfTypeUtil.toDate(begin).getTime();
            final long after = DfTypeUtil.toDate(flashDateTime(requestManager)).getTime();
            return DfTraceViewUtil.convertToPerformanceView(after - before);
        }).orElse("no ended");
        sb.append(" [").append(performanceCost).append("]");
    }

    protected void setupProcess(StringBuilder sb, InOutLogKeeper keeper) {
        keeper.getProcessHash().ifPresent(hash -> { // basically present
            sb.append(" #").append(hash);
        }); // no else because of sub item
    }

    protected void setupUserAgent(StringBuilder sb, RequestManager requestManager) {
        requestManager.getHeaderUserAgent().ifPresent(userAgent -> {
            sb.append(" userAgent:{").append(Srl.cut(userAgent, 50, "...")).append("}"); // may be too big so cut
        });
    }

    protected void setupCause(StringBuilder sb, ActionRuntime runtime) {
        final RuntimeException failureCause = runtime.getFailureCause();
        if (failureCause != null) {
            sb.append(" *").append(failureCause.getClass().getSimpleName());
            sb.append(" #").append(Integer.toHexString(failureCause.hashCode()));
        }
    }

    protected boolean willBeLineSeparatedLater(InOutLogKeeper keeper) {
        return keeper.getResponseBodyContent().filter(body -> { // response body may have line separator
            return !keeper.getOption().isSuppressResponseBody() && body.contains("\n");
        }).isPresent();
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected LocalDateTime flashDateTime(RequestManager requestManager) { // flash not to depends on transaction
        final TimeManager timeManager = requestManager.getTimeManager();
        return DfTypeUtil.toLocalDateTime(timeManager.flashDate(), timeManager.getBusinessTimeZone());
    }

    protected String buildRequestParameterExp(InOutLogKeeper keeper) {
        final Map<String, Object> parameterMap = keeper.getRequestParameterMap();
        if (parameterMap.isEmpty()) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        parameterMap.forEach((key, value) -> {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(key).append("=");
            if (value instanceof Object[]) {
                final Object[] objArray = (Object[]) value;
                if (objArray.length == 1) {
                    sb.append(objArray[0]);
                } else {
                    int index = 0;
                    sb.append("[");
                    for (Object obj : objArray) {
                        if (index > 0) {
                            sb.append(", ");
                        }
                        sb.append(obj);
                        ++index;
                    }
                    sb.append("]");
                }
            } else {
                sb.append(value);
            }
        });
        sb.insert(0, "{").append("}");
        return sb.toString();
    }

    protected boolean buildInOut(StringBuilder sb, String title, String value, boolean alreadyLineSep) {
        boolean nowLineSep = alreadyLineSep;
        if (value != null && value.contains("\n")) {
            sb.append("\n").append(title).append(":").append("\n");
            nowLineSep = true;
        } else {
            sb.append(alreadyLineSep ? "\n" : " ").append(title).append(":");
        }
        sb.append(value == null || !value.isEmpty() ? value : "(empty)");
        return nowLineSep;
    }

    // ===================================================================================
    //                                                                        Asynchronous
    //                                                                        ============
    protected void asyncShow(RequestManager requestManager, String whole) {
        requestManager.getAsyncManager().async(new ConcurrentAsyncCall() {
            @Override
            public ConcurrentAsyncImportance importance() {
                return ConcurrentAsyncImportance.TERTIARY; // as low priority
            }

            @Override
            public void callback() {
                log(whole);
            }
        });
    }
}
