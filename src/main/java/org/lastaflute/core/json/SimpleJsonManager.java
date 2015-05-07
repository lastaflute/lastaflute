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
package org.lastaflute.core.json;

import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.direction.OptionalCoreDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
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
    /** The assistant directory (AD) for framework. (NotNull: after initialization) */
    @Resource
    protected FwAssistantDirector assistantDirector;

    /** Is development here? */
    protected boolean developmentHere;

    /** Is null property suppressed (not displayed) in output JSON string? */
    protected boolean nullsSuppressed;

    /** The real parser of JSON. (NotNull: after initialization) */
    protected RealJsonParser realJsonParser;

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    /**
     * Initialize this component. <br>
     * This is basically called by DI setting file.
     */
    @PostConstruct
    public synchronized void initialize() {
        final OptionalCoreDirection direction = assistOptionalCoreDirection();
        developmentHere = direction.isDevelopmentHere();
        final JsonResourceProvider provider = direction.assistJsonResourceProvider();
        nullsSuppressed = provider != null ? provider.isNullsSuppressed() : false;
        final RealJsonParser provided = provider != null ? provider.provideJsonParser() : null;
        realJsonParser = provided != null ? provided : createDefaultJsonParser();
        showBootLogging();
    }

    protected OptionalCoreDirection assistOptionalCoreDirection() {
        return assistantDirector.assistOptionalCoreDirection();
    }

    protected RealJsonParser createDefaultJsonParser() {
        return newGsonRealJsonParser();
    }

    protected void showBootLogging() {
        if (logger.isInfoEnabled()) {
            logger.info("[JSON Manager]");
            logger.info(" realJsonParser: " + DfTypeUtil.toClassTitle(realJsonParser));
        }
    }

    // ===================================================================================
    //                                                                               GSON
    //                                                                              ======
    protected RealJsonParser newGsonRealJsonParser() {
        // TODO jflute lastaflute: [D] research: Gson thread safe?
        final GsonBuilder builder = new GsonBuilder();
        if (developmentHere) {
            builder.setPrettyPrinting();
        }
        if (!nullsSuppressed) {
            builder.serializeNulls();
        }
        final Gson gson = builder.create();
        return new RealJsonParser() {

            @SuppressWarnings("unchecked")
            @Override
            public <BEAN> BEAN fromJson(String json, Class<BEAN> beanType) { // are not null, already checked
                final BEAN bean = gson.fromJson(json, beanType); // if empty JSON, new-only instance
                return bean != null ? bean : (BEAN) DfReflectionUtil.newInstance(beanType);
            }

            @Override
            public <BEAN> List<BEAN> fromJsonList(String json, ParameterizedType elementType) { // are not null, already checked
                final List<BEAN> list = gson.fromJson(json, elementType); // if empty JSON, empty list
                return list != null ? Collections.unmodifiableList(list) : DfCollectionUtil.emptyList();
            }

            @Override
            public String toJson(Object bean) { // is not null, already checked
                return gson.toJson(bean);
            }
        };
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
    public <ELEMENT> List<ELEMENT> fromJsonList(String json, ParameterizedType elementType) {
        assertArgumentNotNull("json", json);
        assertArgumentNotNull("elementType", elementType);
        return realJsonParser.fromJsonList(json, elementType);
    }

    @Override
    public String toJson(Object bean) {
        assertArgumentNotNull("bean", bean);
        return realJsonParser.toJson(bean);
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