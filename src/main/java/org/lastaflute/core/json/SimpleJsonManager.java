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
package org.lastaflute.core.json;

import java.lang.reflect.ParameterizedType;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.Srl;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.direction.FwCoreDirection;
import org.lastaflute.core.json.bind.JsonYourCollectionResource;
import org.lastaflute.core.json.engine.GsonJsonEngine;
import org.lastaflute.core.json.engine.RealJsonEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

/**
 * @author jflute
 * @author awaawa
 */
public class SimpleJsonManager implements JsonManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(SimpleJsonManager.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant director (AD) for framework. (NotNull: after initialization) */
    @Resource
    private FwAssistantDirector assistantDirector;

    /** Is development here? */
    protected boolean developmentHere;

    /** Is null property suppressed (not displayed) in output JSON string? */
    protected boolean nullsSuppressed;

    /** Is pretty print suppressed (not line separating) in output JSON string? */
    protected boolean prettyPrintSuppressed;

    /** The option of JSON mapping. (NotNull, EmptyAllowed: if empty, use default) */
    protected OptionalThing<JsonMappingOption> jsonMappingOption = OptionalThing.empty();

    /** The real parser of JSON. (NotNull: after initialization) */
    protected RealJsonEngine realJsonParser;

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    /**
     * Initialize this component. <br>
     * This is basically called by DI setting file.
     */
    @PostConstruct
    public synchronized void initialize() {
        final FwCoreDirection direction = assistCoreDirection();
        developmentHere = direction.isDevelopmentHere();
        final JsonResourceProvider provider = direction.assistJsonResourceProvider();
        nullsSuppressed = provider != null ? provider.isNullsSuppressed() : false;
        prettyPrintSuppressed = provider != null ? provider.isPrettyPrintSuppressed() : false;
        jsonMappingOption = OptionalThing.ofNullable(extractMappingOption(provider), () -> {
            throw new IllegalStateException("Not found the JSON mapping option.");
        });
        reflectCompatibleYourCollections(provider);

        // should be last because of using other instance variable
        final RealJsonEngine provided = provider != null ? provider.swtichJsonEngine() : null;
        realJsonParser = provided != null ? provided : createDefaultJsonParser();
        showBootLogging();
    }

    protected FwCoreDirection assistCoreDirection() {
        return assistantDirector.assistCoreDirection();
    }

    protected JsonMappingOption extractMappingOption(JsonResourceProvider provider) {
        if (provider == null) {
            return null;
        }
        @SuppressWarnings("deprecation")
        final JsonMappingOption providedOption = provider.provideOption();
        if (providedOption != null) {
            return providedOption;
        } else {
            return provider.provideMappingOption();
        }
    }

    protected void reflectCompatibleYourCollections(JsonResourceProvider provider) {
        @SuppressWarnings("deprecation")
        final List<JsonYourCollectionResource> yourCollections = provider != null ? provider.provideYourCollections() : null;
        if (yourCollections != null) {
            jsonMappingOption.ifPresent(op -> op.yourCollections(yourCollections)).orElse(() -> {
                jsonMappingOption = OptionalThing.of(new JsonMappingOption().yourCollections(yourCollections));
            });
        }
    }

    protected RealJsonEngine createDefaultJsonParser() {
        return createGsonJsonEngine(jsonMappingOption);
    }

    protected void showBootLogging() {
        if (logger.isInfoEnabled()) {
            logger.info("[JSON Manager]");
            logger.info(" realJsonParser: " + DfTypeUtil.toClassTitle(realJsonParser));
            final String adjustment = buildAdjustmentExp();
            if (!adjustment.isEmpty()) {
                logger.info(" adjustment: " + adjustment);
            }
            if (jsonMappingOption.isPresent()) { // not use lambda to keep log indent
                logger.info(" option: " + jsonMappingOption.get());
            }
        }
    }

    protected String buildAdjustmentExp() {
        final StringBuilder sb = new StringBuilder();
        final String delimiter = ", ";
        if (nullsSuppressed) {
            sb.append(delimiter).append("nullsSuppressed");
        }
        if (prettyPrintSuppressed) {
            sb.append(delimiter).append("prettyPrintSuppressed");
        }
        final String adjustment = Srl.ltrim(sb.toString(), delimiter);
        return adjustment;
    }

    // ===================================================================================
    //                                                                               Gson
    //                                                                              ======
    protected RealJsonEngine createGsonJsonEngine(OptionalThing<JsonMappingOption> mappingOption) {
        final boolean serializeNulls = !nullsSuppressed;
        final boolean prettyPrinting = !prettyPrintSuppressed && developmentHere;
        return new GsonJsonEngine(builder -> {
            setupSerializeNullsSettings(builder, serializeNulls);
            setupPrettyPrintingSettings(builder, prettyPrinting);
        }, op -> {
            mappingOption.ifPresent(another -> op.acceptAnother(another));
        });
    }

    protected void setupSerializeNullsSettings(GsonBuilder builder, boolean serializeNulls) {
        if (serializeNulls) {
            builder.serializeNulls();
        }
    }

    protected void setupPrettyPrintingSettings(GsonBuilder builder, boolean prettyPrinting) {
        if (prettyPrinting) {
            builder.setPrettyPrinting();
        }
    }

    // ===================================================================================
    //                                                                              JSONIC
    //                                                                              ======
    // memorable code for JSONIC
    //protected static final Class<?> JSONIC_DUMMY_TYPE = new Object() {
    //}.getClass();
    //
    //protected RealJsonParser newJSonicRealJsonParser() {
    //    return new RealJsonParser() {
    //        @Override
    //        public String encode(Object bean) {
    //            return JSON.encode(bean, developmentHere);
    //        }
    //
    //        @Override
    //        public <BEAN> BEAN decode(String json, Class<BEAN> beanType) {
    //            return JSON.decode(json, beanType);
    //        }
    //
    //        @Override
    //        public <BEAN> List<BEAN> decodeList(String json, Class<BEAN> beanType) {
    //            return JSON.decode(json, new TypeReference<List<BEAN>>() {
    //                // nothing is overridden
    //            });
    //        }
    //
    //        @SuppressWarnings("unchecked")
    //        @Override
    //        public <BEAN> BEAN mappingJsonTo(String json, Supplier<BEAN> beanSupplier) {
    //            BEAN bean = beanSupplier.get();
    //            return (BEAN) new JSON() {
    //                @Override
    //                protected <T> T create(Context context, Class<? extends T> beanType) throws Exception {
    //                    if (context.getDepth() == 0) {
    //                        return (T) bean; // first bean instance is provided, basically for action form
    //                    }
    //                    return super.create(context, beanType);
    //                }
    //            }.parse(json, bean.getClass());
    //        }
    //
    //        @Override
    //        public <BEAN> List<BEAN> mappingJsonToList(String json, Supplier<BEAN> beanSupplier) {
    //            return new JSON() {
    //                private Boolean asList;
    //
    //                @SuppressWarnings("unchecked")
    //                @Override
    //                protected <T> T create(Context context, Class<? extends T> beanType) throws Exception {
    //                    if (asList != null && context.getDepth() == 0 && List.class.isAssignableFrom(beanType)) {
    //                        asList = true;
    //                    }
    //                    if (asList != null && asList && context.getDepth() == 1) {
    //                        return (T) beanSupplier.get(); // element bean instance is provided, basically for action form
    //                    }
    //                    return super.create(context, beanType);
    //                }
    //            }.parse(json, new TypeReference<List<BEAN>>() {
    //                // nothing is overridden
    //            });
    //        }
    //    };
    //}

    // ===================================================================================
    //                                                                        from/to JSON
    //                                                                        ============
    @Override
    public <BEAN> BEAN fromJson(String json, Class<BEAN> beanType) {
        assertArgumentNotNull("json", json);
        assertArgumentNotNull("beanType", beanType);
        return realJsonParser.fromJson(json, beanType);
    }

    @Override
    public <BEAN> BEAN fromJsonParameteried(String json, ParameterizedType parameterizedType) {
        assertArgumentNotNull("json", json);
        assertArgumentNotNull("parameterizedType", parameterizedType);
        return realJsonParser.fromJsonParameteried(json, parameterizedType);
    }

    @Override
    public String toJson(Object bean) {
        assertArgumentNotNull("bean", bean);
        return realJsonParser.toJson(bean);
    }

    // ===================================================================================
    //                                                                        Another Rule
    //                                                                        ============
    @Override
    public RealJsonEngine newAnotherEngine(OptionalThing<JsonMappingOption> mappingOption) {
        assertArgumentNotNull("mappingOption", mappingOption);
        return createGsonJsonEngine(mappingOption);
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

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public OptionalThing<JsonMappingOption> getJsonMappingOption() { // for e.g. LastaDoc
        return jsonMappingOption;
    }
}