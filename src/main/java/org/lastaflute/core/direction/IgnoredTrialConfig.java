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
package org.lastaflute.core.direction;

import java.io.IOException;
import java.io.InputStream;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfResourceUtil;

/**
 * @author jflute
 * @since 0.8.3 (2016/08/13 Saturday)
 */
public class IgnoredTrialConfig {

    public OptionalThing<String> trial() { // return present if exists
        final String path = getTrialConfigPath();
        final InputStream ins = DfResourceUtil.getResourceStream(path);
        final boolean exists = ins != null;
        if (exists) {
            try {
                ins.close(); // just in case
            } catch (IOException ignored) {}
        }
        return exists ? OptionalThing.of(path) : OptionalThing.empty();
    }

    protected String getTrialConfigPath() { // you can override
        return "trial_config.ignored"; // as default, you should add the file to .gitignore
    }
}
