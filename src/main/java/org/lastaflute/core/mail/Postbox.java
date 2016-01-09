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
package org.lastaflute.core.mail;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.dbflute.mail.PostOffice;
import org.dbflute.mail.Postcard;
import org.dbflute.mail.send.SMailDeliveryDepartment;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.direction.FwCoreDirection;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.di.DisposableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 * @since 0.6.0 (2015/05/04 Monday)
 */
public class Postbox {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(Postbox.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant director (AD) for framework. (NotNull: after initialization) */
    @Resource
    protected FwAssistantDirector assistantDirector;

    /** Everybody knows, it's post office. (NullAllowed: null means no mail) */
    protected PostOffice postOffice;

    /** Is hot deploy requested? (true only when local development) */
    protected boolean hotDeployRequested;

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
        final SMailDeliveryDepartment deliveryDepartment = direction.assistMailDeliveryDepartment();
        postOffice = deliveryDepartment != null ? newPostOffice(deliveryDepartment) : null;
        prepareHotDeploy();
        showBootLogging();
    }

    protected FwCoreDirection assistCoreDirection() {
        return assistantDirector.assistCoreDirection();
    }

    protected PostOffice newPostOffice(SMailDeliveryDepartment deliveryDepartment) {
        return new PostOffice(deliveryDepartment);
    }

    protected void showBootLogging() {
        if (logger.isInfoEnabled()) {
            logger.info("[Postbox]");
            if (postOffice != null) {
                final SMailDeliveryDepartment department = postOffice.getDeliveryDepartment();
                logger.info(" postOffice: " + buildPostOfficeExp());
                logger.info(" postalParkingLot: " + department.getParkingLot());
                logger.info(" postalPersonnel: " + department.getPersonnel());
            } else {
                logger.info(" postOffice: *no used");
            }
        }
    }

    protected String buildPostOfficeExp() {
        return postOffice.getClass().getSimpleName() + "@" + Integer.toHexString(postOffice.hashCode());
    }

    // ===================================================================================
    //                                                                               Post
    //                                                                              ======
    public void post(LaMailPostcard postcard) {
        assertPostOfficeWorks(postcard);
        reloadIfNeeds();
        final Postcard nativePostcard = postcard.toNativePostcard();
        postOffice.deliver(nativePostcard);
        saveMemories(postcard);
    }

    protected void assertPostOfficeWorks(LaMailPostcard postcard) {
        if (postOffice == null) {
            String msg = "No mail settings so cannot send your mail: " + postcard;
            throw new IllegalStateException(msg);
        }
    }

    protected void saveMemories(LaMailPostcard postcard) {
        if (!ThreadCacheContext.exists()) {
            return;
        }
        final PostedMailCounter counter = counterComesHere();
        counter.incrementPosting();
        final Postcard nativePostcard = postcard.toNativePostcard();
        if (nativePostcard.isDryrun()) {
            counter.incrementDryrun();
        }
        if (nativePostcard.isAlsoHtmlFile()) {
            counter.incrementAlsoHtml();
        }
        if (nativePostcard.isForcedlyDirect()) {
            counter.incrementForcedlyDirect();
        }
    }

    protected PostedMailCounter counterComesHere() {
        PostedMailCounter counter = ThreadCacheContext.findMailCounter();
        if (counter == null) {
            counter = new PostedMailCounter();
            ThreadCacheContext.registerMailCounter(counter);
        }
        return counter;
    }

    // ===================================================================================
    //                                                                          Hot Deploy
    //                                                                          ==========
    protected void prepareHotDeploy() { // only unused if cool
        DisposableUtil.add(() -> requestHotDeploy());
        hotDeployRequested = false;
    }

    protected void requestHotDeploy() { // called when request ending if HotDeploy
        // no sync to avoid disposable thread locking this (or deadlock)
        // should be synchronized in office process
        postOffice.workingDispose(); // actual dispose
        hotDeployRequested = true;
    }

    protected void reloadIfNeeds() {
        if (hotDeployRequested) {
            // INFO to find mistake that it uses HotDeploy in production
            logger.info("...Reloading postbox for MailFlute by HotDeploy request");
            prepareHotDeploy(); // for next HotDeploy, actual disposing when dispose() called by framework
        }
    }
}
