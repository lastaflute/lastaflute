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
package org.lastaflute.core.template;

import java.io.InputStream;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.dbflute.helper.filesystem.FileTextIO;
import org.dbflute.twowaysql.SqlAnalyzer;
import org.dbflute.twowaysql.context.CommandContext;
import org.dbflute.twowaysql.context.CommandContextCreator;
import org.dbflute.twowaysql.node.Node;
import org.dbflute.twowaysql.pmbean.SimpleMapPmb;
import org.dbflute.util.DfResourceUtil;

/**
 * @author jflute
 * @since 0.6.0 (2015/05/23 Saturday)
 */
public class SimpleTemplateManager implements TemplateManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========s
    protected static final CommandContextCreator contextCreator;
    static {
        final String[] argNames = new String[] { "pmb" };
        final Class<?>[] argTypes = new Class<?>[] { SimpleMapPmb.class };
        contextCreator = new CommandContextCreator(argNames, argTypes);
    }
    protected static final FileTextIO textIO = new FileTextIO().encodeAsUTF8().removeUTF8Bom();

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
        return evaluate(readText(templatePath), pmb);
    }

    @Override
    public String parse(String templatePath, Map<String, Object> variableMap) {
        assertArgumentNotNull("templatePath", templatePath);
        assertArgumentNotNull("variableMap", variableMap);
        return evaluate(readText(templatePath), variableMap);
    }

    protected String readText(String templatePath) {
        final InputStream ins = DfResourceUtil.getResourceStream(templatePath);
        if (ins == null) {
            throw new IllegalStateException("Not found the template path: " + templatePath);
        }
        return textIO.read(ins);
    }

    // ===================================================================================
    //                                                                            Evaluate
    //                                                                            ========
    // TODO jflute lastaflute: [B] adjustment of line separator
    // very similar to pm-comment proofreader of MailFlute but no recycle to be independent
    protected String evaluate(String templateText, Object pmb) {
        final Node node = analyze(templateText);
        final CommandContext ctx = prepareContext(pmb);
        node.accept(ctx);
        return ctx.getSql();
    }

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
    //                                                                       Assist Helper
    //                                                                       =============
    protected void assertArgumentNotNull(String variableName, Object value) {
        if (variableName == null) {
            throw new IllegalArgumentException("The variableName should not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("The argument '" + variableName + "' should not be null.");
        }
    }
}
