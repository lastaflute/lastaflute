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
package org.lastaflute.web.ruts.config.analyzer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.lastaflute.web.exception.ActionPathParameterDifferentArgsException;
import org.lastaflute.web.exception.UrlPatternBeginBraceNotFoundException;
import org.lastaflute.web.exception.UrlPatternEndBraceNotFoundException;
import org.lastaflute.web.exception.UrlPatternFrontOrRearSlashUnneededException;
import org.lastaflute.web.exception.UrlPatternMethodKeywordWithOptionalArgException;
import org.lastaflute.web.exception.UrlPatternNonsenseSettingException;
import org.lastaflute.web.ruts.config.specifed.SpecifiedUrlPattern;
import org.lastaflute.web.util.LaActionExecuteUtil;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class UrlPatternAnalyzer {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    public static final String ELEMENT_BASIC_PATTERN = "([^/]+)";

    // [RegularExpression and JDK Version Story] by jflute (2021/06/09)
    // e.g.
    //  ProductsAction@get$index()
    //  ProductsPurchasesAction@get$index()
    //
    // GET /products/1/purchases/2 was ProductsAction hit (then NumberFormatException 404)
    //
    // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
    // // JDK 8: false
    // // JDK 11: true
    // // JDK 16: true
    // log(Pattern.compile("^([^/&&\\-\\.\\d]+)$").matcher("purchases/1/2").find());
    //
    // // JDK 8: false
    // // JDK 11: false
    // // JDK 16: false
    // log(Pattern.compile("^([[^/]&&\\-\\.\\d]+)$").matcher("purchases/1/2").find());
    // _/_/_/_/_/_/_/_/_/_/
    //
    // it works well by quoting "^/" (means not slach) with "[]"
    // so first I wrote test deeply to keep matching result and after that I fixed it
    //
    //public static final String ELEMENT_NUMBER_PATTERN = "([^/&&\\-\\.\\d]+)";
    public static final String ELEMENT_NUMBER_PATTERN = "([[^/]&&\\-\\.\\d]+)";

    public static final String METHOD_KEYWORD_MARK = "@word";
    public static final String REST_DELIMITER = "$";

    // ===================================================================================
    //                                                                             Extract
    //                                                                             =======
    public OptionalThing<String> extractRestfulHttpMethod(Method executeMethod) {
        final String methodName = executeMethod.getName();
        final String extracted = methodName.contains(REST_DELIMITER) ? Srl.substringFirstFront(methodName, REST_DELIMITER) : null;
        return OptionalThing.ofNullable(extracted, () -> {
            throw new IllegalStateException("Not found RESTful HTTP method: " + toSimpleMethodExp(executeMethod));
        });
    }

    // ===================================================================================
    //                                                                              Choose
    //                                                                              ======
    public UrlPatternChosenBox choose(Method executeMethod, String mappingMethodName,
            OptionalThing<SpecifiedUrlPattern> specifiedUrlPattern, List<Class<?>> pathParamTypeList) {
        specifiedUrlPattern.ifPresent(pattern -> {
            checkSpecifiedUrlPattern(executeMethod, pattern, pathParamTypeList);
        });
        final UrlPatternChosenBox chosenBox;
        if (specifiedUrlPattern.isPresent()) { // e.g. urlPattern="{}"
            final String patternValue = specifiedUrlPattern.get().getPatternValue();
            chosenBox = adjustUrlPatternMethodPrefix(executeMethod, patternValue, mappingMethodName, /*specified*/true);
        } else { // urlPattern=[no definition]
            if (!pathParamTypeList.isEmpty()) { // e.g. sea(int pageNumber)
                final String derivedUrlPattern = buildDerivedUrlPattern(pathParamTypeList);
                chosenBox = adjustUrlPatternMethodPrefix(executeMethod, derivedUrlPattern, mappingMethodName, /*specified*/false);
            } else { // e.g. index(), sea() *no parameter
                chosenBox = adjustUrlPatternByMethodNameWithoutParam(mappingMethodName);
            }
        }
        return chosenBox;
    }

    protected String buildDerivedUrlPattern(List<Class<?>> pathParamTypeList) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pathParamTypeList.size(); i++) {
            sb.append(i > 0 ? "/" : "").append("{}");
        }
        return sb.toString();
    }

    public static class UrlPatternChosenBox {

        protected final String resolvedUrlPattern;
        protected final String sourceUrlPattern;
        protected final boolean specified;
        protected boolean methodNamePrefix;

        public UrlPatternChosenBox(String resolvedUrlPattern, String sourceUrlPattern, boolean specified) {
            assertArgumentNotNull("resolvedUrlPattern", resolvedUrlPattern);
            assertArgumentNotNull("sourceUrlPattern", sourceUrlPattern);
            this.resolvedUrlPattern = resolvedUrlPattern;
            this.sourceUrlPattern = sourceUrlPattern;
            this.specified = specified;
        }

        public UrlPatternChosenBox withMethodPrefix() {
            methodNamePrefix = true;
            return this;
        }

        protected void assertArgumentNotNull(String variableName, Object value) {
            if (variableName == null) {
                throw new IllegalArgumentException("The variableName should not be null.");
            }
            if (value == null) {
                throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
            }
        }

        public String getResolvedUrlPattern() {
            return resolvedUrlPattern;
        }

        public String getSourceUrlPattern() {
            return sourceUrlPattern;
        }

        public boolean isSpecified() {
            return specified;
        }

        public boolean isMethodNamePrefix() {
            return methodNamePrefix;
        }
    }

    // -----------------------------------------------------
    //                                       Check Specified
    //                                       ---------------
    protected void checkSpecifiedUrlPattern(Method executeMethod, SpecifiedUrlPattern specifiedUrlPattern,
            List<Class<?>> pathParamTypeList) {
        final String patternStr = specifiedUrlPattern.getPatternValue();
        if (canBeAbbreviatedUrlPattern(patternStr)) {
            throwUrlPatternNonsenseSettingException(executeMethod, patternStr);
        }
        if (hasFrontOrRearSlashUrlPattern(patternStr)) {
            throwUrlPatternFrontOrRearSlashUnneededException(executeMethod, patternStr);
        }
        if (hasMethodKeywordWithOptionalArg(patternStr, pathParamTypeList)) {
            throwUrlPatternMethodKeywordWithOptionalArgException(executeMethod, patternStr);
        }
    }

    protected boolean canBeAbbreviatedUrlPattern(String str) { // format check so simple logic
        return Srl.equalsPlain(str, "{}", "{}/{}", "{}/{}/{}", "{}/{}/{}/{}", "{}/{}/{}/{}/{}", "{}/{}/{}/{}/{}/{}");
    }

    protected void throwUrlPatternNonsenseSettingException(Method executeMethod, String specifiedUrlPattern) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Found the non-sense urlPattern.");
        br.addItem("Advice");
        br.addElement("You can abbreviate the urlPattern attribute");
        br.addElement("because it is very simple pattern.");
        br.addElement("  (x):");
        br.addElement("    @Execute(urlPattern = \"{}\") // *Bad");
        br.addElement("    public void index(int pageNumber) {");
        br.addElement("  (o):");
        br.addElement("    @Execute // Good: abbreviate it");
        br.addElement("    public void index(int pageNumber) {");
        br.addElement("  (x):");
        br.addElement("    @Execute(urlPattern = \"{}/{}\") // *Bad");
        br.addElement("    public void index(int pageNumber, String keyword) {");
        br.addElement("  (o):");
        br.addElement("    @Execute // Good: abbreviate it");
        br.addElement("    public void index(int pageNumber, String keyword) {");
        br.addItem("Execute Method");
        br.addElement(toSimpleMethodExp(executeMethod));
        br.addItem("Specified urlPattern");
        br.addElement(specifiedUrlPattern);
        final String msg = br.buildExceptionMessage();
        throw new UrlPatternNonsenseSettingException(msg);
    }

    protected boolean hasFrontOrRearSlashUrlPattern(String specifiedUrlPattern) {
        return specifiedUrlPattern.startsWith("/") || specifiedUrlPattern.endsWith("/");
    }

    protected void throwUrlPatternFrontOrRearSlashUnneededException(Method executeMethod, String specifiedUrlPattern) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Unneeded front or rear slash '/' in urlPattern.");
        br.addItem("Advice");
        br.addElement("Remove the front or rear slash '/' from your urlPattern");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    urlPattern=\"/{}/sea/{}/\" // *Bad");
        br.addElement("    urlPattern=\"{}/sea/{}/\" // *Bad");
        br.addElement("    urlPattern=\"/{}/sea/{}\" // *Bad");
        br.addElement("  (o):");
        br.addElement("    urlPattern=\"{}/sea/{}\" // Good");
        br.addItem("Execute Method");
        br.addElement(toSimpleMethodExp(executeMethod));
        br.addItem("Specified urlPattern");
        br.addElement(specifiedUrlPattern);
        final String msg = br.buildExceptionMessage();
        throw new UrlPatternFrontOrRearSlashUnneededException(msg);
    }

    protected boolean hasMethodKeywordWithOptionalArg(String specifiedUrlPattern, List<Class<?>> pathParamTypeList) {
        return specifiedUrlPattern.contains(METHOD_KEYWORD_MARK) && pathParamTypeList.stream().anyMatch(tp -> {
            return OptionalThing.class.isAssignableFrom(tp);
        });
    }

    protected void throwUrlPatternMethodKeywordWithOptionalArgException(Method executeMethod, String specifiedUrlPattern) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Found the method keyword of urlPattern with optional argument.");
        br.addItem("Advice");
        br.addElement("Cannot use method keyword with optional argument.");
        br.addElement("Remove method keyword or quit using optional type.");
        br.addElement("  (x):");
        br.addElement("    @Execute(urlPattern = \"{}/@word\")");
        br.addElement("    public void sea(OptionalThing<Integer> landId) { // *Bad");
        br.addElement("  (o):");
        br.addElement("    @Execute");
        br.addElement("    public void sea(OptionalThing<Integer> landId) { // Good");
        br.addElement("  (o):");
        br.addElement("    @Execute(urlPattern = \"{}/@word\")");
        br.addElement("    public void sea(Integer landId) { // Good");
        br.addItem("Execute Method");
        br.addElement(toSimpleMethodExp(executeMethod));
        br.addItem("Specified urlPattern");
        br.addElement(specifiedUrlPattern);
        final String msg = br.buildExceptionMessage();
        throw new UrlPatternMethodKeywordWithOptionalArgException(msg);
    }

    // -----------------------------------------------------
    //                                        has urlPattern
    //                                        --------------
    protected UrlPatternChosenBox adjustUrlPatternMethodPrefix(Method executeMethod, String sourceUrlPattern, String methodName,
            boolean specified) {
        final String keywordMark = getMethodKeywordMark();
        if (methodName.equals("index")) { // e.g. index(pageNumber), urlPattern="{}"
            if (sourceUrlPattern.contains(keywordMark)) {
                throwUrlPatternMethodKeywordMarkButIndexMethodException(executeMethod, sourceUrlPattern, keywordMark);
            }
            return new UrlPatternChosenBox(sourceUrlPattern, sourceUrlPattern, specified);
        } else { // e.g. sea(pageNumber), urlPattern="{}"
            if (sourceUrlPattern.contains(keywordMark)) { // e.g. @word/{}/@word
                final List<String> keywordList = splitMethodKeywordList(methodName);
                if (keywordList.size() != Srl.count(sourceUrlPattern, keywordMark)) { // e.g. sea() but @word/{}/@word
                    throwUrlPatternMethodKeywordMarkUnmatchedCountException(executeMethod, sourceUrlPattern, keywordMark);
                }
                final String resolved = keywordList.stream().reduce(sourceUrlPattern, (first, second) -> {
                    return Srl.substringFirstFront(first, keywordMark) + second + Srl.substringFirstRear(first, keywordMark);
                }); // e.g. sea/land
                return new UrlPatternChosenBox(resolved, sourceUrlPattern, specified);
            } else {
                return new UrlPatternChosenBox(methodName + "/" + sourceUrlPattern, sourceUrlPattern, specified).withMethodPrefix();
            }
        }
    }

    protected String getMethodKeywordMark() {
        return METHOD_KEYWORD_MARK;
    }

    protected List<String> splitMethodKeywordList(String methodName) {
        final char[] charArray = methodName.toCharArray();
        final StringBuilder sb = new StringBuilder();
        final List<String> splitList = new ArrayList<String>();
        Character previousCh = null;
        for (char ch : charArray) {
            if (previousCh != null && !Character.isUpperCase(previousCh) && Character.isUpperCase(ch)) {
                splitList.add(sb.toString().toLowerCase()); // e.g. Land to land
                sb.delete(0, sb.length()); // clear all
            }
            sb.append(ch);
            previousCh = ch;
        }
        splitList.add(sb.toString().toLowerCase()); // e.g. Land to land
        return splitList;
    }

    protected void throwUrlPatternMethodKeywordMarkButIndexMethodException(Method executeMethod, String urlPattern, String keywordMark) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Found the method keyword mark for index() in urlPattern.");
        br.addItem("Advice");
        br.addElement("Cannot use method keyword mark for index().");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    @Execute(urlPattern=\"" + keywordMark + "/{}/" + keywordMark + "\") // *Bad");
        br.addElement("    public HtmlResponse index(int piary) {");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    @Execute(urlPattern=\"" + keywordMark + "/{}/" + keywordMark + "\") // Good: e.g. sea/3/land");
        br.addElement("    public HtmlResponse seaLand(int piary) {");
        br.addElement("    }");
        br.addItem("Execute Method");
        br.addElement(toSimpleMethodExp(executeMethod));
        br.addItem("URL Pattern");
        br.addElement(urlPattern);
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg);
    }

    protected void throwUrlPatternMethodKeywordMarkUnmatchedCountException(Method executeMethod, String urlPattern, String keywordMark) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Unmatched count of method keyword mark in urlPattern.");
        br.addItem("Advice");
        br.addElement("The count of method keyword mark in urlPattern");
        br.addElement("should match with method word count.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    @Execute(urlPattern=\"" + keywordMark + "/{}\") // *Bad");
        br.addElement("    public HtmlResponse seaLand(int piary) {");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    @Execute(urlPattern=\"" + keywordMark + "/{}/" + keywordMark + "\") // Good: e.g. sea/3/land");
        br.addElement("    public HtmlResponse seaLand(int piary) {");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    @Execute(urlPattern=\"{}/" + keywordMark + "\") // Good: e.g. 3/sea");
        br.addElement("    public HtmlResponse sea(int piary) {");
        br.addElement("    }");
        br.addItem("Execute Method");
        br.addElement(toSimpleMethodExp(executeMethod));
        br.addItem("URL Pattern");
        br.addElement(urlPattern);
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg);
    }

    // -----------------------------------------------------
    //                                         No urlPattern
    //                                         -------------
    protected UrlPatternChosenBox adjustUrlPatternByMethodNameWithoutParam(String methodName) {
        if (methodName.equals("index")) {
            return new UrlPatternChosenBox("", "", /*non-specified*/false); // empty if index to avoid '/index/' hit
        } else {
            return new UrlPatternChosenBox(methodName, "", /*non-specified*/false).withMethodPrefix();
        }
    }

    // ===================================================================================
    //                                                                           to Regexp
    //                                                                           =========
    public UrlPatternRegexpBox toRegexp(Method executeMethod, String urlPattern, List<Class<?>> pathParamTypeList,
            Map<Integer, Class<?>> optionalGenericTypeMap) {
        final StringBuilder sb = new StringBuilder(32);
        final char[] chars = urlPattern.toCharArray();
        final int length = chars.length;
        List<String> varList = null;
        int parameterIndex = -1;
        int index = -1;
        for (int i = 0; i < length; i++) {
            final char currentChar = chars[i];
            if (currentChar == '{') { // begin brace
                index = i;
            } else if (currentChar == '}') { // end brace
                assertBeginBraceExists(executeMethod, urlPattern, index, i);
                ++parameterIndex;
                setupParameterPattern(sb, pathParamTypeList, optionalGenericTypeMap, parameterIndex);
                final String elementName = urlPattern.substring(index + 1, i);
                assertNoNameParameter(executeMethod, urlPattern, elementName);
                if (varList == null) {
                    varList = new ArrayList<String>(4);
                }
                varList.add(buildParamName(executeMethod, urlPattern, varList, elementName));
                index = -1;
            } else if (index < 0) {
                sb.append(currentChar);
            }
        }
        assertEndBraceExists(executeMethod, urlPattern, index);
        return new UrlPatternRegexpBox(buildRegexpPattern(sb.toString()), varList);
    }

    protected void setupParameterPattern(StringBuilder sb, List<Class<?>> pathParamTypeList, Map<Integer, Class<?>> optionalGenericTypeMap,
            int parameterIndex) {
        if (needsNumberTypePattern(pathParamTypeList, optionalGenericTypeMap, parameterIndex)) {
            sb.append(ELEMENT_NUMBER_PATTERN); // to enable mapping index(Integer) and sea()
        } else {
            sb.append(ELEMENT_BASIC_PATTERN);
        }
    }

    // #hope jflute similer to PathParamArgs's one so want to refactor (2018/10/30)
    protected boolean needsNumberTypePattern(List<Class<?>> pathParamTypeList, Map<Integer, Class<?>> optionalGenericTypeMap,
            int parameterIndex) {
        if (pathParamTypeList.size() <= parameterIndex) {
            return false; // different count of parameters will be checked later so only avoid out-of-index here
        }
        final Class<?> parameterType = pathParamTypeList.get(parameterIndex);
        if (Number.class.isAssignableFrom(parameterType)) {
            return true;
        }
        if (parameterType.isPrimitive() && Stream.of(long.class, int.class, short.class, byte.class, double.class, float.class)
                .anyMatch(numType -> numType.isAssignableFrom(parameterType))) { // from pull request #55 (thanks!)
            return true;
        }
        final Class<?> genericType = optionalGenericTypeMap.get(parameterIndex);
        return genericType != null && Number.class.isAssignableFrom(genericType);
    }

    protected String buildParamName(Method executeMethod, String urlPattern, List<String> nameList, String elementName) {
        return "arg" + nameList.size(); // for internal management
    }

    protected Pattern buildRegexpPattern(String pattern) {
        return Pattern.compile("^" + escapeRegexpPattern(pattern) + "$");
    }

    protected String escapeRegexpPattern(String pattern) {
        // if e.g. get$sea$land(), the pattern is 'sea$land' so you can use '$' in URL if restful
        return Srl.replace(pattern, "$", "\\$");
    }

    public static class UrlPatternRegexpBox {

        protected final Pattern regexpPattern;
        protected final List<String> varList;

        public UrlPatternRegexpBox(Pattern regexpPattern, List<String> varList) {
            assertArgumentNotNull("regexpPattern", regexpPattern);
            this.regexpPattern = regexpPattern;
            this.varList = varList != null ? Collections.unmodifiableList(varList) : Collections.emptyList();
        }

        protected void assertArgumentNotNull(String variableName, Object value) {
            if (variableName == null) {
                throw new IllegalArgumentException("The variableName should not be null.");
            }
            if (value == null) {
                throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
            }
        }

        public Pattern getRegexpPattern() {
            return regexpPattern;
        }

        public List<String> getVarList() {
            return varList;
        }
    }

    // -----------------------------------------------------
    //                                    Assert Begin Brace
    //                                    ------------------
    protected void assertBeginBraceExists(Method executeMethod, String urlPattern, int beginIndex, int endIndex) {
        if (beginIndex < 0) {
            throwUrlPatternBeginBraceNotFoundException(executeMethod, urlPattern, beginIndex, endIndex);
        }
    }

    protected void throwUrlPatternBeginBraceNotFoundException(Method executeMethod, String urlPattern, int beginIndex, int endIndex) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found begin brace for the end brace in the urlPattern.");
        br.addItem("Advice");
        br.addElement("Make sure your urlPattern's braces.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    @Execute(urlPattern = \"pageNumber}\")  // *Bad");
        br.addElement("    public HtmlResponse index(...) {");
        br.addElement("        ...");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    @Execute(urlPattern = \"{pageNumber}\")  // Good");
        br.addElement("    public HtmlResponse index(...) {");
        br.addElement("        ...");
        br.addElement("    }");
        br.addItem("Execute Method");
        br.addElement(toSimpleMethodExp(executeMethod));
        br.addItem("URL Pattern");
        br.addElement(urlPattern);
        br.addItem("End Brace Index");
        br.addElement(endIndex);
        final String msg = br.buildExceptionMessage();
        throw new UrlPatternBeginBraceNotFoundException(msg);
    }

    // -----------------------------------------------------
    //                               Assert NoName Parameter
    //                               -----------------------
    protected void assertNoNameParameter(Method executeMethod, String urlPattern, String elementName) {
        if (!elementName.isEmpty()) {
            throwUrlPatternNamedParameterUnsupportedException(executeMethod, urlPattern, elementName);
        }
    }

    protected void throwUrlPatternNamedParameterUnsupportedException(Method executeMethod, String urlPattern, String elementName) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Named parameter is unsupported in URL pattern.");
        br.addItem("Advice");
        br.addElement("You can use only no-name '{}' parameter in your urlPattern.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    @Execute(urlPattern = \"{sea}/land/{ikspiary}\") // *Bad");
        br.addElement("    public HtmlResponse index(Integer pageNumber, String keyword) {");
        br.addElement("  (o):");
        br.addElement("    @Execute(urlPattern = \"{}/land/{}\") // Good");
        br.addElement("    public HtmlResponse index(Integer pageNumber, String keyword) {");
        br.addElement("  (x):");
        br.addElement("    @Execute(urlPattern = \"{pageNumber}\") // *Bad");
        br.addElement("    public HtmlResponse index(Integer pageNumber) {");
        br.addElement("  (o):");
        br.addElement("    @Execute // Good: you can abbreviate if simple pattern");
        br.addElement("    public HtmlResponse index(Integer pageNumber) {");
        br.addElement("  (o):");
        br.addElement("    @Execute(urlPattern = \"{+}\") // Good: is optional mark");
        br.addElement("    public HtmlResponse index(Integer pageNumber) {");
        br.addItem("Execute Method");
        br.addElement(toSimpleMethodExp(executeMethod));
        br.addItem("URL Pattern");
        br.addElement(urlPattern);
        br.addItem("Named Parameter");
        br.addElement(elementName);
        final String msg = br.buildExceptionMessage();
        throw new UrlPatternBeginBraceNotFoundException(msg);
    }

    // -----------------------------------------------------
    //                                      Assert End Brace
    //                                      ----------------
    protected void assertEndBraceExists(Method executeMethod, String urlPattern, int index) {
        if (index >= 0) {
            throwUrlPatternEndBraceNotFoundException(executeMethod, urlPattern, index);
        }
    }

    protected void throwUrlPatternEndBraceNotFoundException(Method executeMethod, String urlPattern, int index) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found end brace for the begin brace in the urlPattern.");
        br.addItem("Advice");
        br.addElement("Make sure your urlPattern's braces.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    @Execute(urlPattern = \"{pageNumber\")  // *Bad");
        br.addElement("    public HtmlResponse index(...) {");
        br.addElement("        ...");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    @Execute(urlPattern = \"{pageNumber}\")  // Good");
        br.addElement("    public HtmlResponse index(...) {");
        br.addElement("        ...");
        br.addElement("    }");
        br.addItem("Execute Method");
        br.addElement(toSimpleMethodExp(executeMethod));
        br.addItem("URL Pattern");
        br.addElement(urlPattern);
        br.addItem("Begin Brace Index");
        br.addElement(index);
        final String msg = br.buildExceptionMessage();
        throw new UrlPatternEndBraceNotFoundException(msg);
    }

    // ===================================================================================
    //                                                                Check Variable Count
    //                                                                ====================
    public void checkUrlPatternVariableCount(Method executeMethod, List<String> urlPatternVarList, List<Class<?>> pathParamTypeList) {
        if (urlPatternVarList.size() != pathParamTypeList.size()) {
            throwActionPathParameterDifferentArgsException(executeMethod, urlPatternVarList, pathParamTypeList);
        }
    }

    protected void throwActionPathParameterDifferentArgsException(Method executeMethod, List<String> urlPatternVarList,
            List<Class<?>> pathParamTypeList) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Different number of argument for path parameter.");
        br.addItem("Advice");
        br.addElement("Make sure your urlPattern or arguments.");
        br.addElement("  (x):");
        br.addElement("    @Execute(\"{}/sea/{}\")");
        br.addElement("    public HtmlResponse index(Integer land) { // *Bad");
        br.addElement("  (o):");
        br.addElement("    @Execute(\"{}/sea/{}\")");
        br.addElement("    public HtmlResponse index(Integer land, String ikspiary) { // Good");
        br.addItem("Execute Method");
        br.addElement(toSimpleMethodExp(executeMethod));
        br.addItem("urlPattern Variable List");
        br.addElement(urlPatternVarList);
        br.addItem("Defined Argument List");
        br.addElement(pathParamTypeList);
        final String msg = br.buildExceptionMessage();
        throw new ActionPathParameterDifferentArgsException(msg);
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected String toSimpleMethodExp(Method executeMethod) {
        return LaActionExecuteUtil.buildSimpleMethodExp(executeMethod);
    }
}