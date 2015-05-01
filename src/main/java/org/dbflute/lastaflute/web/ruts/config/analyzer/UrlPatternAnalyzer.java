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
package org.dbflute.lastaflute.web.ruts.config.analyzer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.lastaflute.web.exception.UrlPatternBeginBraceNotFoundException;
import org.dbflute.lastaflute.web.exception.UrlPatternEndBraceNotFoundException;
import org.dbflute.lastaflute.web.util.LaActionExecuteUtil;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class UrlPatternAnalyzer {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    public static final String ELEMENT_PATTERN = "([^/]+)";

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

    // ===================================================================================
    //                                                                  Assert Begin Brace
    //                                                                  ==================
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
        br.addElement("    @Execute(urlPattern = \"pageNumber}\")  // *NG");
        br.addElement("    public HtmlResponse index(...) {");
        br.addElement("        ...");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    @Execute(urlPattern = \"{pageNumber}\")  // OK");
        br.addElement("    public HtmlResponse index(...) {");
        br.addElement("        ...");
        br.addElement("    }");
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        br.addItem("URL Pattern");
        br.addElement(urlPattern);
        br.addItem("End Brace Index");
        br.addElement(endIndex);
        final String msg = br.buildExceptionMessage();
        throw new UrlPatternBeginBraceNotFoundException(msg);
    }

    // ===================================================================================
    //                                                             Assert NoName Parameter
    //                                                             =======================
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
        br.addElement("    @Execute(urlPattern = \"{sea}/land/{ikspiary}\") // *NG");
        br.addElement("    public HtmlResponse index(int pageNumber, String keyword) {");
        br.addElement("  (o):");
        br.addElement("    @Execute(urlPattern = \"{}/land/{}\") // OK");
        br.addElement("    public HtmlResponse index(int pageNumber, String keyword) {");
        br.addElement("  (x):");
        br.addElement("    @Execute(urlPattern = \"{pageNumber}\") // *NG");
        br.addElement("    public HtmlResponse index(int pageNumber) {");
        br.addElement("  (o):");
        br.addElement("    @Execute // OK: you can abbreviate if simple pattern");
        br.addElement("    public HtmlResponse index(int pageNumber) {");
        br.addElement("  (o):");
        br.addElement("    @Execute(urlPattern = \"{+}\") // OK: is optional mark");
        br.addElement("    public HtmlResponse index(int pageNumber) {");
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        br.addItem("URL Pattern");
        br.addElement(urlPattern);
        br.addItem("Named Parameter");
        br.addElement(elementName);
        final String msg = br.buildExceptionMessage();
        throw new UrlPatternBeginBraceNotFoundException(msg);
    }

    // ===================================================================================
    //                                                                    Assert End Brace
    //                                                                    ================
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
        br.addElement("    @Execute(urlPattern = \"{pageNumber\")  // *NG");
        br.addElement("    public HtmlResponse index(...) {");
        br.addElement("        ...");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    @Execute(urlPattern = \"{pageNumber}\")  // OK");
        br.addElement("    public HtmlResponse index(...) {");
        br.addElement("        ...");
        br.addElement("    }");
        br.addItem("Execute Method");
        br.addElement(LaActionExecuteUtil.buildSimpleMethodExp(executeMethod));
        br.addItem("URL Pattern");
        br.addElement(urlPattern);
        br.addItem("Begin Brace Index");
        br.addElement(index);
        final String msg = br.buildExceptionMessage();
        throw new UrlPatternEndBraceNotFoundException(msg);
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