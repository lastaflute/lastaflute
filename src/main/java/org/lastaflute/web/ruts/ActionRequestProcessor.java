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
import java.util.Collection;

import javax.servlet.ServletException;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.db.jta.stage.TransactionStage;
import org.lastaflute.di.helper.beans.PropertyDesc;
import org.lastaflute.web.callback.ActionRuntime;
import org.lastaflute.web.exception.RequestForwardFailureException;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.config.ActionFormProperty;
import org.lastaflute.web.ruts.config.ModuleConfig;
import org.lastaflute.web.ruts.process.ActionCoinHelper;
import org.lastaflute.web.ruts.process.ActionFormMapper;
import org.lastaflute.web.ruts.process.ActionResponseReflector;
import org.lastaflute.web.ruts.process.RequestUrlParam;
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
    public void process(ActionExecute execute, RequestUrlParam urlParam) throws IOException, ServletException {
        // initializing and clearing thread cache here so you can use thread cache in your action execute
        final boolean exists = ThreadCacheContext.exists();
        try {
            if (!exists) { // inherits existing cache when nested call e.g. forward
                ThreadCacheContext.initialize();
            }
            final ActionRuntime runtime = createActionRuntime(execute, urlParam);
            fire(runtime); // #to_action
        } finally {
            if (!exists) {
                ThreadCacheContext.clear();
            }
        }
    }

    protected ActionRuntime createActionRuntime(ActionExecute execute, RequestUrlParam urlParam) {
        return new ActionRuntime(execute, urlParam);
    }

    // ===================================================================================
    //                                                                               Fire
    //                                                                              ======
    /**
     * Fire the action, creating, populating, performing and to next.
     * @param runtime The runtime meta of action execute, which has action execute, URL parameter and states. (NotNull)
     * @throws IOException When the action fails about the IO.
     * @throws ServletException When the action fails about the Servlet.
     */
    protected void fire(ActionRuntime runtime) throws IOException, ServletException {
        final ActionResponseReflector reflector = createResponseReflector(runtime);
        ready(runtime, reflector);

        final OptionalThing<VirtualActionForm> form = prepareActionForm(runtime);
        populateParameter(runtime, form);

        final VirtualAction action = createAction(runtime, reflector);
        final NextJourney journey = performAction(action, form, runtime); // #to_action

        toNext(runtime, journey);
    }

    // ===================================================================================
    //                                                                               Ready
    //                                                                               =====
    protected ActionResponseReflector createResponseReflector(ActionRuntime runtime) {
        return new ActionResponseReflector(runtime, getRequestManager());
    }

    protected void ready(ActionRuntime runtime, ActionResponseReflector reflector) {
        actionCoinHelper.prepareRequestClientErrorHandlingIfApi(runtime, reflector);
        actionCoinHelper.prepareRequestServerErrorHandlingIfApi(runtime, reflector);
        actionCoinHelper.saveRuntimeToRequest(runtime);
        actionCoinHelper.removeCachedMessages();
        actionCoinHelper.resolveLocale(runtime);
    }

    // ===================================================================================
    //                                                                         Action Form
    //                                                                         ===========
    public OptionalThing<VirtualActionForm> prepareActionForm(ActionRuntime runtime) {
        final ActionExecute execute = runtime.getActionExecute();
        final OptionalThing<VirtualActionForm> optForm = execute.createActionForm();
        optForm.ifPresent(form -> saveFormToRequest(execute, form)); // to use form tag
        runtime.setActionForm(optForm); // to use in action hook
        return optForm;
    }

    protected void saveFormToRequest(ActionExecute execute, VirtualActionForm value) {
        getRequestManager().setAttribute(execute.getFormMeta().get().getFormKey(), value);
    }

    protected void populateParameter(ActionRuntime runtime, OptionalThing<VirtualActionForm> form) throws IOException, ServletException {
        actionFormMapper.populateParameter(runtime, form);
    }

    // ===================================================================================
    //                                                                              Action
    //                                                                              ======
    public VirtualAction createAction(ActionRuntime runtime, ActionResponseReflector reflector) {
        return newGodHandableAction(runtime, reflector, getTransactionStage(), getRequestManager());
    }

    protected GodHandableAction newGodHandableAction(ActionRuntime runtime, ActionResponseReflector reflector, TransactionStage stage,
            RequestManager requestManager) {
        return new GodHandableAction(runtime, reflector, stage, requestManager);
    }

    protected NextJourney performAction(VirtualAction action, OptionalThing<VirtualActionForm> form, ActionRuntime runtime)
            throws IOException, ServletException {
        try {
            return action.execute(form); // #to_action
        } catch (RuntimeException e) {
            return handleActionFailureException(action, form, runtime, e);
        } finally {
            actionCoinHelper.clearContextJustInCase();
        }
    }

    protected NextJourney handleActionFailureException(VirtualAction action, OptionalThing<VirtualActionForm> optForm,
            ActionRuntime runtime, RuntimeException cause) throws IOException, ServletException {
        throw new ServletException(cause);
    }

    // ===================================================================================
    //                                                                             to Next
    //                                                                             =======
    protected void toNext(ActionRuntime runtime, NextJourney journey) throws IOException, ServletException {
        if (journey.isEmpty()) { // e.g. JSON handling
            return;
        }
        final String routingPath = journey.getRoutingPath();
        if (journey.isRedirectTo()) {
            doRedirect(runtime, routingPath, journey.isAsIs());
        } else {
            exportFormPropertyToRequest(runtime); // for e.g. EL expression in JSP
            doForward(runtime, routingPath);
        }
    }

    // -----------------------------------------------------
    //                                              Redirect
    //                                              --------
    protected void doRedirect(ActionRuntime runtime, String redirectPath, boolean asIs) throws IOException {
        final ResponseManager responseManager = getRequestManager().getResponseManager();
        if (asIs) {
            responseManager.redirectAsIs(redirectPath);
        } else { // mainly here
            responseManager.redirect(redirectPath);
        }
    }

    protected void exportFormPropertyToRequest(ActionRuntime runtime) {
        runtime.getActionExecute().getFormMeta().ifPresent(meta -> {
            final Collection<ActionFormProperty> properties = meta.properties();
            if (properties.isEmpty()) {
                return;
            }
            final RequestManager requestManager = getRequestManager();
            final Object form = runtime.getActionForm().get().getRealForm();
            for (ActionFormProperty property : properties) {
                if (isExportableProperty(property.getPropertyDesc())) {
                    final Object propertyValue = property.getPropertyValue(form);
                    if (propertyValue != null) {
                        requestManager.setAttribute(property.getPropertyName(), propertyValue);
                    }
                }
            }
        });
    }

    protected boolean isExportableProperty(PropertyDesc pd) {
        return !pd.getPropertyType().getName().startsWith("javax.servlet");
    }

    // -----------------------------------------------------
    //                                               Forward
    //                                               -------
    protected void doForward(ActionRuntime runtime, String forwardPath) throws IOException, ServletException {
        try {
            getRequestManager().getResponseManager().forward(forwardPath);
        } catch (RuntimeException | IOException | ServletException e) { // because of e.g. compile error may be poor
            throwRequestForwardFailureException(runtime, forwardPath, e);
        }
    }

    protected void throwRequestForwardFailureException(ActionRuntime runtime, String forwardPath, Exception e) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Failed to forward the request to the path.");
        br.addItem("Advice");
        br.addElement("Read the nested exception message.");
        br.addItem("Action Runtime");
        br.addElement(runtime);
        br.addItem("Forward Path");
        br.addElement(forwardPath);
        final String msg = br.buildExceptionMessage();
        throw new RequestForwardFailureException(msg, e);
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
