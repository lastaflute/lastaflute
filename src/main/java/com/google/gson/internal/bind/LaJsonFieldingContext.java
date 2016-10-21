package com.google.gson.internal.bind;

import java.lang.reflect.Field;

/**
 * @author jflute
 * @since 0.8.5 (2016/10/21 Friday at showbase)
 */
public class LaJsonFieldingContext {

    protected static final ThreadLocal<Field> _defaultThreadLocal = new ThreadLocal<Field>();

    public static Field getJsonFieldOnThread() {
        return _defaultThreadLocal.get();
    }

    public static void setJsonFieldOnThread(Field field) {
        if (field == null) {
            String msg = "The argument 'field' should not be null.";
            throw new IllegalArgumentException(msg);
        }
        _defaultThreadLocal.set(field);
    }

    public static void clearAccessContextOnThread() {
        _defaultThreadLocal.set(null);
    }
}
