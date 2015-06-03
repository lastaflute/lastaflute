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
package org.lastaflute.web.ruts.config;

import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.dbflute.util.DfTypeUtil;
import org.lastaflute.di.core.ComponentDef;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;
import org.lastaflute.di.util.ArrayMap;
import org.lastaflute.web.path.ActionAdjustmentProvider;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.ruts.NextJourney;
import org.lastaflute.web.util.LaServletContextUtil;

/**
 * @author modified by jflute (originated in Struts and Seasar)
 */
public class ActionMapping {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // all not null
    protected final ComponentDef actionDef;
    protected final BeanDesc actionDesc;
    protected final String actionName;
    protected final ActionAdjustmentProvider adjustmentProvider;
    protected final ArrayMap<String, ActionExecute> executeMap = new ArrayMap<String, ActionExecute>(); // array to get first

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionMapping(ComponentDef actionDef, String actionName, ActionAdjustmentProvider adjustmentProvider) {
        this.actionDef = actionDef;
        this.actionDesc = BeanDescFactory.getBeanDesc(actionDef.getComponentClass());
        this.actionName = actionName;
        this.adjustmentProvider = adjustmentProvider;
    }

    // -----------------------------------------------------
    //                                      Register Execute
    //                                      ----------------
    public void registerExecute(ActionExecute execute) {
        executeMap.put(execute.getExecuteMethod().getName(), execute);
    }

    // ===================================================================================
    //                                                                       Create Action
    //                                                                       =============
    public Object createAction() {
        return actionDef.getComponent();
    }

    // ===================================================================================
    //                                                                        Find Execute
    //                                                                        ============
    // optional unused for performance
    public ActionExecute findActionExecute(String paramPath) { // null allowed when not found
        for (ActionExecute execute : executeMap.values()) {
            if (execute.isTargetExecute(paramPath)) {
                return execute;
            }
        }
        return null;
    }

    public ActionExecute findActionExecute(HttpServletRequest request) { // null allowed when not found
        for (ActionExecute execute : executeMap.values()) {
            if (execute.isTargetExecute(request)) { // request parameter contains e.g. doUpdate=update
                return execute;
            }
        }
        return doFindFixedActionExecute(); // e.g. index() for /sea/land/
    }

    protected ActionExecute doFindFixedActionExecute() {
        final ActionExecute indexFound = executeMap.get("index");
        if (indexFound != null) {
            return indexFound;
        }
        // remove it on LastaFlute, more strict mapping as possible
        //if (executeMap.size() == 1) { // e.g. no index() but only-one method exists
        //    return executeMap.get(0);
        //}
        return null; // not found
    }

    public ActionExecute getActionExecute(Method method) { // null allowed when not found
        return executeMap.get(method.getName());
    }

    // ===================================================================================
    //                                                                  Forward Adjustment
    //                                                                  ==================
    // o to suppress that URL that contains dot is handled as JSP
    // o routing path of forward e.g. /member/list/ -> MemberListAction
    public NextJourney createNextJourney(HtmlResponse response) { // almost copied from super
        String path = response.getRoutingPath();
        final boolean redirectTo = response.isRedirectTo();
        if (path.indexOf(":") < 0) {
            if (!path.startsWith("/")) {
                path = buildActionPath(getActionDef().getComponentName()) + path;
            }
            if (!redirectTo && isJspForward(path)) {
                path = filterJspPath(path);
            }
        }
        return newNextJourney(path, redirectTo, response.isAsIs());
    }

    protected NextJourney newNextJourney(String routingPath, boolean redirectTo, boolean asIs) {
        return new NextJourney(routingPath, redirectTo, asIs);
    }

    protected String buildActionPath(String componentName) {
        if (!componentName.endsWith("Action")) {
            throw new IllegalArgumentException(componentName);
        }
        if (componentName.equals("indexAction")) {
            return "/";
        }
        final String filtered;
        if (componentName.endsWith("indexAction")) {
            filtered = componentName.substring(0, componentName.length() - 12);
        } else {
            filtered = componentName.substring(0, componentName.length() - 6);
        }
        return "/" + filtered.replace('_', '/') + "/";
    }

    protected boolean isJspForward(String path) {
        return path.endsWith(".jsp"); // you can only forward to JSP
    }

    protected String filterJspPath(String path) {
        final String viewPrefix = LaServletContextUtil.getViewPrefix();
        if (viewPrefix != null) {
            path = viewPrefix + path; // e.g. /WEB-INF/view/...
        }
        final String filtered = adjustmentProvider.filterHtmlPath(path, this);
        return filtered != null ? filtered : path;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final String title = DfTypeUtil.toClassTitle(this);
        final String hash = Integer.toHexString(hashCode());
        return title + ":{" + actionName + ", executeCount=" + executeMap.size() + "}@" + hash;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public ComponentDef getActionDef() {
        return actionDef;
    }

    public BeanDesc getActionDesc() {
        return actionDesc;
    }

    public String getActionName() {
        return actionName;
    }

    public Map<String, ActionExecute> getExecuteMap() {
        return executeMap;
    }
}
