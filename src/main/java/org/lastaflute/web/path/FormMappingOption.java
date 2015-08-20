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
package org.lastaflute.web.path;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author jflute
 */
public class FormMappingOption {

    // TODO jflute only when development? (2015/08/20)
    protected boolean undefinedParameterError;
    protected Set<String> indefinableParameterSet;

    public FormMappingOption asUndefinedParameterError() {
        undefinedParameterError = true;
        return this;
    }

    public FormMappingOption indefinableParameters(String... indefinableParameters) {
        indefinableParameterSet = createIndefinableParameterSet(indefinableParameters);
        return this;
    }

    protected Set<String> createIndefinableParameterSet(String... indefinableParameters) {
        return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(indefinableParameters)));
    }

    public boolean isUndefinedParameterError() {
        return undefinedParameterError;
    }

    public Set<String> getIndefinableParameterSet() {
        return indefinableParameterSet != null ? indefinableParameterSet : Collections.emptySet();
    }
}
