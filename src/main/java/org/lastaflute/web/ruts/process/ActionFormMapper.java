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
package org.lastaflute.web.ruts.process;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.jdbc.Classification;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.core.util.GenericTypeRef;
import org.lastaflute.core.util.LaDBFluteUtil;
import org.lastaflute.core.util.LaDBFluteUtil.ClassificationUnknownCodeException;
import org.lastaflute.di.core.aop.javassist.AspectWeaver;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.ParameterizedClassDesc;
import org.lastaflute.di.helper.beans.PropertyDesc;
import org.lastaflute.di.helper.beans.exception.BeanIllegalPropertyException;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;
import org.lastaflute.di.util.LdiArrayUtil;
import org.lastaflute.di.util.LdiClassUtil;
import org.lastaflute.di.util.LdiModifierUtil;
import org.lastaflute.web.api.JsonParameter;
import org.lastaflute.web.callback.ActionRuntime;
import org.lastaflute.web.direction.FwWebDirection;
import org.lastaflute.web.exception.IndexedPropertyNonParameterizedListException;
import org.lastaflute.web.exception.IndexedPropertyNotListArrayException;
import org.lastaflute.web.exception.RequestClassifiationConvertFailureException;
import org.lastaflute.web.exception.RequestJsonParseFailureException;
import org.lastaflute.web.exception.RequestPropertyMappingFailureException;
import org.lastaflute.web.path.ActionAdjustmentProvider;
import org.lastaflute.web.path.FormMappingOption;
import org.lastaflute.web.ruts.VirtualActionForm;
import org.lastaflute.web.ruts.config.ActionFormMeta;
import org.lastaflute.web.ruts.config.ActionFormProperty;
import org.lastaflute.web.ruts.config.ModuleConfig;
import org.lastaflute.web.ruts.config.analyzer.ExecuteArgAnalyzer;
import org.lastaflute.web.ruts.multipart.MultipartRequestHandler;
import org.lastaflute.web.ruts.multipart.MultipartRequestWrapper;
import org.lastaflute.web.ruts.multipart.MultipartResourceProvider;
import org.lastaflute.web.ruts.process.exception.ActionFormPopulateFailureException;
import org.lastaflute.web.ruts.process.exception.RequestUndefinedParameterInFormException;
import org.lastaflute.web.servlet.filter.RequestLoggingFilter.RequestClientErrorException;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author modified by jflute (originated in Seasar and Struts)
 */
public class ActionFormMapper {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final String MULTIPART_CONTENT_TYPE = "multipart/form-data";
    protected static final String CACHE_KEY_DECODED_PROPERTY_MAP = "requestProcessor.decodedPropertyMap";
    protected static final String CACHE_KEY_URL_PARAM_NAMES_CACHED_SET = "requestProcessor.urlParamNames.cachedSet";
    protected static final String CACHE_KEY_URL_PARAM_NAMES_UNIQUE_METHOD = "requestProcessor.urlParamNames.uniqueMethod";
    protected static final char NESTED_DELIM = '.';
    protected static final char INDEXED_DELIM = '[';
    protected static final char INDEXED_DELIM2 = ']';
    protected static final char MAPPED_DELIM = '(';
    protected static final char MAPPED_DELIM2 = ')';
    protected static final String[] EMPTY_STRING_ARRAY = new String[0];
    protected static final String LF = "\n";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ModuleConfig moduleConfig;
    protected final FwAssistantDirector assistantDirector;
    protected final RequestManager requestManager;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionFormMapper(ModuleConfig moduleConfig, FwAssistantDirector assistantDirector, RequestManager requestManager) {
        this.moduleConfig = moduleConfig;
        this.assistantDirector = assistantDirector;
        this.requestManager = requestManager;
    }

    // ===================================================================================
    //                                                                            Populate
    //                                                                            ========
    public void populateParameter(ActionRuntime runtime, OptionalThing<VirtualActionForm> optForm) throws IOException, ServletException {
        if (!optForm.isPresent()) {
            return;
        }
        final VirtualActionForm virtualActionForm = optForm.get();
        if (handleJsonBody(runtime, virtualActionForm)) {
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
        final Object realForm = virtualActionForm.getRealForm();
        final Map<String, Object> params = getAllParameters(multipartHandler);
        for (Entry<String, Object> entry : params.entrySet()) {
            final String name = entry.getKey();
            final Object value = entry.getValue();
            try {
                setProperty(realForm, name, value, null, null);
            } catch (Throwable cause) {
                handleIllegalPropertyPopulateException(realForm, name, value, runtime, cause); // adjustment here
            }
        }
    }

    protected boolean isMultipartRequest() {
        return requestManager.getContentType().map(contentType -> {
            return contentType.startsWith(MULTIPART_CONTENT_TYPE) && requestManager.isPost();
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

    protected Map<String, Object> getAllParameters(MultipartRequestHandler multipartHandler) {
        final HttpServletRequest request = requestManager.getRequest();
        final Map<String, Object> paramMap = new LinkedHashMap<String, Object>();
        final Enumeration<String> em = request.getParameterNames();
        while (em.hasMoreElements()) {
            final String name = em.nextElement();
            paramMap.put(name, request.getParameterValues(name));
        }
        if (multipartHandler != null) {
            paramMap.putAll(multipartHandler.getAllElements());
        }
        return paramMap;
    }

    protected void handleIllegalPropertyPopulateException(Object form, String name, Object value, ActionRuntime runtime, Throwable cause)
            throws ServletException {
        if (isRequestClientErrorException(cause)) { // for indexed property check
            throw new ServletException(cause);
        }
        throwActionFormPopulateFailureException(form, name, value, runtime, cause);
    }

    protected boolean isRequestClientErrorException(Throwable cause) {
        return cause instanceof RequestClientErrorException;
    }

    protected void throwActionFormPopulateFailureException(Object form, String name, Object value, ActionRuntime runtime, Throwable cause) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Failed to populate the parameter to the form.");
        br.addItem("Action Runtime");
        br.addElement(runtime);
        br.addItem("Action Form");
        br.addElement(form);
        br.addItem("Property Name");
        br.addElement(name);
        br.addItem("Property Value");
        final Object valueObj;
        if (value instanceof String[]) {
            final List<Object> objList = DfCollectionUtil.toListFromArray(value);
            valueObj = objList.size() == 1 ? objList.get(0) : objList;
        } else {
            valueObj = (String[]) value;
        }
        br.addElement(valueObj);
        final String msg = br.buildExceptionMessage();
        throw new ActionFormPopulateFailureException(msg, cause);
    }

    // ===================================================================================
    //                                                                           JSON Body
    //                                                                           =========
    protected boolean handleJsonBody(ActionRuntime runtime, VirtualActionForm virtualActionForm) throws IOException {
        if (isJsonBodyForm(virtualActionForm.getFormMeta().getFormType())) {
            mappingJsonBody(runtime, virtualActionForm, getRequestBody());
            return true;
        }
        if (isListJsonBodyForm(virtualActionForm)) {
            mappingListJsonBody(runtime, virtualActionForm, getRequestBody());
            return true;
        }
        return false;
    }

    protected boolean isJsonBodyForm(Class<? extends Object> formType) {
        return formType.getName().endsWith(ExecuteArgAnalyzer.BODY_SUFFIX);
    }

    protected boolean isListJsonBodyForm(VirtualActionForm virtualActionForm) {
        return virtualActionForm.getFormMeta().getListFormParameterGenericType().map(genericType -> {
            return isJsonBodyForm(genericType);
        }).orElse(false);
    }

    protected String getRequestBody() throws IOException {
        return requestManager.getRequestBody();
    }

    // -----------------------------------------------------
    //                                             Bean JSON
    //                                             ---------
    protected void mappingJsonBody(ActionRuntime runtime, VirtualActionForm virtualActionForm, String json) {
        final JsonManager jsonManager = getJsonManager();
        try {
            final Object fromJson = jsonManager.fromJson(json, virtualActionForm.getFormMeta().getFormType());
            acceptJsonRealForm(virtualActionForm, fromJson);
        } catch (RuntimeException e) {
            throwJsonBodyParseFailureException(runtime, virtualActionForm, json, e);
        }
    }

    protected void throwJsonBodyParseFailureException(ActionRuntime runtime, VirtualActionForm virtualActionForm, String json,
            RuntimeException e) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Cannot parse json on the request body.");
        sb.append(LF).append(LF).append("[JsonBody Parse Failure]");
        sb.append(LF).append(runtime);
        sb.append(LF).append(virtualActionForm);
        sb.append(LF).append(json);
        final Map<String, Object> retryMap = retryJsonAsMapForDebug(json);
        List<JsonDebugChallenge> challengeList = new ArrayList<JsonDebugChallenge>();
        if (!retryMap.isEmpty()) {
            sb.append(LF).append(buildDebugChallengeTitle());
            final List<JsonDebugChallenge> nestedList = prepareJsonBodyDebugChallengeList(virtualActionForm, retryMap, null);
            for (JsonDebugChallenge challenge : nestedList) {
                sb.append(challenge.toChallengeDisp());
            }
            challengeList.addAll(nestedList);
        }
        sb.append(LF).append(e.getClass().getName()).append(LF).append(e.getMessage());
        throwRequestJsonParseFailureException(sb.toString(), challengeList, e);
    }

    protected List<JsonDebugChallenge> prepareJsonBodyDebugChallengeList(VirtualActionForm virtualActionForm, Map<String, Object> retryMap,
            Integer elementIndex) {
        if (retryMap.isEmpty()) {
            return Collections.emptyList();
        }
        final List<JsonDebugChallenge> challengeList = new ArrayList<JsonDebugChallenge>();
        for (ActionFormProperty property : virtualActionForm.getFormMeta().properties()) {
            final String propertyName = property.getPropertyName();
            final Class<?> propertyType = property.getPropertyDesc().getPropertyType();
            final JsonDebugChallenge challenge = createJsonDebugChallenge(retryMap, propertyName, propertyType, elementIndex);
            challengeList.add(challenge);
        }
        return Collections.unmodifiableList(challengeList);
    }

    // -----------------------------------------------------
    //                                             List JSON
    //                                             ---------
    protected void mappingListJsonBody(ActionRuntime runtime, VirtualActionForm virtualActionForm, String json) {
        try {
            final ActionFormMeta formMeta = virtualActionForm.getFormMeta();
            final ParameterizedType pt = formMeta.getListFormParameterParameterizedType().get(); // already checked
            final List<Object> fromJsonList = getJsonManager().fromJsonList(json, pt);
            acceptJsonRealForm(virtualActionForm, fromJsonList);
        } catch (RuntimeException e) {
            throwListJsonBodyParseFailureException(runtime, virtualActionForm, json, e);
        }
    }

    protected void throwListJsonBodyParseFailureException(ActionRuntime runtime, VirtualActionForm virtualActionForm, String json,
            RuntimeException e) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Cannot parse list json on the request body.");
        sb.append(LF).append(LF).append("[List JsonBody Parse Failure]");
        sb.append(LF).append(runtime);
        sb.append(LF).append(virtualActionForm);
        sb.append(LF).append(json);
        final List<Map<String, Object>> retryList = retryJsonListAsMapForDebug(json);
        final List<JsonDebugChallenge> challengeList = new ArrayList<JsonDebugChallenge>();
        if (!retryList.isEmpty()) {
            sb.append(LF).append(buildDebugChallengeTitle());
            int index = 1;
            for (Map<String, Object> retryMap : retryList) {
                sb.append(LF).append(" (index: ").append(index).append(")");
                final List<JsonDebugChallenge> nestedList = prepareJsonBodyDebugChallengeList(virtualActionForm, retryMap, index);
                challengeList.addAll(nestedList);
                nestedList.forEach(challenge -> sb.append(challenge.toChallengeDisp()));
                ++index;
            }
        }
        throwRequestJsonParseFailureException(sb.toString(), challengeList, e);
    }

    // -----------------------------------------------------
    //                                          Assist Logic
    //                                          ------------
    protected void acceptJsonRealForm(VirtualActionForm virtualActionForm, Object realForm) {
        virtualActionForm.acceptRealForm(realForm);
    }

    // ===================================================================================
    //                                                                        Property Set
    //                                                                        ============
    protected void setProperty(Object bean, String name, Object value, Object parentBean, String parentName) {
        if (bean == null) {
            return;
        }
        final int nestedIndex = name.indexOf(NESTED_DELIM); // e.g. sea.mythica
        final int indexedIndex = name.indexOf(INDEXED_DELIM); // e.g. sea[0]
        final int mappedIndex = name.indexOf(MAPPED_DELIM); // e.g. sea(over)
        if (nestedIndex < 0 && indexedIndex < 0 && mappedIndex < 0) { // as simple
            setSimpleProperty(bean, name, value, parentBean, parentName);
        } else {
            final int minIndex = minIndex(minIndex(nestedIndex, indexedIndex), mappedIndex);
            if (minIndex == nestedIndex) { // e.g. sea.mythica
                final String front = name.substring(0, minIndex);
                final Object simpleProperty = prepareSimpleProperty(bean, front);
                final String rear = name.substring(minIndex + 1);
                setProperty(simpleProperty, rear, value, bean, front); // *recursive
            } else if (minIndex == indexedIndex) { // e.g. sea[0]
                final IndexParsedResult result = parseIndex(name.substring(indexedIndex + 1));
                final int[] resultIndexes = result.indexes;
                final String resultName = result.name;
                final String front = name.substring(0, indexedIndex);
                if (resultName == null || resultName.isEmpty()) {
                    setIndexedProperty(bean, front, resultIndexes, value);
                } else {
                    final Object indexedProperty = prepareIndexedProperty(bean, front, resultIndexes);
                    setProperty(indexedProperty, resultName, value, bean, front); // *recursive
                }
            } else { // map e.g. sea(over)
                final int endIndex = name.indexOf(MAPPED_DELIM2, mappedIndex); // sea(over)
                final String front = name.substring(0, mappedIndex);
                final String middle = name.substring(mappedIndex + 1, endIndex);
                final String rear = name.substring(endIndex + 1);
                setProperty(bean, front + "." + middle + rear, value, bean, front); // *recursive
            }
        }
    }

    protected static class IndexParsedResult {
        public int[] indexes = new int[0];
        public String name;
    }

    protected int minIndex(int index1, int index2) {
        if (index1 >= 0 && index2 < 0) {
            return index1;
        } else if (index1 < 0 && index2 >= 0) {
            return index2;
        } else {
            return Math.min(index1, index2);
        }
    }

    // ===================================================================================
    //                                                                     Simple Property
    //                                                                     ===============
    protected Object prepareSimpleProperty(Object bean, String name) {
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

    protected void setSimpleProperty(Object bean, String name, Object value, Object parentBean, String parentName) {
        try {
            doSetSimpleProperty(bean, name, value, parentBean, parentName);
        } catch (BeanIllegalPropertyException e) {
            if (!(e.getCause() instanceof NumberFormatException)) {
                throw e;
            }
            // here: non-number GET or URL parameter but number type property
            // suppress easy 500 error by non-number GET or URL parameter
            //  (o): /edit/123/
            //  (x): /edit/abc/ *this case
            // you can accept ID on URL parameter as Long type in ActionForm
            final Object dispValue;
            if (value instanceof Object[]) {
                dispValue = Arrays.asList((Object[]) value).toString();
            } else {
                dispValue = value;
            }
            String beanExp = bean != null ? bean.getClass().getName() : null; // null check just in case
            String msg = "The property value cannot be number: " + beanExp + ", name=" + name + ", value=" + dispValue;
            throwRequestPropertyMappingFailureException(msg, e);
        }
    }

    protected void doSetSimpleProperty(Object bean, String name, Object value, Object parentBean, String parentName) {
        if (bean instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) bean;
            setMapProperty(map, name, value, parentBean, parentName);
            return;
        }
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(bean.getClass());
        if (!beanDesc.hasPropertyDesc(name)) {
            handleUndefinedParameter(bean, name, value, beanDesc);
            return;
        }
        final PropertyDesc pd = beanDesc.getPropertyDesc(name);
        if (!pd.isWritable()) {
            handleUndefinedParameter(bean, name, value, beanDesc);
            return;
        }
        final Class<?> propertyType = pd.getPropertyType();
        if (propertyType.isArray()) { // e.g. public String[] strArray;
            pd.setValue(bean, prepareStringArray(value)); // plain mapping to array, e.g. JSON not supported
        } else if (List.class.isAssignableFrom(propertyType)) { // e.g. public List<...> anyList;
            if (isJsonParameterProperty(pd)) { // e.g. public List<SeaJsonBean> jsonList;
                pd.setValue(bean, parseJsonParameter(bean, name, prepareStringScalar(value), pd));
            } else { // e.g. public List<String> strList;
                pd.setValue(bean, prepareStringList(value, propertyType));
            }
        } else { // not array or list, e.g. String, Object
            final String exp = prepareStringScalar(value); // not null (empty if null)
            if (isJsonParameterProperty(pd)) { // e.g. JsonPrameter
                pd.setValue(bean, parseJsonParameter(bean, name, exp, pd));
            } else { // mainly here, e.g. String, Integer, LocalDate, CDef, ...
                pd.setValue(bean, convertStringToPropertyNativeIfNeeds(bean, name, exp, pd));
            }
        }
    }

    protected String[] prepareStringArray(Object value) {
        if (value != null && value instanceof String[]) {
            return (String[]) value;
        } else {
            return value != null ? new String[] { value.toString() } : EMPTY_STRING_ARRAY;
        }
    }

    protected String prepareStringScalar(Object value) {
        if (value != null && value instanceof String[] && ((String[]) value).length > 0) {
            return ((String[]) value)[0];
        } else {
            return value != null ? value.toString() : "";
        }
    }

    protected List<String> prepareStringList(Object value, Class<?> propertyType) {
        final String[] ary = prepareStringArray(value);
        if (ary.length == 0) {
            return Collections.emptyList();
        }
        final boolean absList = LdiModifierUtil.isAbstract(propertyType); // e.g. List or ArrayList
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

    protected Object convertStringToPropertyNativeIfNeeds(Object bean, String name, String exp, PropertyDesc pd) {
        // not to depend on conversion logic in BeanDesc
        final Class<?> propertyType = pd.getPropertyType();
        final Object filtered;
        if (propertyType.isPrimitive()) {
            filtered = DfTypeUtil.toWrapper(exp, propertyType);
        } else if (Number.class.isAssignableFrom(propertyType)) {
            filtered = DfTypeUtil.toNumber(exp, propertyType);
            // old date types are unsupported for LocalDate invitation
            //} else if (Timestamp.class.isAssignableFrom(propertyType)) {
            //    filtered = DfTypeUtil.toTimestamp(exp);
            //} else if (Time.class.isAssignableFrom(propertyType)) {
            //    filtered = DfTypeUtil.toTime(exp);
            //} else if (java.util.Date.class.isAssignableFrom(propertyType)) {
            //    filtered = DfTypeUtil.toDate(exp);
        } else if (LocalDate.class.isAssignableFrom(propertyType)) { // #date_parade
            filtered = DfTypeUtil.toLocalDate(exp);
        } else if (LocalDateTime.class.isAssignableFrom(propertyType)) {
            filtered = DfTypeUtil.toLocalDateTime(exp);
        } else if (LocalTime.class.isAssignableFrom(propertyType)) {
            filtered = DfTypeUtil.toLocalTime(exp);
        } else if (Boolean.class.isAssignableFrom(propertyType)) {
            filtered = DfTypeUtil.toBoolean(exp);
        } else if (isClassificationProperty(propertyType)) { // means CDef
            filtered = toVerifiedClassification(bean, name, exp, pd);
        } else {
            filtered = exp;
        }
        return filtered;
    }

    // -----------------------------------------------------
    //                                   Undefined Parameter
    //                                   -------------------
    protected void handleUndefinedParameter(Object bean, String name, Object value, BeanDesc beanDesc) {
        if (needsUndefinedParameterCheck() && !getIndefinableParameterSet().contains(name)) {
            throwRequestUndefinedParameterInFormException(bean, name, value, beanDesc);
        }
    }

    protected boolean needsUndefinedParameterCheck() {
        final FormMappingOption option = adjustFormMapping();
        return option != null && option.isUndefinedParameterError();
    }

    protected Set<String> getIndefinableParameterSet() {
        final FormMappingOption option = adjustFormMapping();
        return option != null ? option.getIndefinableParameterSet() : Collections.emptySet();
    }

    // -----------------------------------------------------
    //                                          Map Property
    //                                          ------------
    protected void setMapProperty(Map<String, Object> map, String name, Object value, Object parentBean, String parentName) {
        final boolean stringArray = isMapValueStringArray(parentBean, parentName);
        final Object registered;
        if (value instanceof String[]) {
            final String[] values = (String[]) value;
            registered = stringArray ? values : values.length > 0 ? values[0] : null;
        } else {
            registered = stringArray ? (value != null ? new String[] { value.toString() } : EMPTY_STRING_ARRAY) : value;
        }
        map.put(name, registered);
    }

    protected boolean isMapValueStringArray(Object parentBean, String parentName) {
        if (parentBean == null) {
            return false;
        }
        final PropertyDesc propertyDesc = BeanDescFactory.getBeanDesc(parentBean.getClass()).getPropertyDesc(parentName);
        final Class<?> valueClassOfMap = propertyDesc.getValueClassOfMap();
        return valueClassOfMap != null && valueClassOfMap.isArray() && String[].class.isAssignableFrom(valueClassOfMap);
    }

    // -----------------------------------------------------
    //                                        JSON Parameter
    //                                        --------------
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

    protected Object parseJsonParameter(Object bean, String name, String json, PropertyDesc pd) {
        final JsonManager jsonManager = getJsonManager();
        final Class<?> propertyType = pd.getPropertyType();
        if (isListJsonProperty(propertyType)) { // e.g. public List<...> beanList;
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
                return jsonManager.fromJsonList(json, paramedType); // e.g. public List<SeaBean> beanList;
            } catch (RuntimeException e) {
                throwListJsonParameterParseFailureException(bean, name, json, paramedType, e);
                return null; // unreachable
            }
        } else { // e.g. public SeaBean seaBean;
            try {
                return jsonManager.fromJson(json, propertyType);
            } catch (RuntimeException e) {
                throwJsonParameterParseFailureException(bean, name, json, propertyType, e);
                return null; // unreachable
            }
        }
    }

    // -----------------------------------------------------
    //                                             List JSON
    //                                             ---------
    protected boolean isListJsonProperty(Class<?> propertyType) { // just type
        return List.class.equals(propertyType);
    }

    protected void throwListJsonPropertyNonGenericException(Object bean, String name, String json, PropertyDesc pd) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Non-generic list cannot handle the JSON.");
        br.addItem("Action Form");
        br.addElement(bean);
        br.addItem("NonGeneric Property");
        br.addElement(pd);
        br.addItem("Unhandled JSON");
        br.addElement(json);
        final String msg = br.buildExceptionMessage();
        throw new ActionFormPopulateFailureException(msg);
    }

    protected void throwListJsonPropertyNonParameterizedException(Object bean, String name, String json, PropertyDesc pd, Type plainType) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Non-parameterized list cannot handle the JSON.");
        br.addItem("Action Form");
        br.addElement(bean);
        br.addItem("NonParameterized Property");
        br.addElement(pd);
        br.addItem("NonParameterized Type");
        br.addElement(plainType);
        br.addItem("Unhandled JSON");
        br.addElement(json);
        final String msg = br.buildExceptionMessage();
        throw new ActionFormPopulateFailureException(msg);
    }

    protected void throwListJsonPropertyGenericNotScalarException(Object bean, String name, String json, PropertyDesc pd,
            ParameterizedType paramedType) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not scalar generic type for the list JSON parameter.");
        br.addItem("Action Form");
        br.addElement(bean);
        br.addItem("Generic Property");
        br.addElement(pd);
        br.addItem("Parameterizd Type");
        br.addElement(paramedType);
        br.addItem("Unhandled JSON");
        br.addElement(json);
        final String msg = br.buildExceptionMessage();
        throw new ActionFormPopulateFailureException(msg);
    }

    protected void throwListJsonParameterParseFailureException(Object bean, String name, String json, Type propertyType,
            RuntimeException e) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Cannot parse list json of the request parameter.");
        final List<Map<String, Object>> retryList = retryJsonListAsMapForDebug(json);
        final List<JsonDebugChallenge> challengeList = new ArrayList<JsonDebugChallenge>();
        final StringBuilder challengeSb = new StringBuilder();
        if (!retryList.isEmpty()) {
            final Class<?> elementType = DfReflectionUtil.getGenericFirstClass(propertyType);
            if (elementType != null) { // just in case
                int index = 0;
                for (Map<String, Object> retryMap : retryList) {
                    challengeSb.append(LF).append(" (index: ").append(index).append(")");
                    final List<JsonDebugChallenge> nestedList = prepareJsonParameterDebugChallengeList(retryMap, elementType, json, index);
                    challengeList.addAll(nestedList);
                    nestedList.forEach(challenge -> challengeSb.append(challenge.toChallengeDisp()));
                    ++index;
                }
            }
        }
        final String challengeDisp = challengeSb.toString();
        buildClientErrorHeader(sb, "List JsonParameter Parse Failure", bean, name, json, propertyType, challengeDisp);
        throwRequestJsonParseFailureException(sb.toString(), challengeList, e);
    }

    // -----------------------------------------------------
    //                                             Bean JSON
    //                                             ---------
    protected void throwJsonParameterParseFailureException(Object bean, String name, String json, Class<?> propertyType,
            RuntimeException e) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Cannot parse json of the request parameter.");
        final Map<String, Object> retryMap = retryJsonAsMapForDebug(json);
        final List<JsonDebugChallenge> challengeList = prepareJsonParameterDebugChallengeList(retryMap, propertyType, json, null);
        final String challengeDisp = buildJsonParameterDebugChallengeDisp(challengeList);
        buildClientErrorHeader(sb, "JsonParameter Parse Failure", bean, name, json, propertyType, challengeDisp);
        throwRequestJsonParseFailureException(sb.toString(), challengeList, e);
    }

    protected List<JsonDebugChallenge> prepareJsonParameterDebugChallengeList(Map<String, Object> retryMap, Class<?> beanType, String json,
            Integer elementIndex) {
        if (retryMap.isEmpty()) {
            return Collections.emptyList();
        }
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(beanType);
        final int fieldSize = beanDesc.getFieldSize();
        final List<JsonDebugChallenge> challengeList = new ArrayList<JsonDebugChallenge>(fieldSize);
        for (int i = 0; i < fieldSize; i++) {
            final Field field = beanDesc.getField(i);
            final JsonDebugChallenge challenge = createJsonDebugChallenge(retryMap, field.getName(), field.getType(), elementIndex);
            challengeList.add(challenge);
        }
        return Collections.unmodifiableList(challengeList);
    }

    protected String buildJsonParameterDebugChallengeDisp(List<JsonDebugChallenge> challengeList) {
        if (challengeList.isEmpty()) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        challengeList.forEach(challenge -> sb.append(challenge.toChallengeDisp()));
        return sb.toString();
    }

    // -----------------------------------------------------
    //                                        Classification
    //                                        --------------
    protected boolean isClassificationProperty(Class<?> propertyType) {
        return LaDBFluteUtil.isClassificationType(propertyType);
    }

    protected Classification toVerifiedClassification(Object bean, String name, String code, PropertyDesc pd) {
        final Class<?> propertyType = pd.getPropertyType();
        try {
            return LaDBFluteUtil.toVerifiedClassification(propertyType, code);
        } catch (ClassificationUnknownCodeException e) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Cannot convert the code of the request parameter to the classification.");
            buildClientErrorHeader(sb, "Classification Convert Failure", bean, name, code, propertyType, null);
            throwRequestClassifiationConvertFailureException(sb.toString(), e);
            return null; // unreachable
        }
    }

    // ===================================================================================
    //                                                                         Parse Index
    //                                                                         ===========
    protected IndexParsedResult parseIndex(String name) { // override for checking indexed property
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

    protected IndexParsedResult doParseIndex(String name) {
        IndexParsedResult result = new IndexParsedResult();
        while (true) {
            int index = name.indexOf(INDEXED_DELIM2);
            if (index < 0) {
                throw new IllegalArgumentException(INDEXED_DELIM2 + " is not found in " + name);
            }
            result.indexes = LdiArrayUtil.add(result.indexes, Integer.valueOf(name.substring(0, index)).intValue());
            name = name.substring(index + 1);
            if (name.length() == 0) {
                break;
            } else if (name.charAt(0) == INDEXED_DELIM) {
                name = name.substring(1);
            } else if (name.charAt(0) == NESTED_DELIM) {
                name = name.substring(1);
                break;
            } else {
                throw new IllegalArgumentException(name);
            }
        }
        result.name = name;
        return result;
    }

    protected void throwIndexedPropertyNonNumberIndexException(String name, NumberFormatException e) {
        String msg = "Non number index of the indexed property: name=" + name + LF + e.getMessage();
        throwRequestPropertyMappingFailureException(msg, e);
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
        String msg = "Minus index of the indexed property: name=" + name;
        throwRequestPropertyMappingFailureException(msg);
    }

    protected void throwIndexedPropertySizeOverException(String name, int index) {
        String msg = "Too large size of the indexed property: name=" + name + ", index=" + index;
        throwRequestPropertyMappingFailureException(msg);
    }

    // ===================================================================================
    //                                                                    Indexed Property
    //                                                                    ================
    // -----------------------------------------------------
    //                                  Set Indexed Property
    //                                  --------------------
    @SuppressWarnings("unchecked")
    protected void setIndexedProperty(Object bean, String name, int[] indexes, Object value) {
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
            array = expand(array, indexes, elementType);
            pd.setValue(bean, array);
            setArrayValue(array, indexes, value);
        } else if (List.class.isAssignableFrom(propertyType)) {
            List<Object> list = (List<Object>) pd.getValue(bean);
            if (list == null) {
                list = new ArrayList<Object>(Math.max(50, indexes[0]));
                pd.setValue(bean, list);
            }
            ParameterizedClassDesc paramDesc = pd.getParameterizedClassDesc();
            for (int i = 0; i < indexes.length; i++) {
                if (paramDesc == null || !paramDesc.isParameterizedClass() || !List.class.isAssignableFrom(paramDesc.getRawClass())) {
                    final StringBuilder sb = new StringBuilder();
                    for (int j = 0; j <= i; j++) {
                        sb.append("[").append(indexes[j]).append("]");
                    }
                    throwIndexedPropertyNonParameterizedListException(beanDesc, pd);
                }
                paramDesc = paramDesc.getArguments()[0];
                for (int j = list.size(); j <= indexes[i]; j++) {
                    if (i == indexes.length - 1) {
                        list.add(LdiClassUtil.newInstance(convertClass(paramDesc.getRawClass())));
                    } else {
                        list.add(new ArrayList<Object>());
                    }
                }
                if (i < indexes.length - 1) {
                    list = (List<Object>) list.get(indexes[i]);
                }
            }
            list.set(indexes[indexes.length - 1], value);
        } else {
            throwIndexedPropertyNotListArrayException(beanDesc, pd);
        }
    }

    protected void setArrayValue(Object array, int[] indexes, Object value) {
        for (int i = 0; i < indexes.length - 1; i++) {
            array = Array.get(array, indexes[i]);
        }
        Array.set(array, indexes[indexes.length - 1], value);
    }

    // -----------------------------------------------------
    //                              Prepare Indexed Property
    //                              ------------------------
    @SuppressWarnings("unchecked")
    protected Object prepareIndexedProperty(Object bean, String name, int[] indexes) {
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(bean.getClass());
        if (!beanDesc.hasPropertyDesc(name)) {
            return null;
        }
        final PropertyDesc pd = beanDesc.getPropertyDesc(name);
        if (!pd.isReadable()) {
            return null;
        }
        if (pd.getPropertyType().isArray()) {
            Object array = pd.getValue(bean);
            final Class<?> elementType = getArrayElementType(pd.getPropertyType(), indexes.length);
            if (array == null) {
                int[] newIndexes = new int[indexes.length];
                newIndexes[0] = indexes[0] + 1;
                array = Array.newInstance(elementType, newIndexes);
            }
            array = expand(array, indexes, elementType);
            pd.setValue(bean, array);
            return getArrayValue(array, indexes, elementType);
        } else {
            if (List.class.isAssignableFrom(pd.getPropertyType())) {
                List<Object> list = (List<Object>) pd.getValue(bean);
                if (list == null) {
                    list = new ArrayList<Object>(Math.max(50, indexes[0]));
                    pd.setValue(bean, list);
                }
                ParameterizedClassDesc pcd = pd.getParameterizedClassDesc();
                for (int i = 0; i < indexes.length; i++) {
                    if (pcd == null || !pcd.isParameterizedClass() || !List.class.isAssignableFrom(pcd.getRawClass())) {
                        StringBuilder sb = new StringBuilder();
                        for (int j = 0; j <= i; j++) {
                            sb.append("[").append(indexes[j]).append("]");
                        }
                        throwIndexedPropertyNonParameterizedListException(beanDesc, pd);
                    }
                    int size = list.size();
                    pcd = pcd.getArguments()[0];
                    for (int j = size; j <= indexes[i]; j++) {
                        if (i == indexes.length - 1) {
                            list.add(LdiClassUtil.newInstance(convertClass(pcd.getRawClass())));
                        } else {
                            list.add(new ArrayList<Integer>());
                        }
                    }
                    if (i < indexes.length - 1) {
                        list = (List<Object>) list.get(indexes[i]);
                    }
                }
                return list.get(indexes[indexes.length - 1]);
            } else {
                throwIndexedPropertyNotListArrayException(beanDesc, pd);
                return null; // unreachable
            }
        }
    }

    protected Object getArrayValue(Object array, int[] indexes, Class<?> elementType) {
        Object value = array;
        elementType = convertClass(elementType);
        for (int i = 0; i < indexes.length; i++) {
            Object element = Array.get(value, indexes[i]);
            if (i == indexes.length - 1 && element == null) {
                element = LdiClassUtil.newInstance(elementType);
                Array.set(value, indexes[i], element);
            }
            value = element;
        }
        return value;
    }

    protected void throwIndexedPropertyNonParameterizedListException(final BeanDesc beanDesc, final PropertyDesc pd) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The list of indexed property was not parameterized.");
        br.addItem("ActionForm");
        br.addElement(getRealClass(beanDesc.getBeanClass()));
        br.addItem("Property");
        br.addElement(pd);
        final String msg = br.buildExceptionMessage();
        throw new IndexedPropertyNonParameterizedListException(msg);
    }

    protected void throwIndexedPropertyNotListArrayException(BeanDesc beanDesc, PropertyDesc pd) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The indexed property was not list or array.");
        br.addItem("ActionForm");
        br.addElement(getRealClass(beanDesc.getBeanClass()));
        br.addItem("Property");
        br.addElement(pd);
        final String msg = br.buildExceptionMessage();
        throw new IndexedPropertyNotListArrayException(msg);
    }

    // -----------------------------------------------------
    //                                          Array Helper
    //                                          ------------
    protected Class<?> getArrayElementType(Class<?> clazz, int depth) {
        for (int i = 0; i < depth; i++) {
            clazz = clazz.getComponentType();
        }
        return clazz;
    }

    protected Object expand(Object array, int[] indexes, Class<?> elementType) {
        int length = Array.getLength(array);
        if (length <= indexes[0]) {
            int[] newIndexes = new int[indexes.length];
            newIndexes[0] = indexes[0] + 1;
            Object newArray = Array.newInstance(elementType, newIndexes);
            System.arraycopy(array, 0, newArray, 0, length);
            array = newArray;
        }
        if (indexes.length > 1) {
            int[] newIndexes = new int[indexes.length - 1];
            for (int i = 1; i < indexes.length; i++) {
                newIndexes[i - 1] = indexes[i];
            }
            Array.set(array, indexes[0], expand(Array.get(array, indexes[0]), newIndexes, elementType));
        }
        return array;
    }

    protected Class<?> convertClass(Class<?> clazz) {
        return LdiModifierUtil.isAbstract(clazz) && Map.class.isAssignableFrom(clazz) ? HashMap.class : clazz;
    }

    // -----------------------------------------------------
    //                                            Real Class
    //                                            ----------
    protected Class<?> getRealClass(Class<?> clazz) {
        return clazz.getName().indexOf(AspectWeaver.SUFFIX_ENHANCED_CLASS) > 0 ? clazz.getSuperclass() : clazz;
    }

    // ===================================================================================
    //                                                                         JSON Assist
    //                                                                         ===========
    protected JsonManager getJsonManager() {
        return ContainerUtil.getComponent(JsonManager.class);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> retryJsonAsMapForDebug(String json) {
        try {
            return getJsonManager().fromJson(json, Map.class);
        } catch (RuntimeException ignored) {
            return Collections.emptyMap();
        }
    }

    protected List<Map<String, Object>> retryJsonListAsMapForDebug(String json) {
        try {
            return getJsonManager().fromJsonList(json, new GenericTypeRef<List<Map<String, Object>>>() {
            }.getParameterizedType());
        } catch (RuntimeException ignored) {
            return Collections.emptyList();
        }
    }

    protected JsonDebugChallenge createJsonDebugChallenge(Map<String, Object> retryMap, String propertyName, Class<?> propertyType,
            Integer elementIndex) {
        return new JsonDebugChallenge(propertyName, propertyType, retryMap.get(propertyName), elementIndex);
    }

    // ===================================================================================
    //                                                                        Client Error
    //                                                                        ============
    protected void buildClientErrorHeader(StringBuilder sb, String title, Object bean, String name, String value, Type propertyType,
            String challengeDisp) {
        sb.append(LF).append(LF).append("[").append(title).append("]");
        sb.append(LF).append("Mapping To: ");
        sb.append(bean.getClass().getSimpleName()).append("#").append(name).append(" (").append(propertyType.getTypeName()).append(")");
        sb.append(LF).append("Requested Value: ").append(value != null && value.contains(LF) ? LF : "").append(value);
        if (challengeDisp != null && challengeDisp.length() > 0) {
            sb.append(LF).append(buildDebugChallengeTitle());
            sb.append(challengeDisp); // debugChallenge starts with LF
        }
    }

    protected String buildDebugChallengeTitle() {
        return "Debug Challenge: (o: maybe assignable, x: cannot, v: no value, ?: unknown)";
    }

    // no server error because it can occur by user's trick
    // while, is likely to due to client bugs (or server) so request client error
    protected void throwRequestJsonParseFailureException(String msg, List<JsonDebugChallenge> challengeList, RuntimeException e) {
        throw new RequestJsonParseFailureException(msg, e).withChallengeList(challengeList);
    }

    protected void throwRequestPropertyMappingFailureException(String msg) {
        throw new RequestPropertyMappingFailureException(msg);
    }

    protected void throwRequestPropertyMappingFailureException(String msg, RuntimeException e) {
        throw new RequestPropertyMappingFailureException(msg, e);
    }

    protected void throwRequestClassifiationConvertFailureException(String msg, Exception e) {
        throw new RequestClassifiationConvertFailureException(msg, e);
    }

    protected void throwRequestUndefinedParameterInFormException(Object bean, String name, Object value, BeanDesc beanDesc) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Undefined parameter in the form.");
        br.addItem("Advice");
        br.addElement("Request parameters should be related to any property of form.");
        br.addElement("For example:");
        br.addElement("  (x): ?sea=mystic&land=oneman");
        br.addElement("    public class MaihamaForm { // *NG: 'land' is undefined");
        br.addElement("        public String sea;");
        br.addElement("    }");
        br.addElement("  (o): ?sea=mystic&land=oneman");
        br.addElement("    public class MaihamaForm {");
        br.addElement("        public String sea;");
        br.addElement("        public String land; // OK");
        br.addElement("    }");
        br.addElement("");
        br.addElement("If you want to ignore the parameter from this check,");
        br.addElement("adjust FormMappingOption in ActionAdjustmentProvider.");
        br.addItem("Action Form");
        br.addElement(bean.getClass().getName());
        br.addItem("Defined Property");
        final StringBuilder propertySb = new StringBuilder();
        for (int i = 0; i < beanDesc.getPropertyDescSize(); i++) {
            propertySb.append(i % 5 == 4 ? "\n" : "");
            propertySb.append(i > 0 ? ", " : "");
            propertySb.append(beanDesc.getPropertyDesc(i).getPropertyName());
        }
        br.addElement(propertySb);
        br.addItem("Requested Parameter");
        br.addElement(name + "=" + (value instanceof Object[] ? Arrays.asList((Object[]) value) : value));
        final String msg = br.buildExceptionMessage();
        throw new RequestUndefinedParameterInFormException(msg);
    }

    // ===================================================================================
    //                                                                  Assistant Director
    //                                                                  ==================
    protected ActionAdjustmentProvider getAdjustmentProvider() {
        return assistWebDirection().assistActionAdjustmentProvider();
    }

    protected FormMappingOption adjustFormMapping() {
        return getAdjustmentProvider().adjustFormMapping();
    }

    protected FwWebDirection assistWebDirection() {
        return assistantDirector.assistWebDirection();
    }
}
