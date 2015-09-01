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
package org.lastaflute.core.json;

import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;

/**
 * @author jflute
 */
public class JsonMappingOption {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected OptionalThing<DateTimeFormatter> localDateFormatter = OptionalThing.empty(); // not null
    protected OptionalThing<DateTimeFormatter> localDateTimeFormatter = OptionalThing.empty(); // not null
    protected OptionalThing<DateTimeFormatter> localTimeFormatter = OptionalThing.empty(); // not null
    protected OptionalThing<Function<Object, Boolean>> booleanDeserializer = OptionalThing.empty(); // not null
    protected OptionalThing<Function<Boolean, Object>> booleanSerializer = OptionalThing.empty(); // not null
    protected boolean emptyToNullReading;
    protected boolean nullToEmptyWriting;
    protected boolean everywhereQuoteWriting;

    // ===================================================================================
    //                                                                      Accept Another
    //                                                                      ==============
    public JsonMappingOption acceptAnother(JsonMappingOption another) {
        localDateFormatter = another.getLocalDateFormatter();
        localDateTimeFormatter = another.getLocalDateTimeFormatter();
        localTimeFormatter = another.getLocalTimeFormatter();
        booleanDeserializer = another.getBooleanDeserializer();
        booleanSerializer = another.getBooleanSerializer();
        emptyToNullReading = another.isEmptyToNullReading();
        nullToEmptyWriting = another.isNullToEmptyWriting();
        everywhereQuoteWriting = another.isEverywhereQuoteWriting();
        return this;
    }

    // ===================================================================================
    //                                                                      Option Setting
    //                                                                      ==============
    public JsonMappingOption formatLocalDateBy(DateTimeFormatter localDateFormatter) {
        if (localDateFormatter == null) {
            throw new IllegalArgumentException("The argument 'localDateFormatter' should not be null.");
        }
        this.localDateFormatter = OptionalThing.of(localDateFormatter);
        return this;
    }

    public JsonMappingOption formatLocalDateTimeBy(DateTimeFormatter localDateTimeFormatter) {
        if (localDateTimeFormatter == null) {
            throw new IllegalArgumentException("The argument 'localDateTimeFormatter' should not be null.");
        }
        this.localDateTimeFormatter = OptionalThing.of(localDateTimeFormatter);
        return this;
    }

    public JsonMappingOption formatLocalTimeBy(DateTimeFormatter localTimeFormatter) {
        if (localTimeFormatter == null) {
            throw new IllegalArgumentException("The argument 'localTimeFormatter' should not be null.");
        }
        this.localTimeFormatter = OptionalThing.of(localTimeFormatter);
        return this;
    }

    public JsonMappingOption deserializeBooleanBy(Function<Object, Boolean> booleanDeserializer) {
        if (booleanDeserializer == null) {
            throw new IllegalArgumentException("The argument 'booleanDeserializer' should not be null.");
        }
        this.booleanDeserializer = OptionalThing.of(booleanDeserializer);
        return this;
    }

    public JsonMappingOption serializeBooleanBy(Function<Boolean, Object> booleanSerializer) {
        if (booleanSerializer == null) {
            throw new IllegalArgumentException("The argument 'booleanSerializer' should not be null.");
        }
        this.booleanSerializer = OptionalThing.of(booleanSerializer);
        return this;
    }

    /**
     * Set up as empty-to-null reading.
     * @return this. (NotNull)
     */
    public JsonMappingOption asEmptyToNullReading() {
        emptyToNullReading = true;
        return this;
    }

    /**
     * Set up as null-to-empty writing.
     * @return this. (NotNull)
     */
    public JsonMappingOption asNullToEmptyWriting() {
        nullToEmptyWriting = true;
        return this;
    }

    /**
     * Setup as everywhere-quote writing, e.g. even if Integer, quote it.
     * @return this. (NotNull)
     */
    public JsonMappingOption asEverywhereQuoteWriting() {
        everywhereQuoteWriting = true;
        return this;
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final String delimiter = ", ";
        localDateFormatter.ifPresent(ter -> sb.append(delimiter).append(ter));
        localDateTimeFormatter.ifPresent(ter -> sb.append(delimiter).append(ter));
        localTimeFormatter.ifPresent(ter -> sb.append(delimiter).append(ter));
        if (emptyToNullReading) {
            sb.append(delimiter).append("emptyToNullReading");
        }
        if (nullToEmptyWriting) {
            sb.append(delimiter).append("nullToEmptyWriting");
        }
        if (everywhereQuoteWriting) {
            sb.append(delimiter).append("everywhereQuoteWriting");
        }
        return "{" + Srl.ltrim(sb.toString(), delimiter) + "}";
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public OptionalThing<DateTimeFormatter> getLocalDateFormatter() {
        return localDateFormatter;
    }

    public OptionalThing<DateTimeFormatter> getLocalDateTimeFormatter() {
        return localDateTimeFormatter;
    }

    public OptionalThing<DateTimeFormatter> getLocalTimeFormatter() {
        return localTimeFormatter;
    }

    public OptionalThing<Function<Object, Boolean>> getBooleanDeserializer() {
        return booleanDeserializer;
    }

    public OptionalThing<Function<Boolean, Object>> getBooleanSerializer() {
        return booleanSerializer;
    }

    public boolean isEmptyToNullReading() {
        return emptyToNullReading;
    }

    public boolean isNullToEmptyWriting() {
        return nullToEmptyWriting;
    }

    public boolean isEverywhereQuoteWriting() {
        return everywhereQuoteWriting;
    }
}
