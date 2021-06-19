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
package org.lastaflute.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author jflute (2021/05/18 Tuesday at roppongi japanese)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface RestfulAction {

    /**
     * You can define hyphenated resource names here.
     * <pre>
     * <span style="color: #3F7E5E">// e.g. /ballet-dancers/</span>
     * <span style="color: #3F7E5E">//  ballet.dancers.BalletDancersAction</span>
     * &#064;RestfulAction(hyphenate="ballet-dancers")
     * 
     * <span style="color: #3F7E5E">// e.g. /dancers/1/favorite-studios/</span>
     * <span style="color: #3F7E5E">//  dancers.favorite.studios.DancersFavoriteStudiosAction</span>
     * &#064;RestfulAction(hyphenate="favorite-studios")
     * 
     * <span style="color: #3F7E5E">// e.g. /ballet-dancers/1/favorite-studios/</span>
     * <span style="color: #3F7E5E">//  ballet.dancers.favorite.studios.BalletDancersFavoriteStudiosAction</span>
     * &#064;RestfulAction(hyphenate={"ballet-dancers", "favorite-studios"})
     * </pre>
     * 
     * <p>Cannot set e.g. "sea", "-sea", "sea-", "sea--land", "sea/land", "Sea-Land", "sea$land-piari", "sea_land-piari".</p>
     * 
     * @return The hyphenated resource names for the action.
     */
    String[] hyphenate() default {};

    /**
     * You can define e.g. get$sea(), get$land() in RESTful action. <br>
     * It depends on router, available if e.g. NumericBasedRestfulRouter.
     * @return true if you use event suffix.
     */
    boolean allowEventSuffix() default false;
}