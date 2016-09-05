/*
 * Copyright 2015-2016 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.dbflute.bhv.core.BehaviorCommandHook;
import org.dbflute.bhv.proposal.callback.ExecutedSqlCounter;
import org.dbflute.hook.AccessContext;
import org.dbflute.hook.AccessContext.AccessModuleProvider;
import org.dbflute.hook.AccessContext.AccessProcessProvider;
import org.dbflute.hook.AccessContext.AccessUserProvider;
import org.dbflute.hook.CallbackContext;
import org.dbflute.hook.SqlFireHook;
import org.dbflute.hook.SqlLogHandler;
import org.dbflute.hook.SqlResultHandler;
import org.dbflute.hook.SqlStringFilter;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.DfTraceViewUtil;
import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.Srl;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.direction.FwCoreDirection;
import org.lastaflute.core.exception.ExceptionTranslator;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.core.magic.ThreadCompleted;
import org.lastaflute.core.magic.async.ConcurrentAsyncOption.ConcurrentAsyncInheritType;
import org.lastaflute.core.magic.destructive.BowgunDestructiveAdjuster;
import org.lastaflute.core.mail.PostedMailCounter;
import org.lastaflute.db.dbflute.accesscontext.PreparedAccessContext;
import org.lastaflute.db.dbflute.callbackcontext.traceablesql.RomanticTraceableSqlFireHook;
import org.lastaflute.db.dbflute.callbackcontext.traceablesql.RomanticTraceableSqlResultHandler;
import org.lastaflute.db.dbflute.callbackcontext.traceablesql.RomanticTraceableSqlStringFilter;
import org.lastaflute.db.jta.romanticist.SavedTransactionMemories;
import org.lastaflute.db.jta.romanticist.TransactionMemoriesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class SimpleAsyncManager implements AsyncManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(SimpleAsyncManager.class);
    protected static final String LF = "\n";
    protected static final String EX_IND = "  "; // indent for exception message

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant director (AD) for framework. (NotNull: after initialization) */
    @Resource
    private FwAssistantDirector assistantDirector;

    /** The translator of exception. (NotNull: after initialization) */
    @Resource
    private ExceptionTranslator exceptionTranslator;

    /** The default option of asynchronous process. (NotNull: after initialization) */
    protected ConcurrentAsyncOption defaultConcurrentAsyncOption;

    /** The primary service of executor for asynchronous process. (NotNull: after initialization) */
    protected ExecutorService primaryExecutorService;

    /** The secondary service of executor for asynchronous process. (NotNull: after initialization) */
    protected ExecutorService secondaryExecutorService;

    /** The service of executor for waiting queue. (NullAllowed: lazy-loaded) */
    protected ExecutorService waitingQueueExecutorService;

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    /**
     * Initialize this component. <br>
     * This is basically called by DI setting file.
     */
    @PostConstruct
    public synchronized void initialize() {
        final FwCoreDirection direction = assistCoreDirection();
        final ConcurrentAsyncExecutorProvider provider = direction.assistConcurrentAsyncExecutorProvider();
        defaultConcurrentAsyncOption = provider != null ? provider.provideDefaultOption() : null;
        if (defaultConcurrentAsyncOption == null) {
            defaultConcurrentAsyncOption = new ConcurrentAsyncOption();
        }
        primaryExecutorService = createDefaultPrimaryExecutorService(provider);
        secondaryExecutorService = createDefaultSecondaryExecutorService(provider);
        showBootLogging();
    }

    protected FwCoreDirection assistCoreDirection() {
        return assistantDirector.assistCoreDirection();
    }

    protected ExecutorService createDefaultPrimaryExecutorService(ConcurrentAsyncExecutorProvider provider) {
        return createDefaultExecutorService(provider);
    }

    protected ExecutorService createDefaultSecondaryExecutorService(ConcurrentAsyncExecutorProvider provider) {
        return createDefaultExecutorService(provider);
    }

    // -----------------------------------------------------
    //                              Default Executor Service
    //                              ------------------------
    protected ExecutorService createDefaultExecutorService(ConcurrentAsyncExecutorProvider provider) {
        final int corePoolSize = 0;
        Integer maximumPoolSize = provider != null ? provider.provideMaxPoolSize() : null;
        if (maximumPoolSize == null) {
            maximumPoolSize = 10;
        }
        final long keepAliveTime = 60L;
        final TimeUnit seconds = TimeUnit.SECONDS;
        final BlockingQueue<Runnable> workQueue = createDefaultBlockingQueue();
        final RejectedExecutionHandler rejected = createRejectedExecutionHandler();
        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, seconds, workQueue, rejected);
    }

    protected BlockingQueue<Runnable> createDefaultBlockingQueue() {
        return new SynchronousQueue<Runnable>(); // waits when pool short
    }

    // -----------------------------------------------------
    //                            Rejected Execution Handler
    //                            --------------------------
    protected RejectedExecutionHandler createRejectedExecutionHandler() {
        return (runnable, executor) -> {
            handleRejectedExecution(runnable, executor);
        };
    }

    protected void handleRejectedExecution(final Runnable runnable, final ThreadPoolExecutor executor) { // caller thread
        if (logger.isDebugEnabled()) {
            logger.debug("#flow #async ...Registering the runnable to waiting queue as retry: " + runnable);
        }
        getWaitingQueueExecutorService().execute(() -> {
            try {
                retryPuttingQueue(runnable, executor);
            } catch (InterruptedException e) {
                final String torExp = buildExecutorHashExp(executor);
                logger.warn("*Failed to put the runnable to the executor" + torExp + "'s queue: " + runnable, e);
            }
        });
    }

    protected ExecutorService getWaitingQueueExecutorService() { // caller thread
        if (waitingQueueExecutorService != null) {
            return waitingQueueExecutorService;
        }
        synchronized (this) {
            if (waitingQueueExecutorService != null) {
                return waitingQueueExecutorService;
            }
            logger.info("#flow #async ...Creating the executor service for waiting queue.");
            waitingQueueExecutorService = newWaitingQueueExecutorService();
            return waitingQueueExecutorService;
        }
    }

    protected ExecutorService newWaitingQueueExecutorService() { // caller thread
        return Executors.newFixedThreadPool(2); // not only one just in case
    }

    protected void retryPuttingQueue(Runnable runnable, ThreadPoolExecutor executor) throws InterruptedException { // waiting queue thread
        if (logger.isDebugEnabled()) {
            final String torExp = buildExecutorHashExp(executor);
            logger.debug("#flow #async ...Retrying putting the runnable to the executor" + torExp + " in waiting queue: " + runnable);
        }
        executor.getQueue().put(runnable);
        if (logger.isDebugEnabled()) {
            final String torExp = buildExecutorHashExp(executor);
            logger.debug("#flow #async Success to retry putting the runnable to the executor" + torExp + " in waiting queue: " + runnable);
        }
    }

    protected String buildExecutorHashExp(ExecutorService executor) {
        return "@" + Integer.toHexString(executor.hashCode());
    }

    // -----------------------------------------------------
    //                                          Boot Logging
    //                                          ------------
    protected void showBootLogging() {
        if (logger.isInfoEnabled()) {
            logger.info("[Async Manager]");
            logger.info(" defaultConcurrentAsyncOption: " + defaultConcurrentAsyncOption);
            logger.info(" primaryExecutorService: " + buildExecutorNamedExp(primaryExecutorService));
            logger.info(" secondaryExecutorService: " + buildExecutorNamedExp(secondaryExecutorService));
        }
    }

    protected String buildExecutorNamedExp(ExecutorService executor) {
        return primaryExecutorService.getClass().getSimpleName() + buildExecutorHashExp(executor);
    }

    // ===================================================================================
    //                                                                  Asynchronous Entry
    //                                                                  ==================
    @Override
    public void async(ConcurrentAsyncCall noArgLambda) {
        assertThreadCallbackNotNull(noArgLambda);
        assertExecutorServiceValid();
        if (noArgLambda.asPrimary()) {
            doAsyncPrimary(noArgLambda);
        } else {
            doAsyncSecondary(noArgLambda);
        }
    }

    protected void doAsyncPrimary(ConcurrentAsyncCall callback) {
        actuallyAsync(callback, primaryExecutorService, "primary");
    }

    protected void doAsyncSecondary(ConcurrentAsyncCall callback) {
        actuallyAsync(callback, secondaryExecutorService, "secondary");
    }

    protected void actuallyAsync(ConcurrentAsyncCall callback, ExecutorService service, String title) {
        if (isDestructiveAsyncToNormalSync()) { // destructive (for e.g. UnitTest)
            destructiveNormalSync(callback);
        } else { // basically here
            final String keyword = title + buildExecutorHashExp(service);
            final Runnable task = createRunnable(callback, keyword);
            service.submit(task); // real asynchronous
        }
    }

    protected void destructiveNormalSync(ConcurrentAsyncCall callback) {
        if (logger.isInfoEnabled()) { // no way of production so INFO
            logger.info("#flow #async *Non-asynchronous by destructive adjuster, so executing as synchronous.");
        }
        // *not same state as real asynchronous, thread local values are different so dangerous
        callback.callback(); // normal synchronous
    }

    // ===================================================================================
    //                                                                     Create Runnable
    //                                                                     ===============
    protected Runnable createRunnable(ConcurrentAsyncCall call, String keyword) { // in caller thread
        final Map<String, Object> threadCacheMap = inheritThreadCacheContext(call);
        final AccessContext accessContext = inheritAccessContext(call);
        final CallbackContext callbackContext = inheritCallbackContext(call);
        final Map<String, Object> variousContextMap = findCallerVariousContextMap();
        return () -> { // in other thread
            prepareThreadCacheContext(call, threadCacheMap);
            preparePreparedAccessContext(call, accessContext);
            prepareCallbackContext(call, callbackContext);
            final Object variousPreparedObj = prepareVariousContext(call, variousContextMap);
            final long before = showRunning(keyword);
            Throwable cause = null;
            try {
                call.callback();
            } catch (Throwable e) {
                handleAsyncCallbackException(call, before, e);
                cause = e;
            } finally {
                showFinishing(keyword, before, cause); // should be before clearing because of using them
                clearVariousContext(call, variousContextMap, variousPreparedObj);
                clearCallbackContext(call);
                clearPreparedAccessContext(call);
                clearThreadCacheContext(call);
            }
        };
    }

    // ===================================================================================
    //                                                                           Show Call
    //                                                                           =========
    protected long showRunning(String keyword) {
        if (logger.isDebugEnabled()) {
            logger.debug("#flow #async ...Running asynchronous call as {}", keyword);
        }
        return System.currentTimeMillis();
    }

    protected void showFinishing(String keyword, long before, Throwable cause) {
        if (logger.isDebugEnabled()) {
            final long after = System.currentTimeMillis();
            final StringBuilder sb = new StringBuilder();
            sb.append("#flow #async ...Finishing asynchronous call as ").append(keyword).append(":");
            sb.append(LF).append("[Asynchronous Result]");
            sb.append(LF).append(" performanceView: ").append(toPerformanceView(before, after));
            extractSqlCount().ifPresent(counter -> {
                sb.append(LF).append(" sqlCount: ").append(counter.toLineDisp());
            });
            extractMailCount().ifPresent(counter -> {
                sb.append(LF).append(" mailCount: ").append(counter.toLineDisp());
            });
            if (cause != null) {
                sb.append(LF).append(" cause: ").append(cause.getClass().getSimpleName()).append(" *Read the exception message!");
            }
            logger.debug(sb.toString());
        }
    }

    protected String toPerformanceView(long before, long after) {
        return DfTraceViewUtil.convertToPerformanceView(after - before);
    }

    // ===================================================================================
    //                                                                        Thread Cache
    //                                                                        ============
    protected Map<String, Object> inheritThreadCacheContext(ConcurrentAsyncCall call) {
        return new HashMap<String, Object>(ThreadCacheContext.getReadOnlyCacheMap());
    }

    protected void prepareThreadCacheContext(ConcurrentAsyncCall call, Map<String, Object> threadCacheMap) {
        ThreadCacheContext.initialize();
        threadCacheMap.forEach((key, value) -> {
            if (value instanceof ThreadCompleted) { // cannot be inherited
                return;
            }
            ThreadCacheContext.setObject(key, value);
        });
    }

    protected void clearThreadCacheContext(ConcurrentAsyncCall call) {
        ThreadCacheContext.clear();
    }

    // ===================================================================================
    //                                                                       AccessContext
    //                                                                       =============
    protected AccessContext inheritAccessContext(ConcurrentAsyncCall call) {
        final AccessContext src = PreparedAccessContext.getAccessContextOnThread(); // null allowed
        if (src == null) {
            return null;
        }
        final AccessContext dest = newAccessContext();
        dest.setAccessDate(src.getAccessDate());
        dest.setAccessDateProvider(src.getAccessDateProvider());
        dest.setAccessTimestamp(src.getAccessTimestamp());
        dest.setAccessTimestampProvider(src.getAccessTimestampProvider());
        dest.setAccessLocalDate(src.getAccessLocalDate());
        dest.setAccessLocalDateProvider(src.getAccessLocalDateProvider());
        dest.setAccessLocalDateTime(src.getAccessLocalDateTime());
        dest.setAccessLocalDateTimeProvider(src.getAccessLocalDateTimeProvider());
        final String accessUser = src.getAccessUser();
        if (accessUser != null) {
            dest.setAccessUser(accessUser);
        } else {
            final AccessUserProvider accessUserProvider = src.getAccessUserProvider();
            if (accessUserProvider != null) {
                dest.setAccessUser(accessUserProvider.provideUser()); // fixed
            }
        }
        final String accessProcess = src.getAccessProcess();
        if (accessProcess != null) {
            dest.setAccessProcess(accessProcess);
        } else {
            final AccessProcessProvider accessProcessProvider = src.getAccessProcessProvider();
            if (accessProcessProvider != null) {
                dest.setAccessProcess(accessProcessProvider.provideProcess()); // fixed
            }
        }
        final String accessModule = src.getAccessModule();
        if (accessModule != null) {
            dest.setAccessModule(accessModule);
        } else {
            final AccessModuleProvider accessModuleProvider = src.getAccessModuleProvider();
            if (accessModuleProvider != null) {
                dest.setAccessModule(accessModuleProvider.provideModule()); // fixed
            }
        }
        final Map<String, Object> accessValueMap = src.getAccessValueMap();
        if (accessValueMap != null) {
            for (Entry<String, Object> entry : accessValueMap.entrySet()) {
                dest.registerAccessValue(entry.getKey(), entry.getValue());
            }
        }
        return dest;
    }

    protected AccessContext newAccessContext() {
        return new AccessContext();
    }

    protected void preparePreparedAccessContext(ConcurrentAsyncCall call, AccessContext accessContext) {
        if (accessContext != null) {
            PreparedAccessContext.setAccessContextOnThread(accessContext);
        }
    }

    protected void clearPreparedAccessContext(ConcurrentAsyncCall call) {
        PreparedAccessContext.clearAccessContextOnThread();
    }

    // ===================================================================================
    //                                                                     CallbackContext
    //                                                                     ===============
    protected CallbackContext inheritCallbackContext(ConcurrentAsyncCall call) {
        final CallbackContext src = CallbackContext.getCallbackContextOnThread(); // null allowed
        if (src == null) {
            return null;
        }
        final CallbackContext dest = newCallbackContext();
        final ConcurrentAsyncOption option = call.option();
        final ConcurrentAsyncOption defaultOption = defaultConcurrentAsyncOption;
        if (isInherit(option.getBehaviorCommandHookType(), defaultOption.getBehaviorCommandHookType())) {
            final BehaviorCommandHook hook = src.getBehaviorCommandHook();
            if (hook != null) {
                dest.setBehaviorCommandHook(hook);
            }
        }
        if (isInherit(option.getSqlFireHookType(), defaultOption.getSqlFireHookType())) {
            final SqlFireHook hook = src.getSqlFireHook();
            if (hook != null) {
                dest.setSqlFireHook(hook);
            }
        } else { // as default
            dest.setSqlFireHook(createDefaultSqlFireHook(call));
        }
        if (isInherit(option.getSqlLogHandlerType(), defaultOption.getSqlLogHandlerType())) {
            final SqlLogHandler handler = src.getSqlLogHandler();
            if (handler != null) {
                dest.setSqlLogHandler(handler);
            }
        }
        if (isInherit(option.getSqlResultHandlerType(), defaultOption.getSqlResultHandlerType())) {
            final SqlResultHandler handler = src.getSqlResultHandler();
            if (handler != null) {
                dest.setSqlResultHandler(handler);
            }
        } else {
            dest.setSqlResultHandler(createDefaultSqlResultHandler(call));
        }
        if (isInherit(option.getSqlStringFilterType(), defaultOption.getSqlStringFilterType())) {
            final SqlStringFilter filter = src.getSqlStringFilter();
            if (filter != null) {
                dest.setSqlStringFilter(filter);
            }
        } else { // as default
            dest.setSqlStringFilter(createDefaultSqlStringFilter(call));
        }
        return dest;
    }

    protected CallbackContext newCallbackContext() {
        return new CallbackContext();
    }

    protected boolean isInherit(ConcurrentAsyncInheritType inheritType, ConcurrentAsyncInheritType defaultType) {
        if (inheritType != null) {
            return inheritType.equals(ConcurrentAsyncInheritType.INHERIT);
        } else {
            return defaultType != null && defaultType.equals(ConcurrentAsyncInheritType.INHERIT);
        }
    }

    protected SqlFireHook createDefaultSqlFireHook(ConcurrentAsyncCall call) {
        return new RomanticTraceableSqlFireHook();
    }

    protected SqlStringFilter createDefaultSqlStringFilter(ConcurrentAsyncCall call) {
        final Method entryMethod = ThreadCacheContext.findEntryMethod();
        if (entryMethod != null) {
            return newDefaultSqlStringFilter(call, entryMethod, "(via " + DfTypeUtil.toClassTitle(call) + ")");
        }
        try {
            final Method callbackMethod = DfReflectionUtil.getPublicMethod(call.getClass(), "callback", null);
            if (callbackMethod != null) {
                return newDefaultSqlStringFilter(call, callbackMethod, null);
            }
        } catch (RuntimeException ignored) {}
        return null;
    }

    protected SqlStringFilter newDefaultSqlStringFilter(ConcurrentAsyncCall call, final Method actionMethod, final String additionalInfo) {
        return new RomanticTraceableSqlStringFilter(actionMethod, () -> additionalInfo);
    }

    protected SqlResultHandler createDefaultSqlResultHandler(ConcurrentAsyncCall call) {
        return new RomanticTraceableSqlResultHandler();
    }

    protected void prepareCallbackContext(ConcurrentAsyncCall call, CallbackContext callbackContext) {
        if (callbackContext != null && callbackContext.hasAnyInterface()) {
            CallbackContext.setCallbackContextOnThread(callbackContext);
        }
    }

    protected void clearCallbackContext(ConcurrentAsyncCall call) {
        CallbackContext.clearCallbackContextOnThread();
    }

    // ===================================================================================
    //                                                                      VariousContext
    //                                                                      ==============
    protected Map<String, Object> findCallerVariousContextMap() { // for extension
        return null;
    }

    protected Object prepareVariousContext(ConcurrentAsyncCall call, Map<String, Object> variousContextMap) { // for extension
        return null;
    }

    protected void clearVariousContext(ConcurrentAsyncCall call, Map<String, Object> callerVariousContextMap, Object variousPreparedObj) { // for extension
    }

    // ===================================================================================
    //                                                                  Exception Handling
    //                                                                  ==================
    protected void handleAsyncCallbackException(ConcurrentAsyncCall call, long before, Throwable cause) {
        // not use second argument here, same reason as logging filter
        final Throwable handled = exceptionTranslator.filterCause(cause);
        logger.error(buildAsyncCallbackExceptionMessage(call, before, handled));
    }

    protected String buildAsyncCallbackExceptionMessage(ConcurrentAsyncCall call, long before, Throwable cause) {
        final String requestPath = ThreadCacheContext.findRequestPath(); // null allowed when e.g. batch
        final Method entryMethod = ThreadCacheContext.findEntryMethod(); // might be null just in case
        final Object userBean = ThreadCacheContext.findUserBean(); // null allowed when e.g. batch
        final StringBuilder sb = new StringBuilder();
        sb.append("Failed to callback the asynchronous process: #flow #async");
        sb.append(LF);
        sb.append("/= = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =: ");
        if (requestPath != null) {
            sb.append(Srl.substringFirstFront(requestPath, "?")); // except query-string
        } else {
            if (entryMethod != null) {
                sb.append(entryMethod.getDeclaringClass().getSimpleName());
            } else {
                sb.append(call.getClass().getName());
            }
        }
        sb.append(LF).append(EX_IND);
        sb.append("callbackInterface=").append(call);
        setupExceptionMessageRequestInfo(sb, requestPath, entryMethod, userBean);
        setupExceptionMessageAccessContext(sb);
        setupExceptionMessageCallbackContext(sb);
        setupExceptionMessageVariousContext(sb, call, cause);
        setupExceptionMessageSqlCountIfExists(sb);
        setupExceptionMessageTransactionMemoriesIfExists(sb);
        setupExceptionMessageMailCountIfExists(sb);
        final long after = System.currentTimeMillis();
        final String performanceView = DfTraceViewUtil.convertToPerformanceView(after - before);
        sb.append(LF);
        sb.append("= = = = = = = = = =/ [").append(performanceView).append("] #").append(Integer.toHexString(cause.hashCode()));
        buildExceptionStackTrace(cause, sb);
        return sb.toString().trim();
    }

    protected void setupExceptionMessageRequestInfo(StringBuilder sb, String requestPath, Method entryMethod, Object userBean) {
        if (requestPath != null) {
            sb.append(LF).append(EX_IND).append("; requestPath=").append(requestPath);
        }
        if (entryMethod != null) {
            final Class<?> declaringClass = entryMethod.getDeclaringClass();
            sb.append(LF).append(EX_IND).append("; entryMethod=");
            sb.append(declaringClass.getName()).append("@").append(entryMethod.getName()).append("()");
        }
        if (userBean != null) {
            sb.append(LF).append(EX_IND).append("; userBean=").append(userBean);
        }
    }

    protected void setupExceptionMessageAccessContext(StringBuilder sb) {
        sb.append(LF).append(EX_IND).append("; accessContext=").append(PreparedAccessContext.getAccessContextOnThread());
    }

    protected void setupExceptionMessageCallbackContext(StringBuilder sb) {
        sb.append(LF).append(EX_IND).append("; callbackContext=").append(CallbackContext.getCallbackContextOnThread());
    }

    protected void setupExceptionMessageVariousContext(StringBuilder sb, ConcurrentAsyncCall call, Throwable cause) {
        final StringBuilder variousContextSb = new StringBuilder();
        buildVariousContextInAsyncCallbackExceptionMessage(variousContextSb, call, cause);
        if (variousContextSb.length() > 0) {
            sb.append(LF).append(EX_IND).append(variousContextSb.toString());
        }
    }

    protected void buildVariousContextInAsyncCallbackExceptionMessage(StringBuilder sb, ConcurrentAsyncCall call, Throwable cause) {
    }

    protected void setupExceptionMessageSqlCountIfExists(StringBuilder sb) {
        extractSqlCount().ifPresent(counter -> {
            sb.append(LF).append(EX_IND).append("; sqlCount=").append(counter.toLineDisp());
        });
    }

    protected void setupExceptionMessageTransactionMemoriesIfExists(StringBuilder sb) {
        // e.g.
        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // ; transactionMemories=wholeShow:  
        // *RomanticTransaction@2d1cd52a
        // << Transaction Current State >>
        // beginning time: 2015/12/22 12:04:40.574
        // table command: map:{PRODUCT = list:{selectCursor ; scalarSelect(LocalDate).max}}
        // << Transaction Recent Result >>
        // 1. (2015/12/22 12:04:40.740) [00m00s027ms] PRODUCT@selectCursor => Object:{}
        // 2. (2015/12/22 12:04:40.773) [00m00s015ms] PRODUCT@scalarSelect(LocalDate).max => LocalDate:{value=2013-08-02}
        // _/_/_/_/_/_/_/_/_/_/
        final SavedTransactionMemories memories = ThreadCacheContext.findTransactionMemories();
        if (memories != null) {
            final List<TransactionMemoriesProvider> providerList = memories.getOrderedProviderList();
            final StringBuilder txSb = new StringBuilder();
            for (TransactionMemoriesProvider provider : providerList) {
                provider.provide().ifPresent(result -> {
                    if (txSb.length() == 0) {
                        txSb.append(LF).append(EX_IND).append("; transactionMemories=wholeShow:");
                    }
                    txSb.append(Srl.indent(EX_IND.length(), LF + "*" + result));
                });
            }
            sb.append(txSb);
        }
    }

    protected void setupExceptionMessageMailCountIfExists(StringBuilder sb) {
        extractMailCount().ifPresent(counter -> {
            sb.append(LF).append(EX_IND).append("; mailCount=").append(counter.toLineDisp());
        });
    }

    protected void buildExceptionStackTrace(Throwable cause, StringBuilder sb) { // similar to logging filter
        sb.append(LF);
        final ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        PrintStream ps = null;
        try {
            ps = new PrintStream(out);
            cause.printStackTrace(ps);
            final String encoding = "UTF-8";
            try {
                sb.append(out.toString(encoding));
            } catch (UnsupportedEncodingException continued) {
                logger.warn("Unknown encoding: " + encoding, continued);
                sb.append(out.toString()); // retry without encoding
            }
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    // ===================================================================================
    //                                                                           SQL Count
    //                                                                           =========
    protected OptionalThing<ExecutedSqlCounter> extractSqlCount() {
        final CallbackContext context = CallbackContext.getCallbackContextOnThread();
        if (context == null) {
            return OptionalThing.empty();
        }
        final SqlStringFilter filter = context.getSqlStringFilter();
        if (filter == null || !(filter instanceof ExecutedSqlCounter)) {
            return OptionalThing.empty();
        }
        return OptionalThing.of(((ExecutedSqlCounter) filter));
    }

    // ===================================================================================
    //                                                                          Mail Count
    //                                                                          ==========
    protected OptionalThing<PostedMailCounter> extractMailCount() {
        return OptionalThing.ofNullable(ThreadCacheContext.findMailCounter(), () -> {
            throw new IllegalStateException("Not found the mail count in the thread cache.");
        });
    }

    // ===================================================================================
    //                                                                         Destructive
    //                                                                         ===========
    protected boolean isDestructiveAsyncToNormalSync() { // basically for UnitTest
        return BowgunDestructiveAdjuster.isAsyncToNormalSync();
    }

    // ===================================================================================
    //                                                                       Assert Helper
    //                                                                       =============
    protected void assertThreadCallbackNotNull(ConcurrentAsyncCall callback) {
        if (callback == null) {
            throw new IllegalArgumentException("The argument 'callback' should not be null.");
        }
    }

    protected void assertExecutorServiceValid() {
        if (primaryExecutorService == null) {
            throw new IllegalArgumentException("The primaryExecutorService should not be null.");
        }
        if (secondaryExecutorService == null) {
            throw new IllegalArgumentException("The secondaryExecutorService should not be null.");
        }
    }
}
