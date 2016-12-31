/*
 * Copyright 2015-2017 the original author or authors.
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
package org.lastaflute.core.template;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.dbflute.helper.filesystem.FileTextIO;
import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.twowaysql.SqlAnalyzer;
import org.dbflute.twowaysql.context.CommandContext;
import org.dbflute.twowaysql.context.CommandContextCreator;
import org.dbflute.twowaysql.node.Node;
import org.dbflute.twowaysql.pmbean.SimpleMapPmb;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfResourceUtil;
import org.dbflute.util.Srl;
import org.dbflute.util.Srl.ScopeInfo;
import org.lastaflute.core.template.exception.TemplateFileParseFailureException;

/**
 * @author jflute
 * @since 0.6.0 (2015/05/23 Saturday)
 */
public class SimpleTemplateManager implements TemplateManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final String IF_PREFIX = "/*IF ";
    protected static final String FOR_PREFIX = "/*FOR ";
    protected static final String END_COMMENT = "/*END*/";
    protected static final String CLOSE_MARK = "*/";

    public static final String META_DELIMITER = ">>>";
    public static final String COMMENT_BEGIN = "/*";
    public static final String COMMENT_END = "*/";
    public static final String TITLE_BEGIN = "[";
    public static final String TITLE_END = "]";
    public static final String OPTION_LABEL = "option:";
    public static final String PROPDEF_PREFIX = "-- !!";
    public static final Set<String> optionSet;

    static {
        optionSet = Collections.unmodifiableSet(DfCollectionUtil.newLinkedHashSet("genAsIs"));
    }

    public static final List<String> allowedPrefixList; // except first line (comment)

    static {
        allowedPrefixList = Arrays.asList(OPTION_LABEL, PROPDEF_PREFIX);
    }

    protected static final String LF = "\n";
    protected static final String CR = "\r";
    protected static final String CRLF = "\r\n";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final FileTextIO textIO = createFileTextIO();

    protected FileTextIO createFileTextIO() {
        return new FileTextIO().encodeAsUTF8().removeUTF8Bom().replaceCrLfToLf();
    }

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    /**
     * Initialize this component. <br>
     * This is basically called by DI setting file.
     */
    @PostConstruct
    public synchronized void initialize() {
        // empty for now
    }

    // ===================================================================================
    //                                                                               Parse
    //                                                                               =====
    @Override
    public String parse(TemplatePmb pmb) {
        assertArgumentNotNull("pmb", pmb);
        final String templatePath = pmb.getTemplatePath();
        assertArgumentNotNull("pmb.getTemplatePath()", templatePath);
        final String evaluated = evaluate(readText(templatePath), pmb);
        return filterBodyMeta(templatePath, evaluated);
    }

    @Override
    public String parse(String templatePath, Map<String, Object> variableMap) {
        assertArgumentNotNull("templatePath", templatePath);
        assertArgumentNotNull("variableMap", variableMap);
        final String evaluated = evaluate(readText(templatePath), variableMap);
        return filterBodyMeta(templatePath, evaluated);
    }

    protected String readText(String templatePath) {
        final InputStream ins = DfResourceUtil.getResourceStream(templatePath);
        if (ins == null) {
            throw new IllegalStateException("Not found the template path: " + templatePath);
        }
        return textIO.read(ins);
    }

    protected String filterBodyMeta(String templatePath, String evaluated) {
        if (evaluated == null) {
            throw new IllegalStateException("Not found the evaluated text: " + templatePath);
        }
        final String delimiter = META_DELIMITER;
        if (evaluated.contains(delimiter)) {
            verifyFormat(templatePath, evaluated, delimiter);
            final String rear = Srl.substringFirstRear(evaluated, delimiter);
            final String realText;
            if (rear.startsWith(LF)) {
                realText = rear.substring(LF.length());
            } else if (rear.startsWith(CRLF)) {
                realText = rear.substring(CRLF.length());
            } else { // e.g. >>> Hello
                realText = rear;
            }
            return realText;
        } else { // no delimiter
            throwTemplateMetaNotFoundException(templatePath, evaluated);
            return null; // unreachable
        }
    }

    // ===================================================================================
    //                                                                            Evaluate
    //                                                                            ========
    // very similar to pm-comment proofreader of MailFlute but no recycle to be independent
    // -----------------------------------------------------
    //                                              Evaluate
    //                                              --------
    protected String evaluate(String templateText, Object pmb) {
        final Node node = analyze(filterTemplateText(templateText, pmb));
        final CommandContext ctx = prepareContext(pmb);
        node.accept(ctx);
        return ctx.getSql();
    }

    // -----------------------------------------------------
    //                                       Line Adjustment
    //                                       ---------------
    protected String filterTemplateText(String templateText, Object pmb) {
        final String replaced = Srl.replace(templateText, CRLF, LF);
        final List<String> lineList = Srl.splitList(replaced, LF);
        final StringBuilder sb = new StringBuilder(templateText.length());
        boolean nextNoLine = false;
        int lineNumber = 0;
        for (String line : lineList) {
            ++lineNumber;
            if (nextNoLine) {
                sb.append(line);
                nextNoLine = false;
                continue;
            }
            if (isIfEndCommentLine(line) || isForEndCommentLine(line)) {
                appendLfLine(sb, lineNumber, Srl.substringLastFront(line, END_COMMENT));
                sb.append(LF).append(END_COMMENT);
                nextNoLine = true;
                continue;
            }
            final String realLine;
            if (isOnlyIfCommentLine(line) || isOnlyForCommentLine(line) || isOnlyEndCommentLine(line)) {
                nextNoLine = true;
                realLine = Srl.ltrim(line);
            } else {
                realLine = line;
            }
            appendLfLine(sb, lineNumber, realLine);
        }
        return sb.toString();
    }

    protected boolean isOnlyIfCommentLine(String line) {
        final String trimmed = line.trim();
        return trimmed.startsWith(IF_PREFIX) && trimmed.endsWith(CLOSE_MARK) && Srl.count(line, CLOSE_MARK) == 1;
    }

    protected boolean isOnlyForCommentLine(String line) {
        final String trimmed = line.trim();
        return trimmed.startsWith(FOR_PREFIX) && trimmed.endsWith(CLOSE_MARK) && Srl.count(line, CLOSE_MARK) == 1;
    }

    protected boolean isOnlyEndCommentLine(String line) {
        return line.trim().equals(END_COMMENT);
    }

    protected boolean isIfEndCommentLine(String line) {
        return line.startsWith(IF_PREFIX) && line.endsWith(END_COMMENT) && Srl.count(line, CLOSE_MARK) > 1;
    }

    protected boolean isForEndCommentLine(String line) {
        return line.startsWith(FOR_PREFIX) && line.endsWith(END_COMMENT) && Srl.count(line, CLOSE_MARK) > 1;
    }

    protected void appendLfLine(final StringBuilder sb, int lineNumber, String line) {
        sb.append(lineNumber > 1 ? LF : "").append(line);
    }

    // -----------------------------------------------------
    //                                      Analyze Template
    //                                      ----------------
    protected Node analyze(String templateText) {
        return createSqlAnalyzer(templateText, true).analyze();
    }

    protected SqlAnalyzer createSqlAnalyzer(String templateText, boolean blockNullParameter) {
        final SqlAnalyzer analyzer = new SqlAnalyzer(templateText, blockNullParameter) {
            protected String filterAtFirst(String sql) {
                return sql; // keep body
            }
        }.overlookNativeBinding().switchBindingToReplaceOnlyEmbedded(); // adjust for plain template
        return analyzer;
    }

    protected CommandContext prepareContext(Object pmb) {
        final Object filteredPmb = filterPmb(pmb);
        final String[] argNames = new String[] { "pmb" };
        final Class<?>[] argTypes = new Class<?>[] { filteredPmb.getClass() };
        final CommandContextCreator creator = newCommandContextCreator(argNames, argTypes);
        return creator.createCommandContext(new Object[] { filteredPmb });
    }

    protected static CommandContextCreator newCommandContextCreator(String[] argNames, Class<?>[] argTypes) {
        return new CommandContextCreator(argNames, argTypes);
    }

    protected Object filterPmb(Object pmb) {
        if (pmb instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> variableMap = ((Map<String, Object>) pmb);
            final SimpleMapPmb<Object> mapPmb = new SimpleMapPmb<Object>();
            variableMap.forEach((key, value) -> mapPmb.addParameter(key, value));
            return mapPmb;
        } else {
            return pmb;
        }
    }

    // ===================================================================================
    //                                                                       Verify Format
    //                                                                       =============
    protected void verifyFormat(String templatePath, String evaluated, String delimiter) {
        final String meta = Srl.substringFirstFront(evaluated, delimiter);
        if (!meta.endsWith(LF)) { // also CRLF checked
            throwBodyMetaNoIndependentDelimiterException(templatePath, evaluated);
        }
        final int rearIndex = evaluated.indexOf(delimiter) + delimiter.length();
        if (evaluated.length() > rearIndex) { // just in case (empty template possible?)
            final String rearFirstStr = evaluated.substring(rearIndex, rearIndex + 1);
            if (!Srl.equalsPlain(rearFirstStr, LF, CR)) { // e.g. >>> Hello, ...
                throwBodyMetaNoIndependentDelimiterException(templatePath, evaluated);
            }
        }
        if (!meta.startsWith(COMMENT_BEGIN)) { // also leading spaces not allowed
            throwTemplateMetaNotStartWithHeaderCommentException(templatePath, evaluated, meta);
        }
        if (!meta.contains(COMMENT_END)) {
            throwBodyMetaHeaderCommentEndMarkNotFoundException(templatePath, evaluated, meta);
        }
        final String headerComment = Srl.extractScopeFirst(evaluated, COMMENT_BEGIN, COMMENT_END).getContent();
        final ScopeInfo titleScope = Srl.extractScopeFirst(headerComment, TITLE_BEGIN, TITLE_END);
        if (titleScope == null) {
            throwBodyMetaTitleCommentNotFoundException(templatePath, evaluated);
        }
        final String desc = Srl.substringFirstRear(headerComment, TITLE_END);
        if (desc.isEmpty()) {
            throwBodyMetaDescriptionCommentNotFoundException(templatePath, evaluated);
        }
        final String rearMeta = Srl.substringFirstRear(meta, COMMENT_END);
        // no way because of already checked
        //if (!rearMeta.contains(LF)) {
        //}
        final List<String> splitList = Srl.splitList(rearMeta, LF);
        if (!splitList.get(0).trim().isEmpty()) { // after '*/'
            throwBodyMetaHeaderCommentEndMarkNoIndependentException(templatePath, evaluated);
        }
        final int nextIndex = 1;
        if (splitList.size() > nextIndex) { // after header comment
            final List<String> nextList = splitList.subList(nextIndex, splitList.size());
            final int nextSize = nextList.size();
            int index = 0;
            for (String line : nextList) {
                if (index == nextSize - 1) { // last loop
                    if (line.isEmpty()) { // empty line only allowed in last loop
                        break;
                    }
                }
                if (!allowedPrefixList.stream().anyMatch(prefix -> line.startsWith(prefix))) {
                    throwBodyMetaUnknownLineException(templatePath, evaluated, line);
                }
                if (line.startsWith(OPTION_LABEL)) {
                    final String options = Srl.substringFirstRear(line, OPTION_LABEL);
                    final List<String> optionList = Srl.splitListTrimmed(options, ".");
                    for (String option : optionList) {
                        if (!optionSet.contains(option)) {
                            throwBodyMetaUnknownOptionException(templatePath, evaluated, option);
                        }
                    }
                }
                ++index;
            }
        }
    }

    protected void throwBodyMetaNoIndependentDelimiterException(String templatePath, String evaluated) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("No independent delimter of template meta.");
        br.addItem("Advice");
        br.addElement("The delimter of template meta should be independent in line.");
        br.addElement("For example:");
        br.addElement("  (x)");
        br.addElement("    /*");
        br.addElement("     [...your template's title]");
        br.addElement("     ...your template's description");
        br.addElement("    */ >>>                    // *Bad");
        br.addElement("    ...your template body");
        br.addElement("  (x)");
        br.addElement("    /*");
        br.addElement("     [...your template's title]");
        br.addElement("     ...your template's description");
        br.addElement("    */");
        br.addElement("    >>> ...your template body // *Bad");
        br.addElement("  (o)");
        br.addElement("    /*");
        br.addElement("     [...your template's title]");
        br.addElement("     ...your template's description");
        br.addElement("    */");
        br.addElement("    >>>                       // Good");
        br.addElement("    ...your template body");
        setupTemplateFileInfo(br, templatePath, evaluated);
        final String msg = br.buildExceptionMessage();
        throw new TemplateFileParseFailureException(msg);
    }

    protected void throwTemplateMetaNotStartWithHeaderCommentException(String templatePath, String evaluated, String meta) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not start with the header comment in the template meta.");
        br.addItem("Advice");
        br.addElement("The template meta should start with '/*' and should contain '*/'.");
        br.addElement("It means header comment of template file is required.");
        br.addElement("For example:");
        br.addElement("  (x)");
        br.addElement("    subject: ...              // *Bad");
        br.addElement("    >>>");
        br.addElement("    ...your template body");
        br.addElement("");
        br.addElement("  (o)");
        br.addElement("    /*                        // Good");
        br.addElement("     [...your template's title]");
        br.addElement("     ...your template's description");
        br.addElement("    */");
        br.addElement("    subject: ...");
        br.addElement("    >>>");
        br.addElement("    ...your template body");
        br.addElement("");
        br.addElement("And example:");
        br.addElement("  /*");
        br.addElement("   [New Member's Registration]");
        br.addElement("   The memebr will be formalized after click.");
        br.addElement("   And the ...");
        br.addElement("  */");
        br.addElement("  >>>");
        br.addElement("  Hello, sea");
        br.addElement("  ...");
        setupTemplateFileInfo(br, templatePath, evaluated);
        br.addItem("Body Meta");
        br.addElement(meta);
        final String msg = br.buildExceptionMessage();
        throw new TemplateFileParseFailureException(msg);
    }

    protected void throwBodyMetaHeaderCommentEndMarkNotFoundException(String templatePath, String evaluated, String meta) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the header comment end mark in the template meta.");
        br.addItem("Advice");
        br.addElement("The template meta should start with '/*' and should contain '*/'.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    /*");
        br.addElement("     ...");
        br.addElement("    >>>");
        br.addElement("    ...your template body");
        br.addElement("  (x):");
        br.addElement("    /*");
        br.addElement("     ...");
        br.addElement("    >>>");
        br.addElement("    */");
        br.addElement("    ...your template body");
        br.addElement("  (o):");
        br.addElement("    /*");
        br.addElement("     ...");
        br.addElement("    */              // Good");
        br.addElement("    >>>");
        br.addElement("    ...your template body");
        setupTemplateFileInfo(br, templatePath, evaluated);
        br.addItem("Body Meta");
        br.addElement(meta);
        final String msg = br.buildExceptionMessage();
        throw new TemplateFileParseFailureException(msg);
    }

    protected void throwBodyMetaTitleCommentNotFoundException(String templatePath, String evaluated) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the title in the header comment of template meta.");
        br.addItem("Advice");
        br.addElement("The template meta should contain TITLE in the header comment.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    /*");
        br.addElement("     ...your template's description     // *Bad");
        br.addElement("    */");
        br.addElement("    >>>");
        br.addElement("    ...your template body");
        br.addElement("  (o):");
        br.addElement("    /*");
        br.addElement("     [...your template's title]         // Good");
        br.addElement("     ...your template's description");
        br.addElement("    */");
        br.addElement("    >>>");
        br.addElement("    ...your template body");
        setupTemplateFileInfo(br, templatePath, evaluated);
        final String msg = br.buildExceptionMessage();
        throw new TemplateFileParseFailureException(msg);
    }

    protected void throwBodyMetaDescriptionCommentNotFoundException(String templatePath, String evaluated) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the description in the header comment of template meta.");
        br.addItem("Advice");
        br.addElement("The template meta should contain DESCRIPTION");
        br.addElement("in the header comment like this:");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    /*");
        br.addElement("     [...your template's title]");
        br.addElement("    */                                  // *Bad");
        br.addElement("    >>>");
        br.addElement("    ...your template body");
        br.addElement("  (o):");
        br.addElement("    /*");
        br.addElement("     [...your template's title]");
        br.addElement("     ...your template's description     // Good");
        br.addElement("    */");
        br.addElement("    >>>");
        br.addElement("    ...your template body");
        setupTemplateFileInfo(br, templatePath, evaluated);
        final String msg = br.buildExceptionMessage();
        throw new TemplateFileParseFailureException(msg);
    }

    protected void throwBodyMetaHeaderCommentEndMarkNoIndependentException(String templatePath, String evaluated) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("No independent the header comment end mark in the template meta.");
        br.addItem("Advice");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    /*");
        br.addElement("     [...your template's title]");
        br.addElement("     ...your template's description");
        br.addElement("    */ option: ...         // *Bad");
        br.addElement("    >>>");
        br.addElement("    ...your template body");
        br.addElement("  (o):");
        br.addElement("    /*");
        br.addElement("     [...your template's title]");
        br.addElement("     ...your template's description");
        br.addElement("    */");
        br.addElement("    option: ...            // Good");
        br.addElement("    >>>");
        br.addElement("    ...your template body");
        setupTemplateFileInfo(br, templatePath, evaluated);
        final String msg = br.buildExceptionMessage();
        throw new TemplateFileParseFailureException(msg);
    }

    protected void throwBodyMetaUnknownLineException(String templatePath, String evaluated, String line) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Unknown line in the template meta.");
        br.addItem("Advice");
        br.addElement("The template meta should start with option:");
        br.addElement("or fixed style, e.g. '-- !!...!!'");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    /*");
        br.addElement("     [...your template's title]");
        br.addElement("     ...your template's description");
        br.addElement("    */");
        br.addElement("    maihama     // *Bad: unknown meta definition");
        br.addElement("    >>>");
        br.addElement("    ...your template body");
        br.addElement("  (x):");
        br.addElement("    /*");
        br.addElement("     [...your template's title]");
        br.addElement("     ...your template's description");
        br.addElement("    */");
        br.addElement("                // *Bad: empty line not allowed");
        br.addElement("    >>>");
        br.addElement("    ...your template body");
        br.addElement("  (o):");
        br.addElement("    /*");
        br.addElement("     [...your template's title]");
        br.addElement("     ...your template's description");
        br.addElement("    */");
        br.addElement("    >>>");
        br.addElement("    ...your template body");
        br.addElement("  (o):");
        br.addElement("    /*");
        br.addElement("     [...your template's title]");
        br.addElement("     ...your template's description");
        br.addElement("    */");
        br.addElement("    -- !!String memberName!!");
        br.addElement("    >>>");
        br.addElement("    ...your template body");
        setupTemplateFileInfo(br, templatePath, evaluated);
        br.addItem("Unknown Line");
        br.addElement(line);
        final String msg = br.buildExceptionMessage();
        throw new TemplateFileParseFailureException(msg);
    }

    protected void throwBodyMetaUnknownOptionException(String bodyFile, String fileText, String option) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Unknown option for MailFlute body meta.");
        br.addItem("Advice");
        br.addElement("You can specify the following option:");
        br.addElement(optionSet);
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    /*");
        br.addElement("     [...your template's title]");
        br.addElement("     ...your template's description");
        br.addElement("    */");
        br.addElement("    option: maihama      // *Bad: unknown option");
        br.addElement("    >>>");
        br.addElement("    ...your template body");
        br.addElement("  (o):");
        br.addElement("    /*");
        br.addElement("     [...your template's title]");
        br.addElement("     ...your template's description");
        br.addElement("    */");
        br.addElement("    option: genAsIs      // Good");
        br.addElement("    >>>");
        br.addElement("    ...your template body");
        br.addItem("Body File");
        br.addElement(bodyFile);
        br.addItem("File Text");
        br.addElement(fileText);
        br.addItem("Unknown Option");
        br.addElement(option);
        final String msg = br.buildExceptionMessage();
        throw new TemplateFileParseFailureException(msg);
    }

    protected void throwTemplateMetaNotFoundException(String templatePath, String evaluated) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the delimiter for template meta.");
        br.addItem("Advice");
        br.addElement("The delimiter of template meta is '>>>'.");
        br.addElement("It should be defined.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    /*");
        br.addElement("     [...your template's title]");
        br.addElement("     ...your template's description");
        br.addElement("    */");
        br.addElement("    ...your template body    // *Bad");
        br.addElement("  (o):");
        br.addElement("    /*");
        br.addElement("     [...your template's title]");
        br.addElement("     ...your template's description");
        br.addElement("    */");
        br.addElement("    >>>                      // Good");
        br.addElement("    ...your template body");
        br.addElement("  (o):");
        br.addElement("    /*");
        br.addElement("     [...your template's title]");
        br.addElement("     ...your template's description");
        br.addElement("    */");
        br.addElement("    option: ...options");
        br.addElement("    -- !!String memberName!!");
        br.addElement("    >>>                      // Good");
        br.addElement("    ...your template body");
        setupTemplateFileInfo(br, templatePath, evaluated);
        final String msg = br.buildExceptionMessage();
        throw new TemplateFileParseFailureException(msg);
    }

    protected void setupTemplateFileInfo(ExceptionMessageBuilder br, String templatePath, String evaluated) {
        br.addItem("Template File");
        br.addElement(templatePath);
        br.addItem("Evaluated");
        br.addElement(evaluated);
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
