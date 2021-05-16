/*
 * Copyright 2015-2020 the original author or authors.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.Srl;
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
    protected final ComponentDef actionDef; // not null
    protected final BeanDesc actionDesc; // not null
    protected final String actionName; // not null, actually actionDef's component name, used as mapping key in module config
    protected final ActionAdjustmentProvider adjustmentProvider; // not null

    // -----------------------------------------------------
    //                                        Action Execute
    //                                        --------------
    // #thinking jflute already not needs to be map? may migrate to simple list after deleting getExecuteMap() (2021/05/16)
    protected final ArrayMap<String, ActionExecute> executeMap = new ArrayMap<String, ActionExecute>(); // not null, array to get first
    protected ActionExecute defaultablePlainExecute; // null allowed
    protected Map<String, ActionExecute> defaultableRestfulExecuteMap; // null allowed, map:{httpMethod : execute}

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionMapping(ComponentDef actionDef, String actionName, ActionAdjustmentProvider adjustmentProvider) {
        this.actionDef = actionDef;
        this.actionDesc = BeanDescFactory.getBeanDesc(actionDef.getComponentClass());
        this.actionName = actionName;
        this.adjustmentProvider = adjustmentProvider;
    }

    // ===================================================================================
    //                                                                    Register Execute
    //                                                                    ================
    public void registerExecute(ActionExecute execute) {
        // plain name here, may contain restful http method e.g. get$index
        final String executeKey = generateExecuteKey(execute);
        executeMap.put(executeKey, execute);
        if (isDefaultableExecuteMethod(execute)) {
            setupDefaultableIndexExecute(execute);
        }
    }

    // -----------------------------------------------------
    //                                           Execute Key
    //                                           -----------
    protected String generateExecuteKey(ActionExecute execute) {
        // can accept overload methods here for RESTful GET pair
        // however overload is restricted by customizer with the pair control
        // e.g.
        //  index(Integer productId) => index(java.lang.Integer)
        //  index(Integer productId, Long purchaseid) => index(java.lang.Integer,java.lang.Long)
        //  index(OptionalThing<Integer> productId) => index(org.dbflute.optional.OptionalThing)
        //  sea(SeaForm form) => sea()
        final Method executeMethod = execute.getExecuteMethod();
        final String paramExp = execute.getPathParamArgs().map(args -> {
            return args.getPathParamTypeList().stream().map(tp -> tp.getName()).collect(Collectors.joining(","));
        }).orElse("");
        return executeMethod.getName() + "(" + paramExp + ")";
    }

    // -----------------------------------------------------
    //                                   Defaultable Execute
    //                                   -------------------
    protected boolean isDefaultableExecuteMethod(ActionExecute execute) {
        if (!execute.isIndexMethod()) {
            return false;
        }
        // index() or e.g. get$index() here
        final OptionalThing<PathParamArgs> args = execute.getPathParamArgs();
        return !args.isPresent() || args.get().isOptionalParameter(0); // can accept when no parameter
    }

    protected void setupDefaultableIndexExecute(ActionExecute execute) {
        // only either index() and index(OptionalThing) exists as LastaFlute rule
        // so actually no overriding happens here
        if (execute.getRestfulHttpMethod().isPresent()) { // e.g. get$index()
            final String httpMethod = execute.getRestfulHttpMethod().get();
            if (defaultableRestfulExecuteMap == null) {
                defaultableRestfulExecuteMap = new HashMap<String, ActionExecute>();
            }
            final ActionExecute existingExecute = defaultableRestfulExecuteMap.get(httpMethod);
            if (existingExecute != null) {
                if (existingExecute.getPathParamArgs().isPresent()) { // has optional first parameter
                    defaultableRestfulExecuteMap.put(httpMethod, execute); // use current execute that has no parameter
                }
                // if existing execute has no parameter, keep it
            } else {
                defaultableRestfulExecuteMap.put(httpMethod, execute);
            }
        } else { // e.g. index()
            if (defaultablePlainExecute != null) {
                if (defaultablePlainExecute.getPathParamArgs().isPresent()) { // has optional first parameter
                    defaultablePlainExecute = execute; // use current execute that has no parameter
                }
                // if existing execute has no parameter, keep it
            } else {
                defaultablePlainExecute = execute;
            }
        }
    }

    // ===================================================================================
    //                                                                      Search Execute
    //                                                                      ==============
    public List<ActionExecute> searchExecuteByMethodName(String methodName) { // empty allowed when not found
        return executeMap.values().stream().filter(ex -> {
            return ex.getExecuteMethod().getName().equals(methodName);
        }).collect(Collectors.toList());
    }

    @Deprecated // for Lasta Meta (lasta-doc engine), use searchExecuteByMethodName() insteadO
    public ActionExecute getActionExecute(Method method) {
        // basically not multiple as LastaFlute rule except RESTful GET pairs
        final List<ActionExecute> executeList = searchExecuteByMethodName(method.getName());
        return !executeList.isEmpty() ? executeList.get(0) : null;
    }

    // ===================================================================================
    //                                                                       Create Action
    //                                                                       =============
    public Object createAction() {
        return actionDef.getComponent();
    }

    // ===================================================================================
    //                                                              Find Execute (Mapping)
    //                                                              ======================
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

    protected ActionExecute doFindFixedActionExecute(HttpServletRequest request) { // for no path parameter
        if (defaultablePlainExecute != null) {
            return defaultablePlainExecute;
        }
        if (defaultableRestfulExecuteMap != null) {
            final String httpMethod = request.getMethod(); // basically not null
            if (httpMethod != null) { // just in case
                final ActionExecute restFound = defaultableRestfulExecuteMap.get(httpMethod.toLowerCase());
                if (restFound != null) { // e.g. get$index()
                    return restFound;
                }
            }
        }
        // traditional code, remove it on LastaFlute, more strict mapping as possible
        //if (executeMap.size() == 1) { // e.g. no index() but only-one method exists
        //    return executeMap.get(0);
        //}
        return null; // not found
    }

    // ===================================================================================
    //                                                              Next Journey (Forward)
    //                                                              ======================
    // #for_now jflute traditional code, may not need to be here but keep it (2021/05/16)
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

    @Deprecated // map should be closed, but for compatible (e.g. UTFlute mock logic)
    public Map<String, ActionExecute> getExecuteMap() {
        final Map<String, ActionExecute> adjustedMap = new LinkedHashMap<String, ActionExecute>();
        final Set<Entry<String, ActionExecute>> entrySet = executeMap.entrySet();
        for (Entry<String, ActionExecute> entry : entrySet) {
            final String key = entry.getKey();
            ActionExecute execute = entry.getValue();
            adjustedMap.put(key, execute);

            // compatible logic (limitted)
            if (!execute.getPathParamArgs().isPresent()) { // only case of no parameter
                final String compatibleKey = Srl.substringFirstFront(key, "("); // e.g. index
                adjustedMap.put(compatibleKey, execute); // you can get by "index", "sea"
            }
        }
        return executeMap;
    }

    public List<ActionExecute> getExecuteList() { // read-only
        return Collections.unmodifiableList(new ArrayList<ActionExecute>(executeMap.values()));
    }

    public OptionalThing<ActionExecute> getDefaultablePlainExecute() {
        return OptionalThing.ofNullable(defaultablePlainExecute, () -> {
            throw new IllegalStateException("Not found the defaultablePlainExecute.");
        });
    }

    public Map<String, ActionExecute> getDefaultableRestfulExecuteMap() { // read-only
        if (defaultableRestfulExecuteMap != null) {
            return Collections.unmodifiableMap(defaultableRestfulExecuteMap);
        } else {
            return Collections.emptyMap();
        }
    }
}
