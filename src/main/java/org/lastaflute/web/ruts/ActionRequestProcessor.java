/*
 * Copyright 2015-2019 the original author or authors.
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
package org.lastaflute.web.ruts;

import java.io.IOException;
import java.time.LocalDateTime;

import javax.servlet.ServletException;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.db.jta.stage.NoneTransactionStage;
import org.lastaflute.db.jta.stage.TransactionStage;
import org.lastaflute.db.jta.stage.VestibuleTxProvider;
import org.lastaflute.web.path.ActionAdjustmentProvider;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.config.ModuleConfig;
import org.lastaflute.web.ruts.inoutlogging.InOutLogKeeper;
import org.lastaflute.web.ruts.inoutlogging.InOutLogger;
import org.lastaflute.web.ruts.process.ActionFormMapper;
import org.lastaflute.web.ruts.process.ActionResponseReflector;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.ruts.process.actioncoins.ActionCoinsHelper;
import org.lastaflute.web.ruts.process.pathparam.RequestPathParam;
import org.lastaflute.web.ruts.renderer.HtmlRenderer;
import org.lastaflute.web.ruts.renderer.HtmlRenderingProvider;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author jflute
 */
public class ActionRequestProcessor {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                 Initialized Component
    //                                 ---------------------
    protected ModuleConfig moduleConfig;
    protected ActionCoinsHelper actionCoinsHelper;
    protected ActionFormMapper actionFormMapper;
    protected InOutLogger inOutLogger; // null allowed if unused logging

    // -----------------------------------------------------
    //                                     Lazy-Loaded Cache
    //                                     -----------------
    // don't use directly
    /**
     * The cache of assistant director, which can be lazy-loaded when you get it.
     * Don't use these variables directly, you should use the getter. (NotNull: after lazy-load)
     */
    protected FwAssistantDirector cachedAssistantDirector;

    /** The cache of request manager, just same as cachedAssistantDirector. (NotNull: after lazy-load) */
    protected RequestManager cachedRequestManager;

    /** The cache of transaction stage, just same as cachedAssistantDirector. (NotNull: after lazy-load) */
    protected TransactionStage cachedTransactionStage;

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    public void initialize(ModuleConfig moduleConfig) throws ServletException {
        this.moduleConfig = moduleConfig;
        this.actionCoinsHelper = createActionCoinHelper(moduleConfig);
        this.actionFormMapper = createActionFormPopulator(moduleConfig);
        this.inOutLogger = prepareInOutLogger(moduleConfig);
    }

    protected ActionCoinsHelper createActionCoinHelper(ModuleConfig moduleConfig) {
        return new ActionCoinsHelper(moduleConfig, getAssistantDirector(), getRequestManager());
    }

    protected ActionFormMapper createActionFormPopulator(ModuleConfig moduleConfig) {
        return new ActionFormMapper(moduleConfig, getAssistantDirector(), getRequestManager());
    }

    protected InOutLogger prepareInOutLogger(ModuleConfig moduleConfig) {
        return InOutLogKeeper.isEnabled(getRequestManager()) ? createInOutLogger() : null;
    }

    protected InOutLogger createInOutLogger() {
        return new InOutLogger();
    }

    // ===================================================================================
    //                                                                             Process
    //                                                                             =======
    public void process(ActionExecute execute, RequestPathParam pathParam) throws IOException, ServletException {
        // initializing and clearing thread cache here so you can use thread cache in your action execute
        final boolean exists = ThreadCacheContext.exists();
        try {
            if (!exists) { // inherits existing cache when nested call e.g. forward
                ThreadCacheContext.initialize();
            }
            final ActionRuntime runtime = createActionRuntime(execute, pathParam);
            beforeFire();
            try {
                fire(runtime); // #to_action
            } catch (RuntimeException | IOException | ServletException e) {
                catchFire(e);
                throw e;
            } finally {
                finallyFire(runtime);
            }
        } finally {
            if (!exists) {
                ThreadCacheContext.clear();
            }
        }
    }

    protected ActionRuntime createActionRuntime(ActionExecute execute, RequestPathParam pathParam) {
        return new ActionRuntime(getRequestManager().getRequestPath(), execute, pathParam);
    }

    // -----------------------------------------------------
    //                                        Before/Finally
    //                                        --------------
    protected void beforeFire() {
        final LocalDateTime beginTime = askBeginTime();
        final String processHash = askProcessHash(beginTime);
        ThreadCacheContext.registerBeginTime(beginTime);
        ThreadCacheContext.registerProcessHash(processHash);
        beginInOutLoggingIfNeeds(beginTime, processHash);
    }

    protected LocalDateTime askBeginTime() { // not in transaction but flash just in case
        final TimeManager timeManager = getRequestManager().getTimeManager();
        return DfTypeUtil.toLocalDateTime(timeManager.flashDate(), timeManager.getBusinessTimeZone());
    }

    protected String askProcessHash(LocalDateTime beginTime) {
        final String bestEffortUnique = beginTime.toString() + Thread.currentThread().getName();
        return Integer.toHexString(bestEffortUnique.hashCode());
    }

    protected void catchFire(Throwable cause) {
        // action exceptions are already translated to ServletException here
        // then basically runtime has it as failure cause so no need to keep in this case
        // but it can be framework cause even if application throws and catches as business handling
        keepInOutLogFrameworkCauseIfNeeds(cause);
    }

    protected void finallyFire(ActionRuntime runtime) {
        showInOutLogIfNeeds(runtime);
    }

    // ===================================================================================
    //                                                                               Fire
    //                                                                              ======
    /**
     * Fire the action, creating, populating, performing and to next.
     * @param runtime The runtime meta of action execute, which has action execute, path parameter and states. (NotNull)
     * @throws IOException When the action fails about the IO.
     * @throws ServletException When the action fails about the Servlet.
     */
    protected void fire(ActionRuntime runtime) throws IOException, ServletException {
        final ActionResponseReflector reflector = createResponseReflector(runtime);
        ready(runtime, reflector);

        final OptionalThing<VirtualForm> form = prepareActionForm(runtime);
        populateParameter(runtime, form);

        final VirtualAction action = createAction(runtime, reflector);
        final NextJourney journey = performAction(action, form, runtime); // #to_action

        toNext(runtime, journey);
    }

    // ===================================================================================
    //                                                                               Ready
    //                                                                               =====
    protected ActionResponseReflector createResponseReflector(ActionRuntime runtime) {
        final ActionAdjustmentProvider adjustmentProvider = getAssistantDirector().assistWebDirection().assistActionAdjustmentProvider();
        return new ActionResponseReflector(runtime, getRequestManager(), adjustmentProvider);
    }

    protected void ready(ActionRuntime runtime, ActionResponseReflector reflector) {
        actionCoinsHelper.prepareRequestClientErrorHandlingIfApi(runtime, reflector);
        actionCoinsHelper.prepareRequestServerErrorHandlingIfApi(runtime, reflector);
        actionCoinsHelper.saveRuntimeToRequest(runtime);
        actionCoinsHelper.removeCachedMessages();
        actionCoinsHelper.resolveLocale(runtime);
    }

    // ===================================================================================
    //                                                                         Action Form
    //                                                                         ===========
    public OptionalThing<VirtualForm> prepareActionForm(ActionRuntime runtime) {
        final ActionExecute execute = runtime.getActionExecute();
        final OptionalThing<VirtualForm> optForm = execute.createActionForm();
        optForm.ifPresent(form -> saveFormToRequest(execute, form)); // to use form tag
        runtime.manageActionForm(optForm); // to use in action hook
        return optForm;
    }

    protected void saveFormToRequest(ActionExecute execute, VirtualForm value) {
        getRequestManager().setAttribute(execute.getFormMeta().get().getFormKey(), value);
    }

    protected void populateParameter(ActionRuntime runtime, OptionalThing<VirtualForm> form) throws ServletException {
        actionFormMapper.populateParameter(runtime, form);
    }

    // ===================================================================================
    //                                                                              Action
    //                                                                              ======
    public VirtualAction createAction(ActionRuntime runtime, ActionResponseReflector reflector) {
        return newGodHandableAction(runtime, reflector, prepareVestibuleTxStage(), getRequestManager());
    }

    protected TransactionStage prepareVestibuleTxStage() {
        final VestibuleTxProvider provider = getAssistantDirector().assistDbDirection().assistVestibuleTxProvider();
        if (provider != null && provider.isSuppressed()) {
            return NoneTransactionStage.DEFAULT_INSTANCE;
        } else { // mainly here
            return getTransactionStage();
        }
    }

    protected GodHandableAction newGodHandableAction(ActionRuntime runtime, ActionResponseReflector reflector, TransactionStage stage,
            RequestManager requestManager) {
        return new GodHandableAction(runtime, reflector, stage, requestManager);
    }

    protected NextJourney performAction(VirtualAction action, OptionalThing<VirtualForm> form, ActionRuntime runtime)
            throws ServletException {
        try {
            return action.execute(form); // #to_action
        } catch (RuntimeException e) {
            return handleActionFailureException(action, form, runtime, e);
        } finally {
            actionCoinsHelper.clearContextJustInCase();
        }
    }

    protected NextJourney handleActionFailureException(VirtualAction action, OptionalThing<VirtualForm> optForm, ActionRuntime runtime,
            RuntimeException cause) throws ServletException {
        throw new ServletException(cause);
    }

    // ===================================================================================
    //                                                                             to Next
    //                                                                             =======
    protected void toNext(ActionRuntime runtime, NextJourney journey) throws IOException, ServletException {
        if (journey.hasJourneyProvider()) { // e.g. HTML/JSON response
            journey.getJourneyProvider().bonVoyage();
        }
        if (journey.hasViewRouting()) { // basically HTML response
            if (journey.isRedirectTo()) {
                doRedirect(runtime, journey);
            } else {
                final HtmlRenderer renderer = prepareHtmlRenderer(runtime, journey);
                renderer.render(getRequestManager(), runtime, journey);
            }
        }
        // do nothing if undefined
    }

    protected void doRedirect(ActionRuntime runtime, NextJourney journey) throws IOException {
        getRequestManager().getResponseManager().redirect(journey);
    }

    protected HtmlRenderer prepareHtmlRenderer(ActionRuntime runtime, NextJourney journey) {
        final HtmlRenderingProvider provider = getAssistantDirector().assistWebDirection().assistHtmlRenderingProvider();
        final HtmlRenderer renderer = provider.provideRenderer(runtime, journey);
        if (renderer == null) {
            throwHtmlRenderingProviderReturnNullException(runtime, journey);
        }
        return renderer;
    }

    protected void throwHtmlRenderingProviderReturnNullException(ActionRuntime runtime, NextJourney journey) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The provideRenderer() returned null.");
        br.addItem("Action Runtime");
        br.addElement(runtime);
        br.addItem("Next Journey");
        br.addElement(journey);
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg);
    }

    // ===================================================================================
    //                                                                       InOut Logging
    //                                                                       =============
    protected void beginInOutLoggingIfNeeds(LocalDateTime beginTime, String processHash) {
        InOutLogKeeper.prepare(getRequestManager()).ifPresent(keeper -> {
            keeper.keepBeginDateTime(beginTime);
            keeper.keepProcessHash(processHash);
        });
    }

    protected void keepInOutLogFrameworkCauseIfNeeds(Throwable frameworkCause) {
        InOutLogKeeper.prepare(getRequestManager()).ifPresent(keeper -> {
            keeper.keepFrameworkCause(frameworkCause);
        });
    }

    protected void showInOutLogIfNeeds(ActionRuntime runtime) {
        final RequestManager requestManager = getRequestManager();
        InOutLogKeeper.prepare(requestManager).ifPresent(keeper -> {
            if (inOutLogger != null) { // double check just in case
                inOutLogger.show(requestManager, runtime, keeper);
            }
        });
    }

    // ===================================================================================
    //                                                                           Component
    //                                                                           =========
    protected FwAssistantDirector getAssistantDirector() {
        if (cachedAssistantDirector != null) {
            return cachedAssistantDirector;
        }
        synchronized (this) {
            if (cachedAssistantDirector != null) {
                return cachedAssistantDirector;
            }
            cachedAssistantDirector = ContainerUtil.getComponent(FwAssistantDirector.class);
        }
        return cachedAssistantDirector;
    }

    protected RequestManager getRequestManager() {
        if (cachedRequestManager != null) {
            return cachedRequestManager;
        }
        synchronized (this) {
            if (cachedRequestManager != null) {
                return cachedRequestManager;
            }
            cachedRequestManager = ContainerUtil.getComponent(RequestManager.class);
        }
        return cachedRequestManager;
    }

    protected TransactionStage getTransactionStage() {
        if (cachedTransactionStage != null) {
            return cachedTransactionStage;
        }
        synchronized (this) {
            if (cachedTransactionStage != null) {
                return cachedTransactionStage;
            }
            cachedTransactionStage = ContainerUtil.getComponent(TransactionStage.class);
        }
        return cachedTransactionStage;
    }
}
