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
package org.lastaflute.db.dbflute.classification;

import java.util.Locale;
import java.util.function.Function;

import org.dbflute.jdbc.ClassificationMeta;
import org.dbflute.optional.OptionalObject;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.lastaflute.db.dbflute.exception.ProvidedClassificationNotFoundException;

/**
 * @author jflute
 */
public abstract class TypicalListedClassificationProvider implements ListedClassificationProvider {

    @Override
    public ClassificationMeta provide(String classificationName) throws ProvidedClassificationNotFoundException {
        final ClassificationMeta onMainSchema = findOnMainSchema(classificationName);
        if (onMainSchema == null) {
            handleClassificationNotFound(classificationName);
        }
        return onMainSchema;
    }

    protected ClassificationMeta findOnMainSchema(String classificationName) throws ProvidedClassificationNotFoundException {
        final String projectDelimiter = getProjectDelimiter(); // dot means group delimiter so use other mark here
        if (classificationName.contains(projectDelimiter)) { // e.g. sea$land: means land classification in sea project
            final String projectName = Srl.substringFirstFront(classificationName, projectDelimiter);
            final String pureName = Srl.substringFirstRear(classificationName, projectDelimiter);
            return chooseClassificationFinder(projectName).apply(pureName);
        } else { // e.g. sea: means sea classification
            return getDefaultClassificationFinder().apply(classificationName);
        }
    }

    protected String getProjectDelimiter() {
        return "-";
    }

    protected abstract Function<String, ClassificationMeta> chooseClassificationFinder(String projectName)
            throws ProvidedClassificationNotFoundException;

    protected abstract Function<String, ClassificationMeta> getDefaultClassificationFinder();

    protected void handleClassificationNotFound(String classificationName) throws ProvidedClassificationNotFoundException {
        throw new ProvidedClassificationNotFoundException("Not found the classification: " + classificationName);
    }

    @Override
    public OptionalThing<String> determineAlias(Locale locale) {
        return OptionalObject.empty();
    }
}
