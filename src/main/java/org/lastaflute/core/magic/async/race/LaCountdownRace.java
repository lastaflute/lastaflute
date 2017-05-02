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
package org.lastaflute.core.magic.async.race;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.helper.thread.CountDownRace;
import org.dbflute.util.Srl;
import org.lastaflute.core.magic.async.race.exception.LaCountdownRaceExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 * @since 0.9.6 (2017/05/02 Tuesday)
 */
public class LaCountdownRace { // migrated from DBFlute

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger _log = LoggerFactory.getLogger(CountDownRace.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Map<Integer, Object> _runnerRequestMap; // map:{entryNumber, parameterObject} 
    protected final ExecutorService _service;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public LaCountdownRace(int runnerCount) { // assigned by only runner count (without parameter)
        if (runnerCount < 1) {
            String msg = "The argument 'runnerCount' should not be minus or zero: " + runnerCount;
            throw new IllegalArgumentException(msg);
        }
        _runnerRequestMap = newRunnerRequestMap(runnerCount);
        for (int i = 0; i < runnerCount; i++) { // basically synchronized with parameter size
            final int entryNumber = i + 1;
            _runnerRequestMap.put(entryNumber, null);
        }
        _service = prepareExecutorService();
    }

    public LaCountdownRace(List<Object> parameterList) { // assigned by parameters (the size is runner count)
        if (parameterList == null) {
            throw new IllegalArgumentException("The argument 'parameterList' should not be null.");
        }
        _runnerRequestMap = newRunnerRequestMap(parameterList.size());
        int index = 0;
        for (Object parameter : parameterList) {
            final int entryNumber = index + 1;
            _runnerRequestMap.put(entryNumber, parameter);
            ++index;
        }
        _service = prepareExecutorService();
    }

    protected Map<Integer, Object> newRunnerRequestMap(int size) {
        return new LinkedHashMap<Integer, Object>(size);
    }

    protected ExecutorService prepareExecutorService() {
        return Executors.newCachedThreadPool();
    }

    // ===================================================================================
    //                                                                         Thread Fire
    //                                                                         ===========
    public void readyGo(LaCountdownRaceExecution oneArgLambda) {
        if (oneArgLambda == null) {
            throw new IllegalArgumentException("The argument 'oneArgLambda (execution)' should be not null.");
        }
        doReadyGo(oneArgLambda);
    }

    protected void doReadyGo(LaCountdownRaceExecution execution) {
        if (_runnerRequestMap.isEmpty()) { // e.g. empty parameter list
            return;
        }
        final int runnerCount = _runnerRequestMap.size();
        final CountDownLatch ready = new CountDownLatch(runnerCount);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch goal = new CountDownLatch(runnerCount);
        final LaCountdownRaceLatch ourLatch = new LaCountdownRaceLatch(runnerCount);
        final Object lockObj = new Object();
        final List<Future<Void>> futureList = new ArrayList<Future<Void>>();
        for (Entry<Integer, Object> entry : _runnerRequestMap.entrySet()) {
            final Integer entryNumber = entry.getKey();
            final Object parameter = entry.getValue(); // null allowed
            final Callable<Void> callable = createCallable(execution, ready, start, goal, ourLatch, entryNumber, parameter, lockObj);
            final Future<Void> future = _service.submit(callable);
            futureList.add(future);
        }

        if (_log.isDebugEnabled()) {
            _log.debug("...Ready Go! CountDownRace just begun! (runner=" + runnerCount + ")");
        }
        start.countDown(); // fire!
        try {
            goal.await(); // wait until all threads are finished
            if (_log.isDebugEnabled()) {
                _log.debug("All runners finished line! (runner=" + runnerCount + ")");
            }
        } catch (InterruptedException e) {
            String msg = "goal.await() was interrupted!";
            throw new IllegalStateException(msg, e);
        }

        handleFuture(futureList, execution);
    }

    protected void handleFuture(List<Future<Void>> futureList, LaCountdownRaceExecution execution) {
        final boolean throwImmediatelyByFirstCause = execution.isThrowImmediatelyByFirstCause();
        final List<Throwable> runnerCauseList = new ArrayList<Throwable>();
        for (Future<Void> future : futureList) {
            try {
                future.get();
            } catch (InterruptedException e) {
                throw new IllegalStateException("future.get() was interrupted: " + future, e);
            } catch (ExecutionException e) {
                if (throwImmediatelyByFirstCause) {
                    throw new LaCountdownRaceExecutionException(buildRunnerGoalFailureNotice(), e.getCause());
                } else {
                    runnerCauseList.add(e.getCause());
                }
            }
        }
        if (!runnerCauseList.isEmpty()) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice(buildRunnerGoalFailureNotice());
            br.addItem("Advice");
            br.addElement("Confirm all causes thrown by runners.");
            br.addItem("Runner Cause");
            int index = 0;
            for (Throwable cause : runnerCauseList) {
                if (index > 0) {
                    br.addElement("");
                }
                buildRunnerExStackTrace(br, cause, 0);
                ++index;
            }
            final String msg = br.buildExceptionMessage();
            throw new LaCountdownRaceExecutionException(msg, runnerCauseList);
        }
    }

    protected String buildRunnerGoalFailureNotice() {
        return "Failed to reach the goal of countdown race for runners.";
    }

    protected void buildRunnerExStackTrace(ExceptionMessageBuilder br, Throwable cause, int nestLevel) {
        final StringBuilder headerSb = new StringBuilder();
        if (nestLevel > 0) {
            headerSb.append("Caused by: ");
        }
        final String causeNameBase = cause.getClass().getName() + ":";
        final String causeMessage = cause.getMessage();
        if (causeMessage != null && Srl.contains(causeMessage, "\n")) {
            headerSb.append(causeNameBase);
            br.addElement(headerSb.toString());
            br.addElement(causeMessage);
        } else {
            headerSb.append(causeNameBase + " " + causeMessage);
            br.addElement(headerSb.toString());
        }
        final StackTraceElement[] stackTrace = cause.getStackTrace();
        if (stackTrace == null) { // just in case
            return;
        }
        final int limit = nestLevel == 0 ? 10 : 3;
        int index = 0;
        for (StackTraceElement element : stackTrace) {
            if (index > limit) { // not all because it's not error
                br.addElement("  ...");
                break;
            }
            final String className = element.getClassName();
            final String fileName = element.getFileName(); // might be null
            final int lineNumber = element.getLineNumber();
            final String methodName = element.getMethodName();
            final StringBuilder lineSb = new StringBuilder();
            lineSb.append("  at ").append(className).append(".").append(methodName).append("(").append(fileName);
            if (lineNumber >= 0) {
                lineSb.append(":").append(lineNumber);
            }
            lineSb.append(")");
            br.addElement(lineSb.toString());
            ++index;
        }
        final Throwable nested = cause.getCause();
        if (nested != null && nested != cause) {
            buildRunnerExStackTrace(br, nested, nestLevel + 1);
        }
    }

    // ===================================================================================
    //                                                                            Callable
    //                                                                            ========
    protected Callable<Void> createCallable(LaCountdownRaceExecution execution, final CountDownLatch ready, final CountDownLatch start,
            final CountDownLatch goal, final LaCountdownRaceLatch ourLatch, final int entryNumber, final Object parameter,
            final Object lockObj) {
        execution.readyCaller();
        return new Callable<Void>() {
            public Void call() { // each thread here
                execution.hookBeforeCountdown();
                final long threadId = Thread.currentThread().getId();
                try {
                    ready.countDown();
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        String msg = "start.await() was interrupted: start=" + start;
                        throw new IllegalStateException(msg, e);
                    }
                    RuntimeException cause = null;
                    try {
                        execution.execute(createRunner(threadId, ourLatch, entryNumber, parameter, lockObj));
                    } catch (RuntimeException e) {
                        cause = e;
                    }
                    if (cause != null) {
                        throw cause;
                    }
                } finally {
                    execution.hookBeforeGoalFinally();
                    goal.countDown();
                    ourLatch.reset(); // to release waiting threads
                }
                return null;
            }
        };
    }

    protected LaCountdownRaceRunner createRunner(long threadId, LaCountdownRaceLatch ourLatch, int entryNumber, Object parameter,
            Object lockObj) {
        return new LaCountdownRaceRunner(threadId, ourLatch, entryNumber, parameter, lockObj, _runnerRequestMap.size());
    }
}
