package com.google.gson.internal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

/**
 * @author jflute
 * @since 0.8.5 (2016/10/23 Sunday)
 */
public class LaGsonTypes {

    public static Type getYourCollectionElementType(Type context, Class<?> contextRawType, Class<?> yourCollectionType) {
        Type collectionType = $Gson$Types.getSupertype(context, contextRawType, yourCollectionType);
        if (collectionType instanceof WildcardType) {
            collectionType = ((WildcardType) collectionType).getUpperBounds()[0];
        }
        if (collectionType instanceof ParameterizedType) {
            return ((ParameterizedType) collectionType).getActualTypeArguments()[0];
        }
        return Object.class;
    }
}
