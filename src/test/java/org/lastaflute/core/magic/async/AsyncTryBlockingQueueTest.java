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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;

import org.dbflute.utflute.core.PlainTestCase;
import org.dbflute.utflute.core.cannonball.CannonballDragon;
import org.dbflute.utflute.core.cannonball.CannonballOption;

/**
 * @author jflute
 */
public class AsyncTryBlockingQueueTest extends PlainTestCase {

    // ===================================================================================
    //                                                                    SynchronousQueue
    //                                                                    ================
    // o has no element, put and waiting for other thread's getting  
    public void test_SynchronousQueue() throws Exception {
        SynchronousQueue<String> queue = new SynchronousQueue<String>();
        log("!! prologue queue.offer(): before={}, result={}, after={}", queue.size(), queue.offer("resort"), queue.size());
        cannonball(car -> {
            car.projectA(dragon -> putQueue(dragon, queue, "sea"), 1); // waits suddenly
            car.projectA(dragon -> putQueue(dragon, queue, "land"), 2); // also waits
            car.projectA(dragon -> takeQueue(queue), 3); // release either
            car.projectA(dragon -> takeQueue(queue), 4); // release last
        }, new CannonballOption().threadCount(4));
        log("!! epiloque queue.toString(): {}", queue);
    }

    private void putQueue(CannonballDragon dragon, SynchronousQueue<String> queue, String queueAs) {
        dragon.releaseIfOvertime(100);
        try {
            log("$$ begin queue.put({})", queueAs);
            queue.put(queueAs);
            log("$$ end queue.put({})", queueAs);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Failed to put " + queueAs, e);
        }
    }

    private void takeQueue(SynchronousQueue<String> queue) {
        try {
            log("$$ queue.take(): {}", queue.take());
        } catch (InterruptedException e) {
            throw new IllegalStateException("Failed to take", e);
        }
    }

    // ===================================================================================
    //                                                                 LinkedBlockingQueue
    //                                                                 ===================
    // o ordered as FIFO
    // o has many elements, no wait until capacity
    public void test_LinkedBlockingQueue() throws Exception {
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>(); // max integer
        log("!! prologue queue.offer(): before={}, result={}, after={}", queue.size(), queue.offer("resort"), queue.size());
        cannonball(car -> {
            car.projectA(dragon -> putQueue(dragon, queue, "sea"), 1);
            car.projectA(dragon -> putQueue(dragon, queue, "land"), 2);
            car.projectA(dragon -> takeQueue(queue), 3);
            car.projectA(dragon -> takeQueue(queue), 4);
        }, new CannonballOption().threadCount(4));
        log("!! epiloque queue.toString(): {}", queue);
    }

    private void putQueue(CannonballDragon dragon, LinkedBlockingQueue<String> queue, String queueAs) {
        dragon.releaseIfOvertime(100);
        try {
            log("$$ begin queue.put({})", queueAs);
            queue.put(queueAs);
            log("$$ end queue.put({})", queueAs);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Failed to put " + queueAs, e);
        }
    }

    private void takeQueue(LinkedBlockingQueue<String> queue) {
        try {
            log("$$ queue.take(): {}", queue.take());
        } catch (InterruptedException e) {
            throw new IllegalStateException("Failed to take", e);
        }
    }
}
