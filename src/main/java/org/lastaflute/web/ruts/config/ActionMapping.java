/*
 * Copyright 2015-2018 the original author or authors.
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

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.di.core.ComponentDef;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;
import org.lastaflute.di.util.ArrayMap;
import org.lastaflute.web.path.ActionAdjustmentProvider;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.ruts.NextJourney;
import org.lastaflute.web.ruts.NextJourney.PlannedJourneyProvider;
import org.lastaflute.web.util.LaServletContextUtil;

/**
 * @author modified by jflute (originated in Seasar and Struts)
 */
public class ActionMapping {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // all not null
    protected final ComponentDef actionDef;
    protected final BeanDesc actionDesc;
    protected final String actionName; // actually actionDef's component name, used as mapping key in module config
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
        // plain name here, may contain restful http method e.g. get$index
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
            if (execute.determineTargetByPathParameter(paramPath)) {
                return execute;
            }
        }
        return null;
    }

    public ActionExecute findActionExecute(HttpServletRequest request) { // null allowed when not found
        for (ActionExecute execute : executeMap.values()) {
            if (execute.determineTargetByRequestParameter(request)) { // request parameter contains e.g. doUpdate=update
                return execute;
            }
        }
        return doFindFixedActionExecute(request); // e.g. index() for /sea/land/
    }

    protected ActionExecute doFindFixedActionExecute(HttpServletRequest request) {
        final ActionExecute indexFound = executeMap.get("index");
        if (indexFound != null) {
            return indexFound;
        }
        final String httpMethod = request.getMethod();
        if (httpMethod != null) { // just in case
            final String restIndex = httpMethod.toLowerCase() + "$index"; // e.g. get$index()
            final ActionExecute restFound = executeMap.get(restIndex); // retry as restful
            if (restFound != null) {
                return restFound;
            }
        }
        // remove it on LastaFlute, more strict mapping as possible
        //if (executeMap.size() == 1) { // e.g. no index() but only-one method exists
        //    return executeMap.get(0);
        //}
        return null; // not found
    }

    public ActionExecute getActionExecute(Method method) { // null allowed when not found
        return executeMap.get(method.getName()); // find plainly, key may contain restful HTTP method
    }

    // ===================================================================================
    //                                                                  Forward Adjustment
    //                                                                  ==================
    // o to suppress that URL that contains dot is handled as JSP
    // o routing path of forward e.g. /member/list/ -> MemberListAction
    public NextJourney createNextJourney(PlannedJourneyProvider journeyProvider, HtmlResponse response) { // almost copied from super
        String path = response.getRoutingPath();
        final boolean redirectTo = response.isRedirectTo();
        if (path.indexOf(":") < 0) {
            if (!path.startsWith("/")) {
                path = buildActionPath(getActionDef().getComponentName()) + path;
            }
            if (!redirectTo && isHtmlForward(path)) {
                path = filterHtmlPath(path);
            }
        }
        return newNextJourney(journeyProvider, path, redirectTo, response.isAsIs(), response.getViewObject());
    }

    protected NextJourney newNextJourney(PlannedJourneyProvider journeyProvider // fixed
            , String routingPath, boolean redirectTo, boolean asIs // routing resources
            , OptionalThing<Object> viewObject // for e.g. mixer2
    ) {
        return new NextJourney(journeyProvider, routingPath, redirectTo, asIs, viewObject);
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

    protected boolean isHtmlForward(String path) {
        return isHtmlPath(path) || isJspPath(path);
    }

    protected String filterHtmlPath(String path) {
        String origin = path;
        if (isJspPath(origin)) {
            final String viewPrefix = LaServletContextUtil.getJspViewPrefix();
            if (viewPrefix != null) {
                origin = viewPrefix + origin; // e.g. /WEB-INF/view/...
            }
        }
        final String filtered = adjustmentProvider.filterHtmlPath(origin, this);
        return filtered != null ? filtered : origin;
    }

    protected boolean isHtmlPath(String path) {
        return path.endsWith(".html");
    }

    protected boolean isJspPath(String path) {
        return path.endsWith(".jsp");
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
