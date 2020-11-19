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
package org.lastaflute.web.ruts.inoutlogging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTraceViewUtil;
import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.Srl;
import org.lastaflute.core.magic.async.ConcurrentAsyncCall;
import org.lastaflute.core.mail.RequestedMailCount;
import org.lastaflute.core.remoteapi.RequestedRemoteApiCount;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.db.dbflute.callbackcontext.traceablesql.RequestedSqlCount;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.servlet.request.RequestManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 * @author awaawa
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
        if (isExceptLogging(requestManager, runtime, keeper)) { // e.g. HealthcheckAction
            return;
        }
        // no async for now, because this process is after response committed by jflute (2017/08/11)
        try {
            doShowInOutLog(requestManager, runtime, keeper);
        } catch (RuntimeException continued) { // not main process
            logger.info("*Failed to show in-out log: " + runtime.getRequestPath(), continued);
        }
    }

    protected boolean isExceptLogging(RequestManager requestManager, ActionRuntime runtime, InOutLogKeeper keeper) {
        return keeper.getOption().getLoggingExceptDeterminer().map(determiner -> determiner.test(runtime)).orElse(false);
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
        setupBasic(sb, requestManager, runtime, keeper);
        setupBegin(sb, keeper);
        setupPerformance(sb, requestManager, keeper);
        setupProcess(sb, keeper);
        setupCaller(sb, requestManager, keeper);
        setupCause(sb, runtime, keeper);

        // in-out data here
        boolean alreadyLineSep = false;

        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // Request: requestHeader, requestParameter, requestBody
        // _/_/_/_/_/_/_/_/_/_/
        final String requestHeaderExp = buildRequestHeaderExp(keeper, option);
        if (requestHeaderExp != null) {
            final String title = "requestHeader";
            final String realExp = requestHeaderExp;
            alreadyLineSep = buildInOutRequest(sb, title, realExp, alreadyLineSep, keeper);
        }
        final String paramsExp = buildRequestParameterExp(keeper, option);
        if (paramsExp != null) {
            final String title = "requestParameter";
            final String realExp = option.getRequestParameterFilter().map(filter -> filter.apply(paramsExp)).orElse(paramsExp);
            alreadyLineSep = buildInOutRequest(sb, title, realExp, alreadyLineSep, keeper);
        }
        if (keeper.getRequestBodyContent().isPresent()) {
            final String body = keeper.getRequestBodyContent().get();
            final String title = "requestBody(" + keeper.getRequestBodyType().orElse("unknown") + ")";
            final String realExp = option.getRequestBodyFilter().map(filter -> filter.apply(body)).orElse(body);
            alreadyLineSep = buildInOutRequest(sb, title, realExp, alreadyLineSep, keeper);
        }

        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // Response: responseHeader, responseBody
        // _/_/_/_/_/_/_/_/_/_/
        final String responseHeaderExp = buildResponseHeaderExp(keeper);
        if (responseHeaderExp != null) {
            final String title = "responseHeader";
            final String realExp = responseHeaderExp;
            alreadyLineSep = buildInOutResponsePlus(sb, title, realExp, alreadyLineSep);
        }
        if (!keeper.getOption().isSuppressResponseBody()) {
            if (keeper.getResponseBodyContent().isPresent()) {
                final String body = keeper.getResponseBodyContent().get();
                final String title = "responseBody(" + keeper.getResponseBodyType().orElse("unknown") + ")";
                final String realExp = option.getResponseBodyFilter().map(filter -> filter.apply(body)).orElse(body);
                alreadyLineSep = buildInOutResponsePlus(sb, title, realExp, alreadyLineSep);
            }
        }

        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // Various: sqlCount, mailCount, remoteApiCount
        // _/_/_/_/_/_/_/_/_/_/
        final OptionalThing<RequestedSqlCount> optSql =
                requestManager.getAttribute(LastaWebKey.DBFLUTE_SQL_COUNT_KEY, RequestedSqlCount.class);
        if (optSql.isPresent()) {
            final RequestedSqlCount count = optSql.get();
            if (count.getTotalCountOfSql() > 0) {
                alreadyLineSep = buildInOutResponsePlus(sb, "sqlCount", count.toString(), alreadyLineSep);
            }
        }
        final OptionalThing<RequestedMailCount> optMail =
                requestManager.getAttribute(LastaWebKey.MAILFLUTE_MAIL_COUNT_KEY, RequestedMailCount.class);
        if (optMail.isPresent()) {
            final RequestedMailCount count = optMail.get();
            if (count.getCountOfPosting() > 0) {
                alreadyLineSep = buildInOutResponsePlus(sb, "mailCount", count.toString(), alreadyLineSep);
            }
        }
        final OptionalThing<RequestedRemoteApiCount> optRemoteApi =
                requestManager.getAttribute(LastaWebKey.REMOTEAPI_COUNT_KEY, RequestedRemoteApiCount.class);
        if (optRemoteApi.isPresent()) {
            final RequestedRemoteApiCount count = optRemoteApi.get();
            if (!count.getFacadeCountMap().isEmpty()) {
                alreadyLineSep = buildInOutResponsePlus(sb, "remoteApiCount", count.toString(), alreadyLineSep);
            }
        }

        return sb.toString();
    }

    // ===================================================================================
    //                                                                         Setup Parts
    //                                                                         ===========
    protected void setupBasic(StringBuilder sb, RequestManager requestManager, ActionRuntime runtime, InOutLogKeeper keeper) {
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

    protected LocalDateTime flashDateTime(RequestManager requestManager) { // flash not to depends on transaction
        final TimeManager timeManager = requestManager.getTimeManager();
        return DfTypeUtil.toLocalDateTime(timeManager.flashDate(), timeManager.getBusinessTimeZone());
    }

    protected void setupProcess(StringBuilder sb, InOutLogKeeper keeper) {
        keeper.getProcessHash().ifPresent(hash -> { // basically present
            sb.append(" #").append(hash);
        }); // no else because of sub item
    }

    protected void setupCaller(StringBuilder sb, RequestManager requestManager, InOutLogKeeper keeper) {
        requestManager.getHeaderUserAgent().ifPresent(userAgent -> {
            sb.append(" caller:{").append(Srl.cut(userAgent, 50, "...")).append("}"); // may be too big so cut
        });
    }

    protected void setupCause(StringBuilder sb, ActionRuntime runtime, InOutLogKeeper keeper) {
        final RuntimeException failureCause = runtime.getFailureCause();
        if (failureCause != null) {
            doSetupCause(sb, failureCause);
        } else { // application does not have exception but framework may have...
            keeper.getFrameworkCause().ifPresent(frameworkCause -> {
                if (frameworkCause instanceof ServletException) {
                    final Throwable rootCause = ((ServletException) frameworkCause).getRootCause();
                    if (rootCause != null) { // the servlet exception is simple wrapper
                        doSetupCause(sb, rootCause);
                    } else { // the servlet exception is main exception
                        doSetupCause(sb, frameworkCause);
                    }
                } else { // basically framework's runtime exception or IO exception
                    doSetupCause(sb, frameworkCause);
                }
            });
        }
    }

    protected void doSetupCause(StringBuilder sb, Throwable cause) {
        sb.append(" *").append(cause.getClass().getSimpleName());
        sb.append(" #").append(Integer.toHexString(cause.hashCode()));
    }

    // ===================================================================================
    //                                                                    Build Expression
    //                                                                    ================
    // -----------------------------------------------------
    //                                        Request Header
    //                                        --------------
    protected String buildRequestHeaderExp(InOutLogKeeper keeper, InOutLogOption option) {
        return keeper.getRequestHeaderMapProvider().map(provider -> buildMapExp(provider.get())).orElse(null);
    }

    // -----------------------------------------------------
    //                                     Request Parameter
    //                                     -----------------
    protected String buildRequestParameterExp(InOutLogKeeper keeper, InOutLogOption option) {
        return doBuildRequestParameterExp(keeper.getRequestParameterMap(), option);
    }

    protected String doBuildRequestParameterExp(Map<String, Object> requestParameterMap, InOutLogOption option) {
        return buildMapExp(filterRequestParameterMap(requestParameterMap, option));
    }

    protected Map<String, Object> filterRequestParameterMap(Map<String, Object> requestParameterMap, InOutLogOption option) {
        final Map<String, Object> realMap = option.getRequestParameterValueFilter().map(filter -> {
            if (requestParameterMap.isEmpty()) {
                return requestParameterMap;
            }
            final Map<String, Object> filteredMap = new LinkedHashMap<>(requestParameterMap.size());
            requestParameterMap.forEach((key, value) -> {
                final Object filteredValue = filter.apply(new InOutValueEntry(key, value));
                filteredMap.put(key, filteredValue != null ? filteredValue : value);
            });
            return filteredMap;
        }).orElse(requestParameterMap);
        return realMap;
    }

    // -----------------------------------------------------
    //                                    Response Parameter
    //                                    ------------------
    protected String buildResponseHeaderExp(InOutLogKeeper keeper) {
        return keeper.getResponseHeaderMapProvider().map(provider -> buildMapExp(provider.get())).orElse(null);
    }

    // -----------------------------------------------------
    //                                   Building Expression
    //                                   -------------------
    protected String buildMapExp(Map<String, Object> parameterMap) {
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
                doBuildMapExpObjectArray(sb, value);
            } else if (value instanceof List<?>) {
                doBuildMapExpList(sb, value);
            } else {
                doBuildMapExpScalar(sb, value);
            }
        });
        sb.insert(0, "{").append("}");
        return sb.toString();
    }

    protected void doBuildMapExpObjectArray(StringBuilder sb, Object value) {
        final Object[] objArray = (Object[]) value;
        if (objArray.length == 1) {
            sb.append(objArray[0]); // request parameter may have one parameter as array
        } else {
            int index = 0;
            sb.append("[");
            for (Object obj : objArray) {
                if (index > 0) {
                    sb.append(", ");
                }
                doBuildMapExpScalar(sb, obj);
                ++index;
            }
            sb.append("]");
        }
    }

    protected void doBuildMapExpList(StringBuilder sb, Object value) {
        @SuppressWarnings("unchecked")
        final List<Object> objList = (List<Object>) value;
        if (objList.size() == 1) {
            sb.append(objList.get(0));
        } else {
            sb.append(objList.toString()); // e.g. [sea, land]
        }
    }

    protected void doBuildMapExpScalar(StringBuilder sb, Object value) {
        sb.append(value);
    }

    // ===================================================================================
    //                                                                         Build InOut
    //                                                                         ===========
    protected boolean buildInOutRequest(StringBuilder sb, String title, String value, boolean alreadyLineSep, InOutLogKeeper keeper) {
        String noSepDelim = " "; // as default (if same line show)
        if (willBeLineSeparatedLater(keeper) && !value.contains("\n") && !alreadyLineSep) {
            sb.append("\n"); // line-separate request beginning point for view
            noSepDelim = "";
        }
        final boolean nextLineSep = doBuildInOut(sb, title, value, alreadyLineSep, noSepDelim);
        return noSepDelim.isEmpty() || nextLineSep; // empty means already line-separated here
    }

    protected boolean willBeLineSeparatedLater(InOutLogKeeper keeper) {
        return keeper.getResponseBodyContent().filter(body -> { // response body may have line separator
            return !keeper.getOption().isSuppressResponseBody() && body.contains("\n");
        }).isPresent();
    }

    protected boolean buildInOutResponsePlus(StringBuilder sb, String title, String value, boolean alreadyLineSep) {
        return doBuildInOut(sb, title, value, alreadyLineSep, " ");
    }

    protected boolean doBuildInOut(StringBuilder sb, String title, String value, boolean alreadyLineSep, String noSepDelim) {
        final boolean nextLineSep;
        if (value != null && value.contains("\n")) {
            sb.append("\n").append(title).append(":").append("\n");
            nextLineSep = true;
        } else {
            sb.append(alreadyLineSep ? "\n" : noSepDelim).append(title).append(":");
            nextLineSep = alreadyLineSep;
        }
        sb.append(value == null || !value.isEmpty() ? value : "(empty)");
        return nextLineSep;
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

    // ===================================================================================
    //                                                                         Value Entry
    //                                                                         ===========
    public static class InOutValueEntry {

        protected final String key;
        protected final Object value;

        public InOutValueEntry(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }
    }
}
