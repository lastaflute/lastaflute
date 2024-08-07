/*
 * Copyright 2015-2024 the original author or authors.
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
package org.lastaflute.web.validation.theme.conversion;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

// _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
// #hope support TYPE_USE? or include element type failure? by jflute (2017/10/30)
//
// e.g. TYPE_USE
//  public List<@ValidateTypeFailure Integer> seaIdList;
//
// e.g. include
//  @ValidateTypeFailure
//  public List<Integer> seaIdList;
// _/_/_/_/_/_/_/_/_/_/
// #hope support in JSON Body (now only for Form) by jflute (2017/10/31)
/**
 * @author jflute
 * @since 0.6.5 (2015/11/04 Wednesday)
 */
@Target({ FIELD })
@Retention(RUNTIME)
@Documented
public @interface ValidateTypeFailure {

    String DEFAULT_MESSAGE = "{org.lastaflute.validator.constraints.TypeAny.message}";

    Class<?>[] groups() default {};
}
