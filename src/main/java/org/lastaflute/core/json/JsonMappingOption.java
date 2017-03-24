/*
 * Copyright 2015-2017 the original author or authors.
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
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.lastaflute.core.json.bind.JsonYourCollectionResource;
import org.lastaflute.core.json.filter.JsonSimpleTextReadingFilter;

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
    protected boolean emptyToNullReading; // e.g. String, Integer, LocalDate, ... (except List)
    protected boolean nullToEmptyWriting; // same
    protected boolean everywhereQuoteWriting; // e.g. Integer(1) to "1"
    protected OptionalThing<JsonSimpleTextReadingFilter> simpleTextReadingFilter = OptionalThing.empty(); // not null
    protected boolean listNullToEmptyReading; // [] if null
    protected boolean listNullToEmptyWriting; // same
    protected OptionalThing<JsonFieldNaming> fieldNaming = OptionalThing.empty(); // not null;
    protected List<JsonYourCollectionResource> yourCollections = Collections.emptyList();

    // ===================================================================================
    //                                                                    Supplement Class
    //                                                                    ================
    public static enum JsonFieldNaming {

        /** Gson's FieldNamingPolicy.IDENTITY */
        IDENTITY,
        /** Gson's FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES */
        CAMEL_TO_LOWER_SNAKE
    }

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
        simpleTextReadingFilter = another.getSimpleTextReadingFilter();
        listNullToEmptyReading = another.isListNullToEmptyReading();
        listNullToEmptyWriting = another.isListNullToEmptyWriting();
        fieldNaming = another.getFieldNaming();
        yourCollections = another.getYourCollections();
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

    public JsonMappingOption filterSimpleTextReading(JsonSimpleTextReadingFilter simpleTextReadingFilter) {
        if (simpleTextReadingFilter == null) {
            throw new IllegalArgumentException("The argument 'simpleTextReadingFilter' should not be null.");
        }
        this.simpleTextReadingFilter = OptionalThing.of(simpleTextReadingFilter);
        return this;
    }

    /**
     * Set up as list-null-to-empty reading. (null value of List is read as empty list)
     * @return this. (NotNull)
     */
    public JsonMappingOption asListNullToEmptyReading() {
        listNullToEmptyReading = true;
        return this;
    }

    /**
     * Set up as list-null-to-empty writing. (null value of List is writtern as empty list)
     * @return this. (NotNull)
     */
    public JsonMappingOption asListNullToEmptyWriting() {
        listNullToEmptyWriting = true;
        return this;
    }

    /**
     * Set up as list-null-to-empty writing. (null value of List is writtern as empty list)
     * @param fieldNaming The type of field naming. (NotNull)
     * @return this. (NotNull)
     */
    public JsonMappingOption asFieldNaming(JsonFieldNaming fieldNaming) {
        if (fieldNaming == null) {
            throw new IllegalArgumentException("The argument 'fieldNaming' should not be null.");
        }
        this.fieldNaming = OptionalThing.of(fieldNaming);
        return this;
    }

    /**
     * Set up the your collections for JSON property. <br>
     * You can use e.g. ImmutableList (Eclipse Collections) as JSON property type.
     * @param yourCollections The list of your collection resource. (NotNull)
     * @return this. (NotNull)
     */
    public JsonMappingOption yourCollections(List<JsonYourCollectionResource> yourCollections) {
        if (yourCollections == null) {
            throw new IllegalArgumentException("The argument 'yourCollections' should not be null.");
        }
        this.yourCollections = yourCollections;
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
        booleanDeserializer.ifPresent(zer -> sb.append(delimiter).append(zer));
        booleanSerializer.ifPresent(zer -> sb.append(delimiter).append(zer));
        if (emptyToNullReading) {
            sb.append(delimiter).append("emptyToNullReading");
        }
        if (nullToEmptyWriting) {
            sb.append(delimiter).append("nullToEmptyWriting");
        }
        if (everywhereQuoteWriting) {
            sb.append(delimiter).append("everywhereQuoteWriting");
        }
        simpleTextReadingFilter.ifPresent(ter -> sb.append(delimiter).append(ter));
        fieldNaming.ifPresent(ing -> sb.append(delimiter).append(ing));
        if (!yourCollections.isEmpty()) {
            final List<String> expList = yourCollections.stream().map(ons -> {
                return ons.getYourType().getSimpleName();
            }).collect(Collectors.toList());
            sb.append(delimiter).append(expList);
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

    public OptionalThing<JsonSimpleTextReadingFilter> getSimpleTextReadingFilter() {
        return simpleTextReadingFilter;
    }

    public boolean isListNullToEmptyReading() {
        return listNullToEmptyReading;
    }

    public boolean isListNullToEmptyWriting() {
        return listNullToEmptyWriting;
    }

    public OptionalThing<JsonFieldNaming> getFieldNaming() {
        return fieldNaming;
    }

    public List<JsonYourCollectionResource> getYourCollections() {
        return Collections.unmodifiableList(yourCollections);
    }
}
