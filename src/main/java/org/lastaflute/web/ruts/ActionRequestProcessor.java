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
package org.lastaflute.web.ruts;

import java.io.IOException;

import javax.servlet.ServletException;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.db.jta.stage.TransactionStage;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.config.ModuleConfig;
import org.lastaflute.web.ruts.process.ActionCoinHelper;
import org.lastaflute.web.ruts.process.ActionFormMapper;
import org.lastaflute.web.ruts.process.ActionRequestResource;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.request.ResponseManager;

/**
 * @author modified by jflute (originated in Seasar and Struts)
 */
public class ActionRequestProcessor {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                 Initialized Component
    //                                 ---------------------
    protected ModuleConfig moduleConfig;
    protected ActionCoinHelper actionCoinHelper;
    protected ActionFormMapper actionFormMapper;

    // -----------------------------------------------------
    //                                     Lazy-Loaded Cache
    //                                     -----------------
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
        this.actionCoinHelper = createActionCoinHelper(moduleConfig);
        this.actionFormMapper = createActionFormPopulator(moduleConfig);
    }

    protected ActionCoinHelper createActionCoinHelper(ModuleConfig moduleConfig) {
        return new ActionCoinHelper(moduleConfig, getAssistantDirector(), getRequestManager());
    }

    protected ActionFormMapper createActionFormPopulator(ModuleConfig moduleConfig) {
        return new ActionFormMapper(moduleConfig, getAssistantDirector(), getRequestManager());
    }

    // ===================================================================================
    //                                                                             Process
    //                                                                             =======
    public void process(ActionExecute execute, ActionRequestResource resource) throws IOException, ServletException {
        // initializing and clearing thread cache here so you can use thread cache in your action execute
        final boolean exists = ThreadCacheContext.exists();
        try {
            if (!exists) { // inherits existing cache when nested call e.g. forward
                ThreadCacheContext.initialize();
            }
            doProcess(execute, resource); // #to_action
        } finally {
            if (!exists) {
                ThreadCacheContext.clear();
            }
        }
    }

    // ===================================================================================
    //                                                                           Main Flow
    //                                                                           =========
    protected void doProcess(ActionExecute execute, ActionRequestResource resource) throws IOException, ServletException {
        ready();
        final OptionalThing<VirtualActionForm> form = prepareActionForm(execute);
        populateParameter(execute, form);
        final VirtualAction action = createAction(execute, resource);
        final NextJourney journey = performAction(action, form, execute); // #to_action
        toNext(execute, journey);
    }

    protected void ready() {
        actionCoinHelper.removeCachedMessages();
    }

    // ===================================================================================
    //                                                                         Action Form
    //                                                                         ===========
    public OptionalThing<VirtualActionForm> prepareActionForm(ActionExecute execute) {
        final OptionalThing<VirtualActionForm> parameterForm = execute.createActionForm();
        parameterForm.ifPresent(form -> saveFormToRequest(execute, form)); // to use form tag
        return parameterForm;
    }

    protected void saveFormToRequest(ActionExecute execute, VirtualActionForm value) {
        getRequestManager().setAttribute(execute.getFormMeta().get().getFormKey(), value);
    }

    protected void populateParameter(ActionExecute execute, OptionalThing<VirtualActionForm> form) throws IOException, ServletException {
        actionFormMapper.populateParameter(execute, form);
    }

    // ===================================================================================
    //                                                                              Action
    //                                                                              ======
    public VirtualAction createAction(ActionExecute execute, ActionRequestResource resource) {
        return newGodHandableAction(execute, resource, getTransactionStage());
    }

    protected GodHandableAction newGodHandableAction(ActionExecute execute, ActionRequestResource resource, TransactionStage stage) {
        return new GodHandableAction(execute, resource, getRequestManager(), stage);
    }

    protected NextJourney performAction(VirtualAction action, OptionalThing<VirtualActionForm> form, ActionExecute execute)
            throws IOException, ServletException {
        try {
            return action.execute(form); // #to_action
        } catch (RuntimeException e) {
            return handleActionFailureException(action, form, execute, e);
        } finally {
            actionCoinHelper.clearContextJustInCase();
        }
    }

    protected NextJourney handleActionFailureException(VirtualAction action, OptionalThing<VirtualActionForm> optForm,
            ActionExecute execute, RuntimeException cause) throws IOException, ServletException {
        throw new ServletException(cause);
    }

    // ===================================================================================
    //                                                                             to Next
    //                                                                             =======
    protected void toNext(ActionExecute execute, NextJourney journey) throws IOException, ServletException {
        if (journey.isEmpty()) { // e.g. JSON handling
            return;
        }
        final String routingPath = journey.getRoutingPath();
        if (journey.isRedirectTo()) {
            doRedirect(execute, routingPath, journey.isAsIs());
        } else {
            doForward(execute, routingPath);
        }
    }

    protected void doRedirect(ActionExecute execute, String redirectPath, boolean asIs) throws IOException {
        final ResponseManager responseManager = getRequestManager().getResponseManager();
        if (asIs) {
            responseManager.redirectAsIs(redirectPath);
        } else { // mainly here
            responseManager.redirect(redirectPath);
        }
    }

    protected void doForward(ActionExecute execute, String forwardPath) throws IOException, ServletException {
        getRequestManager().getResponseManager().forward(forwardPath);
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
