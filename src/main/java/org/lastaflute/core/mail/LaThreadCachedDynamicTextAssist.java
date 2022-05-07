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
package org.lastaflute.core.mail;

import java.util.Locale;

import org.dbflute.mail.send.embedded.receptionist.SMailDynamicDataResource;
import org.dbflute.mail.send.embedded.receptionist.SMailDynamicTextAssist;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.magic.ThreadCacheContext;

/**
 * @since 0.6.0 (2015/05/16 Saturday)
 */
public abstract class LaThreadCachedDynamicTextAssist implements SMailDynamicTextAssist {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final Object NONE = new Object();

    // ===================================================================================
    //                                                                        Dynamic Data
    //                                                                        ============
    @Override
    public OptionalThing<? extends Object> prepareDynamicData(SMailDynamicDataResource resource) {
        final boolean exists = ThreadCacheContext.exists();
        final String cacheKey = exists ? generateDataCacheKey(resource) : null;
        if (exists) {
            final Object cached = ThreadCacheContext.getObject(cacheKey);
            if (cached != null) {
                return OptionalThing.ofNullable(!cached.equals(NONE) ? cached : null, () -> {
                    throw new IllegalStateException("Not found the dynamic data: " + resource);
                });
            }
        }
        final OptionalThing<? extends Object> assisted = loadDynamicData(resource);
        if (exists) {
            assisted.ifPresent(dynamicData -> {
                ThreadCacheContext.setObject(cacheKey, dynamicData);
            }).orElse(() -> {
                ThreadCacheContext.setObject(cacheKey, NONE); // also cache not-found
            });
            return assisted;
        } else {
            return assisted;
        }
    }

    // -----------------------------------------------------
    //                                             Cache Key
    //                                             ---------
    protected String generateDataCacheKey(SMailDynamicDataResource resource) {
        final String templatePath = resource.getTemplatePath();
        final boolean filesystem = resource.isFilesystem();
        final OptionalThing<Locale> receiverLocale = resource.getReceiverLocale();
        return "fw:mailDynamicData:" + templatePath + ":" + filesystem + ":" + receiverLocale;
    }

    // -----------------------------------------------------
    //                                                 Load
    //                                                ------
    protected abstract OptionalThing<? extends Object> loadDynamicData(SMailDynamicDataResource resource);
}
