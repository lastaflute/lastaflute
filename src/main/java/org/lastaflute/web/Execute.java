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
package org.lastaflute.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.lastaflute.web.token.TxToken;

/**
 * @author modified by jflute (originated in Seasar)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Execute {

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
     * 
     * @return The type of transaction token. (NotNull)
     */
    TxToken token() default TxToken.NONE;
}