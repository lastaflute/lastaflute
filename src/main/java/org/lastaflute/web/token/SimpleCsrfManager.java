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
package org.lastaflute.web.token;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.di.util.UUID;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.direction.FwWebDirection;
import org.lastaflute.web.exception.CrossSiteRequestForgeriesForbiddenException;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.request.ResponseManager;
import org.lastaflute.web.servlet.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 * @since 0.4.0 (2015/06/22 Monday)
 */
public class SimpleCsrfManager implements CsrfManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(SimpleCsrfManager.class);
    public static final String CSRF_TOKEN_HEADER = "X-CSRF-TOKEN";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant directory (AD) for framework. (NotNull: after initialization) */
    @Resource
    protected FwAssistantDirector assistantDirector;

    @Resource
    protected RequestManager requestManager;

    @Resource
    protected ResponseManager responseManager;

    @Resource
    protected SessionManager sessionManager;

    protected CsrfResourceProvider resourceProvider; // null allowed

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    @PostConstruct
    public void initialize() {
        final FwWebDirection direction = assistWebDirection();
        resourceProvider = direction.assistCsrfResourceProvider();
        showBootLogging();
    }

    protected FwWebDirection assistWebDirection() {
        return assistantDirector.assistWebDirection();
    }

    protected void showBootLogging() {
        if (logger.isInfoEnabled()) {
            logger.info("[Csrf Manager]");
            logger.info(" resourceProvider: " + resourceProvider);
        }
    }

    // ===================================================================================
    //                                                                      Token Handling
    //                                                                      ==============
    @Override
    public void beginToken() {
        final String token = generateToken();
        responseManager.addHeader(getTokenHeaderName(), token);
        sessionManager.setAttribute(LastaWebKey.CSRF_TOKEN_KEY, token);
    }

    protected String generateToken() {
        if (resourceProvider != null) {
            final CsrfTokenGenerator generator = resourceProvider.provideTokenGenerator();
            if (generator != null) {
                final String generated = generator.generate();
                if (generated == null) {
                    throw new IllegalStateException("Returned null from provided token generator: " + generator);
                }
                return generated;
            }
        }
        return UUID.create();
    }

    @Override
    public void verifyToken() {
        requestManager.getHeader(getTokenHeaderName()).ifPresent(headerToken -> {
            sessionManager.getAttribute(LastaWebKey.CSRF_TOKEN_KEY, String.class).ifPresent(savedToken -> {
                if (!headerToken.equals(savedToken)) {
                    throwCsrfHeaderSavedTokenNotMatchedException(headerToken, savedToken);
                }
            }).orElse(() -> {
                throwCsrfHeaderSavedTokenNotMatchedException(headerToken, null);
            });
        }).orElse(() -> {
            throwCsrfHeaderNotFoundException();
        });
    }

    protected String getTokenHeaderName() {
        if (resourceProvider != null) {
            final String provided = resourceProvider.provideTokenHeaderName();
            if (provided != null) {
                return provided;
            }
        }
        return CSRF_TOKEN_HEADER;
    }

    protected void throwCsrfHeaderSavedTokenNotMatchedException(String headerToken, String savedToken) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Forbidden request as Cross Site Request Forgeries.");
        br.addItem("Advice");
        br.addElement("Not match the header token with saved token in session.");
        br.addItem("Request Path");
        br.addElement(requestManager.getRequestPathAndQuery());
        setupSessionInfo(br);
        br.addItem("Header Token");
        br.addElement(headerToken);
        br.addItem("Saved Token");
        br.addElement(savedToken);
        final String msg = br.buildExceptionMessage();
        throw new CrossSiteRequestForgeriesForbiddenException(msg);
    }

    protected void throwCsrfHeaderNotFoundException() {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Forbidden request as Cross Site Request Forgeries.");
        br.addItem("Advice");
        br.addElement("Not found the CSRF header in the request.");
        br.addItem("Request Path");
        br.addElement(requestManager.getRequestPathAndQuery());
        setupSessionInfo(br);
        final String msg = br.buildExceptionMessage();
        throw new CrossSiteRequestForgeriesForbiddenException(msg);
    }

    protected void setupSessionInfo(ExceptionMessageBuilder br) {
        br.addItem("Session");
        final List<String> attributeNameList = sessionManager.getAttributeNameList();
        if (!attributeNameList.isEmpty()) {
            for (String attr : attributeNameList) {
                br.addElement(attr + " = " + sessionManager.getAttribute(attr, Object.class).orElse(null));
            }
        } else {
            br.addElement("{}");
        }
    }
}
