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
package org.lastaflute.web.ruts.process;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Date;
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
import org.dbflute.util.DfTypeUtil.ParseDateException;
import org.dbflute.util.Srl;
import org.lastaflute.web.exception.ForcedRequest404NotFoundException;
import org.lastaflute.web.exception.UrlParamArgsDifferentCountException;
import org.lastaflute.web.exception.UrlParamOptionalParameterEmptyAccessException;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.util.LaActionExecuteUtil;
import org.lastaflute.web.util.LaDBFluteUtil;
import org.lastaflute.web.util.LaDBFluteUtil.ClassificationConvertFailureException;

/**
 * @author jflute
 */
public class RequestUrlParamAnalyzer {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final RequestManager requestManager;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public RequestUrlParamAnalyzer(RequestManager requestManager) {
        this.requestManager = requestManager;
    }

    // ===================================================================================
    //                                                                             Analyze
    //                                                                             =======
    /**
     * @param execute The definition of action execute. (NotNull)
     * @param paramPath The parameter path from URL. (NullAllowed)
     * @return The object for URL parameter value that has e.g. map:{index = value} (NotNull)
     */
    public RequestUrlParam analyzeUrlParam(ActionExecute execute, String paramPath) {
        return doAnalyzeUrlParam(execute, extractRealParamPath(execute, paramPath));
    }

    protected String extractRealParamPath(ActionExecute execute, String paramPath) {
        if (paramPath == null) {
            return null;
        }
        final String real;
        if (execute.isIndexMethod()) {
            real = paramPath;
        } else { // sea()
            final String methodName = execute.getExecuteMethod().getName();
            if (paramPath.equals(methodName) || paramPath.startsWith(methodName + "/")) { // e.g. sea or sea/3/
                real = Srl.ltrim(Srl.substringFirstRear(paramPath, execute.getExecuteMethod().getName()), "/");
            } else {
                real = paramPath;
            }
        }
        return real;
    }

    protected RequestUrlParam doAnalyzeUrlParam(ActionExecute execute, String paramPath) {
        final List<Class<?>> urlParamTypeList = execute.getUrlParamArgs().map(args -> {
            return args.getUrlParamTypeList();
        }).orElse(Collections.emptyList());
        final Map<Integer, Class<?>> optGenTypeMap = execute.getUrlParamArgs().map(args -> {
            return args.getOptionalGenericTypeMap();
        }).orElse(Collections.emptyMap());
        final Map<Integer, Object> urlParamValueMap;
        if (paramPath != null && paramPath.length() > 0) {
            urlParamValueMap = fromParamPath(execute, paramPath, urlParamTypeList, optGenTypeMap);
        } else {
            urlParamValueMap = withoutParamPath(execute, urlParamTypeList);
        }
        assertUrlParamArgsCountMatches(execute, paramPath, urlParamTypeList, urlParamValueMap);
        checkRequiredParameter(execute, paramPath, urlParamValueMap, optGenTypeMap);
        return newRequestUrlParam(urlParamTypeList, urlParamValueMap);
    }

    protected RequestUrlParam newRequestUrlParam(List<Class<?>> urlParamTypeList, Map<Integer, Object> urlParamValueMap) {
        return new RequestUrlParam(urlParamTypeList, urlParamValueMap);
    }

    // -----------------------------------------------------
    //                                        from ParamPath
    //                                        --------------
    protected String adjustParamPathPrefix(ActionExecute execute, String paramPath) {
        return execute.isIndexMethod() ? paramPath : execute.getExecuteMethod().getName() + "/" + paramPath;
    }

    protected Map<Integer, Object> fromParamPath(ActionExecute execute, String paramPath, List<Class<?>> urlParamTypeList,
            Map<Integer, Class<?>> optGenTypeMap) {
        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // e.g. index(String first) /product/list/2/
        //  => urlPatternRegexp=^([^/]+)$, paramPath=2, urlParamTypeList=[String]
        // 
        // e.g. sea(String first) /product/list/sea/2/
        //  => urlPatternRegexp=^sea/([^/]+)$, paramPath=2, urlParamTypeList=[String]
        // 
        // e.g. land(String first, String second) /product/list/sea/2/3/
        //  => urlPatternRegexp=^land/([^/]+)/([^/]+)$, paramPath=2/3, urlParamTypeList=[String, String]
        // _/_/_/_/_/_/_/_/_/_/
        final List<String> paramList = prepareParamList(execute, paramPath, urlParamTypeList);
        final Map<Integer, Object> valueMap = new LinkedHashMap<Integer, Object>(urlParamTypeList.size());
        int index = 0;
        for (Class<?> paramType : urlParamTypeList) {
            final String plainValue = paramList.get(index);
            valueMap.put(index, filterUrlParam(execute, index, paramType, optGenTypeMap, plainValue));
            ++index;
        }
        return Collections.unmodifiableMap(valueMap);
    }

    protected List<String> prepareParamList(ActionExecute execute, String paramPath, List<Class<?>> urlParamTypeList) {
        final List<String> paramList = new ArrayList<String>(urlParamTypeList.size());
        final Matcher matcher = execute.getUrlPatternRegexp().matcher(adjustParamPathPrefix(execute, paramPath));
        if (matcher.find()) {
            for (int i = 0; i < urlParamTypeList.size(); i++) {
                paramList.add(matcher.group(i + 1)); // group 1 origin (0 provides all string)
            }
        } else { // e.g. optional parameter and actually no set it
            final List<String> elementList = Srl.splitList(paramPath, "/"); // if contains pure slash, %2F here
            for (String element : elementList) {
                paramList.add(element); // group 1 origin (0 provides all string)
            }
            final int diffCount = urlParamTypeList.size() - elementList.size();
            for (int i = 0; i < diffCount; i++) { // adjust to same count
                paramList.add(null); // dummy value e.g. for optional parameter
            }
        }
        return paramList;
    }

    // -----------------------------------------------------
    //                                     without ParamPath
    //                                     -----------------
    protected Map<Integer, Object> withoutParamPath(ActionExecute execute, List<Class<?>> urlParamTypeList) {
        final Map<Integer, Object> urlParamValueMap;
        if (!urlParamTypeList.isEmpty()) { // e.g. /sea/land/3/ but /sea/land/
            final Map<Integer, Object> valueMap = new LinkedHashMap<Integer, Object>(urlParamTypeList.size());
            int index = 0;
            for (Class<?> paramType : urlParamTypeList) {
                final Object value = isOptionalParameterType(paramType) ? createEmptyOptional(execute, index, paramType) : null;
                valueMap.put(index, value); // null value checked later
                ++index;
            }
            urlParamValueMap = Collections.unmodifiableMap(valueMap);
        } else {
            urlParamValueMap = Collections.emptyMap();
        }
        return urlParamValueMap;
    }

    // ===================================================================================
    //                                                                Filter URL Parameter
    //                                                                ====================
    protected Object filterUrlParam(ActionExecute execute, int index, Class<?> paramType, Map<Integer, Class<?>> optGenTypeMap,
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
            return doFilterUrlParam(execute, index, paramType, optGenTypeMap, decoded);
        } catch (NumberFormatException | ParseDateException e) { // conversion failures
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

    protected Object doFilterUrlParam(ActionExecute execute, int index, Class<?> paramType, Map<Integer, Class<?>> optGenTypeMap,
            Object filtered) {
        if (Number.class.isAssignableFrom(paramType)) {
            filtered = DfTypeUtil.toNumber(filtered, paramType);
        } else if (int.class.isAssignableFrom(paramType)) {
            filtered = DfTypeUtil.toPrimitiveInt(filtered);
        } else if (long.class.isAssignableFrom(paramType)) {
            filtered = DfTypeUtil.toPrimitiveLong(filtered);
        } else if (Date.class.isAssignableFrom(paramType)) {
            filtered = DfTypeUtil.toDate(filtered);
        } else if (LocalDate.class.isAssignableFrom(paramType)) {
            filtered = DfTypeUtil.toLocalDate(filtered);
        } else if (LocalDateTime.class.isAssignableFrom(paramType)) {
            filtered = DfTypeUtil.toLocalDateTime(filtered);
        } else if (LocalTime.class.isAssignableFrom(paramType)) {
            filtered = DfTypeUtil.toLocalTime(filtered);
        } else if (LaDBFluteUtil.isClassificationType(paramType)) {
            filtered = toVerifiedClassification(execute, paramType, filtered);
        } else if (isOptionalParameterType(paramType)) {
            final Class<?> optGenType = optGenTypeMap.get(index);
            if (optGenType != null) {
                final Object paramValue = doFilterUrlParam(execute, index, optGenType, optGenTypeMap, filtered);
                filtered = createPresentOptional(paramType, paramValue);
            } else { // basically no way
                throwOptionalGenericTypeNotFoundException(execute, index, paramType, optGenTypeMap, filtered);
            }
        }
        return filtered;
    }

    protected Object toVerifiedClassification(ActionExecute execute, Class<?> paramType, Object filtered) {
        try {
            return LaDBFluteUtil.toVerifiedClassification(paramType, filtered);
        } catch (ClassificationConvertFailureException e) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Cannot convert the code of the URL parameter to the classification:");
            sb.append("\n[Classification Convert Failure]");
            sb.append("\n").append(execute);
            sb.append("\ncode=").append(filtered);
            sb.append("\n").append(e.getClass().getName()).append("\n").append(e.getMessage());
            final String msg = sb.toString();
            throw new ForcedRequest404NotFoundException(msg, e);
        }
    }

    protected void throwOptionalGenericTypeNotFoundException(ActionExecute execute, int index, Class<?> urlParamType,
            Map<Integer, Class<?>> optionalGenericTypeMap, Object filtered) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the optional generic type for the parameter.");
        br.addItem("Action Execute");
        br.addElement(execute);
        br.addItem("Parameter Index");
        br.addElement(index);
        br.addItem("Parameter Type");
        br.addElement(urlParamType);
        br.addItem("OptionalGenericType Map");
        br.addElement(optionalGenericTypeMap);
        br.addItem("Filtered Value");
        br.addElement(filtered);
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg);
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
        br.addElement(paramType);
        br.addItem("Parameter Value");
        br.addElement("plain   : " + plainValue);
        br.addElement("decoded : " + decoded);
        br.addItem("Cause");
        br.addElement(cause.getMessage());
        return br.buildExceptionMessage();
    }

    // ===================================================================================
    //                                                                Assert URL Parameter
    //                                                                ====================
    protected void assertUrlParamArgsCountMatches(ActionExecute execute, String paramPath, List<Class<?>> urlParamTypeList,
            Map<Integer, Object> urlParamValueMap) {
        if (urlParamTypeList.size() != urlParamValueMap.size()) {
            throwUrlParamArgsDifferentCountException(execute, paramPath, urlParamTypeList, urlParamValueMap);
        }
    }

    protected void throwUrlParamArgsDifferentCountException(ActionExecute execute, String paramPath, List<Class<?>> urlParamTypeList,
            Map<Integer, Object> urlParamValueMap) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Different count of URL parameters from URL pattern.");
        br.addItem("Advice");
        br.addElement("The count of URL parameters defined at execute method");
        br.addElement("should be same as definition in URL pattern.");
        br.addElement("  (x):");
        br.addElement("    @Execute(urlPattern = \"{}/sea/{}\")");
        br.addElement("    public HtmlResponse index(int pageNumber) {  // *NG");
        br.addElement("  (o):");
        br.addElement("    @Execute(urlPattern = \"{}/sea/{}\")");
        br.addElement("    public HtmlResponse index(int pageNumber, String keyword) { // OK");
        br.addElement("  (o):");
        br.addElement("    @Execute // OK: same as \"{}\"");
        br.addElement("    public HtmlResponse index(int pageNumber) {");
        br.addItem("Action Execute");
        br.addElement(execute);
        br.addItem("Parameter Path");
        br.addElement(paramPath);
        br.addItem("Defined Args");
        br.addElement(urlParamTypeList);
        br.addItem("Value Map");
        br.addElement(urlParamValueMap);
        final String msg = br.buildExceptionMessage();
        throw new UrlParamArgsDifferentCountException(msg);
    }

    protected void checkRequiredParameter(ActionExecute execute, String paramPath, Map<Integer, Object> urlParamValueMap,
            Map<Integer, Class<?>> optGenTypeMap) {
        for (Entry<Integer, Object> entry : urlParamValueMap.entrySet()) {
            final Integer index = entry.getKey();
            final Object value = entry.getValue();
            if (optGenTypeMap.containsKey(index)) { // not required
                if (value == null || !(value instanceof OptionalThing)) { // no way
                    throwIllegalOptionalHandlingException(execute, paramPath, urlParamValueMap, optGenTypeMap, index, value);
                }
                continue;
            } else { // required
                if (value == null) { // already filtered, e.g. empty string to null
                    throwExecuteParameterMismatchException(buildRequiredPropertyNotFoundMessage(execute, paramPath, index));
                } else if (value instanceof OptionalThing) { // no way
                    throwIllegalOptionalHandlingException(execute, paramPath, urlParamValueMap, optGenTypeMap, index, value);
                }
            }
        }
    }

    protected void throwIllegalOptionalHandlingException(ActionExecute execute, String paramPath, Map<Integer, Object> urlParamValueMap,
            Map<Integer, Class<?>> optGenTypeMap, int index, Object value) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Illegal optional handling. (Framework Exception)");
        br.addItem("Action Execute");
        br.addElement(execute);
        br.addItem("Parameter Path");
        br.addElement(paramPath);
        br.addItem("URLParameter Value Map");
        br.addElement(urlParamValueMap);
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
        throw new ForcedRequest404NotFoundException(msg);
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
            throwUrlParamOptionalParameterEmptyAccessException(execute, fixedIndex, paramType);
        });
    }

    protected void throwUrlParamOptionalParameterEmptyAccessException(ActionExecute execute, int index, Class<?> paramType) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The empty optional parameter was accessed.");
        br.addItem("Advice");
        br.addElement("You should access the optional object with empty check.");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse index(OptionalThing<Integer> pageNumber) {");
        br.addElement("        int num = pageNumber.get(); // *NG");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public HtmlResponse index(OptionalThing<Integer> pageNumber) {");
        br.addElement("        pageNumber.ifPresent(num -> {; // OK");
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
        throw new UrlParamOptionalParameterEmptyAccessException(msg);
    }
}
