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
package org.lastaflute.web.ruts.config.analyzer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.util.Srl;
import org.lastaflute.web.exception.ActionUrlParameterDifferentArgsException;
import org.lastaflute.web.exception.UrlPatternBeginBraceNotFoundException;
import org.lastaflute.web.exception.UrlPatternEndBraceNotFoundException;
import org.lastaflute.web.exception.UrlPatternNonsenseSettingException;
import org.lastaflute.web.util.LaActionExecuteUtil;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class UrlPatternAnalyzer {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    public static final String ELEMENT_PATTERN = "([^/]+)";
    public static final String METHOD_KEYWORD_MARK = "@word";

    // ===================================================================================
    //                                                                     Check Specified
    //                                                                     ===============
    public void checkSpecifiedUrlPattern(Method executeMethod, String specifiedUrlPattern) {
        if (specifiedUrlPattern != null && canBeAbbreviatedUrlPattern(specifiedUrlPattern)) {
            throwUrlPatternNonsenseSettingException(executeMethod, specifiedUrlPattern);
        }
    }

    protected boolean canBeAbbreviatedUrlPattern(String str) { // format check so simple logic
        return Srl.equalsPlain(str, "{}", "{}/{}", "{}/{}/{}", "{}/{}/{}/{}", "{}/{}/{}/{}/{}", "{}/{}/{}/{}/{}/{}");
    }

    protected void throwUrlPatternNonsenseSettingException(Method executeMethod, String specifiedUrlPattern) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The urlPattern was non-sense.");
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

    // ===================================================================================
    //                                                                              Choose
    //                                                                              ======
    public String chooseUrlPattern(Method executeMethod, String specifiedUrlPattern, List<Class<?>> urlParamTypeList) {
        final String methodName = executeMethod.getName();
        if (specifiedUrlPattern != null && !specifiedUrlPattern.isEmpty()) { // e.g. urlPattern="{}"
            return adjustUrlPatternMethodPrefix(executeMethod, specifiedUrlPattern, methodName);
        } else { // urlPattern=[no definition]
            if (!urlParamTypeList.isEmpty()) { // e.g. sea(int pageNumber)
                final String derivedUrlPattern = buildDerivedUrlPattern(urlParamTypeList);
                return adjustUrlPatternMethodPrefix(executeMethod, derivedUrlPattern, methodName);
            } else { // e.g. index(), sea() *no parameter
                return adjustUrlPatternByMethodNameWithoutParam(methodName);
            }
        }
    }

    protected String buildDerivedUrlPattern(List<Class<?>> urlParamTypeList) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < urlParamTypeList.size(); i++) {
            sb.append(i > 0 ? "/" : "").append("{}");
        }
        return sb.toString();
    }

    // -----------------------------------------------------
    //                                        has urlPattern
    //                                        --------------
    protected String adjustUrlPatternMethodPrefix(Method executeMethod, String specifiedUrlPattern, String methodName) {
        final String keywordMark = getMethodKeywordMark();
        if (methodName.equals("index")) { // e.g. index(pageNumber), urlPattern="{}"
            if (specifiedUrlPattern.contains(keywordMark)) {
                throwUrlPatternMethodKeywordMarkButIndexMethodException(executeMethod, specifiedUrlPattern, keywordMark);
            }
            return specifiedUrlPattern;
        } else { // e.g. sea(pageNumber), urlPattern="{}"
            if (specifiedUrlPattern.contains(keywordMark)) { // e.g. @word/{}/@word
                final List<String> keywordList = splitMethodKeywordList(methodName);
                if (keywordList.size() != Srl.count(specifiedUrlPattern, keywordMark)) { // e.g. sea() but @word/{}/@word
                    throwUrlPatternMethodKeywordMarkUnmatchedCountException(executeMethod, specifiedUrlPattern, keywordMark);
                }
                return keywordList.stream().reduce(specifiedUrlPattern, (first, second) -> {
                    return Srl.substringFirstFront(first, keywordMark) + second + Srl.substringFirstRear(first, keywordMark);
                }); // e.g. sea/land
            } else {
                return methodName + "/" + specifiedUrlPattern;
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
                splitList.add(sb.toString().toLowerCase());
                sb.delete(0, sb.length()); // clear all
            }
            sb.append(ch);
            previousCh = ch;
        }
        splitList.add(sb.toString());
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
    protected String adjustUrlPatternByMethodNameWithoutParam(String methodName) {
        return !methodName.equals("index") ? methodName : ""; // to avoid '/index/' hit
    }

    // ===================================================================================
    //                                                                             Analyze
    //                                                                             =======
    /**
     * @param executeMethod The execute method basically for debug message. (NotNull)
     * @param urlPattern The URL pattern to be analyzed. (NotNull)
     * @param box The box of URL parameter to accept additional info. (NotNull)
     * @return The analyzed URL pattern. (NotNull)
     */
    public String analyzeUrlPattern(Method executeMethod, String urlPattern, UrlPatternBox box) {
        final StringBuilder sb = new StringBuilder(32);
        final char[] chars = urlPattern.toCharArray();
        final int length = chars.length;
        List<String> varList = null;
        int index = -1;
        for (int i = 0; i < length; i++) {
            final char currentChar = chars[i];
            if (currentChar == '{') { // begin brace
                index = i;
            } else if (currentChar == '}') { // end brace
                assertBeginBraceExists(executeMethod, urlPattern, index, i);
                sb.append(ELEMENT_PATTERN);
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
        box.setUrlPatternVarList(varList != null ? Collections.unmodifiableList(varList) : Collections.emptyList());
        return sb.toString();
    }

    protected String buildParamName(Method executeMethod, String urlPattern, List<String> nameList, String elementName) {
        return "arg" + nameList.size(); // for internal management
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
        br.addElement("    public HtmlResponse index(int pageNumber, String keyword) {");
        br.addElement("  (o):");
        br.addElement("    @Execute(urlPattern = \"{}/land/{}\") // Good");
        br.addElement("    public HtmlResponse index(int pageNumber, String keyword) {");
        br.addElement("  (x):");
        br.addElement("    @Execute(urlPattern = \"{pageNumber}\") // *Bad");
        br.addElement("    public HtmlResponse index(int pageNumber) {");
        br.addElement("  (o):");
        br.addElement("    @Execute // Good: you can abbreviate if simple pattern");
        br.addElement("    public HtmlResponse index(int pageNumber) {");
        br.addElement("  (o):");
        br.addElement("    @Execute(urlPattern = \"{+}\") // Good: is optional mark");
        br.addElement("    public HtmlResponse index(int pageNumber) {");
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
    //                                                            Build Regular Expression
    //                                                            ========================
    public Pattern buildUrlPatternRegexp(String pattern) {
        return Pattern.compile("^" + pattern + "$");
    }

    // ===================================================================================
    //                                                                Check Variable Count
    //                                                                ====================
    public void checkUrlPatternVariableCount(Method executeMethod, List<String> urlPatternVarList, List<Class<?>> urlParamTypeList) {
        if (urlPatternVarList.size() != urlParamTypeList.size()) {
            throwActionUrlParameterDifferentArgsException(executeMethod, urlPatternVarList, urlParamTypeList);
        }
    }

    protected void throwActionUrlParameterDifferentArgsException(Method executeMethod, List<String> urlPatternVarList,
            List<Class<?>> urlParamTypeList) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Different number of argument for URL parameter.");
        br.addItem("Advice");
        br.addElement("Make sure your urlPattern or arguments.");
        br.addElement("  (x):");
        br.addElement("    @Execute(\"{}/sea/{}\")");
        br.addElement("    public HtmlResponse index(int land) { // *Bad");
        br.addElement("  (o):");
        br.addElement("    @Execute(\"{}/sea/{}\")");
        br.addElement("    public HtmlResponse index(int land, String ikspiary) { // Good");
        br.addItem("Execute Method");
        br.addElement(toSimpleMethodExp(executeMethod));
        br.addItem("urlPattern Variable List");
        br.addElement(urlPatternVarList);
        br.addItem("Defined Argument List");
        br.addElement(urlParamTypeList);
        final String msg = br.buildExceptionMessage();
        throw new ActionUrlParameterDifferentArgsException(msg);
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    public String toSimpleMethodExp(Method executeMethod) {
        return LaActionExecuteUtil.buildSimpleMethodExp(executeMethod);
    }

    // ===================================================================================
    //                                                                     URL Pattern Box
    //                                                                     ===============
    public static class UrlPatternBox {

        protected List<String> urlPatternVarList;

        public List<String> getUrlPatternVarList() {
            return urlPatternVarList;
        }

        public void setUrlPatternVarList(List<String> urlPatternVarList) {
            this.urlPatternVarList = urlPatternVarList;
        }
    }
}