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
package org.lastaflute.web.hook;

import java.util.List;
import java.util.function.Supplier;

import org.dbflute.exception.EntityAlreadyDeletedException;
import org.dbflute.exception.EntityAlreadyExistsException;
import org.dbflute.exception.EntityAlreadyUpdatedException;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.exception.ExceptionTranslator;
import org.lastaflute.core.exception.LaApplicationException;
import org.lastaflute.core.exception.LaApplicationMessage;
import org.lastaflute.core.message.exception.MessagingApplicationException;
import org.lastaflute.web.api.ApiFailureResource;
import org.lastaflute.web.api.ApiManager;
import org.lastaflute.web.exception.ActionApplicationExceptionHandler;
import org.lastaflute.web.exception.MessageResponseApplicationException;
import org.lastaflute.web.login.LoginManager;
import org.lastaflute.web.login.exception.LoginFailureException;
import org.lastaflute.web.login.exception.LoginUnauthorizedException;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.response.ApiResponse;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.ruts.renderer.HtmlRenderingProvider;
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
    protected static final HtmlResponse DEFAULT_SHOW_ERRORS_FORWARD = HtmlResponse.fromForwardPath("/error/show_errors.jsp");
    protected static final String LF = "\n";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final FwAssistantDirector assistantDirector;
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
        this.assistantDirector = resource.getAssistantDirector();
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
    public ActionResponse performMonologue(ActionRuntime runtime) {
        final RuntimeException cause = runtime.getFailureCause();
        RuntimeException translated = null;
        try {
            translateException(cause);
        } catch (RuntimeException e) {
            translated = e;
        }
        final RuntimeException handlingEx = translated != null ? translated : cause;
        final ActionResponse response = handleApplicationException(runtime, handlingEx);
        if (response.isDefined()) {
            return response;
        }
        if (translated != null) {
            throw translated;
        }
        return ActionResponse.undefined();
    }

    protected void translateException(RuntimeException cause) {
        exceptionTranslator.translateException(cause);
    }

    // ===================================================================================
    //                                                               Application Exception
    //                                                               =====================
    /**
     * Handle the application exception thrown by (basically) action execute. <br>
     * Though this is same as global-exceptions settings of Struts, <br>
     * There is more flexibility than the function so you can set it here. <br>
     * This is called by callback process so you should NOT call this directly in your action.
     * @param runtime The runtime meta of action execute. (NotNull)
     * @param cause The exception thrown by (basically) action execute, might be translated. (NotNull)
     * @return The action response for application exception. (NotNull: and if defined, go to the path, UndefinedAllowed: unhandled as application exception)
     */
    protected ActionResponse handleApplicationException(ActionRuntime runtime, RuntimeException cause) {
        final ActionResponse response;
        if (cause instanceof LaApplicationException) {
            final LaApplicationException appEx = (LaApplicationException) cause;
            final ActionResponse specified = asSpecifiedApplicationException(runtime, appEx);
            response = specified.isDefined() ? specified : asEmbeddedApplicationException(runtime, appEx);
            reflectEmbeddedApplicationMessagesIfExists(runtime, appEx); // override existing messages if exists
        } else { // e.g. framework exception
            response = asDBFluteApplicationException(runtime, cause);
        }
        if (response.isDefined()) {
            showApplicationExceptionHandling(runtime, cause, response);
            if (needsApplicationExceptionApiDispatch(runtime, cause, response)) {
                return dispatchApiApplicationException(runtime, cause, response);
            }
        }
        return response;
    }

    protected ActionResponse asSpecifiedApplicationException(ActionRuntime runtime, LaApplicationException appEx) {
        return applicationExceptionHandler.handle(appEx);
    }

    protected ActionResponse asEmbeddedApplicationException(ActionRuntime runtime, LaApplicationException appEx) {
        ActionResponse response = ActionResponse.undefined();
        if (appEx instanceof LoginUnauthorizedException) {
            response = handleLoginUnauthorizedException(runtime, (LoginUnauthorizedException) appEx);
        } else if (appEx instanceof MessagingApplicationException) {
            response = handleMessagingApplicationException(runtime, (MessagingApplicationException) appEx);
        }
        if (response.isUndefined()) {
            response = handleUnknownApplicationException(appEx);
        }
        return response;
    }

    protected void reflectEmbeddedApplicationMessagesIfExists(ActionRuntime runtime, LaApplicationException appEx) {
        final List<LaApplicationMessage> messageList = appEx.getMessageList();
        if (!messageList.isEmpty()) {
            logger.debug("...Saving embedded application message as action error: {}", messageList);
            sessionManager.errors().clear(); // for overriding
            messageList.forEach(msg -> sessionManager.errors().add(msg.getMessageKey(), msg.getValues()));
        }
    }

    protected ActionResponse asDBFluteApplicationException(ActionRuntime runtime, RuntimeException cause) {
        ActionResponse response = ActionResponse.undefined();
        if (cause instanceof EntityAlreadyDeletedException) {
            response = handleEntityAlreadyDeletedException(runtime, (EntityAlreadyDeletedException) cause);
        } else if (cause instanceof EntityAlreadyUpdatedException) {
            response = handleEntityAlreadyUpdatedException(runtime, (EntityAlreadyUpdatedException) cause);
        } else if (cause instanceof EntityAlreadyExistsException) {
            response = handleEntityAlreadyExistsException(runtime, (EntityAlreadyExistsException) cause);
        }
        return response;
    }

    // -----------------------------------------------------
    //                                         Show Handling
    //                                         -------------
    protected void showApplicationExceptionHandling(ActionRuntime runtime, RuntimeException cause, ActionResponse response) {
        showAppEx(cause, () -> {
            // not show forwardTo because of forwarding log later
            final StringBuilder sb = new StringBuilder();
            sb.append("...Handling application exception:");
            sb.append("\n_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/");
            sb.append("\n[Application Exception]");
            sb.append("\n action   : ").append(runtime);
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
        // to trace it in production just in case
        // several exception is depend on circumstances
        // whether application exception or not 
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

    // -----------------------------------------------------
    //                                          API Dispatch
    //                                          ------------
    protected boolean needsApplicationExceptionApiDispatch(ActionRuntime runtime, RuntimeException cause, ActionResponse response) {
        return runtime.isApiExecute();
    }

    protected ApiResponse dispatchApiApplicationException(ActionRuntime runtime, RuntimeException cause, ActionResponse response) {
        final ApiResponse handled = apiManager.handleApplicationException(createApiFailureResource(runtime), cause);
        clearUnneededSessionErrorsForApi(handled);
        return handled;
    }

    protected ApiFailureResource createApiFailureResource(ActionRuntime runtime) { // pick up errors from session here
        return new ApiFailureResource(runtime, sessionManager.errors().get(), requestManager);
    }

    protected void clearUnneededSessionErrorsForApi(ApiResponse handled) { // clear errors from session here
        if (handled.isDefined()) { // basically true, should be handled in hook
            sessionManager.errors().clear(); // already unneeded
        }
    }

    // -----------------------------------------------------
    //                                    Login Unauthorized
    //                                    ------------------
    protected ActionResponse handleLoginUnauthorizedException(ActionRuntime runtime, LoginUnauthorizedException appEx) {
        assertLoginManagerPresent();
        if (appEx instanceof LoginFailureException) {
            saveErrors(getErrorsLoginFailureKey()); // needs to show message for user
        }
        final HtmlResponse response = redirectToLoginAction();
        appEx.mappingRedirectable(response);
        return response;
    }

    protected void assertLoginManagerPresent() {
        loginManager.orElseThrow(() -> {
            return new IllegalStateException("Not found the login manager, the application exception is mistake?");
        });
    }

    protected String getErrorsLoginFailureKey() {
        return typicalKeySupplier.getErrorsLoginFailureKey();
    }

    // -----------------------------------------------------
    //                                           Message Key
    //                                           -----------
    protected ActionResponse handleMessagingApplicationException(ActionRuntime runtime, MessagingApplicationException appEx) {
        // no save here because of saved as embedded message later
        //saveErrors(appEx.getErrors());
        if (appEx instanceof MessageResponseApplicationException) {
            return ((MessageResponseApplicationException) appEx).getResponseHook().map(hook -> {
                return hook.hook();
            }).orElseGet(() -> prepareShowErrorsForward(runtime));
        } else {
            return prepareShowErrorsForward(runtime);
        }
    }

    protected HtmlResponse prepareShowErrorsForward(ActionRuntime runtime) {
        final HtmlRenderingProvider renderingProvider = assistantDirector.assistWebDirection().assistHtmlRenderingProvider();
        final HtmlResponse response = renderingProvider.provideShowErrorsResponse(runtime);
        assertShowErrorsDefined(renderingProvider, response);
        return response;
    }

    protected void assertShowErrorsDefined(HtmlRenderingProvider renderingProvider, HtmlResponse response) {
        if (response == null) {
            throw new IllegalStateException("Not provided the response to show errors: provider=" + renderingProvider);
        }
        if (response.isUndefined()) {
            throw new IllegalStateException("Cannot return undefined response to show errors: provider=" + renderingProvider);
        }
    }

    protected HtmlResponse getShowErrorsDefaultForward() {
        return HtmlResponse.fromForwardPath(typicalKeySupplier.getShowErrorsDefaultPath());
    }

    // -----------------------------------------------------
    //                                               Unknown
    //                                               -------
    protected ActionResponse handleUnknownApplicationException(LaApplicationException appEx) {
        logger.warn("*Unknown application exception: {}", appEx.getClass(), appEx);
        return redirectToUnknownAppcalitionExceptionAction(); // basically no way
    }

    protected ActionResponse redirectToUnknownAppcalitionExceptionAction() {
        return redirectToLoginAction(); // cannot help it
    }

    // -----------------------------------------------------
    //                             (DBFlute) Already Deleted
    //                             -------------------------
    protected ActionResponse handleEntityAlreadyDeletedException(ActionRuntime runtime, EntityAlreadyDeletedException cause) {
        saveErrors(getErrorsAppAlreadyDeletedKey());
        return getErrorMessageAlreadyDeletedJsp(runtime);
    }

    protected String getErrorsAppAlreadyDeletedKey() {
        return typicalKeySupplier.getErrorsAppDbAlreadyDeletedKey();
    }

    protected ActionResponse getErrorMessageAlreadyDeletedJsp(ActionRuntime runtime) {
        return prepareShowErrorsForward(runtime); // as default
    }

    // -----------------------------------------------------
    //                              (DBFlute) Already Update
    //                              ------------------------
    protected ActionResponse handleEntityAlreadyUpdatedException(ActionRuntime runtime, EntityAlreadyUpdatedException cause) {
        saveErrors(getErrorsAppAlreadyUpdatedKey());
        return getErrorMessageAlreadyUpdatedJsp(runtime);
    }

    protected String getErrorsAppAlreadyUpdatedKey() {
        return typicalKeySupplier.getErrorsAppDbAlreadyUpdatedKey();
    }

    protected ActionResponse getErrorMessageAlreadyUpdatedJsp(ActionRuntime runtime) {
        return prepareShowErrorsForward(runtime); // as default
    }

    // -----------------------------------------------------
    //                              (DBFlute) Already Exists
    //                              ------------------------
    protected ActionResponse handleEntityAlreadyExistsException(ActionRuntime runtime, EntityAlreadyExistsException cause) {
        saveErrors(getErrorsAppAlreadyExistsKey());
        return getErrorMessageAlreadyExistsJsp(runtime);
    }

    protected String getErrorsAppAlreadyExistsKey() {
        return TypicalKey.ERRORS_APP_DB_ALREADY_EXISTS;
    };

    protected ActionResponse getErrorMessageAlreadyExistsJsp(ActionRuntime runtime) {
        return prepareShowErrorsForward(runtime); // as default
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected void saveErrors(String messageKey) {
        sessionManager.errors().save(messageKey); // cleared later if API
    }

    protected HtmlResponse redirectToLoginAction() {
        return loginManager.map(nager -> {
            return nager.redirectToLoginAction();
        }).orElseGet(() -> {
            return HtmlResponse.undefined();
        });
    }
}
