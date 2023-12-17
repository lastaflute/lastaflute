/*
 * Copyright 2015-2022 the original author or authors.
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
import java.util.function.Consumer;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.direction.FwCoreDirection;
import org.lastaflute.core.json.control.JsonControlMeta;
import org.lastaflute.core.json.control.JsonMappingControlMeta;
import org.lastaflute.core.json.control.JsonPrintControlMeta;
import org.lastaflute.core.json.control.JsonPrintControlState;
import org.lastaflute.core.json.engine.GsonJsonEngine;
import org.lastaflute.core.json.engine.RealJsonEngine;
import org.lastaflute.core.json.engine.YourJsonEngineCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

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

    /** The option of JSON mapping. (NotNull, EmptyAllowed: if empty, use default) */
    protected OptionalThing<JsonMappingOption> mappingOption = OptionalThing.empty();

    /** The control state of JSON print. (NotNull, EmptyAllowed: if empty, use default) */
    protected OptionalThing<JsonPrintControlState> printControlState = OptionalThing.empty();

    /** The your creator of JSON engine. (NotNull, EmptyAllowed: if empty, use default) */
    protected OptionalThing<YourJsonEngineCreator> yourEngineCreator = OptionalThing.empty();

    /** The real engine of JSON. (NotNull: after initialization) */
    protected RealJsonEngine realJsonEngine;

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
        mappingOption = OptionalThing.ofNullable(extractMappingOption(provider), () -> {
            throw new IllegalStateException("Not found the JSON mapping option.");
        });
        printControlState = OptionalThing.of(preparePrintState(provider)); // fixedly exists for now
        yourEngineCreator = OptionalThing.ofNullable(extractYourEngineCreator(provider), () -> {
            throw new IllegalStateException("Not found the your engine creator.");
        });

        // should be last because of using other instance variable
        realJsonEngine = createDefaultJsonEngine();
        showBootLogging();
    }

    protected FwCoreDirection assistCoreDirection() {
        return assistantDirector.assistCoreDirection();
    }

    // -----------------------------------------------------
    //                                        Mapping Option
    //                                        --------------
    protected JsonMappingOption extractMappingOption(JsonResourceProvider provider) {
        return provider != null ? provider.provideMappingOption() : null;
    }

    // -----------------------------------------------------
    //                                          Print Option
    //                                          ------------
    @SuppressWarnings("deprecation")
    protected JsonPrintControlState preparePrintState(JsonResourceProvider provider) {
        JsonPrintControlState printState = new JsonPrintControlState();
        printState.asNullsSuppressed(provider != null ? provider.isNullsSuppressed() : false);
        printState.asPrettyPrintSuppressed(provider != null ? provider.isPrettyPrintSuppressed() : false);
        return printState;
    }

    // -----------------------------------------------------
    //                                   Your Engine Creator
    //                                   -------------------
    protected YourJsonEngineCreator extractYourEngineCreator(JsonResourceProvider provider) {
        return provider != null ? provider.prepareYourEngineCreator() : null;
    }

    // -----------------------------------------------------
    //                                           Json Engine
    //                                           -----------
    protected RealJsonEngine createDefaultJsonEngine() {
        final JsonEngineResource resource = new JsonEngineResource();
        mappingOption.ifPresent(op -> {
            resource.acceptMappingOption(op);
        });
        yourEngineCreator.ifPresent(creator -> {
            resource.useYourEngineCreator(creator);
        });
        return createGsonJsonEngine(resource);
    }

    // -----------------------------------------------------
    //                                          Boot Logging
    //                                          ------------
    protected void showBootLogging() {
        if (logger.isInfoEnabled()) {
            logger.info("[JSON Manager]");
            logger.info(" realJsonParser: " + DfTypeUtil.toClassTitle(realJsonEngine));
            if (mappingOption.isPresent()) { // not use lambda to keep log indent
                logger.info(" mapping: " + mappingOption.get());
            }
            // print control cannot be changed (deprecated) so no show for now
            if (yourEngineCreator.isPresent()) { // me too
                logger.info(" creator: " + yourEngineCreator.get());
            }
        }
    }

    // ===================================================================================
    //                                                                        from/to JSON
    //                                                                        ============
    @Override
    public <BEAN> BEAN fromJson(String json, Class<BEAN> beanType) {
        assertArgumentNotNull("json", json);
        assertArgumentNotNull("beanType", beanType);
        return realJsonEngine.fromJson(json, beanType);
    }

    @Override
    public <BEAN> BEAN fromJsonParameteried(String json, ParameterizedType parameterizedType) {
        assertArgumentNotNull("json", json);
        assertArgumentNotNull("parameterizedType", parameterizedType);
        return realJsonEngine.fromJsonParameteried(json, parameterizedType);
    }

    @Override
    public String toJson(Object bean) {
        assertArgumentNotNull("bean", bean);
        return realJsonEngine.toJson(bean);
    }

    // ===================================================================================
    //                                                                    new Ruled Engine
    //                                                                    ================
    @Override
    public RealJsonEngine newAnotherEngine(OptionalThing<JsonMappingOption> mappingOption) {
        assertArgumentNotNull("mappingOption", mappingOption);
        final JsonEngineResource resource = new JsonEngineResource();
        mappingOption.ifPresent(op -> {
            resource.acceptMappingOption(op);
        });
        return newRuledEngine(resource);
    }

    @Override
    public RealJsonEngine newRuledEngine(JsonEngineResource resource) {
        assertArgumentNotNull("resource", resource);
        return createGsonJsonEngine(resource);
    }

    // ===================================================================================
    //                                                                        Control Meta
    //                                                                        ============
    @Override
    public JsonControlMeta pulloutControlMeta() {
        return new JsonControlMeta(getMappingControlMeta(), getPrintControlOption());
    }

    // ===================================================================================
    //                                                                               Gson
    //                                                                              ======
    // mappingOption is specified for another engine 
    protected GsonJsonEngine createGsonJsonEngine(JsonEngineResource resource) {
        final boolean serializeNulls = isGsonSerializeNulls();
        final boolean prettyPrinting = isGsonPrettyPrinting();
        final Consumer<GsonBuilder> builderSetupper = prepareGsonBuilderSetupper(serializeNulls, prettyPrinting);
        final Consumer<JsonMappingOption> optionSetupper = prepareMappingOptionSetupper(resource.getMappingOption());
        return resource.getYourEngineCreator().map(creator -> { // rare option
            return createYourEngine(creator, builderSetupper, optionSetupper);
        }).orElseGet(() -> { // mainly here
            return newGsonJsonEngine(builderSetupper, optionSetupper);
        });
    }

    protected boolean isGsonSerializeNulls() {
        return !isNullsSuppressed();
    }

    protected boolean isGsonPrettyPrinting() {
        return !isPrettyPrintSuppressed() && isDevelopmentHere(); // in development only
    }

    protected Consumer<GsonBuilder> prepareGsonBuilderSetupper(boolean serializeNulls, final boolean prettyPrinting) {
        return builder -> {
            setupSerializeNullsSettings(builder, serializeNulls);
            setupPrettyPrintingSettings(builder, prettyPrinting);
        };
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

    protected Consumer<JsonMappingOption> prepareMappingOptionSetupper(OptionalThing<JsonMappingOption> mappingOption) {
        return op -> {
            mappingOption.ifPresent(another -> op.acceptAnother(another));
        };
    }

    protected GsonJsonEngine createYourEngine(YourJsonEngineCreator creator, Consumer<GsonBuilder> builderSetupper,
            Consumer<JsonMappingOption> optionSetupper) {
        final GsonJsonEngine yourEngine = creator.create(builderSetupper, optionSetupper);
        if (yourEngine == null) {
            throw new IllegalStateException("Your engine creator should not return null: " + yourEngineCreator);
        }
        return yourEngine;
    }

    protected GsonJsonEngine newGsonJsonEngine(Consumer<GsonBuilder> builderSetupper, Consumer<JsonMappingOption> optionSetupper) {
        return new GsonJsonEngine(builderSetupper, optionSetupper);
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected boolean isNullsSuppressed() {
        return printControlState.map(op -> op.isNullsSuppressed()).orElse(false);
    }

    protected boolean isPrettyPrintSuppressed() {
        return printControlState.map(op -> op.isPrettyPrintSuppressed()).orElse(false);
    }

    protected boolean isDevelopmentHere() {
        return developmentHere;
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
    public OptionalThing<JsonMappingOption> getJsonMappingOption() { // for compatible, e.g. LastaDoc
        return mappingOption;
    }

    public OptionalThing<JsonMappingControlMeta> getMappingControlMeta() {
        return mappingOption.map(op -> op); // strange lambda here, only for cast to meta type
    }

    public OptionalThing<JsonPrintControlMeta> getPrintControlOption() {
        return printControlState.map(op -> op); // me too
    }
}