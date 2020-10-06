/*
 * Copyright 2015-2019 the original author or authors.
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
package org.lastaflute.core.json.control;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.json.JsonMappingOption.JsonFieldNaming;
import org.lastaflute.core.json.bind.JsonYourCollectionResource;
import org.lastaflute.core.json.bind.JsonYourScalarResource;
import org.lastaflute.core.json.filter.JsonSimpleTextReadingFilter;
import org.lastaflute.core.json.filter.JsonTypeableTextReadingFilter;

/**
 * @author jflute
 * @since 1.1.1 (2018/12/18 Tuesday at showbase)
 */
public interface JsonMappingControlMeta {

    OptionalThing<DateTimeFormatter> getLocalDateFormatter();

    OptionalThing<Predicate<String>> getLocalDateFormattingTrigger();

    OptionalThing<DateTimeFormatter> getLocalDateTimeFormatter();

    OptionalThing<Predicate<String>> getLocalDateTimeFormattingTrigger();

    OptionalThing<DateTimeFormatter> getLocalTimeFormatter();

    OptionalThing<Predicate<String>> getLocalTimeFormattingTrigger();

    OptionalThing<DateTimeFormatter> getZonedDateTimeFormatter();

    OptionalThing<Function<Object, Boolean>> getBooleanDeserializer();

    OptionalThing<Function<Boolean, Object>> getBooleanSerializer();

    OptionalThing<Predicate<Class<?>>> getEmptyToNullReadingDeterminer();

    OptionalThing<Predicate<Class<?>>> getNullToEmptyWritingDeterminer();

    boolean isEverywhereQuoteWriting();

    OptionalThing<JsonSimpleTextReadingFilter> getSimpleTextReadingFilter();

    OptionalThing<JsonTypeableTextReadingFilter> getTypeableTextReadingFilter();

    boolean isListNullToEmptyReading();

    boolean isListNullToEmptyWriting();

    OptionalThing<JsonFieldNaming> getFieldNaming();

    List<JsonYourCollectionResource> getYourCollections(); // not null, read-only

    List<JsonYourScalarResource> getYourScalars(); // not null, read-only

    OptionalThing<Consumer<Object>> getYourUltimateCustomizer();
}
