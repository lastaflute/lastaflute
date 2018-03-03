/*
 * Copyright 2015-2018 the original author or authors.
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
import org.lastaflute.core.util.LaClassificationUtil;
import org.lastaflute.db.dbflute.exception.ProvidedClassificationNotFoundException;

/**
 * @author jflute
 */
public abstract class TypicalListedClassificationProvider implements ListedClassificationProvider {

    // ===================================================================================
    //                                                                        Provide Meta
    //                                                                        ============
    @Override
    public ClassificationMeta provide(String classificationName) throws ProvidedClassificationNotFoundException {
        final ClassificationMeta meta = resolveMeta(classificationName);
        if (meta == null) {
            handleClassificationNotFound(classificationName);
        }
        return meta;
    }

    protected ClassificationMeta resolveMeta(String classificationName) // returns null when not found
            throws ProvidedClassificationNotFoundException { // and throws when project not found
        final String projectDelimiter = getProjectDelimiter(); // dot means group delimiter so use other mark here
        if (classificationName.contains(projectDelimiter)) { // e.g. sea-land: means land classification in sea project
            final String projectName = Srl.substringFirstFront(classificationName, projectDelimiter);
            final String pureName = Srl.substringFirstRear(classificationName, projectDelimiter);
            return chooseClassificationFinder(projectName).apply(pureName);
        } else { // e.g. sea: means sea classification
            return getDefaultClassificationFinder().apply(classificationName);
        }
    }

    protected String getProjectDelimiter() {
        return "-"; // e.g. maihamadb-MemberStatus
    }

    /**
     * Choose DB classification finder for the project. <br>
     * (not contains application classification)
     * <pre>
     * e.g.
     *  protected Function&lt;String, ClassificationMeta&gt; chooseClassificationFinder(String projectName)
     *          throws ProvidedClassificationNotFoundException {
     *      if (DBCurrent.getInstance().projectName().equals(projectName)) {
     *          return clsName -&gt; onMainSchema(clsName).orElse(null); // null means not found
     *      } else {
     *          throw new ProvidedClassificationNotFoundException("Unknown DBFlute project name: " + projectName);
     *      }
     *  }
     * </pre>
     * @param projectName The project name of DBFlute. (NotNull)
     * @return The finder of classification as function "from classification name to null-allowed meta". (NotNull)
     * @throws ProvidedClassificationNotFoundException When the project is not found.
     */
    protected abstract Function<String, ClassificationMeta> chooseClassificationFinder(String projectName)
            throws ProvidedClassificationNotFoundException;

    /**
     * Get default classification finder when project name is not specified. <br>
     * Basically it returns main schema's classification. <br>
     * (you can contain application classification)
     * <pre>
     * e.g.
     *  protected Function&lt;String, ClassificationMeta&gt; getDefaultClassificationFinder() {
     *      return clsName -&gt; {
     *          return onMainSchema(clsName).orElseGet(() -&gt; {
     *              return onAppCls(clsName).orElse(null); // null means not found
     *          });
     *      };
     *  }
     * </pre>
     * @return The finder of classification as function "from classification name to null-allowed meta". (NotNull)
     */
    protected abstract Function<String, ClassificationMeta> getDefaultClassificationFinder();

    // -----------------------------------------------------
    //                                          Assist Logic
    //                                          ------------
    // helper for sub class
    protected OptionalThing<ClassificationMeta> findMeta(Class<?> defmetaType, String classificationName) {
        return LaClassificationUtil.findMeta(defmetaType, classificationName);
    }

    // you can customize it when not found
    protected void handleClassificationNotFound(String classificationName) throws ProvidedClassificationNotFoundException {
        throw new ProvidedClassificationNotFoundException("Not found the classification: " + classificationName);
    }

    // ===================================================================================
    //                                                                     Delimiter Alias
    //                                                                     ===============
    @Override
    public OptionalThing<String> determineAlias(Locale locale) {
        return OptionalObject.empty();
    }
}
