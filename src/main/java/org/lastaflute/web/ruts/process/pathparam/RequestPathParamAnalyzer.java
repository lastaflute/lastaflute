/*
 * Copyright 2015-2021 the original author or authors.
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
package org.lastaflute.web.ruts.process.pathparam;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.DfTypeUtil.ParseBooleanException;
import org.dbflute.util.DfTypeUtil.ParseDateException;
import org.dbflute.util.Srl;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.core.util.LaClassificationUtil;
import org.lastaflute.core.util.LaClassificationUtil.ClassificationUnknownCodeException;
import org.lastaflute.web.exception.Forced404NotFoundException;
import org.lastaflute.web.exception.PathParamArgsDifferentCountException;
import org.lastaflute.web.exception.PathParamOptionalParameterEmptyAccessException;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.util.LaActionExecuteUtil;

/**
 * @author jflute
 */
public class RequestPathParamAnalyzer {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RequestManager requestManager;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public RequestPathParamAnalyzer(RequestManager requestManager) {
        this.requestManager = requestManager;
    }

    // ===================================================================================
    //                                                                             Analyze
    //                                                                             =======
    // [attention]
    // pathParam means path parameters or object that has the parameters
    // paramPath means string of path for parameter from part of URL
    /**
     * @param execute The definition of action execute. (NotNull)
     * @param paramPath The path for parameter from part of URL. (NullAllowed)
     * @return The object for path parameter value that has e.g. map:{index = value} (NotNull)
     */
    public RequestPathParam analyzePathParam(ActionExecute execute, String paramPath) {
        return doAnalyzePathParam(execute, extractRealParamPath(execute, paramPath));
    }

    protected String extractRealParamPath(ActionExecute execute, String paramPath) {
        if (paramPath == null) {
            return null;
        }
        final String real;
        if (execute.isIndexMethod()) {
            real = paramPath;
        } else { // sea()
            final String methodName = execute.getMappingMethodName();
            if (paramPath.equals(methodName) || paramPath.startsWith(methodName + "/")) { // e.g. sea or sea/3/
                real = Srl.ltrim(Srl.substringFirstRear(paramPath, methodName), "/");
            } else {
                real = paramPath;
            }
        }
        return real;
    }

    protected RequestPathParam doAnalyzePathParam(ActionExecute execute, String paramPath) {
        final List<Class<?>> pathParamTypeList = execute.getPathParamArgs().map(args -> {
            return args.getPathParamTypeList();
        }).orElse(Collections.emptyList());
        final Map<Integer, Class<?>> optGenTypeMap = execute.getPathParamArgs().map(args -> {
            return args.getOptionalGenericTypeMap();
        }).orElse(Collections.emptyMap());
        final Map<Integer, Object> pathParamValueMap;
        if (paramPath != null && paramPath.length() > 0) {
            pathParamValueMap = fromParamPath(execute, paramPath, pathParamTypeList, optGenTypeMap);
        } else {
            pathParamValueMap = withoutParamPath(execute, pathParamTypeList);
        }
        assertPathParamArgsCountMatches(execute, paramPath, pathParamTypeList, pathParamValueMap);
        checkRequiredParameter(execute, paramPath, pathParamValueMap, optGenTypeMap);
        return newRequestPathParam(pathParamTypeList, pathParamValueMap);
    }

    protected RequestPathParam newRequestPathParam(List<Class<?>> pathParamTypeList, Map<Integer, Object> pathParamValueMap) {
        return new RequestPathParam(pathParamTypeList, pathParamValueMap);
    }

    // -----------------------------------------------------
    //                                        from ParamPath
    //                                        --------------
    protected Map<Integer, Object> fromParamPath(ActionExecute execute, String paramPath, List<Class<?>> pathParamTypeList,
            Map<Integer, Class<?>> optGenTypeMap) {
        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // e.g. index(String first) /product/list/2/
        //  => urlPatternRegexp=^([^/]+)$, paramPath=2, pathParamTypeList=[String]
        // 
        // e.g. sea(String first) /product/list/sea/2/
        //  => urlPatternRegexp=^sea/([^/]+)$, paramPath=2, pathParamTypeList=[String]
        // 
        // e.g. land(String first, String second) /product/list/sea/2/3/
        //  => urlPatternRegexp=^land/([^/]+)/([^/]+)$, paramPath=2/3, pathParamTypeList=[String, String]
        // _/_/_/_/_/_/_/_/_/_/
        final List<String> paramList = prepareParamList(execute, paramPath, pathParamTypeList);
        final Map<Integer, Object> valueMap = new LinkedHashMap<Integer, Object>(pathParamTypeList.size());
        int index = 0;
        for (Class<?> paramType : pathParamTypeList) {
            final String plainValue = paramList.get(index);
            valueMap.put(index, filterPathParam(execute, index, paramType, optGenTypeMap, plainValue));
            ++index;
        }
        return Collections.unmodifiableMap(valueMap);
    }

    protected List<String> prepareParamList(ActionExecute execute, String paramPath, List<Class<?>> pathParamTypeList) {
        final List<String> paramList = new ArrayList<String>(pathParamTypeList.size());
        final Matcher matcher = execute.getPreparedUrlPattern().matcher(adjustParamPathPrefix(execute, paramPath));
        if (matcher.find()) {
            for (int i = 0; i < pathParamTypeList.size(); i++) {
                paramList.add(matcher.group(i + 1)); // group 1 origin (0 provides all string)
            }
        } else { // e.g. optional parameter and actually no set it
            final List<String> elementList = Srl.splitList(paramPath, "/"); // if contains pure slash, %2F here
            for (String element : elementList) {
                paramList.add(element); // group 1 origin (0 provides all string)
            }
            final int diffCount = pathParamTypeList.size() - elementList.size();
            for (int i = 0; i < diffCount; i++) { // adjust to same count
                paramList.add(null); // dummy value e.g. for optional parameter
            }
        }
        return paramList;
    }

    protected String adjustParamPathPrefix(ActionExecute execute, String paramPath) {
        if (execute.isIndexMethod()) {
            return paramPath;
        } else {
            if (execute.getPreparedUrlPattern().isMethodNamePrefix()) { // e.g. sea()
                return execute.getExecuteMethod().getName() + "/" + paramPath;
            } else { // e.g. @word/{}/@word
                return paramPath;
            }
        }
    }

    // -----------------------------------------------------
    //                                     without ParamPath
    //                                     -----------------
    protected Map<Integer, Object> withoutParamPath(ActionExecute execute, List<Class<?>> pathParamTypeList) {
        final Map<Integer, Object> pathParamValueMap;
        if (!pathParamTypeList.isEmpty()) { // e.g. /sea/land/3/ but /sea/land/
            final Map<Integer, Object> valueMap = new LinkedHashMap<Integer, Object>(pathParamTypeList.size());
            int index = 0;
            for (Class<?> paramType : pathParamTypeList) {
                final Object value = isOptionalParameterType(paramType) ? createEmptyOptional(execute, index, paramType) : null;
                valueMap.put(index, value); // null value checked later
                ++index;
            }
            pathParamValueMap = Collections.unmodifiableMap(valueMap);
        } else {
            pathParamValueMap = Collections.emptyMap();
        }
        return pathParamValueMap;
    }

    // ===================================================================================
    //                                                                Filter URL Parameter
    //                                                                ====================
    protected Object filterPathParam(ActionExecute execute, int index, Class<?> paramType, Map<Integer, Class<?>> optGenTypeMap,
            String plainValue) {
        if (plainValue == null || plainValue.isEmpty()) {
            if (optGenTypeMap.containsKey(index)) { // optional parameter
                return createEmptyOptional(execute, index, paramType);
            } else { // required but no value
                return null; // will be checked immediately later
            }
        }
        final String decoded = urlDecode(plainValue);
        try {
            return doFilterPathParam(execute, index, paramType, optGenTypeMap, decoded);
        } catch (NumberFormatException | ParseDateException | ParseBooleanException e) { // conversion failures
            // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
            // suppress easy 500 error by e.g. non-number path parameter
            //  (o): /edit/123/
            //  (x): /edit/abc/ *this case
            // 
            // classification's failure is already handled as 404 here
            // _/_/_/_/_/_/_/_/_/_/
            handleParameterConversionFailureException(execute, index, paramType, plainValue, decoded, e);
            return null; // unreachable
        }
    }

    // -----------------------------------------------------
    //                                       Actually Filter
    //                                       ---------------
    protected String urlDecode(String value) {
        final String encoding = requestManager.getCharacterEncoding().get(); // should be already set
        try {
            return URLDecoder.decode(value, encoding);
        } catch (UnsupportedEncodingException e) {
            String msg = "Unsupported encoding: value=" + value + ", encoding=" + encoding;
            throw new IllegalStateException(msg, e);
        }
    }

    protected Object doFilterPathParam(ActionExecute execute, int index, Class<?> paramType, Map<Integer, Class<?>> optGenTypeMap,
            String exp) {
        final Object filtered;
        if (paramType.isPrimitive()) {
            filtered = DfTypeUtil.toWrapper(exp, paramType);
        } else if (Number.class.isAssignableFrom(paramType)) {
            filtered = DfTypeUtil.toNumber(exp, paramType);
            // old date types are unsupported for LocalDate invitation
            //} else if (Timestamp.class.isAssignableFrom(paramType)) {
            //    filtered = DfTypeUtil.toTimestamp(exp);
            //} else if (Time.class.isAssignableFrom(paramType)) {
            //    filtered = DfTypeUtil.toTime(exp);
            //} else if (java.util.Date.class.isAssignableFrom(paramType)) {
            //    filtered = DfTypeUtil.toDate(exp);
        } else if (LocalDate.class.isAssignableFrom(paramType)) { // #date_parade
            filtered = DfTypeUtil.toLocalDate(exp);
        } else if (LocalDateTime.class.isAssignableFrom(paramType)) {
            filtered = DfTypeUtil.toLocalDateTime(exp);
        } else if (LocalTime.class.isAssignableFrom(paramType)) {
            filtered = DfTypeUtil.toLocalTime(exp);
        } else if (Boolean.class.isAssignableFrom(paramType)) {
            filtered = DfTypeUtil.toBoolean(exp);
        } else if (LaClassificationUtil.isCls(paramType)) {
            filtered = toVerifiedClassification(execute, paramType, exp);
        } else if (isOptionalParameterType(paramType)) {
            final Class<?> optGenType = optGenTypeMap.get(index);
            if (optGenType != null) {
                final Object paramValue = doFilterPathParam(execute, index, optGenType, optGenTypeMap, exp);
                filtered = createPresentOptional(paramType, paramValue);
            } else { // basically no way
                throwOptionalGenericTypeNotFoundException(execute, index, paramType, optGenTypeMap, exp);
                return null; // unreachable
            }
        } else {
            filtered = exp;
        }
        return filtered;
    }

    protected Object toVerifiedClassification(ActionExecute execute, Class<?> paramType, Object filtered) {
        try {
            return LaClassificationUtil.toCls(paramType, filtered);
        } catch (ClassificationUnknownCodeException e) {
            handleClassificationUnknownCodeException(execute, filtered, e);
            return null; // unreachable
        }
    }

    protected void handleClassificationUnknownCodeException(ActionExecute execute, Object filtered, ClassificationUnknownCodeException e) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Cannot convert the code of the path parameter to the classification:");
        sb.append("\n[Classification Convert Failure]");
        sb.append("\n").append(execute);
        sb.append("\ncode=").append(filtered);
        sb.append("\n").append(e.getClass().getName()).append("\n").append(e.getMessage());
        final String msg = sb.toString();
        throw new Forced404NotFoundException(msg, getClassificationUnknownCodeMessages(), e);
    }

    protected UserMessages getClassificationUnknownCodeMessages() {
        return UserMessages.empty();
    }

    protected void throwOptionalGenericTypeNotFoundException(ActionExecute execute, int index, Class<?> paramType,
            Map<Integer, Class<?>> optionalGenericTypeMap, Object filtered) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the optional generic type for the parameter.");
        br.addItem("Action Execute");
        br.addElement(execute);
        br.addItem("Parameter Index");
        br.addElement(index);
        br.addItem("Parameter Type");
        br.addElement(paramType + buildOptionalGenericDisp(optionalGenericTypeMap, index));
        br.addItem("OptionalGenericType Map");
        br.addElement(optionalGenericTypeMap);
        br.addItem("Filtered Value");
        br.addElement(filtered);
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg);
    }

    protected String buildOptionalGenericDisp(Map<Integer, Class<?>> optionalGenericTypeMap, int index) {
        final Class<?> genericType = optionalGenericTypeMap.get(index);
        return genericType != null ? "<" + genericType.getName() + ">" : "";
    }

    // -----------------------------------------------------
    //                                    Conversion Failure
    //                                    ------------------
    protected void handleParameterConversionFailureException(ActionExecute execute, int index, Class<?> paramType, String plainValue,
            String decoded, RuntimeException cause) {
        final String msg = buildParameterConversionFailureMessage(execute, index, paramType, plainValue, decoded, cause);
        throwExecuteParameterMismatchException(msg); // treat it as no such execute
    }

    protected String buildParameterConversionFailureMessage(ActionExecute execute, int index, Class<?> paramType, String plainValue,
            String decoded, RuntimeException cause) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Cannot convert the parameter to argument type.");
        br.addItem("Action Execute");
        br.addElement(execute);
        br.addItem("Parameter Index");
        br.addElement(index);
        br.addItem("Parameter Type");
        br.addElement(paramType.getName() + buildOptionalGenericDisp(execute, index));
        br.addItem("Parameter Value");
        br.addElement("plain   : " + plainValue);
        br.addElement("decoded : " + decoded);
        br.addItem("Cause");
        br.addElement(cause.getMessage());
        return br.buildExceptionMessage();
    }

    protected String buildOptionalGenericDisp(ActionExecute execute, int index) {
        return execute.getPathParamArgs().map(args -> {
            final Class<?> genericType = args.getOptionalGenericTypeMap().get(index);
            return genericType != null ? "<" + genericType.getName() + ">" : "";
        }).orElse("");
    }

    // ===================================================================================
    //                                                                Assert URL Parameter
    //                                                                ====================
    protected void assertPathParamArgsCountMatches(ActionExecute execute, String paramPath, List<Class<?>> pathParamTypeList,
            Map<Integer, Object> pathParamValueMap) {
        if (pathParamTypeList.size() != pathParamValueMap.size()) {
            throwPathParamArgsDifferentCountException(execute, paramPath, pathParamTypeList, pathParamValueMap);
        }
    }

    protected void throwPathParamArgsDifferentCountException(ActionExecute execute, String paramPath, List<Class<?>> pathParamTypeList,
            Map<Integer, Object> pathParamValueMap) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Different count of path parameters from URL pattern.");
        br.addItem("Advice");
        br.addElement("The count of path parameters defined at execute method");
        br.addElement("should be same as definition in URL pattern.");
        br.addElement("  (x):");
        br.addElement("    @Execute(urlPattern = \"{}/sea/{}\")");
        br.addElement("    public HtmlResponse index(int pageNumber) {  // *Bad");
        br.addElement("  (o):");
        br.addElement("    @Execute(urlPattern = \"{}/sea/{}\")");
        br.addElement("    public HtmlResponse index(int pageNumber, String keyword) { // Good");
        br.addElement("  (o):");
        br.addElement("    @Execute // Good: same as \"{}\"");
        br.addElement("    public HtmlResponse index(int pageNumber) {");
        br.addItem("Action Execute");
        br.addElement(execute);
        br.addItem("Parameter Path");
        br.addElement(paramPath);
        br.addItem("Defined Args");
        br.addElement(pathParamTypeList);
        br.addItem("Value Map");
        br.addElement(pathParamValueMap);
        final String msg = br.buildExceptionMessage();
        throw new PathParamArgsDifferentCountException(msg);
    }

    protected void checkRequiredParameter(ActionExecute execute, String paramPath, Map<Integer, Object> pathParamValueMap,
            Map<Integer, Class<?>> optGenTypeMap) {
        for (Entry<Integer, Object> entry : pathParamValueMap.entrySet()) {
            final Integer index = entry.getKey();
            final Object value = entry.getValue();
            if (optGenTypeMap.containsKey(index)) { // not required
                if (value == null || !(value instanceof OptionalThing)) { // no way
                    throwIllegalOptionalHandlingException(execute, paramPath, pathParamValueMap, optGenTypeMap, index, value);
                }
                continue;
            } else { // required
                if (value == null) { // already filtered, e.g. empty string to null
                    throwExecuteParameterMismatchException(buildRequiredPropertyNotFoundMessage(execute, paramPath, index));
                } else if (value instanceof OptionalThing) { // no way
                    throwIllegalOptionalHandlingException(execute, paramPath, pathParamValueMap, optGenTypeMap, index, value);
                }
            }
        }
    }

    protected void throwIllegalOptionalHandlingException(ActionExecute execute, String paramPath, Map<Integer, Object> pathParamValueMap,
            Map<Integer, Class<?>> optGenTypeMap, int index, Object value) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Illegal optional handling. (Framework Exception)");
        br.addItem("Action Execute");
        br.addElement(execute);
        br.addItem("Parameter Path");
        br.addElement(paramPath);
        br.addItem("Path Parameter Value Map");
        br.addElement(pathParamValueMap);
        br.addItem("Optional GenericType Map");
        br.addElement(optGenTypeMap);
        br.addItem("Parameter Index");
        br.addElement(index);
        br.addItem("Parameter Value");
        br.addElement("type  : " + value != null ? value.getClass().getName() : null);
        br.addElement("value : " + value);
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg);
    }

    protected String buildRequiredPropertyNotFoundMessage(ActionExecute execute, String paramPath, int index) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the value of required property.");
        br.addItem("Action Execute");
        br.addElement(execute);
        br.addItem("Parameter Path");
        br.addElement(paramPath);
        br.addItem("Parameter Index");
        br.addElement(index);
        return br.buildExceptionMessage();
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected boolean isOptionalParameterType(Class<?> paramType) {
        return LaActionExecuteUtil.isOptionalParameterType(paramType);
    }

    protected void throwExecuteParameterMismatchException(String msg) {
        // no server error because it can occur by user's trick easily e.g. changing URL
        // while, might be client bugs (or server) so request delicate error
        throw new Forced404NotFoundException(msg, getExecuteParameterMismatchMessages());
    }

    protected UserMessages getExecuteParameterMismatchMessages() {
        return UserMessages.empty();
    }

    // -----------------------------------------------------
    //                                      Present Optional
    //                                      ----------------
    protected Object createPresentOptional(Class<?> paramType, Object paramValue) {
        return OptionalThing.of(paramValue);
    }

    // -----------------------------------------------------
    //                                        Empty Optional
    //                                        --------------
    protected OptionalThing<Object> createEmptyOptional(ActionExecute execute, int fixedIndex, Class<?> paramType) {
        return OptionalThing.ofNullable(null, () -> {
            throwPathParamOptionalParameterEmptyAccessException(execute, fixedIndex, paramType);
        });
    }

    protected void throwPathParamOptionalParameterEmptyAccessException(ActionExecute execute, int index, Class<?> paramType) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The empty optional parameter was accessed.");
        br.addItem("Advice");
        br.addElement("You should access the optional object with empty check.");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse index(OptionalThing<Integer> pageNumber) {");
        br.addElement("        int num = pageNumber.get(); // *Bad");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public HtmlResponse index(OptionalThing<Integer> pageNumber) {");
        br.addElement("        pageNumber.ifPresent(num -> {; // Good");
        br.addElement("            ... = selectPage(num)");
        br.addElement("        }).orElse(() -> {");
        br.addElement("            ...");
        br.addElement("        });");
        br.addElement("    }");
        br.addItem("Action Execute");
        br.addElement(execute);
        br.addItem("Parameter Index");
        br.addElement(index);
        br.addItem("Parameter Type");
        br.addElement(paramType);
        final String msg = br.buildExceptionMessage();
        throw new PathParamOptionalParameterEmptyAccessException(msg);
    }
}
