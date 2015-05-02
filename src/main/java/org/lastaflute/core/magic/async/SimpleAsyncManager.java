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
package org.lastaflute.core.magic.async;

import java.lang.reflect.Method;
import java.util.HashMap;
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
import org.dbflute.bhv.proposal.callback.TraceableSqlAdditionalInfoProvider;
import org.dbflute.hook.AccessContext;
import org.dbflute.hook.AccessContext.AccessModuleProvider;
import org.dbflute.hook.AccessContext.AccessProcessProvider;
import org.dbflute.hook.AccessContext.AccessUserProvider;
import org.dbflute.hook.CallbackContext;
import org.dbflute.hook.SqlFireHook;
import org.dbflute.hook.SqlLogHandler;
import org.dbflute.hook.SqlResultHandler;
import org.dbflute.hook.SqlStringFilter;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.DfTraceViewUtil;
import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.Srl;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.direction.OptionalCoreDirection;
import org.lastaflute.core.exception.ExceptionTranslator;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.core.magic.async.ConcurrentAsyncOption.ConcurrentAsyncInheritType;
import org.lastaflute.db.dbflute.accesscontext.PreparedAccessContext;
import org.lastaflute.db.dbflute.callbackcontext.RomanticTraceableSqlFireHook;
import org.lastaflute.db.dbflute.callbackcontext.RomanticTraceableSqlStringFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class SimpleAsyncManager implements AsyncManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger LOG = LoggerFactory.getLogger(SimpleAsyncManager.class);
    protected static final String LF = "\n";
    protected static final String IND = "  ";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant directory (AD) for framework. (NotNull: after initialization) */
    @Resource
    protected FwAssistantDirector assistantDirector;

    /** The translator of exception. (NotNull: after initialization) */
    @Resource
    protected ExceptionTranslator exceptionTranslator;

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
        final OptionalCoreDirection direction = getOptionalCoreDirection();
        final ConcurrentAsyncExecutorProvider provider = direction.assistConcurrentAsyncExecutorProvider();
        defaultConcurrentAsyncOption = provider != null ? provider.provideDefaultOption() : null;
        if (defaultConcurrentAsyncOption == null) {
            defaultConcurrentAsyncOption = new ConcurrentAsyncOption();
        }
        primaryExecutorService = createDefaultPrimaryExecutorService(provider);
        secondaryExecutorService = createDefaultSecondaryExecutorService(provider);
        showBootLogging();
    }

    protected OptionalCoreDirection getOptionalCoreDirection() {
        return assistantDirector.assistOptionalCoreDirection();
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
        return new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) { // caller thread
                handleRejectedExecution(runnable, executor);
            }
        };
    }

    protected void handleRejectedExecution(final Runnable runnable, final ThreadPoolExecutor executor) { // caller thread
        if (LOG.isDebugEnabled()) {
            LOG.debug("#flow #async ...Registering the runnable to waiting queue as retry: " + runnable);
        }
        getWaitingQueueExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    retryPuttingQueue(runnable, executor);
                } catch (InterruptedException e) {
                    final String torExp = buildExecutorHashExp(executor);
                    LOG.warn("*Failed to put the runnable to the executor" + torExp + "'s queue: " + runnable, e);
                }
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
            LOG.info("#flow #async ...Creating the executor service for waiting queue.");
            waitingQueueExecutorService = newWaitingQueueExecutorService();
            return waitingQueueExecutorService;
        }
    }

    protected ExecutorService newWaitingQueueExecutorService() { // caller thread
        return Executors.newFixedThreadPool(2); // not only one just in case
    }

    protected void retryPuttingQueue(Runnable runnable, ThreadPoolExecutor executor) throws InterruptedException { // waiting queue thread
        if (LOG.isDebugEnabled()) {
            final String torExp = buildExecutorHashExp(executor);
            LOG.debug("#flow #async ...Retrying putting the runnable to the executor" + torExp + " in waiting queue: " + runnable);
        }
        executor.getQueue().put(runnable);
        if (LOG.isDebugEnabled()) {
            final String torExp = buildExecutorHashExp(executor);
            LOG.debug("#flow #async Success to retry putting the runnable to the executor" + torExp + " in waiting queue: " + runnable);
        }
    }

    protected String buildExecutorHashExp(ExecutorService executor) {
        return "@" + Integer.toHexString(executor.hashCode());
    }

    // -----------------------------------------------------
    //                                          Boot Logging
    //                                          ------------
    protected void showBootLogging() {
        if (LOG.isInfoEnabled()) {
            LOG.info("[Async Manager]");
            LOG.info(" defaultConcurrentAsyncOption: " + defaultConcurrentAsyncOption);
            LOG.info(" primaryExecutorService: " + buildExecutorNamedExp(primaryExecutorService));
            LOG.info(" secondaryExecutorService: " + buildExecutorNamedExp(secondaryExecutorService));
        }
    }

    protected String buildExecutorNamedExp(ExecutorService executor) {
        return primaryExecutorService.getClass().getSimpleName() + buildExecutorHashExp(executor);
    }

    // ===================================================================================
    //                                                                  Asynchronous Entry
    //                                                                  ==================
    @Override
    public void async(ConcurrentAsyncCall noArgInLambda) {
        assertThreadCallbackNotNull(noArgInLambda);
        assertExecutorServiceValid();
        if (noArgInLambda.asPrimary()) {
            doAsyncPrimary(noArgInLambda);
        } else {
            doAsyncSecondary(noArgInLambda);
        }
    }

    protected void doAsyncPrimary(ConcurrentAsyncCall callback) {
        final String keyword = "primary" + buildExecutorHashExp(primaryExecutorService);
        primaryExecutorService.execute(createRunnable(callback, keyword));
    }

    protected void doAsyncSecondary(ConcurrentAsyncCall callback) {
        final String keyword = "secondary" + buildExecutorHashExp(secondaryExecutorService);
        secondaryExecutorService.submit(createRunnable(callback, keyword));
    }

    // ===================================================================================
    //                                                                     Create Runnable
    //                                                                     ===============
    protected Runnable createRunnable(final ConcurrentAsyncCall call, final String keyword) {
        final Map<String, Object> threadCacheMap = inheritThreadCacheContext(call);
        final AccessContext accessContext = inheritAccessContext(call);
        final CallbackContext callbackContext = inheritCallbackContext(call);
        final Map<String, Object> variousContextMap = findCallerVariousContextMap();
        return new Runnable() {
            public void run() {
                final long before = showRunning(keyword);
                prepareThreadCacheContext(call, threadCacheMap);
                prepareAccessContext(call, accessContext);
                prepareCallbackContext(call, callbackContext);
                final Object variousPreparedObj = prepareVariousContext(call, variousContextMap);
                try {
                    call.callback();
                } catch (RuntimeException e) {
                    handleAsyncCallbackException(call, before, e);
                } finally {
                    clearVariousContext(call, variousContextMap, variousPreparedObj);
                    clearAccessContext(call);
                    clearCallbackContext(call);
                    clearThreadCacheContext(call);
                    showFinishing(keyword, before);
                }
            }
        };
    }

    // -----------------------------------------------------
    //                                       Caller Resource
    //                                       ---------------
    protected Map<String, Object> inheritThreadCacheContext(ConcurrentAsyncCall call) {
        return new HashMap<String, Object>(ThreadCacheContext.getReadOnlyCacheMap());
    }

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
        return new RomanticTraceableSqlStringFilter(actionMethod, new TraceableSqlAdditionalInfoProvider() {
            public String provide() {
                return additionalInfo;
            }
        });
    }

    protected Map<String, Object> findCallerVariousContextMap() { // for extension
        return null;
    }

    // ===================================================================================
    //                                                                Asynchronous Process
    //                                                                ====================
    protected long showRunning(String keyword) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("#flow #async ...Running asynchronous call as " + keyword);
        }
        return System.currentTimeMillis();
    }

    protected void prepareThreadCacheContext(ConcurrentAsyncCall call, Map<String, Object> threadCacheMap) {
        ThreadCacheContext.initialize();
        for (Entry<String, Object> entry : threadCacheMap.entrySet()) {
            ThreadCacheContext.setObject(entry.getKey(), entry.getValue());
        }
    }

    protected void prepareAccessContext(ConcurrentAsyncCall call, AccessContext accessContext) {
        if (accessContext != null) {
            PreparedAccessContext.setAccessContextOnThread(accessContext);
        }
    }

    protected void prepareCallbackContext(ConcurrentAsyncCall call, CallbackContext callbackContext) {
        if (callbackContext != null && callbackContext.hasAnyInterface()) {
            CallbackContext.setCallbackContextOnThread(callbackContext);
        }
    }

    protected Object prepareVariousContext(ConcurrentAsyncCall call, Map<String, Object> variousContextMap) { // for extension
        return null;
    }

    // -----------------------------------------------------
    //                                             Exception
    //                                             ---------
    protected void handleAsyncCallbackException(ConcurrentAsyncCall call, long before, Throwable cause) {
        Throwable handled = null;
        if (cause instanceof RuntimeException) {
            try {
                exceptionTranslator.translateException((RuntimeException) cause);
            } catch (RuntimeException e) {
                handled = e;
            }
        }
        if (handled == null) {
            handled = cause;
        }
        LOG.error(buildAsyncCallbackExceptionMessage(call, before, handled), handled);
    }

    protected String buildAsyncCallbackExceptionMessage(ConcurrentAsyncCall call, long before, Throwable cause) {
        final String requestPath = ThreadCacheContext.findRequestPath(); // null allowed when e.g. batch
        final Method entryMethod = ThreadCacheContext.findEntryMethod(); // might be null just in case
        final Object userBean = ThreadCacheContext.findUserBean(); // null allowed when e.g. batch
        final StringBuilder sb = new StringBuilder();
        sb.append("Failed to callback the asynchronous process.");
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
        sb.append(LF).append(IND);
        sb.append("callbackInterface=").append(call);
        if (requestPath != null) {
            sb.append(LF).append(IND);
            sb.append(", requestPath=").append(requestPath);
        }
        if (entryMethod != null) {
            sb.append(LF).append(IND);
            final Class<?> declaringClass = entryMethod.getDeclaringClass();
            sb.append(", entryMethod=").append(declaringClass.getName()).append("#").append(entryMethod.getName()).append("()");
        }
        if (userBean != null) {
            sb.append(LF).append(IND);
            sb.append(", userBean=").append(userBean);
        }
        sb.append(LF).append(IND);
        final AccessContext accessContext = PreparedAccessContext.getAccessContextOnThread();
        sb.append(", accessContext=").append(accessContext);
        sb.append(LF).append(IND);
        final CallbackContext callbackContext = CallbackContext.getCallbackContextOnThread();
        sb.append(", callbackContext=").append(callbackContext);
        final StringBuilder variousContextSb = new StringBuilder();
        buildVariousContextInAsyncCallbackExceptionMessage(call, cause, variousContextSb);
        if (variousContextSb.length() > 0) {
            sb.append(LF).append(IND);
            sb.append(variousContextSb.toString());
        }
        sb.append(LF);
        final long after = System.currentTimeMillis();
        final String performanceView = DfTraceViewUtil.convertToPerformanceView(after - before);
        sb.append("= = = = = = = = = =/ [").append(performanceView).append("] #").append(Integer.toHexString(cause.hashCode()));
        return sb.toString();
    }

    protected void buildVariousContextInAsyncCallbackExceptionMessage(ConcurrentAsyncCall call, Throwable cause, StringBuilder sb) {
    }

    // -----------------------------------------------------
    //                                             Finishing
    //                                             ---------
    protected void clearVariousContext(ConcurrentAsyncCall call, Map<String, Object> callerVariousContextMap, Object variousPreparedObj) { // for extension
    }

    protected void clearCallbackContext(ConcurrentAsyncCall call) {
        CallbackContext.clearCallbackContextOnThread();
    }

    protected void clearAccessContext(ConcurrentAsyncCall call) {
        PreparedAccessContext.clearAccessContextOnThread();
    }

    protected void clearThreadCacheContext(ConcurrentAsyncCall call) {
        ThreadCacheContext.clear();
    }

    protected void showFinishing(String keyword, long before) {
        if (LOG.isDebugEnabled()) {
            final long after = System.currentTimeMillis();
            final String performanceView = DfTraceViewUtil.convertToPerformanceView(after - before);
            LOG.debug("#flow #async ...Finishing asynchronous call as " + keyword + ": " + performanceView);
        }
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
