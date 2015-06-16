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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.jdbc.Classification;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfReflectionUtil;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.di.core.aop.javassist.AspectWeaver;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.ParameterizedClassDesc;
import org.lastaflute.di.helper.beans.PropertyDesc;
import org.lastaflute.di.helper.beans.exception.IllegalPropertyRuntimeException;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;
import org.lastaflute.di.util.LdiArrayUtil;
import org.lastaflute.di.util.LdiClassUtil;
import org.lastaflute.di.util.LdiModifierUtil;
import org.lastaflute.web.api.JsonBody;
import org.lastaflute.web.api.JsonParameter;
import org.lastaflute.web.callback.ActionRuntime;
import org.lastaflute.web.direction.FwWebDirection;
import org.lastaflute.web.exception.ForcedRequest404NotFoundException;
import org.lastaflute.web.exception.IndexedPropertyNonParameterizedListException;
import org.lastaflute.web.exception.IndexedPropertyNotListArrayException;
import org.lastaflute.web.exception.RequestClassifiationConvertFailureException;
import org.lastaflute.web.exception.RequestJsonParseFailureException;
import org.lastaflute.web.exception.RequestPropertyMappingFailureException;
import org.lastaflute.web.path.ActionAdjustmentProvider;
import org.lastaflute.web.ruts.VirtualActionForm;
import org.lastaflute.web.ruts.config.ActionFormMeta;
import org.lastaflute.web.ruts.config.ActionFormProperty;
import org.lastaflute.web.ruts.config.ModuleConfig;
import org.lastaflute.web.ruts.multipart.ActionMultipartRequestHandler;
import org.lastaflute.web.ruts.multipart.MultipartRequestHandler;
import org.lastaflute.web.ruts.multipart.MultipartRequestWrapper;
import org.lastaflute.web.ruts.process.exception.ActionFormPopulateFailureException;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.util.LaDBFluteUtil;
import org.lastaflute.web.util.LaDBFluteUtil.ClassificationConvertFailureException;

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
            multipartHandler = newActionMultipartRequestHandler();
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

    protected ActionMultipartRequestHandler newActionMultipartRequestHandler() {
        return new ActionMultipartRequestHandler();
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
        if (isRequest404NotFoundException(cause)) { // for indexed property check
            throw new ServletException(cause);
        }
        throwActionFormPopulateFailureException(form, name, value, runtime, cause);
    }

    protected boolean isRequest404NotFoundException(Throwable cause) {
        return cause instanceof ForcedRequest404NotFoundException;
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
        if (isJsonBodyListForm(virtualActionForm)) {
            mappingListJsonBody(runtime, virtualActionForm, getRequestBody());
            return true;
        }
        return false;
    }

    protected boolean isJsonBodyForm(Class<? extends Object> formType) {
        return formType.getAnnotation(JsonBody.class) != null;
    }

    protected boolean isJsonBodyListForm(VirtualActionForm virtualActionForm) {
        return virtualActionForm.getFormMeta().getListFormParameterGenericType().map(genericType -> {
            return isJsonBodyForm(genericType);
        }).orElse(false);
    }

    protected String getRequestBody() throws IOException {
        return requestManager.getRequestBody();
    }

    protected void mappingJsonBody(ActionRuntime runtime, VirtualActionForm virtualActionForm, String json) {
        final JsonManager jsonManager = getJsonManager();
        try {
            final Object fromJson = jsonManager.fromJson(json, virtualActionForm.getFormMeta().getFormType());
            acceptJsonRealForm(virtualActionForm, fromJson);
        } catch (RuntimeException e) {
            final Map<String, Object> retryMap = retryJsonAsMapForDebug(json, jsonManager);
            throwJsonBodyParseFailureException(runtime, virtualActionForm, json, retryMap, e);
        }
    }

    protected void acceptJsonRealForm(VirtualActionForm virtualActionForm, Object realForm) {
        virtualActionForm.acceptRealForm(realForm);
    }

    protected void throwJsonBodyParseFailureException(ActionRuntime runtime, VirtualActionForm virtualActionForm, String json,
            Map<String, Object> retryMap, RuntimeException e) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Cannot parse json of the request body:");
        sb.append(LF).append("[JsonBody Parse Failure]");
        sb.append(LF).append(runtime);
        sb.append(LF).append(virtualActionForm);
        sb.append(LF).append(json);
        if (retryMap != null) {
            sb.append(LF).append(buildDebugChallengeTitle());
            for (ActionFormProperty property : virtualActionForm.getFormMeta().properties()) {
                buildJsonDebugChallenge(sb, retryMap, property.getPropertyName(), property.getPropertyDesc().getPropertyType());
            }
        }
        sb.append(LF).append(e.getClass().getName()).append(LF).append(e.getMessage());
        throwRequestJsonParseFailureException(sb.toString(), e);
    }

    protected void mappingListJsonBody(ActionRuntime runtime, VirtualActionForm virtualActionForm, String json) {
        try {
            final ActionFormMeta formMeta = virtualActionForm.getFormMeta();
            final ParameterizedType pt = formMeta.getListFormParameterParameterizedType().get(); // already checked
            final List<Object> fromJsonList = getJsonManager().fromJsonList(json, pt);
            acceptJsonRealForm(virtualActionForm, fromJsonList);
        } catch (RuntimeException e) {
            // TODO jflute lastaflute: [A] fitting: List Json debug challenge (2015/06/17)
            //getJsonManager().fromJsonList(json, (ParameterizedType) new TypeToken<List<Map<?, ?>>>() {
            //}.getType());
            throwListJsonBodyParseFailureException(runtime, virtualActionForm, json, null, e);
        }
    }

    protected void throwListJsonBodyParseFailureException(ActionRuntime runtime, VirtualActionForm virtualActionForm, String json,
            String debugChallenge, RuntimeException e) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Cannot parse list json of the request body:");
        sb.append("\n[List JsonBody Parse Failure]");
        sb.append(LF).append(runtime);
        sb.append(LF).append(virtualActionForm);
        sb.append(LF).append(json);
        if (debugChallenge != null) {
            sb.append(LF).append(debugChallenge);
        }
        throwRequestJsonParseFailureException(sb.toString(), e);
    }

    // ===================================================================================
    //                                                                        Property Set
    //                                                                        ============
    protected void setProperty(Object bean, String name, Object value, Object parentBean, String parentName) {
        if (bean == null) {
            return;
        }
        final int nestedIndex = name.indexOf(NESTED_DELIM); // sea.mythica
        final int indexedIndex = name.indexOf(INDEXED_DELIM); // sea[0]
        final int mappedIndex = name.indexOf(MAPPED_DELIM); // sea(over)
        if (nestedIndex < 0 && indexedIndex < 0 && mappedIndex < 0) {
            setSimpleProperty(bean, name, value, parentBean, parentName);
        } else {
            final int minIndex = minIndex(minIndex(nestedIndex, indexedIndex), mappedIndex);
            if (minIndex == nestedIndex) {
                final String front = name.substring(0, minIndex);
                final Object simpleProperty = prepareSimpleProperty(bean, front);
                final String rear = name.substring(minIndex + 1);
                setProperty(simpleProperty, rear, value, bean, front); // *recursive
            } else if (minIndex == indexedIndex) {
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
            } else { // map
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
            actuallySetSimpleProperty(bean, name, value, parentBean, parentName);
        } catch (IllegalPropertyRuntimeException e) {
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

    // ===================================================================================
    //                                                                 Actually Set Simple
    //                                                                 ===================
    protected void actuallySetSimpleProperty(Object bean, String name, Object value, Object parentBean, String parentName) {
        if (bean instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) bean;
            setMapProperty(map, name, value, parentBean, parentName);
            return;
        }
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(bean.getClass());
        if (!beanDesc.hasPropertyDesc(name)) {
            return;
        }
        final PropertyDesc pd = beanDesc.getPropertyDesc(name);
        if (!pd.isWritable()) {
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
            final String realValue = prepareStringScalar(value); // not null (empty if null)
            if (isJsonParameterProperty(pd)) { // e.g. JsonPrameter
                pd.setValue(bean, parseJsonParameter(bean, name, realValue, pd));
            } else if (isClassificationProperty(propertyType)) { // means CDef
                pd.setValue(bean, toVerifiedClassification(bean, name, realValue, pd));
            } else { // mainly here, e.g. String, Integer
                pd.setValue(bean, realValue);
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
        if (isJsonPropertyListType(propertyType)) { // e.g. public List<...> beanList;
            if (!pd.isParameterized()) { // e.g. public List anyList;
                throwJsonPropertyListTypeNonGenericException(bean, name, json, pd); // program mistake
                return null; // unreachable
            }
            final ParameterizedClassDesc paramedDesc = pd.getParameterizedClassDesc();
            final Type plainType = paramedDesc.getParameterizedType(); // not null
            if (!(plainType instanceof ParameterizedType)) { // generic array type? anyway check it
                throwJsonPropertyListTypeNonParameterizedException(bean, name, json, pd, plainType); // program mistake
                return null; // unreachable
            }
            final ParameterizedType paramedType = (ParameterizedType) plainType;
            if (Object.class.equals(paramedDesc.getGenericFirstType())) { // e.g. public List<?> beanList;
                throwJsonPropertyListTypeGenericNotScalarException(bean, name, json, pd, paramedType);
                return null; // unreachable
            }
            try {
                return jsonManager.fromJsonList(json, paramedType); // e.g. public List<SeaBean> beanList;
            } catch (RuntimeException e) {
                // TODO jflute lastaflute: [A] fitting: List JSON debug challenge, JSON parameter (2015/06/17)
                throwJsonParameterParseFailureException(bean, name, json, paramedType, null, e);
                return null; // unreachable
            }
        } else { // e.g. public SeaBean seaBean;
            try {
                return jsonManager.fromJson(json, propertyType);
            } catch (RuntimeException e) {
                final Map<String, Object> retryMap = retryJsonAsMapForDebug(json, jsonManager);
                throwJsonParameterParseFailureException(bean, name, json, propertyType, retryMap, e);
                return null; // unreachable
            }
        }
    }

    protected boolean isJsonPropertyListType(Class<?> propertyType) { // just type
        return List.class.equals(propertyType);
    }

    protected void throwJsonPropertyListTypeNonGenericException(Object bean, String name, String json, PropertyDesc pd) {
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

    protected void throwJsonPropertyListTypeNonParameterizedException(Object bean, String name, String json, PropertyDesc pd, Type plainType) {
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

    protected void throwJsonPropertyListTypeGenericNotScalarException(Object bean, String name, String json, PropertyDesc pd,
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

    protected void throwJsonParameterParseFailureException(Object bean, String name, String json, Type propertyType,
            Map<String, Object> retryMap, RuntimeException e) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Cannot parse json of the request parameter:");
        final String debugChallenge = retryMap != null ? buildJsonParameterDebugChallenge(propertyType, retryMap) : null;
        buildClientErrorHeader(sb, "JsonParameter Parse Failure", bean, name, json, propertyType, debugChallenge);
        throwRequestJsonParseFailureException(sb.toString(), e);
    }

    protected String buildJsonParameterDebugChallenge(Type propertyType, Map<String, Object> retryMap) {
        final Class<?> rawClass = DfReflectionUtil.getRawClass(propertyType);
        if (rawClass == null) { // just in case
            return null;
        }
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(rawClass);
        final StringBuilder sb = new StringBuilder();
        sb.append(buildDebugChallengeTitle());
        for (int i = 0; i < beanDesc.getFieldSize(); i++) {
            final Field field = beanDesc.getField(i);
            final String fieldName = field.getName();
            final Class<?> fieldType = field.getType();
            buildJsonDebugChallenge(sb, retryMap, fieldName, fieldType);
        }
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
        } catch (ClassificationConvertFailureException e) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Cannot convert the code of the request parameter to the classification:");
            buildClientErrorHeader(sb, "Classification Convert Failure", bean, name, "code=" + code, propertyType, null);
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
        // TODO jflute lastaflute: [E] improvement: before checking index size, not to be out of memory
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
        final FwWebDirection direction = assistantDirector.assistWebDirection();
        final ActionAdjustmentProvider provider = direction.assistActionAdjustmentProvider();
        return provider.provideIndexedPropertySizeLimit();
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
    protected Map<String, Object> retryJsonAsMapForDebug(String json, final JsonManager jsonManager) {
        try {
            return jsonManager.fromJson(json, Map.class); // retry for debug
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    protected String buildDebugChallengeTitle() {
        return "Debug Challenge:";
    }

    protected void buildJsonDebugChallenge(StringBuilder sb, Map<String, Object> retryMap, String propertyName, final Class<?> propertyType) {
        final Object mappedValue = retryMap.get(propertyName);
        final Class<? extends Object> valueType = mappedValue != null ? mappedValue.getClass() : null;
        final String possible = mappedValue != null ? (propertyType.isAssignableFrom(valueType) ? "o" : "x") : "v";
        sb.append(LF).append("  ").append(possible).append(" ");
        sb.append(propertyType.getSimpleName()).append(" ").append(propertyName);
        if (mappedValue != null) {
            sb.append(" = (").append(valueType.getSimpleName()).append(") \"").append(mappedValue).append("\"");
        } else {
            sb.append(" = null");
        }
    }

    // ===================================================================================
    //                                                                        Client Error
    //                                                                        ============
    protected void buildClientErrorHeader(StringBuilder sb, String title, Object bean, String name, String value, Type propertyType,
            String debugChallenge) {
        sb.append(LF).append("[").append(title).append("]");
        sb.append(LF).append(bean.getClass().getSimpleName()).append("#").append(name);
        sb.append(": ").append(propertyType.getTypeName());
        sb.append(LF).append(value);
        if (debugChallenge != null) {
            sb.append(LF).append(debugChallenge);
        }
    }

    // no server error because it can occur by user's trick
    // while, is likely to due to client bugs (or server) so request client error
    protected void throwRequestJsonParseFailureException(String msg, RuntimeException e) {
        throw new RequestJsonParseFailureException(msg, e);
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
}
