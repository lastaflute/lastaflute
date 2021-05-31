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
package org.lastaflute.core.util;

import org.dbflute.util.Srl;

/**
 * @author jflute
 * @since 0.7.3 (2015/12/27 Sunday)
 */
public class LaStringUtil {

    public static boolean isEmpty(String str) {
        return Srl.is_Null_or_Empty(str);
    }

    public static boolean isNotEmpty(String str) {
        return Srl.is_NotNull_and_NotEmpty(str);
    }
}
