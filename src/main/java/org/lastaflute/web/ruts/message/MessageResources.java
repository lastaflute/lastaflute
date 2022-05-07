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
package org.lastaflute.web.ruts.message;

import java.util.Locale;

/**
 * @author modified by jflute (originated in Struts)
 */
public interface MessageResources {

    String getMessage(Locale locale, String key);

    String getMessage(Locale locale, String key, Object arg0);

    String getMessage(Locale locale, String key, Object arg0, Object arg1);

    String getMessage(Locale locale, String key, Object arg0, Object arg1, Object arg2);

    String getMessage(Locale locale, String key, Object arg0, Object arg1, Object arg2, Object arg3);

    String getMessage(Locale locale, String key, Object[] args);

    boolean isPresent(Locale locale, String key);
}
