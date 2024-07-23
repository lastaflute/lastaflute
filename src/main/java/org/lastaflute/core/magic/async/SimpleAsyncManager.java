/*
 * Copyright 2015-2024 the original author or authors.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.dbflute.bhv.core.BehaviorCommandHook;
import org.dbflute.bhv.proposal.callback.ExecutedSqlCounter;
import org.dbflute.helper.message.ExceptionMessageBuilder;
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
import org.lastaflute.core.magic.async.ConcurrentAsyncCall.ConcurrentAsyncImportance;
import org.lastaflute.core.magic.async.ConcurrentAsyncOption.ConcurrentAsyncInheritType;
import org.lastaflute.core.magic.async.bridge.AsyncStateBridge;
import org.lastaflute.core.magic.async.bridge.AsyncStateBridgeOpCall;
import org.lastaflute.core.magic.async.bridge.AsyncStateBridgeOption;
import org.lastaflute.core.magic.async.bridge.BridgeCallAdapter;
import org.lastaflute.core.magic.async.exception.ConcurrentParallelRunnerException;
import org.lastaflute.core.magic.async.future.BasicYourFuture;
import org.lastaflute.core.magic.async.future.DestructiveYourFuture;
import org.lastaflute.core.magic.async.future.YourFuture;
import org.lastaflute.core.magic.async.waiting.WaitingAsyncException;
import org.lastaflute.core.magic.async.waiting.WaitingAsyncResult;
import org.lastaflute.core.magic.destructive.BowgunDestructiveAdjuster;
import org.lastaflute.core.mail.PostedMailCounter;
import org.lastaflute.core.remoteapi.CalledRemoteApiCounter;
import org.lastaflute.db.dbflute.accesscontext.PreparedAccessContext;
import org.lastaflute.db.dbflute.callbackcontext.traceablesql.RomanticTraceableSqlFireHook;
import org.lastaflute.db.dbflute.callbackcontext.traceablesql.RomanticTraceableSqlResultHandler;
import org.lastaflute.db.dbflute.callbackcontext.traceablesql.RomanticTraceableSqlStringFilter;
import org.lastaflute.db.jta.romanticist.SavedTransactionMemories;
import org.lastaflute.db.jta.romanticist.TransactionMemoriesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

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

    /** The tertiary service of executor for asynchronous process. (NotNull: after initialization) */
    protected ExecutorService tertiaryExecutorService;

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
        tertiaryExecutorService = createDefaultSecondaryExecutorService(provider);
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
            logger.info(" tertiaryExecutorService: " + buildExecutorNamedExp(tertiaryExecutorService));
        }
    }

    protected String buildExecutorNamedExp(ExecutorService executor) {
        return executor.getClass().getSimpleName() + buildExecutorHashExp(executor);
    }

    // ===================================================================================
    //                                                                  Asynchronous Entry
    //                                                                  ==================
    @Override
    public YourFuture async(ConcurrentAsyncCall noArgLambda) {
        assertThreadCallbackNotNull(noArgLambda);
        assertExecutorServiceValid();
        if (noArgLambda.asPrimary()) { // forcing option
            return doAsyncPrimary(noArgLambda);
        } else {
            final ConcurrentAsyncImportance importance = noArgLambda.importance();
            if (importance != null) {
                if (ConcurrentAsyncImportance.PRIMARY.equals(importance)) {
                    return doAsyncPrimary(noArgLambda);
                } else if (ConcurrentAsyncImportance.SECONDARY.equals(importance)) {
                    return doAsyncSecondary(noArgLambda);
                } else if (ConcurrentAsyncImportance.TERTIARY.equals(importance)) {
                    return doAsyncTertiary(noArgLambda);
                } else { // no way
                    throw new IllegalStateException("Unknown importance: " + importance);
                }
            } else {
                return doAsyncSecondary(noArgLambda); // as default
            }
        }
    }

    protected YourFuture doAsyncPrimary(ConcurrentAsyncCall callback) {
        return actuallyAsync(callback, primaryExecutorService, "primary");
    }

    protected YourFuture doAsyncSecondary(ConcurrentAsyncCall callback) {
        return actuallyAsync(callback, secondaryExecutorService, "secondary");
    }

    protected YourFuture doAsyncTertiary(ConcurrentAsyncCall callback) {
        return actuallyAsync(callback, tertiaryExecutorService, "tertiary");
    }

    protected YourFuture actuallyAsync(ConcurrentAsyncCall callback, ExecutorService service, String title) {
        if (isDestructiveAsyncToNormalSync()) { // destructive (for e.g. UnitTest)
            return destructiveNormalSync(callback);
        } else { // basically here
            final String keyword = title + buildExecutorHashExp(service);
            final Callable<WaitingAsyncResult> task = createCallableTask(callback, keyword);
            final Future<WaitingAsyncResult> nativeFuture = service.submit(task); // real asynchronous
            return new BasicYourFuture(nativeFuture);
        }
    }

    protected DestructiveYourFuture destructiveNormalSync(ConcurrentAsyncCall callback) {
        if (logger.isInfoEnabled()) { // no way of production so INFO
            logger.info("#flow #async *Non-asynchronous by destructive adjuster, so executing as synchronous.");
        }
        // *not same state as real asynchronous, thread local values are different so dangerous
        callback.callback(); // normal synchronous
        return new DestructiveYourFuture();
    }

    // ===================================================================================
    //                                                                     Create Runnable
    //                                                                     ===============
    protected Callable<WaitingAsyncResult> createCallableTask(ConcurrentAsyncCall call, String keyword) { // in caller thread
        final Map<String, Object> threadCacheMap = inheritThreadCacheContext(call);
        final AccessContext accessContext = inheritAccessContext(call);
        final CallbackContext callbackContext = inheritCallbackContext(call);
        final Map<String, Object> variousContextMap = findCallerVariousContextMap();
        return () -> { // in new thread
            prepareThreadCacheContext(call, threadCacheMap);
            preparePreparedAccessContext(call, accessContext);
            prepareCallbackContext(call, callbackContext);
            final Object variousPreparedObj = prepareVariousContext(call, variousContextMap);
            final long before = showRunning(keyword);
            final WaitingAsyncResult result = new WaitingAsyncResult();
            Throwable cause = null;
            try {
                call.callback();
            } catch (Throwable e) {
                handleAsyncCallbackException(call, before, result, e);
                cause = e;
            } finally {
                showFinishing(keyword, before, cause); // should be before clearing because of using them
                clearVariousContext(call, variousContextMap, variousPreparedObj);
                clearCallbackContext(call);
                clearPreparedAccessContext(call);
                clearThreadCacheContext(call);
                call.hookFinally(result);
            }
            return result;
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
            extractSqlCounter().ifPresent(counter -> { // present even if total is zero
                if (counter.getTotalCountOfSql() > 0) { // not required in asynchronous process
                    sb.append(LF).append(" sqlCount: ").append(counter.toLineDisp());
                }
            });
            extractMailCounter().ifPresent(counter -> {
                sb.append(LF).append(" mailCount: ").append(counter.toLineDisp());
            });
            extractRemoteApiCounter().ifPresent(counter -> {
                sb.append(LF).append(" remoteApiCount: ").append(counter.toLineDisp());
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
        return doInheritThreadCacheContext();
    }

    protected Map<String, Object> doInheritThreadCacheContext() {
        return new HashMap<String, Object>(ThreadCacheContext.getReadOnlyCacheMap());
    }

    protected void prepareThreadCacheContext(ConcurrentAsyncCall call, Map<String, Object> threadCacheMap) {
        doPrepareThreadCacheContext(threadCacheMap);
    }

    protected void doPrepareThreadCacheContext(Map<String, Object> threadCacheMap) {
        ThreadCacheContext.initialize();
        threadCacheMap.forEach((key, value) -> {
            if (value instanceof ThreadCompleted) { // cannot be inherited
                return;
            }
            ThreadCacheContext.setObject(key, value);
        });
    }

    protected void clearThreadCacheContext(ConcurrentAsyncCall call) {
        doClearThreadCacheContext();
    }

    protected void doClearThreadCacheContext() {
        ThreadCacheContext.clear();
    }

    // ===================================================================================
    //                                                                       AccessContext
    //                                                                       =============
    protected AccessContext inheritAccessContext(ConcurrentAsyncCall call) { // null allowed
        return doInheritAccessContext();
    }

    protected AccessContext doInheritAccessContext() { // null allowed
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
        doPreparePreparedAccessContext(accessContext);
    }

    protected void doPreparePreparedAccessContext(AccessContext accessContext) {
        if (accessContext != null) {
            PreparedAccessContext.setAccessContextOnThread(accessContext);
        }
    }

    protected void clearPreparedAccessContext(ConcurrentAsyncCall call) {
        doClearPreparedAccessContext();
    }

    protected void doClearPreparedAccessContext() {
        PreparedAccessContext.clearAccessContextOnThread();
    }

    // ===================================================================================
    //                                                                     CallbackContext
    //                                                                     ===============
    protected CallbackContext inheritCallbackContext(ConcurrentAsyncCall call) { // null allowed
        return doInheritCallbackContext(call);
    }

    protected CallbackContext doInheritCallbackContext(ConcurrentAsyncCall call) {
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
        doPrepareCallbackContext(callbackContext);
    }

    protected void doPrepareCallbackContext(CallbackContext callbackContext) {
        if (callbackContext != null && callbackContext.hasAnyInterface()) {
            CallbackContext.setCallbackContextOnThread(callbackContext);
        }
    }

    protected void clearCallbackContext(ConcurrentAsyncCall call) {
        doClearCallbackContext();
    }

    protected void doClearCallbackContext() {
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
    protected void handleAsyncCallbackException(ConcurrentAsyncCall call, long before, WaitingAsyncResult result, Throwable cause) {
        // not use second argument here, same reason as logging filter
        final Throwable handled = exceptionTranslator.filterCause(cause);
        final boolean errorLoggingEnabled = isErrorLoggingEnabled(call, handled);
        final String msg = buildAsyncCallbackExceptionMessage(call, before, handled, /*containsStackTrace*/errorLoggingEnabled);
        if (errorLoggingEnabled) {
            logger.error(msg); // contains stack trace here
        }
        result.setWaitingAsyncException(createWaitingAsyncException(msg, handled));
    }

    // -----------------------------------------------------
    //                                         Whole Message
    //                                         -------------
    protected String buildAsyncCallbackExceptionMessage(ConcurrentAsyncCall call, long before, Throwable cause,
            boolean containsStackTrace) {
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
        setupExceptionMessageRemoteApiCountIfExists(sb);
        final long after = System.currentTimeMillis();
        final String performanceView = DfTraceViewUtil.convertToPerformanceView(after - before);
        sb.append(LF);
        sb.append("= = = = = = = = = =/ [").append(performanceView).append("] #").append(Integer.toHexString(cause.hashCode()));
        if (containsStackTrace) {
            buildExceptionStackTrace(cause, sb);
        }
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
        extractSqlCounter().ifPresent(counter -> {
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
        extractMailCounter().ifPresent(counter -> {
            sb.append(LF).append(EX_IND).append("; mailCount=").append(counter.toLineDisp());
        });
    }

    protected void setupExceptionMessageRemoteApiCountIfExists(StringBuilder sb) {
        extractRemoteApiCounter().ifPresent(counter -> {
            sb.append(LF).append(EX_IND).append("; remoteApiCount=").append(counter.toLineDisp());
        });
    }

    protected void buildExceptionStackTrace(Throwable cause, StringBuilder sb) { // similar to logging filter
        sb.append(LF);
        final ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        final String encoding = "UTF-8"; // for on-memory closed-scope I/O
        PrintStream ps = null;
        try {
            final boolean autoFlush = false; // the output stream does not need flush
            ps = new PrintStream(out, autoFlush, encoding);
            cause.printStackTrace(ps);
            sb.append(out.toString(encoding));
        } catch (UnsupportedEncodingException continued) { // basically no way
            logger.warn("Unknown encoding: " + encoding, continued);
            sb.append(out.toString()); // retry without encoding
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    // -----------------------------------------------------
    //                                         Error Logging
    //                                         -------------
    protected boolean isErrorLoggingEnabled(ConcurrentAsyncCall call, Throwable handled) {
        return !call.suppressesErrorLogging();
    }

    // -----------------------------------------------------
    //                                        Waiting Result
    //                                        --------------
    protected WaitingAsyncException createWaitingAsyncException(String msg, Throwable handled) {
        return new WaitingAsyncException(msg, handled);
    }

    // ===================================================================================
    //                                                                         SQL Counter
    //                                                                         ===========
    protected OptionalThing<ExecutedSqlCounter> extractSqlCounter() {
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
    //                                                                        Mail Counter
    //                                                                        ============
    protected OptionalThing<PostedMailCounter> extractMailCounter() {
        return OptionalThing.ofNullable(ThreadCacheContext.findMailCounter(), () -> {
            throw new IllegalStateException("Not found the mail counter in the thread cache.");
        });
    }

    // ===================================================================================
    //                                                                   RemoteApi Counter
    //                                                                   =================
    protected OptionalThing<CalledRemoteApiCounter> extractRemoteApiCounter() {
        return OptionalThing.ofNullable(ThreadCacheContext.findRemoteApiCounter(), () -> {
            throw new IllegalStateException("Not found the remote-api counter in the thread cache.");
        });
    }

    // ===================================================================================
    //                                                                         Destructive
    //                                                                         ===========
    protected boolean isDestructiveAsyncToNormalSync() { // basically for UnitTest
        return BowgunDestructiveAdjuster.isAsyncToNormalSync();
    }

    // ===================================================================================
    //                                                                        Bridge State
    //                                                                        ============
    @Override
    public AsyncStateBridge bridgeState(AsyncStateBridgeOpCall opLambda) {
        final AsyncStateBridgeOption option = createAsyncStateBridgeOption(opLambda);
        final BridgeCallAdapter callAdapter = newBridgeCallAdapter();
        final Callable<WaitingAsyncResult> callableTask = createCallableTask(() -> {
            callAdapter.delegate();
        }, "bridge");
        return newAsyncStateBridge(callAdapter, callableTask, option);
    }

    protected AsyncStateBridgeOption createAsyncStateBridgeOption(AsyncStateBridgeOpCall opLambda) {
        final AsyncStateBridgeOption option = new AsyncStateBridgeOption();
        opLambda.callback(option);
        return option;
    }

    protected BridgeCallAdapter newBridgeCallAdapter() {
        return new BridgeCallAdapter();
    }

    protected AsyncStateBridge newAsyncStateBridge(BridgeCallAdapter callAdapter, Callable<WaitingAsyncResult> callableTask,
            AsyncStateBridgeOption option) {
        return new AsyncStateBridge(callAdapter, callableTask, option);
    }

    // ===================================================================================
    //                                                                            Parallel
    //                                                                            ========
    @Override
    public void parallel(ConcurrentParallelCall runnerLambda, ConcurrentParallelOpCall opLambda) {
        final ConcurrentParallelOption option = createConcurrentParallelOption(opLambda);
        readyGo(runnerLambda, option);
    }

    // -----------------------------------------------------
    //                                              Settings
    //                                              --------
    protected ConcurrentParallelOption createConcurrentParallelOption(ConcurrentParallelOpCall opLambda) {
        final ConcurrentParallelOption op = new ConcurrentParallelOption();
        opLambda.callback(op);
        return op;
    }

    // -----------------------------------------------------
    //                                              Ready Go
    //                                              --------
    protected void readyGo(ConcurrentParallelCall runnerLambda, ConcurrentParallelOption option) {
        if (isEmptyParallel(option)) { // parameters are specified but empty
            logger.debug("#flow #parallel Empty parameter list so do nothing");
            return;
        }
        final Map<Integer, Object> parameterHistoryMap = new LinkedHashMap<>(); // for exception handling
        final Map<Integer, YourFuture> allFutureMap = new LinkedHashMap<>(); // for e.g. waiting for all done, exception handling
        final Set<YourFuture> runningPossibleFutureSet = new LinkedHashSet<>(); // for e.g. control of concurrency count limit
        final Object lockObj = new Object();
        option.getParameterList().ifPresent(parameterList -> {
            logger.debug("#flow #parallel ...Starting parameter-based parallel runners: params=" + parameterList.size());
            int entryNumber = 1; // e.g. 1, 2, 3...
            for (Object parameter : parameterList) {
                parameterHistoryMap.put(entryNumber, parameter);
                final YourFuture future = doParallelAsync(runnerLambda, entryNumber, parameter, lockObj, runningPossibleFutureSet, option);
                keepParallelFuture(entryNumber, future, allFutureMap, runningPossibleFutureSet);
                ++entryNumber;
            }
        }).orElse(() -> {
            final int runnerCount = getParallelEmptyParameterRunnerCount();
            logger.debug("#flow #parallel ...Starting fixed-count parallel runners: count=" + runnerCount);
            for (int i = 0; i < runnerCount; i++) {
                final int entryNumber = i + 1; // e.g. 1, 2, 3...
                final YourFuture future =
                        doParallelAsync(runnerLambda, entryNumber, /*parameter*/null, lockObj, runningPossibleFutureSet, option);
                keepParallelFuture(entryNumber, future, allFutureMap, runningPossibleFutureSet);
            }
        });
        waitForParallelRunnerAllDone(allFutureMap, option);
        if (!option.isErrorHandlingSubsumed()) { // default here, tell caller about exceptions
            throwParallelRunnerException(allFutureMap, parameterHistoryMap, option);
        }
    }

    protected boolean isEmptyParallel(ConcurrentParallelOption option) {
        final OptionalThing<List<Object>> optParamList = option.getParameterList();
        return optParamList.isPresent() && optParamList.get().isEmpty(); // specified but empty
    }

    protected int getParallelEmptyParameterRunnerCount() {
        return 5; // #for_now jflute fixed now, but should it be option? (needed? on-demand supported?)
    }

    protected void keepParallelFuture(int entryNumber, YourFuture future, Map<Integer, YourFuture> allFutureMap,
            Set<YourFuture> runningPossibleFutureSet) {
        allFutureMap.put(entryNumber, future);
        runningPossibleFutureSet.add(future);
        if (entryNumber % 20 == 0) { // sometimes to avoid many iterator instances
            runningPossibleFutureSet.removeIf(existing -> existing.isDone()); // running only at this moment
        }
    }

    // -----------------------------------------------------
    //                                 Parallel Asynchronous
    //                                 ---------------------
    protected YourFuture doParallelAsync(ConcurrentParallelCall runnerLambda, int entryNumber, Object parameter, Object lockObj,
            Set<YourFuture> runningPossibleFutureSet, ConcurrentParallelOption option) {
        option.getConcurrencyCountLimit().ifPresent(concurrencyCountlimit -> {
            waitForParallelConcurrencyLimitation(concurrencyCountlimit, runningPossibleFutureSet, option);
        });
        return async(createParallelAsyncCall(runnerLambda, entryNumber, parameter, lockObj, option));
    }

    protected void waitForParallelConcurrencyLimitation(Integer concurrencyCountlimit, Set<YourFuture> runningPossibleFutureSet,
            ConcurrentParallelOption option) {
        // [waiting-interval milliseconds performance]
        // when 2000 parameters and limit 5 and light processes
        //   1L  : 00m00s925ms, 00m01s133ms
        //   10L : 00m03s502ms, 00m02s447ms
        //   20L : 00m05s249ms, 00m05s193ms
        //   100L: 00m31s037ms
        // (avoid too late and too short-span) 
        final long waitingIntervalMillis = option.getWaitingIntervalMillis().orElse(20L);
        while (true) {
            final long runningCount = runningPossibleFutureSet.stream().filter(future -> !future.isDone()).count();
            if (runningCount < concurrencyCountlimit) {
                break; // OK
            }
            try {
                Thread.sleep(waitingIntervalMillis);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Failed to sleep the current thread: " + Thread.currentThread(), e);
            }
        }
    }

    protected ConcurrentAsyncCall createParallelAsyncCall(ConcurrentParallelCall runnerLambda, int entryNumber, Object parameter,
            Object lockObj, ConcurrentParallelOption option) {
        return new ConcurrentAsyncCall() {
            @Override
            public void callback() { // contains destructive handling
                final long threadId = Thread.currentThread().getId();
                final ConcurrentParallelRunner runner = createConcurrentParallelRunner(threadId, entryNumber, parameter, lockObj);
                runnerLambda.callback(runner);
            }

            @Override
            public boolean suppressesErrorLogging() {
                return !option.isErrorHandlingSubsumed(); // "suppress" as default
            }
        };
    }

    protected ConcurrentParallelRunner createConcurrentParallelRunner(long threadId, int entryNumber, Object parameter, Object lockObj) {
        return new ConcurrentParallelRunner(threadId, entryNumber, parameter, lockObj);
    }

    // -----------------------------------------------------
    //                                     Wait for all done
    //                                     -----------------
    protected void waitForParallelRunnerAllDone(Map<Integer, YourFuture> futureMap, ConcurrentParallelOption option) {
        final long waitingIntervalMillis = option.getWaitingIntervalMillis().orElse(100L); // as default fixedly
        while (true) {
            if (futureMap.values().stream().allMatch(future -> future.isDone())) {
                logger.debug("#flow #parallel ...Finishing all runners of parallel(): runnerCount={}", futureMap.size());
                break;
            }
            try {
                Thread.sleep(waitingIntervalMillis);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Failed to sleep the current thread: " + Thread.currentThread(), e);
            }
        }
    }

    // -----------------------------------------------------
    //                                    Parallel Exception
    //                                    ------------------
    protected void throwParallelRunnerException(Map<Integer, YourFuture> allFutureMap, Map<Integer, Object> parameterHistoryMap,
            ConcurrentParallelOption option) {
        final List<WaitingAsyncException> asyncExpList = new ArrayList<>();
        allFutureMap.forEach((entryNumber, future) -> {
            final WaitingAsyncResult result = future.waitForDone();
            result.getWaitingAsyncException().ifPresent(exp -> {
                asyncExpList.add(exp);
                exp.setEntryNumber(entryNumber);
                exp.setParameter(parameterHistoryMap.get(entryNumber)); // null allowed when no-parameter
            });
        });
        if (!asyncExpList.isEmpty()) {
            throwConcurrentParallelRunnerException(asyncExpList, parameterHistoryMap, option);
        }
    }

    protected void throwConcurrentParallelRunnerException(List<WaitingAsyncException> asyncExpList, Map<Integer, Object> parameterMap,
            ConcurrentParallelOption option) {
        final String notice = "Failed to finish processes of parallel runners.";
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice(notice);
        br.addItem("Advice");
        br.addElement("Confirm causes thrown by runners.");
        br.addElement("The exception of first-done process is treated as main cause.");
        br.addElement("And you can get all exception instances by getRunnerCauseList().");
        br.addItem("Option");
        br.addElement(option); // contains parameterList
        if (!asyncExpList.isEmpty()) {
            br.addItem("Native Cause");
            for (WaitingAsyncException asyncExp : asyncExpList) {
                final Throwable cause = asyncExp.getCause();
                final Object causeExp = cause != null ? cause.getClass().getName() : null; // null check just in case
                final String entrySuffix = asyncExp.getEntryNumber().map(entryNumber -> { // basically present
                    final String paramExp = asyncExp.getParameter().map(param -> ": " + param).orElse(""); // parameter-based or fixed-count
                    return " // " + entryNumber + paramExp; // both entry number and parameter are always together in parallel()
                }).orElse("");
                br.addElement(causeExp + entrySuffix);
                if (cause != null) {
                    final Throwable nestedCause = cause.getCause();
                    if (nestedCause != null) {
                        br.addElement(" |-" + nestedCause.getClass().getName());
                        final Throwable nestedNestedCause = nestedCause.getCause();
                        if (nestedNestedCause != null) {
                            br.addElement("   |-" + nestedNestedCause.getClass().getName()); // until here for now
                        }
                    }
                }
            }
        }
        final String msg = br.buildExceptionMessage();
        final WaitingAsyncException firstCause = !asyncExpList.isEmpty() ? asyncExpList.get(0) : null;
        throw new ConcurrentParallelRunnerException(msg, firstCause, asyncExpList);
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
