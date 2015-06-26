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
package org.lastaflute.core.mail;

import java.util.Locale;

import org.dbflute.mail.Postcard;
import org.dbflute.mail.send.embedded.receptionist.SMailDynamicTextAssist;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.magic.ThreadCacheContext;

/**
 * @since 0.6.0 (2015/05/16 Saturday)
 */
public abstract class LaThreadCachedDynamicTextAssist implements SMailDynamicTextAssist {

    protected static final Object NONE = new Object();

    @Override
    public String assist(Postcard postcard, String path, boolean filesystem, OptionalThing<Locale> receiverLocale) {
        final boolean exists = ThreadCacheContext.exists();
        final String cacheKey = exists ? generateCacheKey(path, filesystem, receiverLocale) : null;
        if (exists) {
            final String cached = ThreadCacheContext.getObject(cacheKey);
            if (cached != null) {
                return !cached.equals(NONE) ? cached : null;
            }
        }
        final String assisted = doAssist(postcard, path, filesystem, receiverLocale);
        if (exists) {
            if (assisted != null) {
                ThreadCacheContext.setObject(cacheKey, assisted);
                return ThreadCacheContext.getObject(cacheKey);
            } else {
                ThreadCacheContext.setObject(cacheKey, NONE); // also cache not-found
                return null;
            }
        } else {
            return assisted;
        }
    }

    protected String generateCacheKey(String path, boolean filesystem, OptionalThing<Locale> receiverLocale) {
        return "fw:mailText:" + path + ":" + filesystem + ":" + receiverLocale;
    }

    protected abstract String doAssist(Postcard postcard, String path, boolean filesystem, OptionalThing<Locale> receiverLocale);
}
