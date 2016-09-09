/*
 * Copyright 2015-2016 the original author or authors.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.dbflute.jdbc.Classification;
import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.Srl;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.PropertyDesc;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;

/**
 * @author jflute
 * @since 0.8.4 (2016/09/08 Thursday)
 */
public abstract class Lato {

    // ===================================================================================
    //                                                                          toString()
    //                                                                          ==========
    /**
     * @param obj The owner of toString(). (NullAllowed: if null, returns "null")
     * @return The expression of property values. (NotNull)
     */
    public static String string(Object obj) { // without class name
        if (obj == null) {
            return "null";
        }
        final Map<Object, String> alreadyAppearedSet = new HashMap<Object, String>();
        return deriveExpression(obj, alreadyAppearedSet, () -> {
            alreadyAppearedSet.put(obj, "(cyclic)");
            return resolveBeanExpression(obj, alreadyAppearedSet);
        });
    }

    protected static String deriveExpression(Object obj, Map<Object, String> alreadyAppearedSet, Supplier<String> beanResolver) {
        final String exp;
        if (obj == null) {
            exp = "null";
        } else if (obj instanceof String) {
            exp = (String) obj;
        } else if (obj instanceof Number) {
            exp = obj.toString();
        } else if (DfTypeUtil.isAnyLocalDate(obj)) {
            exp = obj.toString();
        } else if (obj instanceof Classification) {
            exp = ((Classification) obj).code();
        } else if (obj instanceof List) {
            @SuppressWarnings("unchecked")
            final List<? extends Object> list = (List<? extends Object>) obj;
            exp = buildListString(list, alreadyAppearedSet);
        } else if (obj instanceof Object[]) {
            exp = buildListString(Arrays.asList(((Object[]) obj)), alreadyAppearedSet);
        } else {
            exp = beanResolver.get();
        }
        return exp;
    }

    protected static String resolveBeanExpression(Object obj, Map<Object, String> alreadyAppearedSet) {
        if (obj == null) {
            throw new IllegalArgumentException("The argument 'obj' should not be null.");
        }
        final Class<? extends Object> targetType = obj.getClass();
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(targetType);
        final int propertySize = beanDesc.getPropertyDescSize();
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < propertySize; i++) {
            final PropertyDesc pd = beanDesc.getPropertyDesc(i);
            final String propertyName = pd.getPropertyName();
            final Object propertyValue = pd.getValue(obj);
            final String exp = deriveExpression(propertyValue, alreadyAppearedSet, () -> {
                return resolveObjectString(propertyValue, alreadyAppearedSet);
            });
            sb.append(i > 0 ? ", " : "").append(propertyName).append("=").append(exp);
        }
        sb.append("}");
        return sb.toString();
    }

    // ===================================================================================
    //                                                                         List String
    //                                                                         ===========
    protected static String buildListString(List<? extends Object> list, Map<Object, String> alreadyAppearedSet) {
        final String appeared = handleAlreadyAppeared(list, alreadyAppearedSet);
        if (appeared != null) {
            return appeared;
        }
        final StringBuilder listSb = new StringBuilder();
        listSb.append("[");
        int index = 0;
        for (Object element : list) {
            listSb.append(index > 0 ? ", " : "");
            listSb.append(resolveObjectString(element, alreadyAppearedSet));
            ++index;
        }
        listSb.append("]");
        final String exp = listSb.toString();
        alreadyAppearedSet.put(list, exp); // real expression
        return exp;
    }

    // ===================================================================================
    //                                                                       Object String
    //                                                                       =============
    protected static String resolveObjectString(Object bean, Map<Object, String> alreadyAppearedSet) {
        if (bean == null) {
            return "null";
        }
        final String appeared = handleAlreadyAppeared(bean, alreadyAppearedSet);
        if (appeared != null) {
            return appeared;
        }
        final String exp;
        if (mightBeOverridden(bean)) {
            final String str = bean.toString();
            exp = str != null ? str : "null";
        } else { // virtual override
            exp = resolveBeanExpression(bean, alreadyAppearedSet);
        }
        alreadyAppearedSet.put(bean, exp); // real expression
        return exp;
    }

    protected static boolean mightBeOverridden(Object value) {
        final String exp = value.toString();
        return exp == null || !exp.equals(sameAsObjectToString(value));
    }

    protected static String sameAsObjectToString(Object value) {
        return value.getClass().getName() + "@" + Integer.toHexString(value.hashCode());
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected static String handleAlreadyAppeared(Object obj, Map<Object, String> alreadyAppearedSet) {
        if (alreadyAppearedSet.containsKey(obj)) {
            final String alreadyExp = alreadyAppearedSet.get(obj);
            if (alreadyExp != null) {
                return Srl.cut(alreadyExp, 30, "...(same)") + (alreadyExp.startsWith("{") ? "}" : "");
            } else {
                return "null"; // basically noway, but just in case
            }
        } else {
            alreadyAppearedSet.put(obj, "(cyclic)"); // as parent
            return null; // first-time object
        }
    }
}
