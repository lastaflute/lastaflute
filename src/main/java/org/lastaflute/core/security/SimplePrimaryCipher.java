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
package org.lastaflute.core.security;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.direction.FwCoreDirection;
import org.lastaflute.core.direction.exception.FwRequiredAssistNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class SimplePrimaryCipher implements PrimaryCipher {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(SimplePrimaryCipher.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant director (AD) for framework. (NotNull: after initialization) */
    @Resource
    private FwAssistantDirector assistantDirector;

    /** The invertible cipher for primary values. (NotNull: after initialization) */
    protected InvertibleCryptographer invertibleCryptographer;

    /** The invertible cipher for primary values. (NotNull: after initialization) */
    protected OneWayCryptographer oneWayCryptographer;

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
        final SecurityResourceProvider provider = direction.assistSecurityResourceProvider();
        invertibleCryptographer = provider.providePrimaryInvertibleCryptographer();
        if (invertibleCryptographer == null) {
            String msg = "The provider returned null invertible cryptographer: " + provider;
            throw new FwRequiredAssistNotFoundException(msg);
        }
        oneWayCryptographer = provider.providePrimaryOneWayCryptographer();
        if (oneWayCryptographer == null) {
            String msg = "The provider returned null one-way cryptographer: " + provider;
            throw new FwRequiredAssistNotFoundException(msg);
        }
        showBootLogging();
    }

    protected FwCoreDirection assistCoreDirection() {
        return assistantDirector.assistCoreDirection();
    }

    protected void showBootLogging() {
        if (logger.isInfoEnabled()) {
            logger.info("[Primary Cipher]");
            logger.info(" invertibleCryptographer: " + invertibleCryptographer);
            logger.info(" oneWayCryptographer: " + oneWayCryptographer);
        }
    }

    // ===================================================================================
    //                                                                     Encrypt/Decrypt
    //                                                                     ===============
    @Override
    public String encrypt(String plainText) {
        return invertibleCryptographer.encrypt(plainText);
    }

    @Override
    public String decrypt(String cryptedText) {
        return invertibleCryptographer.decrypt(cryptedText);
    }

    // ===================================================================================
    //                                                                             One-Way
    //                                                                             =======
    @Override
    public String oneway(String plainText) {
        return oneWayCryptographer.oneway(plainText);
    }
}
