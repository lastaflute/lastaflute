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
package org.lastaflute.web.token;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

/**
 * @author jflute
 * @since 0.4.0 (2015/06/22 Monday)
 */
public class SimpleCsrfManager implements CsrfManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(SimpleCsrfManager.class);
    protected static final String DEFAULT_TOKEN_HEADER = "X-CSRF-TOKEN";
    protected static final String DEFAULT_TOKEN_PARAMETER = "_csrf";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant director (AD) for framework. (NotNull: after initialization) */
    @Resource
    private FwAssistantDirector assistantDirector;

    @Resource
    private RequestManager requestManager;

    @Resource
    private ResponseManager responseManager;

    @Resource
    private SessionManager sessionManager;

    /** The header name of CSRF token on request and response. (NotNull, Changeable) */
    protected String tokenHeaderName = DEFAULT_TOKEN_HEADER;

    /** The parameter name of CSRF token on request parameter. (NotNull, Changeable) */
    protected String tokenParameterName = DEFAULT_TOKEN_PARAMETER;

    /** The generator of CSRF token. (NotNull: after initialization) */
    protected CsrfTokenGenerator tokenGenerator;

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    @PostConstruct
    public void initialize() {
        final FwWebDirection direction = assistWebDirection();
        final CsrfResourceProvider resourceProvider = direction.assistCsrfResourceProvider();
        if (resourceProvider != null) {
            final String providedHeaderName = resourceProvider.provideTokenHeaderName();
            if (providedHeaderName != null) {
                tokenHeaderName = providedHeaderName;
            }
            final String providedParameterName = resourceProvider.provideTokenParameterName();
            if (providedParameterName != null) {
                tokenParameterName = providedParameterName;
            }
            final CsrfTokenGenerator providedGenerator = resourceProvider.provideTokenGenerator();
            if (providedGenerator != null) {
                tokenGenerator = providedGenerator;
            }
        }
        if (tokenGenerator == null) {
            tokenGenerator = createDefaultTokenGenerator();
        }
        showBootLogging();
    }

    protected FwWebDirection assistWebDirection() {
        return assistantDirector.assistWebDirection();
    }

    protected CsrfTokenGenerator createDefaultTokenGenerator() {
        return () -> UUID.create();
    }

    protected void showBootLogging() {
        if (logger.isInfoEnabled()) {
            logger.info("[Csrf Manager]");
            logger.info(" tokenHeaderName: " + tokenHeaderName);
            logger.info(" tokenParameterName: " + tokenParameterName);
            logger.info(" tokenGenerator: " + tokenGenerator);
        }
    }

    // ===================================================================================
    //                                                                        CSRF Process
    //                                                                        ============
    @Override
    public void beginToken() {
        final String token = generateToken();
        responseManager.addHeader(getTokenHeaderName(), token);
        saveToken(token);
    }

    protected String generateToken() {
        final String generated = tokenGenerator.generate();
        if (generated == null) {
            throw new IllegalStateException("Returned null from token generator: " + tokenGenerator);
        }
        return generated;
    }

    @Override
    public void verifyToken() {
        getRequestHeaderToken().ifPresent(headerToken -> {
            doVerifyToken(headerToken);
        }).orElse(() -> {
            getRequestParameterToken().ifPresent(parameterToken -> {
                doVerifyToken(parameterToken);
            }).orElse(() -> {
                throwCsrfHeaderNotFoundException();
            });
        });
    }

    protected void doVerifyToken(String requestedToken) {
        getSavedToken().ifPresent(savedToken -> {
            if (!requestedToken.equals(savedToken)) {
                throwCsrfHeaderSavedTokenNotMatchedException(requestedToken, savedToken);
            }
        }).orElse(() -> {
            throwCsrfHeaderSavedTokenNotMatchedException(requestedToken, null);
        });
    }

    protected void throwCsrfHeaderSavedTokenNotMatchedException(String headerToken, String savedToken) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Forbidden request as Cross Site Request Forgeries.");
        br.addItem("Advice");
        br.addElement("Not match the header token with saved token in session.");
        br.addItem("Request Path");
        br.addElement(requestManager.getRequestPathAndQuery());
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
        final String msg = br.buildExceptionMessage();
        throw new CrossSiteRequestForgeriesForbiddenException(msg);
    }

    // ===================================================================================
    //                                                                      Token Handling
    //                                                                      ==============
    @Override
    public String getTokenHeaderName() {
        return tokenHeaderName;
    }

    @Override
    public String getTokenParameterName() {
        return tokenParameterName;
    }

    @Override
    public OptionalThing<String> getRequestHeaderToken() {
        return requestManager.getHeader(getTokenHeaderName());
    }

    @Override
    public OptionalThing<String> getRequestParameterToken() {
        return requestManager.getParameter(getTokenParameterName());
    }

    @Override
    public OptionalThing<String> getSavedToken() {
        return sessionManager.getAttribute(getTokenSavingKey(), String.class);
    }

    protected void saveToken(String token) {
        sessionManager.setAttribute(getTokenSavingKey(), token);
    }

    protected String getTokenSavingKey() {
        return LastaWebKey.CSRF_TOKEN_KEY;
    }
}
