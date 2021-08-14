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
 * @author modified by jflute (originated in Seasar)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Execute {

    /**
     * The URL pattern to adjust method mapping. <br>
     * <pre>
     * <span style="color: #3F7E5E">// e.g. you can add fixed word between variables</span>
     * &#064;Execute(<span style="color: #CC4747">urlPattern</span>=<span style="color: #2A00FF">"{}/piary/{}"</span>) <span style="color: #3F7E5E">// e.g. sea/3/piary/7</span>
     * public HtmlResponse sea(int landId, int dstoreId) {
     * }
     * 
     * <span style="color: #3F7E5E">// e.g. you can adjust mapping by camel case keywords of method name</span>
     * &#064;Execute(<span style="color: #CC4747">urlPattern</span>=<span style="color: #2A00FF">"&#064;word/{}/&#064;word"</span>) <span style="color: #3F7E5E">// e.g. sea/3/land</span>
     * public HtmlResponse seaLand(int piaryId) {
     * }
     * </pre>
     * @return The pattern expression of URL pattern. (NotNull, EmptyAllowed: if empty, default mapping)
     */
    String urlPattern() default "";

    /**
     * Suppress default action transaction. <br>
     * Then, use TransactionStage and begin transaction manually.
     * @return The determination, true or false.
     */
    boolean suppressTransaction() default false;

    /**
     * Suppress validator call check of framework. <br>
     * When validator annotations are specified in your form (or body), <br>
     * you should call validate() or validateApi() in your execute method of action. <br>
     * For example, annotations exist but not called, framework gives you error. <br>
     * But you can supresses the check by this attribute.
     * @return The determination, true or false.
     */
    boolean suppressValidatorCallCheck() default false;

    /**
     * The limit of SQL execution count in one request. <br>
     * If it's over, show warning log (so also in production) <br>
     * For example: (43 executions / limit 30)
     * <pre>
     * *Too many SQL executions: 43/30 in ProductListAction@index()
     * </pre>
     * @return The integer for limit of SQL execution count in one request. (MinusAllowed: use default limit)
     */
    int sqlExecutionCountLimit() default -1;

    /**
     * The HTTP status for success story of this execute method. <br>
     * This is ignored if returned response already has HTTP status.
     * @return The annotation of HTTP status for success story. (NotNull)
     */
    HttpStatus successHttpStatus() default @HttpStatus(value = -1, desc = ""); // since 1.2.1

    @interface HttpStatus {

        /**
         * @return The integer of HTTP status for success story. (MinusAllowed: use default status)
         */
        int value();

        /**
         * @return The string of description about HTTP status for e.g. client developer. (NotEmpty: empty allowed only if -1 value, however...)
         */
        String desc();
    }
}