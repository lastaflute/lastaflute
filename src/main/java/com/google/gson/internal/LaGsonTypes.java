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
package com.google.gson.internal;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.util.DfReflectionUtil;

/**
 * @author jflute
 * @since 0.8.5 (2016/10/23 Sunday)
 */
public class LaGsonTypes {

    private static final Method supertypeMethod; // cached for (small?) performance
    static {
        final String methodName = "getSupertype";
        final Class<?>[] argTypes = new Class<?>[] { Type.class, Class.class, Class.class };
        supertypeMethod = DfReflectionUtil.getWholeMethod($Gson$Types.class, methodName, argTypes);
        if (supertypeMethod == null) { // basically no way, or different Gson version
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Failed to get the Gson method by reflection.");
            br.addItem("Advice");
            br.addElement("This LastaFlute version requires over Gson-2.10.x version.");
            br.addElement("Make sure your Gson dependency definition.");
            br.addItem("Gson Method");
            br.addElement(methodName + Arrays.asList(argTypes));
            final String msg = br.buildExceptionMessage();
            throw new IllegalStateException(msg);
        }
        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // #thinking jflute however, no problem of Gson's module restriction? (2023/05/10)
        // test:
        // Java8 LastaFlute on Java8 test-fortress with ImmutableList :: OK
        // Java8 LastaFlute on Java17 test-fortress with ImmutableList :: OK
        // (maybe...Java17 LastaFlute has problem? or using module system on application?)
        // _/_/_/_/_/_/_/_/_/_/
        supertypeMethod.setAccessible(true); // the method is private scope
    }

    public static Type getYourCollectionElementType(Type context, Class<?> contextRawType, Class<?> yourCollectionType) {
        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // use reflection here by jflute (2023/05/10)
        // because the method modifier changed from package scope to private scope since Gson-2.10.x
        // old code:
        //Type collectionType = $Gson$Types.getSupertype(context, contextRawType, yourCollectionType);
        // _/_/_/_/_/_/_/_/_/_/
        Type collectionType =
                (Type) DfReflectionUtil.invoke(supertypeMethod, null, new Object[] { context, contextRawType, yourCollectionType });
        if (collectionType instanceof WildcardType) {
            collectionType = ((WildcardType) collectionType).getUpperBounds()[0];
        }
        if (collectionType instanceof ParameterizedType) {
            return ((ParameterizedType) collectionType).getActualTypeArguments()[0];
        }
        return Object.class;
    }
}
