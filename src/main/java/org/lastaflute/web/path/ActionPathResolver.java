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
package org.lastaflute.web.path;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfStringUtil;
import org.dbflute.util.Srl;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.di.core.LaContainer;
import org.lastaflute.di.naming.NamingConvention;
import org.lastaflute.di.util.LdiStringUtil;
import org.lastaflute.web.UrlChain;
import org.lastaflute.web.direction.FwWebDirection;
import org.lastaflute.web.exception.ActionClassPackageMismatchException;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.util.LaActionExecuteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The resolver of action.
 * @author jflute
 */
public class ActionPathResolver {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(ActionPathResolver.class);

    private static final UrlChain EMPTY_URL_CHAIN = new UrlChain(null);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant director (AD) for framework. (NotNull: after initialization) */
    @Resource
    private FwAssistantDirector assistantDirector;

    /** The container instance of Seasar for this class (not root but you can get root). (NotNull) */
    @Resource
    private LaContainer container;

    /** The naming convention instance of Seasar. (NotNull) */
    @Resource
    private NamingConvention namingConvention;

    /** The provider of action adjustment. (NotNull: after initialization) */
    protected ActionAdjustmentProvider actionAdjustmentProvider;

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    /**
     * Initialize this component. <br>
     * This is basically called by DI setting file.
     */
    @PostConstruct
    public synchronized void initialize() {
        final FwWebDirection direction = assistOptionalActionDirection();
        actionAdjustmentProvider = direction.assistActionAdjustmentProvider();
        showBootLogging();
    }

    protected FwWebDirection assistOptionalActionDirection() {
        return assistantDirector.assistWebDirection();
    }

    protected void showBootLogging() {
        if (logger.isInfoEnabled()) {
            logger.info("[Action Resolver]");
            logger.info(" actionAdjustmentProvider: " + actionAdjustmentProvider);
        }
    }

    // ===================================================================================
    //                                                                 ActionPath Handling
    //                                                                 ===================
    /**
     * Handle the action path from the specified request path.
     * @param requestPath The request path to be analyzed. (NotNull)
     * @param handler The handler of the action path when the action is found. (NotNull)
     * @return Is it actually handled? (false if not found)
     * @throws Exception When the handler throws or internal process throws.
     */
    public boolean handleActionPath(String requestPath, ActionFoundPathHandler handler) throws Exception {
        assertArgumentNotNull("requestPath", requestPath);
        assertArgumentNotNull("handler", handler);
        final String customized = actionAdjustmentProvider.customizeActionMappingRequestPath(requestPath);
        return doHandleActionPath(customized != null ? customized : requestPath, handler);
    }

    protected boolean doHandleActionPath(String requestPath, ActionFoundPathHandler handler) throws Exception {
        final String[] names = LdiStringUtil.split(requestPath, "/"); // e.g. [sea, land] if /sea/land/
        final LaContainer root = container.getRoot(); // because actions are in root
        final String rootAction = "rootAction";
        if (names.length == 0) { // root action, / => rootAction
            if (hasActionDef(root, rootAction)) {
                if (actuallyHandleActionPath(requestPath, handler, rootAction, null)) {
                    return true;
                }
            }
        }
        StringBuilder prefixSb = null; // lazy loaded, "" => sea => seaLand
        List<String> previousList = null; // lazy loaded, (empty) => [sea] => [sea, land]
        for (int index = 0; index < names.length; index++) {
            final String currentName = names[index];
            if (containsNotAllowedCharacterAsActionPath(currentName)) { // e.g. /Sea/land/, /sea/Land/
                return false; // cannot use upper case in action path (while, allowed in param path)
            }
            final int nextIndex = index + 1;
            if (index == 0) { // first loop
                // /sea/ => seaAction
                final String directAction = buildActionName(null, currentName);
                if (hasActionDef(root, directAction)) {
                    if (actuallyHandleActionPath(requestPath, handler, directAction, buildParamPath(names, nextIndex))) {
                        return true;
                    }
                }
                // /sea/ => sea_seaAction
                final String wholePkgAction = buildActionName(currentName + "_", currentName);
                if (hasActionDef(root, wholePkgAction)) {
                    if (actuallyHandleActionPath(requestPath, handler, wholePkgAction, buildParamPath(names, nextIndex))) {
                        return true;
                    }
                }
            } else { // second or more loop
                // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
                // e.g. process of outer and nested loop
                // 
                // second : /sea/land/
                //   first  : seaLandAction
                //   more   : sea_seaLandAction
                //   whole  : sea_land_seaLandAction
                // 
                // third : /sea/land/iks/
                //   first  : seaLandIksAction
                //   second : sea_seaLandIksAction
                //   more   : sea_land_seaLandIksAction
                //   whole  : sea_land_iks_seaLandIksAction
                // 
                // fourth : /sea/land/iks/amba
                //   first  : seaLandIksAmbaAction
                //   second : sea_seaLandIksAmbaAction
                //   third  : sea_land_seaLandIksAmbaAction
                //   more   : sea_land_iks_seaLandIksAmbaAction
                //   whole  : sea_land_iks_amba_seaLandIksAmbaAction
                // _/_/_/_/_/_/_/_/_/_/
                final String classPrefix = prefixSb.toString() + initCap(currentName); // seaLand, seaLandIks
                StringBuilder pkgSb = null; // lazy loaded
                for (String previous : previousList) { // always one or more loop
                    final String actionName = buildActionName(pkgSb != null ? pkgSb.toString() : null, classPrefix);
                    if (hasActionDef(root, actionName)) {
                        if (actuallyHandleActionPath(requestPath, handler, actionName, buildParamPath(names, nextIndex))) {
                            return true;
                        }
                    }
                    pkgSb = pkgSb != null ? pkgSb : new StringBuilder(requestPath.length());
                    pkgSb.append(previous).append("_");
                }
                final String morePkgActionName = buildActionName(pkgSb.toString(), classPrefix);
                if (hasActionDef(root, morePkgActionName)) {
                    if (actuallyHandleActionPath(requestPath, handler, morePkgActionName, buildParamPath(names, nextIndex))) {
                        return true;
                    }
                }
                pkgSb.append(currentName).append("_"); // sea_land_, sea_land_iks_, ...
                final String wholePkgActionName = buildActionName(pkgSb.toString(), classPrefix);
                if (hasActionDef(root, wholePkgActionName)) {
                    if (actuallyHandleActionPath(requestPath, handler, wholePkgActionName, buildParamPath(names, nextIndex))) {
                        return true;
                    }
                }
            }
            prefixSb = prefixSb != null ? prefixSb : new StringBuilder(requestPath.length());
            prefixSb.append(index == 0 ? currentName : initCap(currentName));
            previousList = previousList != null ? previousList : new ArrayList<String>(4);
            previousList.add(currentName);
        }
        if (names.length > 0) { // e.g. /sea/land but not found except root action
            // only root action's named methods are low priority (to avoid searching cost)
            if (hasActionDef(root, rootAction)) {
                if (actuallyHandleActionPath(requestPath, handler, rootAction, buildParamPath(names, 0))) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean hasActionDef(LaContainer root, String componentName) {
        try {
            return root.hasComponentDef(componentName);
        } catch (NoClassDefFoundError e) { // basically only when HotDeploy
            // e.g. /sealand/ => sealand.SeaLandAction
            // /Sea/land/ has been already handled before (should be 404)
            throwActionClassPackageMismatchException(componentName, e);
            return false;
        }
    }

    protected void throwActionClassPackageMismatchException(String componentName, NoClassDefFoundError e) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Mismatch between action class and package.");
        br.addItem("Advice");
        br.addElement("Make sure your action definition like this:");
        br.addElement("  (x):");
        br.addElement("    sealand.SeaLandAction");
        br.addElement("  (o):");
        br.addElement("    sealand.SealandAction  => /sealand/");
        br.addElement("    sea.SeaLandAction      => /sea/land/");
        br.addElement("    sea.land.SeaLandAction => /sea/land/");
        br.addElement("    SeaLandAction          => /sea/land/");
        br.addItem("Illegal Action");
        br.addElement(componentName);
        final String msg = br.buildExceptionMessage();
        throw new ActionClassPackageMismatchException(msg, e);
    }

    protected boolean containsNotAllowedCharacterAsActionPath(String currentName) {
        return Srl.isUpperCaseAny(currentName);
    }

    protected String buildActionName(String pkg, String classPrefix) {
        return (pkg != null ? pkg : "") + classPrefix + namingConvention.getActionSuffix();
    }

    protected String buildParamPath(String[] names, int index) {
        final int length = names.length;
        if (index >= length) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = index; i < length; i++) {
            if (i != index) {
                sb.append('/');
            }
            sb.append(names[i]);
        }
        return sb.toString(); // e.g. 3 when /member/list/3/
    }

    protected boolean actuallyHandleActionPath(String requestPath, ActionFoundPathHandler handler, String actionName, String paramPath)
            throws Exception {
        final boolean emptyParam = paramPath == null || paramPath.isEmpty();
        final ActionExecute execByParam = !emptyParam ? findExecuteConfig(actionName, paramPath).orElse(null) : null;
        if (emptyParam || execByParam != null) { // certainly hit
            return handler.handleActionPath(requestPath, actionName, paramPath, execByParam);
        }
        return false;
    }

    protected OptionalThing<ActionExecute> findExecuteConfig(String actionName, String paramPath) {
        return LaActionExecuteUtil.findActionExecute(actionName, paramPath);
    }

    // ===================================================================================
    //                                                                  ActionPath Convert
    //                                                                  ==================
    /**
     * Convert to URL string to move the action. <br>
     * e.g. ProductListAction to /product/list/ <br>
     * And not contain context path.
     * @param actionType The class type of action that it redirects to. (NotNull)
     * @return The URL string to move to the action. (NotNull)
     */
    public String toActionUrl(Class<?> actionType) {
        assertArgumentNotNull("actionType", actionType);
        return toActionUrl(actionType, EMPTY_URL_CHAIN);
    }

    /**
     * Convert to URL string to move the action. <br>
     * e.g. ProductListAction with moreUrl(3) to /product/list/3 <br>
     * And not contain context path.
     * @param actionType The class type of action that it redirects to. (NotNull)
     * @param chain The chain of URL to build additional info on URL. (NotNull)
     * @return The URL string to move to the action. (NotNull)
     */
    public String toActionUrl(Class<?> actionType, UrlChain chain) {
        assertArgumentNotNull("actionType", actionType);
        assertArgumentNotNull("chain", chain);
        final String actionPath = resolveActionPath(actionType);
        final List<Object> getParamList = extractGetParamList(chain);
        final StringBuilder sb = new StringBuilder();
        sb.append(actionPath);
        buildUrlParts(sb, chain);
        buildGetParam(sb, actionPath, getParamList);
        buildHashOnUrl(sb, chain);
        return sb.toString();
    }

    protected List<Object> extractGetParamList(UrlChain chain) {
        final Object[] paramsOnGet = chain != null ? chain.getParamsOnGet() : null;
        final List<Object> getParamList;
        if (paramsOnGet != null) {
            getParamList = DfCollectionUtil.newArrayList(paramsOnGet);
        } else {
            getParamList = DfCollectionUtil.emptyList();
        }
        return getParamList;
    }

    protected void buildUrlParts(StringBuilder sb, UrlChain chain) {
        final Object[] urlParts = chain != null ? chain.getUrlParts() : null;
        boolean existsParts = false;
        if (urlParts != null) {
            for (Object param : urlParts) {
                if (param != null) {
                    sb.append(param).append("/");
                    existsParts = true;
                }
            }
        }
        if (existsParts) {
            sb.delete(sb.length() - 1, sb.length()); // e.g. member/edit/3/ to member/edit/3
        }
    }

    protected void buildGetParam(StringBuilder sb, String actionPath, List<Object> getParamList) {
        int index = 0;
        for (Object param : getParamList) {
            if (index == 0) { // first loop
                sb.append("?");
            } else {
                if (index % 2 == 0) {
                    sb.append("&");
                } else if (index % 2 == 1) {
                    sb.append("=");
                } else { // no way
                    String msg = "no way: url=" + actionPath + " get-params=" + getParamList;
                    throw new IllegalStateException(msg);
                }
            }
            sb.append(param != null ? param : "");
            ++index;
        }
    }

    protected void buildHashOnUrl(StringBuilder sb, UrlChain chain) {
        final Object hash = chain != null ? chain.getHashOnUrl() : null;
        if (hash != null) {
            sb.append("#").append(hash);
        }
    }

    // -----------------------------------------------------
    //                                    Resolve ActionPath
    //                                    ------------------
    public String resolveActionPath(Class<?> actionType) {
        final String delimiter = "/";
        return delimiter + decamelize(toSimpleActionName(actionType), delimiter) + delimiter;
    }

    protected String toSimpleActionName(Class<?> actionType) {
        final String componentName = namingConvention.fromClassNameToComponentName(actionType.getName());
        return removeRearActionSuffixIfNeeds(Srl.substringLastRear(componentName, "_"));
    }

    protected String removeRearActionSuffixIfNeeds(String path) {
        final String actionSuffix = namingConvention.getActionSuffix();
        return path.endsWith(actionSuffix) ? path.substring(0, path.length() - actionSuffix.length()) : path;
    }

    protected String decamelize(String simpleName, String delimiter) {
        return Srl.decamelize(simpleName, delimiter).toLowerCase(); // seaLand => SEA/LAND => sea/land
    }

    // ===================================================================================
    //                                                              ActionPath Calculation
    //                                                              ======================
    public String calculateActionPathByJspPath(String requestPath) {
        final String lastPathElement = substringLastRear(requestPath, "/");
        if (!lastPathElement.contains(".")) { // no JSP
            return requestPath;
        }
        // basically JSP here
        final String frontPathElement = substringLastFront(requestPath, "/");
        final String fileNameNoExt = substringLastFront(lastPathElement, ".");
        final String pathBase = frontPathElement + "/";
        if (!fileNameNoExt.contains("_")) { // e.g. list.jsp
            return pathBase; // e.g. /member/ (submit name is needed in this case)
        }
        // the file name has package prefix here
        // e.g. /member/member_list.jsp or /member/list/member_purchase_list.jsp
        final List<String> wordList = splitList(fileNameNoExt, "_"); // e.g. [member, list] or [member, purchase, list]
        if (wordList.size() < 2) { // no way (just in case)
            return pathBase;
        }
        final String firstHit = resolveJspActionPath(requestPath, frontPathElement, pathBase, wordList);
        if (firstHit != null) {
            return firstHit; // e.g. /member/list/ (from /member/member_list.jsp)
        }
        final List<String> retryList = prepareHtmlRetryWordList(requestPath, wordList);
        if (retryList != null && !retryList.isEmpty()) { // e.g. [member, list] (from sp_member_list.jsp)
            final String retryHit = resolveJspActionPath(requestPath, frontPathElement, pathBase, retryList);
            if (retryHit != null) {
                return retryHit; // e.g. /member/list/ (from /member/sp_member_list.jsp)
            }
        }
        // e.g. /member/purchase_list.jsp
        return pathBase; // e.g. /member/ (submit name is needed in this case)
    }

    protected String resolveJspActionPath(String requestPath, String frontPathElement, String pathBase, List<String> wordList) {
        String previousSuffix = "";
        for (int i = 0; i < wordList.size(); i++) {
            // e.g. 1st: '' + '/' + member, 2nd: /member + '/' + purchase
            final String pathSuffix = previousSuffix + "/" + wordList.get(i);
            final boolean nextLoopLast = wordList.size() == i + 2;
            if (nextLoopLast && frontPathElement.endsWith(pathSuffix)) {
                // e.g. 1st: /member/list/, 2nd: /member/purchase/list/
                final String lastElement = wordList.get(i + 1);
                final String resolvedPath;
                if (lastElement.equals("index")) { // e.g. /member/list/member_list_index.jsp
                    resolvedPath = pathBase;
                } else {
                    resolvedPath = pathBase + lastElement + "/";
                }
                return resolvedPath;
            }
            previousSuffix = pathSuffix;
        }
        return null;
    }

    protected List<String> prepareHtmlRetryWordList(String requestPath, List<String> wordList) {
        return actionAdjustmentProvider.prepareHtmlRetryWordList(requestPath, wordList);
    }

    // ===================================================================================
    //                                                                    Expected Routing
    //                                                                    ================
    public String prepareExpectedRoutingMessage(String requestPath) { // for debug
        final StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("/= = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = *No routing action:\n");
        sb.append("e.g. expected actions for ").append(requestPath).append("\n");
        final String customizedPath = actionAdjustmentProvider.customizeActionMappingRequestPath(requestPath);
        final List<String> nameList = buildExpectedRoutingActionList(customizedPath != null ? customizedPath : requestPath);
        boolean exists = false;
        for (String name : nameList) {
            if (name.endsWith("@index()") && containsNotAllowedCharacterAsActionPath(requestPath)) { // e.g. /product/List/
                continue;
            }
            final String packageExp = Srl.substringLastFront(name, ".");
            if (!containsNotAllowedCharacterAsActionPath(packageExp)) {
                sb.append("  web.").append(name).append("\n");
                exists = true;
            }
        }
        if (exists) {
            sb.append("  (and so on...)\n");
        } else {
            sb.append("  *no suggestion... e.g. cannot use upper case in action path\n");
        }
        sb.append("= = = = = = = = = =/");
        return sb.toString();
    }

    protected List<String> buildExpectedRoutingActionList(String requestPath) {
        final List<String> tokenList;
        {
            final String trimmedPath = trim(requestPath, "/"); // /member/list/ -> member/list
            final List<String> splitList = splitList(trimmedPath, "/"); // [member, list]
            tokenList = new ArrayList<String>(splitList.size()); // removed empty elements
            for (String element : splitList) {
                if (element.trim().length() == 0) {
                    continue; // e.g. /member//list/
                }
                tokenList.add(element);
            }
        }
        // e.g. / or /123/ or /123/foo/
        if (tokenList.isEmpty() || mayBeParameterToken(tokenList.get(0))) {
            final List<String> nameList = new ArrayList<String>(1);
            nameList.add("RootAction#index()");
            return nameList;
        }
        final StringBuilder namedActionSb = new StringBuilder();
        final StringBuilder methodActionSb = new StringBuilder();
        final StringBuilder wholePkgActionSb = new StringBuilder();
        final StringBuilder pkgPrefix = new StringBuilder();
        boolean existsMethodAction = false;
        boolean existsWholePkgAction = false;
        for (int index = 0; index < tokenList.size(); index++) {
            final String current = tokenList.get(index);
            if (index == 0) {
                namedActionSb.append(current).append(".");
                methodActionSb.append(current).append(".");
            }
            final boolean beforeLastLoop = index < tokenList.size() - 1;
            final String next = beforeLastLoop ? tokenList.get(index + 1) : null;
            final boolean nextParam = next != null ? mayBeParameterToken(next) : false;
            final String capElement = initCap(current);
            if (beforeLastLoop && !nextParam) { // before last action token
                wholePkgActionSb.append(current).append(".");
                pkgPrefix.append(capElement);
            } else { // last action token here (last loop or next token is parameter)
                // web.SeaAction#index() or web.sea.SeaLandAction#index()
                namedActionSb.append(pkgPrefix).append(capElement).append("Action@index()");

                if (index > 0) {
                    // web.sea.SeaAction#land()
                    methodActionSb.append(pkgPrefix).append("Action@");
                    methodActionSb.append(current).append("()");
                    existsMethodAction = true;

                    // web.land.sea.LandSeaAction#index()
                    pkgPrefix.append(capElement);
                    wholePkgActionSb.append(current).append(".").append(pkgPrefix).append("Action@index()");
                    existsWholePkgAction = true;
                }
                break;
            }
        }
        final List<String> nameList = new ArrayList<String>(3);
        nameList.add(namedActionSb.toString());
        if (existsMethodAction) {
            nameList.add(methodActionSb.toString());
        }
        if (existsWholePkgAction) {
            nameList.add(wholePkgActionSb.toString());
        }
        return nameList;
    }

    protected boolean mayBeParameterToken(String token) {
        if (isFirstCharNumber(token)) { // e.g. 123 or 4ab
            return true;
        }
        if (DfStringUtil.containsAny(token, ".", "%", "?", "&")) { // e.g. a.b or %2d or ?foo=bar or ...
            return true;
        }
        return false;
    }

    protected boolean isFirstCharNumber(String token) {
        return "0123456789+-".contains(token.substring(0, 1));
    }

    // ===================================================================================
    //                                                                      General Helper
    //                                                                      ==============
    protected String replace(String str, String fromStr, String toStr) {
        return DfStringUtil.replace(str, fromStr, toStr);
    }

    protected String substringFirstFront(String str, String... delimiters) {
        return DfStringUtil.substringFirstFront(str, delimiters);
    }

    protected String substringFirstRear(String str, String... delimiters) {
        return DfStringUtil.substringFirstRear(str, delimiters);
    }

    protected String substringLastFront(String str, String... delimiters) {
        return DfStringUtil.substringLastFront(str, delimiters);
    }

    protected String substringLastRear(String str, String... delimiters) {
        return DfStringUtil.substringLastRear(str, delimiters);
    }

    protected List<String> splitList(String str, String delimiter) {
        return DfStringUtil.splitList(str, delimiter);
    }

    protected String initCap(String str) {
        return DfStringUtil.initCap(str);
    }

    protected String initUncap(String str) {
        return DfStringUtil.initUncap(str);
    }

    protected String trim(String str, String trimStr) {
        return DfStringUtil.trim(str, trimStr);
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            throw new IllegalArgumentException("The variableName should not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
        }
    }
}
