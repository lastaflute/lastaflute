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
package org.dbflute.lastaflute.web.ruts.process;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.lasta.di.core.aop.javassist.AspectWeaver;
import org.dbflute.lasta.di.helper.beans.BeanDesc;
import org.dbflute.lasta.di.helper.beans.ParameterizedClassDesc;
import org.dbflute.lasta.di.helper.beans.PropertyDesc;
import org.dbflute.lasta.di.helper.beans.exception.IllegalPropertyRuntimeException;
import org.dbflute.lasta.di.helper.beans.factory.BeanDescFactory;
import org.dbflute.lasta.di.util.LdiArrayUtil;
import org.dbflute.lasta.di.util.LdiClassUtil;
import org.dbflute.lasta.di.util.LdiModifierUtil;
import org.dbflute.lastaflute.core.direction.FwAssistantDirector;
import org.dbflute.lastaflute.core.json.JsonManager;
import org.dbflute.lastaflute.core.util.ContainerUtil;
import org.dbflute.lastaflute.web.api.JsonBody;
import org.dbflute.lastaflute.web.api.JsonParameter;
import org.dbflute.lastaflute.web.direction.OptionalWebDirection;
import org.dbflute.lastaflute.web.exception.ForcedRequest404NotFoundException;
import org.dbflute.lastaflute.web.exception.IndexedPropertyNotListArrayRuntimeException;
import org.dbflute.lastaflute.web.exception.NoParameterizedListRuntimeException;
import org.dbflute.lastaflute.web.path.ActionAdjustmentProvider;
import org.dbflute.lastaflute.web.ruts.VirtualActionForm;
import org.dbflute.lastaflute.web.ruts.config.ActionExecute;
import org.dbflute.lastaflute.web.ruts.config.ModuleConfig;
import org.dbflute.lastaflute.web.ruts.multipart.ActionMultipartRequestHandler;
import org.dbflute.lastaflute.web.ruts.multipart.MultipartRequestHandler;
import org.dbflute.lastaflute.web.ruts.multipart.MultipartRequestWrapper;
import org.dbflute.lastaflute.web.servlet.filter.RequestLoggingFilter;
import org.dbflute.lastaflute.web.servlet.request.RequestManager;
import org.dbflute.optional.OptionalThing;

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
    public void populateParameter(ActionExecute execute, OptionalThing<VirtualActionForm> optForm) throws IOException, ServletException {
        if (!optForm.isPresent()) {
            return;
        }
        final VirtualActionForm virtualActionForm = optForm.get();
        if (handleJsonBody(execute, virtualActionForm)) {
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
        final Map<String, Object> params = getAllParameters(requestManager.getRequest(), multipartHandler, execute);
        for (Entry<String, Object> entry : params.entrySet()) {
            final String name = entry.getKey();
            final Object value = entry.getValue();
            try {
                setProperty(realForm, name, value, null, null);
            } catch (Throwable cause) {
                handleIllegalPropertyPopulateException(realForm, name, value, cause); // adjustment here
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

    protected Map<String, Object> getAllParameters(HttpServletRequest request, MultipartRequestHandler multipartHandler,
            ActionExecute execute) {
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

    protected void handleIllegalPropertyPopulateException(Object form, String name, Object value, Throwable cause) throws ServletException {
        if (isRequest404NotFoundException(cause)) { // for indexed property check
            throw new ServletException(cause);
        }
        throwFormParameterPopulateFailureExcetion(form, name, value, cause);
    }

    protected void throwFormParameterPopulateFailureExcetion(Object form, String name, Object value, Throwable cause) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Failed to populate the parameter to the form.");
        br.addItem("Action Form");
        br.addElement(form);
        br.addItem("Property Name");
        br.addElement(name);
        br.addItem("Property Value");
        br.addElement(value);
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg, cause);
    }

    protected boolean isRequest404NotFoundException(Throwable cause) {
        return cause instanceof RequestLoggingFilter.Request404NotFoundException;
    }

    // ===================================================================================
    //                                                                           JSON Body
    //                                                                           =========
    protected boolean handleJsonBody(ActionExecute execute, VirtualActionForm virtualActionForm) throws IOException {
        if (isJsonBodyForm(virtualActionForm.getFormMeta().getFormType())) {
            mappingJsonBody(execute, virtualActionForm, getRequestBody());
            return true;
        }
        if (isJsonBodyListForm(virtualActionForm)) {
            mappingListJsonBody(execute, virtualActionForm, getRequestBody());
            return true;
        }
        return false;
    }

    protected boolean isJsonBodyForm(Class<? extends Object> formType) {
        return formType.getAnnotation(JsonBody.class) != null;
    }

    protected boolean isJsonBodyListForm(VirtualActionForm virtualActionForm) {
        return virtualActionForm.getFormMeta().getListFormGenericType().map(genericType -> {
            return isJsonBodyForm(genericType);
        }).orElse(false);
    }

    protected String getRequestBody() throws IOException {
        return requestManager.getRequestBody();
    }

    protected void mappingJsonBody(ActionExecute execute, VirtualActionForm virtualActionForm, String json) {
        try {
            final Object realForm = getJsonManager().mappingJsonTo(json, virtualActionForm.getFormSupplier());
            acceptJsonRealForm(virtualActionForm, realForm);
        } catch (RuntimeException e) {
            throwJsonBodyParseFailureException(execute, virtualActionForm, json, e);
        }
    }

    protected void mappingListJsonBody(ActionExecute execute, VirtualActionForm virtualActionForm, String json) {
        try {
            final Object realForm = getJsonManager().mappingJsonToList(json, virtualActionForm.getFormSupplier());
            acceptJsonRealForm(virtualActionForm, realForm);
        } catch (RuntimeException e) {
            throwListJsonBodyParseFailureException(execute, virtualActionForm, json, e);
        }
    }

    protected JsonManager getJsonManager() {
        return ContainerUtil.getComponent(JsonManager.class);
    }

    protected void acceptJsonRealForm(VirtualActionForm virtualActionForm, Object realForm) {
        virtualActionForm.acceptRealForm(realForm);
    }

    protected void throwJsonBodyParseFailureException(ActionExecute execute, VirtualActionForm virtualActionForm, String json,
            RuntimeException e) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Cannot parse json of the request body:");
        sb.append("\n[JsonBody Parse Failure]");
        sb.append("\n").append(execute);
        sb.append("\n").append(virtualActionForm);
        sb.append("\n").append(json);
        sb.append("\n").append(e.getClass().getName()).append("\n").append(e.getMessage());
        // TODO jflute lastaflute: [E] JSON parse error, option to switch to 500
        throwRequest404NotFoundException(sb.toString());
    }

    protected void throwListJsonBodyParseFailureException(ActionExecute execute, VirtualActionForm virtualActionForm, String json,
            RuntimeException e) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Cannot parse list json of the request body:");
        sb.append("\n[List JsonBody Parse Failure]");
        sb.append("\n").append(execute);
        sb.append("\n").append(virtualActionForm);
        sb.append("\n").append(json);
        sb.append("\n").append(e.getClass().getName()).append("\n").append(e.getMessage());
        throwRequest404NotFoundException(sb.toString());
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
            throwRequest404NotFoundException(msg);
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
        if (propertyType.isArray()) {
            pd.setValue(bean, value);
        } else if (List.class.isAssignableFrom(propertyType)) {
            final boolean abstractProperty = LdiModifierUtil.isAbstract(propertyType);
            final List<String> valueList;
            if (abstractProperty) {
                valueList = new ArrayList<String>();
            } else {
                @SuppressWarnings("unchecked")
                final List<String> cast = (List<String>) LdiClassUtil.newInstance(propertyType);
                valueList = cast;
            }
            valueList.addAll(Arrays.asList((String[]) value));
            pd.setValue(bean, valueList);
        } else if (value == null) {
            pd.setValue(bean, null);
        } else if (value instanceof String[]) { // almost parameters are here
            final String[] values = (String[]) value;
            final String realValue = values.length > 0 ? values[0] : null;
            if (isJsonParameterProperty(pd)) {
                final Object jsonObj = parseJsonProperty(bean, name, realValue, propertyType);
                pd.setValue(bean, jsonObj);
            } else { // normally here
                pd.setValue(bean, realValue);
            }
        } else {
            pd.setValue(bean, value);
        }
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

    protected Object parseJsonProperty(Object bean, String name, String json, Class<?> propertyType) {
        final JsonManager jsonManager = getJsonManager();
        try {
            return jsonManager.decode(json, propertyType);
        } catch (RuntimeException e) {
            throwJsonPropertyParseFailureException(bean, name, json, propertyType, e);
            return null; // unreachable
        }
    }

    protected void throwJsonPropertyParseFailureException(Object bean, String name, String json, Class<?> propertyType, RuntimeException e) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Cannot parse json of the request parameter:");
        sb.append("\n[JsonProperty Parse Failure]");
        sb.append("\n").append(bean.getClass().getSimpleName()).append("#").append(name);
        sb.append(" (").append(propertyType.getSimpleName()).append(")");
        sb.append("\n").append(json);
        sb.append("\n").append(e.getClass().getName()).append("\n").append(e.getMessage());
        throwRequest404NotFoundException(sb.toString());
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
        String msg = "Non number index of the indexed property: name=" + name + "\n" + e.getMessage();
        throwRequest404NotFoundException(msg);
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
        final OptionalWebDirection direction = assistantDirector.assistOptionalWebDirection();
        final ActionAdjustmentProvider provider = direction.assistActionAdjustmentProvider();
        return provider.provideIndexedPropertySizeLimit();
    }

    protected void throwIndexedPropertyMinusIndexException(String name, int index) {
        String msg = "Minus index of the indexed property: name=" + name;
        throwRequest404NotFoundException(msg);
    }

    protected void throwIndexedPropertySizeOverException(String name, int index) {
        String msg = "Too large size of the indexed property: name=" + name + ", index=" + index;
        throwRequest404NotFoundException(msg);
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
            setArrayValue(array, indexes, value);
        } else if (List.class.isAssignableFrom(pd.getPropertyType())) {
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
                    throw new NoParameterizedListRuntimeException(getRealClass(beanDesc.getBeanClass()), pd.getPropertyName() + sb);
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
            throw new IndexedPropertyNotListArrayRuntimeException(getRealClass(beanDesc.getBeanClass()), pd.getPropertyName());
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
        BeanDesc beanDesc = BeanDescFactory.getBeanDesc(bean.getClass());
        if (!beanDesc.hasPropertyDesc(name)) {
            return null;
        }
        PropertyDesc pd = beanDesc.getPropertyDesc(name);
        if (!pd.isReadable()) {
            return null;
        }
        if (pd.getPropertyType().isArray()) {
            Object array = pd.getValue(bean);
            Class<?> elementType = getArrayElementType(pd.getPropertyType(), indexes.length);
            if (array == null) {
                int[] newIndexes = new int[indexes.length];
                newIndexes[0] = indexes[0] + 1;
                array = Array.newInstance(elementType, newIndexes);
            }
            array = expand(array, indexes, elementType);
            pd.setValue(bean, array);
            return getArrayValue(array, indexes, elementType);
        } else if (List.class.isAssignableFrom(pd.getPropertyType())) {
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
                    throw new NoParameterizedListRuntimeException(getRealClass(beanDesc.getBeanClass()), pd.getPropertyName() + sb);
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
            throw new IndexedPropertyNotListArrayRuntimeException(getRealClass(beanDesc.getBeanClass()), pd.getPropertyName());
        }
    }

    protected Object getArrayValue(Object array, int[] indexes, Class<?> elementType) {
        Object value = array;
        elementType = convertClass(elementType);
        for (int i = 0; i < indexes.length; i++) {
            Object value2 = Array.get(value, indexes[i]);
            if (i == indexes.length - 1 && value2 == null) {
                value2 = LdiClassUtil.newInstance(elementType);
                Array.set(value, indexes[i], value2);
            }
            value = value2;
        }
        return value;
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
        if (LdiModifierUtil.isAbstract(clazz) && Map.class.isAssignableFrom(clazz)) {
            return HashMap.class;
        }
        return clazz;
    }

    // -----------------------------------------------------
    //                                            Real Class
    //                                            ----------
    protected Class<?> getRealClass(Class<?> clazz) {
        if (clazz.getName().indexOf(AspectWeaver.SUFFIX_ENHANCED_CLASS) > 0) {
            return clazz.getSuperclass();
        }
        return clazz;
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected void throwRequest404NotFoundException(String msg) {
        throw new ForcedRequest404NotFoundException(msg);
    }
}
