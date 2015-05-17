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
package org.lastaflute.web.callback;

import java.util.function.Supplier;

import org.dbflute.exception.EntityAlreadyDeletedException;
import org.dbflute.exception.EntityAlreadyExistsException;
import org.dbflute.exception.EntityAlreadyUpdatedException;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.exception.ExceptionTranslator;
import org.lastaflute.core.exception.LaApplicationException;
import org.lastaflute.web.api.ApiFailureResource;
import org.lastaflute.web.api.ApiManager;
import org.lastaflute.web.exception.ActionApplicationExceptionHandler;
import org.lastaflute.web.exception.MessageKeyApplicationException;
import org.lastaflute.web.login.LoginManager;
import org.lastaflute.web.login.exception.LoginFailureException;
import org.lastaflute.web.login.exception.LoginTimeoutException;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 */
public class TypicalGodHandMonologue {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(TypicalGodHandMonologue.class);
    protected static final String LF = "\n";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RequestManager requestManager;
    protected final SessionManager sessionManager;
    protected final OptionalThing<LoginManager> loginManager;
    protected final ApiManager apiManager;
    protected final ExceptionTranslator exceptionTranslator;
    protected final TypicalEmbeddedKeySupplier typicalKeySupplier;
    protected final ActionApplicationExceptionHandler applicationExceptionHandler;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public TypicalGodHandMonologue(TypicalGodHandResource resource, TypicalEmbeddedKeySupplier typicalKeySupplier,
            ActionApplicationExceptionHandler applicationExceptionHandler) {
        this.requestManager = resource.getRequestManager();
        this.sessionManager = resource.getSessionManager();
        this.loginManager = resource.getLoginManager();
        this.apiManager = resource.getApiManager();
        this.exceptionTranslator = resource.getExceptionTranslator();
        this.typicalKeySupplier = typicalKeySupplier;
        this.applicationExceptionHandler = applicationExceptionHandler;
    }

    // ===================================================================================
    //                                                                           Monologue
    //                                                                           =========
    public ActionResponse performMonologue(ActionRuntimeMeta runtimeMeta) {
        final RuntimeException cause = runtimeMeta.getFailureCause();
        RuntimeException translated = null;
        try {
            translateException(cause);
        } catch (RuntimeException e) {
            translated = e;
        }
        final RuntimeException handlingEx = translated != null ? translated : cause;
        final ActionResponse response = handleApplicationException(runtimeMeta, handlingEx);
        if (response.isPresent()) {
            return response;
        }
        if (translated != null) {
            throw translated;
        }
        return ActionResponse.empty();
    }

    protected void translateException(RuntimeException cause) {
        exceptionTranslator.translateException(cause);
    }

    // ===================================================================================
    //                                                               Application Exception
    //                                                               =====================
    /**
     * Handle the application exception thrown by (basically) action execute. <br>
     * Though this is same as global-exceptions settings of Struts,
     * There is more flexibility than the function so you can set it here. <br>
     * This is called by callback process so you should NOT call this directly in your action.
     * @param executeMeta The meta of action execute. (NotNull)
     * @param cause The exception thrown by (basically) action execute, might be translated. (NotNull)
     * @return The forward path. (NullAllowed: if not null, it goes to the path)
     */
    protected ActionResponse handleApplicationException(ActionRuntimeMeta executeMeta, RuntimeException cause) { // called by callback
        final ActionResponse forwardTo = doHandleApplicationException(executeMeta, cause);
        showApplicationExceptionHandlingIfNeeds(cause, forwardTo);
        if (needsApplicationExceptionApiDispatch(executeMeta, forwardTo)) {
            return dispatchApiApplicationException(executeMeta, cause);
        }
        return forwardTo;
    }

    // -----------------------------------------------------
    //                                     Actually Handling
    //                                     -----------------
    protected ActionResponse doHandleApplicationException(ActionRuntimeMeta executeMeta, RuntimeException cause) {
        ActionResponse forwardTo = ActionResponse.empty();
        if (cause instanceof LaApplicationException) {
            final LaApplicationException appEx = (LaApplicationException) cause;
            forwardTo = doHandleSpecifiedApplicationException(appEx);
            if (forwardTo.isEmpty()) {
                forwardTo = doHandleEmbeddedApplicationException(appEx);
            }
            reflectEmbeddedApplicationMessagesIfExists(appEx); // override existing messages if exists
        } else { // e.g. framework exception
            forwardTo = doHandleDBFluteApplicationException(cause);
        }
        return forwardTo;
    }

    protected ActionResponse doHandleSpecifiedApplicationException(LaApplicationException appEx) {
        return applicationExceptionHandler.handle(appEx);
    }

    protected ActionResponse doHandleEmbeddedApplicationException(LaApplicationException appEx) {
        ActionResponse forwardTo = ActionResponse.empty();
        if (appEx instanceof LoginFailureException) {
            forwardTo = handleLoginFailureException((LoginFailureException) appEx);
        } else if (appEx instanceof LoginTimeoutException) {
            forwardTo = handleLoginTimeoutException((LoginTimeoutException) appEx);
        } else if (appEx instanceof MessageKeyApplicationException) {
            forwardTo = handleMessageKeyApplicationException((MessageKeyApplicationException) appEx);
        }
        if (forwardTo.isEmpty()) {
            forwardTo = handleUnknownApplicationException(appEx);
        }
        return forwardTo;
    }

    protected void reflectEmbeddedApplicationMessagesIfExists(LaApplicationException appEx) {
        final String errorsKey = appEx.getErrorKey();
        if (errorsKey != null) {
            logger.debug("...Saving embedded application message as action error: {}", errorsKey);
            sessionManager.errors().save(errorsKey, appEx.getErrorArgs());
        }
    }

    protected ActionResponse doHandleDBFluteApplicationException(RuntimeException cause) {
        ActionResponse forwardTo = ActionResponse.empty();
        if (cause instanceof EntityAlreadyDeletedException) {
            forwardTo = handleEntityAlreadyDeletedException((EntityAlreadyDeletedException) cause);
        } else if (cause instanceof EntityAlreadyUpdatedException) {
            forwardTo = handleEntityAlreadyUpdatedException((EntityAlreadyUpdatedException) cause);
        } else if (cause instanceof EntityAlreadyExistsException) {
            forwardTo = handleEntityAlreadyExistsException((EntityAlreadyExistsException) cause);
        }
        return forwardTo;
    }

    // -----------------------------------------------------
    //                                         Show Handling
    //                                         -------------
    protected void showApplicationExceptionHandlingIfNeeds(RuntimeException cause, ActionResponse response) {
        if (response.isEmpty()) {
            return;
        }
        showAppEx(cause, () -> {
            /* not show forwardTo because of forwarding log later */
            final StringBuilder sb = new StringBuilder();
            sb.append("...Handling application exception:");
            sb.append("\n_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/");
            sb.append("\n[Application Exception]");
            sb.append("\n response : ").append(response);
            sessionManager.errors().get().ifPresent(errors -> {
                sb.append("\n messages : ").append(errors.toString());
            });
            buildApplicationExceptionStackTrace(cause, sb, 0);
            sb.append("\n_/_/_/_/_/_/_/_/_/_/");
            return sb.toString();
        });
    }

    protected void showAppEx(RuntimeException cause, Supplier<String> msgSupplier) {
        // basically trace in production just in case
        // if it's noisy and unneeded, override this method
        if (logger.isInfoEnabled()) {
            logger.info(msgSupplier.get());
        }
    }

    protected void buildApplicationExceptionStackTrace(Throwable cause, StringBuilder sb, int nestLevel) {
        sb.append(LF).append(nestLevel > 0 ? "Caused by: " : "");
        sb.append(cause.getClass().getName()).append(": ").append(cause.getMessage());
        final StackTraceElement[] stackTrace = cause.getStackTrace();
        if (stackTrace == null) { // just in case
            return;
        }
        final int limit = nestLevel == 0 ? 10 : 3;
        int index = 0;
        for (StackTraceElement element : stackTrace) {
            if (index > limit) { // not all because it's not error
                sb.append(LF).append("  ...");
                break;
            }
            final String className = element.getClassName();
            final String fileName = element.getFileName(); // might be null
            final int lineNumber = element.getLineNumber();
            final String methodName = element.getMethodName();
            sb.append(LF).append("  at ").append(className).append(".").append(methodName);
            sb.append("(").append(fileName);
            if (lineNumber >= 0) {
                sb.append(":").append(lineNumber);
            }
            sb.append(")");
            ++index;
        }
        final Throwable nested = cause.getCause();
        if (nested != null && nested != cause) {
            buildApplicationExceptionStackTrace(nested, sb, nestLevel + 1);
        }
    }

    protected boolean needsApplicationExceptionApiDispatch(ActionRuntimeMeta executeMeta, ActionResponse forwardTo) {
        return forwardTo.isPresent() && executeMeta.isApiAction();
    }

    protected ActionResponse dispatchApiApplicationException(ActionRuntimeMeta executeMeta, RuntimeException cause) {
        final ApiFailureResource resource = createApiApplicationExceptionResource();
        return apiManager.handleApplicationException(resource, executeMeta, cause);
    }

    protected ApiFailureResource createApiApplicationExceptionResource() {
        return new ApiFailureResource(sessionManager.errors().get(), requestManager);
    }

    // -----------------------------------------------------
    //                             (DBFlute) Already Deleted
    //                             -------------------------
    protected ActionResponse handleEntityAlreadyDeletedException(EntityAlreadyDeletedException cause) {
        saveErrors(getErrorsAppAlreadyDeletedKey());
        return getErrorMessageAlreadyDeletedJsp();
    }

    protected String getErrorsAppAlreadyDeletedKey() {
        return typicalKeySupplier.getErrorsAppAlreadyDeletedKey();
    }

    protected ActionResponse getErrorMessageAlreadyDeletedJsp() {
        return getErrorMessageForward(); // as default
    }

    // -----------------------------------------------------
    //                              (DBFlute) Already Update
    //                              ------------------------
    protected ActionResponse handleEntityAlreadyUpdatedException(EntityAlreadyUpdatedException cause) {
        saveErrors(getErrorsAppAlreadyUpdatedKey());
        return getErrorMessageAlreadyUpdatedJsp();
    }

    protected String getErrorsAppAlreadyUpdatedKey() {
        return typicalKeySupplier.getErrorsAppAlreadyUpdatedKey();
    }

    protected ActionResponse getErrorMessageAlreadyUpdatedJsp() {
        return getErrorMessageForward(); // as default
    }

    // -----------------------------------------------------
    //                              (DBFlute) Already Exists
    //                              ------------------------
    protected ActionResponse handleEntityAlreadyExistsException(EntityAlreadyExistsException cause) {
        saveErrors(getErrorsAppAlreadyExistsKey());
        return getErrorMessageAlreadyExistsJsp();
    }

    protected String getErrorsAppAlreadyExistsKey() {
        return TypicalKey.ERRORS_APP_ALREADY_EXISTS;
    };

    protected ActionResponse getErrorMessageAlreadyExistsJsp() {
        return getErrorMessageForward(); // as default
    }

    // -----------------------------------------------------
    //                                         Login Failure
    //                                         -------------
    protected ActionResponse handleLoginFailureException(LoginFailureException appEx) {
        assertLoginManagerExists(appEx);
        saveErrors(getErrorsLoginFailureKey());
        return redirectToLoginAction();
    }

    protected String getErrorsLoginFailureKey() {
        return typicalKeySupplier.getErrorsLoginFailureKey();
    }

    // -----------------------------------------------------
    //                                         Login Timeout
    //                                         -------------
    protected ActionResponse handleLoginTimeoutException(LoginTimeoutException appEx) {
        assertLoginManagerExists(appEx);
        return redirectToLoginAction(); // no message because of rare case
    }

    protected void assertLoginManagerExists(RuntimeException appEx) {
        if (!loginManager.isPresent()) {
            String msg = "Not found the login manager, this application exception is mistake?";
            throw new IllegalStateException(msg, appEx);
        }
    }

    // -----------------------------------------------------
    //                                           Message Key
    //                                           -----------
    protected ActionResponse handleMessageKeyApplicationException(MessageKeyApplicationException appEx) {
        // no save here because of saved later
        //saveErrors(appEx.getErrors());
        return getErrorMessageForward();
    }

    protected HtmlResponse getErrorMessageForward() {
        return HtmlResponse.fromForwardPath(typicalKeySupplier.getErrorMessageForwardPath());
    }

    // -----------------------------------------------------
    //                                               Unknown
    //                                               -------
    protected ActionResponse handleUnknownApplicationException(LaApplicationException appEx) {
        logger.info("*Unknown application exception: {}", appEx.getClass(), appEx);
        return redirectToUnknownAppcalitionExceptionAction(); // basically no way
    }

    protected ActionResponse redirectToUnknownAppcalitionExceptionAction() {
        if (loginManager == null) { // if no-use-login system
            return ActionResponse.empty(); // because of non login exception, treat it as system exception
        }
        return redirectToLoginAction(); // basically no way, login action just in case
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected void saveErrors(String messageKey) {
        sessionManager.errors().save(messageKey);
    }

    protected ActionResponse redirectToLoginAction() {
        return loginManager.map(nager -> {
            return HtmlResponse.fromRedirectPath(nager.redirectToLoginAction());
        }).orElseGet(() -> {
            return HtmlResponse.empty();
        });
    }
}
