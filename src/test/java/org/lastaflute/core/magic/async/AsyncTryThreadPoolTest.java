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
package org.lastaflute.core.magic.async;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.dbflute.utflute.core.PlainTestCase;
import org.dbflute.util.DfTraceViewUtil;

/**
 * @author jflute
 */
public class AsyncTryThreadPoolTest extends PlainTestCase {

    // ===================================================================================
    //                                                                     FixedThreadPool
    //                                                                     ===============
    // o always has fixed count threads (keepAliveTime is 0L)
    // o uses LinkedBlockingQueue as max integer
    // o so (almost) no wait if many execution requests
    // o (RejectedExecutionHandler is called when queue is max)
    public void test_FixedThreadPool() throws IOException {
        ThreadPoolExecutor service = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        long before = currentUtilDate().getTime();
        executeFixedService(service, "sea");
        executeFixedService(service, "land");
        executeFixedService(service, "piari"); // pool short, no wait
        executeFixedService(service, "bonvo"); // pool short, no wait
        executeFixedService(service, "dstore"); // pool short, no wait
        executeFixedService(service, "amba"); // pool short, no wait
        epiloqueOfFixedService(service, before);
    }

    private void executeFixedService(ExecutorService service, String executeAs) {
        log("!! service.execute({})", executeAs);
        service.execute(() -> {
            log("$$ begin {}", executeAs);
            sleep(200); // mock heavy process
            log("$$ end {}", executeAs);
        });
    }

    private void epiloqueOfFixedService(ThreadPoolExecutor service, long before) {
        log("!! *epiloque of service: poolSize={}, performance={}", service.getPoolSize(), buildPerformanceView(before));
        sleep(2000);
        log("!! *epiloque of test: poolSize={}", service.getPoolSize());
    }

    // ===================================================================================
    //                                                                 OnDemand ThreadPool
    //                                                                 ===================
    // #thinking why one thread? by jflute
    public void test_on_demand_ThreadPool_basic() throws IOException {
        ThreadPoolExecutor service = createOnDemandThreadPool();
        long before = currentUtilDate().getTime();
        executeOnDemandService(service, "sea");
        executeOnDemandService(service, "land");
        executeOnDemandService(service, "piari"); // pool short, no wait
        executeOnDemandService(service, "bonvo"); // pool short, no wait
        executeOnDemandService(service, "dstore"); // pool short, no wait
        executeOnDemandService(service, "amba"); // pool short, no wait
        epiloqueOfOnDemandService(service, before);
    }

    public void test_on_demand_ThreadPool_directQueuing() throws IOException {
        ThreadPoolExecutor service = createOnDemandThreadPool();
        long before = currentUtilDate().getTime();
        registerOnDemandQueue(service, "sea");
        registerOnDemandQueue(service, "land");
        registerOnDemandQueue(service, "piari"); // pool short, no wait
        sleep(300);
        executeOnDemandService(service, "bonvo"); // pool short, no wait
        sleep(300);
        registerOnDemandQueue(service, "dstore"); // pool short, no wait
        epiloqueOfOnDemandService(service, before);
    }

    private void executeOnDemandService(ThreadPoolExecutor service, String executeAs) {
        log("!! service.execute({}): (before)workQueue.size()={}", executeAs, service.getQueue().size());
        service.execute(() -> {
            log("$$ begin {}", executeAs);
            sleep(300); // mock heavy process
            log("$$ end {}", executeAs);
        });
    }

    private void registerOnDemandQueue(ThreadPoolExecutor service, String executeAs) {
        BlockingQueue<Runnable> workQueue = service.getQueue();
        log("!! workQueue.add({}): (before)workQueue.size()={}", executeAs, workQueue.size());
        workQueue.add(() -> {
            log("$$ begin {}", executeAs);
            sleep(300); // mock heavy process
            log("$$ end {}", executeAs);
        });
    }

    private ThreadPoolExecutor createOnDemandThreadPool() {
        final int corePoolSize = 0;
        final Integer maximumPoolSize = 2;
        final long keepAliveTime = 60L;
        final TimeUnit seconds = TimeUnit.SECONDS;
        final LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
        final RejectedExecutionHandler rejected = new AbortPolicy();
        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, seconds, workQueue, rejected);
    }

    private void epiloqueOfOnDemandService(ThreadPoolExecutor service, long before) {
        log("!! *epiloque of service: poolSize={}, performance={}", service.getPoolSize(), buildPerformanceView(before));
        sleep(3000);
        log("!! *epiloque of test: poolSize={}", service.getPoolSize());
    }

    // ===================================================================================
    //                                                             AsyncManager Simulation
    //                                                             =======================
    public void test_AsyncManager_simulation() throws IOException {
        ThreadPoolExecutor service = createDefaultExecutorService();
        long before = currentUtilDate().getTime();
        executeSimulationService(service, "sea");
        executeSimulationService(service, "land");
        executeSimulationService(service, "piari"); // pool short, no wait
        executeSimulationService(service, "bonvo"); // pool short, no wait
        executeSimulationService(service, "dstore"); // pool short, no wait
        executeSimulationService(service, "amba"); // pool short, no wait
        epiloqueOfAsyncService(service, before);
    }

    private void executeSimulationService(ThreadPoolExecutor service, String executeAs) {
        log("!! service.execute({}): (before)workQueue.size()={}", executeAs, service.getQueue().size());
        service.execute(() -> {
            log("$$ begin {}", executeAs);
            sleep(300); // mock heavy process
            log("$$ end {}", executeAs);
        });
    }

    protected ThreadPoolExecutor createDefaultExecutorService() {
        ExecutorService waitingService = Executors.newFixedThreadPool(2);
        final int corePoolSize = 0;
        final Integer maximumPoolSize = 2;
        final long keepAliveTime = 60L;
        final TimeUnit seconds = TimeUnit.SECONDS;
        final BlockingQueue<Runnable> workQueue = new SynchronousQueue<Runnable>();
        final RejectedExecutionHandler rejected = createRejectedExecutionHandler(waitingService);
        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, seconds, workQueue, rejected);
    }

    protected RejectedExecutionHandler createRejectedExecutionHandler(ExecutorService waitingService) {
        return (runnable, executor) -> {
            handleRejectedExecution(runnable, executor, waitingService);
        };
    }

    protected void handleRejectedExecution(final Runnable runnable, final ThreadPoolExecutor executor, ExecutorService waitingService) { // caller thread
        log("!! ...Registering the runnable to waiting queue as retry");
        waitingService.execute(() -> {
            log("%% ...Retryng putting queue: size={}", executor.getQueue().size());
            try {
                executor.getQueue().put(runnable); // waiting for free thread
            } catch (InterruptedException e) {
                log("%% *Failed to put the runnable to the executor queue", e);
            }
            log("%% ...Ending retry process: size={}", executor.getQueue().size());
        });
        log("!! ...Ending handleRejectedExecution()");
    }

    private void epiloqueOfAsyncService(ThreadPoolExecutor service, long before) {
        log("!! *epiloque of service: poolSize={}, performance={}", service.getPoolSize(), buildPerformanceView(before));
        sleep(3000);
        log("!! *epiloque of test: poolSize={}", service.getPoolSize());
    }

    // ===================================================================================
    //                                                                        Internal Try
    //                                                                        ============
    // part of ThreadPoolExecutor
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    private static final int COUNT_BITS = Integer.SIZE - 3;
    private static final int CAPACITY = (1 << COUNT_BITS) - 1;
    private static final int RUNNING = -1 << COUNT_BITS;

    private static int runStateOf(int c) {
        return c & ~CAPACITY;
    }

    private static int workerCountOf(int c) {
        return c & CAPACITY;
    }

    private static int ctlOf(int rs, int wc) {
        return rs | wc;
    }

    public void test_internalStaticMethods() {
        int c = ctl.get();
        log("workerCountOf(c): c={}, return={}", c, workerCountOf(c));
        log("runStateOf(c): c={}, return={}", c, runStateOf(c));
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    private String buildPerformanceView(long before) {
        return DfTraceViewUtil.convertToPerformanceView(currentUtilDate().getTime() - before);
    }
}
