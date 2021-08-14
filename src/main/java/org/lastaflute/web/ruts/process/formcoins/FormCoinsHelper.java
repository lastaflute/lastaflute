/*
 * Copyright 2015-2021 the original author or authors.
 * Copyright 2015-2021 the original author or authors.
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
package org.lastaflute.web.ruts.process.formcoins;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
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
import java.util.function.Function;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.jdbc.Classification;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.DfTypeUtil.ParseBooleanException;
import org.dbflute.util.DfTypeUtil.ParseDateException;
import org.dbflute.util.Srl;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.json.JsonObjectConvertible;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.core.util.LaClassificationUtil;
import org.lastaflute.core.util.LaClassificationUtil.ClassificationUnknownCodeException;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.ParameterizedClassDesc;
import org.lastaflute.di.helper.beans.PropertyDesc;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;
import org.lastaflute.di.helper.misc.ParameterizedRef;
import org.lastaflute.di.util.LdiClassUtil;
import org.lastaflute.di.util.LdiModifierUtil;
import org.lastaflute.web.exception.Forced400BadRequestException;
import org.lastaflute.web.exception.IndexedPropertyNonParameterizedListException;
import org.lastaflute.web.exception.IndexedPropertyNotListArrayException;
import org.lastaflute.web.exception.JsonBodyCannotReadFromRequestException;
import org.lastaflute.web.exception.RequestClassifiationConvertFailureException;
import org.lastaflute.web.exception.RequestJsonParseFailureException;
import org.lastaflute.web.exception.RequestPropertyMappingFailureException;
import org.lastaflute.web.path.FormMappingOption;
import org.lastaflute.web.ruts.VirtualForm;
import org.lastaflute.web.ruts.config.ActionFormProperty;
import org.lastaflute.web.ruts.multipart.MultipartRequestHandler;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.ruts.process.debugchallenge.JsonDebugChallenge;
import org.lastaflute.web.ruts.process.exception.ActionFormPopulateFailureException;
import org.lastaflute.web.ruts.process.exception.RequestUndefinedParameterInFormException;
import org.lastaflute.web.servlet.filter.RequestLoggingFilter.RequestClientErrorException;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * @author jflute
 * @since 1.1.2 (2019/05/03 Friday)
 */
public class FormCoinsHelper { // keep singleton-able to be simple

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final String LF = "\n";

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final FwAssistantDirector assistantDirector;
    protected final RequestManager requestManager;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public FormCoinsHelper(FwAssistantDirector assistantDirector, RequestManager requestManager) {
        this.assistantDirector = assistantDirector;
        this.requestManager = requestManager;
    }

    // ===================================================================================
    //                                                                            Populate
    //                                                                            ========
    public Map<String, Object> prepareRequestParameterMap(MultipartRequestHandler multipartHandler, FormMappingOption option) {
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
        final OptionalThing<Function<Map<String, Object>, Map<String, Object>>> optFilter = option.getRequestParameterMapFilter();
        if (optFilter.isPresent()) { // no map() here, to keep normal route simple
            final Map<String, Object> filteredMap = optFilter.get().apply(Collections.unmodifiableMap(paramMap));
            return filteredMap != null ? filteredMap : paramMap;
        } else { // normally here
            return paramMap;
        }
    }

    public void handleIllegalPropertyPopulateException(Object form, String name, Object value, ActionRuntime runtime, Throwable cause)
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
            valueObj = value;
        }
        br.addElement(valueObj);
        final String msg = br.buildExceptionMessage();
        throw new ActionFormPopulateFailureException(msg, cause);
    }

    // ===================================================================================
    //                                                                           JSON Body
    //                                                                           =========
    public void throwJsonBodyCannotReadFromRequestException(VirtualForm virtualForm, RuntimeException e) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Cannot read request body for JSON.");
        br.addItem("Advice");
        br.addElement("Your action expects JSON string on request body.");
        br.addElement("Make sure your request for JSON body.");
        br.addElement("Or it should be form...? e.g. SeaBody => SeaForm");
        br.addItem("Body Class");
        br.addElement(virtualForm);
        final String msg = br.buildExceptionMessage();
        throw new JsonBodyCannotReadFromRequestException(msg, e);
    }

    public String buildJsonBodyDebugDisplay(String value) {
        // want to show all as parameter, but limit just in case to avoid large logging
        final String trimmed = value.trim();
        return !trimmed.isEmpty() ? "\n" + Srl.cut(trimmed, 800, "...") : " *empty body"; // might have rear LF
    }

    // -----------------------------------------------------
    //                                             Bean JSON
    //                                             ---------
    public void throwJsonBodyParseFailureException(ActionRuntime runtime, VirtualForm virtualForm, String json, RuntimeException e) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Cannot parse json on the request body.");
        sb.append(LF).append(LF).append("[JsonBody Parse Failure]");
        sb.append(LF).append(runtime);
        sb.append(LF).append(virtualForm);
        sb.append(LF).append(json);
        final Map<String, Object> retryMap = retryJsonAsMapForDebug(json);
        List<JsonDebugChallenge> challengeList = new ArrayList<JsonDebugChallenge>();
        if (!retryMap.isEmpty()) {
            sb.append(LF).append(buildDebugChallengeTitle());
            final List<JsonDebugChallenge> nestedList = prepareJsonBodyDebugChallengeList(virtualForm, retryMap, null);
            for (JsonDebugChallenge challenge : nestedList) {
                sb.append(challenge.toChallengeDisp());
            }
            challengeList.addAll(nestedList);
        }
        throwRequestJsonParseFailureException(sb.toString(), challengeList, e);
    }

    // -----------------------------------------------------
    //                                             List JSON
    //                                             ---------
    public void throwListJsonBodyParseFailureException(ActionRuntime runtime, VirtualForm virtualForm, String json, RuntimeException e) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Cannot parse list json on the request body.");
        sb.append(LF).append(LF).append("[List JsonBody Parse Failure]");
        sb.append(LF).append(runtime);
        sb.append(LF).append(virtualForm);
        sb.append(LF).append(json);
        final List<Map<String, Object>> retryList = retryJsonListAsMapForDebug(json);
        final List<JsonDebugChallenge> challengeList = new ArrayList<JsonDebugChallenge>();
        if (!retryList.isEmpty()) {
            sb.append(LF).append(buildDebugChallengeTitle());
            int index = 1;
            for (Map<String, Object> retryMap : retryList) {
                sb.append(LF).append(" (index: ").append(index).append(")");
                final List<JsonDebugChallenge> nestedList = prepareJsonBodyDebugChallengeList(virtualForm, retryMap, index);
                challengeList.addAll(nestedList);
                nestedList.forEach(challenge -> sb.append(challenge.toChallengeDisp()));
                ++index;
            }
        }
        throwRequestJsonParseFailureException(sb.toString(), challengeList, e);
    }

    // -----------------------------------------------------
    //                                       Debug Challenge
    //                                       ---------------
    protected List<JsonDebugChallenge> prepareJsonBodyDebugChallengeList(VirtualForm virtualForm, Map<String, Object> retryMap,
            Integer elementIndex) {
        if (retryMap.isEmpty()) {
            return Collections.emptyList();
        }
        final List<JsonDebugChallenge> challengeList = new ArrayList<JsonDebugChallenge>();
        for (ActionFormProperty property : virtualForm.getFormMeta().properties()) {
            final String propertyName = property.getPropertyName();
            final Class<?> propertyType = property.getPropertyDesc().getPropertyType();
            final JsonDebugChallenge challenge = createJsonDebugChallenge(retryMap, propertyName, propertyType, elementIndex);
            challengeList.add(challenge);
        }
        return Collections.unmodifiableList(challengeList);
    }

    // ===================================================================================
    //                                                                        Property Set
    //                                                                        ============
    public int minIndex(int index1, int index2) {
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
    // -----------------------------------------------------
    //                                             List JSON
    //                                             ---------
    public void throwListJsonPropertyNonGenericException(Object bean, String name, String json, PropertyDesc pd) {
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

    public void throwListJsonPropertyNonParameterizedException(Object bean, String name, String json, PropertyDesc pd, Type plainType) {
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

    public void throwListJsonPropertyGenericNotScalarException(Object bean, String name, String json, PropertyDesc pd,
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

    public void throwListJsonParameterParseFailureException(Object bean, String name, String json, Type propertyType, RuntimeException e) {
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
    public void throwJsonParameterParseFailureException(Object bean, String name, String json, Class<?> propertyType, RuntimeException e) {
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
    public boolean isClassificationProperty(Class<?> propertyType) {
        return LaClassificationUtil.isCls(propertyType);
    }

    public Classification toVerifiedClassification(Object bean, String name, Object code, Class<?> propertyType) {
        try {
            return LaClassificationUtil.toCls(propertyType, code);
        } catch (ClassificationUnknownCodeException e) { // simple message because of catched later
            String msg = "Cannot convert the code to the classification: " + code + " to " + propertyType.getSimpleName();
            throwRequestClassifiationConvertFailureException(msg, e);
            return null; // unreachable
        }
    }

    protected void throwRequestClassifiationConvertFailureException(String msg, Exception e) {
        throw new RequestClassifiationConvertFailureException(msg, e);
    }

    // -----------------------------------------------------
    //                                          Type Failure
    //                                          ------------
    public void throwTypeFailureBadRequest(Object bean, String propertyPath, Class<?> propertyType, Object exp, RuntimeException cause) {
        if (cause instanceof Forced400BadRequestException) { // already bad request so no need to new
            throw cause; // e.g. classification's exception
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("The property cannot be the type: property=");
        sb.append(bean != null ? bean.getClass().getSimpleName() : null);
        sb.append("@").append(propertyPath).append("(").append(propertyType.getSimpleName()).append(") value=").append(exp);
        throwRequestPropertyMappingFailureException(sb.toString(), cause); // though bad request
    }

    // -----------------------------------------------------
    //                                       Mapping Failure
    //                                       ---------------
    public void handleMappingFailureException(Object bean, String name, Object value, StringBuilder pathSb, PropertyDesc pd,
            RuntimeException e) {
        if (!isBadRequestMappingFailureException(e)) {
            throw e;
        }
        // e.g. non-number GET but number type property
        // suppress easy 500 error by e.g. non-number GET parameter (similar with path parameter)
        //  (o): ?seaId=123
        //  (x): ?seaId=abc *this case
        final String beanExp = bean != null ? bean.getClass().getName() : null; // null check just in case
        final Object dispValue = value instanceof Object[] ? Arrays.asList((Object[]) value).toString() : value;
        final StringBuilder sb = new StringBuilder();
        sb.append("Failed to set the value to the property.");
        buildClientErrorHeader(sb, "Form Mapping Failure", beanExp, name, dispValue, pd.getPropertyType(), null);
        throwRequestPropertyMappingFailureException(sb.toString(), e);
    }

    protected boolean isBadRequestMappingFailureException(RuntimeException e) {
        // may be BeanIllegalPropertyException so also check nested exception
        return isTypeFailureException(e) || isTypeFailureException(e.getCause());
    }

    public boolean isTypeFailureException(Throwable cause) { // except classification here
        return cause instanceof NumberFormatException // e.g. Integer, Long
                || cause instanceof ParseDateException // e.g. LocalDate
                || cause instanceof ParseBooleanException // e.g. Boolean
                || cause instanceof RequestClassifiationConvertFailureException // e.g. CDef
        ;
    }

    // ===================================================================================
    //                                                                         Parse Index
    //                                                                         ===========
    public void throwIndexedPropertyNonNumberIndexException(String name, NumberFormatException e) {
        String msg = "Non number index of the indexed property: name=" + name + LF + e.getMessage();
        throwRequestPropertyMappingFailureException(msg, e);
    }

    public void throwIndexedPropertyMinusIndexException(String name, int index) {
        String msg = "Minus index of the indexed property: name=" + name;
        throwRequestPropertyMappingFailureException(msg);
    }

    public void throwIndexedPropertySizeOverException(String name, int index) {
        String msg = "Too large size of the indexed property: name=" + name + ", index=" + index;
        throwRequestPropertyMappingFailureException(msg);
    }

    // ===================================================================================
    //                                                                    Indexed Property
    //                                                                    ================
    // -----------------------------------------------------
    //                                  Set Indexed Property
    //                                  --------------------
    public void setArrayValue(Object array, int[] indexes, Object value) {
        for (int i = 0; i < indexes.length - 1; i++) {
            array = Array.get(array, indexes[i]);
        }
        Array.set(array, indexes[indexes.length - 1], value);
    }

    // -----------------------------------------------------
    //                              Prepare Indexed Property
    //                              ------------------------
    public Object getArrayValue(Object array, int[] indexes, Class<?> elementType) {
        Object value = array;
        elementType = convertArrayClass(elementType);
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

    // -----------------------------------------------------
    //                                             Exception
    //                                             ---------
    public void throwIndexedPropertyNonParameterizedListException(BeanDesc beanDesc, PropertyDesc pd) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The list of indexed property was not parameterized.");
        br.addItem("ActionForm");
        br.addElement(toIndexedBeanRealClass(beanDesc.getBeanClass()));
        br.addItem("Property");
        br.addElement(pd);
        br.addItem("Parameterized"); // parameterized info is important here
        final ParameterizedClassDesc paramDesc = pd.getParameterizedClassDesc();
        if (paramDesc != null) {
            br.addElement("isParameterizedClass: " + paramDesc.isParameterizedClass());
            br.addElement("parameterizedType: " + paramDesc.getParameterizedType());
            br.addElement("rawClass: " + paramDesc.getRawClass());
            final ParameterizedClassDesc[] arguments = paramDesc.getArguments();
            if (arguments != null && arguments.length > 0) {
                int index = 0;
                for (ParameterizedClassDesc arg : arguments) {
                    br.addElement("argument" + index + ": " + arg.getParameterizedType());
                    ++index;
                }
            }
        } else {
            br.addElement("getParameterizedClassDesc() returns null");
        }
        final String msg = br.buildExceptionMessage();
        throw new IndexedPropertyNonParameterizedListException(msg);
    }

    public void throwIndexedPropertyNotListArrayException(BeanDesc beanDesc, PropertyDesc pd) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The indexed property was not list or array.");
        br.addItem("Advice");
        br.addElement("Confirm the property type in your form.");
        if ("ImmutableList".equals(pd.getPropertyType().getSimpleName())) { // patch message
            br.addElement("And if you use ImmutableList of Eclipse Collections (as option),");
            br.addElement("unfortunately it is not supported as indexed property.");
            br.addElement("So use 'java.util.List'.");
        }
        br.addItem("ActionForm");
        br.addElement(toIndexedBeanRealClass(beanDesc.getBeanClass()));
        br.addItem("Property");
        br.addElement(pd);
        final String msg = br.buildExceptionMessage();
        throw new IndexedPropertyNotListArrayException(msg);
    }

    // -----------------------------------------------------
    //                                            Real Class
    //                                            ----------
    protected Class<?> toIndexedBeanRealClass(Class<?> clazz) {
        return ContainerUtil.toRealClassIfEnhanced(clazz);
    }

    // -----------------------------------------------------
    //                                          Array Helper
    //                                          ------------
    public Class<?> getArrayElementType(Class<?> clazz, int depth) {
        for (int i = 0; i < depth; i++) {
            clazz = clazz.getComponentType();
        }
        return clazz;
    }

    public Object expandArray(Object array, int[] indexes, Class<?> elementType) {
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
            Array.set(array, indexes[0], expandArray(Array.get(array, indexes[0]), newIndexes, elementType));
        }
        return array;
    }

    public Class<?> convertArrayClass(Class<?> clazz) {
        return LdiModifierUtil.isAbstract(clazz) && Map.class.isAssignableFrom(clazz) ? HashMap.class : clazz;
    }

    // ===================================================================================
    //                                                                         JSON Assist
    //                                                                         ===========
    public JsonObjectConvertible chooseJsonObjectConvertible(ActionRuntime runtime, FormMappingOption option) {
        return option.getRequestJsonEngineProvider()
                .map(provider -> (JsonObjectConvertible) provider.apply(runtime))
                .orElseGet(() -> getJsonManager());
    }

    protected JsonManager getJsonManager() {
        return requestManager.getJsonManager();
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
            return getJsonManager().fromJsonParameteried(json, new ParameterizedRef<List<Map<String, Object>>>() {
            }.getType());
        } catch (RuntimeException ignored) {
            return Collections.emptyList();
        }
    }

    protected JsonDebugChallenge createJsonDebugChallenge(Map<String, Object> retryMap, String propertyName, Class<?> propertyType,
            Integer elementIndex) {
        final JsonManager jsonManager = requestManager.getJsonManager();
        final Object mappedValue = retryMap.get(propertyName);
        return new JsonDebugChallenge(jsonManager, propertyName, propertyType, mappedValue, elementIndex);
    }

    // ===================================================================================
    //                                                                        Client Error
    //                                                                        ============
    public void buildClientErrorHeader(StringBuilder sb, String title, Object bean, String name, Object value, Type propertyType,
            String challengeDisp) {
        sb.append(LF).append(LF).append("[").append(title).append("]");
        sb.append(LF).append("Mapping To: ");
        sb.append(bean.getClass().getSimpleName()).append("@").append(name);
        sb.append(" (").append(propertyType.getTypeName()).append(")");
        sb.append(LF).append("Requested Value: ");
        if (value != null) {
            final String exp = value.toString();
            sb.append(exp.contains(LF) ? LF : "").append(exp);
        } else {
            sb.append("null");
        }
        if (challengeDisp != null && challengeDisp.length() > 0) {
            sb.append(LF).append(buildDebugChallengeTitle());
            sb.append(challengeDisp); // debugChallenge starts with LF
        }
    }

    public String buildDebugChallengeTitle() {
        return "Debug Challenge: (o: maybe assignable, x: cannot, v: no value, ?: unknown)";
    }

    // no server error because it can occur by user's trick
    // while, is likely to due to client bugs (or server) so request client error
    public void throwRequestJsonParseFailureException(String msg, List<JsonDebugChallenge> challengeList, RuntimeException cause) {
        throw new RequestJsonParseFailureException(msg, getRequestJsonParseFailureMessages(), cause).withChallengeList(challengeList);
    }

    protected UserMessages getRequestJsonParseFailureMessages() {
        return UserMessages.empty();
    }

    public void throwRequestPropertyMappingFailureException(String msg) {
        throw new RequestPropertyMappingFailureException(msg, getRequestPropertyMappingFailureMessages());
    }

    public void throwRequestPropertyMappingFailureException(String msg, RuntimeException cause) {
        throw new RequestPropertyMappingFailureException(msg, getRequestPropertyMappingFailureMessages(), cause);
    }

    protected UserMessages getRequestPropertyMappingFailureMessages() {
        return UserMessages.empty();
    }

    public void throwRequestUndefinedParameterInFormException(Object bean, String name, Object value, FormMappingOption option,
            BeanDesc beanDesc) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Undefined parameter in the form.");
        br.addItem("Advice");
        br.addElement("Request parameters should be related to any property of form.");
        br.addElement("For example:");
        br.addElement("  (x): ?sea=mystic&land=oneman");
        br.addElement("    public class MaihamaForm { // *Bad: 'land' is undefined");
        br.addElement("        public String sea;");
        br.addElement("    }");
        br.addElement("  (o): ?sea=mystic&land=oneman");
        br.addElement("    public class MaihamaForm {");
        br.addElement("        public String sea;");
        br.addElement("        public String land; // Good");
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
        br.addItem("Mapping Option");
        br.addElement(option);
        final String msg = br.buildExceptionMessage();
        throw new RequestUndefinedParameterInFormException(msg, getRequestUndefinedParameterInFormMessages());
    }

    protected UserMessages getRequestUndefinedParameterInFormMessages() {
        return UserMessages.empty();
    }
}
