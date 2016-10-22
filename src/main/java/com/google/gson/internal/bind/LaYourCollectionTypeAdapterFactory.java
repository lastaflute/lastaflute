package com.google.gson.internal.bind;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.LaGsonTypes;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * @author jflute
 * @since 0.8.5 (2016/10/23 Sunday)
 */
public class LaYourCollectionTypeAdapterFactory implements TypeAdapterFactory {

    protected final Class<?> yourType;
    protected final Function<Collection<? extends Object>, Iterable<? extends Object>> yourCollectionCreator;

    public LaYourCollectionTypeAdapterFactory(Class<?> yourType,
            Function<Collection<? extends Object>, Iterable<? extends Object>> yourCollectionCreator) {
        this.yourType = yourType;
        this.yourCollectionCreator = yourCollectionCreator;
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        final Type type = typeToken.getType();

        final Class<? super T> rawType = typeToken.getRawType();
        if (!yourType.isAssignableFrom(rawType)) {
            return null;
        }

        final Type elementType = LaGsonTypes.getYourCollectionElementType(type, rawType, yourType);
        final TypeAdapter<?> elementTypeAdapter = gson.getAdapter(TypeToken.get(elementType));

        @SuppressWarnings({ "unchecked", "rawtypes" }) // create() doesn't define a type parameter
        final TypeAdapter<T> result = new Adapter(gson, elementType, elementTypeAdapter);
        return result;
    }

    private final class Adapter<E> extends TypeAdapter<Iterable<E>> {
        private final TypeAdapter<E> elementTypeAdapter;

        public Adapter(Gson context, Type elementType, TypeAdapter<E> elementTypeAdapter) {
            this.elementTypeAdapter = new TypeAdapterRuntimeTypeWrapper<E>(context, elementTypeAdapter, elementType);
        }

        @Override
        public Iterable<E> read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            final List<E> mutableList = new ArrayList<E>();
            in.beginArray();
            while (in.hasNext()) {
                E instance = elementTypeAdapter.read(in);
                mutableList.add(instance);
            }
            in.endArray();
            @SuppressWarnings("unchecked")
            final Iterable<E> ite = (Iterable<E>) yourCollectionCreator.apply(mutableList);
            return ite;
        }

        @Override
        public void write(JsonWriter out, Iterable<E> collection) throws IOException {
            if (collection == null) {
                out.nullValue();
                return;
            }

            out.beginArray();
            for (E element : collection) {
                elementTypeAdapter.write(out, element);
            }
            out.endArray();
        }
    }
}
