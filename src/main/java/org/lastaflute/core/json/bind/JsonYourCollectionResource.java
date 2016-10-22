package org.lastaflute.core.json.bind;

import java.util.Collection;
import java.util.function.Function;

/**
 * @author jflute
 * @since 0.8.5 (2016/10/23 Sunday) 
 */
public class JsonYourCollectionResource {

    protected final Class<?> yourType;
    protected final Function<Collection<? extends Object>, Iterable<? extends Object>> yourCollectionCreator;

    public JsonYourCollectionResource(Class<?> yourType,
            Function<Collection<? extends Object>, Iterable<? extends Object>> yourCollectionCreator) {
        this.yourType = yourType;
        this.yourCollectionCreator = yourCollectionCreator;
    }

    @Override
    public String toString() {
        return "yourCollection:{" + yourType.getName() + "}";
    }

    public Class<?> getYourType() {
        return yourType;
    }

    public Function<Collection<? extends Object>, Iterable<? extends Object>> getYourCollectionCreator() {
        return yourCollectionCreator;
    }
}
