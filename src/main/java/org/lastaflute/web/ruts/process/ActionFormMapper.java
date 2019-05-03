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
package org.lastaflute.web.ruts.process;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.dbflute.jdbc.Classification;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.json.JsonObjectConvertible;
import org.lastaflute.core.magic.ThreadCacheContext;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.ParameterizedClassDesc;
import org.lastaflute.di.helper.beans.PropertyDesc;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;
import org.lastaflute.di.util.LdiArrayUtil;
import org.lastaflute.di.util.LdiClassUtil;
import org.lastaflute.di.util.LdiModifierUtil;
import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.api.JsonParameter;
import org.lastaflute.web.direction.FwWebDirection;
import org.lastaflute.web.path.ActionAdjustmentProvider;
import org.lastaflute.web.path.FormMappingOption;
import org.lastaflute.web.ruts.VirtualForm;
import org.lastaflute.web.ruts.config.ActionFormMeta;
import org.lastaflute.web.ruts.config.ModuleConfig;
import org.lastaflute.web.ruts.inoutlogging.InOutLogKeeper;
import org.lastaflute.web.ruts.multipart.MultipartRequestHandler;
import org.lastaflute.web.ruts.multipart.MultipartRequestWrapper;
import org.lastaflute.web.ruts.multipart.MultipartResourceProvider;
import org.lastaflute.web.ruts.process.formcoins.FormCoinsHelper;
import org.lastaflute.web.ruts.process.populate.FormSimpleTextParameterFilter;
import org.lastaflute.web.ruts.process.populate.FormSimpleTextParameterMeta;
import org.lastaflute.web.ruts.process.populate.FormYourCollectionResource;
import org.lastaflute.web.servlet.filter.RequestLoggingFilter.WholeShowErrorFlushAttribute;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.validation.theme.conversion.TypeFailureBean;
import org.lastaflute.web.validation.theme.conversion.TypeFailureElement;
import org.lastaflute.web.validation.theme.conversion.ValidateTypeFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author modified by jflute (originated in Seasar and Struts)
 */
public class ActionFormMapper {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(ActionFormMapper.class);
    protected static final String MULTIPART_CONTENT_TYPE = "multipart/form-data";
    protected static final String CACHE_KEY_DECODED_PROPERTY_MAP = "requestProcessor.decodedPropertyMap";
    protected static final String CACHE_KEY_URL_PARAM_NAMES_CACHED_SET = "requestProcessor.pathParamNames.cachedSet";
    protected static final String CACHE_KEY_URL_PARAM_NAMES_UNIQUE_METHOD = "requestProcessor.pathParamNames.uniqueMethod";
    protected static final char NESTED_DELIM = '.';
    protected static final char INDEXED_DELIM = '[';
    protected static final char INDEXED_DELIM2 = ']';
    protected static final char MAPPED_DELIM = '(';
    protected static final char MAPPED_DELIM2 = ')';
    protected static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final FormMappingOption NULLOBJ_FORM_MAPPING_OPTION = new FormMappingOption(); // simple cache, private to be immutable

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // simpleton so cannot keep request instance...
    // (I wonder this mapper should be request scope because of too many arguments)
    protected final ModuleConfig moduleConfig;
    protected final FwAssistantDirector assistantDirector;
    protected final RequestManager requestManager;
    protected final FormCoinsHelper coinsHelper;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionFormMapper(ModuleConfig moduleConfig, FwAssistantDirector assistantDirector, RequestManager requestManager) {
        this.moduleConfig = moduleConfig;
        this.assistantDirector = assistantDirector;
        this.requestManager = requestManager;
        this.coinsHelper = new FormCoinsHelper(requestManager);
    }

    // ===================================================================================
    //                                                                            Populate
    //                                                                            ========
    public void populateParameter(ActionRuntime runtime, OptionalThing<VirtualForm> optForm) throws ServletException {
        if (!optForm.isPresent()) {
            return;
        }
        final FormMappingOption option = adjustFormMapping(); // not null
        final VirtualForm virtualForm = optForm.get();
        if (handleJsonBody(runtime, virtualForm, option)) {
            return;
        }
        MultipartRequestHandler multipartHandler = null;
        if (isMultipartRequest()) {
            final MultipartRequestWrapper wrapper = newMultipartRequestWrapper(requestManager.getRequest());
            ContainerUtil.overrideExternalRequest(wrapper);
            multipartHandler = createMultipartRequestHandler();
            multipartHandler.handleRequest(wrapper);
            if (MultipartRequestHandler.findExceededException(wrapper) != null) {
                return; // you can confirm exceeded by the static find method
            }
        }
        final Object realForm = virtualForm.getRealForm(); // not null
        final Map<String, Object> parameterMap = prepareRequestParameterMap(multipartHandler, option);
        try {
            for (Entry<String, Object> entry : parameterMap.entrySet()) {
                final String name = entry.getKey();
                final Object value = entry.getValue();
                try {
                    setProperty(runtime, virtualForm, realForm, name, value, /*pathSb*/null, option, null, null);
                } catch (Throwable cause) {
                    handleIllegalPropertyPopulateException(realForm, name, value, runtime, cause); // adjustment here
                }
            }
        } finally {
            keepParameterForInOutLoggingIfNeeds(parameterMap);
        }
    }

    protected boolean isMultipartRequest() {
        return requestManager.getContentType().map(contentType -> {
            return contentType.startsWith(MULTIPART_CONTENT_TYPE) && requestManager.isHttpMethodPost();
        }).orElse(false);
    }

    protected MultipartRequestWrapper newMultipartRequestWrapper(HttpServletRequest request) {
        return new MultipartRequestWrapper(request);
    }

    protected MultipartRequestHandler createMultipartRequestHandler() {
        final MultipartResourceProvider provider = assistWebDirection().assistMultipartResourceProvider();
        if (provider == null) {
            throw new IllegalStateException("No provider for multipart request in assistant director.");
        }
        final MultipartRequestHandler handler = provider.createHandler();
        if (handler == null) {
            throw new IllegalStateException("provideHandler() returned null: " + provider);
        }
        return handler;
    }

    protected Map<String, Object> prepareRequestParameterMap(MultipartRequestHandler multipartHandler, FormMappingOption option) {
        return coinsHelper.prepareRequestParameterMap(multipartHandler, option);
    }

    protected void handleIllegalPropertyPopulateException(Object form, String name, Object value, ActionRuntime runtime, Throwable cause)
            throws ServletException {
        coinsHelper.handleIllegalPropertyPopulateException(form, name, value, runtime, cause);
    }

    protected void keepParameterForInOutLoggingIfNeeds(Map<String, Object> parameterMap) {
        InOutLogKeeper.prepare(requestManager).ifPresent(keeper -> keeper.keepRequestParameter(parameterMap));
    }

    // ===================================================================================
    //                                                                           JSON Body
    //                                                                           =========
    protected boolean handleJsonBody(ActionRuntime runtime, VirtualForm virtualForm, FormMappingOption option) {
        final ActionFormMeta formMeta = virtualForm.getFormMeta();
        if (formMeta.isJsonBodyMapping()) {
            if (formMeta.isRootSymbolForm()) {
                mappingJsonBody(runtime, virtualForm, prepareJsonFromRequestBody(virtualForm), option);
            } else if (formMeta.isTypedListForm()) {
                mappingListJsonBody(runtime, virtualForm, prepareJsonFromRequestBody(virtualForm), option);
            }
            // basically no way here (but no exception just in case)
        }
        return false;
    }

    protected String prepareJsonFromRequestBody(VirtualForm virtualForm) {
        try {
            final String body = requestManager.getRequestBody();
            if (logger.isDebugEnabled()) {
                logger.debug("#flow ...Parsing JSON from request body:{}", buildJsonBodyDebugDisplay(body));
            }
            keepRequestBodyForErrorFlush(virtualForm, body);
            keepRequestBodyForInOutLoggingIfNeeds(body, "json");
            return body;
        } catch (RuntimeException e) {
            throwJsonBodyCannotReadFromRequestException(virtualForm, e);
            return null; // unreachable
        }
    }

    protected void throwJsonBodyCannotReadFromRequestException(VirtualForm virtualForm, RuntimeException e) {
        coinsHelper.throwJsonBodyCannotReadFromRequestException(virtualForm, e);
    }

    protected String buildJsonBodyDebugDisplay(String value) {
        return coinsHelper.buildJsonBodyDebugDisplay(value);
    }

    protected void keepRequestBodyForErrorFlush(VirtualForm virtualForm, String body) {
        // request body can be read only once so needs to keep it for error logging
        requestManager.setAttribute(LastaWebKey.REQUEST_BODY_KEY, new WholeShowErrorFlushAttribute(body));
    }

    protected void keepRequestBodyForInOutLoggingIfNeeds(String bodyContent, String bodyType) {
        InOutLogKeeper.prepare(requestManager).ifPresent(keeper -> keeper.keepRequestBody(bodyContent, bodyType));
    }

    // -----------------------------------------------------
    //                                             Bean JSON
    //                                             ---------
    protected void mappingJsonBody(ActionRuntime runtime, VirtualForm virtualForm, String json, FormMappingOption option) {
        option.getRequestJsonEngineProvider();
        try {
            final Class<?> formType = virtualForm.getFormMeta().getRootFormType(); // called only when root here
            final Object fromJson = chooseJsonObjectConvertible(runtime, option).fromJson(json, formType);
            acceptJsonRealForm(virtualForm, fromJson);
        } catch (RuntimeException e) {
            throwJsonBodyParseFailureException(runtime, virtualForm, json, e);
        }
    }

    protected void throwJsonBodyParseFailureException(ActionRuntime runtime, VirtualForm virtualForm, String json, RuntimeException e) {
        coinsHelper.throwJsonBodyParseFailureException(runtime, virtualForm, json, e);
    }

    // -----------------------------------------------------
    //                                             List JSON
    //                                             ---------
    protected void mappingListJsonBody(ActionRuntime runtime, VirtualForm virtualForm, String json, FormMappingOption option) {
        try {
            final ActionFormMeta formMeta = virtualForm.getFormMeta();
            final ParameterizedType pt = formMeta.getListFormParameterParameterizedType().get(); // already checked
            final List<Object> fromJsonList = chooseJsonObjectConvertible(runtime, option).fromJsonParameteried(json, pt);
            acceptJsonRealForm(virtualForm, fromJsonList);
        } catch (RuntimeException e) {
            throwListJsonBodyParseFailureException(runtime, virtualForm, json, e);
        }
    }

    protected void throwListJsonBodyParseFailureException(ActionRuntime runtime, VirtualForm virtualForm, String json, RuntimeException e) {
        coinsHelper.throwListJsonBodyParseFailureException(runtime, virtualForm, json, e);
    }

    // -----------------------------------------------------
    //                                          Assist Logic
    //                                          ------------
    protected void acceptJsonRealForm(VirtualForm virtualForm, Object realForm) {
        virtualForm.acceptRealForm(realForm);
    }

    // ===================================================================================
    //                                                                        Property Set
    //                                                                        ============
    /**
     * @param runtime The runtime information of currently requested action. (NotNull)
     * @param virtualForm The virtual instance of form, which has real form. (NotNull)
     * @param bean The bean instance that has properties for request parameters e.g. form (NullAllowed: if null, do nothing)
     * @param name The name of property for the parameter. (NotNull)
     * @param value The value of the request parameter (NullAllowed, EmptyAllowed)
     * @param pathSb The property path that has nested structure info e.g. sea.land.iksName (NullAllowed: if first level)
     * @param option The option of form mapping. (NotNull)
     * @param parentBean The parent bean of current property's bean, for e.g. map property (NullAllowed: if first level)
     * @param parentName The parent property name of current property, for e.g. map property (NullAllowed: if first level)
     */
    protected void setProperty(ActionRuntime runtime, VirtualForm virtualForm, Object bean, String name, Object value, StringBuilder pathSb,
            FormMappingOption option, Object parentBean, String parentName) { // may be recursively called
        if (bean == null) { // e.g. recursive call and no property
            return;
        }
        doSetProperty(runtime, virtualForm, bean, name, value, parentBean, parentName, pathSb != null ? pathSb : new StringBuilder(),
                option);
    }

    protected void doSetProperty(ActionRuntime runtime, VirtualForm virtualForm, Object bean, String name, Object value, Object parentBean,
            String parentName, StringBuilder pathSb, FormMappingOption option) {
        final int nestedIndex = name.indexOf(NESTED_DELIM); // e.g. sea.mythica
        final int indexedIndex = name.indexOf(INDEXED_DELIM); // e.g. sea[0]
        final int mappedIndex = name.indexOf(MAPPED_DELIM); // e.g. sea(over)
        pathSb.append(pathSb.length() > 0 ? "." : "").append(name);
        if (nestedIndex < 0 && indexedIndex < 0 && mappedIndex < 0) { // as simple
            setSimpleProperty(runtime, virtualForm, bean, name, value, pathSb, option, parentBean, parentName);
        } else {
            final int minIndex = minIndex(minIndex(nestedIndex, indexedIndex), mappedIndex);
            if (minIndex == nestedIndex) { // e.g. sea.mythica
                final String front = name.substring(0, minIndex);
                final Object simpleProperty = prepareSimpleProperty(runtime, bean, front);
                final String rear = name.substring(minIndex + 1);
                setProperty(runtime, virtualForm, simpleProperty, rear, value, pathSb, option, bean, front); // *recursive
            } else if (minIndex == indexedIndex) { // e.g. sea[0]
                final IndexParsedResult result = parseIndex(name.substring(indexedIndex + 1)); // e.g. "0]"
                final int[] resultIndexes = result.indexes;
                final String resultName = result.name;
                final String front = name.substring(0, indexedIndex);
                if (resultName == null || resultName.isEmpty()) { // e.g. sea[0]
                    setIndexedProperty(bean, front, resultIndexes, value, option);
                } else { // e.g. sea[0][0], sea[0].mystic
                    final Object indexedProperty = prepareIndexedProperty(bean, front, resultIndexes, option);
                    setProperty(runtime, virtualForm, indexedProperty, resultName, value, pathSb, option, bean, front); // *recursive
                }
            } else { // map e.g. sea(over)
                final int endIndex = name.indexOf(MAPPED_DELIM2, mappedIndex); // sea(over)
                final String front = name.substring(0, mappedIndex);
                final String middle = name.substring(mappedIndex + 1, endIndex);
                final String rear = name.substring(endIndex + 1);
                setProperty(runtime, virtualForm, bean, front + "." + middle + rear, value, pathSb, option, bean, front); // *recursive
            }
        }
    }

    protected static class IndexParsedResult {
        public int[] indexes = new int[0];
        public String name; // next name e.g. "" from "sea[0]", "1]" from "sea[0][1]", "mystic" from "sea[0].mystic"
    }

    protected int minIndex(int index1, int index2) {
        return coinsHelper.minIndex(index1, index2);
    }

    // ===================================================================================
    //                                                                     Simple Property
    //                                                                     ===============
    protected Object prepareSimpleProperty(ActionRuntime runtime, Object bean, String name) {
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(bean.getClass());
        if (!beanDesc.hasPropertyDesc(name)) {
            return null;
        }
        final PropertyDesc pd = beanDesc.getPropertyDesc(name);
        if (!pd.isReadable()) {
            return null;
        }
        Object value = pd.getValue(bean);
        if (value == null) {
            final Class<?> propertyType = pd.getPropertyType();
            if (!LdiModifierUtil.isAbstract(propertyType)) {
                value = LdiClassUtil.newInstance(propertyType);
                if (pd.isWritable()) {
                    pd.setValue(bean, value);
                }
            } else if (Map.class.isAssignableFrom(propertyType)) {
                value = new HashMap<String, Object>();
                if (pd.isWritable()) {
                    pd.setValue(bean, value);
                }
            }
        }
        return value;
    }

    protected void setSimpleProperty(ActionRuntime runtime, VirtualForm virtualForm, Object bean, String name, Object value,
            StringBuilder pathSb, FormMappingOption option, Object parentBean, String parentName) {
        if (bean instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) bean;
            setMapProperty(map, name, value, option, parentBean, parentName);
            return;
        }
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(bean.getClass());
        if (!beanDesc.hasPropertyDesc(name)) {
            handleUndefinedParameter(bean, name, value, option, beanDesc);
            return;
        }
        final PropertyDesc pd = beanDesc.getPropertyDesc(name);
        if (!pd.isWritable()) {
            handleUndefinedParameter(bean, name, value, option, beanDesc);
            return;
        }
        try {
            mappingToProperty(runtime, virtualForm, bean, name, value, pathSb, option, pd);
        } catch (RuntimeException e) {
            handleMappingFailureException(beanDesc, name, value, pathSb, pd, e);
        }
    }

    protected void handleUndefinedParameter(Object bean, String name, Object value, FormMappingOption option, BeanDesc beanDesc) {
        if (option.isUndefinedParameterError() && !option.getIndefinableParameterSet().contains(name)) {
            throwRequestUndefinedParameterInFormException(bean, name, value, option, beanDesc);
        }
    }

    // -----------------------------------------------------
    //                                   Mapping to Property
    //                                   -------------------
    /**
     * @param runtime The runtime information of currently requested action. (NotNull)
     * @param virtualForm The virtual instance of form, which has real form. (NotNull
     * @param bean The bean instance that has properties for request parameters e.g. form (NotNull)
     * @param name The name of property for the parameter. (NotNull)
     * @param value The value of the request parameter (NullAllowed, EmptyAllowed)
     * @param pathSb The property path that has nested structure info e.g. sea.land.iksName (NotNull)
     * @param option The option of form mapping. (NotNull)
     * @param pd The description for the property. (NotNull)
     */
    protected void mappingToProperty(ActionRuntime runtime, VirtualForm virtualForm, Object bean, String name, Object value,
            StringBuilder pathSb, FormMappingOption option, PropertyDesc pd) {
        final Class<?> propertyType = pd.getPropertyType();
        final Object mappedValue;
        if (propertyType.isArray()) { // fixedly String #for_now e.g. public String[] strArray; so use List<>
            mappedValue = prepareStringArray(value, name, propertyType, option); // plain mapping to array, e.g. JSON not supported
        } else { // e.g. List, ImmutableList, MutableList, String, Integer, ...
            // process of your collections should be first because MutableList is java.util.List
            final Object yourCollection = prepareYourCollection(runtime, virtualForm, bean, name, value, pathSb, option, pd);
            if (yourCollection != null) { // e.g. ImmutableList (Eclipse Collections)
                mappedValue = yourCollection;
            } else { // mainly here
                if (List.class.isAssignableFrom(propertyType)) { // e.g. public List<...> anyList;
                    mappedValue = prepareObjectList(runtime, virtualForm, bean, name, value, pathSb, option, pd);
                } else { // simple object types
                    final Object scalar = prepareObjectScalar(value);
                    if (isJsonParameterProperty(pd)) { // e.g. JsonPrameter for Object
                        mappedValue = parseJsonParameterAsObject(runtime, virtualForm, bean, name, adjustAsJsonString(scalar), pathSb,
                                option, pd);
                    } else { // e.g. String, Integer, LocalDate, CDef, MultipartFormFile, ...
                        mappedValue = prepareNativeValue(runtime, virtualForm, bean, name, scalar, pathSb, option, pd);
                    }
                }
            }
        }
        pd.setValue(bean, mappedValue);
    }

    // -----------------------------------------------------
    //                                        Array Property
    //                                        --------------
    protected String[] prepareStringArray(Object value, String propertyName, Class<?> proeprtyType, FormMappingOption option) { // not null (empty if null)
        final String[] result;
        if (value != null && value instanceof String[]) {
            result = (String[]) value;
        } else {
            result = value != null ? new String[] { value.toString() } : EMPTY_STRING_ARRAY;
        }
        return filterIfSimpleText(result, option, propertyName, proeprtyType);
    }

    // -----------------------------------------------------
    //                                         List Property
    //                                         -------------
    protected List<?> prepareObjectList(ActionRuntime runtime, VirtualForm virtualForm, Object bean, String name, Object value,
            StringBuilder pathSb, FormMappingOption option, PropertyDesc pd) {
        final List<?> mappedValue;
        if (isJsonParameterProperty(pd)) { // e.g. public List<SeaJsonBean> jsonList;
            final Object scalar = prepareObjectScalar(value);
            mappedValue = parseJsonParameterAsList(runtime, virtualForm, bean, name, adjustAsJsonString(scalar), pathSb, option, pd);
        } else { // e.g. List<String>, List<CDef.MemberStatus>
            mappedValue = prepareSimpleElementList(virtualForm, bean, name, value, pathSb, option, pd);
        }
        return mappedValue;
    }

    protected List<? extends Object> prepareSimpleElementList(VirtualForm virtualForm, Object bean, String name, Object value,
            StringBuilder pathSb, FormMappingOption option, PropertyDesc pd) {
        final Class<?> propertyType = pd.getPropertyType();
        final List<String> strList = prepareStringList(value, name, propertyType, option);
        if (pd.isParameterized()) {
            final Class<?> elementType = pd.getParameterizedClassDesc().getGenericFirstType();
            final List<Object> mappedList = strList.stream().map(exp -> { // already filtered
                return convertToNativeIfPossible(bean, name, exp, elementType, option);
            }).collect(Collectors.toList());
            return Collections.unmodifiableList(mappedList);
        }
        return strList;
    }

    protected List<String> prepareStringList(Object value, String propertyName, Class<?> propertyType, FormMappingOption option) {
        final String[] ary = prepareStringArray(value, propertyName, propertyType, option); // with filter
        if (ary.length == 0) {
            return Collections.emptyList();
        }
        final boolean absList = LdiModifierUtil.isAbstract(propertyType); // e.g. List
        final List<String> valueList = absList ? new ArrayList<String>(ary.length) : newStringList(propertyType);
        for (String element : ary) {
            valueList.add(element);
        }
        return Collections.unmodifiableList(valueList);
    }

    @SuppressWarnings("unchecked")
    protected List<String> newStringList(Class<?> propertyType) {
        return (List<String>) LdiClassUtil.newInstance(propertyType);
    }

    // -----------------------------------------------------
    //                                      Your Collections
    //                                      ----------------
    protected Object prepareYourCollection(ActionRuntime runtime, VirtualForm virtualForm, Object bean, String name, Object value,
            StringBuilder pathSb, FormMappingOption option, PropertyDesc pd) {
        final List<FormYourCollectionResource> yourCollections = option.getYourCollections();
        if (yourCollections.isEmpty()) {
            return null; // no settings of your collections
        }
        final Class<?> propertyType = pd.getPropertyType();
        for (FormYourCollectionResource yourCollection : yourCollections) {
            if (!propertyType.equals(yourCollection.getYourType())) { // just type in form mapping (to avoid complexity)
                continue;
            }
            final List<?> objectList = prepareObjectList(runtime, virtualForm, bean, name, value, pathSb, option, pd);
            final Iterable<? extends Object> applied = yourCollection.getYourCollectionCreator().apply(objectList);
            final Object mappedValue;
            if (applied instanceof List<?>) {
                mappedValue = applied;
            } else {
                final List<Object> newList = new ArrayList<>();
                for (Object element : applied) {
                    newList.add(element);
                }
                mappedValue = newList;
            }
            return mappedValue;
        }
        return null; // is not your collections
    }

    // -----------------------------------------------------
    //                                       Scalar Property
    //                                       ---------------
    protected Object prepareObjectScalar(Object value) { // null allowed
        if (value != null && value instanceof String[] && ((String[]) value).length > 0) {
            return ((String[]) value)[0];
        } else {
            return value;
        }
    }

    // -----------------------------------------------------
    //                                          Map Property
    //                                          ------------
    protected void setMapProperty(Map<String, Object> map, String name, Object value, FormMappingOption option, Object parentBean,
            String parentName) {
        final boolean strArray = isMapValueStringArray(parentBean, parentName);
        final Object registered;
        if (value instanceof String[]) {
            final String[] values = (String[]) value;
            registered = strArray ? values : values.length > 0 ? values[0] : null;
        } else {
            registered = strArray ? (value != null ? new String[] { value.toString() } : EMPTY_STRING_ARRAY) : value;
        }
        map.put(name, filterIfSimpleText(registered, option, name, map.getClass()));
    }

    protected boolean isMapValueStringArray(Object parentBean, String parentName) {
        if (parentBean == null) {
            return false;
        }
        final PropertyDesc pd = BeanDescFactory.getBeanDesc(parentBean.getClass()).getPropertyDesc(parentName);
        final Class<?> valueClassOfMap = pd.getValueClassOfMap();
        return valueClassOfMap != null && valueClassOfMap.isArray() && String[].class.isAssignableFrom(valueClassOfMap);
    }

    // -----------------------------------------------------
    //                                        JSON Parameter
    //                                        --------------
    protected String adjustAsJsonString(Object value) { // not null (empty if null)
        if (value != null) {
            if (value instanceof String) {
                return (String) value;
            } else {
                return value.toString();
            }
        } else {
            return "";
        }
    }

    protected boolean isJsonParameterProperty(PropertyDesc pd) {
        final Class<JsonParameter> annoType = JsonParameter.class;
        final Field field = pd.getField();
        if (field != null && field.getAnnotation(annoType) != null) {
            return true;
        }
        if (field != null && !LdiModifierUtil.isPublic(field)) { // not public field
            if (pd.hasReadMethod()) {
                final Method readMethod = pd.getReadMethod();
                if (readMethod != null && readMethod.getAnnotation(annoType) != null) {
                    return true;
                }
            }
            if (pd.hasWriteMethod()) {
                final Method writeMethod = pd.getWriteMethod();
                if (writeMethod != null && writeMethod.getAnnotation(annoType) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    protected List<?> parseJsonParameterAsList(ActionRuntime runtime, VirtualForm virtualForm, Object bean, String name, String json,
            StringBuilder pathSb, FormMappingOption option, PropertyDesc pd) {
        if (!pd.isParameterized()) { // e.g. public List anyList;
            throwListJsonPropertyNonGenericException(bean, name, json, pd); // program mistake
            return null; // unreachable
        }
        final ParameterizedClassDesc paramedDesc = pd.getParameterizedClassDesc();
        final Type plainType = paramedDesc.getParameterizedType(); // not null
        if (!(plainType instanceof ParameterizedType)) { // generic array type? anyway check it
            throwListJsonPropertyNonParameterizedException(bean, name, json, pd, plainType); // program mistake
            return null; // unreachable
        }
        final ParameterizedType paramedType = (ParameterizedType) plainType;
        if (Object.class.equals(paramedDesc.getGenericFirstType())) { // e.g. public List<?> beanList;
            throwListJsonPropertyGenericNotScalarException(bean, name, json, pd, paramedType);
            return null; // unreachable
        }
        try {
            return (List<?>) chooseJsonObjectConvertible(runtime, option).fromJsonParameteried(json, paramedType); // e.g. public List<SeaBean> beanList;
        } catch (RuntimeException e) {
            throwListJsonParameterParseFailureException(bean, name, json, paramedType, e);
            return null; // unreachable
        }
    }

    protected Object parseJsonParameterAsObject(ActionRuntime runtime, VirtualForm virtualForm, Object bean, String name, String json,
            StringBuilder pathSb, FormMappingOption option, PropertyDesc pd) {
        final Class<?> propertyType = pd.getPropertyType();
        try {
            return chooseJsonObjectConvertible(runtime, option).fromJson(json, propertyType);
        } catch (RuntimeException e) {
            throwJsonParameterParseFailureException(bean, name, json, propertyType, e);
            return null; // unreachable
        }
    }

    // -----------------------------------------------------
    //                                             List JSON
    //                                             ---------
    protected void throwListJsonPropertyNonGenericException(Object bean, String name, String json, PropertyDesc pd) {
        coinsHelper.throwListJsonPropertyNonGenericException(bean, name, json, pd);
    }

    protected void throwListJsonPropertyNonParameterizedException(Object bean, String name, String json, PropertyDesc pd, Type plainType) {
        coinsHelper.throwListJsonPropertyNonParameterizedException(bean, name, json, pd, plainType);
    }

    protected void throwListJsonPropertyGenericNotScalarException(Object bean, String name, String json, PropertyDesc pd,
            ParameterizedType paramedType) {
        coinsHelper.throwListJsonPropertyGenericNotScalarException(bean, name, json, pd, paramedType);
    }

    protected void throwListJsonParameterParseFailureException(Object bean, String name, String json, Type propertyType,
            RuntimeException e) {
        coinsHelper.throwListJsonParameterParseFailureException(bean, name, json, propertyType, e);
    }

    // -----------------------------------------------------
    //                                             Bean JSON
    //                                             ---------
    protected void throwJsonParameterParseFailureException(Object bean, String name, String json, Class<?> propertyType,
            RuntimeException e) {
        coinsHelper.throwJsonParameterParseFailureException(bean, name, json, propertyType, e);
    }

    // -----------------------------------------------------
    //                                        Classification
    //                                        --------------
    protected boolean isClassificationProperty(Class<?> propertyType) {
        return coinsHelper.isClassificationProperty(propertyType);
    }

    protected Classification toVerifiedClassification(Object bean, String name, Object code, Class<?> propertyType) {
        return coinsHelper.toVerifiedClassification(bean, name, code, propertyType);
    }

    // -----------------------------------------------------
    //                                       Property Native
    //                                       ---------------
    protected Object prepareNativeValue(ActionRuntime runtime, VirtualForm virtualForm, Object bean, String name, Object exp,
            StringBuilder pathSb, FormMappingOption option, PropertyDesc pd) {
        final Class<?> propertyType = pd.getPropertyType();
        try {
            final Object filtered = filterIfSimpleText(exp, option, name, propertyType);
            return convertToNativeIfPossible(bean, name, filtered, propertyType, option);
        } catch (RuntimeException e) {
            if (isTypeFailureException(e)) {
                virtualForm.acceptTypeFailure(pathSb.toString(), exp); // to render failure value
                handleTypeFailure(virtualForm, bean, name, exp, pd, propertyType, pathSb, e);
                return null;
            } else {
                throw e;
            }
        }
    }

    protected Object convertToNativeIfPossible(Object bean, String name, Object exp, Class<?> propertyType, FormMappingOption option) {
        // not to depend on conversion logic in BeanDesc
        final Object converted;
        if (propertyType.isPrimitive()) {
            if (propertyType.equals(boolean.class) && isCheckboxOn(exp)) {
                converted = true;
            } else {
                converted = DfTypeUtil.toWrapper(exp, propertyType);
            }
        } else if (String.class.isAssignableFrom(propertyType)) {
            if (option.isKeepEmptyStringParameter()) {
                converted = exp != null ? exp : ""; // empty string as default
            } else { // filter empty to null or plain
                return exp instanceof String && ((String) exp).isEmpty() ? null : exp;
            }
        } else if (Number.class.isAssignableFrom(propertyType)) {
            converted = DfTypeUtil.toNumber(exp, propertyType);
            // old date types are unsupported for LocalDate invitation
            //} else if (Timestamp.class.isAssignableFrom(propertyType)) {
            //    filtered = DfTypeUtil.toTimestamp(exp);
            //} else if (Time.class.isAssignableFrom(propertyType)) {
            //    filtered = DfTypeUtil.toTime(exp);
            //} else if (java.util.Date.class.isAssignableFrom(propertyType)) {
            //    filtered = DfTypeUtil.toDate(exp);
        } else if (LocalDate.class.isAssignableFrom(propertyType)) { // #date_parade
            // #hope can specify date, date-time pattern by FormMappingOption by jflute (2017/10/30)
            converted = DfTypeUtil.toLocalDate(exp); // as flexible parsing
        } else if (LocalDateTime.class.isAssignableFrom(propertyType)) {
            converted = DfTypeUtil.toLocalDateTime(exp); // as flexible parsing
        } else if (LocalTime.class.isAssignableFrom(propertyType)) {
            converted = DfTypeUtil.toLocalTime(exp); // as flexible parsing
        } else if (ZonedDateTime.class.isAssignableFrom(propertyType)) {
            converted = toZonedDateTime(exp, option);
        } else if (Boolean.class.isAssignableFrom(propertyType)) {
            converted = toBoolean(exp, option);
        } else if (isClassificationProperty(propertyType)) { // means CDef
            converted = toVerifiedClassification(bean, name, exp, propertyType);
        } else { // e.g. multipart form file or unsupported type
            converted = exp;
        }
        return converted;
    }

    protected Object toZonedDateTime(Object exp, FormMappingOption option) {
        final Object converted;
        if (exp == null || (exp instanceof String && ((String) exp).isEmpty())) {
            converted = null;
        } else {
            // DfTypeUtil.toZonedDateTime() needs to be adjusted at DBFlute-1.1.3 so parse by myself for now
            // (in the meantime, zoned date-time does not need flexble parsing because of almost transfer expression)
            converted = ZonedDateTime.parse(exp.toString(), getZonedDateTimeFormatter(option));
        }
        return converted;
    }

    protected DateTimeFormatter getZonedDateTimeFormatter(FormMappingOption option) {
        return option.getZonedDateTimeFormatter().orElseGet(() -> DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    protected Object toBoolean(Object exp, FormMappingOption option) {
        final Object converted;
        if (isCheckboxOn(exp)) {
            converted = true;
        } else {
            if (exp instanceof String && ((String) exp).isEmpty()) { // pinpoint patch
                converted = null; // toBoolean("") before DBFlute-1.1.3 throws exception so avoid it
            } else {
                converted = DfTypeUtil.toBoolean(exp);
            }
        }
        return converted;
    }

    protected boolean isCheckboxOn(Object exp) {
        return "on".equals(exp);
    }

    // -----------------------------------------------------
    //                                          Type Failure
    //                                          ------------
    protected void handleTypeFailure(VirtualForm virtualForm, Object bean, String name, Object exp, PropertyDesc pd, Class<?> propertyType,
            StringBuilder pathSb, RuntimeException cause) {
        final ValidateTypeFailure annotation = extractTypeFailureAnnotation(pd);
        if (annotation != null) {
            if (ThreadCacheContext.exists()) { // just in case
                saveTypeFailureBean(virtualForm, bean, name, exp, propertyType, pathSb, annotation, cause);
            } else { // basically no way
                logger.debug("*Not found the thread cache for validation of type failure: {}", pathSb, cause);
            }
            return;
        } else {
            throw cause;
        }
    }

    protected ValidateTypeFailure extractTypeFailureAnnotation(PropertyDesc pd) {
        final Field field = pd.getField();
        if (field != null) {
            final ValidateTypeFailure annotation = field.getAnnotation(ValidateTypeFailure.class);
            if (annotation != null) {
                return annotation;
            }
        }
        final Method readMethod = pd.getReadMethod();
        if (readMethod != null) {
            final ValidateTypeFailure annotation = readMethod.getAnnotation(ValidateTypeFailure.class);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    protected Object saveTypeFailureBean(VirtualForm virtualForm, Object bean, String name, Object exp, Class<?> propertyType,
            StringBuilder pathSb, ValidateTypeFailure annotation, RuntimeException cause) {
        final String propertyPath = pathSb.toString();
        showTypeFailure(propertyPath, propertyType, exp, cause);
        prepareTypeFailureBean(virtualForm).register(createTypeFailureElement(bean, propertyPath, propertyType, exp, annotation, cause));
        return null; // set null to form here, checked later by thread local
    }

    protected void showTypeFailure(String propertyPath, Class<?> propertyType, Object exp, RuntimeException cause) {
        final String causeExp = cause.getClass().getSimpleName();
        logger.debug("...Registering type failure as validation: {}({}) '{}' {}", propertyPath, propertyType, exp, causeExp);
    }

    protected TypeFailureBean prepareTypeFailureBean(VirtualForm virtualForm) { // thread cache already checked here
        final Class<?> keyType = virtualForm.getFormMeta().getRootFormType();
        TypeFailureBean typeFailure = (TypeFailureBean) ThreadCacheContext.findValidatorTypeFailure(keyType);
        if (typeFailure == null) {
            typeFailure = new TypeFailureBean();
            ThreadCacheContext.registerValidatorTypeFailure(keyType, typeFailure);
        }
        return typeFailure;
    }

    protected TypeFailureElement createTypeFailureElement(Object bean, String propertyPath, Class<?> propertyType, Object exp,
            ValidateTypeFailure annotation, RuntimeException cause) {
        return new TypeFailureElement(propertyPath, propertyType, exp, annotation, cause, () -> {
            throwTypeFailureBadRequest(bean, propertyPath, propertyType, exp, cause);
        });
    }

    protected void throwTypeFailureBadRequest(Object bean, String propertyPath, Class<?> propertyType, Object exp, RuntimeException cause) {
        coinsHelper.throwTypeFailureBadRequest(bean, propertyPath, propertyType, exp, cause);
    }

    // -----------------------------------------------------
    //                                       Mapping Failure
    //                                       ---------------
    protected void handleMappingFailureException(Object bean, String name, Object value, StringBuilder pathSb, PropertyDesc pd,
            RuntimeException e) {
        coinsHelper.handleMappingFailureException(bean, name, value, pathSb, pd, e);
    }

    protected boolean isTypeFailureException(Throwable cause) { // except classification here
        return coinsHelper.isTypeFailureException(cause);
    }

    // -----------------------------------------------------
    //                                    Filter Simple Text
    //                                    ------------------
    protected <OBJ> OBJ filterIfSimpleText(OBJ value, FormMappingOption option, String propertyName, Class<?> propertyType) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            final String str = (String) value;
            if (option.getSimpleTextParameterFilter().isPresent()) { // basically false
                // not use map() to avoid many new instances at main route (not here)
                final FormSimpleTextParameterFilter filter = option.getSimpleTextParameterFilter().get();
                final FormSimpleTextParameterMeta meta = createFormSimpleTextParameterMeta(propertyName, propertyType);
                @SuppressWarnings("unchecked")
                final OBJ filtered = (OBJ) filter.filter(str, meta);
                return filtered;
            } else { // mainly here
                return value;
            }
        } else if (value instanceof String[]) {
            final String[] ary = (String[]) value;
            option.getSimpleTextParameterFilter().ifPresent(filter -> {
                final FormSimpleTextParameterMeta meta = createFormSimpleTextParameterMeta(propertyName, propertyType);
                int index = 0;
                for (String element : ary) {
                    if (element != null) { // just in case
                        ary[index] = filter.filter(element, meta);
                    }
                    ++index;
                }
            });
            @SuppressWarnings("unchecked")
            final OBJ resultObj = (OBJ) ary;
            return resultObj;
        } else {
            return value;
        }
    }

    protected FormSimpleTextParameterMeta createFormSimpleTextParameterMeta(String propertyName, Class<?> propertyType) {
        return new FormSimpleTextParameterMeta(propertyName, propertyType);
    }

    // ===================================================================================
    //                                                                         Parse Index
    //                                                                         ===========
    protected IndexParsedResult parseIndex(String name) { // name is e.g. "0]" or "0][0]" or "0].mystic"
        final IndexParsedResult parseResult;
        try {
            parseResult = doParseIndex(name);
        } catch (NumberFormatException e) {
            throwIndexedPropertyNonNumberIndexException(name, e);
            return null; // unreachable
        }
        checkIndexedPropertySize(name, parseResult);
        return parseResult;
    }

    protected IndexParsedResult doParseIndex(String name) { // name is e.g. "0]" or "0][1]" or "0].mystic"
        IndexParsedResult result = new IndexParsedResult();
        while (true) {
            int endIndex = name.indexOf(INDEXED_DELIM2);
            if (endIndex < 0) {
                throw new IllegalArgumentException(INDEXED_DELIM2 + " is not found in " + name);
            }
            result.indexes = LdiArrayUtil.add(result.indexes, Integer.valueOf(name.substring(0, endIndex)).intValue());
            name = name.substring(endIndex + 1);
            if (name.length() == 0) {
                break;
            } else if (name.charAt(0) == INDEXED_DELIM) { // e.g. "[1]" from "0][1]"
                name = name.substring(1); // e.g. "1]"
            } else if (name.charAt(0) == NESTED_DELIM) { // e.g. ".mystic" from "0].mystic"
                name = name.substring(1); // e.g. "mystic"
                break;
            } else {
                throw new IllegalArgumentException(name);
            }
        }
        result.name = name;
        return result;
    }

    protected void throwIndexedPropertyNonNumberIndexException(String name, NumberFormatException e) {
        coinsHelper.throwIndexedPropertyNonNumberIndexException(name, e);
    }

    protected void checkIndexedPropertySize(String name, IndexParsedResult parseResult) {
        final int[] indexes = parseResult.indexes;
        if (indexes.length == 0) {
            return;
        }
        final int indexedPropertySizeLimit = getIndexedPropertySizeLimit();
        for (int index : indexes) {
            if (index < 0) {
                throwIndexedPropertyMinusIndexException(name, index);
            }
            if (index > indexedPropertySizeLimit) {
                throwIndexedPropertySizeOverException(name, index);
            }
        }
    }

    protected int getIndexedPropertySizeLimit() {
        return getAdjustmentProvider().provideIndexedPropertySizeLimit();
    }

    protected void throwIndexedPropertyMinusIndexException(String name, int index) {
        coinsHelper.throwIndexedPropertyMinusIndexException(name, index);
    }

    protected void throwIndexedPropertySizeOverException(String name, int index) {
        coinsHelper.throwIndexedPropertySizeOverException(name, index);
    }

    // ===================================================================================
    //                                                                    Indexed Property
    //                                                                    ================
    // -----------------------------------------------------
    //                                  Set Indexed Property
    //                                  --------------------
    protected void setIndexedProperty(Object bean, String name, int[] indexes, Object value, FormMappingOption option) { // e.g. sea[0]
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(bean.getClass());
        if (!beanDesc.hasPropertyDesc(name)) {
            return;
        }
        final PropertyDesc pd = beanDesc.getPropertyDesc(name);
        if (!pd.isWritable()) {
            return;
        }
        if (value.getClass().isArray() && Array.getLength(value) > 0) {
            value = Array.get(value, 0);
        }
        final Class<?> propertyType = pd.getPropertyType();
        if (propertyType.isArray()) {
            Object array = pd.getValue(bean);
            final Class<?> elementType = getArrayElementType(propertyType, indexes.length);
            if (array == null) {
                int[] newIndexes = new int[indexes.length];
                newIndexes[0] = indexes[0] + 1;
                array = Array.newInstance(elementType, newIndexes);
            }
            array = expandArray(array, indexes, elementType);
            pd.setValue(bean, array);
            setArrayValue(array, indexes, value);
        } else { // e.g. List, ImmutableList, MutableList
            // process of your collections should be first because MutableList is java.util.List 
            final Optional<FormYourCollectionResource> yourCollection = findListableYourCollection(pd, option);
            if (yourCollection.isPresent()) { // e.g. ImmutableList, MutableList
                doSetIndexedPropertyListable(bean, name, indexes, value, beanDesc, pd, option, newList -> {
                    @SuppressWarnings("unchecked")
                    final List<Object> filtered = (List<Object>) yourCollection.get().getYourCollectionCreator().apply(newList);
                    return filtered;
                });
            } else if (List.class.isAssignableFrom(propertyType)) {
                doSetIndexedPropertyListable(bean, name, indexes, value, beanDesc, pd, option, Function.identity());
            } else {
                throwIndexedPropertyNotListArrayException(beanDesc, pd);
            }
        }
    }

    protected void doSetIndexedPropertyListable(Object bean, String name, int[] indexes, Object value, BeanDesc beanDesc, PropertyDesc pd,
            FormMappingOption option, Function<List<Object>, List<Object>> listInstanceFilter) {
        handleIndexedPropertyListable(bean, name, indexes, beanDesc, pd, option, listInstanceFilter, list -> {
            list.set(indexes[indexes.length - 1], value);
            return null; // unused
        });
    }

    protected void setArrayValue(Object array, int[] indexes, Object value) {
        coinsHelper.setArrayValue(array, indexes, value);
    }

    // -----------------------------------------------------
    //                              Prepare Indexed Property
    //                              ------------------------
    protected Object prepareIndexedProperty(Object bean, String name, int[] indexes, FormMappingOption option) {
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(bean.getClass());
        if (!beanDesc.hasPropertyDesc(name)) {
            return null;
        }
        final PropertyDesc pd = beanDesc.getPropertyDesc(name);
        if (!pd.isReadable()) {
            return null;
        }
        final Class<?> propertyType = pd.getPropertyType();
        if (propertyType.isArray()) {
            Object array = pd.getValue(bean);
            final Class<?> elementType = getArrayElementType(propertyType, indexes.length);
            if (array == null) {
                int[] newIndexes = new int[indexes.length];
                newIndexes[0] = indexes[0] + 1;
                array = Array.newInstance(elementType, newIndexes);
            }
            array = expandArray(array, indexes, elementType);
            pd.setValue(bean, array);
            return getArrayValue(array, indexes, elementType);
        } else { // e.g. List, ImmutableList, MutableList
            // process of your collections should be first because MutableList is java.util.List 
            final Optional<FormYourCollectionResource> yourCollection = findListableYourCollection(pd, option);
            if (yourCollection.isPresent()) { // e.g. ImmutableList, MutableList
                return doPrepareIndexedPropertyListable(bean, name, indexes, beanDesc, pd, option, newList -> {
                    @SuppressWarnings("unchecked")
                    final List<Object> filtered = (List<Object>) yourCollection.get().getYourCollectionCreator().apply(newList);
                    return filtered;
                });
            } else if (List.class.isAssignableFrom(propertyType)) { // e.g. List (can be ArrayList)
                return doPrepareIndexedPropertyListable(bean, name, indexes, beanDesc, pd, option, Function.identity());
            } else { // cannot treat it
                throwIndexedPropertyNotListArrayException(beanDesc, pd);
                return null; // unreachable
            }
        }
    }

    protected Object getArrayValue(Object array, int[] indexes, Class<?> elementType) {
        return coinsHelper.getArrayValue(array, indexes, elementType);
    }

    protected Object doPrepareIndexedPropertyListable(Object bean, String name, int[] indexes, BeanDesc beanDesc, PropertyDesc pd,
            FormMappingOption option, Function<List<Object>, List<Object>> listInstanceFilter) {
        return handleIndexedPropertyListable(bean, name, indexes, beanDesc, pd, option, listInstanceFilter, list -> {
            return list.get(indexes[indexes.length - 1]);
        });
    }

    // -----------------------------------------------------
    //                                        Indexed Helper
    //                                        --------------
    protected Optional<FormYourCollectionResource> findListableYourCollection(PropertyDesc pd, FormMappingOption option) {
        final Class<?> propertyType = pd.getPropertyType();
        final List<FormYourCollectionResource> yourCollections = option.getYourCollections();
        return yourCollections.stream() // checking defined type and instance type
                .filter(res -> propertyType.equals(res.getYourType())) // just type in form mapping (to avoid complexity)
                .filter(res -> res.getYourCollectionCreator().apply(Collections.emptyList()) instanceof List)
                .findFirst(); // basically only-one here (if no duplicate type specified)
    }

    @SuppressWarnings("unchecked")
    protected <RESULT> RESULT handleIndexedPropertyListable(Object bean, String name, int[] indexes, BeanDesc beanDesc, PropertyDesc pd,
            FormMappingOption option, Function<List<Object>, List<Object>> listInstanceFilter,
            Function<List<Object>, RESULT> listProcessHandler) {
        List<Object> list = (List<Object>) pd.getValue(bean);
        if (list == null) {
            list = listInstanceFilter.apply(new ArrayList<Object>(Math.max(50, indexes[0])));
            pd.setValue(bean, list); // and initialize field value
        }
        final boolean certainlyCanAdd = list instanceof ArrayList<?>; // mainly true
        ParameterizedClassDesc paramDesc = pd.getParameterizedClassDesc();

        // cannot add() if e.g. ImmutableList, so prepare mutable working list here (and switch later)
        List<Object> workingList = certainlyCanAdd ? list : DfCollectionUtil.newArrayList(list);

        for (int i = 0; i < indexes.length; i++) {
            // remove check of List because it may be ImmutableList
            //if (paramDesc == null || !paramDesc.isParameterizedClass() || !List.class.isAssignableFrom(paramDesc.getRawClass())) {
            if (paramDesc == null || !paramDesc.isParameterizedClass()) {
                // what is this? obviously unneeded so comment it out by jflute (2017/12/13)
                //final StringBuilder sb = new StringBuilder();
                //for (int j = 0; j <= i; j++) {
                //    sb.append("[").append(indexes[j]).append("]");
                //}
                throwIndexedPropertyNonParameterizedListException(beanDesc, pd);
            }
            final int size = workingList.size();
            paramDesc = paramDesc.getArguments()[0];
            for (int j = size; j <= indexes[i]; j++) {
                if (i == indexes.length - 1) {
                    workingList.add(LdiClassUtil.newInstance(convertArrayClass(paramDesc.getRawClass())));
                } else {
                    workingList.add(new ArrayList<Object>());
                }
            }
            if (i < indexes.length - 1) {
                workingList = (List<Object>) workingList.get(indexes[i]);
            }
        }
        final RESULT result = listProcessHandler.apply(workingList);
        if (!certainlyCanAdd) {
            pd.setValue(bean, listInstanceFilter.apply(workingList));
        }
        return result;
    }

    protected void throwIndexedPropertyNonParameterizedListException(BeanDesc beanDesc, PropertyDesc pd) {
        coinsHelper.throwIndexedPropertyNonParameterizedListException(beanDesc, pd);
    }

    protected void throwIndexedPropertyNotListArrayException(BeanDesc beanDesc, PropertyDesc pd) {
        coinsHelper.throwIndexedPropertyNotListArrayException(beanDesc, pd);
    }

    // -----------------------------------------------------
    //                                          Array Helper
    //                                          ------------
    protected Class<?> getArrayElementType(Class<?> clazz, int depth) {
        return coinsHelper.getArrayElementType(clazz, depth);
    }

    protected Object expandArray(Object array, int[] indexes, Class<?> elementType) {
        return coinsHelper.expandArray(array, indexes, elementType);
    }

    protected Class<?> convertArrayClass(Class<?> clazz) {
        return coinsHelper.convertArrayClass(clazz);
    }

    // ===================================================================================
    //                                                                         JSON Assist
    //                                                                         ===========
    protected JsonObjectConvertible chooseJsonObjectConvertible(ActionRuntime runtime, FormMappingOption option) {
        return coinsHelper.chooseJsonObjectConvertible(runtime, option);
    }

    // ===================================================================================
    //                                                                        Client Error
    //                                                                        ============
    protected void buildClientErrorHeader(StringBuilder sb, String title, Object bean, String name, Object value, Type propertyType,
            String challengeDisp) {
        coinsHelper.buildClientErrorHeader(sb, title, bean, name, value, propertyType, challengeDisp);
    }

    protected void throwRequestPropertyMappingFailureException(String msg) {
        coinsHelper.throwRequestPropertyMappingFailureException(msg);
    }

    protected void throwRequestPropertyMappingFailureException(String msg, RuntimeException cause) {
        coinsHelper.throwRequestPropertyMappingFailureException(msg, cause);
    }

    protected void throwRequestUndefinedParameterInFormException(Object bean, String name, Object value, FormMappingOption option,
            BeanDesc beanDesc) {
        coinsHelper.throwRequestUndefinedParameterInFormException(bean, name, value, option, beanDesc);
    }

    // ===================================================================================
    //                                                                  Assistant Director
    //                                                                  ==================
    protected FormMappingOption adjustFormMapping() {
        final FormMappingOption option = getAdjustmentProvider().adjustFormMapping();
        return option != null ? option : NULLOBJ_FORM_MAPPING_OPTION;
    }

    protected ActionAdjustmentProvider getAdjustmentProvider() {
        return assistWebDirection().assistActionAdjustmentProvider();
    }

    protected FwWebDirection assistWebDirection() {
        return assistantDirector.assistWebDirection();
    }
}
